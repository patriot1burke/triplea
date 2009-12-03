/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Logic to fire rockets.
 */
public class RocketsFireHelper
{

	private boolean isWW2V2(GameData data)
    {
    	return games.strategy.triplea.Properties.getWW2V2(data);
    }
	
    private boolean isAllRocketsAttack(GameData data)
    {
        return games.strategy.triplea.Properties.getAllRocketsAttack(data);
    }
	
    private boolean isRocketsCanFlyOverImpassables(GameData data)
    {
        return games.strategy.triplea.Properties.getRocketsCanFlyOverImpassables(data);
    }

    /**
     * @return
     */
    private boolean isSBRAffectsUnitProduction(GameData data)
    {
        return games.strategy.triplea.Properties.getSBRAffectsUnitProduction(data);
    }   
    
	private boolean isOneRocketAttackPerFactory(GameData data)
    {
    	return games.strategy.triplea.Properties.getRocketAttackPerFactoryRestricted(data);
    }
	
	private boolean isPUCap(GameData data)
	{
    	return games.strategy.triplea.Properties.getPUCap(data);
	}
	
	private boolean isLimitRocketDamagePerTurn(GameData data)
	{
    	return games.strategy.triplea.Properties.getLimitRocketDamagePerTurn(data);
	}

	private boolean isLimitRocketDamageToProduction(GameData data)
	{
    	return games.strategy.triplea.Properties.getLimitRocketDamageToProduction(data);
	}
	
    public RocketsFireHelper()
    {

    }

    public void fireRockets(IDelegateBridge bridge, GameData data, PlayerID player)
    {

        Set<Territory> rocketTerritories = getTerritoriesWithRockets(data, player);
        if (rocketTerritories.isEmpty())
        {
            getRemote(bridge).reportMessage("No aa guns to fire rockets with");
            return;
        }
//TODO this is weird!  Check the parens
        if ((isWW2V2(data) || isAllRocketsAttack(data)) || isOneRocketAttackPerFactory(data))
            fireWW2V2(data, player, rocketTerritories, bridge);
        else
            fireWW2V1(data, player, rocketTerritories, bridge);

    }

    private void fireWW2V2(GameData data, PlayerID player, Set<Territory> rocketTerritories, IDelegateBridge bridge)
    {
        Set<Territory> attackedTerritories = new HashSet<Territory>();
        Iterator<Territory> iter = rocketTerritories.iterator();
        while (iter.hasNext())
        {
            Territory territory = iter.next();
            Set<Territory> targets = getTargetsWithinRange(territory, data, player);
            targets.removeAll(attackedTerritories);
            if (targets.isEmpty())
                continue;
            Territory target = getTarget(targets, player, bridge, territory);
            if (target != null)
            {
                attackedTerritories.add(target);
                fireRocket(player, target, bridge, data, territory);
            }
        }
    }

    private void fireWW2V1(GameData data, PlayerID player, Set<Territory> rocketTerritories, IDelegateBridge bridge)
    {
        Set<Territory> targets = new HashSet<Territory>();
        Iterator<Territory> iter = rocketTerritories.iterator();
        while (iter.hasNext())
        {
            Territory territory = iter.next();
            targets.addAll(getTargetsWithinRange(territory, data, player));
        }

        if (targets.isEmpty())
        {
            getRemote(bridge).reportMessage("No targets to attack with rockets");
            return;
        }

        Territory attacked = getTarget(targets, player, bridge, null);
        if (attacked != null)
            fireRocket(player, attacked, bridge, data, null);
    }

