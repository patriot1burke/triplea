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
/*
 * StrategicBombingRaidBattle.java
 * 
 * Created on November 29, 2001, 2:21 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.engine.GameOverException;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.ConnectionLostException;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TechAbilityAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Sean Bridges
 * @version 1.0
 */
public class StrategicBombingRaidBattle extends AbstractBattle implements BattleStepStrings
{
	private static final long serialVersionUID = 8490171037606078890L;
	private final static String RAID = "Strategic bombing raid";
	
	protected final HashMap<Unit, HashSet<Unit>> m_targets = new HashMap<Unit, HashSet<Unit>>(); // these would be the factories or other targets. does not include aa.
	protected final ExecutionStack m_stack = new ExecutionStack();
	protected List<String> m_steps;
	protected List<Unit> m_defendingAA;
	protected List<String> m_AAtypes;
	
	private int m_bombingRaidTotal;
	private final IntegerMap<Unit> m_bombingRaidDamage = new IntegerMap<Unit>();
	
	/**
	 * Creates new StrategicBombingRaidBattle
	 * 
	 * @param battleSite
	 *            - battle territory
	 * @param data
	 *            - game data
	 * @param attacker
	 *            - attacker PlayerID
	 * @param defender
	 *            - defender PlayerID
	 * @param battleTracker
	 *            - BattleTracker
	 **/
	public StrategicBombingRaidBattle(final Territory battleSite, final GameData data, final PlayerID attacker, final BattleTracker battleTracker)
	{
		super(battleSite, attacker, battleTracker, true, BattleType.BOMBING_RAID, data);
		m_isAmphibious = false;
		updateDefendingUnits();
	}
	
	@Override
	protected void removeUnitsThatNoLongerExist()
	{
		if (m_headless)
			return;
		// we were having a problem with units that had been killed previously were still part of battle's variables, so we double check that the stuff still exists here.
		m_defendingUnits.retainAll(m_battleSite.getUnits().getUnits());
		m_attackingUnits.retainAll(m_battleSite.getUnits().getUnits());
		final Iterator<Unit> iter = m_targets.keySet().iterator();
		while (iter.hasNext())
		{
			if (!m_battleSite.getUnits().getUnits().contains(iter.next()))
				iter.remove();
		}
	}
	
