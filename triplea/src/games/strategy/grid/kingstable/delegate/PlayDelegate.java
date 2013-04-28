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
package games.strategy.grid.kingstable.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.kingstable.attachments.TerritoryAttachment;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.grid.ui.display.IGridGameDisplay;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Responsible for performing a move in a game of King's Table.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2012-12-25 06:54:45 +0800 (Tue, 25 Dec 2012) $
 */
@AutoSave(beforeStepStart = false, afterStepEnd = true)
public class PlayDelegate extends AbstractDelegate implements IGridPlayDelegate
{
	private Matches matches = null;
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		if (matches == null)
			matches = new Matches(getData());
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.setStatus(m_player.getName() + "'s turn");
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		final KingsTablePlayExtendedDelegateState state = new KingsTablePlayExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final KingsTablePlayExtendedDelegateState s = (KingsTablePlayExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
	}
	
	public boolean delegateCurrentlyRequiresUserInput()
	{
		return true;
	}
	
	public void signalStatus(final String status)
	{
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.setStatus(status);
	}
	
	/**
	 * Attempt to move a piece from the start <code>Territory</code> to the end <code>Territory</code>.
	 * 
	 * @param start
	 *            <code>Territory</code> where the move should start
	 * @param end
	 *            <code>Territory</code> where the move should end
	 */
	public String play(final IGridPlayData play)
	{
		final Territory start = play.getStart();
		final Territory end = play.getEnd();
		final String error = isValidPlay(start, end, m_player, getData());
		if (error != null)
			return error;
		final Collection<Territory> captured = checkForCaptures(end, m_player, getData());
		performPlay(start, end, captured, m_player);
		return null;
	}
	
