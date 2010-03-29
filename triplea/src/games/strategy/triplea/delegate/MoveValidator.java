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
 * MoveValidator.java
 *
 * Created on November 9, 2001, 4:05 PM
 * @version $LastChangedDate$
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 *
 * @author  Sean Bridges
 *
 * Provides some static methods for validating movement.
 */
public class MoveValidator
{

    public static final String TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE = "Transport has already unloaded units in a previous phase";
    public static final String TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO = "Transport has already unloaded units to ";
    public static final String CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND = "Cannot load and unload an allied transport in the same round";
    public static final String CANT_MOVE_THROUGH_IMPASSIBLE = "Can't move through impassible territories";
    public static final String TOO_POOR_TO_VIOLATE_NEUTRALITY = "Not enough money to pay for violating neutrality";
    public static final String CANNOT_VIOLATE_NEUTRALITY = "Cannot violate neutrality";
    public static final String NOT_ALL_AIR_UNITS_CAN_LAND = "Not all air units can land";
    public static final String TRANSPORT_CANNOT_LOAD_AND_UNLOAD_AFTER_COMBAT = "Transport cannot both load AND unload after being in combat";
    
    /**
     * Tests the given collection of units to see if they have the movement necessary
     * to move.
     * @arg alreadyMoved maps Unit -> movement
     */
    
    public static boolean hasEnoughMovement(Collection<Unit> units, Route route)
    {    

        for (Unit unit : units)
        {
            if (!hasEnoughMovement(unit, route))
                return false;
        }
        return true;
    }

    /**
     * @param route
     */
    private static int getMechanizedSupportAvail(Route route, Collection<Unit> units, PlayerID player)
    {
    	int mechanizedSupportAvailable = 0;
    	
    	if(isMechanizedInfantry(player))
    	{	
    		CompositeMatch<Unit> transportArmor = new CompositeMatchAnd<Unit>(Matches.UnitCanBlitz, Matches.unitIsOwnedBy(player));
    		mechanizedSupportAvailable = Match.countMatches(units, transportArmor);
    	}
        return mechanizedSupportAvailable;
    }

