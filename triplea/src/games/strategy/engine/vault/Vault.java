/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package games.strategy.engine.vault;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.*;

import games.strategy.net.IChannelMessenger;
import games.strategy.net.IChannelSubscribor;

/**
 * A vault is a secure way for the client and server to share information without 
 * trusting each other.<p>
 * 
 * Data can be locked in the vault by a node.  This data then is not readable
 * by other nodes until the data is unlocked.<p>
 * 
 * When the data is unlocked by the original node, other nodes can read the data.  
 * When data is put in the vault, it cant be changed by the originating node.<p>
 * 
 * NOTE: to allow the data locked in the vault to be gc'd, the <code>release(VaultID id)<code> method 
 * should be called when it is no longer needed.
 * 
 * 
 * @author Sean Bridges
 */
public class Vault
{
    private static final String VAULT_CHANNEL = "games.strategy.engine.vault.VAULT_CHANNEL";
    
    private static final String ALGORITHM = "DES";

    //0xCAFEBABE
    //we encrypt both this value and data when we encrypt data.
    //when decrypting we ensure that KNOWN_VAL 
    private static final byte[] KNOWN_VAL = new byte[] {0xC, 0xA, 0xF, 0xE, 0xB, 0xA, 0xB, 0xE };
    
    private final KeyGenerator m_keyGen;
    private final IChannelMessenger m_channelMessenger;
    
    //Maps VaultID -> SecretKey
    private final Map m_secretKeys = Collections.synchronizedMap(new HashMap());

    //maps ValutID -> encrypted byte[]
    private final Map m_unverifiedValues = Collections.synchronizedMap(new HashMap());
    //maps VaultID -> byte[]
    private final Map m_verifiedValues = Collections.synchronizedMap(new HashMap());
    //maps INode->Long
    //these are the ids of the remote keys that we have seen
    //dont allow values to be reused
    private final Map m_maxRemoteIDIndexes = Collections.synchronizedMap(new HashMap());
    
    /**
     * @param channelMessenger
     */
    public Vault(final IChannelMessenger channelMessenger)
    {
        m_channelMessenger = channelMessenger;
        
        if(!m_channelMessenger.hasChannel(VAULT_CHANNEL))
        {
            m_channelMessenger.createChannel(IRemoteVault.class, VAULT_CHANNEL);
        }
        m_channelMessenger.registerChannelSubscriber(m_remoteVault, VAULT_CHANNEL);
        
        try
        {
            m_keyGen = KeyGenerator.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            throw new IllegalStateException("Nothing known about algorithm:" +  ALGORITHM);
        }
    }
    
    private IRemoteVault getRemoteBroadcaster()
    {
        return (IRemoteVault) m_channelMessenger.getChannelBroadcastor(VAULT_CHANNEL);
    }
    
    /**
     * place data in the vault.  An encrypted form of the data is sent at this
     * time to all nodes.  <p>
     * The same key used to encrypt the KNOWN_VALUE so that nodes can verify
     * the key when it is used to decrypt the data.  
     * 
     * @param data - the data to lock
     * @return the VaultId of the data
     */
    //this method is synchronized since vault ids must arrive in order
    public synchronized VaultID lock(byte[] data)
    {
        VaultID id = new VaultID(m_channelMessenger.getLocalNode());
        SecretKey key = m_keyGen.generateKey();
        m_secretKeys.put(id, key);
        //we already know it, so might as well keep it
        m_verifiedValues.put(id, data);
        
        Cipher cipher;
        try
        {
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);            
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        } catch (NoSuchPaddingException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }catch (InvalidKeyException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }     
        
        //join the data and known value into one array
        byte[] dataAndCheck = joinDataAndKnown(data);
        
        byte[] encrypted;
        try
        {
            encrypted = cipher.doFinal(dataAndCheck);
        } catch (Exception e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
        
        //tell the world
        getRemoteBroadcaster().addLockedValue(id, encrypted);
        
        return id;
    }
        
    
    /**
     * Join known and data into one array.
     * package access so we can test.
     */
    static byte[] joinDataAndKnown(byte[] data)
    {
        byte[] dataAndCheck = new byte[KNOWN_VAL.length + data.length];
        System.arraycopy(KNOWN_VAL, 0,  dataAndCheck, 0, KNOWN_VAL.length);
        System.arraycopy(data, 0,  dataAndCheck, KNOWN_VAL.length, data.length);
        return dataAndCheck;
    }