    Set<Territory> getTerritoriesWithRockets(GameData data, PlayerID player)
    {

        Set<Territory> territories = new HashSet<Territory>();

        CompositeMatch<Unit> ownedAA = new CompositeMatchAnd<Unit>();
        ownedAA.add(Matches.UnitIsAA);
        ownedAA.add(Matches.unitIsOwnedBy(player));

        BattleTracker tracker = MoveDelegate.getBattleTracker(data);
        
        Iterator iter = data.getMap().iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            if (current.isWater())
                continue;
            if(tracker.wasConquered(current)) 
                continue;
            
            
            if (current.getUnits().someMatch(ownedAA))
                territories.add(current);
        }
        return territories;
    }

    private Set<Territory> getTargetsWithinRange(Territory territory, GameData data, PlayerID player)
    {

        Collection possible = data.getMap().getNeighbors(territory, 3);

        CompositeMatch<Unit> enemyFactory = new CompositeMatchAnd<Unit>();
        enemyFactory.add(Matches.UnitIsFactory);
        enemyFactory.add(Matches.enemyUnit(player, data));

        Set<Territory> hasFactory = new HashSet<Territory>();
        
        boolean rocketsOverImpassables = isRocketsCanFlyOverImpassables(data);
        Match impassable = Matches.TerritoryIsNotImpassable;

        Iterator iter = possible.iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            
            Route route = data.getMap().getRoute(territory, current, impassable);
            if(route != null && route.getLength() <= 3)
            {
                if (current.getUnits().someMatch(enemyFactory)) 
                    hasFactory.add(current);
            }
        }
        return hasFactory;
    }

    private Territory getTarget(Collection<Territory> targets, PlayerID player, IDelegateBridge bridge, Territory from)
    {
        //ask even if there is only once choice
        //that will allow the user to not attack if he doesnt want to
        
        return ((ITripleaPlayer) bridge.getRemote()).whereShouldRocketsAttack(targets, from);
    }

    private void fireRocket(PlayerID player, Territory attackedTerritory, IDelegateBridge bridge, GameData data, Territory attackFrom)
    {

        PlayerID attacked = attackedTerritory.getOwner();
        Resource PUs = data.getResourceList().getResource(Constants.PUS);
        //int cost = bridge.getRandom(Constants.MAX_DICE);

        int cost = bridge.getRandom(Constants.MAX_DICE, "Rocket fired by " + player.getName() + " at " + attacked.getName());

        //account for 0 base
        cost++;
        
        TerritoryAttachment ta = TerritoryAttachment.get(attackedTerritory);
        int territoryProduction = ta.getProduction();
        int unitProduction = 0;
        boolean SBRAffectsUnitProd = isSBRAffectsUnitProduction(data);
        
        if(SBRAffectsUnitProd)
        {
            //get current production
            unitProduction = ta.getUnitProduction();                
            //Detemine the min that can be taken as losses
            //int alreadyLost = DelegateFinder.moveDelegate(data).PUsAlreadyLost(attackedTerritory);
            int alreadyLost = territoryProduction - unitProduction;
            
            int limit = 2 * territoryProduction  - alreadyLost;
            cost = Math.min(cost, limit);

            // Record production lost
            DelegateFinder.moveDelegate(data).PUsLost(attackedTerritory, cost);
        	Collection<Unit> damagedFactory = Match.getMatches(attackedTerritory.getUnits().getUnits(), Matches.UnitIsFactory);

    		IntegerMap<Unit> hits = new IntegerMap<Unit>();
        	for(Unit factory:damagedFactory)
        	{
        		hits.put(factory,1);
        	}
            
        	bridge.addChange(ChangeFactory.unitsHit(hits));
        	
           /* Change change = ChangeFactory.attachmentPropertyChange(ta, (new Integer(unitProduction - cost)).toString(), "unitProduction");
            bridge.addChange(change);
            bridge.getHistoryWriter().addChildToEvent("Rocket attack costs " + cost + " production.");*/
        }
        //in WW2V2, limit rocket attack cost to production value of factory.
        else if (isWW2V2(data) || isLimitRocketDamageToProduction(data))
        {
            // If we are limiting total PUs lost then take that into account
            if (isPUCap(data) || isLimitRocketDamagePerTurn(data))
            {
                int alreadyLost = DelegateFinder.moveDelegate(data).PUsAlreadyLost(attackedTerritory);
                territoryProduction -= alreadyLost;
                territoryProduction = Math.max(0, territoryProduction);
            }

            if (cost > territoryProduction)
            {
                cost = territoryProduction;
            }
        }
        
        // Trying to remove more PUs than the victim has is A Bad Thing[tm]
        int availForRemoval = attacked.getResources().getQuantity(PUs);
        if (cost > availForRemoval)
            cost = availForRemoval;

        // Record the PUs lost
        DelegateFinder.moveDelegate(data).PUsLost(attackedTerritory, cost);
       
        if(SBRAffectsUnitProd)
        {
            getRemote(bridge).reportMessage("Rocket attack in " + attackedTerritory.getName() + " costs: " + cost + " production.");
        
            bridge.getHistoryWriter().startEvent("Rocket attack in " + attackedTerritory.getName() + " costs: " + cost + " production.");
            
            Change change = ChangeFactory.attachmentPropertyChange(ta, (new Integer(unitProduction - cost)).toString(), "unitProduction");
            bridge.addChange(change);
            
        }
        else
        {
            getRemote(bridge).reportMessage("Rocket attack in " + attackedTerritory.getName() + " costs:" + cost);
      
            String transcriptText = attacked.getName() + " lost " + cost + " PUs to rocket attack by " + player.getName();
            bridge.getHistoryWriter().startEvent(transcriptText);

            Change rocketCharge = ChangeFactory.changeResourcesChange(attacked, PUs, -cost);
            bridge.addChange(rocketCharge);
        }
        //this is null in WW2V1
        if(attackFrom != null)
        {
            List<Unit> units = attackFrom.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitIsAA, Matches.unitIsOwnedBy(player) ));
            
            if(units.size() > 0)
            {
                //only one fired
                Change change = DelegateFinder.moveDelegate(data).markNoMovementChange( Collections.singleton(units.get(0)));
                bridge.addChange(change);
            }
            else
            {
                new IllegalStateException("No aa guns?" + attackFrom.getUnits().getUnits());
            }
        }
        

    }

    
    private ITripleaPlayer getRemote(IDelegateBridge bridge)
    {
        return (ITripleaPlayer) bridge.getRemote();
    }
    
}