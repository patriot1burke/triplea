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
 * BattleCalculator.java
 *
 * Created on November 29, 2001, 2:27 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.triplea.weakAI.WeakAI;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * Utiltity class for determing casualties and selecting casualties. The code
 * was being dduplicated all over the place.
 */
public class BattleCalculator
{
	//we want to sort in a determined way so that those looking at the dice results 
    //can tell what dice is for who
    //we also want to sort by movement, so casualties will be choosen as the 
    //units with least movement
    public static void sortPreBattle(List<Unit> units, GameData data)
    {
        Comparator<Unit> comparator = new Comparator<Unit>()
        {
          public int compare(Unit u1, Unit u2)
          {            
              if(u1.getUnitType().equals(u2.getUnitType()))
                  return UnitComparator.getDecreasingMovementComparator().compare(u1, u2);
              
              return u1.getUnitType().getName().compareTo(u2.getUnitType().getName());
          }
        };
        
        Collections.sort(units, comparator);
        
    }
    
    public static int getAAHits(Collection<Unit> units, IDelegateBridge bridge, int[] dice)
    {
        int attackingAirCount = Match.countMatches(units, Matches.UnitIsAir);

        int hitCount = 0;
        for (int i = 0; i < attackingAirCount; i++)
        {
            if (1 > dice[i])
                hitCount++;
        }
        return hitCount;
    }
    
    /**
     * Choose plane casualties according to specified rules 
     */
    public static  Collection<Unit> getAACasualties(Collection<Unit> planes, DiceRoll dice, IDelegateBridge bridge, PlayerID defender, PlayerID attacker, GameData data, GUID battleID, Territory terr)
    {
    	
    	if(Properties.getLow_Luck(data)) {
    		if(isChooseAA(data)) {
    			return chooseAACasualties(planes, dice, bridge, attacker, data,
						battleID, terr);
    		}
    		return getLowLuckAACasualties(planes, dice, terr.getUnits().someMatch(Matches.UnitIsRadarAA), terr, bridge);
    	} else {
    		//isRollAAIndividually() is the default behavior
        	Boolean rollAAIndividually = isRollAAIndividually(data);
        	
        	//Random AA Casualties
        	if(!rollAAIndividually && isRandomAACasualties(data))
        		return(RandomAACasualties(planes, dice, bridge));
        	
        	// allow player to select casualties from entire set 
        	if(!rollAAIndividually && isChooseAA(data))
        	{    	
        		return chooseAACasualties(planes, dice, bridge, attacker, data,
						battleID, terr);
        	}
        	    
        	return(IndividuallyFiredAACasualties(planes, dice, bridge, defender));
        }
    }

	private static Collection<Unit> chooseAACasualties(Collection<Unit> planes,
			DiceRoll dice, IDelegateBridge bridge, PlayerID attacker,
			GameData data, GUID battleID, Territory terr) {
		String text = "Select " + dice.getHits() + " casualties from aa fire in " + terr.getName();

		CasualtyDetails casualtyMsg =  selectCasualties(attacker, planes, bridge, text, data, dice, false, battleID);
		return  casualtyMsg.getKilled();
	}
    	
    	
	/**
	 * http://triplea.sourceforge.net/mywiki/Forum#nabble-td4658925%7Ca4658925
	 * 
	 *  returns two lists, the first list is the air units that can be evenly divided into groups of 3 or 6 (depending on radar)
	 *  the second list is all the air units that do not fit in the first list 
	 *  
	 */
	public static Tuple<List<Unit>, List<Unit>> categorizeLowLuckAirUnits(Collection<Unit> units, Territory location) {
		
		List<Unit> airUnits = Match.getMatches(units, Matches.UnitIsAir);
		Collection<UnitCategory> categorizedAir = UnitSeperator.categorize(airUnits, null, false, true);
		
		int groupSize = location.getUnits().someMatch(Matches.UnitIsRadarAA) ? 3 : 6;
		
		List<Unit> groupsOfSize = new ArrayList<Unit>();
		List<Unit> toRoll = new ArrayList<Unit>();
		
		for(UnitCategory uc : categorizedAir)
        {            
			int remainder = uc.getUnits().size() % groupSize;        	
        	int splitPosition = uc.getUnits().size() - remainder;
			groupsOfSize.addAll(uc.getUnits().subList(0, splitPosition));
        	toRoll.addAll(uc.getUnits().subList(splitPosition, uc.getUnits().size()));
        }		
		return new Tuple<List<Unit>, List<Unit>>(groupsOfSize, toRoll);		
	}
	
    	
    
