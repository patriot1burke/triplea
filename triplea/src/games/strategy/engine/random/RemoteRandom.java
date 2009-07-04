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
package games.strategy.engine.random;

import java.util.ArrayList;
import java.util.List;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.VerifiedRandomNumbers;
import games.strategy.engine.vault.NotUnlockedException;
import games.strategy.engine.vault.Vault;
import games.strategy.engine.vault.VaultID;

/**
 * @author Sean Bridges
 */
public class RemoteRandom implements IRemoteRandom
{
    private static List<VerifiedRandomNumbers> s_verifiedRandomNumbers = new ArrayList<VerifiedRandomNumbers>();

    public synchronized static List<VerifiedRandomNumbers> getVerifiedRandomNumbers()
    {
        return new ArrayList<VerifiedRandomNumbers>(s_verifiedRandomNumbers);
    }

    private synchronized static void addVerifiedRandomNumber(VerifiedRandomNumbers number)
    {
        s_verifiedRandomNumbers.add(number);
    }
    
    
    private final PlainRandomSource m_plainRandom = new PlainRandomSource();
    private final IGame m_game;
    
    //remembered from generate to unlock
    private VaultID m_remoteVaultID;
    private String m_annotation;
    private int m_max;
    //have we recieved a generate request, but not a unlock request
    private boolean m_waitingForUnlock;
    private int[] m_localNumbers;
    
    
    /**
     * @param id
     */
    public RemoteRandom(IGame game)
    {
        m_game = game;
    }

    /* 
     * @see games.strategy.engine.random.IRemoteRandom#generate(int, int, java.lang.String)
     */
    public int[] generate(int max, int count, String annotation, VaultID remoteVaultID)
    {
        if(m_waitingForUnlock)
            throw new IllegalStateException("Being asked to generate random numbers, but we havent finished last generation");
        m_waitingForUnlock = true;
        
        //clean up here, we know these keys arent needed anymore so release them
        //we cant do this earlier without synchronizing between the server and the client
        //but here we know they arent needed anymore
        if(m_remoteVaultID != null)
        {
            m_game.getVault().release(m_remoteVaultID);
        }
        
        m_remoteVaultID = remoteVaultID;
        m_annotation = annotation;
        m_max = max;
        
        m_localNumbers = m_plainRandom.getRandom(max, count, annotation);
       
        m_game.getVault().waitForID(remoteVaultID, 15000);
        if(!m_game.getVault().knowsAbout(remoteVaultID))
            throw new IllegalStateException("Vault id not known, have:" + m_game.getVault().knownIds() + " looking for:" + remoteVaultID);
        
        return m_localNumbers;
    }

    /* 
     * @see games.strategy.engine.random.IRemoteRandom#unlock()
     */
    public void verifyNumbers()
    {
        Vault vault = m_game.getVault();
       
        vault.waitForIdToUnlock(m_remoteVaultID, 15000);
        
        if(!vault.isUnlocked(m_remoteVaultID))
            throw new IllegalStateException("Server did not unlock random numbers, cheating is suspected");
        
        int[] remoteNumbers;
        try
        {
            remoteNumbers = CryptoRandomSource.bytesToInts(vault.get(m_remoteVaultID));
        } catch (NotUnlockedException e1)
        {
            e1.printStackTrace();
            throw new IllegalStateException("Could not unlock numbers, cheating suspected");
        }
        int[] verifiedNumbers = CryptoRandomSource.xor(remoteNumbers, m_localNumbers, m_max);
        
        addVerifiedRandomNumber(new VerifiedRandomNumbers(m_annotation, verifiedNumbers));
        m_waitingForUnlock = false;
        
    }

}
