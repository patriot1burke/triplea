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
 * BattleCalculator.java
 * 
 * Created on November 29, 2001, 2:27 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleA;
import games.strategy.triplea.ai.weakAI.WeakAI;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.LinkedIntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          Utiltity class for determing casualties and selecting casualties. The code
 *          was being dduplicated all over the place.
 */
public class BattleCalculator
{
	private static Map<String, List<UnitType>> oolCache = new ConcurrentHashMap<String, List<UnitType>>();
	
	public static void clearOOLCache()
	{
		oolCache.clear();
	}
	
	// private static IntegerMap<UnitType> s_costsForTuvForAllPlayersMergedAndAveraged; //There is a problem with this variable, that it isn't being cleared out when we switch maps.
	// we want to sort in a determined way so that those looking at the dice results
	// can tell what dice is for who
	// we also want to sort by movement, so casualties will be choosen as the
	// units with least movement
	public static void sortPreBattle(final List<Unit> units, final GameData data)
	{
		final Comparator<Unit> comparator = new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				if (u1.getUnitType().equals(u2.getUnitType()))
					return UnitComparator.getLowestToHighestMovementComparator().compare(u1, u2);
				return u1.getUnitType().getName().compareTo(u2.getUnitType().getName());
			}
		};
		Collections.sort(units, comparator);
	}
	
	public static int getTotalHitpointsLeft(final Collection<Unit> units)
	{
		if (units == null || units.isEmpty())
			return 0;
		int rVal = 0;
		for (final Unit u : units)
		{
			final UnitAttachment ua = UnitAttachment.get(u.getType());
			rVal += ua.getHitPoints();
			rVal -= u.getHits();
		}
		return rVal;
	}
	
	public static int getTotalHitpointsLeft(final Unit unit)
	{
		if (unit == null)
			return 0;
		final UnitAttachment ua = UnitAttachment.get(unit.getType());
		return ua.getHitPoints() - unit.getHits();
	}
	
	/**
	 * Useful for fast approximations of strength.
	 * Returns a number equal to: (2 * Hitpoints) + (Power * 6 / DiceSides)
	 */
	public static int getNormalizedMetaPower(final int power, final int hitpoints, final int diceSides)
	{
		return (2 * hitpoints) + (power * 6 / diceSides);
	}
	
	public static int getAAHits(final Collection<Unit> units, final IDelegateBridge bridge, final int[] dice)
	{
		final int attackingAirCount = Match.countMatches(units, Matches.UnitIsAir);
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
	public static CasualtyDetails getAACasualties(final boolean defending, final Collection<Unit> planes, final Collection<Unit> allFriendlyUnits, final Collection<Unit> defendingAA,
				final Collection<Unit> allEnemyUnits, final DiceRoll dice, final IDelegateBridge bridge, final PlayerID firingPlayer, final PlayerID hitPlayer, final GUID battleID,
				final Territory terr, final Collection<TerritoryEffect> territoryEffects, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers)
	{
		if (planes.isEmpty())
			return new CasualtyDetails();
		final GameData data = bridge.getData();
		final boolean allowMultipleHitsPerUnit = Match.allMatch(defendingAA, Matches.UnitAAShotDamageableInsteadOfKillingInstantly);
		if (isChooseAA(data))
		{
			final String text = "Select " + dice.getHits() + " casualties from aa fire in " + terr.getName();
			return selectCasualties(null, hitPlayer, planes, allFriendlyUnits, firingPlayer, allEnemyUnits, amphibious, amphibiousLandAttackers, terr, territoryEffects, bridge, text, dice, defending,
						battleID, false, dice.getHits(), allowMultipleHitsPerUnit);
		}
		else
		{
			if (Properties.getLow_Luck(data) || Properties.getLL_AA_ONLY(data))
			{
				return getLowLuckAACasualties(defending, planes, defendingAA, dice, terr, bridge, allowMultipleHitsPerUnit);
			}
			else
			{
				// priority goes: choose -> individually -> random
				// if none are set, we roll individually
				if (isRollAAIndividually(data))
					return IndividuallyFiredAACasualties(defending, planes, defendingAA, dice, terr, bridge, allowMultipleHitsPerUnit);
				if (isRandomAACasualties(data))
					return RandomAACasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
				return IndividuallyFiredAACasualties(defending, planes, defendingAA, dice, terr, bridge, allowMultipleHitsPerUnit);
			}
		}
	}
	
	/**
	 * http://triplea.sourceforge.net/mywiki/Forum#nabble-td4658925%7Ca4658925
	 * 
	 * returns two lists, the first list is the air units that can be evenly divided into groups of 3 or 6 (depending on radar)
	 * the second list is all the air units that do not fit in the first list
	 * 
	 */
	public static Tuple<List<List<Unit>>, List<Unit>> categorizeLowLuckAirUnits(final Collection<Unit> units, final Territory location, final int diceSides, final int groupSize)
	{
		final Collection<UnitCategory> categorizedAir = UnitSeperator.categorize(units, null, false, true);
		final List<List<Unit>> groupsOfSize = new ArrayList<List<Unit>>();
		final List<Unit> toRoll = new ArrayList<Unit>();
		for (final UnitCategory uc : categorizedAir)
		{
			final int remainder = uc.getUnits().size() % groupSize;
			final int splitPosition = uc.getUnits().size() - remainder;
			final List<Unit> group = new ArrayList<Unit>(uc.getUnits().subList(0, splitPosition));
			if (!group.isEmpty())
			{
				for (int i = 0; i < splitPosition; i += groupSize)
				{
					final List<Unit> miniGroup = new ArrayList<Unit>(uc.getUnits().subList(i, i + groupSize));
					if (!miniGroup.isEmpty())
						groupsOfSize.add(miniGroup);
				}
			}
			toRoll.addAll(uc.getUnits().subList(splitPosition, uc.getUnits().size()));
		}
		return new Tuple<List<List<Unit>>, List<Unit>>(groupsOfSize, toRoll);
	}
	
	private static CasualtyDetails getLowLuckAACasualties(final boolean defending, final Collection<Unit> planes, final Collection<Unit> defendingAA, final DiceRoll dice, final Territory location,
				final IDelegateBridge bridge, final boolean allowMultipleHitsPerUnit)
	{
		{
			final Set<Unit> duplicatesCheckSet1 = new HashSet<Unit>(planes);
			if (planes.size() != duplicatesCheckSet1.size())
			{
				throw new IllegalStateException("Duplicate Units Detected: Original List:" + planes + "  HashSet:" + duplicatesCheckSet1);
			}
			final Set<Unit> duplicatesCheckSet2 = new HashSet<Unit>(defendingAA);
			if (defendingAA.size() != duplicatesCheckSet2.size())
			{
				throw new IllegalStateException("Duplicate Units Detected: Original List:" + defendingAA + "  HashSet:" + duplicatesCheckSet2);
			}
		}
		int hitsLeft = dice.getHits();
		if (hitsLeft <= 0)
			return new CasualtyDetails();
		// if we can damage units, do it now
		final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
		
		final GameData data = bridge.getData();
		final Tuple<Integer, Integer> attackThenDiceSides = DiceRoll.getAAattackAndMaxDiceSides(defendingAA, data, !defending);
		final int highestAttack = attackThenDiceSides.getFirst();
		if (highestAttack < 1)
			return new CasualtyDetails();
		final int chosenDiceSize = attackThenDiceSides.getSecond();
		final Triple<Integer, Integer, Boolean> triple = DiceRoll.getTotalAAPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(null, null, !defending, defendingAA, planes, data, false);
		// final int totalPower = triple.getFirst();
		final boolean allSameAttackPower = triple.getThird();
		// multiple HP units need to be counted multiple times:
		final List<Unit> planesList = new ArrayList<Unit>();
		for (final Unit plane : planes)
		{
			final int hpLeft = allowMultipleHitsPerUnit ? (UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits()) :
						(Math.min(1, UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits()));
			for (int hp = 0; hp < hpLeft; ++hp)
			{
				planesList.add(plane); // if allowMultipleHitsPerUnit, then because the number of rolls exactly equals the hitpoints of all units, we roll multiple times for any unit with multiple hitpoints
			}
		}
		// killing the air by groups does not work if the the attack power is different for some of the rolls
		// also, killing by groups does not work if some of the aa guns have 'MayOverStackAA' and we have more hits than the total number of groups (including the remainder group)
		// (when i mean, 'does not work', i mean that it is no longer a mathematically fair way to find casualties)
		// find group size (if no groups, do dice sides)
		final int groupSize;
		if (allSameAttackPower)
			groupSize = chosenDiceSize / highestAttack;
		else
			groupSize = chosenDiceSize;
		final int numberOfGroupsByDiceSides = (int) Math.ceil((double) planesList.size() / (double) groupSize);
		final boolean tooManyHitsToDoGroups = hitsLeft > numberOfGroupsByDiceSides;
		if (!allSameAttackPower || tooManyHitsToDoGroups || chosenDiceSize % highestAttack != 0)
		{
			// we have too many hits, so just pick randomly
			return RandomAACasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
		}
		else
		{
			// if we have a group of 6 fighters and 2 bombers, and dicesides is 6, and attack was 1, then we would want 1 fighter to die for sure. this is what groupsize is for.
			// if the attack is greater than 1 though, and all use the same attack power, then the group size can be smaller (ie: attack is 2, and we have 3 fighters and 2 bombers, we would want 1 fighter to die for sure).
			// categorize with groupSize
			final Tuple<List<List<Unit>>, List<Unit>> airSplit = categorizeLowLuckAirUnits(planesList, location, chosenDiceSize, groupSize);
			// the non rolling air units
			// if we are less hits than the number of groups, OR we have equal hits to number of groups but we also have a remainder that is equal to or greater than group size,
			// THEN we need to make sure to pick randomly, and include the remainder group. (reason we do not do this with any remainder size, is because we might have missed the dice roll to hit the remainder)
			if (hitsLeft < (airSplit.getFirst().size() + ((int) Math.ceil((double) airSplit.getSecond().size() / (double) groupSize))))
			{
				// fewer hits than groups.
				final List<Unit> tempPossibleHitUnits = new ArrayList<Unit>();
				for (final List<Unit> group : airSplit.getFirst())
				{
					tempPossibleHitUnits.add(group.get(0));
				}
				if (airSplit.getSecond().size() > 0)
				{
					// if we have a remainder group, we need to add some of them into the mix
					// but we have to do so randomly.
					final List<Unit> remainders = new ArrayList<Unit>(airSplit.getSecond());
					if (remainders.size() == 1)
					{
						tempPossibleHitUnits.add(remainders.remove(0));
					}
					else
					{
						final int numberOfRemainderGroups = (int) Math.ceil((double) remainders.size() / (double) groupSize);
						final int[] randomRemainder = bridge.getRandom(remainders.size(), numberOfRemainderGroups, null, DiceType.ENGINE, "Deciding which planes should die due to AA fire");
						int pos2 = 0;
						for (int i = 0; i < randomRemainder.length; i++)
						{
							pos2 += randomRemainder[i];
							tempPossibleHitUnits.add(remainders.remove(pos2 % remainders.size()));
						}
					}
				}
				final int[] hitRandom = bridge.getRandom(tempPossibleHitUnits.size(), hitsLeft, null, DiceType.ENGINE, "Deciding which planes should die due to AA fire");
				// now we find the
				int pos = 0;
				for (int i = 0; i < hitRandom.length; i++)
				{
					pos += hitRandom[i];
					final Unit unitHit = tempPossibleHitUnits.remove(pos % tempPossibleHitUnits.size());
					if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(), unitHit) < (getTotalHitpointsLeft(unitHit) - 1)))
					{
						finalCasualtyDetails.addToDamaged(unitHit);
					}
					else
					{
						finalCasualtyDetails.addToKilled(unitHit);
					}
				}
				hitsLeft = 0;
			}
			else
			{
				// kill one in every group
				for (final List<Unit> group : airSplit.getFirst())
				{
					final Unit unitHit = group.get(0);
					if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(), unitHit) < (getTotalHitpointsLeft(unitHit) - 1)))
					{
						finalCasualtyDetails.addToDamaged(unitHit);
					}
					else
					{
						finalCasualtyDetails.addToKilled(unitHit);
					}
					hitsLeft--;
				}
				// for any hits left over...
				if (hitsLeft == airSplit.getSecond().size())
				{
					for (final Unit unitHit : airSplit.getSecond())
					{
						if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(), unitHit) < (getTotalHitpointsLeft(unitHit) - 1)))
						{
							finalCasualtyDetails.addToDamaged(unitHit);
						}
						else
						{
							finalCasualtyDetails.addToKilled(unitHit);
						}
					}
					hitsLeft = 0;
				}
				else if (hitsLeft != 0)
				{
					// the remainder
					// roll all at once to prevent frequent random calls, important for pbem games
					final int[] hitRandom = bridge.getRandom(airSplit.getSecond().size(), hitsLeft, null, DiceType.ENGINE, "Deciding which planes should die due to AA fire");
					int pos = 0;
					for (int i = 0; i < hitRandom.length; i++)
					{
						pos += hitRandom[i];
						final Unit unitHit = airSplit.getSecond().remove(pos % airSplit.getSecond().size());
						if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(), unitHit) < (getTotalHitpointsLeft(unitHit) - 1)))
						{
							finalCasualtyDetails.addToDamaged(unitHit);
						}
						else
						{
							finalCasualtyDetails.addToKilled(unitHit);
						}
					}
					hitsLeft = 0;
				}
			}
		}
		// double check
		if (finalCasualtyDetails.size() != dice.getHits())
		{
			throw new IllegalStateException("wrong number of casulaties, expected:" + dice + " but got: " + finalCasualtyDetails);
		}
		return finalCasualtyDetails;
	}
	
	/**
	 * Choose plane casualties randomly
	 */
	public static CasualtyDetails RandomAACasualties(final Collection<Unit> planes, final DiceRoll dice, final IDelegateBridge bridge, final boolean allowMultipleHitsPerUnit)
	{
		{
			final Set<Unit> duplicatesCheckSet1 = new HashSet<Unit>(planes);
			if (planes.size() != duplicatesCheckSet1.size())
			{
				throw new IllegalStateException("Duplicate Units Detected: Original List:" + planes + "  HashSet:" + duplicatesCheckSet1);
			}
		}
		final int hitsLeft = dice.getHits();
		if (hitsLeft <= 0)
			return new CasualtyDetails();
		final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
		final int planeHP = (allowMultipleHitsPerUnit ? getTotalHitpointsLeft(planes) : planes.size()); // normal behavior is instant kill, which means planes.size()
		final List<Unit> planesList = new ArrayList<Unit>();
		for (final Unit plane : planes)
		{
			final int hpLeft = allowMultipleHitsPerUnit ? (UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits()) :
						(Math.min(1, UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits()));
			for (int hp = 0; hp < hpLeft; ++hp)
			{
				planesList.add(plane); // if allowMultipleHitsPerUnit, then because the number of rolls exactly equals the hitpoints of all units, we roll multiple times for any unit with multiple hitpoints
			}
		}
		// We need to choose which planes die randomly
		if (hitsLeft < planeHP)
		{
			// roll all at once to prevent frequent random calls, important for pbem games
			final int[] hitRandom = bridge.getRandom(planeHP, hitsLeft, null, DiceType.ENGINE, "Deciding which planes should die due to AA fire");
			int pos = 0;
			for (int i = 0; i < hitRandom.length; i++)
			{
				pos += hitRandom[i];
				final Unit unitHit = planesList.remove(pos % planesList.size());
				if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(), unitHit) < (getTotalHitpointsLeft(unitHit) - 1)))
				{
					finalCasualtyDetails.addToDamaged(unitHit);
				}
				else
				{
					finalCasualtyDetails.addToKilled(unitHit);
				}
			}
		}
		else
		{
			for (final Unit plane : planesList)
			{
				if (finalCasualtyDetails.getKilled().contains(plane))
				{
					finalCasualtyDetails.addToDamaged(plane);
				}
				else
				{
					finalCasualtyDetails.addToKilled(plane);
				}
			}
		}
		return finalCasualtyDetails;
	}
	
	/**
	 * Choose plane casualties based on individual AA shots at each aircraft.
	 */
	public static CasualtyDetails IndividuallyFiredAACasualties(final boolean defending, final Collection<Unit> planes, final Collection<Unit> defendingAA, final DiceRoll dice,
				final Territory location, final IDelegateBridge bridge, final boolean allowMultipleHitsPerUnit)
	{
		// if we have aa guns that are not infinite, then we need to randomly decide the aa casualties since there are not enough rolls to have a single roll for each aircraft, or too many rolls
		final int planeHP = (allowMultipleHitsPerUnit ? getTotalHitpointsLeft(planes) : planes.size()); // normal behavior is instant kill, which means planes.size()
		if (DiceRoll.getTotalAAattacks(defendingAA, planes, bridge.getData()) != planeHP)
			return RandomAACasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
		final Triple<Integer, Integer, Boolean> triple = DiceRoll.getTotalAAPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(null, null, !defending, defendingAA, planes, bridge.getData(), false);
		final boolean allSameAttackPower = triple.getThird();
		if (!allSameAttackPower)
			return RandomAACasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
		final Tuple<Integer, Integer> attackThenDiceSides = DiceRoll.getAAattackAndMaxDiceSides(defendingAA, bridge.getData(), !defending);
		final int highestAttack = attackThenDiceSides.getFirst();
		// int chosenDiceSize = attackThenDiceSides[1];
		final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
		final int hits = dice.getHits();
		final List<Unit> planesList = new ArrayList<Unit>();
		for (final Unit plane : planes)
		{
			final int hpLeft = allowMultipleHitsPerUnit ? (UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits()) :
						(Math.min(1, UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits()));
			for (int hp = 0; hp < hpLeft; ++hp)
			{
				planesList.add(plane); // if allowMultipleHitsPerUnit, then because the number of rolls exactly equals the hitpoints of all units, we roll multiple times for any unit with multiple hitpoints
			}
		}
		// We need to choose which planes die based on their position in the list and the individual AA rolls
		if (hits > planeHP)
			throw new IllegalStateException("Can not have more hits than number of die rolls");
		if (hits < planeHP)
		{
			final List<Die> rolls = dice.getRolls(highestAttack);
			for (int i = 0; i < rolls.size(); i++)
			{
				final Die die = rolls.get(i);
				if (die.getType() == DieType.HIT)
				{
					final Unit unit = planesList.get(i);
					if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(), unit) < (getTotalHitpointsLeft(unit) - 1)))
					{
						finalCasualtyDetails.addToDamaged(unit);
					}
					else
					{
						finalCasualtyDetails.addToKilled(unit);
					}
				}
			}
		}
		else
		{
			for (final Unit plane : planesList)
			{
				if (finalCasualtyDetails.getKilled().contains(plane))
				{
					finalCasualtyDetails.addToDamaged(plane);
				}
				else
				{
					finalCasualtyDetails.addToKilled(plane);
				}
			}
		}
		return finalCasualtyDetails;
	}
	
	/**
	 * 
	 * @param battleID
	 *            may be null if we are not in a battle (eg, if this is an aa fire due to moving
	 */
	public static CasualtyDetails selectCasualties(final String step, final PlayerID player, final Collection<Unit> targetsToPickFrom, final Collection<Unit> friendlyUnits,
				final PlayerID enemyPlayer, final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers, final Territory battlesite,
				final Collection<TerritoryEffect> territoryEffects, final IDelegateBridge bridge, final String text, final DiceRoll dice, final boolean defending, final GUID battleID,
				final boolean headLess, final int extraHits, final boolean allowMultipleHitsPerUnit)
	{
		if (targetsToPickFrom.isEmpty())
			return new CasualtyDetails();
		if (!friendlyUnits.containsAll(targetsToPickFrom))
		{
			throw new IllegalStateException("friendlyUnits should but does not contain all units from targetsToPickFrom");
		}
		final GameData data = bridge.getData();
		final boolean isEditMode = BaseEditDelegate.getEditMode(data);
		ITripleaPlayer tripleaPlayer;
		if (player.isNull())
			tripleaPlayer = new WeakAI(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
		else
			tripleaPlayer = (ITripleaPlayer) bridge.getRemotePlayer(player);
		Map<Unit, Collection<Unit>> dependents;
		if (headLess)
			dependents = Collections.emptyMap();
		else
			dependents = getDependents(targetsToPickFrom, data);
		if (isEditMode && !headLess)
		{
			final CasualtyDetails editSelection = tripleaPlayer.selectCasualties(targetsToPickFrom, dependents, 0, text, dice, player, friendlyUnits, enemyPlayer, enemyUnits, amphibious,
						amphibiousLandAttackers, new CasualtyList(), battleID, battlesite, allowMultipleHitsPerUnit);
			List<Unit> killed = editSelection.getKilled();
			// if partial retreat is possible, kill amphibious units first
			if (isPartialAmphibiousRetreat(data))
				killed = killAmphibiousFirst(killed, targetsToPickFrom);
			return editSelection;
		}
		if (dice.getHits() == 0)
			return new CasualtyDetails(Collections.<Unit> emptyList(), Collections.<Unit> emptyList(), true);
		int hitsRemaining = dice.getHits();
		if (isTransportCasualtiesRestricted(data))
		{
			hitsRemaining = extraHits;
		}
		if (!isEditMode && allTargetsOneTypeOneHitPoint(targetsToPickFrom, dependents))
		{
			final List<Unit> killed = new ArrayList<Unit>();
			final Iterator<Unit> iter = targetsToPickFrom.iterator();
			for (int i = 0; i < hitsRemaining; i++)
			{
				if (i >= targetsToPickFrom.size())
					break;
				killed.add(iter.next());
			}
			return new CasualtyDetails(killed, Collections.<Unit> emptyList(), true);
		}
		// Create production cost map, Maybe should do this elsewhere, but in case prices change, we do it here.
		final IntegerMap<UnitType> costs = getCostsForTUV(player, data);
		final Tuple<CasualtyList, List<Unit>> defaultCasualtiesAndSortedTargets = getDefaultCasualties(targetsToPickFrom, hitsRemaining, defending, player, friendlyUnits, enemyPlayer, enemyUnits,
					amphibious, amphibiousLandAttackers, battlesite, costs, territoryEffects, data, allowMultipleHitsPerUnit, true);
		final CasualtyList defaultCasualties = defaultCasualtiesAndSortedTargets.getFirst();
		final List<Unit> sortedTargetsToPickFrom = defaultCasualtiesAndSortedTargets.getSecond();
		if (sortedTargetsToPickFrom.size() != targetsToPickFrom.size() || !targetsToPickFrom.containsAll(sortedTargetsToPickFrom) || !sortedTargetsToPickFrom.containsAll(targetsToPickFrom))
		{
			throw new IllegalStateException("sortedTargetsToPickFrom must contain the same units as targetsToPickFrom list");
		}
		final int totalHitpoints = (allowMultipleHitsPerUnit ? getTotalHitpointsLeft(sortedTargetsToPickFrom) : sortedTargetsToPickFrom.size());
		final CasualtyDetails casualtySelection;
		if (hitsRemaining >= totalHitpoints)
		{
			casualtySelection = new CasualtyDetails(defaultCasualties, true);
		}
		else
		{
			casualtySelection = tripleaPlayer.selectCasualties(sortedTargetsToPickFrom, dependents, hitsRemaining, text, dice, player, friendlyUnits, enemyPlayer, enemyUnits, amphibious,
						amphibiousLandAttackers, defaultCasualties, battleID, battlesite, allowMultipleHitsPerUnit);
		}
		List<Unit> killed = casualtySelection.getKilled();
		// if partial retreat is possible, kill amphibious units first
		if (isPartialAmphibiousRetreat(data))
			killed = killAmphibiousFirst(killed, sortedTargetsToPickFrom);
		final List<Unit> damaged = casualtySelection.getDamaged();
		int numhits = killed.size();
		if (!allowMultipleHitsPerUnit)
			damaged.clear();
		else
		{
			for (final Unit unit : killed)
			{
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				final int damageToUnit = Collections.frequency(damaged, unit);
				numhits += Math.max(0, Math.min(damageToUnit, (ua.getHitPoints() - (1 + unit.getHits())))); // allowed damage
				final Iterator<Unit> iter = damaged.iterator();
				while (iter.hasNext())
				{
					if (unit.equals(iter.next()))
						iter.remove(); // remove from damaged list, since they will die
				}
			}
		}
		// check right number
		if (!isEditMode && !(numhits + damaged.size() == (hitsRemaining > totalHitpoints ? totalHitpoints : hitsRemaining)))
		{
			tripleaPlayer.reportError("Wrong number of casualties selected");
			if (headLess)
			{
				System.err.println("Possible Infinite Loop: Wrong number of casualties selected: number of hits on units " + (numhits + damaged.size()) + " != number of hits to take "
							+ (hitsRemaining > totalHitpoints ? totalHitpoints : hitsRemaining) + ", for " + casualtySelection.toString());
			}
			return selectCasualties(step, player, sortedTargetsToPickFrom, friendlyUnits, enemyPlayer, enemyUnits, amphibious, amphibiousLandAttackers, battlesite, territoryEffects, bridge, text,
						dice, defending, battleID, headLess, extraHits, allowMultipleHitsPerUnit);
		}
		// check we have enough of each type
		if (!sortedTargetsToPickFrom.containsAll(killed) || !sortedTargetsToPickFrom.containsAll(damaged))
		{
			tripleaPlayer.reportError("Cannot remove enough units of those types");
			if (headLess)
			{
				System.err.println("Possible Infinite Loop: Cannot remove enough units of those types: targets " + MyFormatter.unitsToTextNoOwner(sortedTargetsToPickFrom) + ", for "
							+ casualtySelection.toString());
			}
			return selectCasualties(step, player, sortedTargetsToPickFrom, friendlyUnits, enemyPlayer, enemyUnits, amphibious, amphibiousLandAttackers, battlesite, territoryEffects, bridge, text,
						dice, defending, battleID, headLess, extraHits, allowMultipleHitsPerUnit);
		}
		return casualtySelection;
	}
	
	private static List<Unit> killAmphibiousFirst(final List<Unit> killed, final Collection<Unit> targets)
	{
		final Collection<Unit> allAmphibUnits = new ArrayList<Unit>();
		final Collection<Unit> killedNonAmphibUnits = new ArrayList<Unit>();
		final Collection<UnitType> amphibTypes = new ArrayList<UnitType>();
		// Get a list of all selected killed units that are NOT amphibious
		final Match<Unit> aMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitWasNotAmphibious);
		killedNonAmphibUnits.addAll(Match.getMatches(killed, aMatch));
		// If all killed units are amphibious, just return them
		if (killedNonAmphibUnits.isEmpty())
			return killed;
		// Get a list of all units that are amphibious and remove those that are killed
		allAmphibUnits.addAll(Match.getMatches(targets, Matches.UnitWasAmphibious));
		allAmphibUnits.removeAll(Match.getMatches(killed, Matches.UnitWasAmphibious));
		final Iterator<Unit> allAmphibUnitsIter = allAmphibUnits.iterator();
		// Get a collection of the unit types of the amphib units
		while (allAmphibUnitsIter.hasNext())
		{
			final Unit unit = allAmphibUnitsIter.next();
			final UnitType ut = unit.getType();
			if (!amphibTypes.contains(ut))
				amphibTypes.add(ut);
		}
		// For each killed unit- see if there is an amphib unit that can be killed instead
		for (final Unit unit : killedNonAmphibUnits)
		{
			if (amphibTypes.contains(unit.getType()))
			{ // add a unit from the collection
				final List<Unit> oneAmphibUnit = Match.getNMatches(allAmphibUnits, 1, Matches.unitIsOfType(unit.getType()));
				if (oneAmphibUnit.size() > 0)
				{
					final Unit amphibUnit = oneAmphibUnit.iterator().next();
					killed.remove(unit);
					killed.add(amphibUnit);
					allAmphibUnits.remove(amphibUnit);
					continue;
				}
				else
				// If there are no more units of that type, remove the type from the collection
				{
					amphibTypes.remove(unit.getType());
				}
			}
		}
		return killed;
	}
	
	/*
	private static boolean s_enableCasualtySortingCaching = false;
	
	public static void EnableCasualtySortingCaching()
	{
		s_enableCasualtySortingCaching = true;
	}
	
	public static void DisableCasualtySortingCaching()
	{
		s_enableCasualtySortingCaching = false;
		synchronized (s_cachedLock)
		{
			s_cachedSortedCasualties.clear(); // Don't keep all this stuff in memory (basically, we just want this caching so if a battle is simulated 5000 times, we only sort units once)
		}
	}
	
	// Key is the hash of the possible casualties collection[targets], value is the cached sorted result[perfectlySortedUnitsList]
	private static HashMap<Integer, List<Unit>> s_cachedSortedCasualties = new HashMap<Integer, List<Unit>>();
	private static final Object s_cachedLock = new Object();
	*/

	/**
	 * A unit with two hitpoints will be listed twice if they will die. The first time they are listed it is as damaged. The second time they are listed, it is dead.
	 * 
	 * @param targets
	 * @param hits
	 * @param defending
	 * @param player
	 * @param costs
	 * @param data
	 * @return
	 */
	private static Tuple<CasualtyList, List<Unit>> getDefaultCasualties(final Collection<Unit> targetsToPickFrom, final int hits, final boolean defending, final PlayerID player,
				final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer, final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
				final Territory battlesite, final IntegerMap<UnitType> costs, final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean allowMultipleHitsPerUnit,
				final boolean bonus)
	{
		final CasualtyList defaultCasualtySelection = new CasualtyList();
		// Sort units by power and cost in ascending order
		final List<Unit> sorted = sortUnitsForCasualtiesWithSupport(targetsToPickFrom, hits, defending, player, friendlyUnits, enemyPlayer, enemyUnits, amphibious,
					amphibiousLandAttackers, battlesite, costs, territoryEffects, data, allowMultipleHitsPerUnit, bonus);
		// Remove two hit bb's selecting them first for default casualties
		int numSelectedCasualties = 0;
		if (allowMultipleHitsPerUnit)
		{
			for (final Unit unit : sorted)
			{
				// Stop if we have already selected as many hits as there are targets
				if (numSelectedCasualties >= hits)
				{
					return new Tuple<CasualtyList, List<Unit>>(defaultCasualtySelection, sorted);
				}
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				final int extraHP = Math.min((hits - numSelectedCasualties), (ua.getHitPoints() - (1 + unit.getHits())));
				for (int i = 0; i < extraHP; i++)
				{
					numSelectedCasualties++;
					defaultCasualtySelection.addToDamaged(unit);
				}
			}
		}
		// Select units
		for (final Unit unit : sorted)
		{
			// Stop if we have already selected as many hits as there are targets
			if (numSelectedCasualties >= hits)
			{
				return new Tuple<CasualtyList, List<Unit>>(defaultCasualtySelection, sorted);
			}
			defaultCasualtySelection.addToKilled(unit);
			numSelectedCasualties++;
		}
		return new Tuple<CasualtyList, List<Unit>>(defaultCasualtySelection, sorted);
	}
	
	/**
	 * The purpose of this is to return a list in the PERFECT order of which units should be selected to die first,
	 * And that means that certain units MUST BE INTERLEAVED.
	 * This list assumes that you have already taken any extra hit points away from any 2 hitpoint units.
	 * Example: You have a 1 attack Artillery unit that supports, and a 1 attack infantry unit that can receive support.
	 * The best selection of units to die is first to take whichever unit has excess, then cut that down til they are both the same size,
	 * then to take 1 artillery followed by 1 infantry, followed by 1 artillery, then 1 inf, etc, until everyone is dead.
	 * If you just return all infantry followed by all artillery, or the other way around, you will be missing out on some important support provided.
	 * (Veqryn)
	 */
	public static List<Unit> sortUnitsForCasualtiesWithSupport(final Collection<Unit> targetsToPickFrom, final int hits, final boolean defending, final PlayerID player,
				final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer, final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
				final Territory battlesite, final IntegerMap<UnitType> costs, final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean allowMultipleHitsPerUnit,
				final boolean bonus)
	{
		/*
		if (s_enableCasualtySortingCaching)
		{
			synchronized (s_cachedLock)
			{
				//if (s_cachedSortedCasualties.get(targets.hashCode()).isEmpty() || !s_cachedSortedCasualties.get(targets.hashCode()).containsAll(targets)
				//    		|| !targets.containsAll(s_cachedSortedCasualties.get(targets.hashCode())) || s_cachedSortedCasualties.get(targets.hashCode()).size() != targets.size())
				//	s_cachedSortedCasualties.clear();
				//else
				if (s_cachedSortedCasualties.containsKey(targets.hashCode()))
					return s_cachedSortedCasualties.get(targets.hashCode());
			}
		}
		*/
		if (!GameRunner2.getCasualtySelectionSlow())
		{
			return sortUnitsForCasualtiesWithSupportNewWithCaching(targetsToPickFrom, hits, defending, player, friendlyUnits, enemyPlayer, enemyUnits, amphibious, amphibiousLandAttackers, battlesite,
						costs, territoryEffects, data, allowMultipleHitsPerUnit, bonus);
		}
		else
		{
			return sortUnitsForCasualtiesWithSupportBruteForce(targetsToPickFrom, hits, defending, player, friendlyUnits, enemyPlayer, enemyUnits, amphibious, amphibiousLandAttackers, battlesite,
						costs, territoryEffects, data, allowMultipleHitsPerUnit, bonus);
		}
		/*
		if (s_enableCasualtySortingCaching)
		{
			synchronized (s_cachedLock)
			{
				if (!s_cachedSortedCasualties.containsKey(targets.hashCode()))
					s_cachedSortedCasualties.put(targets.hashCode(), perfectlySortedUnitsList);
			}
		}
		*/
	}
	
	private static List<Unit> sortUnitsForCasualtiesWithSupportNewWithCaching(final Collection<Unit> targetsToPickFrom, final int hits, final boolean defending, final PlayerID player,
				final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer, final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
				final Territory battlesite, final IntegerMap<UnitType> costs, final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean allowMultipleHitsPerUnit,
				final boolean bonus)
	{
		// Convert unit lists to unit type lists
		final List<UnitType> targetTypes = new ArrayList<UnitType>();
		for (final Unit u : targetsToPickFrom)
			targetTypes.add(u.getType());
		final List<UnitType> amphibTypes = new ArrayList<UnitType>();
		for (final Unit u : amphibiousLandAttackers)
			amphibTypes.add(u.getType());
		
		// Calculate hashes and cache key
		int targetsHashCode = 1;
		for (final UnitType ut : targetTypes)
			targetsHashCode += ut.hashCode();
		targetsHashCode *= 31;
		int amphibHashCode = 1;
		for (final UnitType ut : amphibTypes)
			amphibHashCode += ut.hashCode();
		amphibHashCode *= 31;
		String key = player.getName() + "|" + battlesite.getName() + "|" + defending + "|" + amphibious + "|" + targetsHashCode + "|" + amphibHashCode;
		
		// Check OOL cache
		final List<UnitType> stored = oolCache.get(key);
		if (stored != null)
		{
			// System.out.println("Hit with cacheSize=" + oolCache.size() + ", key=" + key);
			final List<Unit> result = new ArrayList<Unit>();
			final List<Unit> selectFrom = new ArrayList<Unit>(targetsToPickFrom);
			for (final UnitType ut : stored)
			{
				for (final Iterator<Unit> it = selectFrom.iterator(); it.hasNext();)
				{
					final Unit u = it.next();
					if (ut.equals(u.getType()))
					{
						result.add(u);
						it.remove();
					}
				}
			}
			return result;
		}
		// System.out.println("Miss with cacheSize=" + oolCache.size() + ", key=" + key);
		
		// Sort enough units to kill off
		final List<Unit> sortedUnitsList = new ArrayList<Unit>(targetsToPickFrom);
		Collections.sort(sortedUnitsList, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
		Collections.reverse(sortedUnitsList); // Sort units starting with strongest so that support gets added to them first
		final UnitBattleComparator unitComparatorWithoutPrimaryPower = new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, true);
		final List<Unit> sortedWellEnoughUnitsList = new ArrayList<Unit>();
		final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<Unit, IntegerMap<Unit>>();
		final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<Unit, IntegerMap<Unit>>();
		final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap = DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, sortedUnitsList, new ArrayList<Unit>(enemyUnits),
					defending, false, player, data, battlesite, territoryEffects, amphibious, amphibiousLandAttackers, unitSupportPowerMap, unitSupportRollsMap);
		
		Collections.reverse(sortedUnitsList); // Sort units starting with weakest for finding the worst units
		for (int i = 0; i < sortedUnitsList.size(); ++i)
		{
			// Loop through all target units to find the best unit to take as casualty
			Unit worstUnit = null;
			int minPower = Integer.MAX_VALUE;
			final Set<UnitType> unitTypes = new HashSet<UnitType>();
			for (final Unit u : sortedUnitsList)
			{
				if (unitTypes.contains(u.getType()))
					continue;
				unitTypes.add(u.getType());
				
				// Find unit power
				final Map<Unit, Tuple<Integer, Integer>> currentUnitMap = new HashMap<Unit, Tuple<Integer, Integer>>();
				currentUnitMap.put(u, unitPowerAndRollsMap.get(u));
				int power = DiceRoll.getTotalPowerAndRolls(currentUnitMap, data).getFirst();
				
				// Add any support power that it provides to other units
				final IntegerMap<Unit> unitSupportPowerMapForUnit = unitSupportPowerMap.get(u);
				if (unitSupportPowerMapForUnit != null)
				{
					for (final Unit supportedUnit : unitSupportPowerMapForUnit.keySet())
					{
						Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
						if (strengthAndRolls == null)
							continue;
						
						// Remove any rolls provided by this support so they aren't counted twice
						final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(u);
						if (unitSupportRollsMapForUnit != null)
							strengthAndRolls = new Tuple<Integer, Integer>(strengthAndRolls.getFirst(), strengthAndRolls.getSecond() - unitSupportRollsMapForUnit.getInt(supportedUnit));
						
						// If one roll then just add the power
						if (strengthAndRolls.getSecond() == 1)
						{
							power += unitSupportPowerMapForUnit.getInt(supportedUnit);
							continue;
						}
						
						// Find supported unit power with support
						final Map<Unit, Tuple<Integer, Integer>> supportedUnitMap = new HashMap<Unit, Tuple<Integer, Integer>>();
						supportedUnitMap.put(supportedUnit, strengthAndRolls);
						final int powerWithSupport = DiceRoll.getTotalPowerAndRolls(supportedUnitMap, data).getFirst();
						
						// Find supported unit power without support
						final int strengthWithoutSupport = strengthAndRolls.getFirst() - unitSupportPowerMapForUnit.getInt(supportedUnit);
						final Tuple<Integer, Integer> strengthAndRollsWithoutSupport = new Tuple<Integer, Integer>(strengthWithoutSupport, strengthAndRolls.getSecond());
						supportedUnitMap.put(supportedUnit, strengthAndRollsWithoutSupport);
						final int powerWithoutSupport = DiceRoll.getTotalPowerAndRolls(supportedUnitMap, data).getFirst();
						
						// Add the actual power provided by the support
						final int addedPower = powerWithSupport - powerWithoutSupport;
						power += addedPower;
					}
				}
				
				// Add any power from support rolls that it provides to other units
				final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(u);
				if (unitSupportRollsMapForUnit != null)
				{
					for (final Unit supportedUnit : unitSupportRollsMapForUnit.keySet())
					{
						final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
						if (strengthAndRolls == null)
							continue;
						
						// Find supported unit power with support
						final Map<Unit, Tuple<Integer, Integer>> supportedUnitMap = new HashMap<Unit, Tuple<Integer, Integer>>();
						supportedUnitMap.put(supportedUnit, strengthAndRolls);
						final int powerWithSupport = DiceRoll.getTotalPowerAndRolls(supportedUnitMap, data).getFirst();
						
						// Find supported unit power without support
						final int rollsWithoutSupport = strengthAndRolls.getSecond() - unitSupportRollsMap.get(u).getInt(supportedUnit);
						final Tuple<Integer, Integer> strengthAndRollsWithoutSupport = new Tuple<Integer, Integer>(strengthAndRolls.getFirst(), rollsWithoutSupport);
						supportedUnitMap.put(supportedUnit, strengthAndRollsWithoutSupport);
						final int powerWithoutSupport = DiceRoll.getTotalPowerAndRolls(supportedUnitMap, data).getFirst();
						
						// Add the actual power provided by the support
						final int addedPower = powerWithSupport - powerWithoutSupport;
						power += addedPower;
					}
				}
				
				// Check if unit has lower power
				if (power < minPower || (power == minPower && unitComparatorWithoutPrimaryPower.compare(u, worstUnit) < 0))
				{
					worstUnit = u;
					minPower = power;
				}
			}
			
			// Add worst unit to sorted list, update any units it supported, and remove from other collections
			final IntegerMap<Unit> unitSupportPowerMapForUnit = unitSupportPowerMap.get(worstUnit);
			if (unitSupportPowerMapForUnit != null)
			{
				for (final Unit supportedUnit : unitSupportPowerMapForUnit.keySet())
				{
					final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
					if (strengthAndRolls == null)
						continue;
					
					final int strengthWithoutSupport = strengthAndRolls.getFirst() - unitSupportPowerMapForUnit.getInt(supportedUnit);
					final Tuple<Integer, Integer> strengthAndRollsWithoutSupport = new Tuple<Integer, Integer>(strengthWithoutSupport, strengthAndRolls.getSecond());
					unitPowerAndRollsMap.put(supportedUnit, strengthAndRollsWithoutSupport);
					sortedUnitsList.remove(supportedUnit);
					sortedUnitsList.add(0, supportedUnit);
				}
			}
			final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(worstUnit);
			if (unitSupportRollsMapForUnit != null)
			{
				for (final Unit supportedUnit : unitSupportRollsMapForUnit.keySet())
				{
					final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
					if (strengthAndRolls == null)
						continue;
					
					final int rollsWithoutSupport = strengthAndRolls.getSecond() - unitSupportRollsMapForUnit.getInt(supportedUnit);
					final Tuple<Integer, Integer> strengthAndRollsWithoutSupport = new Tuple<Integer, Integer>(strengthAndRolls.getFirst(), rollsWithoutSupport);
					unitPowerAndRollsMap.put(supportedUnit, strengthAndRollsWithoutSupport);
					sortedUnitsList.remove(supportedUnit);
					sortedUnitsList.add(0, supportedUnit);
				}
			}
			sortedWellEnoughUnitsList.add(worstUnit);
			sortedUnitsList.remove(worstUnit);
			unitPowerAndRollsMap.remove(worstUnit);
			unitSupportPowerMap.remove(worstUnit);
			unitSupportRollsMap.remove(worstUnit);
		}
		sortedWellEnoughUnitsList.addAll(sortedUnitsList);
		
		// Cache result and all subsets of the result
		final List<UnitType> unitTypes = new ArrayList<UnitType>();
		for (final Unit u : sortedWellEnoughUnitsList)
			unitTypes.add(u.getType());
		for (final Iterator<UnitType> it = unitTypes.iterator(); it.hasNext();)
		{
			oolCache.put(key, new ArrayList<UnitType>(unitTypes));
			final UnitType unitTypeToRemove = it.next();
			targetTypes.remove(unitTypeToRemove);
			if (Collections.frequency(targetTypes, unitTypeToRemove) < Collections.frequency(amphibTypes, unitTypeToRemove))
				amphibTypes.remove(unitTypeToRemove);
			targetsHashCode = 1;
			for (final UnitType ut : targetTypes)
				targetsHashCode += ut.hashCode();
			targetsHashCode *= 31;
			amphibHashCode = 1;
			for (final UnitType ut : amphibTypes)
				amphibHashCode += ut.hashCode();
			amphibHashCode *= 31;
			key = player.getName() + "|" + battlesite.getName() + "|" + defending + "|" + amphibious + "|" + targetsHashCode + "|" + amphibHashCode;
			it.remove();
		}
		
		return sortedWellEnoughUnitsList;
	}
	
	private static List<Unit> sortUnitsForCasualtiesWithSupportNew(final Collection<Unit> targetsToPickFrom, final int hits, final boolean defending, final PlayerID player,
				final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer, final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
				final Territory battlesite, final IntegerMap<UnitType> costs, final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean allowMultipleHitsPerUnit,
				final boolean bonus)
	{
		final List<Unit> sortedUnitsList = new ArrayList<Unit>(targetsToPickFrom);
		Collections.sort(sortedUnitsList, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
		
		// Determine if there are enough extra hit points or if all units are going to be killed
		int numberOfUnitsWeMustSort = hits;
		int extraHP = 0;
		if (allowMultipleHitsPerUnit)
		{
			for (final Unit unit : sortedUnitsList)
			{
				extraHP += Math.max(0, UnitAttachment.get(unit.getType()).getHitPoints() - (1 + unit.getHits()));
				if (extraHP >= hits)
				{
					return sortedUnitsList; // No units will be killed as we have enough extra hp, so who cares about doing the time-expensive sort when the UnitBattleComparator is good enough
				}
			}
			numberOfUnitsWeMustSort = Math.max(extraHP, hits - extraHP); // If we have to take 6 hits, and we can damage 2 units, then we really only have to sort for the first 4 units to die (if we can damage 4 units, then we still have to sort those first 4 in case one of them will have to die)
		}
		if (hits > extraHP + sortedUnitsList.size())
		{
			return sortedUnitsList; // If we are going to lose all units, just return this list
		}
		
		// Sort enough units to kill off
		Collections.reverse(sortedUnitsList); // Sort units starting with strongest so that support gets added to them first
		final UnitBattleComparator unitComparatorWithoutPrimaryPower = new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, true);
		final List<Unit> sortedWellEnoughUnitsList = new ArrayList<Unit>();
		final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<Unit, IntegerMap<Unit>>();
		final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<Unit, IntegerMap<Unit>>();
		final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap = DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, sortedUnitsList, new ArrayList<Unit>(enemyUnits),
					defending, false, player, data, battlesite, territoryEffects, amphibious, amphibiousLandAttackers, unitSupportPowerMap, unitSupportRollsMap);
		
		Collections.reverse(sortedUnitsList); // Sort units starting with weakest for finding the worst units
		for (int i = 0; i < numberOfUnitsWeMustSort; ++i)
		{
			// Loop through all target units to find the best unit to take as casualty
			Unit worstUnit = null;
			int minPower = Integer.MAX_VALUE;
			final Set<UnitType> unitTypes = new HashSet<UnitType>();
			for (final Unit u : sortedUnitsList)
			{
				if (unitTypes.contains(u.getType()))
					continue;
				unitTypes.add(u.getType());
				
				// Find unit power
				final Map<Unit, Tuple<Integer, Integer>> currentUnitMap = new HashMap<Unit, Tuple<Integer, Integer>>();
				currentUnitMap.put(u, unitPowerAndRollsMap.get(u));
				int power = DiceRoll.getTotalPowerAndRolls(currentUnitMap, data).getFirst();
				
				// Add any support power that it provides to other units
				final IntegerMap<Unit> unitSupportPowerMapForUnit = unitSupportPowerMap.get(u);
				if (unitSupportPowerMapForUnit != null)
				{
					for (final Unit supportedUnit : unitSupportPowerMapForUnit.keySet())
					{
						Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
						if (strengthAndRolls == null)
							continue;
						
						// Remove any rolls provided by this support so they aren't counted twice
						final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(u);
						if (unitSupportRollsMapForUnit != null)
							strengthAndRolls = new Tuple<Integer, Integer>(strengthAndRolls.getFirst(), strengthAndRolls.getSecond() - unitSupportRollsMapForUnit.getInt(supportedUnit));
						
						// If one roll then just add the power
						if (strengthAndRolls.getSecond() == 1)
						{
							power += unitSupportPowerMapForUnit.getInt(supportedUnit);
							continue;
						}
						
						// Find supported unit power with support
						final Map<Unit, Tuple<Integer, Integer>> supportedUnitMap = new HashMap<Unit, Tuple<Integer, Integer>>();
						supportedUnitMap.put(supportedUnit, strengthAndRolls);
						final int powerWithSupport = DiceRoll.getTotalPowerAndRolls(supportedUnitMap, data).getFirst();
						
						// Find supported unit power without support
						final int strengthWithoutSupport = strengthAndRolls.getFirst() - unitSupportPowerMapForUnit.getInt(supportedUnit);
						final Tuple<Integer, Integer> strengthAndRollsWithoutSupport = new Tuple<Integer, Integer>(strengthWithoutSupport, strengthAndRolls.getSecond());
						supportedUnitMap.put(supportedUnit, strengthAndRollsWithoutSupport);
						final int powerWithoutSupport = DiceRoll.getTotalPowerAndRolls(supportedUnitMap, data).getFirst();
						
						// Add the actual power provided by the support
						final int addedPower = powerWithSupport - powerWithoutSupport;
						power += addedPower;
					}
				}
				
				// Add any power from support rolls that it provides to other units
				final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(u);
				if (unitSupportRollsMapForUnit != null)
				{
					for (final Unit supportedUnit : unitSupportRollsMapForUnit.keySet())
					{
						final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
						if (strengthAndRolls == null)
							continue;
						
						// Find supported unit power with support
						final Map<Unit, Tuple<Integer, Integer>> supportedUnitMap = new HashMap<Unit, Tuple<Integer, Integer>>();
						supportedUnitMap.put(supportedUnit, strengthAndRolls);
						final int powerWithSupport = DiceRoll.getTotalPowerAndRolls(supportedUnitMap, data).getFirst();
						
						// Find supported unit power without support
						final int rollsWithoutSupport = strengthAndRolls.getSecond() - unitSupportRollsMap.get(u).getInt(supportedUnit);
						final Tuple<Integer, Integer> strengthAndRollsWithoutSupport = new Tuple<Integer, Integer>(strengthAndRolls.getFirst(), rollsWithoutSupport);
						supportedUnitMap.put(supportedUnit, strengthAndRollsWithoutSupport);
						final int powerWithoutSupport = DiceRoll.getTotalPowerAndRolls(supportedUnitMap, data).getFirst();
						
						// Add the actual power provided by the support
						final int addedPower = powerWithSupport - powerWithoutSupport;
						power += addedPower;
					}
				}
				
				// Check if unit has lower power
				if (power < minPower || (power == minPower && unitComparatorWithoutPrimaryPower.compare(u, worstUnit) < 0))
				{
					worstUnit = u;
					minPower = power;
				}
			}
			
			// Add worst unit to sorted list, update any units it supported, and remove from other collections
			final IntegerMap<Unit> unitSupportPowerMapForUnit = unitSupportPowerMap.get(worstUnit);
			if (unitSupportPowerMapForUnit != null)
			{
				for (final Unit supportedUnit : unitSupportPowerMapForUnit.keySet())
				{
					final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
					if (strengthAndRolls == null)
						continue;
					
					final int strengthWithoutSupport = strengthAndRolls.getFirst() - unitSupportPowerMapForUnit.getInt(supportedUnit);
					final Tuple<Integer, Integer> strengthAndRollsWithoutSupport = new Tuple<Integer, Integer>(strengthWithoutSupport, strengthAndRolls.getSecond());
					unitPowerAndRollsMap.put(supportedUnit, strengthAndRollsWithoutSupport);
					sortedUnitsList.remove(supportedUnit);
					sortedUnitsList.add(0, supportedUnit);
				}
			}
			final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(worstUnit);
			if (unitSupportRollsMapForUnit != null)
			{
				for (final Unit supportedUnit : unitSupportRollsMapForUnit.keySet())
				{
					final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
					if (strengthAndRolls == null)
						continue;
					
					final int rollsWithoutSupport = strengthAndRolls.getSecond() - unitSupportRollsMapForUnit.getInt(supportedUnit);
					final Tuple<Integer, Integer> strengthAndRollsWithoutSupport = new Tuple<Integer, Integer>(strengthAndRolls.getFirst(), rollsWithoutSupport);
					unitPowerAndRollsMap.put(supportedUnit, strengthAndRollsWithoutSupport);
					sortedUnitsList.remove(supportedUnit);
					sortedUnitsList.add(0, supportedUnit);
				}
			}
			sortedWellEnoughUnitsList.add(worstUnit);
			sortedUnitsList.remove(worstUnit);
			unitPowerAndRollsMap.remove(worstUnit);
			unitSupportPowerMap.remove(worstUnit);
			unitSupportRollsMap.remove(worstUnit);
		}
		sortedWellEnoughUnitsList.addAll(sortedUnitsList);
		return sortedWellEnoughUnitsList;
	}
	
	private static List<Unit> sortUnitsForCasualtiesWithSupportBruteForce(final Collection<Unit> targetsToPickFrom, final int hits, final boolean defending, final PlayerID player,
				final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer, final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
				final Territory battlesite, final IntegerMap<UnitType> costs, final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean allowMultipleHitsPerUnit,
				final boolean bonus)
	{
		final List<Unit> sortedUnitsList = new ArrayList<Unit>(targetsToPickFrom);
		Collections.sort(sortedUnitsList, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
		
		// Select optimal units to kill
		int numberOfUnitsWeMustSort = hits;
		int extraHP = 0;
		if (allowMultipleHitsPerUnit)
		{
			for (final Unit unit : sortedUnitsList)
			{
				extraHP += Math.max(0, UnitAttachment.get(unit.getType()).getHitPoints() - (1 + unit.getHits()));
				if (extraHP >= hits)
				{
					return sortedUnitsList; // no units will be killed as we have enough extra hp, so who cares about doing the time-expensive sort when the UnitBattleComparator is good enough
				}
			}
			numberOfUnitsWeMustSort = Math.max(extraHP, hits - extraHP); // if we have to take 6 hits, and we can damage 2 units, then we really only have to sort for the first 4 units to die (if we can damage 4 units, then we still have to sort those first 4 in case one of them will have to die)
		}
		if (hits > extraHP + sortedUnitsList.size())
		{
			return sortedUnitsList; // if we are going to lose all units, just return this list
		}
		final UnitBattleComparator unitComparatorWithoutPrimaryPower = new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, true);
		final List<Unit> sortedWellEnoughUnitsList = new ArrayList<Unit>();
		for (int i = 0; i < numberOfUnitsWeMustSort; ++i)
		{
			// Loop through all target units to find the best unit to take as casualty
			Unit worstUnit = null;
			int maxPowerDifference = Integer.MIN_VALUE;
			final Set<UnitType> unitTypes = new HashSet<UnitType>();
			for (final Unit u : sortedUnitsList)
			{
				if (unitTypes.contains(u.getType()))
					continue; // Only check each unit type once
				unitTypes.add(u.getType());
				
				// Find my power without current unit
				final List<Unit> units = new ArrayList<Unit>(sortedUnitsList);
				units.remove(u);
				final List<Unit> enemyUnitList = new ArrayList<Unit>(enemyUnits);
				Collections.reverse(units);
				final int power = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(units, units, enemyUnitList, defending, false,
							player, data, battlesite, territoryEffects, amphibious, amphibiousLandAttackers), data).getFirst();
				
				// Find enemy power without current unit (need to consider this since supports can decrease enemy attack/defense)
				final int enemyPower = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(enemyUnitList, enemyUnitList, units, !defending, false,
							enemyPlayer, data, battlesite, territoryEffects, amphibious, amphibiousLandAttackers), data).getFirst();
				
				// Check if unit has higher power
				final int powerDifference = power - enemyPower;
				if (powerDifference > maxPowerDifference || (powerDifference == maxPowerDifference && unitComparatorWithoutPrimaryPower.compare(u, worstUnit) < 0))
				{
					worstUnit = u;
					maxPowerDifference = powerDifference;
				}
			}
			sortedUnitsList.remove(worstUnit);
			sortedWellEnoughUnitsList.add(worstUnit);
		}
		sortedWellEnoughUnitsList.addAll(sortedUnitsList);
		return sortedWellEnoughUnitsList;
	}
	
	/*
	private static List<Unit> sortUnitsForCasualtiesWithSupportLegacy(final Collection<Unit> targetsToPickFrom, final boolean defending, final PlayerID player, final IntegerMap<UnitType> costs,
				final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean bonus)
	{
		final List<Unit> sortedUnitsList = new ArrayList<Unit>(targetsToPickFrom);
		Collections.sort(sortedUnitsList, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
		final List<Unit> perfectlySortedUnitsList = new ArrayList<Unit>();
		int artillerySupportAvailable = DiceRoll.getArtillerySupportAvailable(sortedUnitsList, defending, player);
		int supportableAvailable = DiceRoll.getSupportableAvailable(sortedUnitsList, defending, player);
		if (artillerySupportAvailable == 0 || supportableAvailable == 0)
			return sortedUnitsList;
		// reset, as we don't want to count units which support themselves
		artillerySupportAvailable = 0;
		supportableAvailable = 0;
		final List<List<Unit>> unitsByPowerAll = new ArrayList<List<Unit>>();
		final List<List<Unit>> unitsByPowerBoth = new ArrayList<List<Unit>>();
		final List<List<Unit>> unitsByPowerGives = new ArrayList<List<Unit>>();
		final List<List<Unit>> unitsByPowerReceives = new ArrayList<List<Unit>>();
		final List<List<Unit>> unitsByPowerNone = new ArrayList<List<Unit>>();
		// Decide what is the biggest size for the lists that we will support
		final int maxDiceTimesRolls = 1 + 2 * data.getDiceSides();
		// Find what the biggest unit we have is. If they are bigger than maxDiceTimesRolls, set to maxDiceTimesRolls.
		final int maxPower = Math.max(0, Math.min(getUnitPowerForSorting(sortedUnitsList.get(sortedUnitsList.size() - 1), defending, data, territoryEffects), maxDiceTimesRolls));
		// Fill the lists with the six dice numbers (plus Zero, and any above if we have units with multiple rolls), or unit powers, which we will populate with the units
		for (int i = 0; i <= maxPower; i++)
		{
			unitsByPowerAll.add(new ArrayList<Unit>());
			unitsByPowerBoth.add(new ArrayList<Unit>());
			unitsByPowerGives.add(new ArrayList<Unit>());
			unitsByPowerReceives.add(new ArrayList<Unit>());
			unitsByPowerNone.add(new ArrayList<Unit>());
		}
		// in order to merge lists, we need to separate sortedUnitsList into multiple lists by power
		for (final Unit current : sortedUnitsList)
		{
			int unitPower = getUnitPowerForSorting(current, defending, data, territoryEffects);
			unitPower = Math.max(0, Math.min(unitPower, maxPower)); // getUnitPowerForSorting will return numbers over max_dice IF that units Power * DiceRolls goes over max_dice
			// TODO: if a unit supports itself, it should be in a different power list, as it will always support itself. getUnitPowerForSorting() should test for this and return a higher number.
			unitsByPowerAll.get(unitPower).add(current);
			if (UnitAttachment.get(current.getType()).getArtillery() && UnitAttachment.get(current.getType()).getArtillerySupportable())
				unitsByPowerBoth.get(unitPower).add(current);
			else if (UnitAttachment.get(current.getType()).getArtillery())
			{
				unitsByPowerGives.get(unitPower).add(current);
				artillerySupportAvailable += DiceRoll.getArtillerySupportAvailable(current, defending, player);
			}
			else if (UnitAttachment.get(current.getType()).getArtillerySupportable())
			{
				unitsByPowerReceives.get(unitPower).add(current);
				supportableAvailable += DiceRoll.getSupportableAvailable(current, defending, player);
			}
			else
				unitsByPowerNone.get(unitPower).add(current);
		}
		// now merge the lists
		final List<Unit> tempList1 = new ArrayList<Unit>();
		final List<Unit> tempList2 = new ArrayList<Unit>();
		for (int i = 0; i <= maxPower; i++)
		{
			int iArtillery = DiceRoll.getArtillerySupportAvailable(unitsByPowerGives.get(i), defending, player);
			int aboveArtillery = artillerySupportAvailable - iArtillery;
			artillerySupportAvailable -= iArtillery;
			int iSupportable = DiceRoll.getSupportableAvailable(unitsByPowerReceives.get(i), defending, player);
			int aboveSupportable = supportableAvailable - iSupportable;
			supportableAvailable -= iSupportable;
			if ((iArtillery == 0 && iSupportable == 0) || (iArtillery == 0 && aboveSupportable >= aboveArtillery) || ((iSupportable == 0 || iArtillery == 0) && aboveSupportable == aboveArtillery)
						|| (iSupportable == 0 && aboveSupportable <= aboveArtillery) || (i == maxDiceTimesRolls))
				perfectlySortedUnitsList.addAll(unitsByPowerAll.get(i));
			else
			{
				int count = 0;
				while (0 < unitsByPowerBoth.get(i).size() || 0 < unitsByPowerGives.get(i).size() || 0 < unitsByPowerReceives.get(i).size() || 0 < unitsByPowerNone.get(i).size())
				{
					count++;
					if (count > 100000)
						throw new IllegalStateException("Infinite loop in sortUnitsForCasualtiesWithSupport.");
					tempList1.clear();
					tempList2.clear();
					// four variables: we have artillery, we have support, above has artillery, above has support. need every combination covered.
					if (iArtillery == 0 && aboveArtillery - aboveSupportable > 0 && unitsByPowerReceives.get(i).size() > 0)
					{
						while (aboveArtillery - aboveSupportable > 0 && unitsByPowerReceives.get(i).size() > 0)
						{
							final int last = unitsByPowerReceives.get(i).size() - 1;
							tempList2.add(unitsByPowerReceives.get(i).get(last));
							aboveSupportable += DiceRoll.getSupportableAvailable(unitsByPowerReceives.get(i).get(last), defending, player);
							unitsByPowerReceives.get(i).remove(last);
						}
						tempList1.addAll(unitsByPowerNone.get(i));
						tempList1.addAll(unitsByPowerGives.get(i));
						tempList1.addAll(unitsByPowerReceives.get(i));
						tempList1.addAll(unitsByPowerBoth.get(i));
						unitsByPowerNone.get(i).clear();
						unitsByPowerGives.get(i).clear();
						unitsByPowerReceives.get(i).clear();
						unitsByPowerBoth.get(i).clear();
						Collections.sort(tempList1, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
						Collections.sort(tempList2, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
						perfectlySortedUnitsList.addAll(tempList1);
						perfectlySortedUnitsList.addAll(tempList2);
						continue;
					}
					if (iSupportable == 0 && aboveSupportable - aboveArtillery > 0 && unitsByPowerGives.get(i).size() > 0)
					{
						while (aboveSupportable - aboveArtillery > 0 && unitsByPowerGives.get(i).size() > 0)
						{
							final int last = unitsByPowerGives.get(i).size() - 1;
							tempList2.add(unitsByPowerGives.get(i).get(last));
							aboveArtillery += DiceRoll.getArtillerySupportAvailable(unitsByPowerGives.get(i).get(last), defending, player);
							unitsByPowerGives.get(i).remove(last);
						}
						tempList1.addAll(unitsByPowerNone.get(i));
						tempList1.addAll(unitsByPowerGives.get(i));
						tempList1.addAll(unitsByPowerReceives.get(i));
						tempList1.addAll(unitsByPowerBoth.get(i));
						unitsByPowerNone.get(i).clear();
						unitsByPowerGives.get(i).clear();
						unitsByPowerReceives.get(i).clear();
						unitsByPowerBoth.get(i).clear();
						Collections.sort(tempList1, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
						Collections.sort(tempList2, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
						perfectlySortedUnitsList.addAll(tempList1);
						perfectlySortedUnitsList.addAll(tempList2);
						continue;
					}
					if (iSupportable + aboveSupportable > iArtillery + aboveArtillery && unitsByPowerReceives.get(i).size() > 0)
					{
						while (iSupportable + aboveSupportable > iArtillery + aboveArtillery && unitsByPowerReceives.get(i).size() > 0)
						{
							final int first = 0;
							tempList1.add(unitsByPowerReceives.get(i).get(first));
							iSupportable -= DiceRoll.getSupportableAvailable(unitsByPowerReceives.get(i).get(first), defending, player);
							unitsByPowerReceives.get(i).remove(first);
						}
						tempList1.addAll(unitsByPowerNone.get(i));
						tempList1.addAll(unitsByPowerBoth.get(i));
						unitsByPowerNone.get(i).clear();
						unitsByPowerBoth.get(i).clear();
						Collections.sort(tempList1, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
						perfectlySortedUnitsList.addAll(tempList1);
						continue;
					}
					if (iSupportable + aboveSupportable < iArtillery + aboveArtillery)
					{
						while (iSupportable + aboveSupportable < iArtillery + aboveArtillery && unitsByPowerGives.get(i).size() > 0)
						{
							final int first = 0;
							tempList1.add(unitsByPowerGives.get(i).get(first));
							iArtillery -= DiceRoll.getArtillerySupportAvailable(unitsByPowerGives.get(i).get(first), defending, player);
							unitsByPowerGives.get(i).remove(first);
						}
						tempList1.addAll(unitsByPowerNone.get(i));
						tempList1.addAll(unitsByPowerBoth.get(i));
						unitsByPowerNone.get(i).clear();
						unitsByPowerBoth.get(i).clear();
						Collections.sort(tempList1, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
						perfectlySortedUnitsList.addAll(tempList1);
						continue;
					}
					if (iSupportable + aboveSupportable == iArtillery + aboveArtillery)
					{
						tempList1.addAll(unitsByPowerNone.get(i));
						tempList1.addAll(unitsByPowerBoth.get(i));
						unitsByPowerNone.get(i).clear();
						unitsByPowerBoth.get(i).clear();
						if (!unitsByPowerGives.get(i).isEmpty())
							tempList2.add(unitsByPowerGives.get(i).get(0));
						if (!unitsByPowerReceives.get(i).isEmpty())
							tempList2.add(unitsByPowerReceives.get(i).get(0));
						Collections.sort(tempList2, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
						final Unit u = tempList2.get(0);
						tempList1.add(u);
						final UnitAttachment ua = UnitAttachment.get(u.getType());
						if (ua.getArtillery())
						{
							unitsByPowerGives.get(i).remove(0);
							iArtillery -= DiceRoll.getArtillerySupportAvailable(u, defending, player);
						}
						else
						{
							unitsByPowerReceives.get(i).remove(0);
							iSupportable -= DiceRoll.getSupportableAvailable(u, defending, player);
						}
						Collections.sort(tempList1, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
						perfectlySortedUnitsList.addAll(tempList1);
						continue;
					}
					// and we should never get down here
					throw new IllegalStateException("Possibility not accounted for in sortUnitsForCasualtiesWithSupport.");
				}
			}
		}
		if (perfectlySortedUnitsList.isEmpty() || !perfectlySortedUnitsList.containsAll(sortedUnitsList) || !sortedUnitsList.containsAll(perfectlySortedUnitsList)
					|| perfectlySortedUnitsList.size() != sortedUnitsList.size())
			throw new IllegalStateException("Possibility not accounted for in sortUnitsForCasualtiesWithSupport.");
		return perfectlySortedUnitsList;
	}
	*/

	public static Map<Unit, Collection<Unit>> getDependents(final Collection<Unit> targets, final GameData data)
	{
		// just worry about transports
		final Map<Unit, Collection<Unit>> dependents = new HashMap<Unit, Collection<Unit>>();
		for (final Unit target : targets)
		{
			dependents.put(target, TransportTracker.transportingAndUnloaded(target));
		}
		return dependents;
	}
	
	/**
	 * Return map where keys are unit types and values are PU costs of that unit type, based on a player.
	 * 
	 * Any production rule that produces multiple units
	 * (like artillery in NWO, costs 7 but makes 2 artillery, meaning effective price is 3.5 each)
	 * will have their costs rounded up on a per unit basis (so NWO artillery will become 4).
	 * Therefore, this map should NOT be used for Purchasing information!
	 * 
	 * @param player
	 *            The player to get costs schedule for
	 * @param data
	 *            The game data.
	 * @return a map of unit types to PU cost
	 */
	public static IntegerMap<UnitType> getCostsForTUV(final PlayerID player, final GameData data)
	{
		final Resource PUS;
		data.acquireReadLock();
		try
		{
			PUS = data.getResourceList().getResource(Constants.PUS);
		} finally
		{
			data.releaseReadLock();
		}
		final IntegerMap<UnitType> costs = new IntegerMap<UnitType>();
		final ProductionFrontier frontier = player.getProductionFrontier();
		// any one will do then
		if (frontier == null)
			return getCostsForTuvForAllPlayersMergedAndAveraged(data);
		for (final ProductionRule rule : frontier.getRules())
		{
			final int costPerGroup = rule.getCosts().getInt(PUS);
			final NamedAttachable resourceOrUnit = rule.getResults().keySet().iterator().next();
			if (!(resourceOrUnit instanceof UnitType))
				continue;
			final UnitType type = (UnitType) resourceOrUnit;
			final int numberProduced = rule.getResults().getInt(type);
			// we average the cost for a single unit, rounding up
			final int roundedCostPerSingle = (int) Math.ceil((double) costPerGroup / (double) numberProduced);
			costs.put(type, roundedCostPerSingle);
		}
		// since our production frontier may not cover all the units we control, and not the enemy units,
		// we will add any unit types not in our list, based on the list for everyone
		final IntegerMap<UnitType> costsAll = getCostsForTuvForAllPlayersMergedAndAveraged(data);
		for (final UnitType ut : costsAll.keySet())
		{
			if (!costs.keySet().contains(ut))
				costs.put(ut, costsAll.getInt(ut));
		}
		return costs;
	}
	
	/*
	 * This clears out the variable map keeping track of the average costs of units on this game. Should use this any time you load a game or switch games.
	 *
	public static void clearCostsForTuvForAllPlayersMergedAndAveraged() {
		s_costsForTuvForAllPlayersMergedAndAveraged.clear();
	}*/
	/**
	 * Return a map where key are unit types and values are the AVERAGED for all RULES (not for all players).
	 * 
	 * Any production rule that produces multiple units
	 * (like artillery in NWO, costs 7 but makes 2 artillery, meaning effective price is 3.5 each)
	 * will have their costs rounded up on a per unit basis.
	 * Therefore, this map should NOT be used for Purchasing information!
	 * 
	 * @param data
	 * @return
	 */
	public static IntegerMap<UnitType> getCostsForTuvForAllPlayersMergedAndAveraged(final GameData data)
	{
		/*if (s_costsForTuvForAllPlayersMergedAndAveraged != null && s_costsForTuvForAllPlayersMergedAndAveraged.size() > 0)
			return s_costsForTuvForAllPlayersMergedAndAveraged;*/
		final Resource PUS;
		data.acquireReadLock();
		try
		{
			PUS = data.getResourceList().getResource(Constants.PUS);
		} finally
		{
			data.releaseReadLock();
		}
		final IntegerMap<UnitType> costs = new IntegerMap<UnitType>();
		final HashMap<UnitType, List<Integer>> differentCosts = new HashMap<UnitType, List<Integer>>();
		for (final ProductionRule rule : data.getProductionRuleList().getProductionRules())
		{
			// only works for the first result, so we are assuming each purchase frontier only gives one type of unit
			final NamedAttachable resourceOrUnit = rule.getResults().keySet().iterator().next();
			if (!(resourceOrUnit instanceof UnitType))
				continue;
			final UnitType ut = (UnitType) resourceOrUnit;
			final int numberProduced = rule.getResults().getInt(ut);
			final int costPerGroup = rule.getCosts().getInt(PUS);
			// we round up the cost
			final int roundedCostPerSingle = (int) Math.ceil((double) costPerGroup / (double) numberProduced);
			if (differentCosts.containsKey(ut))
				differentCosts.get(ut).add(roundedCostPerSingle);
			else
			{
				final List<Integer> listTemp = new ArrayList<Integer>();
				listTemp.add(roundedCostPerSingle);
				differentCosts.put(ut, listTemp);
			}
		}
		for (final UnitType ut : differentCosts.keySet())
		{
			int totalCosts = 0;
			final List<Integer> costsForType = differentCosts.get(ut);
			for (final int cost : costsForType)
			{
				totalCosts += cost;
			}
			final int averagedCost = (int) Math.round(((double) totalCosts / (double) costsForType.size()));
			costs.put(ut, averagedCost);
		}
		// s_costsForTuvForAllPlayersMergedAndAveraged = costs; //There is a problem with this variable, that it isn't being cleared out when we switch maps.
		return costs;
	}
	
	/**
	 * Return map where keys are unit types and values are resource costs of that unit type, based on a player.
	 * 
	 * Any production rule that produces multiple units
	 * (like artillery in NWO, costs 7 but makes 2 artillery, meaning effective price is 3.5 each)
	 * will have their costs rounded up on a per unit basis.
	 * Therefore, this map should NOT be used for Purchasing information!
	 * 
	 * @param data
	 * @param includeAverageForMissingUnits
	 * @return
	 */
	public static Map<PlayerID, Map<UnitType, ResourceCollection>> getResourceCostsForTUV(final GameData data, final boolean includeAverageForMissingUnits)
	{
		final LinkedHashMap<PlayerID, Map<UnitType, ResourceCollection>> rVal = new LinkedHashMap<PlayerID, Map<UnitType, ResourceCollection>>();
		final Map<UnitType, ResourceCollection> average = includeAverageForMissingUnits ? getResourceCostsForTUVForAllPlayersMergedAndAveraged(data) : new HashMap<UnitType, ResourceCollection>();
		final List<PlayerID> players = data.getPlayerList().getPlayers();
		players.add(PlayerID.NULL_PLAYERID);
		for (final PlayerID p : players)
		{
			final ProductionFrontier frontier = p.getProductionFrontier();
			// any one will do then
			if (frontier == null)
			{
				rVal.put(p, average);
				continue;
			}
			Map<UnitType, ResourceCollection> current = rVal.get(p);
			if (current == null)
			{
				current = new LinkedHashMap<UnitType, ResourceCollection>();
				rVal.put(p, current);
			}
			for (final ProductionRule rule : frontier.getRules())
			{
				if (rule == null || rule.getResults() == null || rule.getResults().isEmpty() || rule.getCosts() == null || rule.getCosts().isEmpty())
					continue;
				final IntegerMap<NamedAttachable> unitMap = rule.getResults();
				final ResourceCollection costPerGroup = new ResourceCollection(data, rule.getCosts());
				final Set<UnitType> units = new HashSet<UnitType>();
				for (final NamedAttachable resourceOrUnit : unitMap.keySet())
				{
					if (!(resourceOrUnit instanceof UnitType))
						continue;
					units.add((UnitType) resourceOrUnit);
				}
				if (units.isEmpty())
					continue;
				final int totalProduced = unitMap.totalValues();
				if (totalProduced == 1)
				{
					current.put(units.iterator().next(), costPerGroup);
				}
				else if (totalProduced > 1)
				{
					costPerGroup.discount((double) 1 / (double) totalProduced);
					for (final UnitType ut : units)
					{
						current.put(ut, costPerGroup);
					}
				}
			}
			// since our production frontier may not cover all the units we control, and not the enemy units,
			// we will add any unit types not in our list, based on the list for everyone
			for (final UnitType ut : average.keySet())
			{
				if (!current.keySet().contains(ut))
					current.put(ut, average.get(ut));
			}
		}
		rVal.put(null, average);
		return rVal;
	}
	
	/**
	 * Return a map where key are unit types and values are the AVERAGED for all players.
	 * 
	 * Any production rule that produces multiple units
	 * (like artillery in NWO, costs 7 but makes 2 artillery, meaning effective price is 3.5 each)
	 * will have their costs rounded up on a per unit basis.
	 * Therefore, this map should NOT be used for Purchasing information!
	 * 
	 * @param data
	 * @return
	 */
	public static Map<UnitType, ResourceCollection> getResourceCostsForTUVForAllPlayersMergedAndAveraged(final GameData data)
	{
		final Map<UnitType, ResourceCollection> average = new HashMap<UnitType, ResourceCollection>();
		final Resource PUS;
		data.acquireReadLock();
		try
		{
			PUS = data.getResourceList().getResource(Constants.PUS);
		} finally
		{
			data.releaseReadLock();
		}
		final IntegerMap<Resource> defaultMap = new IntegerMap<Resource>();
		defaultMap.put(PUS, 1);
		final ResourceCollection defaultResources = new ResourceCollection(data, defaultMap);
		final Map<UnitType, List<ResourceCollection>> backups = new HashMap<UnitType, List<ResourceCollection>>();
		final Map<UnitType, ResourceCollection> backupAveraged = new HashMap<UnitType, ResourceCollection>();
		for (final ProductionRule rule : data.getProductionRuleList().getProductionRules())
		{
			if (rule == null || rule.getResults() == null || rule.getResults().isEmpty() || rule.getCosts() == null || rule.getCosts().isEmpty())
				continue;
			final IntegerMap<NamedAttachable> unitMap = rule.getResults();
			final ResourceCollection costPerGroup = new ResourceCollection(data, rule.getCosts());
			final Set<UnitType> units = new HashSet<UnitType>();
			for (final NamedAttachable resourceOrUnit : unitMap.keySet())
			{
				if (!(resourceOrUnit instanceof UnitType))
					continue;
				units.add((UnitType) resourceOrUnit);
			}
			if (units.isEmpty())
				continue;
			final int totalProduced = unitMap.totalValues();
			if (totalProduced == 1)
			{
				final UnitType ut = units.iterator().next();
				List<ResourceCollection> current = backups.get(ut);
				if (current == null)
				{
					current = new ArrayList<ResourceCollection>();
					backups.put(ut, current);
				}
				current.add(costPerGroup);
			}
			else if (totalProduced > 1)
			{
				costPerGroup.discount((double) 1 / (double) totalProduced);
				for (final UnitType ut : units)
				{
					List<ResourceCollection> current = backups.get(ut);
					if (current == null)
					{
						current = new ArrayList<ResourceCollection>();
						backups.put(ut, current);
					}
					current.add(costPerGroup);
				}
			}
		}
		for (final Entry<UnitType, List<ResourceCollection>> entry : backups.entrySet())
		{
			final ResourceCollection avgCost = new ResourceCollection(entry.getValue().toArray(new ResourceCollection[entry.getValue().size()]), data);
			if (entry.getValue().size() > 1)
			{
				avgCost.discount((double) 1 / (double) entry.getValue().size());
			}
			backupAveraged.put(entry.getKey(), avgCost);
		}
		final Map<PlayerID, Map<UnitType, ResourceCollection>> allPlayersCurrent = getResourceCostsForTUV(data, false);
		allPlayersCurrent.remove(null);
		for (final UnitType ut : data.getUnitTypeList().getAllUnitTypes())
		{
			final List<ResourceCollection> costs = new ArrayList<ResourceCollection>();
			for (final Map<UnitType, ResourceCollection> entry : allPlayersCurrent.values())
			{
				if (entry.get(ut) != null)
				{
					costs.add(entry.get(ut));
				}
			}
			if (costs.isEmpty())
			{
				final ResourceCollection backup = backupAveraged.get(ut);
				if (backup != null)
				{
					costs.add(backup);
				}
				else
				{
					costs.add(defaultResources);
				}
			}
			final ResourceCollection avgCost = new ResourceCollection(costs.toArray(new ResourceCollection[costs.size()]), data);
			if (costs.size() > 1)
			{
				avgCost.discount((double) 1 / (double) costs.size());
			}
			average.put(ut, avgCost);
		}
		return average;
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
	public static int getTUV(final Collection<Unit> units, final IntegerMap<UnitType> costs)
	{
		int tuv = 0;
		for (final Unit u : units)
		{
			final int unitValue = costs.getInt(u.getType());
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
	public static int getTUV(final Collection<Unit> units, final PlayerID player, final IntegerMap<UnitType> costs, final GameData data)
	{
		final Collection<Unit> playerUnits = Match.getMatches(units, Matches.alliedUnit(player, data));
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
	private static boolean allTargetsOneTypeOneHitPoint(final Collection<Unit> targets, final Map<Unit, Collection<Unit>> dependents)
	{
		final Set<UnitCategory> categorized = UnitSeperator.categorize(targets, dependents, false, false);
		if (categorized.size() == 1)
		{
			final UnitCategory unitCategory = categorized.iterator().next();
			if (unitCategory.getHitPoints() - unitCategory.getDamaged() <= 1)
				return true;
		}
		return false;
	}
	
	public static int getRolls(final Collection<Unit> units, final Territory location, final PlayerID id, final boolean defend, final boolean bombing,
				final Set<List<UnitSupportAttachment>> supportRulesFriendly, final IntegerMap<UnitSupportAttachment> supportLeftFriendlyCopy, final Set<List<UnitSupportAttachment>> supportRulesEnemy,
				final IntegerMap<UnitSupportAttachment> supportLeftEnemyCopy, final Collection<TerritoryEffect> territoryEffects)
	{
		int count = 0;
		for (final Unit unit : units)
		{
			final int unitRoll = getRolls(unit, location, id, defend, bombing, supportRulesFriendly, supportLeftFriendlyCopy, supportRulesEnemy, supportLeftEnemyCopy, territoryEffects);
			count += unitRoll;
		}
		return count;
	}
	
	public static int getRolls(final Collection<Unit> units, final Territory location, final PlayerID id, final boolean defend, final boolean bombing,
				final Collection<TerritoryEffect> territoryEffects)
	{
		return getRolls(units, location, id, defend, bombing, new HashSet<List<UnitSupportAttachment>>(), new IntegerMap<UnitSupportAttachment>(), new HashSet<List<UnitSupportAttachment>>(),
					new IntegerMap<UnitSupportAttachment>(), territoryEffects);
	}
	
	public static int getRolls(final Unit unit, final Territory location, final PlayerID id, final boolean defend, final boolean bombing, final Set<List<UnitSupportAttachment>> supportRulesFriendly,
				final IntegerMap<UnitSupportAttachment> supportLeftFriendlyCopy, final Set<List<UnitSupportAttachment>> supportRulesEnemy,
				final IntegerMap<UnitSupportAttachment> supportLeftEnemyCopy, final Collection<TerritoryEffect> territoryEffects)
	{
		final UnitAttachment unitAttachment = UnitAttachment.get(unit.getType());
		int rolls;
		if (defend)
			rolls = unitAttachment.getDefenseRolls(id);
		else
			rolls = unitAttachment.getAttackRolls(id);
		
		final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> dummyEmptyMap = new HashMap<UnitSupportAttachment, LinkedIntegerMap<Unit>>();
		rolls += DiceRoll.getSupport(unit, supportRulesFriendly, supportLeftFriendlyCopy, dummyEmptyMap, null, false, true);
		rolls += DiceRoll.getSupport(unit, supportRulesEnemy, supportLeftEnemyCopy, dummyEmptyMap, null, false, true);
		rolls = Math.max(0, rolls);
		
		// if we are strategic bombing, we do not care what the strength of the unit is...
		if (bombing)
			return rolls;
		
		int strength;
		if (defend)
			strength = unitAttachment.getDefense(unit.getOwner());
		else
			strength = unitAttachment.getAttack(unit.getOwner());
		// TODO: we should add in isMarine bonus too...
		strength += DiceRoll.getSupport(unit, supportRulesFriendly, supportLeftFriendlyCopy, dummyEmptyMap, null, true, false);
		strength += DiceRoll.getSupport(unit, supportRulesEnemy, supportLeftEnemyCopy, dummyEmptyMap, null, true, false);
		strength += TerritoryEffectHelper.getTerritoryCombatBonus(unit.getType(), territoryEffects, defend);
		
		if (strength <= 0)
			rolls = 0;
		
		/* (getAttackRolls no longer returns zero when unit has zero attack power, so this is no longer needed) 
		// Don't forget that units can have zero attack, and then be given attack power by support, and therefore be able to roll
		if (rolls == 0 && (defend ? unitAttachment.getDefense(id) : unitAttachment.getAttack(id)) == 0)
		{
			if (DiceRoll.getSupport(unit.getType(), supportRulesFriendly, supportLeftFriendlyCopy, true, false) > 0)
				rolls += 1;
		}
		if (rolls == 0 && (defend ? unitAttachment.getDefense(id) : unitAttachment.getAttack(id)) == 0)
		{
			if (TerritoryEffectHelper.getTerritoryCombatBonus(unit.getType(), territoryEffects, defend) > 0)
				rolls += 1;
		}*/
		return rolls;
	}
	
	public static int getRolls(final Unit unit, final Territory location, final PlayerID id, final boolean defend, final boolean bombing, final Collection<TerritoryEffect> territoryEffects)
	{
		return getRolls(unit, location, id, defend, bombing, new HashSet<List<UnitSupportAttachment>>(), new IntegerMap<UnitSupportAttachment>(), new HashSet<List<UnitSupportAttachment>>(),
					new IntegerMap<UnitSupportAttachment>(), territoryEffects);
	}
	
	/**
	 * @return Can transports be used as cannon fodder
	 */
	private static boolean isTransportCasualtiesRestricted(final GameData data)
	{
		return games.strategy.triplea.Properties.getTransportCasualtiesRestricted(data);
	}
	
	/**
	 * @return Random AA Casualties - casualties randomly assigned
	 */
	private static boolean isRandomAACasualties(final GameData data)
	{
		return games.strategy.triplea.Properties.getRandomAACasualties(data);
	}
	
	/**
	 * @return Roll AA Individually - roll against each aircraft
	 */
	private static boolean isRollAAIndividually(final GameData data)
	{
		return games.strategy.triplea.Properties.getRollAAIndividually(data);
	}
	
	/**
	 * @return Choose AA - attacker selects casualties
	 */
	private static boolean isChooseAA(final GameData data)
	{
		return games.strategy.triplea.Properties.getChoose_AA_Casualties(data);
	}
	
	/**
	 * @return Can the attacker retreat non-amphibious units
	 */
	private static boolean isPartialAmphibiousRetreat(final GameData data)
	{
		return games.strategy.triplea.Properties.getPartialAmphibiousRetreat(data);
	}
	
	// nothing but static
	private BattleCalculator()
	{
	}
	
	/**
	 * This returns the exact Power that a unit has according to what DiceRoll.rollDiceLowLuck() would give it.
	 * As such, it needs to exactly match DiceRoll, otherwise this method will become useless.
	 * It does NOT take into account SUPPORT.
	 * It DOES take into account ROLLS.
	 * It needs to be updated to take into account isMarine.
	 */
	public static int getUnitPowerForSorting(final Unit current, final boolean defending, final GameData data, final Collection<TerritoryEffect> territoryEffects)
	{
		/* this is needed if i plan to have it account for support
		Set<List<UnitSupportAttachment>> supportRules = new HashSet<List<UnitSupportAttachment>>();
		IntegerMap<UnitSupportAttachment> supportLeft = new IntegerMap<UnitSupportAttachment>();
		DiceRoll.getSupport(sortedUnitsList,supportRules,supportLeft,data,defending);
		*/
		final boolean lhtrBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(data);
		final UnitAttachment ua = UnitAttachment.get(current.getType());
		int rolls;
		if (defending)
			rolls = ua.getDefenseRolls(current.getOwner());
		else
			rolls = ua.getAttackRolls(current.getOwner());
		// int strength = 0;
		int strengthWithoutSupport = 0;
		// Find the strength the unit has without support
		// lhtr heavy bombers take best of n dice for both attack and defense
		if (rolls > 1 && (lhtrBombers || ua.getChooseBestRoll()))
		{
			if (defending)
				strengthWithoutSupport = ua.getDefense(current.getOwner());
			else
				strengthWithoutSupport = ua.getAttack(current.getOwner());
			strengthWithoutSupport += TerritoryEffectHelper.getTerritoryCombatBonus(current.getType(), territoryEffects, defending);
			// just add one like LL if we are LHTR bombers
			strengthWithoutSupport = Math.min(Math.max(strengthWithoutSupport + 1, 0), data.getDiceSides());
			// strength += DiceRoll.getSupport(current.getType(), supportRules, supportLeft);
			// strength = Math.min(Math.max(strength+1, 0), Constants.MAX_DICE);
		}
		else
		{
			for (int i = 0; i < rolls; i++)
			{
				int tempStrength;
				if (defending)
					tempStrength = ua.getDefense(current.getOwner());
				else
					tempStrength = ua.getAttack(current.getOwner());
				if (defending)
				{
					// if (DiceRoll.isFirstTurnLimitedRoll(player))
					// tempStrength = Math.min(1, tempStrength);
				}
				else
				{
					/* TODO: figure out how to find if we are in a battle, and if that battle is amphibious
					if (ua.getIsMarine() && battle.isAmphibious())
					{
					    Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
					    if(landUnits.contains(current))
					        ++tempStrength;
					} */
				}
				strengthWithoutSupport += TerritoryEffectHelper.getTerritoryCombatBonus(current.getType(), territoryEffects, defending);
				strengthWithoutSupport += Math.min(Math.max(tempStrength, 0), data.getDiceSides());
				// tempStrength += DiceRoll.getSupport(current.getType(), supportRules, supportLeft);
				// strength += Math.min(Math.max(tempStrength, 0), Constants.MAX_DICE);
			}
		}
		return strengthWithoutSupport;
		/*
		//Find the strength this unit gives to other units
		Iterator<UnitSupportAttachment> iter = UnitSupportAttachment.get(data).iterator();
		while(iter.hasNext())
		{
			UnitSupportAttachment rule = iter.next();
			if(rule.getPlayers().isEmpty())
				continue;
			if( defending && rule.getDefence() ||
					!defending && rule.getOffence() )
			{
				CompositeMatchAnd<Unit> canSupport = new CompositeMatchAnd<Unit>(Matches.unitIsOfType((UnitType)rule.getAttachedTo()),Matches.unitOwnedBy(rule.getPlayers()));
				List<Unit> supporters = Match.getMatches(sortedUnitsList, canSupport);
				int numSupport = supporters.size();
				if(rule.getImpArtTech())
					numSupport += Match.getMatches(supporters, Matches.unitOwnerHasImprovedArtillerySupportTech()).size();
				String bonusType = rule.getBonusType();
				//supportLeft.put(rule, numSupport*rule.getNumber());
				Iterator<List<UnitSupportAttachment>> iter2 = supportRules.iterator();
				List<UnitSupportAttachment> ruleType = null;
				boolean found = false;
				while( iter2.hasNext()){
					ruleType = iter2.next();
					if( ruleType.get(0).getBonusType().equals(bonusType) ){
						found = true;
						break;
					}
				}
				if( !found ) {
					ruleType = new ArrayList<UnitSupportAttachment>();
					supportRules.add(ruleType);
				}
				ruleType.add(rule);
			}
		}*/
	}
}