	/**
	 * After a move completes, look to see if any captures occur.
	 * 
	 * @param end
	 *            <code>Territory</code> where the move ended. All potential captures must involve this <code>Territory</code>.
	 * @return
	 */
	public static Collection<Territory> checkForCaptures(final Territory end, final PlayerID player, final GameData data)
	{
		final Matches matches = new Matches(data);
		// At most, four pieces will be captured
		final Collection<Territory> captured = new HashSet<Territory>(4);
		// Failsafe - end should never be null, so only check for captures if it isn't null
		if (end != null)
		{
			// Get the coordinates where the move ended
			final int endX = end.getX();
			final int endY = end.getY();
			final GameMap map = data.getMap();
			// Look above end for a potential capture
			// This extra set of braces is for bug prevention - it makes sure that the scope of possibleCapture stays within the braces
			{
				final Territory possibleCapture = map.getTerritoryFromCoordinates(endX, endY - 1);
				if (matches.eligibleForCapture(possibleCapture, player))
				{
					// Get the territory to the left of the possible capture
					final Territory above = map.getTerritoryFromCoordinates(endX, endY - 2);
					// Can the king be captured?
					if (matches.kingInSquare(possibleCapture))
					{
						final Territory left = map.getTerritoryFromCoordinates(endX - 1, endY - 1);
						final Territory right = map.getTerritoryFromCoordinates(endX + 1, endY - 1);
						if (matches.eligibleParticipantsInKingCapture(player, above, left, right))
							captured.add(possibleCapture);
						else if (matches.kingCanBeCapturedLikeAPawn() && matches.eligibleParticipantInPawnCapture(player, above))
							captured.add(possibleCapture);
					}
					// Can a pawn be captured?
					else if (matches.eligibleParticipantInPawnCapture(player, above))
					{
						captured.add(possibleCapture);
					}
				}
			}
			// Look below end for a potential capture
			// This extra set of braces is for bug prevention - it makes sure that the scope of possibleCapture stays within the braces
			{
				final Territory possibleCapture = map.getTerritoryFromCoordinates(endX, endY + 1);
				if (matches.eligibleForCapture(possibleCapture, player))
				{
					// Get the territory to the left of the possible capture
					final Territory below = map.getTerritoryFromCoordinates(endX, endY + 2);
					// Can the king be captured?
					if (matches.kingInSquare(possibleCapture))
					{
						final Territory left = map.getTerritoryFromCoordinates(endX - 1, endY + 1);
						final Territory right = map.getTerritoryFromCoordinates(endX + 1, endY + 1);
						if (matches.eligibleParticipantsInKingCapture(player, below, left, right))
							captured.add(possibleCapture);
						else if (matches.kingCanBeCapturedLikeAPawn() && matches.eligibleParticipantInPawnCapture(player, below))
							captured.add(possibleCapture);
					}
					// Can a pawn be captured?
					else if (matches.eligibleParticipantInPawnCapture(player, below))
					{
						captured.add(possibleCapture);
					}
				}
			}
			// Look left end for a potential capture
			// This extra set of braces is for bug prevention - it makes sure that the scope of possibleCapture stays within the braces
			{
				final Territory possibleCapture = map.getTerritoryFromCoordinates(endX - 1, endY);
				if (matches.eligibleForCapture(possibleCapture, player))
				{
					// Get the territory to the left of the possible capture
					final Territory left = map.getTerritoryFromCoordinates(endX - 2, endY);
					// Can the king be captured?
					if (matches.kingInSquare(possibleCapture))
					{
						final Territory above = map.getTerritoryFromCoordinates(endX - 1, endY - 1);
						final Territory below = map.getTerritoryFromCoordinates(endX - 1, endY + 1);
						if (matches.eligibleParticipantsInKingCapture(player, left, above, below))
							captured.add(possibleCapture);
						else if (matches.kingCanBeCapturedLikeAPawn() && matches.eligibleParticipantInPawnCapture(player, left))
							captured.add(possibleCapture);
					}
					// Can a pawn be captured?
					else if (matches.eligibleParticipantInPawnCapture(player, left))
					{
						captured.add(possibleCapture);
					}
				}
			}
			// Look right end for a potential capture
			// This extra set of braces is for bug prevention - it makes sure that the scope of possibleCapture stays within the braces
			{
				final Territory possibleCapture = map.getTerritoryFromCoordinates(endX + 1, endY);
				if (matches.eligibleForCapture(possibleCapture, player))
				{
					// Get the territory to the left of the possible capture
					final Territory right = map.getTerritoryFromCoordinates(endX + 2, endY);
					// Can the king be captured?
					if (matches.kingInSquare(possibleCapture))
					{
						final Territory above = map.getTerritoryFromCoordinates(endX + 1, endY - 1);
						final Territory below = map.getTerritoryFromCoordinates(endX + 1, endY + 1);
						if (matches.eligibleParticipantsInKingCapture(player, right, above, below))
							captured.add(possibleCapture);
						else if (matches.kingCanBeCapturedLikeAPawn() && matches.eligibleParticipantInPawnCapture(player, right))
							captured.add(possibleCapture);
					}
					// Can a pawn be captured?
					else if (matches.eligibleParticipantInPawnCapture(player, right))
					{
						captured.add(possibleCapture);
					}
				}
			}
		}
		return captured;
	}
	
