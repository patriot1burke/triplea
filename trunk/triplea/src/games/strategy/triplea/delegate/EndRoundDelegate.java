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
 * EndRoundDelegate.java
 * 
 * Created on January 18, 2002, 9:50 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseTripleADelegate;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.message.IRemote;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.AbstractTriggerAttachment;
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.LocalizeHTML;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JOptionPane;

/**
 * 
 * A delegate used to check for end of game conditions.
 * 
 * @author Sean Bridges
 */
public class EndRoundDelegate extends BaseTripleADelegate
{
	private boolean m_gameOver = false;
	private Collection<PlayerID> m_winners = new ArrayList<PlayerID>();
	
	/** Creates a new instance of EndRoundDelegate */
	public EndRoundDelegate()
	{
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		if (m_gameOver)
			return;
		String victoryMessage = null;
		final GameData data = getData();
		if (isPacificTheater())
		{
			final PlayerID japanese = data.getPlayerList().getPlayerID(Constants.JAPANESE);
			final PlayerAttachment pa = PlayerAttachment.get(japanese);
			if (pa != null && pa.getVps() >= 22)
			{
				victoryMessage = "Axis achieve VP victory";
				m_bridge.getHistoryWriter().startEvent(victoryMessage);
				final Collection<PlayerID> winners = data.getAllianceTracker().getPlayersInAlliance(data.getAllianceTracker().getAlliancesPlayerIsIn(japanese).iterator().next());
				signalGameOver(victoryMessage, winners, m_bridge);
			}
		}
		// Check for Winning conditions
		if (isTotalVictory()) // Check for Win by Victory Cities
		{
			victoryMessage = " achieve TOTAL VICTORY with ";
			checkVictoryCities(m_bridge, victoryMessage, " Total Victory VCs");
		}
		if (isHonorableSurrender())
		{
			victoryMessage = " achieve an HONORABLE VICTORY with ";
			checkVictoryCities(m_bridge, victoryMessage, " Honorable Victory VCs");
		}
		if (isProjectionOfPower())
		{
			victoryMessage = " achieve victory through a PROJECTION OF POWER with ";
			checkVictoryCities(m_bridge, victoryMessage, " Projection of Power VCs");
		}
		if (isEconomicVictory()) // Check for regular economic victory
		{
			final Iterator<String> allianceIter = data.getAllianceTracker().getAlliances().iterator();
			String allianceName = null;
			while (allianceIter.hasNext())
			{
				allianceName = allianceIter.next();
				final int victoryAmount = getEconomicVictoryAmount(data, allianceName);
				final Set<PlayerID> teamMembers = data.getAllianceTracker().getPlayersInAlliance(allianceName);
				final Iterator<PlayerID> teamIter = teamMembers.iterator();
				int teamProd = 0;
				while (teamIter.hasNext())
				{
					final PlayerID player = teamIter.next();
					teamProd += getProduction(player);
					if (teamProd >= victoryAmount)
					{
						victoryMessage = allianceName + " achieve economic victory";
						m_bridge.getHistoryWriter().startEvent(victoryMessage);
						final Collection<PlayerID> winners = data.getAllianceTracker().getPlayersInAlliance(allianceName);
						// Added this to end the game on victory conditions
						signalGameOver(victoryMessage, winners, m_bridge);
					}
				}
			}
		}
		// now check for generic trigger based victories
		if (isTriggeredVictory())
		{
			// First set up a match for what we want to have fire as a default in this delegate. List out as a composite match OR.
			// use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
			final Match<TriggerAttachment> endRoundDelegateTriggerMatch = new CompositeMatchAnd<TriggerAttachment>(
						AbstractTriggerAttachment.availableUses,
						AbstractTriggerAttachment.whenOrDefaultMatch(null, null),
						new CompositeMatchOr<TriggerAttachment>(
									TriggerAttachment.activateTriggerMatch(),
									TriggerAttachment.victoryMatch()));
			// get all possible triggers based on this match.
			final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
						new HashSet<PlayerID>(data.getPlayerList().getPlayers()), endRoundDelegateTriggerMatch, m_bridge);
			if (!toFirePossible.isEmpty())
			{
				// get all conditions possibly needed by these triggers, and then test them.
				final HashMap<ICondition, Boolean> testedConditions = TriggerAttachment.collectTestsForAllTriggers(toFirePossible, m_bridge);
				// get all triggers that are satisfied based on the tested conditions.
				final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<TriggerAttachment>(Match.getMatches(toFirePossible, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));
				// now list out individual types to fire, once for each of the matches above.
				TriggerAttachment.triggerActivateTriggerOther(testedConditions, toFireTestedAndSatisfied, m_bridge, null, null, true, true, true, true);
				TriggerAttachment.triggerVictory(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true, true); // will call signalGameOver itself
			}
		}
		
