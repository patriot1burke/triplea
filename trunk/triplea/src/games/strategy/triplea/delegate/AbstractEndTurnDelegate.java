/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public Licensec
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * EndTurnDelegate.java
 * 
 * Created on November 2, 2001, 12:30 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.RelationshipTypeAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.remote.IAbstractEndTurnDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          At the end of the turn collect income.
 */
public abstract class AbstractEndTurnDelegate extends BaseDelegate implements IAbstractEndTurnDelegate
{
	private boolean m_needToInitialize = true;
	private boolean m_hasPostedTurnSummary = false;
	
	private boolean doBattleShipsRepairEndOfTurn()
	{
		return games.strategy.triplea.Properties.getBattleships_Repair_At_End_Of_Round(getData());
	}
	
	private boolean isGiveUnitsByTerritory()
	{
		return games.strategy.triplea.Properties.getGiveUnitsByTerritory(getData());
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(final IDelegateBridge aBridge)
	{
		// figure out our current PUs before we do anything else, including super methods
		final GameData data = aBridge.getData();
		final Resource PUs = data.getResourceList().getResource(Constants.PUS);
		final int leftOverPUs = aBridge.getPlayerID().getResources().getQuantity(PUs);
		super.start(aBridge);
		if (!m_needToInitialize)
			return;
		m_hasPostedTurnSummary = false;
		// can't collect unless you own your own capital
		final PlayerAttachment pa = PlayerAttachment.get(m_player);
		final List<Territory> capitalsListOriginal = new ArrayList<Territory>(TerritoryAttachment.getAllCapitals(m_player, data));
		final List<Territory> capitalsListOwned = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(m_player, data));
		if ((!capitalsListOriginal.isEmpty() && capitalsListOwned.isEmpty()) || (pa != null && pa.getRetainCapitalProduceNumber() > capitalsListOwned.size()))
			return;
		// just collect resources
		final Collection<Territory> territories = data.getMap().getTerritoriesOwnedBy(m_player);
		int toAdd = getProduction(territories);
		final int blockadeLoss = getProductionLoss(m_player, data);
		toAdd -= blockadeLoss;
		toAdd *= Properties.getPU_Multiplier(data);
		int total = m_player.getResources().getQuantity(PUs) + toAdd;
		String transcriptText;
		if (blockadeLoss == 0)
			transcriptText = m_player.getName() + " collect " + toAdd + MyFormatter.pluralize(" PU", toAdd) + "; end with " + total + MyFormatter.pluralize(" PU", total) + " total";
		else
			transcriptText = m_player.getName() + " collect " + toAdd + MyFormatter.pluralize(" PU", toAdd) + " (" + blockadeLoss + " lost to blockades)" + "; end with " + total
						+ MyFormatter.pluralize(" PU", total) + " total";
		aBridge.getHistoryWriter().startEvent(transcriptText);
		if (isWarBonds(m_player))
		{
			final int bonds = rollWarBonds(aBridge);
			total += bonds;
			toAdd += bonds;
			transcriptText = m_player.getName() + " collect " + bonds + MyFormatter.pluralize(" PU", bonds) + " from War Bonds; end with " + total + MyFormatter.pluralize(" PU", total) + " total";
			aBridge.getHistoryWriter().startEvent(transcriptText);
		}
		if (total < 0)
		{
			toAdd -= total;
			total = 0;
		}
		final Change change = ChangeFactory.changeResourcesChange(m_player, PUs, toAdd);
		aBridge.addChange(change);
		if (data.getProperties().get(Constants.PACIFIC_THEATER, false) && pa != null)
		{
			final Change changeVP = (ChangeFactory.attachmentPropertyChange(pa, (new Integer(Integer.parseInt(pa.getVps()) + (toAdd / 10 + Integer.parseInt(pa.getCaptureVps()) / 10))).toString(),
						"vps"));
			final Change changeCapVP = ChangeFactory.attachmentPropertyChange(pa, "0", "captureVps");
			final CompositeChange ccVP = new CompositeChange(changeVP, changeCapVP);
			aBridge.addChange(ccVP);
		}
		
		addOtherResources(aBridge);
		
		doNationalObjectivesAndOtherEndTurnEffects(aBridge);
		
		if (doBattleShipsRepairEndOfTurn())
		{
			MoveDelegate.repairBattleShips(aBridge, aBridge.getPlayerID(), false);
		}
		if (isGiveUnitsByTerritory() && pa != null && pa.getGiveUnitControl() != null && !pa.getGiveUnitControl().isEmpty())
		{
			changeUnitOwnership(aBridge);
		}
		// now we do upkeep costs, including upkeep cost as a percentage of our entire income for this turn (including NOs)
		final int currentPUs = m_player.getResources().getQuantity(PUs);
		final float gainedPUS = Math.max(0, currentPUs - leftOverPUs);
		int relationshipUpkeepCostFlat = 0;
		int relationshipUpkeepCostPercentage = 0;
		int relationshipUpkeepTotalCost = 0;
		for (final Relationship r : data.getRelationshipTracker().getRelationships(m_player))
		{
			final String[] upkeep = r.getRelationshipType().getRelationshipTypeAttachment().getUpkeepCost().split(":");
			if (upkeep.length == 1 || upkeep[1].equals(RelationshipTypeAttachment.UPKEEP_FLAT))
				relationshipUpkeepCostFlat += Integer.parseInt(upkeep[0]);
			else if (upkeep[1].equals(RelationshipTypeAttachment.UPKEEP_PERCENTAGE))
				relationshipUpkeepCostPercentage += Integer.parseInt(upkeep[0]);
		}
		relationshipUpkeepCostPercentage = Math.min(100, relationshipUpkeepCostPercentage);
		if (relationshipUpkeepCostPercentage != 0)
		{
			relationshipUpkeepTotalCost += Math.round(gainedPUS * (relationshipUpkeepCostPercentage) / 100f);
		}
		if (relationshipUpkeepCostFlat != 0)
		{
			relationshipUpkeepTotalCost += relationshipUpkeepCostFlat;
		}
		// we can't remove more than we have, and we also must flip the sign
		relationshipUpkeepTotalCost = Math.min(currentPUs, relationshipUpkeepTotalCost);
		relationshipUpkeepTotalCost = -1 * relationshipUpkeepTotalCost;
		if (relationshipUpkeepTotalCost != 0)
		{
			final int newTotal = currentPUs + relationshipUpkeepTotalCost;
			transcriptText = m_player.getName() + (relationshipUpkeepTotalCost < 0 ? " pays " : " taxes ") + (-1 * relationshipUpkeepTotalCost)
						+ MyFormatter.pluralize(" PU", relationshipUpkeepTotalCost) + " in order to maintain current relationships with other players, and ends the turn with " + newTotal
						+ MyFormatter.pluralize(" PU", newTotal);
			aBridge.getHistoryWriter().startEvent(transcriptText);
			final Change upkeep = ChangeFactory.changeResourcesChange(m_player, PUs, relationshipUpkeepTotalCost);
			aBridge.addChange(upkeep);
		}
		m_needToInitialize = false;
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		super.end();
		m_needToInitialize = true;
		DelegateFinder.battleDelegate(getData()).getBattleTracker().clear();
	}
	