	/**
	 * Check to see if moving a piece from the start <code>Territory</code> to the end <code>Territory</code> is a valid play.
	 * 
	 * @param start
	 *            <code>Territory</code> where the move should start
	 * @param end
	 *            <code>Territory</code> where the move should end
	 */
	public static String isValidPlay(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		final int unitCount = start.getUnits().getUnitCount(player);
		// The current player must have exactly one unit in the starting territory
		if (unitCount < 1)
			return player.getName() + " doesn't have a piece in the selected starting square.";
		else if (unitCount > 1)
			return "The selected starting square contains more than one piece - that shouldn't be possible.";
		// The destination territory must be empty
		if (!end.getUnits().isEmpty())
			return "The selected destination square is not empty";
		final int startX = start.getX();
		final int endX = end.getX();
		final int startY = start.getY();
		final int endY = end.getY();
		final GameMap map = data.getMap();
		// Pieces can only move in a straight line
		// and the intervening spaces must be empty
		if (startX == endX)
		{
			int y1, y2;
			if (startY < endY)
			{
				y1 = startY + 1;
				y2 = endY - 1;
			}
			else
			{
				y1 = endY + 1;
				y2 = startY - 1;
			}
			for (int y = y1; y <= y2; y++)
			{
				final Territory at = map.getTerritoryFromCoordinates(startX, y);
				if (at.getUnits().size() > 0)
					return "Pieces can only move through empty spaces.";
			}
		}
		else if (startY == endY)
		{
			int x1, x2;
			if (startX < endX)
			{
				x1 = startX + 1;
				x2 = endX - 1;
			}
			else
			{
				x1 = endX + 1;
				x2 = startX - 1;
			}
			for (int x = x1; x <= x2; x++)
			{
				final Territory at = map.getTerritoryFromCoordinates(x, startY);
				if (at.getUnits().size() > 0)
					return "Intervening square (" + x + "," + startY + ") is not empty.";
			}
		}
		else
			return "Pieces can only move in a straight line.";
		// Only the king can move to king's squares
		final Matches matches = new Matches(data);
		if (!matches.kingInSquare(start) && matches.isKingsSquare(end))
		{
			return "Only the king can go there";
		}
		return null;
	}
	
	/**
	 * Move a piece from the start <code>Territory</code> to the end <code>Territory</code>.
	 * 
	 * @param start
	 *            <code>Territory</code> where the move should start
	 * @param end
	 *            <code>Territory</code> where the move should end
	 */
	private void performPlay(final Territory start, final Territory end, final Collection<Territory> captured, final PlayerID player)
	{
		final Collection<Unit> units = start.getUnits().getUnits();
		final String transcriptText = player.getName() + " moved from " + start.getName() + " to " + end.getName();
		m_bridge.getHistoryWriter().startEvent(transcriptText, units);
		final Change removeUnit = ChangeFactory.removeUnits(start, units);
		final Change removeStartOwner = ChangeFactory.changeOwner(start, PlayerID.NULL_PLAYERID);
		final Change addUnit = ChangeFactory.addUnits(end, units);
		final Change addEndOwner = ChangeFactory.changeOwner(end, player);
		final CompositeChange change = new CompositeChange();
		change.add(removeUnit);
		change.add(removeStartOwner);
		change.add(addUnit);
		change.add(addEndOwner);
		for (final Territory at : captured)
		{
			if (at != null)
			{
				final Collection<Unit> capturedUnits = at.getUnits().getUnits();
				final Change capture = ChangeFactory.removeUnits(at, capturedUnits);
				change.add(capture);
				final Change removeOwner = ChangeFactory.changeOwner(at, PlayerID.NULL_PLAYERID);
				change.add(removeOwner);
			}
		}
		m_bridge.addChange(change);
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		final Collection<Territory> refresh = new HashSet<Territory>();
		refresh.add(start);
		refresh.add(end);
		refresh.addAll(captured);
		display.refreshTerritories(refresh);
		display.showGridPlayDataMove(new GridPlayData(start, end, player));
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class implements IPlayDelegate, which inherits from IRemote.
		return IGridPlayDelegate.class;
	}
}


class KingsTablePlayExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 8227905090355825327L;
	Serializable superState;
	// add other variables here:
}


/**
 * Utility class providing matching methods for use in King's Table.
 * 
 * @author Lane Schwartz
 */
class Matches
{
	private final boolean kingCanParticipateInCaptures;
	private final boolean cornerSquaresCanBeUsedToCapturePawns;
	private final boolean centerSquareCanBeUsedToCapturePawns;
	private final boolean cornerSquaresCanBeUsedToCaptureTheKing;
	private final boolean centerSquareCanBeUsedToCaptureTheKing;
	private final boolean edgeOfBoardCanBeUsedToCaptureTheKing;
	private final boolean kingCanBeCapturedLikeAPawn;
	
