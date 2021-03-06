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
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.util.IntegerMap;
import games.strategy.util.Triple;

import java.util.Collection;

/**
 * Remote interface for EditDelegate
 * 
 * @author Tony Clayton
 */
public interface IEditDelegate extends IRemote, IPersistentDelegate
{
	public boolean getEditMode();
	
	public String setEditMode(boolean editMode);
	
	public String removeUnits(Territory t, Collection<Unit> units);
	
	public String addUnits(Territory t, Collection<Unit> units);
	
	public String changeTerritoryOwner(Territory t, PlayerID player);
	
	public String changePUs(PlayerID player, int PUs);
	
	public String changeTechTokens(PlayerID player, int tokens);
	
	public String addTechAdvance(PlayerID player, Collection<TechAdvance> advance);
	
	public String removeTechAdvance(PlayerID player, Collection<TechAdvance> advance);
	
	public String changeUnitHitDamage(final IntegerMap<Unit> unitDamageMap, final Territory territory);
	
	public String changeUnitBombingDamage(final IntegerMap<Unit> unitDamageMap, final Territory territory);
	
	public String addComment(String message);
	
	public String changePoliticalRelationships(Collection<Triple<PlayerID, PlayerID, RelationshipType>> relationshipChanges);
}
