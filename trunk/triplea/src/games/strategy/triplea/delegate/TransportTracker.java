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
/*
 * TransportTracker.java
 * 
 * Created on November 21, 2001, 3:51 PM
 * 
 * @version $LastChangedDate$
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          Tracks which transports are carrying which units. Also tracks the capacity
 *          that has been unloaded. To reset the unloaded call clearUnloadedCapacity().
 */
public class TransportTracker
{
	public static int getCost(final Collection<Unit> units)
	{
		return MoveValidator.getTransportCost(units);
	}
	
	private static void assertTransport(final Unit u)
	{
		if (UnitAttachment.get(u.getType()).getTransportCapacity() == -1)
		{
			throw new IllegalStateException("Not a transport:" + u);
		}
	}
	
	/**
	 * Constructor.
	 */
	private TransportTracker()
	{
	}
	
	/**
	 * Returns the collection of units that the given transport is transporting.
	 * Could be null.
	 */
	public static Collection<Unit> transporting(final Unit transport)
	{
		return new ArrayList<Unit>(((TripleAUnit) transport).getTransporting());
	}
	
	/**
	 * Returns the collection of units that the given transport is transporting.
	 * Could be null.
	 */
	public static Collection<Unit> transporting(final Unit transport, final Collection<Unit> transportedUnitsPossible)
	{
		return new ArrayList<Unit>(((TripleAUnit) transport).getTransporting(transportedUnitsPossible));
	}
	
	public static boolean isTransporting(final Unit transport)
	{
		return !((TripleAUnit) transport).getTransporting().isEmpty();
	}
	
	/**
	 * Returns the collection of units that the given transport has unloaded
	 * this turn. Could be empty.
	 */
	public static Collection<Unit> unloaded(final Unit transport)
	{
		return ((TripleAUnit) transport).getUnloaded();
	}
	
	public static Collection<Unit> transportingAndUnloaded(final Unit transport)
	{
		Collection<Unit> rVal = transporting(transport);
		if (rVal == null)
			rVal = new ArrayList<Unit>();
		rVal.addAll(unloaded(transport));
		return rVal;
	}
	
	/**
	 * Returns a map of transport -> collection of transported units.
	 */
	public static Map<Unit, Collection<Unit>> transporting(final Collection<Unit> units)
	{
		final Map<Unit, Collection<Unit>> returnVal = new HashMap<Unit, Collection<Unit>>();
		for (final Unit transported : units)
		{
			final Unit transport = transportedBy(transported);
			Collection<Unit> transporting = null;
			if (transport != null)
				transporting = transporting(transport);
			if (transporting != null)
			{
				returnVal.put(transport, transporting);
			}
		}
		return returnVal;
	}
	
	/**
	 * Returns a map of transport -> collection of transported units.
	 */
	public static Map<Unit, Collection<Unit>> transporting(final Collection<Unit> transports, final Collection<Unit> transportedUnits)
	{
		final Map<Unit, Collection<Unit>> returnVal = new HashMap<Unit, Collection<Unit>>();
		for (final Unit transported : transportedUnits)
		{
			final Unit transport = transportedBy(transported);
			Collection<Unit> transporting = null;
			if (transport != null)
				transporting = transporting(transport, transportedUnits);
			if (transporting != null)
			{
				returnVal.put(transport, transporting);
			}
		}
		return returnVal;
	}
	
	/**
	 * Return the transport that holds the given unit. Could be null.
	 */
	public static Unit transportedBy(final Unit unit)
	{
		return ((TripleAUnit) unit).getTransportedBy();
	}
	