    private static Collection<Unit> getLowLuckAACasualties(Collection<Unit> planes, DiceRoll dice, boolean useRadar, Territory location, IDelegateBridge bridge) {
    	
    	Tuple<List<Unit>, List<Unit>> airSplit = categorizeLowLuckAirUnits(planes, location);
    	int hitsLeft = dice.getHits();    	
    	
    	Collection<Unit> hitUnits = new ArrayList<Unit>();
    	int groupSize = location.getUnits().someMatch(Matches.UnitIsRadarAA) ? 3 : 6;
    	
        //the non rolling air units
    	for(int i = 0; i < airSplit.getFirst().size(); i+=groupSize) {
    		hitUnits.add(airSplit.getFirst().get(i));
    		hitsLeft--;
    	}


    	if(hitsLeft == airSplit.getSecond().size()) {
    		hitUnits.addAll(airSplit.getSecond());
    	} else if(hitsLeft != 0){
    		//the remainder
        	//roll all at once to prevent frequent random calls, important for pbem games
			int[] hitRandom = bridge.getRandom(airSplit.getSecond().size(), hitsLeft, "Deciding which planes should die due to AA fire");
			int pos = 0;
			for(int i =0; i < hitRandom.length; i++) {
				pos += hitRandom[i];
				hitUnits.add( airSplit.getSecond().remove(pos % airSplit.getSecond().size()));			
			}			
    	}
    	
		if(hitUnits.size() != dice.getHits()) {
        	throw new IllegalStateException("wrong number of casulaties, expected:" + dice + " but hit:" + hitUnits);
        }
        return hitUnits;
    }

    
    
  
    /**
     * Choose plane casualties randomly
     */
    public static Collection<Unit> RandomAACasualties(Collection<Unit> planes, DiceRoll dice, IDelegateBridge bridge)
    {
        Collection<Unit> casualties = new ArrayList<Unit>();
        int hits = dice.getHits();
        List<Unit> planesList = new ArrayList<Unit>(planes);

        // We need to choose which planes die randomly
        if (hits < planesList.size())
        {
            for (int i = 0; i < hits; i++)
            {
                int pos = bridge.getRandom(planesList.size(), "Deciding which planes should die due to AA fire");
                Unit unit = planesList.get(pos);
                planesList.remove(pos);
                casualties.add(unit);
            }
        } else
        {
            casualties.addAll(planesList);
        }

        return casualties;
    }

    /**
     * Choose plane casualties based on individual AA shots at each aircraft.
     */
    public static Collection<Unit> IndividuallyFiredAACasualties(Collection<Unit> planes, DiceRoll dice, IDelegateBridge bridge, PlayerID player)
    {
        Collection<Unit> casualties = new ArrayList<Unit>();
        int hits = dice.getHits();
        List<Unit> planesList = new ArrayList<Unit>(planes);

        // We need to choose which planes die based on their position in the list and the individual AA rolls
        if (hits < planesList.size())
        {
        	int rollAt = 1;
        	if(isAARadar(player))
        		rollAt = 2;
        	
            List<Die> rolls = dice.getRolls(rollAt);

            for (int i = 0; i < rolls.size(); i++)
            {
                Die die = rolls.get(i);
                if(die.getType() == DieType.HIT)
                {
                    Unit unit = planesList.get(i);
                    casualties.add(unit);
                }                
            }
            planesList.removeAll(casualties);
        } else
        {
            casualties.addAll(planesList);
        }

        return casualties;
    }

