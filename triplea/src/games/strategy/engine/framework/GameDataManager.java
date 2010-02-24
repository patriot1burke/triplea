package games.strategy.engine.framework;

import java.io.*;
import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.util.*;
import java.util.zip.*;
import games.strategy.engine.*;

/**
 * <p>
 * Title: TripleA
 * </p>
 * <p>
 * Description: Respsible for loading saved games, new games from xml, and
 * saving games
 * </p>
 * 
 * @author Sean Bridges
 */

public class GameDataManager
{

    private final static String DELEGATE_START = "<DelegateStart>";
    private final static String DELEGATE_DATA_NEXT = "<DelegateData>";
    private final static String DELEGATE_LIST_END = "<EndDelegateList>";

    public GameDataManager()
    {
    }

    public GameData loadGame(File savedGameFile) throws IOException
    {
        InputStream input = null;
        try
        {
            input = new BufferedInputStream(new FileInputStream(savedGameFile));
            return loadGame(input);
        } finally
        {
            try
            {
                if(input != null)
                    input.close();
            } catch (Exception e)
            {
            }
        }

    }

    public GameData loadGame(InputStream input) throws IOException
    {
        return loadGame(new ObjectInputStream(new GZIPInputStream(input)));
    }

    public GameData loadGame(ObjectInputStream input) throws IOException
    {
        try
        {
            //TODO we should check the game version as well
            Version readVersion = (Version) input.readObject();
            if (!readVersion.equals(EngineVersion.VERSION))
                throw new IOException("Incompatible engine versions. We are: " + EngineVersion.VERSION + " . Trying to load game created with: " + readVersion);

            GameData data = (GameData) input.readObject();

            loadDelegates(input, data);
            data.postDeSerialize();

            return data;

        } catch (ClassNotFoundException cnfe)
        {
            throw new IOException(cnfe.getMessage());
        }

    }

    private void loadDelegates(ObjectInputStream input, GameData data) throws ClassNotFoundException, IOException
    {
        for (Object endMarker = input.readObject(); !endMarker.equals(DELEGATE_LIST_END); endMarker = input.readObject())
        {
            String name = (String) input.readObject();
            String displayName = (String) input.readObject();
            String className = (String) input.readObject();

            IDelegate instance;
            try
            {
                instance = (IDelegate) Class.forName(className).newInstance();
                instance.initialize(name, displayName);
                data.getDelegateList().addDelegate(instance);
            } catch (Exception e)
            {
                e.printStackTrace();
                throw new IOException(e.getMessage());
            }

            String next = (String) input.readObject();
            if (next.equals(DELEGATE_DATA_NEXT))
            {
                instance.loadState((Serializable) input.readObject());
            }
        }
    }

    public void saveGame(File destination, GameData data) throws IOException
    {
        
        BufferedOutputStream out = null;
        try
        {
            OutputStream fileStream = new FileOutputStream(destination);
            out = new BufferedOutputStream(fileStream);
            
            saveGame(fileStream, data);
        } finally
        {
            try
            {
                if(out != null)
                    out.close();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void saveGame(OutputStream sink, GameData data) throws IOException
    {
        saveGame(sink, data, true);
    }

    public void saveGame(OutputStream sink, GameData data, boolean saveDelegateInfo) throws IOException
    {        
        //write internally first in case of error
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(25000);
        ObjectOutputStream outStream = new ObjectOutputStream(bytes);

        outStream.writeObject(games.strategy.engine.EngineVersion.VERSION);
        data.acquireReadLock();
        try
        {
            outStream.writeObject(data);
                
	        if (saveDelegateInfo)
	            writeDelegates(data, outStream);
	        else
	            outStream.writeObject(DELEGATE_LIST_END);
        }
        finally
        {
            data.releaseReadLock();
        }
        
        GZIPOutputStream zippedOut = new GZIPOutputStream(sink);
        //now write to file
        zippedOut.write(bytes.toByteArray());
        zippedOut.flush();
        zippedOut.close();
    }

    private void writeDelegates(GameData data, ObjectOutputStream out) throws IOException
    {

        Iterator iter = data.getDelegateList().iterator();
        while (iter.hasNext())
        {
            out.writeObject(DELEGATE_START);

            IDelegate delegate = (IDelegate) iter.next();

            //write out the delegate info
            out.writeObject(delegate.getName());
            out.writeObject(delegate.getDisplayName());
            out.writeObject(delegate.getClass().getName());


            out.writeObject(DELEGATE_DATA_NEXT);
            out.writeObject(delegate.saveState());
        }
        //mark end of delegate section
        out.writeObject(DELEGATE_LIST_END);
    }

}

