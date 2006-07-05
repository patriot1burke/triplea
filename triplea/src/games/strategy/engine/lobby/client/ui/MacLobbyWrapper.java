package games.strategy.engine.lobby.client.ui;


public class MacLobbyWrapper
{
    
    //keep this in its own class, otherwise we get a no class def error when 
    //we try to load the game and the stubs arent in the classpath
    //i think the java validator triggers this
    public static void registerMacShutdownHandler(final LobbyFrame frame)
    {
        com.apple.mrj.MRJApplicationUtils.registerQuitHandler(new com.apple.mrj.MRJQuitHandler()
            {
               public void handleQuit()
               {
                   frame.shutdown();
               }
            });
    }


}
