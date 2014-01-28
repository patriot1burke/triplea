package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.MacFinder;
import games.strategy.util.MD5Crypt;

import java.awt.Window;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;

public class LobbyLogin
{
	private final Window m_parent;
	private final LobbyServerProperties m_serverProperties;
	
	public LobbyLogin(final Window parent, final LobbyServerProperties properties)
	{
		m_parent = parent;
		m_serverProperties = properties;
	}
	
	/**
	 * Attempt to login to the LobbyServer
	 * <p>
	 * 
	 * If we could not login, return null.
	 */
	public LobbyClient login()
	{
		if (!m_serverProperties.isServerAvailable())
		{
			JOptionPane.showMessageDialog(m_parent, m_serverProperties.getServerErrorMessage(), "Could not connect to server", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		if (m_serverProperties.getPort() == -1)
		{
			if (GameRunner2.areWeOldExtraJar())
			{
				JOptionPane.showMessageDialog(
							m_parent,
							"<html>Could not find lobby server for this version of TripleA, <br>Please make sure you are using the latest version: http://triplea.sourceforge.net/ "
										+ "<br /><br />This is because you are using an old engine that is kept for backwards compatibility. "
										+ "<br /><b>In order to load your Old savegames in the New lobby, you must First join the lobby with the latest engine, Then host a game, Then load the old savegame!</b></html>",
							"Could not connect to server", JOptionPane.ERROR_MESSAGE);
			}
			else
			{
				JOptionPane.showMessageDialog(m_parent,
							"<html>Could not find lobby server for this version of TripleA, <br>Please make sure you are using the latest version: http://triplea.sourceforge.net/</html>",
							"Could not connect to server", JOptionPane.ERROR_MESSAGE);
			}
			return null;
		}
		return loginToServer();
	}
	
	private LobbyClient loginToServer()
	{
		final LoginPanel panel = new LoginPanel();
		final LoginPanel.ReturnValue value = panel.show(m_parent);
		if (value == LoginPanel.ReturnValue.LOGON)
		{
			return login(panel);
		}
		else if (value == LoginPanel.ReturnValue.CANCEL)
		{
			return null;
		}
		else if (value == LoginPanel.ReturnValue.CREATE_ACCOUNT)
		{
			return createAccount();
		}
		else
			throw new IllegalStateException("??");
	}
	
	private LobbyClient login(final LoginPanel panel)
	{
		try
		{
			final String mac = MacFinder.GetHashedMacAddress();
			final ClientMessenger messenger = new ClientMessenger(m_serverProperties.getHost(), m_serverProperties.getPort(), panel.getUserName(), mac, new IConnectionLogin()
			{
				private final AtomicReference<String> m_internalError = new AtomicReference<String>();
				
				public void notifyFailedLogin(String message)
				{
					if (m_internalError.get() != null)
						message = m_internalError.get();
					JOptionPane.showMessageDialog(m_parent, message, "Login Failed", JOptionPane.ERROR_MESSAGE);
				}
				
				public Map<String, String> getProperties(final Map<String, String> challengProperties)
				{
					final Map<String, String> props = new HashMap<String, String>();
					if (panel.isAnonymous())
					{
						props.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
					}
					else
					{
						String salt = challengProperties.get(LobbyLoginValidator.SALT_KEY);
						if (salt == null)
						{
							// the server does not have a salt value
							// so there is no user with our name,
							// continue as before
							m_internalError.set("No account with that name exists");
							salt = "none";
						}
						final String hashedPassword = MD5Crypt.crypt(panel.getPassword(), salt);
						props.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, hashedPassword);
					}
					props.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
					return props;
				}
			});
			// sucess, store prefs
			LoginPanel.storePrefs(panel.getUserName(), panel.isAnonymous());
			return new LobbyClient(messenger, panel.isAnonymous());
		} catch (final CouldNotLogInException clne)
		{
			// this has already been dealt with
			return loginToServer();
		} catch (final IOException ioe)
		{
			JOptionPane.showMessageDialog(m_parent, "Could Not Connect to Lobby : " + ioe.getMessage(), "Could not connect", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
	
	private LobbyClient createAccount()
	{
		final CreateUpdateAccountPanel createAccount = CreateUpdateAccountPanel.newCreatePanel();
		final CreateUpdateAccountPanel.ReturnValue value = createAccount.show(m_parent);
		if (value == CreateUpdateAccountPanel.ReturnValue.OK)
		{
			return createAccount(createAccount);
		}
		else
		{
			return null;
		}
	}
	
	private LobbyClient createAccount(final CreateUpdateAccountPanel createAccount)
	{
		try
		{
			final String mac = MacFinder.GetHashedMacAddress();
			final ClientMessenger messenger = new ClientMessenger(m_serverProperties.getHost(), m_serverProperties.getPort(), createAccount.getUserName(), mac, new IConnectionLogin()
			{
				public void notifyFailedLogin(final String message)
				{
					JOptionPane.showMessageDialog(m_parent, message, "Login Failed", JOptionPane.ERROR_MESSAGE);
				}
				
				public Map<String, String> getProperties(final Map<String, String> challengProperties)
				{
					final Map<String, String> props = new HashMap<String, String>();
					props.put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
					props.put(LobbyLoginValidator.EMAIL_KEY, createAccount.getEmail());
					props.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt(createAccount.getPassword()));
					props.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
					return props;
				}
			});
			// default
			LoginPanel.storePrefs(createAccount.getUserName(), false);
			return new LobbyClient(messenger, false);
		} catch (final CouldNotLogInException clne)
		{
			// this has already been dealt with
			return createAccount();
		} catch (final IOException ioe)
		{
			JOptionPane.showMessageDialog(m_parent, ioe.getMessage(), "Account creation failed", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
}
