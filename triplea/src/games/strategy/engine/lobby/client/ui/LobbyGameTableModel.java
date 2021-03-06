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
package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.ILobbyGameBroadcaster;
import games.strategy.engine.lobby.server.ILobbyGameController;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.GUID;
import games.strategy.net.IMessenger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class LobbyGameTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 6399458368730633993L;
	
	
	enum Column
	{
		Host, Name, GV, Round, Players, P, B, EV, Started, Status, Comments, GUID
	}
	
	private final IMessenger m_messenger;
	private final IChannelMessenger m_channelMessenger;
	private final IRemoteMessenger m_remoteMessenger;
	// these must only be accessed in the swing event thread
	private final List<GUID> m_gameIDs = new ArrayList<GUID>();
	private final List<GameDescription> m_games = new ArrayList<GameDescription>();
	
	public LobbyGameTableModel(final IMessenger messenger, final IChannelMessenger channelMessenger, final IRemoteMessenger remoteMessenger)
	{
		m_messenger = messenger;
		m_channelMessenger = channelMessenger;
		m_remoteMessenger = remoteMessenger;
		m_channelMessenger.registerChannelSubscriber(new ILobbyGameBroadcaster()
		{
			public void gameUpdated(final GUID gameId, final GameDescription description)
			{
				assertSentFromServer();
				updateGame(gameId, description);
			}
			
			public void gameAdded(final GUID gameId, final GameDescription description)
			{
				assertSentFromServer();
				addGame(gameId, description);
			}
			
			public void gameRemoved(final GUID gameId)
			{
				assertSentFromServer();
				removeGame(gameId);
			}
		}, ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL);
		final Map<GUID, GameDescription> games = ((ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE)).listGames();
		for (final GUID id : games.keySet())
		{
			addGame(id, games.get(id));
		}
	}
	
	public GameDescription get(final int i)
	{
		return m_games.get(i);
	}
	
	@Override
	public Class<?> getColumnClass(final int columnIndex)
	{
		if (columnIndex == getColumnIndex(Column.Started))
			return Date.class;
		return Object.class;
	}
	
	private void removeGame(final GUID gameId)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final int index = m_gameIDs.indexOf(gameId);
				m_gameIDs.remove(index);
				m_games.remove(index);
				fireTableRowsDeleted(index, index);
			}
		});
	}
	
	private void addGame(final GUID gameId, final GameDescription description)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_gameIDs.add(gameId);
				m_games.add(description);
				fireTableRowsInserted(m_gameIDs.size() - 1, m_gameIDs.size() - 1);
			}
		});
	}
	
	private void assertSentFromServer()
	{
		if (!MessageContext.getSender().equals(m_messenger.getServerNode()))
			throw new IllegalStateException("Invalid sender");
	}
	
	private void updateGame(final GUID gameId, final GameDescription description)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final int index = m_gameIDs.indexOf(gameId);
				m_games.set(index, description);
				fireTableRowsUpdated(index, index);
			}
		});
	}
	
	@Override
	public String getColumnName(final int column)
	{
		return Column.values()[column].toString();
	}
	
	public int getColumnIndex(final Column column)
	{
		return column.ordinal();
	}
	
	public int getColumnCount()
	{
		// -1 so we don't display the guid
		return Column.values().length - 1;
	}
	
	public int getRowCount()
	{
		return m_gameIDs.size();
	}
	
	public Object getValueAt(final int rowIndex, final int columnIndex)
	{
		final Column column = Column.values()[columnIndex];
		final GameDescription description = m_games.get(rowIndex);
		switch (column)
		{
			case Host:
				return description.getHostName();
			case Round:
				return description.getRound();
			case Name:
				return description.getGameName();
			case Players:
				return description.getPlayerCount();
			case P:
				return (description.getPassworded() ? "*" : "");
			case B:
				return (description.getBotSupportEmail() != null && description.getBotSupportEmail().length() > 0 ? "-" : "");
			case GV:
				return description.getGameVersion();
			case EV:
				return description.getEngineVersion();
			case Status:
				return description.getStatus();
			case Comments:
				return description.getComment();
			case Started:
				return description.getStartDateTime();
			case GUID:
				return m_gameIDs.get(rowIndex);
			default:
				throw new IllegalStateException("Unknown column:" + column);
		}
	}
}