		if (isWW2V2() || isWW2V3())
			return;
		final PlayerList playerList = data.getPlayerList();
		// now test older maps that only use these 5 players, to see if someone has won
		final PlayerID russians = playerList.getPlayerID(Constants.RUSSIANS);
		final PlayerID germans = playerList.getPlayerID(Constants.GERMANS);
		final PlayerID british = playerList.getPlayerID(Constants.BRITISH);
		final PlayerID japanese = playerList.getPlayerID(Constants.JAPANESE);
		final PlayerID americans = playerList.getPlayerID(Constants.AMERICANS);
		if (germans == null || russians == null || british == null || japanese == null || americans == null || playerList.size() > 5)
			return;
		// Quick check to see who still owns their own capital
		final boolean russia = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(russians, data).getOwner().equals(russians);
		final boolean germany = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(germans, data).getOwner().equals(germans);
		final boolean britain = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(british, data).getOwner().equals(british);
		final boolean japan = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(japanese, data).getOwner().equals(japanese);
		final boolean america = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(americans, data).getOwner().equals(americans);
		int count = 0;
		if (!russia)
			count++;
		if (!britain)
			count++;
		if (!america)
			count++;
		victoryMessage = " achieve a military victory";
		if (germany && japan && count >= 2)
		{
			m_bridge.getHistoryWriter().startEvent("Axis" + victoryMessage);
			final Collection<PlayerID> winners = data.getAllianceTracker().getPlayersInAlliance("Axis");
			signalGameOver("Axis" + victoryMessage, winners, m_bridge);
		}
		if (russia && !germany && britain && !japan && america)
		{
			m_bridge.getHistoryWriter().startEvent("Allies" + victoryMessage);
			final Collection<PlayerID> winners = data.getAllianceTracker().getPlayersInAlliance("Allies");
			signalGameOver("Allies" + victoryMessage, winners, m_bridge);
		}
	}
	
	@Override
	public void end()
	{
		super.end();
		final GameData data = getData();
		if (games.strategy.triplea.Properties.getTriggers(data))
		{
			final CompositeChange change = new CompositeChange();
			for (final PlayerID player : data.getPlayerList().getPlayers())
			{
				change.add(AbstractTriggerAttachment.triggerSetUsedForThisRound(player, m_bridge));
			}
			if (!change.isEmpty())
			{
				m_bridge.getHistoryWriter().startEvent("Setting uses for triggers used this round.");
				m_bridge.addChange(change);
			}
		}
	}
	
	/*private BattleTracker getBattleTracker()
	{
		return DelegateFinder.battleDelegate(getData()).getBattleTracker();
	}*/
	
	@Override
	public Serializable saveState()
	{
		final EndRoundExtendedDelegateState state = new EndRoundExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_gameOver = m_gameOver;
		state.m_winners = m_winners;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final EndRoundExtendedDelegateState s = (EndRoundExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_gameOver = s.m_gameOver;
		m_winners = s.m_winners;
	}
	
	public boolean delegateCurrentlyRequiresUserInput()
	{
		return false;
	}
	
	private void checkVictoryCities(final IDelegateBridge aBridge, final String victoryMessage, final String victoryType)
	{
		final GameData data = aBridge.getData();
		final Iterator<String> allianceIter = data.getAllianceTracker().getAlliances().iterator();
		String allianceName = null;
		final Collection<Territory> territories = data.getMap().getTerritories();
		while (allianceIter.hasNext())
		{
			allianceName = allianceIter.next();
			final int vcAmount = getVCAmount(data, allianceName, victoryType);
			final Set<PlayerID> teamMembers = data.getAllianceTracker().getPlayersInAlliance(allianceName);
			int teamVCs = 0;
			for (final Territory t : territories)
			{
				if (Matches.isTerritoryOwnedBy(teamMembers).match(t))
				{
					final TerritoryAttachment ta = TerritoryAttachment.get(t);
					if (ta != null)
					{
						teamVCs += ta.getVictoryCity();
					}
				}
			}
			if (teamVCs >= vcAmount)
			{
				aBridge.getHistoryWriter().startEvent(allianceName + victoryMessage + vcAmount + " Victory Cities!");
				final Collection<PlayerID> winners = data.getAllianceTracker().getPlayersInAlliance(allianceName);
				// Added this to end the game on victory conditions
				signalGameOver(allianceName + victoryMessage + vcAmount + " Victory Cities!", winners, aBridge);
			}
		}
	}
	
	private int getEconomicVictoryAmount(final GameData data, final String alliance)
	{
		return data.getProperties().get(alliance + " Economic Victory", 200);
	}
	
	private int getVCAmount(final GameData data, final String alliance, final String type)
	{
		int defaultVC = 20;
		if (type.equals(" Total Victory VCs"))
		{
			defaultVC = 18;
		}
		else if (type.equals(" Honorable Victory VCs"))
		{
			defaultVC = 15;
		}
		else if (type.equals(" Projection of Power VCs"))
		{
			defaultVC = 13;
		}
		return data.getProperties().get((alliance + type), defaultVC);
	}
	
	/**
	 * Notify all players that the game is over.
	 * 
	 * @param status
	 *            the "game over" text to be displayed to each user.
	 */
	public void signalGameOver(final String status, final Collection<PlayerID> winners, final IDelegateBridge aBridge)
	{
		// TO NOT USE m_bridge, because it might be null here! use aBridge instead.
		// If the game is over, we need to be able to alert all UIs to that fact.
		// The display object can send a message to all UIs.
		if (!m_gameOver)
		{
			m_gameOver = true;
			m_winners = winners;
			aBridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_GAME_WON,
						((m_winners != null && !m_winners.isEmpty()) ? m_winners.iterator().next().getName() : PlayerID.NULL_PLAYERID.getName()));
			// send a message to everyone's screen except the HOST (there is no 'current player' for the end round delegate)
			final String title = "Victory Achieved" + (winners.isEmpty() ? "" : " by " + MyFormatter.defaultNamedToTextList(winners, ", ", false));
			getDisplay(aBridge).reportMessageToAll(("<html>" + status + "</html>"), title, true, false, true); // we send the bridge, because we can call this method from outside this delegate, which means our local copy of m_bridge could be null.
			final boolean stopGame;
			if (HeadlessGameServer.headless())
			{
				// a terrible dirty hack, but I can't think of a better way to do it right now. If we are headless, end the game.
				stopGame = true;
			}
			else
			{
				// now tell the HOST, and see if they want to continue the game.
				String displayMessage = LocalizeHTML.localizeImgLinksInHTML(status);
				if (displayMessage.endsWith("</body>"))
					displayMessage = displayMessage.substring(0, displayMessage.length() - "</body>".length()) + "</br><p>Do you want to continue?</p></body>";
				else
					displayMessage = displayMessage + "</br><p>Do you want to continue?</p>";
				// this is currently the ONLY instance of JOptionPane that is allowed outside of the UI classes. maybe there is a better way?
				stopGame = (JOptionPane.OK_OPTION != EventThreadJOptionPane.showConfirmDialog(null, ("<html>" + displayMessage + "</html>"), "Continue Game?  ("
							+ title + ")", JOptionPane.YES_NO_OPTION, new CountDownLatchHandler(true)));
			}
			if (stopGame)
				aBridge.stopGameSequence();
		}
	}
	
	/**
	 * if null, the game is not over yet
	 * 
	 * @return
	 */
	public Collection<PlayerID> getWinners()
	{
		if (!m_gameOver)
			return null;
		return m_winners;
	}
	
	private boolean isWW2V2()
	{
		return games.strategy.triplea.Properties.getWW2V2(getData());
	}
	
	private boolean isWW2V3()
	{
		return games.strategy.triplea.Properties.getWW2V3(getData());
	}
	
	private boolean isPacificTheater()
	{
		return games.strategy.triplea.Properties.getPacificTheater(getData());
	}
	
	private boolean isTotalVictory()
	{
		return games.strategy.triplea.Properties.getTotalVictory(getData());
	}
	
	private boolean isHonorableSurrender()
	{
		return games.strategy.triplea.Properties.getHonorableSurrender(getData());
	}
	
	private boolean isProjectionOfPower()
	{
		return games.strategy.triplea.Properties.getProjectionOfPower(getData());
	}
	
	private boolean isEconomicVictory()
	{
		return games.strategy.triplea.Properties.getEconomicVictory(getData());
	}
	
	private boolean isTriggeredVictory()
	{
		return games.strategy.triplea.Properties.getTriggeredVictory(getData());
	}
	
	public int getProduction(final PlayerID id)
	{
		int sum = 0;
		final Iterator<Territory> territories = getData().getMap().iterator();
		while (territories.hasNext())
		{
			final Territory current = territories.next();
			if (current.getOwner().equals(id))
			{
				sum += TerritoryAttachment.getProduction(current);
			}
		}
		return sum;
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return null;
	}
}


class EndRoundExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 8770361633528374127L;
	Serializable superState;
	// add other variables here:
	public boolean m_gameOver = false;
	public Collection<PlayerID> m_winners = new ArrayList<PlayerID>();
}
