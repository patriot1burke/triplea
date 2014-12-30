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
 * TripleAPlayer.java
 * 
 * Created on November 2, 2001, 8:45 PM
 */
package games.strategy.triplea;

import games.strategy.common.player.AbstractHumanPlayer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.net.GUID;
import games.strategy.sound.DefaultSoundChannel;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UserActionAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.dataObjects.FightBattleDetails;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.delegate.dataObjects.TechRoll;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.BattleDisplay;
import games.strategy.triplea.ui.PlaceData;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ButtonModel;
import javax.swing.SwingUtilities;

/**
 * As a rule, nothing that changes GameData should be in here.
 * It should be using a Change done in a delegate, and done through an IDelegate, which we get through getPlayerBridge().getRemote()
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TripleAPlayer extends AbstractHumanPlayer<TripleAFrame> implements IGamePlayer, ITripleaPlayer
{
	private boolean m_soundPlayedAlreadyCombatMove = false;
	private boolean m_soundPlayedAlreadyNonCombatMove = false;
	private boolean m_soundPlayedAlreadyPurchase = false;
	
	private boolean m_soundPlayedAlreadyTechnology = false;
	private boolean m_soundPlayedAlreadyBattle = false;
	private boolean m_soundPlayedAlreadyEndTurn = false;
	private boolean m_soundPlayedAlreadyPlacement = false;
	
	/** Creates new TripleAPlayer */
	public TripleAPlayer(final String name, final String type)
	{
		super(name, type);
	}
	
	public void reportError(final String error)
	{
		m_ui.notifyError(error);
	}
	
	public void reportMessage(final String message, final String title)
	{
		m_ui.notifyMessage(message, title);
	}
	
	@Override
	public void start(final String name)
	{
		super.start(name); // must call super.start
		if (getPlayerBridge().isGameOver())
		{
			return;
		}
		if (m_ui == null)
		{
			System.out.println("Game frame is null, but entered player step: " + name + " for player: " + this.toString());
			return; // headless games shouldn't get here, but lets try returning anyway
		}
		// TODO: parsing which UI thing we should run based on the string name of a possibly extended delegate class seems like a bad way of doing this whole method. however i can't think of anything better right now.
		
		// This is how we find out our game step: getGameData().getSequence().getStep()
		// The game step contains information, like the exact delegate and the delegate's class, that we can further use if needed.
		// This is how we get our communication bridge for affecting the gamedata: (ISomeDelegate) getPlayerBridge().getRemote()
		// We should never touch the game data directly. All changes to game data are done through the remote, which then changes the game using the DelegateBridge -> change factory
		
		m_ui.requiredTurnSeries(getPlayerID());
		boolean badStep = false;
		enableEditModeMenu();
		if (name.endsWith("Tech"))
		{
			tech();
		}
		else if (name.endsWith("TechActivation"))
		{
		} // the delegate handles everything
		else if (name.endsWith("Bid") || name.endsWith("Purchase"))
		{
			purchase(GameStepPropertiesHelper.isBid(getGameData()));
		}
		else if (name.endsWith("Move"))
		{
			final boolean nonCombat = GameStepPropertiesHelper.isNonCombatMove(getGameData(), false);
			move(nonCombat, name);
			if (!nonCombat)
			{
				m_ui.waitForMoveForumPoster(getPlayerID(), getPlayerBridge());
				// TODO only do forum post if there is a combat
			}
		}
		else if (name.endsWith("Battle"))
		{
			battle();
		}
		else if (name.endsWith("Place"))
		{
			place(GameStepPropertiesHelper.isBid(getGameData()));
		}
		else if (name.endsWith("Politics"))
		{
			politics(true);
		}
		else if (name.endsWith("UserActions"))
		{
			userActions(true);
		}
		else if (name.endsWith("EndTurn"))
		{
			endTurn();
			// reset our sounds
			m_soundPlayedAlreadyCombatMove = false;
			m_soundPlayedAlreadyNonCombatMove = false;
			m_soundPlayedAlreadyPurchase = false;
			m_soundPlayedAlreadyTechnology = false;
			m_soundPlayedAlreadyBattle = false;
			m_soundPlayedAlreadyEndTurn = false;
			m_soundPlayedAlreadyPlacement = false;
		}
		else
		{
			badStep = true;
		}
		disableEditModeMenu();
		if (badStep)
		{
			throw new IllegalArgumentException("Unrecognized step name:" + name);
		}
	}
	
	private void enableEditModeMenu()
	{
		try
		{
			m_ui.setEditDelegate((IEditDelegate) getPlayerBridge().getRemotePersistentDelegate("edit"));
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_ui.getEditModeButtonModel().addActionListener(m_editModeAction);
				m_ui.getEditModeButtonModel().setEnabled(true);
			}
		});
	}
	
	private void disableEditModeMenu()
	{
		m_ui.setEditDelegate(null);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_ui.getEditModeButtonModel().setEnabled(false);
				m_ui.getEditModeButtonModel().removeActionListener(m_editModeAction);
			}
		});
	}
	
	private final AbstractAction m_editModeAction = new AbstractAction()
	{
		private static final long serialVersionUID = -8076017427171022731L;
		
		public void actionPerformed(final ActionEvent ae)
		{
			final boolean editMode = ((ButtonModel) ae.getSource()).isSelected();
			try
			{
				// Set edit mode
				// All GameDataChangeListeners will be notified upon success
				final IEditDelegate editDelegate = (IEditDelegate) getPlayerBridge().getRemotePersistentDelegate("edit");
				editDelegate.setEditMode(editMode);
			} catch (final Exception e)
			{
				e.printStackTrace();
				// toggle back to previous state since setEditMode failed
				m_ui.getEditModeButtonModel().setSelected(!m_ui.getEditModeButtonModel().isSelected());
			}
		}
	};
	
	private void politics(final boolean firstRun)
	{
		if (getPlayerBridge().isGameOver())
		{
			return;
		}
		final IPoliticsDelegate iPoliticsDelegate;
		try
		{
			iPoliticsDelegate = (IPoliticsDelegate) getPlayerBridge().getRemoteDelegate();
		} catch (final ClassCastException e)
		{
			final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
						+ getPlayerBridge().getRemoteDelegate().getClass();
			System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
			e.printStackTrace();
			throw new IllegalStateException(errorContext, e);
		}
		/*
		if (!iPoliticsDelegate.stuffToDoInThisDelegate())
			return;
		*/
		final PoliticalActionAttachment actionChoice = m_ui.getPoliticalActionChoice(getPlayerID(), firstRun, iPoliticsDelegate);
		if (actionChoice != null)
		{
			iPoliticsDelegate.attemptAction(actionChoice);
			politics(false);
		}
	}
	
	private void userActions(final boolean firstRun)
	{
		if (getPlayerBridge().isGameOver())
		{
			return;
		}
		final IUserActionDelegate iUserActionDelegate;
		try
		{
			iUserActionDelegate = (IUserActionDelegate) getPlayerBridge().getRemoteDelegate();
		} catch (final ClassCastException e)
		{
			final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
						+ getPlayerBridge().getRemoteDelegate().getClass();
			System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
			e.printStackTrace();
			throw new IllegalStateException(errorContext, e);
		}
		/*
		if (!iUserActionDelegate.stuffToDoInThisDelegate())
			return;
		*/
		final UserActionAttachment actionChoice = m_ui.getUserActionChoice(getPlayerID(), firstRun, iUserActionDelegate);
		if (actionChoice != null)
		{
			iUserActionDelegate.attemptAction(actionChoice);
			userActions(false);
		}
	}
	
	public boolean acceptAction(final PlayerID playerSendingProposal, final String acceptanceQuestion, final boolean politics)
	{
		final GameData data = getGameData();
		if (!getPlayerID().amNotDeadYet(data) || getPlayerBridge().isGameOver())
		{
			return true;
		}
		return m_ui.acceptAction(playerSendingProposal, "To " + getPlayerID().getName() + ": " + acceptanceQuestion, politics);
	}
	
	private void tech()
	{
		if (getPlayerBridge().isGameOver())
		{
			return;
		}
		final ITechDelegate techDelegate;
		try
		{
			techDelegate = (ITechDelegate) getPlayerBridge().getRemoteDelegate();
		} catch (final ClassCastException e)
		{
			final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
						+ getPlayerBridge().getRemoteDelegate().getClass();
			System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
			e.printStackTrace();
			throw new IllegalStateException(errorContext, e);
		}
		/*
		if (!techDelegate.stuffToDoInThisDelegate())
			return;
		*/
		final PlayerID id = getPlayerID();
		// play a sound for this phase
		if (!m_soundPlayedAlreadyTechnology)
		{
			DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_PHASE_TECHNOLOGY, id.getName());
			m_soundPlayedAlreadyTechnology = true;
		}
		final TechRoll techRoll = m_ui.getTechRolls(id);
		if (techRoll != null)
		{
			
			final TechResults techResults = techDelegate.rollTech(techRoll.getRolls(), techRoll.getTech(), techRoll.getNewTokens(),
						techRoll.getWhoPaysHowMuch());
			if (techResults.isError())
			{
				m_ui.notifyError(techResults.getErrorString());
				tech();
			}
			else
			{
				m_ui.notifyTechResults(techResults);
			}
		}
	}
	
	private void move(final boolean nonCombat, final String stepName)
	{
		if (getPlayerBridge().isGameOver())
		{
			return;
		}
		final IMoveDelegate moveDel;
		try
		{
			moveDel = (IMoveDelegate) getPlayerBridge().getRemoteDelegate();
		} catch (final ClassCastException e)
		{
			final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
						+ getPlayerBridge().getRemoteDelegate().getClass();
			System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
			e.printStackTrace();
			throw new IllegalStateException(errorContext, e);
		}
		/*
		if (!moveDel.stuffToDoInThisDelegate())
			return;
		*/
		final PlayerID id = getPlayerID();
		// play a sound for this phase
		if (nonCombat && !m_soundPlayedAlreadyNonCombatMove)
		{
			DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_PHASE_MOVE_NONCOMBAT, id.getName());
			m_soundPlayedAlreadyNonCombatMove = true;
		}
		else if (!nonCombat && !m_soundPlayedAlreadyCombatMove)
		{
			DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_PHASE_MOVE_COMBAT, id.getName());
			m_soundPlayedAlreadyCombatMove = true;
		}
		
		final MoveDescription moveDescription = m_ui.getMove(id, getPlayerBridge(), nonCombat, stepName);
		if (moveDescription == null)
		{
			if (GameStepPropertiesHelper.isRemoveAirThatCanNotLand(getGameData()))
			{
				if (!canAirLand(true, id))
				{
					move(nonCombat, stepName);
				}
			}
			if (!nonCombat)
			{
				if (canUnitsFight())
				{
					move(nonCombat, stepName);
				}
			}
			return;
		}
		final String error = moveDel.move(moveDescription.getUnits(), moveDescription.getRoute(),
					moveDescription.getTransportsThatCanBeLoaded(), moveDescription.getDependentUnits());
		if (error != null)
		{
			m_ui.notifyError(error);
		}
		move(nonCombat, stepName);
	}
	
	private boolean canAirLand(final boolean movePhase, final PlayerID player)
	{
		Collection<Territory> airCantLand;
		try
		{
			if (movePhase)
			{
				airCantLand = ((IMoveDelegate) getPlayerBridge().getRemoteDelegate()).getTerritoriesWhereAirCantLand(player);
			}
			else
			{
				airCantLand = ((IAbstractPlaceDelegate) getPlayerBridge().getRemoteDelegate()).getTerritoriesWhereAirCantLand();
			}
		} catch (final ClassCastException e)
		{
			final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
						+ getPlayerBridge().getRemoteDelegate().getClass();
			System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
			e.printStackTrace();
			throw new IllegalStateException(errorContext, e);
		}
		if (airCantLand.isEmpty())
		{
			return true;
		}
		else
		{
			if (!m_ui.getOKToLetAirDie(getPlayerID(), airCantLand, movePhase))
			{
				return false;
			}
			return true;
		}
	}
	
	private boolean canUnitsFight()
	{
		Collection<Territory> unitsCantFight;
		try
		{
			unitsCantFight = ((IMoveDelegate) getPlayerBridge().getRemoteDelegate()).getTerritoriesWhereUnitsCantFight();
		} catch (final ClassCastException e)
		{
			final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
						+ getPlayerBridge().getRemoteDelegate().getClass();
			System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
			e.printStackTrace();
			throw new IllegalStateException(errorContext, e);
		}
		if (unitsCantFight.isEmpty())
		{
			return false;
		}
		else
		{
			if (m_ui.getOKToLetUnitsDie(getPlayerID(), unitsCantFight, true))
			{
				return false;
			}
			return true;
		}
	}
	
	private void purchase(final boolean bid)
	{
		if (getPlayerBridge().isGameOver())
		{
			return;
		}
		/*
		if (!purchaseDel.stuffToDoInThisDelegate())
			return;
		*/
		final PlayerID id = getPlayerID();
		// play a sound for this phase
		if (!bid && !m_soundPlayedAlreadyPurchase)
		{
			DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_PHASE_PURCHASE, id.getName());
			m_soundPlayedAlreadyPurchase = true;
		}
		
		// Check if any factories need to be repaired
		String error = null;
		if (id.getRepairFrontier() != null && id.getRepairFrontier().getRules() != null && !id.getRepairFrontier().getRules().isEmpty())
		{
			final GameData data = getGameData();
			if (isDamageFromBombingDoneToUnitsInsteadOfTerritories(data))
			{
				final Match<Unit> myDamaged = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(id), Matches.UnitHasTakenSomeBombingUnitDamage);
				final Collection<Unit> damagedUnits = new ArrayList<Unit>();
				for (final Territory t : data.getMap().getTerritories())
				{
					damagedUnits.addAll(Match.getMatches(t.getUnits().getUnits(), myDamaged));
				}
				if (damagedUnits.size() > 0)
				{
					final HashMap<Unit, IntegerMap<RepairRule>> repair = m_ui.getRepair(id, bid, GameStepPropertiesHelper.getRepairPlayers(data, id));
					if (repair != null)
					{
						final IPurchaseDelegate purchaseDel;
						try
						{
							purchaseDel = (IPurchaseDelegate) getPlayerBridge().getRemoteDelegate();
						} catch (final ClassCastException e)
						{
							final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
										+ getPlayerBridge().getRemoteDelegate().getClass();
							System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
							e.printStackTrace();
							throw new IllegalStateException(errorContext, e);
						}
						error = purchaseDel.purchaseRepair(repair);
						if (error != null)
						{
							m_ui.notifyError(error);
							// dont give up, keep going
							purchase(bid);
						}
					}
				}
			}
		}
		final IntegerMap<ProductionRule> prod = m_ui.getProduction(id, bid);
		if (prod == null)
		{
			return;
		}
		final IPurchaseDelegate purchaseDel;
		try
		{
			purchaseDel = (IPurchaseDelegate) getPlayerBridge().getRemoteDelegate();
		} catch (final ClassCastException e)
		{
			final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
						+ getPlayerBridge().getRemoteDelegate().getClass();
			System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
			e.printStackTrace();
			throw new IllegalStateException(errorContext, e);
		}
		error = purchaseDel.purchase(prod);
		if (error != null)
		{
			m_ui.notifyError(error);
			// dont give up, keep going
			purchase(bid);
		}
	}
	
	private void battle()
	{
		if (getPlayerBridge().isGameOver())
		{
			return;
		}
		final IBattleDelegate battleDel;
		try
		{
			battleDel = (IBattleDelegate) getPlayerBridge().getRemoteDelegate();
		} catch (final ClassCastException e)
		{
			final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
						+ getPlayerBridge().getRemoteDelegate().getClass();
			System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
			e.printStackTrace();
			throw new IllegalStateException(errorContext, e);
		}
		/* 
		if (!battleDel.stuffToDoInThisDelegate())
			return;
		*/
		final PlayerID id = getPlayerID();
		while (true)
		{
			if (getPlayerBridge().isGameOver())
			{
				return;
			}
			final BattleListing battles = battleDel.getBattles();
			if (battles.isEmpty())
			{
				final IBattle battle = battleDel.getCurrentBattle();
				if (battle != null)
				{
					// this should never happen, but it happened once....
					System.err.println("Current battle exists but is not on pending list:  " + battle.toString());
					battleDel.fightCurrentBattle();
				}
				return;
			}
			// play a sound for this phase
			if (!m_soundPlayedAlreadyBattle)
			{
				DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_PHASE_BATTLE, id.getName());
				m_soundPlayedAlreadyBattle = true;
			}
			final FightBattleDetails details = m_ui.getBattle(id, battles.getBattles());
			if (getPlayerBridge().isGameOver())
			{
				return;
			}
			if (details != null)
			{
				final String error = battleDel.fightBattle(details.getWhere(), details.isBombingRaid(), details.getBattleType());
				if (error != null)
				{
					m_ui.notifyError(error);
				}
			}
		}
	}
	
	private void place(final boolean bid)
	{
		if (getPlayerBridge().isGameOver())
		{
			return;
		}
		final PlayerID id = getPlayerID();
		final IAbstractPlaceDelegate placeDel;
		try
		{
			placeDel = (IAbstractPlaceDelegate) getPlayerBridge().getRemoteDelegate();
		} catch (final ClassCastException e)
		{
			final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
						+ getPlayerBridge().getRemoteDelegate().getClass();
			System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
			e.printStackTrace();
			throw new IllegalStateException(errorContext, e);
		}
		/*
		if (!placeDel.stuffToDoInThisDelegate())
			return;
		*/
		while (true)
		{
			// play a sound for this phase
			if (!m_soundPlayedAlreadyPlacement)
			{
				DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_PHASE_PLACEMENT, id.getName());
				m_soundPlayedAlreadyPlacement = true;
			}
			final PlaceData placeData = m_ui.waitForPlace(id, bid, getPlayerBridge());
			if (placeData == null)
			{
				// this only happens in lhtr rules
				if (!GameStepPropertiesHelper.isRemoveAirThatCanNotLand(getGameData()) || canAirLand(false, id) || getPlayerBridge().isGameOver())
				{
					return;
				}
				else
				{
					continue;
				}
			}
			final String error = placeDel.placeUnits(placeData.getUnits(), placeData.getAt());
			if (error != null)
			{
				m_ui.notifyError(error);
			}
		}
	}
	
	private void endTurn()
	{
		if (getPlayerBridge().isGameOver())
		{
			return;
		}
		final GameData data = getGameData();
		// play a sound for this phase
		final IAbstractForumPosterDelegate endTurnDelegate;
		try
		{
			endTurnDelegate = (IAbstractForumPosterDelegate) getPlayerBridge().getRemoteDelegate();
		} catch (final ClassCastException e)
		{
			final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
						+ getPlayerBridge().getRemoteDelegate().getClass();
			System.err.println(errorContext); // for some reason the client is not seeing or getting these errors, so print to err too
			e.printStackTrace();
			throw new IllegalStateException(errorContext, e);
		}
		if (!m_soundPlayedAlreadyEndTurn && TerritoryAttachment.doWeHaveEnoughCapitalsToProduce(getPlayerID(), data))
		{
			// do not play if we are reloading a savegame from pbem (gets annoying)
			if (!endTurnDelegate.getHasPostedTurnSummary())
			{
				DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_PHASE_END_TURN, getPlayerID().getName());
			}
			m_soundPlayedAlreadyEndTurn = true;
		}
		m_ui.waitForEndTurn(getPlayerID(), getPlayerBridge());
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectCasualties
	 */
	public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
				final PlayerID hit, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer, final Collection<Unit> enemyUnits, final boolean amphibious,
				final Collection<Unit> amphibiousLandAttackers, final CasualtyList defaultCasualties, final GUID battleID, final Territory battlesite, final boolean allowMultipleHitsPerUnit)
	{
		return m_ui.getBattlePanel().getCasualties(selectFrom, dependents, count, message, dice, hit, defaultCasualties, battleID, allowMultipleHitsPerUnit);
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectFixedDice(int, int, boolean, java.lang.String)
	 */
	public int[] selectFixedDice(final int numDice, final int hitAt, final boolean hitOnlyIfEquals, final String title, final int diceSides)
	{
		return m_ui.selectFixedDice(numDice, hitAt, hitOnlyIfEquals, title, diceSides);
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectBombardingTerritory(games.strategy.engine.data.Unit, games.strategy.engine.data.Territory, java.util.Collection, boolean)
	 */
	public Territory selectBombardingTerritory(final Unit unit, final Territory unitTerritory, final Collection<Territory> territories,
				final boolean noneAvailable)
	{
		return m_ui.getBattlePanel().getBombardment(unit, unitTerritory, territories, noneAvailable);
	}
	
	/*
	 * Ask if the player wants to attack subs
	 */
	public boolean selectAttackSubs(final Territory unitTerritory)
	{
		return m_ui.getBattlePanel().getAttackSubs(unitTerritory);
	}
	
	/*
	 * Ask if the player wants to attack transports
	 */
	public boolean selectAttackTransports(final Territory unitTerritory)
	{
		return m_ui.getBattlePanel().getAttackTransports(unitTerritory);
	}
	
	/*
	 * Ask if the player wants to attack units
	 */
	public boolean selectAttackUnits(final Territory unitTerritory)
	{
		return m_ui.getBattlePanel().getAttackUnits(unitTerritory);
	}
	
	/*
	 * Ask if the player wants to shore bombard
	 */
	public boolean selectShoreBombard(final Territory unitTerritory)
	{
		return m_ui.getBattlePanel().getShoreBombard(unitTerritory);
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#shouldBomberBomb(games.strategy.engine.data.Territory)
	 */
	public boolean shouldBomberBomb(final Territory territory)
	{
		return m_ui.getStrategicBombingRaid(territory);
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#shouldBomberBomb(games.strategy.engine.data.Territory)
	 */
	public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> potentialTargets, final Collection<Unit> bombers)
	{
		return m_ui.getStrategicBombingRaidTarget(territory, potentialTargets, bombers);
	}
	
	public Territory whereShouldRocketsAttack(final Collection<Territory> candidates, final Territory from)
	{
		return m_ui.getRocketAttack(candidates, from);
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.player.ITripleaPlayer#getNumberOfFightersToMoveToNewCarrier(java.util.Collection, games.strategy.engine.data.Territory)
	 */
	public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved, final Territory from)
	{
		return m_ui.moveFightersToCarrier(fightersThatCanBeMoved, from);
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectTerritoryForAirToLand(java.util.Collection, java.lang.String)
	 */
	public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory,
				final String unitMessage)
	{
		return m_ui.selectTerritoryForAirToLand(candidates, currentTerritory, unitMessage);
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.player.ITripleaPlayer#confirmMoveInFaceOfAA(java.util.Collection)
	 */
	public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories)
	{
		final String question = "Your units will be fired on in: " + MyFormatter.defaultNamedToTextList(aaFiringTerritories, " and ", false)
					+ ".  Do you still want to move?";
		return m_ui.getOK(question);
	}
	
	public boolean confirmMoveKamikaze()
	{
		final String question = "Not all air units in destination territory can land, do you still want to move?";
		return m_ui.getOK(question);
	}
	
	public boolean confirmMoveHariKari()
	{
		final String question = "All units in destination territory will automatically die, do you still want to move?";
		return m_ui.getOK(question);
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.player.ITripleaPlayer#retreatQuery(games.strategy.net.GUID, boolean, java.util.Collection, java.lang.String, java.lang.String)
	 */
	public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleTerritory,
				final Collection<Territory> possibleTerritories, final String message)
	{
		return m_ui.getBattlePanel().getRetreat(battleID, message, possibleTerritories, submerge);
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.player.ITripleaPlayer#scrambleQuery(games.strategy.net.GUID, java.util.Collection, java.lang.String, java.lang.String)
	 
	public Collection<Unit> scrambleQuery(final GUID battleID, final Collection<Territory> possibleTerritories, final String message, final PlayerID player)
	{
		return m_ui.getBattlePanel().getScramble(getPlayerBridge(), battleID, message, possibleTerritories, player);
	}*/
	public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo,
				final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers)
	{
		return m_ui.scrambleUnitsQuery(scrambleTo, possibleScramblers);
	}
	
	public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible, final String message)
	{
		return m_ui.selectUnitsQuery(current, possible, message);
	}
	
	public void confirmEnemyCasualties(final GUID battleId, final String message, final PlayerID hitPlayer)
	{
		// no need, we have already confirmed since we are firing player
		if (m_ui.getLocalPlayers().playing(hitPlayer))
		{
			return;
		}
		// we dont want to confirm enemy casualties
		if (!BattleDisplay.getShowEnemyCasualtyNotification())
		{
			return;
		}
		m_ui.getBattlePanel().confirmCasualties(battleId, message);
	}
	
	public void confirmOwnCasualties(final GUID battleId, final String message)
	{
		m_ui.getBattlePanel().confirmCasualties(battleId, message);
	}
	
	public final boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories(final GameData data)
	{
		return games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data);
	}
	
	public HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> selectKamikazeSuicideAttacks(
				final HashMap<Territory, Collection<Unit>> possibleUnitsToAttack)
	{
		final PlayerID id = getPlayerID();
		final PlayerAttachment pa = PlayerAttachment.get(id);
		if (pa == null)
		{
			return null;
		}
		final IntegerMap<Resource> resourcesAndAttackValues = pa.getSuicideAttackResources();
		if (resourcesAndAttackValues.size() <= 0)
		{
			return null;
		}
		final IntegerMap<Resource> playerResourceCollection = id.getResources().getResourcesCopy();
		final IntegerMap<Resource> attackTokens = new IntegerMap<Resource>();
		for (final Resource possible : resourcesAndAttackValues.keySet())
		{
			final int amount = playerResourceCollection.getInt(possible);
			if (amount > 0)
			{
				attackTokens.put(possible, amount);
			}
		}
		if (attackTokens.size() <= 0)
		{
			return null;
		}
		final HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> rVal = new HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>>();
		for (final Entry<Resource, Integer> entry : attackTokens.entrySet())
		{
			final Resource r = entry.getKey();
			final int max = entry.getValue();
			final HashMap<Territory, IntegerMap<Unit>> selection = m_ui.selectKamikazeSuicideAttacks(possibleUnitsToAttack, r, max);
			for (final Entry<Territory, IntegerMap<Unit>> selectionEntry : selection.entrySet())
			{
				final Territory t = selectionEntry.getKey();
				HashMap<Unit, IntegerMap<Resource>> currentTerr = rVal.get(t);
				if (currentTerr == null)
				{
					currentTerr = new HashMap<Unit, IntegerMap<Resource>>();
				}
				for (final Entry<Unit, Integer> unitEntry : selectionEntry.getValue().entrySet())
				{
					final Unit u = unitEntry.getKey();
					IntegerMap<Resource> currentUnit = currentTerr.get(u);
					if (currentUnit == null)
					{
						currentUnit = new IntegerMap<Resource>();
					}
					currentUnit.add(r, unitEntry.getValue());
					currentTerr.put(u, currentUnit);
				}
				rVal.put(t, currentTerr);
			}
		}
		
		return rVal;
	}
	
	public Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(final List<Territory> territoryChoices, final List<Unit> unitChoices,
				final int unitsPerPick)
	{
		if (territoryChoices == null || territoryChoices.isEmpty() || unitsPerPick < 1)
		{
			return new Tuple<Territory, Set<Unit>>(null, new HashSet<Unit>());
		}
		return m_ui.pickTerritoryAndUnits(this.getPlayerID(), territoryChoices, unitChoices, unitsPerPick);
	}
}