	Matches(final GameData gameData)
	{
		final GameProperties properties = gameData.getProperties();
		kingCanParticipateInCaptures = properties.get("King can participate in captures", true);
		cornerSquaresCanBeUsedToCapturePawns = properties.get("Corner squares can be used to capture pawns", true);
		centerSquareCanBeUsedToCapturePawns = properties.get("Center square can be used to capture pawns", false);
		cornerSquaresCanBeUsedToCaptureTheKing = properties.get("Corner squares can be used to capture the king", false);
		centerSquareCanBeUsedToCaptureTheKing = properties.get("Center square can be used to capture the king", true);
		edgeOfBoardCanBeUsedToCaptureTheKing = properties.get("Edge of board can be used to capture the king", false);
		kingCanBeCapturedLikeAPawn = properties.get("King can be captured like a pawn", false);
	}
	
	public boolean kingCanBeCapturedLikeAPawn()
	{
		return kingCanBeCapturedLikeAPawn;
	}
	
	public boolean kingInSquare(final Territory t)
	{
		if (t == null)
		{
			return false;
		}
		else
		{
			final Collection<Unit> units = t.getUnits().getUnits();
			if (units.isEmpty())
				return false;
			else
			{
				final Unit unit = (Unit) units.toArray()[0];
				if (unit.getType().getName().equals("king"))
					return true;
				else
					return false;
			}
		}
	}
	
	public boolean isKingsExit(final Territory t)
	{
		final TerritoryAttachment ta = ((TerritoryAttachment) t.getAttachment("territoryAttachment"));
		if (ta == null)
			return false;
		else if (ta.getKingsExit())
			return true;
		else
			return false;
	}
	
	public boolean isKingsSquare(final Territory t)
	{
		final TerritoryAttachment ta = ((TerritoryAttachment) t.getAttachment("territoryAttachment"));
		if (ta == null)
			return false;
		else if (ta.getKingsSquare())
			return true;
		else
			return false;
	}
	
	public boolean eligibleParticipantInPawnCapture(final PlayerID currentPlayer, final Territory territory)
	{
		// System.out.println("eligibleParticipantInPawnCapture" + currentPlayer.getName() + " " + territory.getName());
		if (territory == null)
		{
			return false;
		}
		else
		{
			if (territory.getOwner().equals(currentPlayer))
			{
				if (territory.getUnits().isEmpty())
					return false;
				else if (!kingCanParticipateInCaptures && kingInSquare(territory))
					return false;
				else
					return true;
			}
			else
			{
				if (cornerSquaresCanBeUsedToCapturePawns && isKingsExit(territory))
					return true;
				else if (centerSquareCanBeUsedToCapturePawns && isKingsSquare(territory))
					return true;
				else
					return false;
			}
		}
	}
	
	public boolean eligibleParticipantInKingCapture(final PlayerID currentPlayer, final Territory territory)
	{
		if (territory == null)
		{
			if (edgeOfBoardCanBeUsedToCaptureTheKing)
				return true;
			else
				return false;
		}
		else
		{
			if (territory.getOwner().equals(currentPlayer))
			{
				if (territory.getUnits().size() > 0)
					return true;
				else
					return false;
			}
			else
			{
				if (cornerSquaresCanBeUsedToCaptureTheKing && isKingsExit(territory))
					return true;
				else if (centerSquareCanBeUsedToCaptureTheKing && isKingsSquare(territory))
					return true;
				else
					return false;
			}
		}
	}
	
	public boolean eligibleParticipantsInKingCapture(final PlayerID currentPlayer, final Territory... territories)
	{
		if (territories == null || territories.length == 0)
			return false;
		for (final Territory territory : territories)
		{
			if (!eligibleParticipantInKingCapture(currentPlayer, territory))
				return false;
		}
		return true;
	}
	
	public boolean eligibleForCapture(final Territory territory, final PlayerID currentPlayer)
	{
		if (territory == null || territory.getUnits().isEmpty() || territory.getOwner().equals(currentPlayer))
			return false;
		else
			return true;
	}
}
