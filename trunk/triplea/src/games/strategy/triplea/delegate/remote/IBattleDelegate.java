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
package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.BattleListing;

/**
 * @author Sean Bridges
 */
public interface IBattleDelegate extends IRemote, IDelegate
{
	/**
	 * 
	 * @return the battles currently waiting to be fought
	 */
	public BattleListing getBattles();
	
	/**
	 * Fight the battle in the given country
	 * 
	 * @param where
	 *            - where to fight
	 * @param bombing
	 *            - fight a bombing raid
	 * @return an error string if the battle could not be fought or an error occurred, null otherwise
	 */
	public String fightBattle(Territory where, boolean bombing, BattleType type);
	
	/**
	 * Finish the current battle
	 * 
	 * @return an error string if the battle could not be fought or an error occurred, null otherwise
	 */
	public String fightCurrentBattle();
	
	/**
	 * 
	 * @return The location of the currently being fought battle, or null if no battle is in progress.
	 */
	public Territory getCurrentBattleTerritory();
	
	/**
	 * 
	 * @return The current battle if there is one, or null if there is no current battle in progress.
	 */
	public IBattle getCurrentBattle();
}