    public static CasualtyDetails selectCasualties(PlayerID player, Collection<Unit> targets, IDelegateBridge bridge, String text, GameData data,
            DiceRoll dice, boolean defending, GUID battleID)
    {
        return selectCasualties(null, player, targets, bridge, text, data, dice, defending, battleID, false, dice.getHits());
    }

    /**
     * 
     * @param battleID may be null if we are not in a battle (eg, if this is an aa fire due to moving
     */
    public static CasualtyDetails selectCasualties(String step, PlayerID player, Collection<Unit> targets, IDelegateBridge bridge, String text,
            GameData data, DiceRoll dice, boolean defending, GUID battleID, boolean headLess, int extraHits)
    {
    	boolean isEditMode = EditDelegate.getEditMode(data);
        //if (isEditMode || dice.getHits() == 0)
    	if (dice.getHits() == 0)
            return new CasualtyDetails(Collections.<Unit>emptyList(), Collections.<Unit>emptyList(), true);

        int hitsRemaining = dice.getHits();
        
        Map<Unit, Collection<Unit>> dependents;
        if(headLess)
            dependents= Collections.emptyMap();
        else
            dependents= getDependents(targets, data);
        
        if(isTransportCasualtiesRestricted(data))
        {
            hitsRemaining = extraHits;
        }

//TODO check if isEditMode is necessary
        if (!isEditMode && allTargetsOneTypeNotTwoHit(targets, dependents))
        {
            List<Unit> killed = new ArrayList<Unit>(); 
            Iterator<Unit> iter = targets.iterator();
            for (int i = 0; i < hitsRemaining; i++)
            {
                if(i >= targets.size())
                    break;
                
                killed.add(iter.next());
            }
            return new CasualtyDetails(killed, Collections.<Unit>emptyList(), true);
        }

        // Create production cost map, Maybe should do this elsewhere, but in
        // case prices change, we do it here.
        IntegerMap<UnitType> costs = getCosts(player, data);

        List<Unit> defaultCasualties = getDefaultCasualties(targets, hitsRemaining, defending, player, costs, data);

        ITripleaPlayer tripleaPlayer;
        if(player.isNull())
            tripleaPlayer = new WeakAI(player.getName());
        else
            tripleaPlayer = (ITripleaPlayer) bridge.getRemote(player);
        CasualtyDetails casualtySelection = tripleaPlayer.selectCasualties(targets, dependents,  hitsRemaining, text, dice, player,
                defaultCasualties, battleID);

        List<Unit> killed = casualtySelection.getKilled();
        //if partial retreat is possible, kill amphibious units first
    	if(isPartialAmphibiousRetreat(data))
    		killed = killAmphibiousFirst(killed, targets);
    	
        List<Unit> damaged = casualtySelection.getDamaged();

        int numhits = killed.size();
        Iterator<Unit> killedIter = killed.iterator();
        while (killedIter.hasNext())
        {
            Unit unit = killedIter.next();
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if (ua.isTwoHit() && (unit.getHits() == 0))
            {
                numhits++;
                damaged.remove(unit);
            }
        }
        
        
        //check right number 	
        if (!isEditMode && !(numhits + damaged.size() == hitsRemaining))
        {
            tripleaPlayer.reportError("Wrong number of casualties selected");
            return selectCasualties(player, targets, bridge, text, data, dice, defending, battleID);
        }
        //check we have enough of each type
        if (!targets.containsAll(killed) || !targets.containsAll(damaged))
        {
            tripleaPlayer.reportError("Cannot remove enough units of those types");
            return selectCasualties(player, targets, bridge, text, data, dice, defending, battleID);
        }
        return casualtySelection;
    }

	private static List<Unit> killAmphibiousFirst(List<Unit> killed, Collection<Unit> targets) 
	{
		Collection<Unit> allAmphibUnits = new ArrayList<Unit>();
		Collection<Unit> killedNonAmphibUnits = new ArrayList<Unit>();
    	Collection<UnitType> amphibTypes = new ArrayList<UnitType>();
    	
    	//Get a list of all selected killed units that are NOT amphibious
    	Match<Unit> aMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitWasNotAmphibious);
    	killedNonAmphibUnits.addAll(Match.getMatches(killed, aMatch));
    	
