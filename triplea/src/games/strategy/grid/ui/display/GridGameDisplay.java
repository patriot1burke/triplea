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
package games.strategy.grid.ui.display;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.grid.ui.IGridPlayData;

import java.util.Collection;

/**
 * Display for a Grid Game.
 * 
 * @author Lane Schwartz (original) and Veqryn (abstraction)
 * @version $LastChangedDate: 2011-11-22 18:21:37 +0800 (Tue, 22 Nov 2011) $
 */
public class GridGameDisplay implements IGridGameDisplay
{
	@SuppressWarnings("unused")
	private IDisplayBridge m_displayBridge;
	private final GridGameFrame m_ui;
	
	/**
	 * Construct a new display for a King's Table game.
	 * 
	 * The display
	 * 
	 * @param ui
	 * @see games.strategy.engine.display.IDisplay
	 */
	public GridGameDisplay(final GridGameFrame ui)
	{
		m_ui = ui;
	}
	
	public GridGameFrame getGridGameFrame()
	{
		return m_ui;
	}
	
	/**
	 * @see games.strategy.engine.display.IDisplay#initialize(games.strategy.engine.display.IDisplayBridge)
	 */
	public void initialize(final IDisplayBridge bridge)
	{
		m_displayBridge = bridge;
		// m_displayBridge.toString();
	}
	
	/**
	 * Process a user request to exit the program.
	 * 
	 * @see games.strategy.engine.display.IDisplay#shutdown()
	 */
	public void shutDown()
	{
		m_ui.stopGame();
	}
	
	/**
	 * Graphically notify the user of the current game status.
	 * 
	 * @param error
	 *            the status message to display
	 */
	public void setStatus(final String status)
	{
		m_ui.setStatus(status);
	}
	
	/**
	 * Set the game over status for this display to <code>true</code>.
	 */
	public void setGameOver()// (CountDownLatch waiting)
	{
		m_ui.setGameOver();// waiting);
	}
	
	/**
	 * Ask the user interface for this display to process a play and zero or more captures.
	 * 
	 * @param territories
	 *            <code>Collection</code> of <code>Territory</code>s whose pieces have changed
	 */
	public void refreshTerritories(final Collection<Territory> territories)
	{
		m_ui.refreshTerritories(territories);
	}
	
	public void showGridPlayDataMove(final IGridPlayData move)
	{
		m_ui.showGridPlayDataMove(move);
	}
	
	public void showGridEndTurnData(final IGridEndTurnData endTurnData)
	{
		m_ui.showGridEndTurnData(endTurnData);
	}
	
	public void initializeGridMapData(final GameMap map)
	{
		m_ui.initializeGridMapData(map);
	}
}