    /**
     * allow other nodes to see the data.<p>
     * 
     * You can only unlock data that was locked by the same instance of the Vault
     * 
     * @param id - the vault id to unlock
     */
    public void unlock(VaultID id)
    {
        if(!id.getGeneratedOn().equals(m_channelMessenger.getLocalNode()))
        {
            throw new IllegalArgumentException("Cant unlock data that wasnt locked on this node");
        }
        SecretKey key = (SecretKey) m_secretKeys.get(id);
        //allow the secret key to be gc'd
        m_secretKeys.remove(key);
    
        //let everyone unlock it
        getRemoteBroadcaster().unlock(id, key);
    }
    
    /**
     * Note - if an id has been released, then this will return false.
     * If this instance of vault locked id, then this method will return true
     * if the id has not been released.
     * 
     * @return - has this id been unlocked
     */
    public boolean isUnlocked(VaultID id)
    {
      return m_verifiedValues.containsKey(id);   
    }
     
    /**
     * Get the unlocked data.
     *  
     */
    public byte[] get(VaultID id) throws NotUnlockedException
    {
        if(m_verifiedValues.containsKey(id))
            return (byte[]) m_verifiedValues.get(id);
        else if(m_unverifiedValues.containsKey(id))
        	throw new NotUnlockedException();
        else 
            throw new IllegalStateException("Nothing known about id:" + id);
    }
    
    /**
     * Do we know about the given vault id. 
     */
    public boolean knowsAbout(VaultID id)
    {
        return m_verifiedValues.containsKey(id) || m_unverifiedValues.containsKey(id);
    }
    
    
    /**
     * Allow all data associated with the given vault id to be released and garbage collected<p>
     * An id can be released by any node.<p>
     * If the id has already been released, then nothing will happen. 
     * 
     */
    public void release(VaultID id)
    {
        getRemoteBroadcaster().release(id);
    }
    
    private final IRemoteVault m_remoteVault = new IRemoteVault()
    {
        public void addLockedValue(VaultID id, byte[] data)
        {
            if(id.getGeneratedOn().equals(m_channelMessenger.getLocalNode()))
                return;
            
            //we only want to put something in the vault once!
            //if a remote node can lock the same vaultid more than once
            //then we have no security.
            //we rely on the monotone increasing value of ids
            Long lastID = (Long) m_maxRemoteIDIndexes.get(id.getGeneratedOn());
            if(lastID == null)
                lastID = new Long(-1);
            
            if(id.getUniqueID() <= lastID.longValue())
                throw new IllegalStateException("Attempt was made to reuse a vaultID, cheating is suspected");
                
            m_maxRemoteIDIndexes.put(id.getGeneratedOn(), new Long(id.getUniqueID()));
            
            m_unverifiedValues.put(id, data);
        }

        public void unlock(VaultID id, SecretKey key)
        {
            if(id.getGeneratedOn().equals(m_channelMessenger.getLocalNode()))
                return;
            
            Cipher cipher;
            try
            {
                cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key);            
            } catch (NoSuchAlgorithmException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            } catch (NoSuchPaddingException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            }catch (InvalidKeyException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            }     
            
            byte[] encrypted = (byte[]) m_unverifiedValues.get(id);
            //allow it to be gcd
            m_unverifiedValues.remove(id);
            byte[] decrypted;
            
            try
            {
                decrypted = cipher.doFinal(encrypted);
            } catch (Exception e1)
            {
                e1.printStackTrace();
                throw new IllegalStateException(e1.getMessage());
            }
            
            if(decrypted.length < KNOWN_VAL.length)
                throw new IllegalStateException("decrypted is not long enough to have known value, cheating is suspected");
            
            //check that the known value is correct
            //we use the known value to check that the key given to
            //us was the key used to encrypt the value in the first place
            for(int i = 0; i < KNOWN_VAL.length; i++)
            {
                if( KNOWN_VAL[i] !=  decrypted[i])
                    throw new IllegalStateException("Known value of cipher not correct, cheating is suspected");
            }
            
            byte[] data = new byte[decrypted.length - KNOWN_VAL.length];
            System.arraycopy(decrypted, KNOWN_VAL.length, data, 0, data.length);
            
            m_verifiedValues.put(id, data);
     
        }

        public void release(VaultID id)
        {
            m_unverifiedValues.remove(id);
            m_verifiedValues.remove(id);
        }
    };
    
}


interface IRemoteVault extends IChannelSubscribor
{
   public void addLockedValue(VaultID id, byte[] data);
   public void unlock(VaultID id, SecretKey key);
   public void release(VaultID id);
}