    	//If all killed units are amphibious, just return them
    	if(killedNonAmphibUnits.isEmpty())
    		return killed;

    	//Get a list of all units that are amphibious and remove those that are killed
    	allAmphibUnits.addAll(Match.getMatches(targets, Matches.UnitWasAmphibious));
    	allAmphibUnits.removeAll(Match.getMatches(killed, Matches.UnitWasAmphibious));
    	
    	Iterator<Unit> allAmphibUnitsIter = allAmphibUnits.iterator();    	    	
    	//Get a collection of the unit types of the amphib units
    	while(allAmphibUnitsIter.hasNext())
    	{
    		Unit unit = allAmphibUnitsIter.next();
    		UnitType ut = unit.getType();
    		if(!amphibTypes.contains(ut))
    			amphibTypes.add(ut);
    	}
    	
    	//For each killed unit- see if there is an amphib unit that can be killed instead
    	Iterator<Unit> killedNonAmphibUnitsIter = killedNonAmphibUnits.iterator();
    	while(killedNonAmphibUnitsIter.hasNext())
    	{
    		Unit unit = killedNonAmphibUnitsIter.next();
    		
    		if(amphibTypes.contains(unit.getType()))
    		{ //add a unit from the collection
    			List<Unit> oneAmphibUnit = Match.getNMatches(allAmphibUnits, 1, Matches.unitIsOfType(unit.getType()));

    			if(oneAmphibUnit.size()>0)
    			{
    				Unit amphibUnit = oneAmphibUnit.iterator().next();
    				killed.remove(unit);
    				killed.add(amphibUnit);
    				allAmphibUnits.remove(amphibUnit);
    				continue;
    			}
    			else //If there are no more units of that type, remove the type from the collection
    			{
    				amphibTypes.remove(unit.getType());
    			}
    		}
    	}
    	
