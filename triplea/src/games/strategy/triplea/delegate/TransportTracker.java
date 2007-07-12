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

/*
 * TransportTracker.java
 *
 * Created on November 21, 2001, 3:51 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;

import java.util.*;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * Tracks which transports are carrying which units. Also tracks the capacity
 * that has been unloaded. To reset the unloaded call clearUnloadedCapacity().
 */
public class TransportTracker implements java.io.Serializable
{
    // compatible with 0.9.0.2 saved games
    private static final long serialVersionUID = -8724881650087210929L;

    public static int getCost(Collection units)
    {
        return MoveValidator.getTransportCost(units);
    }
    
    private static void assertTransport(Unit u)
    {
        if(UnitAttachment.get(u.getType()).getTransportCapacity() == -1) 
        {
            throw new IllegalStateException("Not a transport:" + u);
        }
    }

    /**
     * Returns the collection of units that the given transport is transporting.
     * Could be null.
     */
    public Collection<Unit> transporting(Unit transport)
    {
        return new ArrayList<Unit>(((TripleAUnit) transport).getTransporting());
    }

    public boolean isTransporting(Unit transport)
    {
        return !((TripleAUnit) transport).getTransporting().isEmpty();
    }

    /**
     * Returns the collection of units that the given transport has unloaded
     * this turn. Could be empty.
     */
    public Collection<Unit> unloaded(Unit transport)
    {
        return ((TripleAUnit) transport).getUnloaded();
    }

    public Collection<Unit> transportingAndUnloaded(Unit transport)
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
    public Map<Unit, Collection<Unit>> transporting(Collection<Unit> units)
    {
        Map<Unit, Collection<Unit>> returnVal = new HashMap<Unit, Collection<Unit>>();

        for(Unit transported : units)
        {
            Unit transport = transportedBy(transported);
            
            Collection<Unit> transporting = null;
            if(transport != null) 
                transporting = transporting((TripleAUnit) transport);
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
    public Unit transportedBy(Unit unit)
    {
        return ((TripleAUnit) unit).getTransportedBy();
    }    

    
    
    
//    
//    
//    
//    
//    
//    /**
//     * Undo the unload
//     */
//    public void undoUnload(Unit unit, Unit transport, PlayerID id)
//    {
//        loadTransport(transport, unit, id);
//        Collection unload = m_unloaded.get(transport);
//        unload.remove(unit);
//    }
//
    public void unload(Unit unit, UndoableMove undoableMove)
    {
        
        TripleAUnit transport = (TripleAUnit) transportedBy(unit);
        assertTransport(transport);
        if(!transport.getTransporting().contains(unit)) 
        {
            throw new IllegalStateException("Not being carried, unit:" + unit + " transport:" + transport);
        }
        
        CompositeChange change = new CompositeChange();
        //clear the loaded by
        change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY ) );
        ArrayList<Unit> newUnloaded = new ArrayList<Unit>(transport.getUnloaded());
        newUnloaded.add(unit);
        
        
        Collection<Unit> newCarrying;
        if(transport.getTransporting().size() == 1) 
        {
            newCarrying = Collections.emptyList();
        } else
        {
            newCarrying = new ArrayList<Unit>(transport.getTransporting());
            newCarrying.remove(unit);
        }
        change.add(ChangeFactory.unitPropertyChange(transport, newCarrying, TripleAUnit.TRANSPORTING ) );
        change.add(ChangeFactory.unitPropertyChange(transport, newUnloaded, TripleAUnit.UNLOADED));
        
        undoableMove.unload(unit, transport);
        undoableMove.addChange(change);
    }
    
  

    public Change loadTransportChange(TripleAUnit transport, Unit unit, PlayerID id)
    {
        assertTransport(transport);   
        CompositeChange change = new CompositeChange();
        //clear the loaded by
        change.add(ChangeFactory.unitPropertyChange(unit, transport, TripleAUnit.TRANSPORTED_BY ) );
        
        
        Collection<Unit> newCarrying = new ArrayList<Unit>(transport.getTransporting());
        if(newCarrying.contains(unit)) 
        {
            throw new IllegalStateException("Already carrying, transport:" + transport + " unt:" + unit);
        }
        newCarrying.add(unit);
        
        change.add(ChangeFactory.unitPropertyChange(transport, newCarrying, TripleAUnit.TRANSPORTING ) );
        
        change.add(ChangeFactory.unitPropertyChange(unit, Boolean.TRUE, TripleAUnit.LOADED_THIS_TURN  ));
        
        return change;
    }
    
    public int getAvailableCapacity(Unit unit)
    {
        UnitAttachment ua = UnitAttachment.get(unit.getType());
        if (ua.getTransportCapacity() == -1)
            return 0;
        int capacity = ua.getTransportCapacity();
        int used = getCost(transporting(unit));
        int unloaded = getCost(unloaded(unit));
        return capacity - used - unloaded;
    }
    

    public Change endOfRoundClearStateChange(GameData data)
    {
        CompositeChange change = new CompositeChange();
        for(Unit unit : data.getUnits().getUnits()) 
        {
            TripleAUnit taUnit = (TripleAUnit) unit;
            if(!taUnit.getUnloaded().isEmpty()) 
            {
                change.add(ChangeFactory.unitPropertyChange(unit, Collections.EMPTY_LIST, TripleAUnit.UNLOADED));
            }
            if(taUnit.getWasLoadedThisTurn()) 
            {
                change.add(ChangeFactory.unitPropertyChange(unit, Boolean.FALSE, TripleAUnit.LOADED_THIS_TURN));
            }
                
        }
        return change;
    }

    
    public Collection<Unit> getUnitsLoadedOnAlliedTransportsThisTurn(Collection<Unit> units)
    {
       
        
        Collection<Unit> rVal = new ArrayList<Unit>();
        
        for(Unit  u : units)
        {
            //a unit loaded onto an allied transport
            //canot be unloaded in the same turn, so
            //if we check both wasLoadedThisTurn and 
            //the transport that transports us, we can tell if
            //we were loaded onto an allied transport
            //if we are no longer being transported,
            //then we must have been transported on our own transport
            TripleAUnit taUnit = (TripleAUnit) u;
            if(taUnit.getWasLoadedThisTurn() &&
                taUnit.getTransportedBy() != null &&
               //an allied transport if the owner of the transport is not the owner of the unit
              !taUnit.getTransportedBy().getOwner().equals(taUnit.getOwner())
            )
            {
                rVal.add(u);
            }
        }
        
        return rVal;
    }

}
