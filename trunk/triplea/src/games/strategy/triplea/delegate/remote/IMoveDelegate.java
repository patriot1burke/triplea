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

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

import java.util.Collection;
import java.util.Map;

/**
 * Remote interface for MoveDelegate
 * 
 * @author Sean Bridges
 */
public interface IMoveDelegate extends IAbstractMoveDelegate, IAbstractForumPosterDelegate
{
	/**
	 * 
	 * @param units
	 *            - the units to move
	 * @param route
	 *            - the route to move along
	 * @param m_transportsThatCanBeLoaded
	 *            - transports that can be loaded while moving, must be non null
	 * @return an error message if the move can't be made, null otherwise
	 */
	public String move(Collection<Unit> units, Route route, Collection<Unit> m_transportsThatCanBeLoaded);
	
	/**
	 * 
	 * @param units
	 *            - the units to move
	 * @param route
	 *            - the route to move along
	 * @param m_transportsThatCanBeLoaded
	 *            - transports that can be loaded while moving, must be non null
	 * @param newDependents
	 *            - units that will be made into new dependents if this move is successful, must be non null
	 * @return an error message if the move can't be made, null otherwise
	 */
	public String move(Collection<Unit> units, Route route, Collection<Unit> m_transportsThatCanBeLoaded, Map<Unit, Collection<Unit>> newDependents);
	
	/**
	 * equivalent to move(units, route, Collections.EMPTY_LIST)
	 * 
	 * @param units
	 *            - the units to move
	 * @param route
	 *            - the route to move along
	 * @return an error message if the move cant be made, null otherwise
	 */
	public String move(Collection<Unit> units, Route route);
	
	/**
	 * Get what air units must move before the end of the players turn
	 * 
	 * @param player
	 *            referring player ID
	 * @return a list of territories with air units that must move of player ID
	 */
	public Collection<Territory> getTerritoriesWhereAirCantLand(PlayerID player);
	
	public Collection<Territory> getTerritoriesWhereAirCantLand();
	
	/**
	 * Get what units must have combat ability
	 * 
	 * @return a list of Territories with units that can't fight
	 */
	public Collection<Territory> getTerritoriesWhereUnitsCantFight();
}