    	return killed;
	}

    private static List<Unit> getDefaultCasualties(Collection<Unit> targets, int hits, boolean defending, PlayerID player, IntegerMap<UnitType> costs, GameData data)
    {
        // Remove two hit bb's selecting them first for default casualties
        ArrayList<Unit> defaultCasualties = new ArrayList<Unit>();
        int numSelectedCasualties = 0;
        Iterator<Unit> targetsIter = targets.iterator();
        while (targetsIter.hasNext())
        {
            // Stop if we have already selected as many hits as there are targets
            if (numSelectedCasualties >= hits)
            {
                return defaultCasualties;
            }
            Unit unit = targetsIter.next();
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if (ua.isTwoHit() && (unit.getHits() == 0))
            {
                numSelectedCasualties++;
                defaultCasualties.add(unit);
            }
        }

        // Sort units by power and cost in ascending order
        List<Unit> sorted = new ArrayList<Unit>(targets);
        Collections.sort(sorted, new UnitBattleComparator(defending, player, costs));
        // Select units
        Iterator<Unit> sortedIter = sorted.iterator();
        while (sortedIter.hasNext())
        {
            // Stop if we have already selected as many hits as there are targets
            if (numSelectedCasualties >= hits)
            {
                return defaultCasualties;
            }
            Unit unit = sortedIter.next();
            
            defaultCasualties.add(unit);
            numSelectedCasualties++;
        }

        return defaultCasualties;
    }

    public static Map<Unit, Collection<Unit>> getDependents(Collection<Unit> targets, GameData data)
    {
        //just worry about transports
        TransportTracker tracker = new TransportTracker();

        Map<Unit, Collection<Unit>> dependents = new HashMap<Unit, Collection<Unit>>();
        Iterator<Unit> iter = targets.iterator();
        while (iter.hasNext())
        {
            Unit target = iter.next();
            dependents.put(target, tracker.transportingAndUnloaded(target));
        }
        return dependents;
    }

    /**
     * Return map where keys are unit types and values are PU costs of that
     * unit type
     * 
     * @param player
     *            The player to get costs schedule for
     * @param data
     *            The game data.
     * @return a map of unit types to PU cost
     */
    public static IntegerMap<UnitType> getCosts(PlayerID player, GameData data)
    {
        IntegerMap<UnitType> costs = new IntegerMap<UnitType>();
        ProductionFrontier frontier =player.getProductionFrontier();
        //any one will do then
        if(frontier == null)
            frontier = data.getProductionFrontierList().getProductionFrontier(data.getProductionFrontierList().getProductionFrontierNames().iterator().next().toString());
        Iterator<ProductionRule> iter = frontier.getRules().iterator();
        while (iter.hasNext())
        {
            ProductionRule rule = iter.next();
            int cost = rule.getCosts().getInt(data.getResourceList().getResource(Constants.PUS));
            UnitType type = (UnitType) rule.getResults().keySet().iterator().next();
            costs.put(type, cost);
        }
        return costs;
    }

    /**
     * Return the total unit value
     * 
     * @param units
     *            A collection of units
     * @param costs
     *            An integer map of unit types to costs.
     * @return the total unit value.
     */
    public static int getTUV(Collection<Unit> units, IntegerMap<UnitType> costs)
    {
        int tuv = 0;
        Iterator<Unit> unitsIter = units.iterator();
        while (unitsIter.hasNext())
        {
            Unit u = (Unit) unitsIter.next();
            int unitValue = costs.getInt(u.getType());
            tuv += unitValue;
        }
        return tuv;
    }

    /**
     * Return the total unit value for a certain player and his allies
     * 
     * @param units
     *            A collection of units
     * @param player
     *            The player to calculate the TUV for.
     * @param costs
     *            An integer map of unit types to costs
     * @return the total unit value.
     */
    public static int getTUV(Collection<Unit> units, PlayerID player, IntegerMap<UnitType> costs, GameData data)
    {
        Collection<Unit> playerUnits = Match.getMatches(units, Matches.alliedUnit(player, data));
        return getTUV(playerUnits, costs);
    }

    /**
     * Checks if the given collections target are all of one category as defined
     * by UnitSeperator.categorize and they are not two hit units.
     * 
     * @param targets
     *            a collection of target units
     * @param dependents
     *            map of depend units for target units
     */
    private static boolean allTargetsOneTypeNotTwoHit(Collection<Unit> targets, Map<Unit, Collection<Unit>> dependents)
    {
        Set<UnitCategory> categorized = UnitSeperator.categorize(targets, dependents, false);
        if (categorized.size() == 1)
        {
            UnitCategory unitCategory =  categorized.iterator().next();
            if (!unitCategory.isTwoHit() || unitCategory.getDamaged())
            {
                return true;
            }
        }

        return false;
    }
    
    public static int getRolls(Collection<Unit> units, PlayerID id, boolean defend)    
    {
        int count = 0;
        int unitRoll = 0;
        Iterator<Unit> iter = units.iterator();
        while (iter.hasNext())
        {
            Unit unit = iter.next();
            unitRoll = getRolls(unit, id, defend);
            
            count += unitRoll;
        }
        return count;
    }

    public static int getRolls(Collection<Unit> units, PlayerID id, boolean defend, int availableSupport)
    {
        int count = 0;
        int unitRoll = 0;
        Iterator<Unit> iter = units.iterator();
        while (iter.hasNext())
        {
            Unit unit = iter.next();                       
            unitRoll = getRolls(unit, id, defend, availableSupport);       
            count += unitRoll;       
        }
        return count;
    }

    public static int getRolls(Unit unit, PlayerID id, boolean defend)    
    {
        UnitAttachment unitAttachment = UnitAttachment.get(unit.getType());
        if (defend)
        {
            //if lhtr
            //check for nulll id since null players dont have game data             
            if(!id.isNull() && games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(id.getData())) 
            {
                //if they have the heavy bomber tech, then 2 rolls for defense
                if(unitAttachment.isStrategicBomber() && TechTracker.getTechAdvances(id).contains(TechAdvance.HEAVY_BOMBER) )
                    return 2;
            }
            return 1;
        }
        
        return unitAttachment.getAttackRolls(id);
    }
  
    public static int getRolls(Unit unit, PlayerID id, boolean defend, int artillerySupport)
    {
        UnitAttachment unitAttachment = UnitAttachment.get(unit.getType());
        if (defend)
        {           
            if(!id.isNull() && games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(id.getData())) 
            {
                //if they have the heavy bomber tech, then 2 rolls for defense
                if(unitAttachment.isStrategicBomber() && TechTracker.getTechAdvances(id).contains(TechAdvance.HEAVY_BOMBER) )
                    return 2;
            }
            return 1;
        }
        
        int attackRoll = unitAttachment.getAttackRolls(id);
        if(attackRoll == 0 && artillerySupport >0 && unitAttachment.isArtillerySupportable())
        	attackRoll += 1;
        
        return attackRoll;

    }
    
    /**
     * @return Can transports be used as cannon fodder
     */
    private static boolean isTransportCasualtiesRestricted(GameData data)
    {
    	return games.strategy.triplea.Properties.getTransportCasualtiesRestricted(data);
    }
    
    /**
     * @return Can transports be used as cannon fodder
     */
	 private static boolean isAARadar(PlayerID player)
	    {
	        TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTATCHMENT_NAME);
	        if(ta == null)
	        	return false;
	        return ta.hasAARadar();     
	    }