	protected void updateDefendingUnits()
	{
		// fill in defenders
		final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed = TechAbilityAttachment.getAirborneTargettedByAA(m_attacker, m_data);
		final Match<Unit> defenders = new CompositeMatchAnd<Unit>(Matches.enemyUnit(m_attacker, m_data),
					new CompositeMatchOr<Unit>(Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite).invert(), Matches.UnitIsAAthatCanFire(m_attackingUnits,
								airborneTechTargetsAllowed, m_attacker, Matches.UnitIsAAforBombingThisUnitOnly, m_round, true, m_data)));
		if (m_targets.isEmpty())
		{
			m_defendingUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), defenders);
		}
		else
		{
			final List<Unit> targets = Match.getMatches(m_battleSite.getUnits().getUnits(),
						Matches.UnitIsAAthatCanFire(m_attackingUnits, airborneTechTargetsAllowed, m_attacker, Matches.UnitIsAAforBombingThisUnitOnly, m_round, true, m_data));
			targets.addAll(m_targets.keySet());
			m_defendingUnits = targets;
		}
	}
	
	@Override
	public boolean isEmpty()
	{
		return m_attackingUnits.isEmpty();
	}
	
	@Override
	public void removeAttack(final Route route, final Collection<Unit> units)
	{
		removeAttackers(units, true);
	}
	
	private void removeAttackers(final Collection<Unit> units, final boolean removeTarget)
	{
		m_attackingUnits.removeAll(units);
		final Iterator<Unit> targetIter = m_targets.keySet().iterator();
		while (targetIter.hasNext())
		{
			final HashSet<Unit> currentAttackers = m_targets.get(targetIter.next());
			currentAttackers.removeAll(units);
			if (currentAttackers.isEmpty() && removeTarget)
				targetIter.remove();
		}
	}
	
	private Unit getTarget(final Unit attacker)
	{
		for (final Unit target : m_targets.keySet())
		{
			if (m_targets.get(target).contains(attacker))
				return target;
		}
		throw new IllegalStateException("Unit " + attacker.getType().getName() + " has no target");
	}
	
	@Override
	public Change addAttackChange(final Route route, final Collection<Unit> units, final HashMap<Unit, HashSet<Unit>> targets)
	{
		m_attackingUnits.addAll(units);
		if (targets == null)
			return ChangeFactory.EMPTY_CHANGE;
		for (final Unit target : targets.keySet())
		{
			HashSet<Unit> currentAttackers = m_targets.get(target);
			if (currentAttackers == null)
				currentAttackers = new HashSet<Unit>();
			currentAttackers.addAll(targets.get(target));
			m_targets.put(target, currentAttackers);
		}
		return ChangeFactory.EMPTY_CHANGE;
	}
	
	@Override
	public void fight(final IDelegateBridge bridge)
	{
		// remove units that may already be dead due to a previous event (like they died from a strategic bombing raid, rocket attack, etc)
		removeUnitsThatNoLongerExist();
		// we were interrupted
		if (m_stack.isExecuting())
		{
			showBattle(bridge);
			m_stack.execute(bridge);
			return;
		}
		// We update Defending Units twice: first time when the battle is created, and second time before the battle begins.
		// The reason is because when the battle is created, there are no attacking units yet in it, meaning that m_targets is empty. We need to update right as battle begins to know we have the full list of targets.
		updateDefendingUnits();
		bridge.getHistoryWriter().startEvent("Strategic bombing raid in " + m_battleSite, m_battleSite);
		if (m_attackingUnits.isEmpty() || (m_defendingUnits.isEmpty() || Match.noneMatch(m_defendingUnits, Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite).invert())))
		{
			endBeforeRolling(bridge);
			return;
		}
		
		BattleCalculator.sortPreBattle(m_attackingUnits, m_data);
		// TODO: determine if the target has the property, not just any unit with the property isAAforBombingThisUnitOnly
		final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed = TechAbilityAttachment.getAirborneTargettedByAA(m_attacker, m_data);
		m_defendingAA = m_battleSite.getUnits().getMatches(
					Matches.UnitIsAAthatCanFire(m_attackingUnits, airborneTechTargetsAllowed, m_attacker, Matches.UnitIsAAforBombingThisUnitOnly, m_round, true, m_data));
		m_AAtypes = UnitAttachment.getAllOfTypeAAs(m_defendingAA);
		Collections.reverse(m_AAtypes); // reverse since stacks are in reverse order
		final boolean hasAA = m_defendingAA.size() > 0;
		m_steps = new ArrayList<String>();
		if (hasAA)
		{
			for (final String typeAA : UnitAttachment.getAllOfTypeAAs(m_defendingAA))
			{
				m_steps.add(typeAA + AA_GUNS_FIRE_SUFFIX);
				m_steps.add(SELECT_PREFIX + typeAA + CASUALTIES_SUFFIX);
				m_steps.add(REMOVE_PREFIX + typeAA + CASUALTIES_SUFFIX);
			}
		}
		m_steps.add(RAID);
		showBattle(bridge);
		final List<IExecutable> steps = new ArrayList<IExecutable>();
		if (hasAA)
		{
			steps.add(new FireAA());
		}
		steps.add(new ConductBombing());
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = 4299575008166316488L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				getDisplay(bridge).gotoBattleStep(m_battleID, RAID);
				if (isDamageFromBombingDoneToUnitsInsteadOfTerritories())
					bridge.getHistoryWriter().addChildToEvent("Bombing raid in " + m_battleSite.getName() + " causes " + m_bombingRaidTotal + " damage total. " +
								(m_bombingRaidDamage.size() > 1 ? (" Damaged units is as follows: " + MyFormatter.integerUnitMapToString(m_bombingRaidDamage, ", ", " = ", false)) : ""));
				else
					bridge.getHistoryWriter().addChildToEvent("Bombing raid costs " + m_bombingRaidTotal + " " + MyFormatter.pluralize("PU", m_bombingRaidTotal));
				// TODO remove the reference to the constant.japanese- replace with a rule
				if (isPacificTheater() || isSBRVictoryPoints())
				{
					if (m_defender.getName().equals(Constants.JAPANESE))
					{
						Change changeVP;
						final PlayerAttachment pa = PlayerAttachment.get(m_defender);
						if (pa != null)
						{
							changeVP = ChangeFactory.attachmentPropertyChange(pa, ((-(m_bombingRaidTotal / 10)) + pa.getVps()), "vps");
							bridge.addChange(changeVP);
							bridge.getHistoryWriter().addChildToEvent("Bombing raid costs " + (m_bombingRaidTotal / 10) + " " + MyFormatter.pluralize("vp", (m_bombingRaidTotal / 10)));
						}
					}
				}
				// kill any suicide attackers (veqryn)
				if (Match.someMatch(m_attackingUnits, Matches.UnitIsSuicide))
				{
					final List<Unit> suicideUnits = Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide);
					m_attackingUnits.removeAll(suicideUnits);
					final Change removeSuicide = ChangeFactory.removeUnits(m_battleSite, suicideUnits);
					final String transcriptText = MyFormatter.unitsToText(suicideUnits) + " lost in " + m_battleSite.getName();
					final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_attacker, m_data);
					final int tuvLostAttacker = BattleCalculator.getTUV(suicideUnits, m_attacker, costs, m_data);
					m_attackerLostTUV += tuvLostAttacker;
					bridge.getHistoryWriter().addChildToEvent(transcriptText, suicideUnits);
					bridge.addChange(removeSuicide);
				}
				// kill any units that can die if they have reached max damage (veqryn)
				if (Match.someMatch(m_targets.keySet(), Matches.UnitCanDieFromReachingMaxDamage))
				{
					final List<Unit> unitsCanDie = Match.getMatches(m_targets.keySet(), Matches.UnitCanDieFromReachingMaxDamage);
					unitsCanDie.retainAll(Match.getMatches(unitsCanDie, Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite)));
					if (!unitsCanDie.isEmpty())
					{
						// m_targets.removeAll(unitsCanDie);
						final Change removeDead = ChangeFactory.removeUnits(m_battleSite, unitsCanDie);
						final String transcriptText = MyFormatter.unitsToText(unitsCanDie) + " lost in " + m_battleSite.getName();
						final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_defender, m_data);
						final int tuvLostDefender = BattleCalculator.getTUV(unitsCanDie, m_defender, costs, m_data);
						m_defenderLostTUV += tuvLostDefender;
						bridge.getHistoryWriter().addChildToEvent(transcriptText, unitsCanDie);
						bridge.addChange(removeDead);
					}
				}
			}
		});
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = -7649516174883172328L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				end(bridge);
			}
		});
		Collections.reverse(steps);
		for (final IExecutable executable : steps)
		{
			m_stack.push(executable);
		}
		m_stack.execute(bridge);
	}
	
	private void endBeforeRolling(final IDelegateBridge bridge)
	{
		getDisplay(bridge).battleEnd(m_battleID, "Bombing raid does no damage");
		m_whoWon = WhoWon.DRAW;
		m_battleResultDescription = BattleRecord.BattleResultDescription.NO_BATTLE;
		m_battleTracker.getBattleRecords(m_data).addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, m_battleResultDescription,
					new BattleResults(this, m_data), m_bombingRaidTotal);
		m_isOver = true;
		m_battleTracker.removeBattle(StrategicBombingRaidBattle.this);
	}
	
	private void end(final IDelegateBridge bridge)
	{
		if (isDamageFromBombingDoneToUnitsInsteadOfTerritories())
			getDisplay(bridge).battleEnd(m_battleID, "Raid causes " + m_bombingRaidTotal + " damage total." +
						(m_bombingRaidDamage.size() > 1 ? (" To units: " + MyFormatter.integerUnitMapToString(m_bombingRaidDamage, ", ", " = ", false)) : ""));
		else
			getDisplay(bridge).battleEnd(m_battleID, "Bombing raid cost " + m_bombingRaidTotal + " " + MyFormatter.pluralize("PU", m_bombingRaidTotal));
		if (m_bombingRaidTotal > 0)
		{
			m_whoWon = WhoWon.ATTACKER;
			m_battleResultDescription = BattleRecord.BattleResultDescription.BOMBED;
		}
		else
		{
			m_whoWon = WhoWon.DEFENDER;
			m_battleResultDescription = BattleRecord.BattleResultDescription.LOST;
		}
		m_battleTracker.getBattleRecords(m_data).addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, m_battleResultDescription,
					new BattleResults(this, m_data), m_bombingRaidTotal);
		m_isOver = true;
		m_battleTracker.removeBattle(StrategicBombingRaidBattle.this);
	}
	
	private void showBattle(final IDelegateBridge bridge)
	{
		final String title = "Bombing raid in " + m_battleSite.getName();
		getDisplay(bridge).showBattle(m_battleID, m_battleSite, title, m_attackingUnits, m_defendingUnits,
					null, null, null, Collections.<Unit, Collection<Unit>> emptyMap(), m_attacker, m_defender, isAmphibious(), getBattleType(), Collections.<Unit> emptySet());
		getDisplay(bridge).listBattleSteps(m_battleID, m_steps);
	}
	
	
	class FireAA implements IExecutable
	{
		private static final long serialVersionUID = -4667856856747597406L;
		DiceRoll m_dice;
		CasualtyDetails m_casualties;
		Collection<Unit> m_casualtiesSoFar = new ArrayList<Unit>();
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			final boolean isEditMode = BaseEditDelegate.getEditMode(bridge.getData());
			for (final String currentTypeAA : m_AAtypes)
			{
				final Collection<Unit> currentPossibleAA = Match.getMatches(m_defendingAA, Matches.UnitIsAAofTypeAA(currentTypeAA));
				final Set<UnitType> targetUnitTypesForThisTypeAA = UnitAttachment.get(currentPossibleAA.iterator().next().getType()).getTargetsAA(m_data);
				final Set<UnitType> airborneTypesTargettedToo = TechAbilityAttachment.getAirborneTargettedByAA(m_attacker, m_data).get(currentTypeAA);
				final Collection<Unit> validAttackingUnitsForThisRoll = Match.getMatches(m_attackingUnits, new CompositeMatchOr<Unit>(
							Matches.unitIsOfTypes(targetUnitTypesForThisTypeAA), new CompositeMatchAnd<Unit>(Matches.UnitIsAirborne, Matches.unitIsOfTypes(airborneTypesTargettedToo))));
				
				final IExecutable roll = new IExecutable()
				{
					private static final long serialVersionUID = 379538344036513009L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						validAttackingUnitsForThisRoll.removeAll(m_casualtiesSoFar);
						if (!validAttackingUnitsForThisRoll.isEmpty())
						{
							m_dice = DiceRoll.rollAA(validAttackingUnitsForThisRoll, currentPossibleAA, bridge, m_battleSite, true);
							if (currentTypeAA.equals("AA"))
							{
								if (m_dice.getHits() > 0)
									bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AA_HIT, m_defender.getName());
								else
									bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AA_MISS, m_defender.getName());
							}
							else
							{
								if (m_dice.getHits() > 0)
									bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAA.toLowerCase() + SoundPath.CLIP_BATTLE_X_HIT,
												m_defender.getName());
								else
									bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAA.toLowerCase() + SoundPath.CLIP_BATTLE_X_MISS,
												m_defender.getName());
							}
						}
					}
				};
				final IExecutable calculateCasualties = new IExecutable()
				{
					private static final long serialVersionUID = -4658133491636765763L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						if (!validAttackingUnitsForThisRoll.isEmpty())
						{
							final CasualtyDetails details = calculateCasualties(validAttackingUnitsForThisRoll, currentPossibleAA, bridge, m_dice, currentTypeAA);
							markDamaged(details.getDamaged(), bridge, true);
							m_casualties = details;
							m_casualtiesSoFar.addAll(details.getKilled());
						}
					}
				};
				final IExecutable notifyCasualties = new IExecutable()
				{
					private static final long serialVersionUID = -4989154196975570919L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						if (!validAttackingUnitsForThisRoll.isEmpty())
						{
							notifyAAHits(bridge, m_dice, m_casualties, currentTypeAA);
						}
					}
				};
				final IExecutable removeHits = new IExecutable()
				{
					private static final long serialVersionUID = -3673833177336068509L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						if (!validAttackingUnitsForThisRoll.isEmpty())
						{
							removeAAHits(bridge, m_dice, m_casualties, currentTypeAA);
						}
					}
				};
				// push in reverse order of execution
				stack.push(removeHits);
				stack.push(notifyCasualties);
				stack.push(calculateCasualties);
				if (!isEditMode)
					stack.push(roll);
			}
		}
	}
	
	private boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories()
	{
		return games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(m_data);
	}
	
	private boolean isWW2V2()
	{
		return games.strategy.triplea.Properties.getWW2V2(m_data);
	}
	
	private boolean isLimitSBRDamageToProduction()
	{
		return games.strategy.triplea.Properties.getLimitRocketAndSBRDamageToProduction(m_data);
	}
	
	private boolean isLimitSBRDamagePerTurn(final GameData data)
	{
		return games.strategy.triplea.Properties.getLimitSBRDamagePerTurn(data);
	}
	
	private boolean isPUCap(final GameData data)
	{
		return games.strategy.triplea.Properties.getPUCap(data);
	}
	
	private boolean isSBRVictoryPoints()
	{
		return games.strategy.triplea.Properties.getSBRVictoryPoint(m_data);
	}
	
	private boolean isPacificTheater()
	{
		return games.strategy.triplea.Properties.getPacificTheater(m_data);
	}
	
	private CasualtyDetails calculateCasualties(final Collection<Unit> validAttackingUnitsForThisRoll, final Collection<Unit> defendingAA, final IDelegateBridge bridge, final DiceRoll dice,
				final String currentTypeAA)
	{
		getDisplay(bridge).notifyDice(m_battleID, dice, SELECT_PREFIX + currentTypeAA + CASUALTIES_SUFFIX);
		final boolean isEditMode = BaseEditDelegate.getEditMode(m_data);
		final boolean allowMultipleHitsPerUnit = Match.allMatch(defendingAA, Matches.UnitAAShotDamageableInsteadOfKillingInstantly);
		if (isEditMode)
		{
			final String text = currentTypeAA + AA_GUNS_FIRE_SUFFIX;
			final CasualtyDetails casualtySelection = BattleCalculator.selectCasualties(RAID, m_attacker, validAttackingUnitsForThisRoll, m_attackingUnits, m_defender, m_defendingUnits,
						m_isAmphibious, m_amphibiousLandAttackers, m_battleSite, m_territoryEffects, bridge, text, /* dice */null,/* defending */false, m_battleID, /* head-less */false, 0,
						allowMultipleHitsPerUnit);
			return casualtySelection;
		}
		final CasualtyDetails casualties = BattleCalculator.getAACasualties(false, validAttackingUnitsForThisRoll, m_attackingUnits, defendingAA, m_defendingUnits, dice, bridge, m_defender,
					m_attacker, m_battleID, m_battleSite, m_territoryEffects, m_isAmphibious, m_amphibiousLandAttackers);
		
		final int totalExpectingHits = dice.getHits() > validAttackingUnitsForThisRoll.size() ? validAttackingUnitsForThisRoll.size() : dice.getHits();
		if (casualties.size() != totalExpectingHits)
			throw new IllegalStateException("Wrong number of casualties, expecting:" + totalExpectingHits + " but got:" + casualties.size());
		return casualties;
	}
	
	private void notifyAAHits(final IDelegateBridge bridge, final DiceRoll dice, final CasualtyDetails casualties, final String currentTypeAA)
	{
		getDisplay(bridge).casualtyNotification(m_battleID, REMOVE_PREFIX + currentTypeAA + CASUALTIES_SUFFIX, dice, m_attacker, new ArrayList<Unit>(casualties.getKilled()),
					new ArrayList<Unit>(casualties.getDamaged()),
					Collections.<Unit, Collection<Unit>> emptyMap());
		final Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					final ITripleaPlayer defender = (ITripleaPlayer) bridge.getRemotePlayer(m_defender);
					defender.confirmEnemyCasualties(m_battleID, "Press space to continue", m_attacker);
				} catch (final ConnectionLostException cle)
				{
					// somone else will deal with this
					// System.out.println(cle.getMessage());
					// cle.printStackTrace(System.out);
				} catch (final GameOverException e)
				{
					// ignore
				} catch (final Exception e)
				{
					// ignore
				}
			}
		};
		final Thread t = new Thread(r, "click to continue waiter");
		t.start();
		final ITripleaPlayer attacker = (ITripleaPlayer) bridge.getRemotePlayer(m_attacker);
		attacker.confirmOwnCasualties(m_battleID, "Press space to continue");
		try
		{
			bridge.leaveDelegateExecution();
			t.join();
		} catch (final InterruptedException e)
		{
			// ignore
		} finally
		{
			bridge.enterDelegateExecution();
		}
	}
	
	private void removeAAHits(final IDelegateBridge bridge, final DiceRoll dice, final CasualtyDetails casualties, final String currentTypeAA)
	{
		final List<Unit> killed = casualties.getKilled();
		if (!killed.isEmpty())
		{
			bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(killed) + " killed by " + currentTypeAA, new ArrayList<Unit>(killed));
			final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_attacker, m_data);
			final int tuvLostAttacker = BattleCalculator.getTUV(killed, m_attacker, costs, m_data);
			m_attackerLostTUV += tuvLostAttacker;
			// m_attackingUnits.removeAll(casualties);
			removeAttackers(killed, false);
			final Change remove = ChangeFactory.removeUnits(m_battleSite, killed);
			bridge.addChange(remove);
		}
	}
	
	
	class ConductBombing implements IExecutable
	{
		private static final long serialVersionUID = 5579796391988452213L;
		private int[] m_dice;
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			final IExecutable rollDice = new IExecutable()
			{
				private static final long serialVersionUID = -4097858758514452368L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					rollDice(bridge);
				}
			};
			final IExecutable findCost = new IExecutable()
			{
				private static final long serialVersionUID = 8573539936364094095L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					findCost(bridge);
				}
			};
			// push in reverse order of execution
			m_stack.push(findCost);
			m_stack.push(rollDice);
		}
		
		private void rollDice(final IDelegateBridge bridge)
		{
			{
				final Set<Unit> duplicatesCheckSet1 = new HashSet<Unit>(m_attackingUnits);
				if (m_attackingUnits.size() != duplicatesCheckSet1.size())
				{
					throw new IllegalStateException("Duplicate Units Detected: Original List:" + m_attackingUnits + "  HashSet:" + duplicatesCheckSet1);
				}
			}
			final int rollCount = BattleCalculator.getRolls(m_attackingUnits, m_battleSite, m_attacker, false, true, m_territoryEffects);
			if (rollCount == 0)
			{
				m_dice = null;
				return;
			}
			m_dice = new int[rollCount];
			final boolean isEditMode = BaseEditDelegate.getEditMode(m_data);
			if (isEditMode)
			{
				final String annotation = m_attacker.getName() + " fixing dice to allocate cost of strategic bombing raid against " + m_defender.getName() + " in " + m_battleSite.getName();
				final ITripleaPlayer attacker = (ITripleaPlayer) bridge.getRemotePlayer(m_attacker);
				m_dice = attacker.selectFixedDice(rollCount, 0, true, annotation, m_data.getDiceSides()); // does not take into account bombers with dice sides higher than getDiceSides
			}
			else
			{
				final boolean doNotUseBombingBonus = !games.strategy.triplea.Properties.getUseBombingMaxDiceSidesAndBonus(m_data);
				final String annotation = m_attacker.getName() + " rolling to allocate cost of strategic bombing raid against " + m_defender.getName() + " in " + m_battleSite.getName();
				if (!games.strategy.triplea.Properties.getLL_DAMAGE_ONLY(m_data))
				{
					if (doNotUseBombingBonus)
					{
						// no low luck, and no bonus, so just roll based on the map's dice sides
						m_dice = bridge.getRandom(m_data.getDiceSides(), rollCount, m_attacker, DiceType.BOMBING, annotation);
					}
					else
					{
						// we must use bombing bonus
						int i = 0;
						final int diceSides = m_data.getDiceSides();
						for (final Unit u : m_attackingUnits)
						{
							final int rolls = BattleCalculator.getRolls(u, m_battleSite, m_attacker, false, true, m_territoryEffects);
							if (rolls < 1)
								continue;
							final UnitAttachment ua = UnitAttachment.get(u.getType());
							int maxDice = ua.getBombingMaxDieSides();
							int bonus = ua.getBombingBonus();
							// both could be -1, meaning they were not set. if they were not set, then we use default dice sides for the map, and zero for the bonus.
							if (maxDice < 0)
								maxDice = diceSides;
							if (bonus < 0)
								bonus = 0;
							// now we roll, or don't if there is nothing to roll.
							if (maxDice > 0)
							{
								final int[] dicerolls = bridge.getRandom(maxDice, rolls, m_attacker, DiceType.BOMBING, annotation);
								for (final int die : dicerolls)
								{
									m_dice[i] = die + bonus;
									i++;
								}
							}
							else
							{
								for (int j = 0; j < rolls; j++)
								{
									m_dice[i] = bonus;
									i++;
								}
							}
						}
					}
				}
				else
				{
					int i = 0;
					final int diceSides = m_data.getDiceSides();
					for (final Unit u : m_attackingUnits)
					{
						final int rolls = BattleCalculator.getRolls(u, m_battleSite, m_attacker, false, true, m_territoryEffects);
						if (rolls < 1)
							continue;
						final UnitAttachment ua = UnitAttachment.get(u.getType());
						int maxDice = ua.getBombingMaxDieSides();
						int bonus = ua.getBombingBonus();
						// both could be -1, meaning they were not set. if they were not set, then we use default dice sides for the map, and zero for the bonus.
						if (maxDice < 0 || doNotUseBombingBonus)
							maxDice = diceSides;
						if (bonus < 0 || doNotUseBombingBonus)
							bonus = 0;
						// now, regardless of whether they were set or not, we have to apply "low luck" to them, meaning in this case that we reduce the luck by 2/3.
						if (maxDice >= 5)
						{
							bonus += (maxDice + 1) / 3;
							maxDice = (maxDice + 1) / 3;
						}
						// now we roll, or don't if there is nothing to roll.
						if (maxDice > 0)
						{
							final int[] dicerolls = bridge.getRandom(maxDice, rolls, m_attacker, DiceType.BOMBING, annotation);
							for (final int die : dicerolls)
							{
								m_dice[i] = die + bonus;
								i++;
							}
						}
						else
						{
							for (int j = 0; j < rolls; j++)
							{
								m_dice[i] = bonus;
								i++;
							}
						}
					}
				}
			}
		}
		
		private void addToTargetDiceMap(final Unit attackerUnit, final Die roll, final HashMap<Unit, List<Die>> targetToDiceMap)
		{
			if (m_targets == null || m_targets.isEmpty())
				return;
			final Unit target = getTarget(attackerUnit);
			List<Die> current = targetToDiceMap.get(target);
			if (current == null)
				current = new ArrayList<Die>();
			current.add(roll);
			targetToDiceMap.put(target, current);
		}
		
		private void findCost(final IDelegateBridge bridge)
		{
			// if no planes left after aa fires, this is possible
			if (m_attackingUnits.isEmpty())
			{
				return;
			}
			int damageLimit = TerritoryAttachment.getProduction(m_battleSite);
			int cost = 0;
			final boolean lhtrBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(m_data);
			int index = 0;
			final Boolean limitDamage = isWW2V2() || isLimitSBRDamageToProduction();
			final List<Die> dice = new ArrayList<Die>();
			final HashMap<Unit, List<Die>> targetToDiceMap = new HashMap<Unit, List<Die>>();
			// limit to maxDamage
			for (final Unit attacker : m_attackingUnits)
			{
				final UnitAttachment ua = UnitAttachment.get(attacker.getType());
				int rolls;
				rolls = BattleCalculator.getRolls(attacker, m_battleSite, m_attacker, false, true, m_territoryEffects);
				int costThisUnit = 0;
				if (rolls > 1 && (lhtrBombers || ua.getChooseBestRoll()))
				{
					// LHTR means we select the best Dice roll for the unit
					int max = 0;
					int maxIndex = index;
					int startIndex = index;
					for (int i = 0; i < rolls; i++)
					{
						// +1 since 0 based
						if (m_dice[index] + 1 > max)
						{
							max = m_dice[index] + 1;
							maxIndex = index;
						}
						index++;
					}
					costThisUnit = max;
					// for show
					final Die best = new Die(m_dice[maxIndex]);
					dice.add(best);
					addToTargetDiceMap(attacker, best, targetToDiceMap);
					for (int i = 0; i < rolls; i++)
					{
						if (startIndex != maxIndex)
						{
							final Die notBest = new Die(m_dice[startIndex], -1, DieType.IGNORED);
							dice.add(notBest);
							addToTargetDiceMap(attacker, notBest, targetToDiceMap);
						}
						startIndex++;
					}
				}
				else
				{
					for (int i = 0; i < rolls; i++)
					{
						costThisUnit += m_dice[index] + 1;
						final Die die = new Die(m_dice[index]);
						dice.add(die);
						addToTargetDiceMap(attacker, die, targetToDiceMap);
						index++;
					}
				}
				costThisUnit = Math.max(0, (costThisUnit + TechAbilityAttachment.getBombingBonus(attacker.getType(), attacker.getOwner(), m_data)));
				if (limitDamage)
					costThisUnit = Math.min(costThisUnit, damageLimit);
				cost += costThisUnit;
				if (!m_targets.isEmpty())
					m_bombingRaidDamage.add(getTarget(attacker), costThisUnit);
			}
			// Limit PUs lost if we would like to cap PUs lost at territory value
			if (isPUCap(m_data) || isLimitSBRDamagePerTurn(m_data))
			{
				final int alreadyLost = DelegateFinder.moveDelegate(m_data).PUsAlreadyLost(m_battleSite);
				final int limit = Math.max(0, damageLimit - alreadyLost);
				cost = Math.min(cost, limit);
				if (!m_targets.isEmpty())
				{
					for (final Unit u : m_bombingRaidDamage.keySet())
					{
						if (m_bombingRaidDamage.getInt(u) > limit)
							m_bombingRaidDamage.put(u, limit);
					}
				}
			}
			// If we damage units instead of territories
			if (isDamageFromBombingDoneToUnitsInsteadOfTerritories())
			{
				// at this point, m_bombingRaidDamage should contain all units that m_targets contains
				if (!m_targets.keySet().containsAll(m_bombingRaidDamage.keySet()))
					throw new IllegalStateException("targets should contain all damaged units");
				for (final Unit current : m_bombingRaidDamage.keySet())
				{
					int currentUnitCost = m_bombingRaidDamage.getInt(current);
					// determine the max allowed damage
					// UnitAttachment ua = UnitAttachment.get(current.getType());
					final TripleAUnit taUnit = (TripleAUnit) current;
					damageLimit = taUnit.getHowMuchMoreDamageCanThisUnitTake(current, m_battleSite);
					if (m_bombingRaidDamage.getInt(current) > damageLimit)
					{
						m_bombingRaidDamage.put(current, damageLimit);
						cost = (cost - currentUnitCost) + damageLimit;
						currentUnitCost = m_bombingRaidDamage.getInt(current);
					}
					final int totalDamage = taUnit.getUnitDamage() + currentUnitCost;
					// display the results
					getDisplay(bridge).bombingResults(m_battleID, dice, currentUnitCost);
					if (currentUnitCost > 0)
					{
						// play a sound
						bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BOMBING_STRATEGIC, m_attacker.getName()); // play sound
					}
					// Record production lost
					DelegateFinder.moveDelegate(m_data).PUsLost(m_battleSite, currentUnitCost);
					// apply the hits to the targets
					final IntegerMap<Unit> damageMap = new IntegerMap<Unit>();
					damageMap.put(current, totalDamage);
					bridge.addChange(ChangeFactory.bombingUnitDamage(damageMap));
					bridge.getHistoryWriter().addChildToEvent("Bombing raid in " + m_battleSite.getName() + " rolls: " + MyFormatter.asDice(targetToDiceMap.get(current)) + " and causes: "
								+ currentUnitCost + " damage to unit: " + current.getType().getName());
					getRemote(bridge).reportMessage("Bombing raid in " + m_battleSite.getName() + " rolls: " + MyFormatter.asDice(targetToDiceMap.get(current)) + " and causes: " + currentUnitCost
								+ " damage to unit: " + current.getType().getName(), "Bombing raid causes " + currentUnitCost + " damage to " + current.getType().getName());
				}
			}
			else
			{
				// Record PUs lost
				DelegateFinder.moveDelegate(m_data).PUsLost(m_battleSite, cost);
				cost *= Properties.getPU_Multiplier(m_data);
				getDisplay(bridge).bombingResults(m_battleID, dice, cost);
				if (cost > 0)
				{
					// play a sound
					bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BOMBING_STRATEGIC, m_attacker.getName()); // play sound
				}
				// get resources
				final Resource PUs = m_data.getResourceList().getResource(Constants.PUS);
				final int have = m_defender.getResources().getQuantity(PUs);
				final int toRemove = Math.min(cost, have);
				final Change change = ChangeFactory.changeResourcesChange(m_defender, PUs, -toRemove);
				bridge.addChange(change);
				bridge.getHistoryWriter().addChildToEvent("Bombing raid in " + m_battleSite.getName() + " rolls: " + MyFormatter.asDice(m_dice) + " and costs: " + cost
							+ " " + MyFormatter.pluralize("PU", cost) + ".");
			}
			m_bombingRaidTotal = cost;
		}
	}
	
	@Override
	public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units, final IDelegateBridge bridge, final boolean withdrawn)
	{
		// should never happen
		// throw new IllegalStateException("StrategicBombingRaidBattle should not have any preceding battle with which to possibly remove dependents from");
	}
}
