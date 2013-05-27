package games.strategy.engine.lobby.client.ui.action;

import games.strategy.common.ui.InGameLobbyWatcherWrapper;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class RemoveGameFromLobbyAction extends AbstractAction
{
	private static final long serialVersionUID = 8802420945692279375L;
	private final InGameLobbyWatcherWrapper m_lobbyWatcher;
	
	public RemoveGameFromLobbyAction(final InGameLobbyWatcherWrapper watcher)
	{
		super("Remove Game From Lobby");
		m_lobbyWatcher = watcher;
	}
	
	public void actionPerformed(final ActionEvent e)
	{
		m_lobbyWatcher.shutDown();
		setEnabled(false);
	}
}