    /**
     * 
     */
    private static int getArialTransportSupportAvail(Route route, Collection<Unit> units, PlayerID player)
    {   
        int arialTransportSupportAvailable = 0;
    	
    	if(isParatroopers(player))
    	{	
    		CompositeMatch<Unit> transportBombers = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, Matches.unitIsOwnedBy(player));
    		arialTransportSupportAvailable = Match.countMatches(units, transportBombers);  
    	
    	}
        return arialTransportSupportAvailable;        
    }

    /**
     * 
     */
    public static boolean hasEnoughMovement(Collection<Unit> units, int length)
    {
        for (Unit unit : units)
        {
            if (!hasEnoughMovement(unit, length))
                return false;
        }
        return true;
    }

    /**
     * Tests the given unit to see if it has the movement necessary
     * to move.
     * @arg alreadyMoved maps Unit -> movement
     */  
    public static boolean hasEnoughMovement(Unit unit, Route route)
    {             
        int left = TripleAUnit.get(unit).getMovementLeft();  
        UnitAttachment ua = UnitAttachment.get(unit.getType());
        PlayerID player = unit.getOwner();
        
    	TerritoryAttachment taStart = null;
    	TerritoryAttachment taEnd = null;
    	
    	if(route.getStart() != null)
    	    taStart = TerritoryAttachment.get(route.getStart());
    	if(route.getEnd() != null)
    	    taEnd = TerritoryAttachment.get(route.getEnd());
    	
        if(ua.isAir())
        {
        	if (taStart != null && taStart.isAirBase())        	
            	left++;
            
            if (taEnd != null && taEnd.isAirBase())
            	left++;
        }
        
        GameStep stepName = unit.getData().getSequence().getStep();
        
        if(ua.isSea() && stepName.getDisplayName().equals("Non Combat Move"))
        {        	
        	//If a zone adjacent to the starting and ending sea zones are allied navalbases, increase the range.
        	//TODO Still need to be able to handle stops on the way (history to get route.getStart() 
            Iterator <Territory> startNeighborIter = unit.getData().getMap().getNeighbors(route.getStart(), 1).iterator();            
            while (startNeighborIter.hasNext())
            {
            	Territory terrNext = (Territory) startNeighborIter.next();
            	TerritoryAttachment taNeighbor = TerritoryAttachment.get(terrNext);
            	if (taNeighbor != null && taNeighbor.isNavalBase() && unit.getData().getAllianceTracker().isAllied(terrNext.getOwner(), player))
            	{
            		Iterator <Territory> endNeighborIter = unit.getData().getMap().getNeighbors(route.getEnd(), 1).iterator();            
                    while (endNeighborIter.hasNext())
                    {
                    	Territory terrEnd = (Territory) endNeighborIter.next();
                    	TerritoryAttachment taEndNeighbor = TerritoryAttachment.get(terrEnd);
                    	if (taEndNeighbor != null && taEndNeighbor.isNavalBase() && unit.getData().getAllianceTracker().isAllied(terrEnd.getOwner(), player))
                    	{
                    		left++;
                    		break;
                    	}
                    }
            	}
            }
        }
       
        //TODO COMCO add rule to allow non-combat paratroops
        /*if(isParatroopers(player) && ua.isStrategicBomber())
        {
            if (stepName.getDisplayName().equals("Combat Move"))
            {   
                Collection<Unit> ownedUnits = route.getStart().getUnits().getMatches(Matches.unitIsOwnedBy(player));
                
                CompositeMatch<Unit> ownedInfantry = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), 
                                Matches.UnitIsLand, Matches.UnitCanBeTransported, Matches.UnitIsNotAA, Matches.UnitIsNotArmour);
                
                if(Match.countMatches(ownedUnits, ownedInfantry)>0)
                {
                }
            }            
        }*/
            
        if(left == -1 || left < route.getLength())
            return false;
        return true;
    }

    
    public static boolean hasEnoughMovement(Unit unit, int length)
    {
        int left = TripleAUnit.get(unit).getMovementLeft(); 
        
        //Be sure to try to check for air/naval bases if only passing length
        if(left == -1 || left < length)
            return false;
        return true;
    }
        
    /**
     * Checks that there are no enemy units on the route except possibly at the end.
     * Submerged enemy units are not considered as they don't affect
     * movement.
     * AA and factory dont count as enemy.
     */
    public static boolean onlyAlliedUnitsOnPath(Route route, PlayerID player, GameData data)
    {
        CompositeMatch<Unit> alliedOrNonCombat = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrFactory, Matches.alliedUnit(player, data));

        // Submerged units do not interfere with movement
        // only relevant for WW2V2
        alliedOrNonCombat.add(Matches.unitIsSubmerged(data));
        
        for(int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);
            if(!current.getUnits().allMatch( alliedOrNonCombat))
                return false;
        }
        return true;
    }

    /**
     * Checks that there only transports, subs and/or allies on the route except at the end.
     * AA and factory dont count as enemy.
     */
    public static boolean onlyIgnoredUnitsOnPath(Route route, PlayerID player, GameData data, boolean ignoreRouteEnd)
    {
    	CompositeMatch<Unit> subOnly = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrFactory, Matches.UnitIsSub, Matches.alliedUnit(player, data));
    	CompositeMatch<Unit> transportOnly = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrFactory, Matches.UnitIsTransport, Matches.UnitIsLand, Matches.alliedUnit(player, data));
    	CompositeMatch<Unit> transportOrSubOnly = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrFactory, Matches.UnitIsTransport, Matches.UnitIsLand, Matches.UnitIsSub, Matches.alliedUnit(player, data));
    	boolean getIgnoreTransportInMovement = isIgnoreTransportInMovement(data);
    	boolean getIgnoreSubInMovement = isIgnoreSubInMovement(data);
    	int routeLength = route.getLength();
    	boolean validMove = false;
    	
    	if(ignoreRouteEnd)
    	{
    	    routeLength -= 1;
    	}
            for(int i = 0; i < routeLength; i++)
            {
                Territory current = route.at(i);
                if(current.isWater())
                {
                    if(getIgnoreTransportInMovement && getIgnoreSubInMovement && current.getUnits().allMatch(transportOrSubOnly))
                    {
                    	validMove = true;
                        continue;
                    }
                    if(getIgnoreTransportInMovement && !getIgnoreSubInMovement && current.getUnits().allMatch(transportOnly))
                    {
                    	validMove = true;
                        continue;
                    }                    
                    if(!getIgnoreTransportInMovement && getIgnoreSubInMovement && current.getUnits().allMatch(subOnly))
                    {
                    	validMove = true;
                        continue;
                    }
                    
                    validMove = false;
                }
            }
    	return validMove;
    }

    public static boolean enemyDestroyerOnPath(Route route, PlayerID player, GameData data)
    {
        Match<Unit> enemyDestroyer = new CompositeMatchAnd<Unit>(Matches.UnitIsDestroyer, Matches.enemyUnit(player, data));
        for(int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);
            if(current.getUnits().someMatch( enemyDestroyer))
                return true;
        }
        return false;
    }

    
    private static boolean getEditMode(GameData data)
    {
        return EditDelegate.getEditMode(data);
    }

    public static boolean hasConqueredNonBlitzedOnRoute(Route route, GameData data)
    {
        for(int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);
            if(MoveDelegate.getBattleTracker(data).wasConquered(current) 
                    && !MoveDelegate.getBattleTracker(data).wasBlitzed(current))
                return true;
        }
        return false;

    }


    public static boolean isBlitzable(Territory current, GameData data, PlayerID player)
    {
        if(current.isWater())
            return false;

        //cant blitz on neutrals
        if(current.getOwner().isNull() && !isNeutralsBlitzable(data))
            return false;

        if(MoveDelegate.getBattleTracker(data).wasConquered(current) 
                && !MoveDelegate.getBattleTracker(data).wasBlitzed(current))
            return false;
        
        CompositeMatch<Unit> blitzableUnits = new CompositeMatchOr<Unit>();
        blitzableUnits.add(Matches.alliedUnit(player, data));
        //WW2V2, cant blitz through factories and aa guns
        //WW2V1 you can 
        if(!isWW2V2(data) && !IsBlitzThroughFactoriesAndAARestricted(data))
        {
            blitzableUnits.add(Matches.UnitIsAAOrFactory);
        }
        
        if(!current.getUnits().allMatch(blitzableUnits))
            return false;
        
        return true;
    }
        
    private static boolean isMechanizedInfantry(PlayerID player)    
    {
        TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTATCHMENT_NAME);
        if(ta == null)
        	return false;
        return ta.hasMechanizedInfantry();
    }
    
    private static boolean isParatroopers(PlayerID player)    
    {
        TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTATCHMENT_NAME);
        if(ta == null)
        	return false;
        return ta.hasParatroopers();
    }    

    public static boolean isUnload(Route route)
    {
        if(route.getLength() == 0)
            return false;
        return route.getStart().isWater() && !route.getEnd().isWater();
    }

    public static boolean isLoad(Route route)
    {
        if(route.getLength() == 0)
            return false;
        return !route.getStart().isWater() && route.getEnd().isWater();
    }

    public static boolean hasNeutralBeforeEnd(Route route)
    {
        for(int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);
            //neutral is owned by null and is not sea
            if(!current.isWater() && current.getOwner().equals(PlayerID.NULL_PLAYERID))
                return true;
        }
        return false;
    }

    public static int getTransportCost(Collection<Unit> units)
  {
    if(units == null)
      return 0;

    int cost = 0;
    Iterator<Unit> iter = units.iterator();
    while (iter.hasNext())
    {
      Unit item = (Unit) iter.next();
      cost += UnitAttachment.get(item.getType()).getTransportCost();
    }
    return cost;
  }

     public static Collection<Unit> getUnitsThatCantGoOnWater(Collection<Unit> units)
    {
        Collection<Unit> retUnits = new ArrayList<Unit>();
        for (Unit unit : units)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if(!ua.isSea() && !ua.isAir() && ua.getTransportCost() == -1)
                retUnits.add(unit);
        }
        return retUnits;
    }

    public static boolean hasUnitsThatCantGoOnWater(Collection<Unit> units)
    {
        return !getUnitsThatCantGoOnWater(units).isEmpty();
    }


    public static int carrierCapacity(Collection<Unit> units)
    {
        int sum = 0;
        Iterator<Unit> iter = units.iterator();
        while(iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if(ua.getCarrierCapacity() != -1)
            {
                sum+=ua.getCarrierCapacity();
            }
        }
        return sum;
    }

    public static int carrierCost(Collection<Unit> units)
    {
        int sum = 0;
        Iterator<Unit> iter = units.iterator();
        while(iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if(ua.getCarrierCost() != -1)
                sum+=ua.getCarrierCost();
        }
        return sum;
    }

    public static boolean hasWater(Route route)
    {
        if(route.getStart().isWater())
            return true;

        return route.someMatch(Matches.TerritoryIsWater);
    }


    public static boolean hasLand(Route route)
    {
        if(!route.getStart().isWater())
            return true;

        for(int i = 0; i < route.getLength(); i++)
        {
            Territory t = route.at(i);
            if(! t.isWater())
                return true;
        }
        return false;
    }

    /**
     * Returns true if the given air units can land in the
     * given territory.
     * Does not take into account whether a battle has been
     * fought in the territory already.
     *
     * Note units must only be air units
     */
    public static boolean canLand(Collection<Unit> airUnits, Territory territory, PlayerID player, GameData data)
    {
        if( !Match.allMatch(airUnits, Matches.UnitIsAir))
            throw new IllegalArgumentException("can only test if air will land");


        if(!territory.isWater() 
               && MoveDelegate.getBattleTracker(data).wasConquered(territory))
            return false;

        if(territory.isWater())
        {
            //if they cant all land on carriers
            if(! Match.allMatch(airUnits, Matches.UnitCanLandOnCarrier))
                return false;

            //when doing the calculation, make sure to include the units
            //in the territory
            Set<Unit> friendly = new HashSet<Unit>();
            friendly.addAll(getFriendly(territory, player, data));
            friendly.addAll(airUnits);

            //make sure we have the carrier capacity
            int capacity = carrierCapacity(friendly);
            int cost = carrierCost(friendly);
            return  capacity >=  cost;
        }
        else
        {
            return isFriendly(player, territory.getOwner(), data);
        }
    }

    public static Collection<Unit> getNonLand(Collection<Unit> units)
    {
        CompositeMatch<Unit> match = new CompositeMatchOr<Unit>();
        match.add(Matches.UnitIsAir);
        match.add(Matches.UnitIsSea);
        return Match.getMatches(units, match);
    }

    public static Collection<Unit> getFriendly(Territory territory, PlayerID player, GameData data)
    {
        return territory.getUnits().getMatches(Matches.alliedUnit(player,data));
    }

    public static boolean isFriendly(PlayerID p1, PlayerID p2, GameData data)
    {
        if(p1.equals(p2) )
            return true;
        else return data.getAllianceTracker().isAllied(p1,p2);
    }

    public static boolean ownedByFriendly(Unit unit, PlayerID player, GameData data)
    {
        PlayerID owner = unit.getOwner();
        return(isFriendly(owner, player, data));
    }


    public static int getMaxMovement(Collection<Unit> units)
    {
        if(units.size() == 0)
            throw new IllegalArgumentException("no units");
        int max = 0;
        Iterator<Unit> iter = units.iterator();
        while(iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            int left = TripleAUnit.get(unit).getMovementLeft();
            max = Math.max(left, max);
        }
        return max;
    }

    
    public static int getLeastMovement(Collection<Unit> units)
    {
        if(units.size() == 0)
            throw new IllegalArgumentException("no units");
        int least = Integer.MAX_VALUE;
        Iterator<Unit> iter = units.iterator();
        while(iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            int left = TripleAUnit.get(unit).getMovementLeft();
            least = Math.min(left, least);
        }
        return least;
    }


    public static int getTransportCapacityFree(Territory territory, PlayerID id, GameData data, TransportTracker tracker)
    {
        Match<Unit> friendlyTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport,
                                                         Matches.alliedUnit(id, data));
        Collection<Unit> transports = territory.getUnits().getMatches(friendlyTransports);
        int sum = 0;
        Iterator<Unit> iter = transports.iterator();
        while(iter.hasNext())
        {
            Unit transport = (Unit) iter.next();
            sum += tracker.getAvailableCapacity(transport);
        }
        return sum;
    }

    public static boolean hasSomeLand(Collection<Unit> units)
    {
        Match<Unit> notAirOrSea = new CompositeMatchAnd<Unit>(Matches.UnitIsNotAir, Matches.UnitIsNotSea);
        return Match.someMatch(units, notAirOrSea);
    }

    private static boolean isWW2V2(GameData data)
    {
        return games.strategy.triplea.Properties.getWW2V2(data);
    }

    private static boolean isNeutralsImpassable(GameData data)
    {
        return games.strategy.triplea.Properties.getNeutralsImpassable(data);
    }

    private static boolean isNeutralsBlitzable(GameData data)
    {
        return games.strategy.triplea.Properties.getNeutralsBlitzable(data);
    }

    private static boolean isWW2V3(GameData data)
    {
        return games.strategy.triplea.Properties.getWW2V3(data);
    }

    private static boolean isMultipleAAPerTerritory(GameData data)
    {
        return games.strategy.triplea.Properties.getMultipleAAPerTerritory(data);
    }
    
    /**
     * @return
     */
    private static boolean isMovementByTerritoryRestricted(GameData data)
    {
    	return games.strategy.triplea.Properties.getMovementByTerritoryRestricted(data);
    }
    
    
    private static boolean IsBlitzThroughFactoriesAndAARestricted(GameData data)
    {
        return games.strategy.triplea.Properties.getBlitzThroughFactoriesAndAARestricted(data);
    }

    private static int getNeutralCharge(GameData data, Route route)
    {
        return getNeutralCharge(data, MoveDelegate.getEmptyNeutral(route).size());
    }

    private static int getNeutralCharge(GameData data, int numberOfTerritories)
    {
        return numberOfTerritories * games.strategy.triplea.Properties.getNeutralCharge(data);
    }

    public static MoveValidationResult validateMove(Collection<Unit> units, 
                                                    Route route, 
                                                    PlayerID player, 
                                                    Collection<Unit> transportsToLoad,
                                                    boolean isNonCombat, 
                                                    final List<UndoableMove> undoableMoves, 
                                                    GameData data)
    {
	    IntegerMap<Unit> movementLeft = new IntegerMap<Unit>();
	    
        MoveValidationResult result = new MoveValidationResult();

        //this should never happen
        if(new HashSet<Unit>(units).size() != units.size()) {
            result.setError("Not all units unique, units:" + units + " unique:" + new HashSet<Unit>(units));
            return result;
        }
        
        if(!data.getMap().isValidRoute(route)) 
        {
            result.setError("Invalid route:" + route);
            return result;
        }
        
        if (validateMovementRestrictedByTerritory(data, units, route, player, result).getError() != null)
        {
        	return result;
        }
        
        if (isNonCombat)
        {
            if (validateNonCombat(data, units, route, player, result).getError() != null)
                return result;
        }
        else
        {
            if (validateCombat(data, units, route, player, result).getError() != null)
                return result;
        }

        if (validateNonEnemyUnitsOnPath(data, units, route, player, result).getError() != null)
            return result;

        if (validateBasic(isNonCombat, data, units, route, player, transportsToLoad, result).getError() != null)
            return result;

        if (validateAirCanLand(data, units, route, player, result, movementLeft).getError() != null)
            return result;

        if (validateTransport(data, undoableMoves, units, route, player, transportsToLoad, result).getError() != null)
            return result;

        if (validateParatroops(isNonCombat, data, undoableMoves, units, route, player, result).getError() != null)
            return result;
        
        if (validateCanal(data, units, route, player, result).getError() != null)
            return result;


        //dont let the user move out of a battle zone
        //the exception is air units and unloading units into a battle zone
        if (MoveDelegate.getBattleTracker(data).hasPendingBattle(route.getStart(), false)
                && Match.someMatch(units, Matches.UnitIsNotAir))
        {
            //if the units did not move into the territory, then they can move out
            //this will happen if there is a submerged sub in the area, and 
            //a different unit moved into the sea zone setting up a battle
            //but the original unit can still remain
            boolean unitsStartedInTerritory = true;
            for(Unit unit : units) 
            {
                if(MoveDelegate.getRouteUsedToMoveInto(undoableMoves, unit, route.getEnd()) != null)
                {
                    unitsStartedInTerritory = false;
                    break;
                }
            }
            
            if(!unitsStartedInTerritory)
            {
            
                boolean unload = MoveValidator.isUnload(route);
                PlayerID endOwner = route.getEnd().getOwner();
                boolean attack = !data.getAllianceTracker().isAllied(endOwner, player) 
                               || MoveDelegate.getBattleTracker(data).wasConquered(route.getEnd());
                //unless they are unloading into another battle
                if (!(unload && attack))
                    return result.setErrorReturnResult("Cannot move units out of battle zone");
            }
        }

        return result;
    }

    private static MoveValidationResult validateCanal(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        if (getEditMode(data))
            return result;

        //if no sea units then we can move
        if (Match.noneMatch(units, Matches.UnitIsSea))
            return result;

        //TODO: merge validateCanal here and provide granular unit warnings
        return result.setErrorReturnResult(validateCanal(route, player, data));
    }

    private static MoveValidationResult validateCombat(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        if (getEditMode(data))
            return result;

        // Don't allow aa guns to move in combat unless they are in a transport
        if (Match.someMatch(units, Matches.UnitIsAA) && (!route.getStart().isWater() || !route.getEnd().isWater()))
        {
            for (Unit unit : Match.getMatches(units, Matches.UnitIsAA))
            	result.addDisallowedUnit("Cannot move AA guns in combat movement phase", unit);
            
            return result;
        }

        //if there is a neutral in the middle must stop unless all are air or getNeutralsBlitzable
        if (MoveValidator.hasNeutralBeforeEnd(route))
        {
        	if (!Match.allMatch(units, Matches.UnitIsAir) && !isNeutralsBlitzable(data))
        		return result.setErrorReturnResult("Must stop land units when passing through neutral territories");
        }

        if (Match.someMatch(units, Matches.UnitIsLand) && route.getLength() >= 1)
        {
        	//check all the territories but the end, if there are enemy territories, make sure they are blitzable
        	//if they are not blitzable, or we aren't all blitz units fail
        	int enemyCount = 0;
        	boolean allEnemyBlitzable = true;

        	for (int i = 0; i < route.getLength() - 1; i++)
        	{
        		Territory current = route.at(i);

        		if (current.isWater())
        			continue;

        		if (!data.getAllianceTracker().isAllied(current.getOwner(), player)
        				|| MoveDelegate.getBattleTracker(data).wasConquered(current))
        		{
        			enemyCount++;
        			allEnemyBlitzable &= MoveValidator.isBlitzable(current, data, player);
        		}
        	}

        	if (enemyCount > 0 && !allEnemyBlitzable)
        	{
        		if(nonParatroopersPresent(player, units))
        			return result.setErrorReturnResult("Cannot blitz on that route");

        	} 
        	else if (enemyCount >= 0 && allEnemyBlitzable && !(route.getStart().isWater() | route.getEnd().isWater()))
        	{
        		Match<Unit> blitzingUnit = new CompositeMatchOr<Unit>(Matches.UnitCanBlitz, Matches.UnitIsAir);
        		Match<Unit> nonBlitzing = new InverseMatch<Unit>(blitzingUnit);
        		Collection<Unit> nonBlitzingUnits = Match.getMatches(units, nonBlitzing);
        		
        		Match<Territory> territoryIsNotEnd = new InverseMatch<Territory>(Matches.territoryIs(route.getEnd()));
        		Match<Territory> nonFriendlyTerritories= new InverseMatch<Territory>(Matches.isTerritoryFriendly(player, data));
        		Match<Territory> notEndOrFriendlyTerrs = new CompositeMatchAnd<Territory>(nonFriendlyTerritories, territoryIsNotEnd);

        		Match<Territory> foughtOver = Matches.territoryWasFoughOver(MoveDelegate.getBattleTracker(data));
        		Match<Territory> notEndWasFought = new CompositeMatchAnd<Territory>(territoryIsNotEnd, foughtOver);

        		Boolean wasStartFoughtOver = MoveDelegate.getBattleTracker(data).wasConquered((Territory) route.getStart()) || MoveDelegate.getBattleTracker(data).wasBlitzed((Territory) route.getStart());
        	
        		for (Unit unit : nonBlitzingUnits)
        		{                       
        			if (Matches.UnitIsParatroop.match(unit))
        				continue;

        			if (Matches.UnitIsInfantry.match(unit))
        				continue;

        			TripleAUnit tAUnit = (TripleAUnit) unit;
        			if(wasStartFoughtOver || tAUnit.getWasInCombat() || route.someMatch(notEndOrFriendlyTerrs) || route.someMatch(notEndWasFought))
        				result.addDisallowedUnit("Not all units can blitz",unit);
        		}
        	}
        }
        else 
        {    //check aircraft
        	if (Match.someMatch(units, Matches.UnitIsAir) && route.getLength() >= 1)
        	{
        		// No neutral countries on route predicate
        		Match<Territory> noNeutral = new InverseMatch<Territory>(new CompositeMatchAnd<Territory>(Matches.TerritoryIsNeutral));
        		//ignore the end territory in our tests                    
        		Match<Territory> territoryIsEnd = Matches.territoryIs(route.getEnd());
        		//See if there are neutrals in the path    
        		if (data.getMap().getRoute(route.getStart(), route.getEnd(), new CompositeMatchOr<Territory>(noNeutral, territoryIsEnd)) == null)
        			return result.setErrorReturnResult("Air units cannot fly over neutral territories");        		
        	}
        }
        
        //make sure no conquered territories on route
        if (MoveValidator.hasConqueredNonBlitzedOnRoute(route, data))
        {
        	//unless we are all air or we are in non combat
        	if (!Match.allMatch(units, Matches.UnitIsAir))
        		return result.setErrorReturnResult("Cannot move through newly captured territories");
        }


        //See if they've already been in combat
        if(Match.someMatch(units, Matches.UnitWasInCombat)  && Match.someMatch(units, Matches.UnitWasUnloadedThisTurn)) 
        {
        	Collection<Territory> end = Collections.singleton(route.getEnd());
        	
        	if(Match.allMatch(end, Matches.isTerritoryEnemyAndNotNeutral(player, data)) && !route.getEnd().getUnits().isEmpty())
        		return result.setErrorReturnResult("Units cannot participate in multiple battles");
        }
        
        return result; 
    }

    private static MoveValidationResult validateNonCombat(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        if (getEditMode(data))
            return result;

        if (route.someMatch(Matches.TerritoryIsImpassable))
            return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSIBLE);

        CompositeMatch<Territory> battle = new CompositeMatchOr<Territory>();
        battle.add(Matches.TerritoryIsNeutral);
        battle.add(Matches.isTerritoryEnemyAndNotNeutral(player, data));

        if (battle.match(route.getEnd()))
        {
        	//If subs and transports can't control sea zones, it's OK to move there
        	if(isSubControlSeaZoneRestricted(data) && Match.allMatch(units, Matches.UnitIsSub))
        		return result;
        	else if(!isTransportControlSeaZone(data) && Match.allMatch(units, Matches.UnitIsTransport))
        		return result;
        	else
        		return result.setErrorReturnResult("Cannot advance units to battle in non combat");
        }
        
        //Subs can't travel under DDs    
        if (isSubmersibleSubsAllowed(data) && Match.allMatch(units, Matches.UnitIsSub))
        {
            //this is ok unless there are destroyer on the path
            if (MoveValidator.enemyDestroyerOnPath(route, player, data))
                return result.setErrorReturnResult("Cannot move submarines under destroyers");           
        }
             

        if (route.getEnd().getUnits().someMatch(Matches.enemyUnit(player, data)))
        {
        	if(onlyIgnoredUnitsOnPath(route, player, data, false))
        		return result;
            
            CompositeMatch<Unit> friendlyOrSubmerged = new CompositeMatchOr<Unit>();
            friendlyOrSubmerged.add(Matches.alliedUnit(player, data));
            friendlyOrSubmerged.add(Matches.unitIsSubmerged(data));
            if (!route.getEnd().getUnits().allMatch(friendlyOrSubmerged))
                return result.setErrorReturnResult("Cannot advance to battle in non combat");
        }

        if (Match.allMatch(units, Matches.UnitIsAir))
        {
            if (route.someMatch(Matches.TerritoryIsNeutral))
                return result.setErrorReturnResult("Air units cannot fly over neutral territories in non combat");
        } else
        {
            CompositeMatch<Territory> neutralOrEnemy = new CompositeMatchOr<Territory>(Matches.TerritoryIsNeutral, Matches.isTerritoryEnemyAndNotNeutral(player, data));
            if (route.someMatch(neutralOrEnemy))
                return result.setErrorReturnResult("Cannot move units to neutral or enemy territories in non combat");
        }
        return result;
    }
    
    //Added to handle restriction of movement to listed territories
    private static MoveValidationResult validateMovementRestrictedByTerritory(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
    	if (getEditMode(data))
            return result;
    	
    	if(!isMovementByTerritoryRestricted(data))
    		return result;
    	
    	RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
    	if(ra == null || ra.getMovementRestrictionTerritories() == null)
    		return result;
    	    	
    	String movementRestrictionType = ra.getMovementRestrictionType();
    	Collection<Territory> listedTerritories = ra.getListedTerritories(ra.getMovementRestrictionTerritories());
    	List<Territory> routeTerrs = route.getTerritories();
    	Iterator<Territory> iter = routeTerrs.iterator();
    	if (movementRestrictionType.equals("allowed"))
    	{	    	
	    	while (iter.hasNext())
	    	{
	    		Territory nextTerr = iter.next();
	    		if(!listedTerritories.contains(nextTerr))
	    			return result.setErrorReturnResult("Cannot move outside restricted territories");
	    	}   	
    	}
    	else if(movementRestrictionType.equals("disallowed"))
    	{	    	
	    	while (iter.hasNext())
	    	{
	    		Territory nextTerr = iter.next();
	    		if(listedTerritories.contains(nextTerr))
	    			return result.setErrorReturnResult("Cannot move to restricted territories");
	    	}   	    		
    	}
    	
    	return result;        
    }
    
    
    private static MoveValidationResult validateNonEnemyUnitsOnPath(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        if (getEditMode(data))
            return result;

        //check to see no enemy units on path
        if (MoveValidator.onlyAlliedUnitsOnPath(route, player, data))
            return result;

        //if we are all air, then its ok
        if (Match.allMatch(units, Matches.UnitIsAir))
            return result;

        if (isSubmersibleSubsAllowed(data) && Match.allMatch(units, Matches.UnitIsSub))
        {
            //this is ok unless there are destroyer on the path
            if (MoveValidator.enemyDestroyerOnPath(route, player, data))
                return result.setErrorReturnResult("Cannot move submarines under destroyers");
            else
                return result;
        }

        if (onlyIgnoredUnitsOnPath(route, player, data, true))
        	return result;
      
        //omit paratroops
        if(nonParatroopersPresent(player, units))
            return result.setErrorReturnResult("Enemy units on path");
               
        return result;
    }

    
    private static MoveValidationResult validateBasic(boolean isNonCombat, GameData data, Collection<Unit> units, Route route, PlayerID player, Collection<Unit> transportsToLoad, MoveValidationResult result)
    {
        boolean isEditMode = getEditMode(data);

        if(units.size() == 0)
            return result.setErrorReturnResult("No units");
        
        for (Unit unit : units)
        {
            if(TripleAUnit.get(unit).getSubmerged())
                result.addDisallowedUnit("Cannot move submerged units", unit);
        }

        //make sure all units are actually in the start territory
        if (!route.getStart().getUnits().containsAll(units))
            return result.setErrorReturnResult("Not enough units in starting territory");

        //make sure transports in the destination
        if (route.getEnd() != null && !route.getEnd().getUnits().containsAll(transportsToLoad) && !units.containsAll(transportsToLoad))
            return result.setErrorReturnResult("Transports not found in route end");

        if (!isEditMode)
        {
            //make sure all units are at least friendly
            for (Unit unit : Match.getMatches(units, Matches.enemyUnit(player, data)))
                result.addDisallowedUnit("Can only move friendly units", unit);

            //check we have enough movement
            //exclude transported units
            Collection<Unit> moveTest;
            if (route.getStart().isWater())
            {
                moveTest = MoveValidator.getNonLand(units);
            } else
            {
                moveTest = units;
            }
            
            for(Unit unit : Match.getMatches(moveTest, Matches.unitIsOwnedBy(player).invert())) {
            	//allow allied fighters to move with carriers
            	if(!(UnitAttachment.get(unit.getType()).getCarrierCost() > 0 &&
            		data.getAllianceTracker().isAllied(player, unit.getOwner()))) {
            		result.addDisallowedUnit("Can only move own troops", unit);
            	}
            }
            
            //Initialize available Mechanized Inf support
            int mechanizedSupportAvailable = getMechanizedSupportAvail(route, units, player);            
            int arialTransportSupportAvailable = getArialTransportSupportAvail(route, units, player);
                        
            // check units individually           
            for (Unit unit : moveTest)
            {
                if (!MoveValidator.hasEnoughMovement(unit, route))
                {
                    if(Matches.UnitIsParatroop.match(unit) && arialTransportSupportAvailable > 0)
                    {
                    	arialTransportSupportAvailable --;                     
                    }
                    else if(mechanizedSupportAvailable > 0 && TripleAUnit.get(unit).getAlreadyMoved() == 0 && (Matches.UnitIsInfantry.match(unit) | Matches.UnitIsMarine.match(unit)))
                    {
                    	mechanizedSupportAvailable --;
                    }
                    else 
                    {                    
                    	result.addDisallowedUnit("Not all units have enough movement",unit);
                    }
                }
            }

            //if there is a neutral in the middle must stop unless all are air or getNeutralsBlitzable
            if (MoveValidator.hasNeutralBeforeEnd(route))
            {
                if (!Match.allMatch(units, Matches.UnitIsAir) && !isNeutralsBlitzable(data))
                    return result.setErrorReturnResult("Must stop land units when passing through neutral territories");
            }

        } // !isEditMode

        //make sure that no non sea non transportable no carriable units
        //end at sea
        if (route.getEnd() != null && route.getEnd().isWater())
        {
            for (Unit unit : MoveValidator.getUnitsThatCantGoOnWater(units))
                result.addDisallowedUnit("Not all units can end at water",unit);
        }

        //if we are water make sure no land
        if (Match.someMatch(units, Matches.UnitIsSea))
        {
            if (MoveValidator.hasLand(route))
                for (Unit unit : Match.getMatches(units, Matches.UnitIsSea))
                    result.addDisallowedUnit("Sea units cannot go on land",unit);
        }

        //make sure that we dont send aa guns to attack
        if (Match.someMatch(units, Matches.UnitIsAA))
        {
            //TODO dont move if some were conquered

            for (int i = 0; i < route.getLength(); i++)
            {
                Territory current = route.at(i);
                if (!(current.isWater() || current.getOwner().equals(player) || data.getAllianceTracker().isAllied(player, current.getOwner())))
                {
                    for (Unit unit : Match.getMatches(units, Matches.UnitIsAA))
                        result.addDisallowedUnit("AA units cannot advance to battle", unit);

                    break;
                }
            }
        }

        //only allow one aa into a land territory unless WW2V2 or WW2V3 or isMultipleAAPerTerritory.
        //if ((!isWW2V3(data) && !isWW2V2(data)) && Match.someMatch(units, Matches.UnitIsAA) && route.getEnd() != null && route.getEnd().getUnits().someMatch(Matches.UnitIsAA)
        if ((!isMultipleAAPerTerritory(data) && !isWW2V3(data) && !isWW2V2(data)) && Match.someMatch(units, Matches.UnitIsAA) && route.getEnd() != null && route.getEnd().getUnits().someMatch(Matches.UnitIsAA)
                && !route.getEnd().isWater())
        {
            for (Unit unit : Match.getMatches(units, Matches.UnitIsAA))
                result.addDisallowedUnit("Only one AA gun allowed in a territory",unit);
        }

        //only allow 1 aa to unload unless WW2V2 or WW2V3.
        if (route.getStart().isWater() && !route.getEnd().isWater() && Match.countMatches(units, Matches.UnitIsAA) > 1 && (!isWW2V3(data) && !isWW2V2(data)))
        {
            Collection<Unit> aaGuns = Match.getMatches(units, Matches.UnitIsAA);
            Iterator<Unit> aaIter = aaGuns.iterator();
            aaIter.next(); // skip first unit
            for (; aaIter.hasNext(); )
                result.addUnresolvedUnit("Only one AA gun can unload in a territory",aaIter.next());
        }

        // don't allow move through impassible territories
        if (!isEditMode && route.someMatch(Matches.TerritoryIsImpassable))
            return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSIBLE);

        if (canCrossNeutralTerritory(data, route, player, result).getError() != null)
            return result;

        if(isNeutralsImpassable(data) && !isNeutralsBlitzable(data) && !route.getMatches(Matches.TerritoryIsNeutral).isEmpty())
            return result.setErrorReturnResult(CANNOT_VIOLATE_NEUTRALITY);
        
        return result;
    }

    private static MoveValidationResult validateAirCanLand(final GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result, IntegerMap<Unit> movementLeft)
    {
        if (getEditMode(data))
            return result;

        //nothing to check
        if (!Match.someMatch(units, Matches.UnitIsAir))
            return result;
        
        //we can land at the end, nothing left to check
        CompositeMatch<Territory> friendlyGround = alliedNonConqueredNonPendingTerritory(data, player);
        if(friendlyGround.match(route.getEnd()))
            return result;
        
        //Find all the air units we'll need to account for
        Match<Unit> ownedAirMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitOwnedBy(player) );        
        Collection<Unit> ownedAir = new ArrayList<Unit>();
        ownedAir.addAll( Match.getMatches(units, ownedAirMatch ));
        ownedAir.addAll(Match.getMatches( route.getEnd().getUnits().getUnits(), ownedAirMatch ));
               
        //Get the farthest we need to search for places to land (including current route length)
        //Generate the IntegerMap containing each aircraft's remaining movement
        int maxMovement = getAirMovementLeft(data, units, ownedAir, route, player, movementLeft);

        //Get the distances to the nearest allied land and owned factory
        int nearestFactory = getNearestFactory(data, route, player, maxMovement, friendlyGround);
        int nearestLand = getNearestLand(data, route, player, maxMovement, friendlyGround);
        
        
        
		
        //find the air units that can't make it to land
        boolean allowKamikaze = games.strategy.triplea.Properties.getKamikaze_Airplanes(data);
        //boolean allowKamikaze =  data.getProperties().get(Constants.KAMIKAZE, false);
        Collection<Unit> airThatMustLandOnCarriers = getAirThatMustLandOnCarriers(ownedAir, allowKamikaze, result, nearestLand, movementLeft);
        
        //we are done, everything can find a place to land
        if(airThatMustLandOnCarriers.isEmpty())
            return result;
        
        /*
         * Here's where we see if we have carriers available to land.
         */
        //TODO can possibly see if we're within remaining fuel from water and skip carriers if not
        //TODO should we exclude existing air units from the following? I don't think so.
        //now, find out where we can land on carriers
        IntegerMap<Integer> carrierCapacity = getInitialCarrierCapacity(data, units, route, player, maxMovement,
				airThatMustLandOnCarriers);

        
        
    	//Check to see if there are carriers to be placed
        Collection<Unit> placeUnits = player.getUnits().getUnits();
        CompositeMatch<Unit> unitIsSeaOrCanLandOnCarrier = new CompositeMatchOr<Unit>(Matches.UnitIsSea, Matches.UnitCanLandOnCarrier);
        placeUnits = Match.getMatches(placeUnits, unitIsSeaOrCanLandOnCarrier);        
        boolean lhtrCarrierProdRules = AirThatCantLandUtil.isLHTRCarrierProduction(data) || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(data);
        boolean hasProducedCarriers = player.getUnits().someMatch(Matches.UnitIsCarrier);
        if (lhtrCarrierProdRules && hasProducedCarriers)
        { 
        	if(nearestFactory-1 <= maxMovement)
        	{
        		placeUnits = Match.getMatches(placeUnits, Matches.UnitIsCarrier);
        		carrierCapacity.put(new Integer(nearestFactory-1), carrierCapacity.getInt(nearestFactory-1) + MoveValidator.carrierCapacity(placeUnits) );        		
        	}
        }        
        //Don't think we need this any more.
        /*Collection<Unit> unitsAtEnd = route.getEnd().getUnits().getMatches(Matches.alliedUnit(player, data));
        unitsAtEnd.addAll(units);*/
       /* // check carrierMustMoveWith, and reserve carrier capacity for allied planes as required
        Collection<Unit> ownedCarrier = Match.getMatches(route.getEnd().getUnits().getUnits(), 
                                                         new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player)));
        Map<Unit, Collection<Unit>> mustMoveWith = carrierMustMoveWith(ownedCarrier, route.getEnd().getUnits().getUnits(), data, player);
    
        int alliedMustMoveCost = 0;
        for (Unit unit : mustMoveWith.keySet())
        {
            Collection<Unit> mustMovePlanes = mustMoveWith.get(unit);
            if (mustMovePlanes == null)
                continue;
            alliedMustMoveCost += MoveValidator.carrierCost(mustMovePlanes);
        }
        
        carrierCapacity.put(new Integer(0), MoveValidator.carrierCapacity(unitsAtEnd) - alliedMustMoveCost);*/
        

        Territory unitTerr = route.getEnd();
        Collection<Territory> neighbors = data.getMap().getNeighbors(unitTerr, 1);
        boolean anyNeighborsWater = Match.someMatch(neighbors, Matches.TerritoryIsWater);
         
        for (Unit unit : Match.getMatches(units, Matches.UnitCanLandOnCarrier))
        {
            int carrierCost = UnitAttachment.get(unit.getType()).getCarrierCost();
            int movement = movementLeft.getInt(unit);

            for(int i = movement; i >=-1; i--)
            {
                if(i == -1 || (i==0 && !unitTerr.isWater()) || (i==1 && !anyNeighborsWater))
                {
                    if (allowKamikaze)
                        result.addUnresolvedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
                    else
                        result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
                    break;
                }

                //Check carriers that are within 'i' zones
                Integer current = new Integer(i);
                if(carrierCost != -1 && carrierCapacity.getInt(current) >= carrierCost)
                {
                    carrierCapacity.put(current,carrierCapacity.getInt(current) - carrierCost);
                    break;
                }

                //Check carriers that could potentially move to within 'i' zones
                //TODO need to subtract distance that fighter must move to reach water
                Integer potentialWithNonComMove = new Integer(i) + 2;
                if(carrierCost != -1 && carrierCapacity.getInt(potentialWithNonComMove) >= carrierCost)
                {
                    carrierCapacity.put(potentialWithNonComMove,carrierCapacity.getInt(potentialWithNonComMove) - carrierCost);
                    break;
                }
            }
            
        }
        
        return result;
    }

	private static IntegerMap<Integer> getInitialCarrierCapacity(final GameData data, Collection<Unit> units, 
			Route route, PlayerID player, int maxMovement, Collection<Unit> airThatMustLandOnCarriers) 
	{
		IntegerMap<Integer> carrierCapacity = new IntegerMap<Integer>();
		
		//Add in the potential movement for owned carriers
		int maxMoveIncludingCarrier = maxMovement +2;
		Territory currRouteEndTerr = route.getEnd();
		Territory currRouteStartTerr = route.getStart();
		int currRouteLength = route.getLength();
		
		//TODO kev perhaps this could be moved up to where we're getting the neighbors for the max+1 above.
		Collection<Territory> candidateTerritories = data.getMap().getNeighbors(currRouteEndTerr, maxMoveIncludingCarrier);
		candidateTerritories.add(currRouteEndTerr);

		Match<Territory> isSea = Matches.TerritoryIsWater;        
		Match<Territory> canMoveThrough = new InverseMatch<Territory>(Matches.TerritoryIsImpassable);

		Iterator<Territory> candidateIter = candidateTerritories.iterator();
		while (candidateIter.hasNext())
		{
			Territory candidateTerr = (Territory) candidateIter.next();
			Route candidateRoute = data.getMap().getRoute(currRouteEndTerr, candidateTerr, canMoveThrough);
			
			if(candidateRoute == null)
				continue;
			Integer candidateRouteLength = new Integer(candidateRoute.getLength());
			
			//Get the unitCollection of all units in the candidate territory.
			UnitCollection CandidateTerrUnitColl = candidateTerr.getUnits();

			Route seaRoute = data.getMap().getRoute(currRouteEndTerr, candidateTerr, isSea);
			Integer candidateSeaRouteLength = Integer.MAX_VALUE;
			if(seaRoute != null)
			{
				candidateSeaRouteLength = seaRoute.getLength();
			}
			
			//we don't want to count units that moved with us (all friendly units - moving units)
			Collection<Unit> initialUnitsAtLocation = CandidateTerrUnitColl.getMatches(Matches.alliedUnit(player, data));
			initialUnitsAtLocation.removeAll(units);
			if(initialUnitsAtLocation.isEmpty())
				continue;
			
			//This is all owned units at the location
			//TODO kev deleted this as allied air is covered in mustMoveWith calculations
			//Collection<Unit> alliedUnitsAtLocation = CandidateTerrUnitColl.getMatches(Matches.alliedUnit(player, data));
			//int extraCapacityAllied = MoveValidator.carrierCapacity(initialUnitsAtLocation) - MoveValidator.carrierCost(alliedUnitsAtLocation);
			Collection<Unit> ownedUnitsAtLocation = CandidateTerrUnitColl.getMatches(Matches.unitIsOwnedBy(player));
			int extraCapacity = MoveValidator.carrierCapacity(initialUnitsAtLocation) - MoveValidator.carrierCost(ownedUnitsAtLocation);
			if(candidateTerr.equals(currRouteStartTerr))                
			{
				extraCapacity += MoveValidator.carrierCost(units);
			}

			// check carrierMustMoveWith, and reserve carrier capacity for allied planes as required
			Collection<Unit> ownedCarrier = Match.getMatches(CandidateTerrUnitColl.getUnits(), 
					new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player)));			
			Map<Unit, Collection<Unit>> mustMoveWith = carrierMustMoveWith(ownedCarrier, initialUnitsAtLocation, data, player);
			
			int alliedMustMoveCost = 0;
			for (Unit unit : mustMoveWith.keySet())
			{
				Collection<Unit> mustMovePlanes = mustMoveWith.get(unit);
				if (mustMovePlanes == null)
					continue;
				alliedMustMoveCost += MoveValidator.carrierCost(mustMovePlanes);
			}

			//If the territory is within the maxMovement put the max of the existing capacity or the new capacity
			if((maxMovement) >= candidateRouteLength)
				//carrierCapacity.put(candidateRouteLength, Math.max(carrierCapacity.getInt(candidateRouteLength), carrierCapacity.getInt(candidateRouteLength)- alliedMustMoveCost));
				carrierCapacity.put(candidateRouteLength, Math.max(carrierCapacity.getInt(candidateRouteLength), carrierCapacity.getInt(candidateRouteLength) + extraCapacity - alliedMustMoveCost));			
			else 
			{
				//Can move OWNED carriers to get them.
				//TODO KEV change the -2 to the max movement remaining for carriers in the candidate territory.
				//This will fix finding carriers who have already used their move.
				if((currRouteLength - maxMovement) >= candidateSeaRouteLength-2)
				{
					if(ownedCarrier.size()>0  && MoveValidator.carrierCapacity(ownedCarrier) - mustMoveWith.size() 
							- MoveValidator.carrierCost(airThatMustLandOnCarriers) >=0 && 
							MoveValidator.hasEnoughMovement(ownedCarrier, route.getLength() 
									- candidateSeaRouteLength))
						carrierCapacity.put(candidateSeaRouteLength, carrierCapacity.getInt(candidateSeaRouteLength) + extraCapacity - alliedMustMoveCost);
						//carrierCapacity.put(candidateSeaRouteLength, carrierCapacity.getInt(candidateSeaRouteLength) - alliedMustMoveCost);	
				}

			}
		}
        
        return carrierCapacity;
	}

	private static Collection<Unit> getAirThatMustLandOnCarriers(Collection<Unit> ownedAir, boolean allowKamikaze, MoveValidationResult result, int nearestLand, IntegerMap<Unit> movementLeft) 
	{
		Collection<Unit> airThatMustLandOnCarriers = new ArrayList<Unit>();
		Match<Unit> cantLandMatch = new InverseMatch<Unit>(Matches.UnitCanLandOnCarrier);
        Iterator<Unit> ownedAirIter = ownedAir.iterator();
        
        while (ownedAirIter.hasNext())
        {
            Unit unit = (Unit) ownedAirIter.next();
            if(movementLeft.getInt(unit) < nearestLand)
            {
                airThatMustLandOnCarriers.add(unit);
                //not everything can land on a carrier (i.e. bombers)
                if(Match.allMatch(Collections.singleton(unit), cantLandMatch))
                {
                	if (allowKamikaze)
                        result.addUnresolvedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
                    else
                        result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
                }
                //TODO see if we need an else condition here to do the add to airThatMustLandOnCarriers
            }
        }
		return airThatMustLandOnCarriers;
	}

	private static int getNearestLand(final GameData data, Route route, PlayerID player, int maxMovement, CompositeMatch<Territory> friendlyGround) {
		return calculateNearestDistances(data, route, player, maxMovement, friendlyGround)[0];
	}
	
	private static int getNearestFactory(final GameData data, Route route, PlayerID player, int maxMovement, CompositeMatch<Territory> friendlyGround) {
		return calculateNearestDistances(data, route, player, maxMovement, friendlyGround)[1];
	}
	
	/**
	 * Don't use, use either getNearestFactory, or getNearestLand instead 
	 */
	private static int[] calculateNearestDistances(final GameData data, Route route, PlayerID player, int maxMovement, CompositeMatch<Territory> friendlyGround) 
	{
		int nearestLand = Integer.MAX_VALUE;
		int nearestFactory = Integer.MAX_VALUE;
		
		Match<Territory> canMoveThrough = new InverseMatch<Territory>(Matches.TerritoryIsImpassable);
		Match<Territory> notNeutral = new InverseMatch<Territory>(Matches.TerritoryIsNeutral);
		Match<Territory> notNeutralAndNotImpassible = new CompositeMatchAnd<Territory>(canMoveThrough, notNeutral);
		Match<Unit> ownedFactory = new CompositeMatchAnd<Unit>(Matches.UnitIsFactory, Matches.unitOwnedBy(player));
		boolean UseNeutrals = !isNeutralsImpassable(data);
		
		//find the closest land territory where everyone can land        
		Iterator<Territory> iter = data.getMap().getNeighbors(route.getEnd(), maxMovement + 1).iterator();

		while (iter.hasNext())
		{
			Territory territory = (Territory) iter.next();

			//can we land there?
			if(!friendlyGround.match(territory))
				continue;            

			boolean hasOwnedFactory = territory.getUnits().someMatch(ownedFactory);

			//Get a path WITHOUT using neutrals
			Route noNeutralRoute = data.getMap().getRoute(route.getEnd(), territory, notNeutralAndNotImpassible); 
			if(noNeutralRoute != null)
			{
				nearestLand = Math.min(nearestLand, noNeutralRoute.getLength());

				//Get nearest factory
				if(hasOwnedFactory)
				{
					nearestFactory = Math.min(nearestFactory, noNeutralRoute.getLength());
				}
			}
			//Get a path WITH using neutrals
			if(UseNeutrals)
			{
				Route neutralViolatingRoute = data.getMap().getRoute(route.getEnd(), territory, notNeutral);
				if((neutralViolatingRoute != null) && getNeutralCharge(data, neutralViolatingRoute) <= player.getResources().getQuantity(Constants.PUS))
				{
					nearestLand = Math.min(nearestLand, neutralViolatingRoute.getLength());

					//Get nearest factory
					if(hasOwnedFactory)
					{
						nearestFactory = Math.min(nearestFactory, neutralViolatingRoute.getLength());
					}
				}
			}
		}		
		return new int[] {nearestLand, nearestFactory};
	}

	private static CompositeMatch<Territory> alliedNonConqueredNonPendingTerritory(final GameData data, PlayerID player) {
		//these is a place where we can land
        //must be friendly and non conqueuerd land
        CompositeMatch<Territory> friendlyGround = new CompositeMatchAnd<Territory>();
        friendlyGround.add(Matches.isTerritoryAllied(player, data));
        friendlyGround.add(new Match<Territory>() 
                {
                    public boolean match(Territory o)
                    {
                        return !MoveDelegate.getBattleTracker(data).wasConquered((Territory) o);
                    }
                }
        );
        friendlyGround.add(new Match<Territory>() 
                {
                    public boolean match(Territory o)
                    {
                        return !MoveDelegate.getBattleTracker(data).hasPendingBattle((Territory) o, false);
                    }
                }
        );
        friendlyGround.add(Matches.TerritoryIsLand);
		return friendlyGround;
	}

	private static int getAirMovementLeft(final GameData data, Collection<Unit> units, Collection<Unit> ownedAir, Route route, PlayerID player, IntegerMap<Unit> movementLeft)
	{
		//this is the farthest we need to look for places to land
		
		//Set up everything we'll need
        Territory startTerr = route.getStart();
		Territory endTerr = route.getEnd();
		        
        int routeLength = route.getLength();
		int maxMovement=0;
		boolean startAirBase = false;
		boolean endAirBase = false;
		TerritoryAttachment taStart = TerritoryAttachment.get(startTerr);
		TerritoryAttachment taEnd = TerritoryAttachment.get(endTerr);

		if(taStart != null && taStart.isAirBase() && data.getAllianceTracker().isAllied(startTerr.getOwner(), player))
			startAirBase = true;

		if (taEnd != null && taEnd.isAirBase() && data.getAllianceTracker().isAllied(endTerr.getOwner(), player))
			endAirBase = true;
		
		//Go through the list of units and set each unit's movement as well as the overall group maxMovement
		Iterator<Unit> ownedAirIter = ownedAir.iterator();
		while(ownedAirIter.hasNext())
		{
			TripleAUnit unit = (TripleAUnit) ownedAirIter.next();
			int movement = unit.getMovementLeft();
			//int left = TripleAUnit.get(ownedAirIter.next()).getMovementLeft();
			if(units.contains(unit))
                movement -= routeLength;
			//If the unit started at an airbase, or is within max range of an airbase, increase the range.
			if (startAirBase)
            	movement++;

			if (endAirBase)
				movement++;
			
			maxMovement= Math.max(movement, maxMovement);
			
			movementLeft.put(unit, movement);
		}
		
		return maxMovement;
				
		//find out how much movement we have left  
       /*
            //If the route.getEnd or a neighboring zone within the max remaining fuel is an allied airbase, increase the range.
            Iterator <Territory> neighboriter = data.getMap().getNeighbors(route.getEnd(), maxMovement).iterator();            
            while (neighboriter.hasNext())
            {
            	Territory terrNext = (Territory) neighboriter.next();
            	TerritoryAttachment taNeighbor = TerritoryAttachment.get(terrNext);
            	if ((taEnd != null && taEnd.isAirBase() && data.getAllianceTracker().isAllied(route.getEnd().getOwner(), unit.getOwner())) || 
            			(taNeighbor != null && taNeighbor.isAirBase()) && data.getAllianceTracker().isAllied(terrNext.getOwner(), unit.getOwner()))
            	{
            		movement++;
            		break;
            	}
            }
        */
	}
	
    // Determines whether we can pay the neutral territory charge for a
    // given route for air units. We can't cross neutral territories
    // in WW2V2.
    private static MoveValidationResult canCrossNeutralTerritory(GameData data, Route route, PlayerID player, MoveValidationResult result)
    {
        //neutrals we will overfly in the first place
        Collection<Territory> neutrals = MoveDelegate.getEmptyNeutral(route);
        int PUs = player.getResources().getQuantity(Constants.PUS);

        if (PUs < getNeutralCharge(data, neutrals.size()))
            return result.setErrorReturnResult(TOO_POOR_TO_VIOLATE_NEUTRALITY);

        return result;
    }

    private static Territory getTerritoryTransportHasUnloadedTo(final List<UndoableMove> undoableMoves, Unit transport)
    {
        
        for(UndoableMove undoableMove : undoableMoves)
        {
            if(undoableMove.wasTransportUnloaded(transport))
            {
                return undoableMove.getRoute().getEnd();
            }
        }
        return null;
    }

    private static MoveValidationResult validateTransport(GameData data, 
                                                          final List<UndoableMove> undoableMoves, 
                                                          Collection<Unit> units, 
                                                          Route route, 
                                                          PlayerID player, 
                                                          Collection<Unit> transportsToLoad, 
                                                          MoveValidationResult result)
    {
        boolean isEditMode = getEditMode(data);
        
        
        if (Match.allMatch(units, Matches.UnitIsAir))
            return result;

        if (!MoveValidator.hasWater(route))
            return result;

        TransportTracker transportTracker = new TransportTracker();
        
        //if unloading make sure length of route is only 1
        if (!isEditMode && MoveValidator.isUnload(route))
        {
            if (route.getLength() > 1)
                return result.setErrorReturnResult("Unloading units must stop where they are unloaded");

            for (Unit unit : transportTracker.getUnitsLoadedOnAlliedTransportsThisTurn(units))
                result.addDisallowedUnit(CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND,unit);

            Collection<Unit> transports = MoveDelegate.mapTransports(route, units, null).values();
            for(Unit transport : transports)
            {
                //TODO This is very sensitive to the order of the transport collection.  The users may 
            	//need to modify the order in which they perform their actions.
            	
                // check whether transport has already unloaded
                if (transportTracker.hasTransportUnloadedInPreviousPhase(transport))
                {
                    for (Unit unit : transportTracker.transporting(transport))
                        result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE, unit);
                }
                // check whether transport is restricted to another territory
                else if (transportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, route.getEnd()))
                {
                    Territory alreadyUnloadedTo = getTerritoryTransportHasUnloadedTo(undoableMoves, transport);
                    for (Unit unit : transportTracker.transporting(transport))
                        result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO + alreadyUnloadedTo.getName(), unit);
                }
                // Check if the transport has already loaded after being in combat
                else if (transportTracker.isTransportUnloadRestrictedInNonCombat(transport))
                {
                	for (Unit unit : transportTracker.transporting(transport))
                	result.addDisallowedUnit(TRANSPORT_CANNOT_LOAD_AND_UNLOAD_AFTER_COMBAT, unit);
                }
            }
        }

        //if we are land make sure no water in route except for transport
        // situations
        Collection<Unit> land = Match.getMatches(units, Matches.UnitIsLand);

        //make sure we can be transported
        Match<Unit> cantBeTransported = new InverseMatch<Unit>(Matches.UnitCanBeTransported);
        for (Unit unit : Match.getMatches(land, cantBeTransported))
            result.addDisallowedUnit("Not all units can be transported",unit);

        //make sure that the only the first or last territory is land
        //dont want situation where they go sea land sea
        if (!isEditMode && MoveValidator.hasLand(route) && !(route.getStart().isWater() || route.getEnd().isWater())) {
        	if(nonParatroopersPresent(player, land) || !allLandUnitsAreBeingParatroopered(units, route) ) {
        		return result.setErrorReturnResult("Invalid move, only start or end can be land when route has water.");
        	} 
        }
            

        //simply because I dont want to handle it yet
        //checks are done at the start and end, dont want to worry about just
        //using a transport as a bridge yet
        //TODO handle this
        if (!isEditMode && !route.getEnd().isWater() && !route.getStart().isWater() && nonParatroopersPresent(player, land))
            return result.setErrorReturnResult("Must stop units at a transport on route");
            
        if (route.getEnd().isWater() && route.getStart().isWater())
        {
            //make sure units and transports stick together
            Iterator<Unit> iter = units.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                UnitAttachment ua = UnitAttachment.get(unit.getType());
                //make sure transports dont leave their units behind
                if (ua.getTransportCapacity() != -1)
                {
                    Collection<Unit> holding = transportTracker.transporting(unit);
                    if (holding != null && !units.containsAll(holding))
                        result.addDisallowedUnit("Transports cannot leave their units",unit);
                }
                //make sure units dont leave their transports behind
                if (ua.getTransportCost() != -1)
                {
                    Unit transport = transportTracker.transportedBy(unit);
                    if (transport != null && !units.contains(transport))
                        result.addDisallowedUnit("Unit must stay with its transport while moving",unit);
                }
            }
        } //end if end is water

        if (MoveValidator.isLoad(route))
        {

            if (!isEditMode && route.getLength() != 1 && nonParatroopersPresent(player, land))
                return result.setErrorReturnResult("Units cannot move before loading onto transports");
                
            CompositeMatch<Unit> enemyNonSubmerged = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), new InverseMatch<Unit>(Matches
                    .unitIsSubmerged(data)));
            if (route.getEnd().getUnits().someMatch(enemyNonSubmerged) && nonParatroopersPresent(player, land))
                if(!onlyIgnoredUnitsOnPath(route, player, data, false))
                    return result.setErrorReturnResult("Cannot load when enemy sea units are present");
            
            Map<Unit,Unit> unitsToTransports = MoveDelegate.mapTransports(route, land, transportsToLoad);

            Iterator<Unit> iter = land.iterator();
            while (!isEditMode && iter.hasNext())
            {
                TripleAUnit unit = (TripleAUnit) iter.next();
                if (unit.getAlreadyMoved() != 0)
                    result.addDisallowedUnit("Units cannot move before loading onto transports",unit);
                Unit transport = unitsToTransports.get(unit);
                if (transport == null)
                    continue;
                if (transportTracker.hasTransportUnloadedInPreviousPhase(transport))
                {
                    result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE, unit);
                }
                else if (transportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, route.getEnd()))
                {
                    Territory alreadyUnloadedTo = getTerritoryTransportHasUnloadedTo(undoableMoves, transport);
                    result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO + alreadyUnloadedTo.getName(), unit);
                }
            }

            if (! unitsToTransports.keySet().containsAll(land))
            {
                // some units didn't get mapped to a transport
                Collection<UnitCategory> unitsToLoadCategories = UnitSeperator.categorize(land);

                if (unitsToTransports.size() == 0 || unitsToLoadCategories.size() == 1)
                {
                    // set all unmapped units as disallowed if there are no transports
                    //   or only one unit category
                    for (Unit unit : land)
                    {
                        if (unitsToTransports.containsKey(unit))
                            continue;                       
                        
                        UnitAttachment ua = UnitAttachment.get(unit.getType());
                        if (ua.getTransportCost() != -1)
                        {
                            result.addDisallowedUnit("Not enough transports", unit);
                            //System.out.println("adding disallowed unit (Not enough transports): "+unit);
                        }
                    }
                }
                else
                {
                    // set all units as unresolved if there is at least one transport 
                    //   and mixed unit categories
                    for (Unit unit : land)
                    {
                        UnitAttachment ua = UnitAttachment.get(unit.getType());
                        if (ua.getTransportCost() != -1)
                        {
                            result.addUnresolvedUnit("Not enough transports", unit);
                            //System.out.println("adding unresolved unit (Not enough transports): "+unit);
                        }
                    }
                }
            }

        }

        return result;
    }
    
    private static boolean allLandUnitsAreBeingParatroopered(Collection<Unit> units, Route route) {
    	//some units that can't be paratrooped
    	if(Match.someMatch(units, new CompositeMatchAnd<Unit>(Matches.UnitIsParatroop.invert(), Matches.UnitIsLand))) {
    		return false;
    	}
    	
    	List<Unit> paratroopsRequiringTransport = getParatroopsRequiringTransport(units, route);
        if(paratroopsRequiringTransport.isEmpty()) 
        {
        	return true;
        }
        
        List<Unit> bombers =  Match.getMatches(units, Matches.UnitIsStrategicBomber);
        Map<Unit, Unit> bombersAndParatroops = MoveDelegate.mapTransports(route, paratroopsRequiringTransport, bombers);
        return bombersAndParatroops != null;
	}

	//checks if there are non-paratroopers present that cause move validations to fail
    private static boolean nonParatroopersPresent(PlayerID player, Collection<Unit> units)
    {
        if(!isParatroopers(player)) {
        	return true;
        }
        
        
        
    	for (Unit unit : Match.getMatches(units, Matches.UnitCanNotBeTransported))
        {
            if (!Matches.UnitIsParatroop.match(unit) && !UnitAttachment.get(unit.getUnitType()).isAir())
                return true;
        }
    	return false;
        
    }
    
    private static List<Unit> getParatroopsRequiringTransport(    		
            Collection<Unit> units, final Route route
            ) {
    	
    	return Match.getMatches(units, 
    			new CompositeMatchAnd<Unit>(
    				Matches.UnitIsParatroop,
    				new Match<Unit>() {
						
						public boolean match(Unit u) {
							return TripleAUnit.get(u).getMovementLeft() < route.getLength();
						}
    					
    				}
    	));    	
    }
    
    private static MoveValidationResult validateParatroops(boolean nonCombat,
                                                           GameData data, 
                                                           final List<UndoableMove> undoableMoves, 
                                                           Collection<Unit> units, 
                                                           Route route, 
                                                           PlayerID player,
                                                           MoveValidationResult result)
    {        
        if(!isParatroopers(player))
            return result;

        if (Match.noneMatch(units, Matches.UnitIsParatroop))
            return result;
        
        if (Match.noneMatch(units, Matches.UnitIsAir))
            return result;
        
        if(nonCombat)
            return result.setErrorReturnResult("Paratroops may not move during NonCombat");
                
        if (!getEditMode(data))
        {
            //if we can move without using paratroop tech, do so
            //this allows moving a bomber/infantry from one friendly
            //territory to another
        	List<Unit> paratroopsRequiringTransport = getParatroopsRequiringTransport(units, route);
            if(paratroopsRequiringTransport.isEmpty()) 
            {
            	return result;
            }
            
            List<Unit> bombers =  Match.getMatches(units, Matches.UnitIsStrategicBomber);
            Map<Unit, Unit> bombersAndParatroops = MoveDelegate.mapTransports(route, paratroopsRequiringTransport, bombers);
           
            for (Unit paratroop : bombersAndParatroops.keySet())
			{				
            	if(TripleAUnit.get(paratroop).getAlreadyMoved() != 0) 
            	{
            		result.addDisallowedUnit("Cannot paratroop units that have already moved", paratroop);
            	}

            	Unit transport = bombersAndParatroops.get(paratroop);
            	if(TripleAUnit.get(transport).getAlreadyMoved() != 0)
            	{
            		result.addDisallowedUnit("Cannot move then transport paratroops", transport);
            	}
			}
            
            for (Unit paratroop : paratroopsRequiringTransport)
            {
            	if(TripleAUnit.get(paratroop).getAlreadyMoved() != 0) 
            	{
            		result.addDisallowedUnit("Cannot paratroop units that have already moved", paratroop);
            	}
            }
            
            //TODO using just allied territories causes it to go to LALA land when moving to water
            //if (Matches.isTerritoryAllied(player, data).match(route.getEnd()))
            if (Matches.isTerritoryFriendly(player, data).match(route.getEnd()))
            {
                CompositeMatch<Unit> paratroopNBombers = new CompositeMatchOr<Unit>();
                paratroopNBombers.add(Matches.UnitIsStrategicBomber);
                paratroopNBombers.add(Matches.UnitIsParatroop);
                if(Match.someMatch(units, paratroopNBombers))
                {
                    result.setErrorReturnResult("Paratroops must advance to battle");
                }
                return result;
            }
            
            for(int i = 0; i < route.getLength() - 1; i++)
            {
                Territory current = route.at(i);
                
                if(!Matches.isTerritoryFriendly(player, data).match(current))
                    return result.setErrorReturnResult("Must stop paratroops in first enemy territory");
            }            
        }
        
        return result;        
    }


    public static String validateCanal(Route route, PlayerID player, GameData data)
    {
        Collection<Territory> territories = route.getTerritories();
        
        for(Territory routeTerritory : territories)
        {   
            Set<CanalAttachment> canalAttachments = CanalAttachment.get(routeTerritory);
            if(canalAttachments.isEmpty())
                continue;
            
            Iterator<CanalAttachment> iter = canalAttachments.iterator();
            while(iter.hasNext() )
            {
                CanalAttachment attachment = iter.next();
                if(attachment == null)
                    continue;
                if(!territories.containsAll( CanalAttachment.getAllCanalSeaZones(attachment.getCanalName(), data) ))
                {
                    continue;
                }
            
            
                for(Territory borderTerritory : attachment.getLandTerritories())
                {
                    if (!data.getAllianceTracker().isAllied(player, borderTerritory.getOwner()))
                    {
                        return "Must own " + borderTerritory.getName() + " to go through " + attachment.getCanalName();
                    }
                    if(MoveDelegate.getBattleTracker(data).wasConquered(borderTerritory))
                    {
                        return "Cannot move through " + attachment.getCanalName() + " without owning " + borderTerritory.getName() + " for an entire turn";
                    }            
                }
            }
        }
        return null;
    }

    public static MustMoveWithDetails getMustMoveWith(Territory start, Collection<Unit> units, GameData data, PlayerID player)
    {
        return new MustMoveWithDetails(mustMoveWith(units, start, data, player));
    }
   
    private static Map<Unit, Collection<Unit>> mustMoveWith(Collection<Unit> units, Territory start, GameData data, PlayerID player)
    {

        List<Unit> sortedUnits = new ArrayList<Unit>(units);

        Collections.sort(sortedUnits, UnitComparator.getIncreasingMovementComparator());

        Map<Unit, Collection<Unit>> mapping = new HashMap<Unit, Collection<Unit>>();
        mapping.putAll(transportsMustMoveWith(sortedUnits));
        mapping.putAll(carrierMustMoveWith(sortedUnits, start, data, player));
        mapping.putAll(bombersMustMoveWith(sortedUnits));
        return mapping;
    }

    private static Map<Unit, Collection<Unit>> transportsMustMoveWith(Collection<Unit> units)
    {
        TransportTracker transportTracker = new TransportTracker();
        Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<Unit, Collection<Unit>>();
        //map transports
        Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsTransport);
        Iterator<Unit> iter = transports.iterator();
        while (iter.hasNext())
        {
            Unit transport = iter.next();
            Collection<Unit> transporting = transportTracker.transporting(transport);
            mustMoveWith.put(transport, transporting);
        }
        return mustMoveWith;
    }

    private static Map<Unit, Collection<Unit>> bombersMustMoveWith(Collection<Unit> units)
    {
        TransportTracker transportTracker = new TransportTracker();
        Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<Unit, Collection<Unit>>();
        //map transports
        Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsStrategicBomber);
        Iterator<Unit> iter = transports.iterator();
        while (iter.hasNext())
        {
            Unit transport = iter.next();
            Collection<Unit> transporting = transportTracker.transporting(transport);
            mustMoveWith.put(transport, transporting);
        }
        return mustMoveWith;
    }
    
    private static Map<Unit, Collection<Unit>> carrierMustMoveWith(Collection<Unit> units, Territory start, GameData data, PlayerID player)
    {
        return carrierMustMoveWith(units, start.getUnits().getUnits(), data, player);
    }

    public static Map<Unit, Collection<Unit>> carrierMustMoveWith(Collection<Unit> units, Collection<Unit> startUnits, GameData data, PlayerID player)
    {
        //we want to get all air units that are owned by our allies
        //but not us that can land on a carrier
        CompositeMatch<Unit> friendlyNotOwnedAir = new CompositeMatchAnd<Unit>();
        friendlyNotOwnedAir.add(Matches.alliedUnit(player, data));
        friendlyNotOwnedAir.addInverse(Matches.unitIsOwnedBy(player));
        friendlyNotOwnedAir.add(Matches.UnitCanLandOnCarrier);

        Collection<Unit> alliedAir = Match.getMatches(startUnits, friendlyNotOwnedAir);

        if (alliedAir.isEmpty())
            return Collections.emptyMap();

        //remove air that can be carried by allied
        CompositeMatch<Unit> friendlyNotOwnedCarrier = new CompositeMatchAnd<Unit>();
        friendlyNotOwnedCarrier.add(Matches.UnitIsCarrier);
        friendlyNotOwnedCarrier.add(Matches.alliedUnit(player, data));
        friendlyNotOwnedCarrier.addInverse(Matches.unitIsOwnedBy(player));

        Collection<Unit> alliedCarrier = Match.getMatches(startUnits, friendlyNotOwnedCarrier);

        Iterator<Unit> alliedCarrierIter = alliedCarrier.iterator();
        while (alliedCarrierIter.hasNext())
        {
            Unit carrier = alliedCarrierIter.next();
            Collection<Unit> carrying = getCanCarry(carrier, alliedAir);
            alliedAir.removeAll(carrying);
        }

        if (alliedAir.isEmpty())
            return Collections.emptyMap();

        Map<Unit, Collection<Unit>> mapping = new HashMap<Unit, Collection<Unit>>();
        //get air that must be carried by our carriers
        Collection<Unit> ownedCarrier = Match.getMatches(units, new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player)));

        Iterator<Unit> ownedCarrierIter = ownedCarrier.iterator();
        while (ownedCarrierIter.hasNext())
        {
            Unit carrier = (Unit) ownedCarrierIter.next();
            Collection<Unit> carrying = getCanCarry(carrier, alliedAir);
            alliedAir.removeAll(carrying);

            mapping.put(carrier, carrying);            
        }

        return mapping;
    }

    private static Collection<Unit> getCanCarry(Unit carrier, Collection<Unit> selectFrom)
    {

        UnitAttachment ua = UnitAttachment.get(carrier.getUnitType());
        Collection<Unit> canCarry = new ArrayList<Unit>();

        int available = ua.getCarrierCapacity();
        Iterator<Unit> iter = selectFrom.iterator();
        TripleAUnit tACarrier = (TripleAUnit) carrier;
        while (iter.hasNext())
        {
            Unit plane = (Unit) iter.next();
    		TripleAUnit tAPlane = (TripleAUnit) plane;
            UnitAttachment planeAttatchment = UnitAttachment.get(plane.getUnitType());
            int cost = planeAttatchment.getCarrierCost();
            if (available >= cost) 
            {
        		if(tACarrier.getAlreadyMoved() == tAPlane.getAlreadyMoved())
        		{
        			available -= cost;
        			canCarry.add(plane);
        		}
            }
            if (available == 0)
                break;
        }
        return canCarry;
    }

    /**
     * Get the route ignoring forced territories
     */
    @SuppressWarnings("unchecked")
    public static Route getBestRoute(Territory start, Territory end, GameData data, PlayerID player, Collection<Unit> units)
    {
        // No neutral countries on route predicate
        Match<Territory> noNeutral = new InverseMatch<Territory>(new CompositeMatchAnd<Territory>(Matches.TerritoryIsNeutral));

        //ignore the end territory in our tests
        //it must be in the route, so it shouldn't affect the route choice
        Match<Territory> territoryIsEnd = Matches.territoryIs(end);

        Route defaultRoute;
        if (isWW2V2(data) || isNeutralsImpassable(data))

        	//defaultRoute = data.getMap().getRoute(start, end, new CompositeMatchOr(new CompositeMatchAnd(noNeutral, notEnemyAA), territoryIsEnd));
        	defaultRoute = data.getMap().getRoute(start, end, new CompositeMatchOr(noNeutral, territoryIsEnd));
        	
        else
        {
        	//defaultRoute = data.getMap().getRoute(start, end, new CompositeMatchOr(notEnemyAA, territoryIsEnd));
        	defaultRoute = data.getMap().getRoute(start, end);
        }

        if (defaultRoute == null)
        	defaultRoute = data.getMap().getRoute(start, end);
        
        if(defaultRoute == null) {
        	return null;
        }
        
        //If start and end are land, try a land route.
        //don't force a land route, since planes may be moving
        if(!start.isWater() && !end.isWater())
        { 
        	Route landRoute;
        	if (isWW2V2(data) || isNeutralsImpassable(data))
        		landRoute = data.getMap().getRoute(start, end, new CompositeMatchAnd( Matches.TerritoryIsLand, new CompositeMatchOr(noNeutral, territoryIsEnd)));
        	else
        	{
        		landRoute = data.getMap().getRoute(start, end, Matches.TerritoryIsLand);
        	}
        	
            if (landRoute != null &&
            		landRoute.getLength() == defaultRoute.getLength())
            {
            	//If there are no air units, return the land route
            	if(Match.noneMatch(units, Matches.UnitIsAir))
            		defaultRoute = landRoute;
            }
        }
        
        //if the start and end are in water, try and get a water route
        //dont force a water route, since planes may be moving
        if (start.isWater() && end.isWater())
        {
            Route waterRoute = data.getMap().getRoute(start, end,
                Matches.TerritoryIsWater);
            if (waterRoute != null &&
                waterRoute.getLength() == defaultRoute.getLength())
            	defaultRoute = waterRoute;
        }

        // No aa guns on route predicate
        Match<Territory> noAA = new InverseMatch<Territory>(Matches.territoryHasEnemyAA(player, data));

        //no enemy units on the route predicate
        Match<Territory> noEnemy = new InverseMatch<Territory>(Matches.territoryHasEnemyUnits(player, data));
        
        
        //these are the conditions we would like the route to satisfy, starting
        //with the most important
        List<Match<Territory>> tests = new ArrayList( Arrays.asList(
                //best if no enemy and no neutral
                new CompositeMatchOr<Territory>(noEnemy, noNeutral),
                //we will be satisfied if no aa and no neutral
                new CompositeMatchOr<Territory>(noAA, noNeutral),
                //single matches
                noEnemy, noAA, noNeutral));
        
        
        //remove matches that already pass
        //ignore the end
        for(Iterator<Match<Territory>> iter = tests.iterator(); iter.hasNext(); ) {
            Match<Territory> current = iter.next();
            if(defaultRoute.allMatch(new CompositeMatchOr(current, territoryIsEnd))) {
                iter.remove();
            }
        }
        
        
        for(Match<Territory> t : tests) {            
            Match<Territory> testMatch = null;
            if (isWW2V2(data) || isNeutralsImpassable(data))
                testMatch = new CompositeMatchAnd<Territory>(t, noNeutral);
            else
                testMatch = t;

            Route testRoute = data.getMap().getRoute(start, end, new CompositeMatchOr<Territory>(testMatch, territoryIsEnd));
            
            if(testRoute != null && testRoute.getLength() == defaultRoute.getLength())
                return testRoute;
        }
            
        
        
        return defaultRoute;
    }
    
    /**
     * @return
     */
    private static boolean isSubmersibleSubsAllowed(GameData data)
    {
    	return games.strategy.triplea.Properties.getSubmersible_Subs(data);
    }

    /**
     * @return
     */
    private static boolean isIgnoreTransportInMovement(GameData data)
    {
    	return games.strategy.triplea.Properties.getIgnoreTransportInMovement(data);
    }

    /**
     * @return
     */
    private static boolean isIgnoreSubInMovement(GameData data)
    {
    	return games.strategy.triplea.Properties.getIgnoreSubInMovement(data);
    }

    /**
     * @return
     */
    private static boolean isSubControlSeaZoneRestricted(GameData data)
    {
    	return games.strategy.triplea.Properties.getSubControlSeaZoneRestricted(data);
    }

    /**
     * @return
     */
    private static boolean isTransportControlSeaZone(GameData data)
    {
    	return games.strategy.triplea.Properties.getTransportControlSeaZone(data);
    }

    /** Creates new MoveValidator */
    private MoveValidator()
    {
    }
}
