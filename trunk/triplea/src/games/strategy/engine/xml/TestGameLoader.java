/*
 * TestGameLoader.java
 * 
 * Created on January 29, 2002, 12:38 PM
 */
package games.strategy.engine.xml;

import games.strategy.engine.data.DefaultUnitFactory;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;

import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 */
public class TestGameLoader implements IGameLoader
{
	private static final long serialVersionUID = -8019996788216172034L;
	
	/**
	 * Return an array of player types that can play on the server.
	 * This array must not contain any entries that could play on the client.
	 */
	public String[] getServerPlayerTypes()
	{
		return null;
	}
	
	/**
	 * Return an array of player types that can play on the client.
	 * This array must not contain any entries that could play on the server.
	 */
	public String[] getClientPlayerTypes()
	{
		return null;
	}
	
	/**
	 * The game is about to start.
	 */
	public void startGame(final IGame game, final Set<IGamePlayer> players, final boolean headless)
	{
	}
	
	/**
	 * Create the players. Given a map of playerName -> type,
	 * where type is one of the Strings returned by a get*PlayerType() method.
	 */
	public Set<IGamePlayer> createPlayers(final Map<String, String> players)
	{
		return null;
	}
	
	/*
	 * @see games.strategy.engine.framework.IGameLoader#getDisplayType()
	 */
	public Class<? extends IChannelSubscribor> getDisplayType()
	{
		return IChannelSubscribor.class;
	}
	
	public Class<? extends IChannelSubscribor> getSoundType()
	{
		return IChannelSubscribor.class;
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.engine.framework.IGameLoader#getRemotePlayerType()
	 */
	public Class<? extends IRemote> getRemotePlayerType()
	{
		return IRemote.class;
	}
	
	public void shutDown()
	{
	}
	
	public IUnitFactory getUnitFactory()
	{
		return new DefaultUnitFactory();
	}
}