	public static Change unloadTransportChange(final TripleAUnit unit, final Territory territory, final PlayerID id, final boolean dependentBattle)
	{
		final CompositeChange change = new CompositeChange();
		final TripleAUnit transport = (TripleAUnit) transportedBy(unit);
		if (transport == null)
			return change;
		assertTransport(transport);
		if (!transport.getTransporting().contains(unit))
		{
			throw new IllegalStateException("Not being carried, unit:" + unit + " transport:" + transport);
		}
		final ArrayList<Unit> newUnloaded = new ArrayList<Unit>(transport.getUnloaded());
		newUnloaded.add(unit);
		change.add(ChangeFactory.unitPropertyChange(unit, territory, TripleAUnit.UNLOADED_TO));
		if (!GameStepPropertiesHelper.isNonCombatMove(unit.getData(), true))
		{
			change.add(ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.UNLOADED_IN_COMBAT_PHASE));
			change.add(ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.UNLOADED_AMPHIBIOUS));
			change.add(ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.UNLOADED_IN_COMBAT_PHASE));
			change.add(ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.UNLOADED_AMPHIBIOUS));
		}
		if (!dependentBattle)
		{
			// TODO: this is causing issues with Scrambling. if the units were unloaded, then scrambling creates a battle, there is no longer any way to have the units removed if those transports die.
			change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY));
		}
		change.add(ChangeFactory.unitPropertyChange(transport, newUnloaded, TripleAUnit.UNLOADED));
		return change;
	}
	
	public static Change unloadAirTransportChange(final TripleAUnit unit, final Territory territory, final PlayerID id, final boolean dependentBattle)
	{
		final CompositeChange change = new CompositeChange();
		final TripleAUnit transport = (TripleAUnit) transportedBy(unit);
		if (transport == null)
			return change;
		assertTransport(transport);
		if (!transport.getTransporting().contains(unit))
		{
			throw new IllegalStateException("Not being carried, unit:" + unit + " transport:" + transport);
		}
		final ArrayList<Unit> newUnloaded = new ArrayList<Unit>(transport.getUnloaded());
		newUnloaded.add(unit);
		change.add(ChangeFactory.unitPropertyChange(unit, territory, TripleAUnit.UNLOADED_TO));
		if (!GameStepPropertiesHelper.isNonCombatMove(unit.getData(), true))
		{
			change.add(ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.UNLOADED_IN_COMBAT_PHASE));
			// change.add(ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.UNLOADED_AMPHIBIOUS));
			change.add(ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.UNLOADED_IN_COMBAT_PHASE));
			// change.add(ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.UNLOADED_AMPHIBIOUS));
		}
		if (!dependentBattle)
		{
			// TODO: this is causing issues with Scrambling. if the units were unloaded, then scrambling creates a battle, there is no longer any way to have the units removed if those transports die.
			change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY));
		}
		// dependencies for battle calc and casualty selection include unloaded. therefore even if we have unloaded this unit, it will die if air transport dies IF we have the unloaded flat set. so don't set it.
		// TODO: fix this bullshit by re-writing entire transportation engine
		// change.add(ChangeFactory.unitPropertyChange(transport, newUnloaded, TripleAUnit.UNLOADED));
		return change;
	}
	
	public static Change loadTransportChange(final TripleAUnit transport, final Unit unit)
	{
		assertTransport(transport);
		final CompositeChange change = new CompositeChange();
		// clear the loaded by
		change.add(ChangeFactory.unitPropertyChange(unit, transport, TripleAUnit.TRANSPORTED_BY));
		final Collection<Unit> newCarrying = new ArrayList<Unit>(transport.getTransporting());
		if (newCarrying.contains(unit))
		{
			throw new IllegalStateException("Already carrying, transport:" + transport + " unt:" + unit);
		}
		newCarrying.add(unit);
		change.add(ChangeFactory.unitPropertyChange(unit, Boolean.TRUE, TripleAUnit.LOADED_THIS_TURN));
		change.add(ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.LOADED_THIS_TURN));
		// If the transport was in combat, flag it as being loaded AFTER combat
		if (transport.getWasInCombat())
		{
			change.add(ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.LOADED_AFTER_COMBAT));
		}
		return change;
	}
	
	public static Change combatTransportChange(final TripleAUnit transport, final PlayerID id)
	{
		assertTransport(transport);
		final CompositeChange change = new CompositeChange();
		change.add(ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.WAS_IN_COMBAT));
		return change;
	}
	
	public static int getAvailableCapacity(final Unit unit)
	{
		final UnitAttachment ua = UnitAttachment.get(unit.getType());
		// Check if there are transports available, also check for destroyer capacity (Tokyo Express)
		if (ua.getTransportCapacity() == -1 || (unit.getData().getProperties().get(Constants.PACIFIC_THEATER, false) && ua.getIsDestroyer() && !unit.getOwner().getName().equals("Japanese")))
			return 0;
		final int capacity = ua.getTransportCapacity();
		final int used = getCost(transporting(unit));
		final int unloaded = getCost(unloaded(unit));
		return capacity - used - unloaded;
	}
	
	public static Collection<Unit> getUnitsLoadedOnAlliedTransportsThisTurn(final Collection<Unit> units)
	{
		final Collection<Unit> rVal = new ArrayList<Unit>();
		for (final Unit u : units)
		{
			// a unit loaded onto an allied transport
			// cannot be unloaded in the same turn, so
			// if we check both wasLoadedThisTurn and
			// the transport that transports us, we can tell if
			// we were loaded onto an allied transport
			// if we are no longer being transported,
			// then we must have been transported on our own transport
			final TripleAUnit taUnit = (TripleAUnit) u;
			if (taUnit.getWasLoadedThisTurn() && taUnit.getTransportedBy() != null &&
						// an allied transport if the owner of the transport is not the owner of the unit
						!taUnit.getTransportedBy().getOwner().equals(taUnit.getOwner()))
			{
				rVal.add(u);
			}
		}
		return rVal;
	}
	
	public static boolean hasTransportUnloadedInPreviousPhase(final Unit transport)
	{
		final Collection<Unit> unloaded = ((TripleAUnit) transport).getUnloaded();
		// See if transport has unloaded anywhere yet
		for (final Unit u : unloaded)
		{
			final TripleAUnit taUnit = (TripleAUnit) u;
			// cannot unload in two different phases
			if (GameStepPropertiesHelper.isNonCombatMove(transport.getData(), true) && taUnit.getWasUnloadedInCombatPhase())
				return true;
		}
		return false;
	}
	
	private static boolean isWW2V2(final GameData data)
	{
		return games.strategy.triplea.Properties.getWW2V2(data);
	}
	
	// TODO here's a bug COMCO
	private static boolean isTransportUnloadRestricted(final GameData data)
	{
		return games.strategy.triplea.Properties.getTransportUnloadRestricted(data);
	}
	
	// In some versions, a transport can never unload into
	// multiple territories in a given turn.
	// In WW2V1 a transport can unload to multiple territories in
	// non-combat phase, provided they are both adjacent to the sea zone.
	public static boolean isTransportUnloadRestrictedToAnotherTerritory(final Unit transport, final Territory territory)
	{
		final Collection<Unit> unloaded = ((TripleAUnit) transport).getUnloaded();
		if (unloaded.isEmpty())
			return false;
		// See if transport has unloaded anywhere yet
		final GameData data = transport.getData();
		for (final Unit u : unloaded)
		{
			final TripleAUnit taUnit = (TripleAUnit) u;
			if (isWW2V2(data) || isTransportUnloadRestricted(data))
			{
				// cannot unload to two different territories
				if (!taUnit.getUnloadedTo().equals(territory))
					return true;
			}
			else
			{
				// cannot unload to two different territories in combat phase
				if (!GameStepPropertiesHelper.isNonCombatMove(transport.getData(), true) && !taUnit.getUnloadedTo().equals(territory))
					return true;
			}
		}
		return false;
	}
	
	// This method should be called after isTransportUnloadRestrictedToAnotherTerritory()
	// returns false, in order to populate the error message.
	// However, we only need to call this method to determine why we can't
	// unload an additional unit. Since transports only hold up to two units,
	// we only need to return one territory, not multiple territories.
	public static Territory getTerritoryTransportHasUnloadedTo(final Unit transport)
	{
		final Collection<Unit> unloaded = ((TripleAUnit) transport).getUnloaded();
		if (unloaded.isEmpty())
			return null;
		final Iterator<Unit> iter = unloaded.iterator();
		return ((TripleAUnit) iter.next()).getUnloadedTo();
	}
	
	// If a transport has been in combat, it cannot load AND unload in non-combat
	public static boolean isTransportUnloadRestrictedInNonCombat(final Unit transport)
	{
		final TripleAUnit taUnit = (TripleAUnit) transport;
		if (GameStepPropertiesHelper.isNonCombatMove(transport.getData(), true) && taUnit.getWasInCombat() && taUnit.getWasLoadedAfterCombat())
			return true;
		return false;
	}
}