/**
 * @return Random AA Casualties - casualties randomly assigned
 */
	 private static boolean isRandomAACasualties(GameData data)
	    {
	    	return games.strategy.triplea.Properties.getRandomAACasualties(data);
	    }

	    /**
	     * @return Roll AA Individually - roll against each aircraft
	     */
	    private static boolean isRollAAIndividually(GameData data)
	    {
	        return games.strategy.triplea.Properties.getRollAAIndividually(data);
	    }

	    /**
	     * @return Choose AA - attacker selects casualties
	     */
	    private static boolean isChooseAA(GameData data)
		{
	    	return games.strategy.triplea.Properties.getChoose_AA_Casualties(data);
		}
	    	
    /**
     * @return Can the attacker retreat non-amphibious units
     */
    private static boolean isPartialAmphibiousRetreat(GameData data)
    {
        return games.strategy.triplea.Properties.getPartialAmphibiousRetreat(data);
    }
    
    //nothing but static
    private BattleCalculator()
    {
    }
}

class UnitBattleComparator implements Comparator<Unit>
{
    private boolean m_defending;
    private PlayerID m_player;
    private IntegerMap<UnitType> m_costs;

    public UnitBattleComparator(boolean defending, PlayerID player, IntegerMap<UnitType> costs)
    {
        m_defending = defending;
        m_player = player;
        m_costs = costs;
    }

    public int compare(Unit u1, Unit u2)
    {
        if(u1.equals(u2))
            return 0;
        
        UnitAttachment ua1 = UnitAttachment.get(u1.getType());
        UnitAttachment ua2 = UnitAttachment.get(u2.getType());
        
        if(ua1 == ua2)
            return 0;
        
        int rolls1 = BattleCalculator.getRolls(u1, m_player, m_defending);
        int rolls2 = BattleCalculator.getRolls(u2, m_player, m_defending);

        if (rolls1 != rolls2)
        {
            return rolls1 - rolls2;
        }
        int power1 = m_defending ? ua1.getDefense(m_player) : ua1.getAttack(m_player);
        int power2 = m_defending ? ua2.getDefense(m_player) : ua2.getAttack(m_player);

        if (power1 != power2)
        {
            return power1 - power2;
        }
        int cost1 = m_costs.getInt(u1.getType());
        int cost2 = m_costs.getInt(u2.getType());

        if (cost1 != cost2)
        {
            return cost1 - cost2;
        }

        return 0;
    }
}