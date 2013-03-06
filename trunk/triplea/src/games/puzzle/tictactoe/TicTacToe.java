/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.puzzle.tictactoe;

import games.puzzle.tictactoe.player.BetterAI;
import games.puzzle.tictactoe.ui.TicTacToeMapPanel;
import games.puzzle.tictactoe.ui.TicTacToeMenu;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.grid.GridGame;
import games.strategy.grid.player.GridGamePlayer;
import games.strategy.grid.player.RandomAI;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridGameMenu;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Main class responsible for a Tic Tac Toe game.
 * 
 * @author Lane Schwartz (original) and Veqryn (abstraction)
 * @version $LastChangedDate$
 */
public class TicTacToe extends GridGame implements IGameLoader
{
	private static final long serialVersionUID = 6817825634310618978L;
	private static final String HUMAN_PLAYER_TYPE = "Human";
	private static final String RANDOM_COMPUTER_PLAYER_TYPE = "Random AI";
	private static final String MINIMAX_COMPUTER_PLAYER_TYPE = "Minimax AI";
	private static final String ALPHABETA_COMPUTER_PLAYER_TYPE = "\u03B1\u03B2 AI";// "αβ AI";
	
	/**
	 * Return an array of player types that can play on the server.
	 */
	@Override
	public String[] getServerPlayerTypes()
	{
		return new String[] { HUMAN_PLAYER_TYPE, ALPHABETA_COMPUTER_PLAYER_TYPE, MINIMAX_COMPUTER_PLAYER_TYPE, RANDOM_COMPUTER_PLAYER_TYPE };
	}
	
	/**
	 * @see IGameLoader.createPlayers(playerNames)
	 */
	@Override
	public Set<IGamePlayer> createPlayers(final Map<String, String> playerNames)
	{
		final Set<IGamePlayer> players = new HashSet<IGamePlayer>();
		final Iterator<String> iter = playerNames.keySet().iterator();
		while (iter.hasNext())
		{
			final String name = iter.next();
			final String type = playerNames.get(name);
			if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE))
			{
				final GridGamePlayer player = new GridGamePlayer(name, type);
				players.add(player);
			}
			else if (type.equals(RANDOM_COMPUTER_PLAYER_TYPE))
			{
				final RandomAI ai = new RandomAI(name, type);
				players.add(ai);
			}
			else if (type.equals(MINIMAX_COMPUTER_PLAYER_TYPE))
			{
				final BetterAI ai = new BetterAI(name, type, BetterAI.Algorithm.MINIMAX);
				players.add(ai);
			}
			else if (type.equals(ALPHABETA_COMPUTER_PLAYER_TYPE))
			{
				final BetterAI ai = new BetterAI(name, type, BetterAI.Algorithm.ALPHABETA);
				players.add(ai);
			}
			else
			{
				throw new IllegalStateException("Player type not recognized:" + type);
			}
		}
		return players;
	}
	
	@Override
	protected Class<? extends GridMapPanel> getGridMapPanelClass()
	{
		return TicTacToeMapPanel.class;
	}
	
	@Override
	protected Class<? extends GridMapData> getGridMapDataClass()
	{
		return GridMapData.class;
	}
	
	@Override
	protected Class<? extends GridGameMenu<GridGameFrame>> getGridTableMenuClass()
	{
		return TicTacToeMenu.class;
	}
}