	@Override
	public Serializable saveState()
	{
		final EndTurnExtendedDelegateState state = new EndTurnExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_needToInitialize = m_needToInitialize;
		state.m_hasPostedTurnSummary = m_hasPostedTurnSummary;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final EndTurnExtendedDelegateState s = (EndTurnExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_needToInitialize = s.m_needToInitialize;
		m_hasPostedTurnSummary = s.m_hasPostedTurnSummary;
	}
	
	private int rollWarBonds(final IDelegateBridge aBridge)
	{
		final PlayerID player = aBridge.getPlayerID();
		final int count = 1;
		final int sides = aBridge.getData().getDiceSides();
		final String annotation = player.getName() + " roll to resolve War Bonds: ";
		DiceRoll dice;
		dice = DiceRoll.rollNDice(aBridge, count, sides, annotation);
		final int total = dice.getDie(0).getValue() + 1;
		// TODO kev add dialog showing dice when built
		getRemotePlayer(player).reportMessage(annotation + total, annotation + total);
		return total;
	}
	
	private ITripleaPlayer getRemotePlayer(final PlayerID player)
	{
		return (ITripleaPlayer) m_bridge.getRemote(player);
	}
	
	private void changeUnitOwnership(final IDelegateBridge aBridge)
	{
		final PlayerID Player = aBridge.getPlayerID();
		final PlayerAttachment pa = PlayerAttachment.get(Player);
		final Collection<PlayerID> PossibleNewOwners = pa.getGiveUnitControl();
		final Collection<Territory> territories = aBridge.getData().getMap().getTerritories();
		for (Territory currTerritory  : territories) {
			final TerritoryAttachment ta = (TerritoryAttachment) currTerritory.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
			// if ownership should change in this territory
			if (ta != null && ta.getChangeUnitOwners() != null && !ta.getChangeUnitOwners().isEmpty())
			{
				final Collection<PlayerID> terrNewOwners = ta.getChangeUnitOwners();
				for (final PlayerID terrNewOwner : terrNewOwners)
				{
					if (PossibleNewOwners.contains(terrNewOwner))
					{
						// PlayerOwnerChange
						final Collection<Unit> units = currTerritory.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitOwnedBy(Player), Matches.UnitCanBeGivenByTerritoryTo(terrNewOwner)));
						final Change changeOwner = ChangeFactory.changeOwner(units, terrNewOwner, currTerritory);
						aBridge.getHistoryWriter().addChildToEvent(changeOwner.toString());
						aBridge.addChange(changeOwner);
					}
				}
			}
		}
	}
	
	protected abstract void addOtherResources(IDelegateBridge bridge);
	
	protected abstract void doNationalObjectivesAndOtherEndTurnEffects(IDelegateBridge bridge);
	
	protected int getProduction(final Collection<Territory> territories)
	{
		return getProduction(territories, getData());
	}
	
	public static int getProduction(final Collection<Territory> territories, final GameData data)
	{
		int value = 0;
		for (Territory current  : territories) {
			final TerritoryAttachment attatchment = (TerritoryAttachment) current.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
			if (attatchment == null)
				throw new IllegalStateException("No attachment for owned territory:" + current.getName());
			// Check if territory is originally owned convoy center
			if (Matches.territoryCanCollectIncomeFrom(current.getOwner(), data).match(current))
				value += attatchment.getProduction();
		}
		return value;
	}
	
	// finds losses due to blockades etc, positive value returned.
	protected int getProductionLoss(final PlayerID player, final GameData data)
	{
		final Collection<Territory> blockable = Match.getMatches(data.getMap().getTerritories(), Matches.territoryIsBlockadeZone);
		final Match<Unit> enemyUnits = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		int totalLoss = 0;
		for (final Territory b : blockable)
		{
			int maxLoss = 0;
			for (final Territory m : data.getMap().getNeighbors(b))
			{
				if (m.getOwner().equals(player))
					maxLoss += TerritoryAttachment.get(m).getProduction();
			}
			int loss = 0;
			final Collection<Unit> enemies = Match.getMatches(b.getUnits().getUnits(), enemyUnits);
			for (final Unit u : enemies)
				loss += UnitAttachment.get(u.getType()).getBlockade();
			totalLoss += Math.min(maxLoss, loss);
		}
		return totalLoss;
	}
	
	private boolean isWarBonds(final PlayerID player)
	{
		final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta != null)
			return ta.hasWarBonds();
		return false;
	}
	
	public void setHasPostedTurnSummary(final boolean hasPostedTurnSummary)
	{
		m_hasPostedTurnSummary = hasPostedTurnSummary;
	}
	
	public boolean getHasPostedTurnSummary()
	{
		return m_hasPostedTurnSummary;
	}
	
	public boolean postTurnSummary(final PBEMMessagePoster poster)
	{
		m_hasPostedTurnSummary = poster.post(m_bridge.getHistoryWriter());
		return m_hasPostedTurnSummary;
	}
	
	@Override
	public String getName()
	{
		return m_name;
	}
	
	@Override
	public String getDisplayName()
	{
		return m_displayName;
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return IAbstractEndTurnDelegate.class;
	}
}


@SuppressWarnings("serial")
class EndTurnExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
	public boolean m_needToInitialize;
	public boolean m_hasPostedTurnSummary;
}
