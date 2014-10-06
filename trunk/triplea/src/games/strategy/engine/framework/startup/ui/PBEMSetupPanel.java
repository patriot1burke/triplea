package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.LocalBeanCache;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import games.strategy.engine.framework.startup.ui.editors.SelectAndViewEditor;
import games.strategy.engine.pbem.GenericEmailSender;
import games.strategy.engine.pbem.GmailEmailSender;
import games.strategy.engine.pbem.HotmailEmailSender;
import games.strategy.engine.pbem.IEmailSender;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.engine.pbem.IWebPoster;
import games.strategy.engine.pbem.NullEmailSender;
import games.strategy.engine.pbem.NullForumPoster;
import games.strategy.engine.pbem.NullWebPoster;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.engine.pbem.TripleAWarClubForumPoster;
import games.strategy.engine.pbem.TripleAWebPoster;
import games.strategy.engine.random.IRemoteDiceServer;
import games.strategy.engine.random.InternalDiceServer;
import games.strategy.engine.random.PBEMDiceRoller;
import games.strategy.engine.random.PropertiesDiceRoller;
import games.strategy.triplea.pbem.AxisAndAlliesForumPoster;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 * A panel for setting up Play by Email/Forum.
 * This panel listens to the GameSelectionModel so it can refresh when a new game is selected or save game loaded
 * The MainPanel also listens to this panel, and we notify it through the notifyObservers()
 */
public class PBEMSetupPanel extends SetupPanel implements Observer
{
	
	private static final long serialVersionUID = 9006941131918034674L;
	
	// -----------------------------------------------------------------------
	// class fields
	// -----------------------------------------------------------------------
	private static final String DICE_ROLLER = "games.strategy.engine.random.IRemoteDiceServer";
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	private final GameSelectorModel m_gameSelectorModel;
	private final SelectAndViewEditor m_diceServerEditor;
	private final SelectAndViewEditor m_forumPosterEditor;
	private final SelectAndViewEditor m_emailSenderEditor;
	private final SelectAndViewEditor m_webPosterEditor;
	private final List<PBEMLocalPlayerComboBoxSelector> m_playerTypes = new ArrayList<PBEMLocalPlayerComboBoxSelector>();
	private final JPanel m_localPlayerPanel = new JPanel();
	private final JButton m_localPlayerSelection = new JButton("Select Local Players and AI's");
	
	// -----------------------------------------------------------------------
	// constructors
	// -----------------------------------------------------------------------
	
	/**
	 * Creates a new instance
	 * 
	 * @param model
	 *            the GameSelectionModel, though which changes are obtained when new games are chosen, or save games loaded
	 */
	public PBEMSetupPanel(final GameSelectorModel model)
	{
		m_gameSelectorModel = model;
		m_diceServerEditor = new SelectAndViewEditor("Dice Server", "");
		m_forumPosterEditor = new SelectAndViewEditor("Post to Forum", "forumPosters.html");
		m_emailSenderEditor = new SelectAndViewEditor("Provider", "emailSenders.html");
		m_webPosterEditor = new SelectAndViewEditor("Send to Website", "websiteSenders.html");
		createComponents();
		layoutComponents();
		setupListeners();
		if (m_gameSelectorModel.getGameData() != null)
		{
			loadAll();
		}
		setWidgetActivation();
	}
	
	private void createComponents()
	{
		m_localPlayerSelection.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				JOptionPane.showMessageDialog(PBEMSetupPanel.this, m_localPlayerPanel, "Select Local Players and AI's", JOptionPane.PLAIN_MESSAGE);
			}
		});
	}
	
	private void layoutComponents()
	{
		removeAll();
		setLayout(new GridBagLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10)); // Empty border works as margin
		
		int row = 0;
		add(m_diceServerEditor, new GridBagConstraints(0, row++, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 20, 0), 0, 0));
		
		// the play by Forum settings
		m_forumPosterEditor.setBorder(new TitledBorder("Play By Forum"));
		add(m_forumPosterEditor, new GridBagConstraints(0, row++, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0), 0, 0));
		
		final JPanel emailPanel = new JPanel(new GridBagLayout());
		emailPanel.setBorder(new TitledBorder("Play By Email"));
		add(emailPanel, new GridBagConstraints(0, row++, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0), 0, 0));
		int panelRow = 0;
		emailPanel.add(m_emailSenderEditor, new GridBagConstraints(0, panelRow++, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0));
		
		// the play by micro web site settings
		// m_webPosterEditor.setBorder(new TitledBorder("Play By Web Site"));
		// add(m_webPosterEditor, new GridBagConstraints(0, row++, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0), 0, 0));
		
		// add selection of local players
		add(m_localPlayerSelection, new GridBagConstraints(0, row++, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(10, 0, 10, 0), 0, 0));
		
		layoutPlayerPanel(this);
		setWidgetActivation();
	}
	
	@Override
	public void setWidgetActivation()
	{
	}
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	private void setupListeners()
	{
		// register, so we get notified when the game model (GameData) changes (e.g if the user load a save game or selects another game)
		m_gameSelectorModel.addObserver(this);
		// subscribe to editor changes, so we can notify the MainPanel
		m_diceServerEditor.addPropertyChangeListener(new NotifyingPropertyChangeListener());
		m_forumPosterEditor.addPropertyChangeListener(new NotifyingPropertyChangeListener());
		m_emailSenderEditor.addPropertyChangeListener(new NotifyingPropertyChangeListener());
		m_webPosterEditor.addPropertyChangeListener(new NotifyingPropertyChangeListener());
	}
	
	private void loadAll()
	{
		loadDiceServer(m_gameSelectorModel.getGameData());
		loadForumPosters(m_gameSelectorModel.getGameData());
		loadEmailSender(m_gameSelectorModel.getGameData());
		loadWebPosters(m_gameSelectorModel.getGameData());
	}
	
	/**
	 * Load the dice rollers from cache, if the game was a save game, the dice roller store is selected
	 * 
	 * @param data
	 *            the game data
	 */
	private void loadDiceServer(final GameData data)
	{
		
		final List<IRemoteDiceServer> diceRollers = new ArrayList<IRemoteDiceServer>(PropertiesDiceRoller.loadFromFile());
		diceRollers.add(new InternalDiceServer());
		
		for (final IRemoteDiceServer diceRoller : diceRollers)
		{
			final IRemoteDiceServer cached = (IRemoteDiceServer) LocalBeanCache.getInstance().getSerializable(diceRoller.getDisplayName());
			if (cached != null)
			{
				diceRoller.setCcAddress(cached.getCcAddress());
				diceRoller.setToAddress(cached.getToAddress());
				diceRoller.setGameId(cached.getGameId());
			}
		}
		
		m_diceServerEditor.setBeans(diceRollers);
		
		if (m_gameSelectorModel.isSavedGame())
		{
			// get the dice roller from the save game, if any
			final IRemoteDiceServer roller = (IRemoteDiceServer) data.getProperties().get(DICE_ROLLER);
			if (roller != null)
			{
				m_diceServerEditor.setSelectedBean(roller);
			}
		}
	}
	
	/**
	 * Load the Forum poster that are stored in the GameData, and select it in the list.
	 * Sensitive information such as passwords are not stored in save games, so the are loaded from the LocalBeanCache
	 * 
	 * @param data
	 *            the game data
	 */
	private void loadForumPosters(final GameData data)
	{
		
		// get the forum posters,
		final List<IForumPoster> forumPosters = new ArrayList<IForumPoster>();
		forumPosters.add((IForumPoster) findCachedOrCreateNew(NullForumPoster.class));
		forumPosters.add((IForumPoster) findCachedOrCreateNew(AxisAndAlliesForumPoster.class));
		forumPosters.add((IForumPoster) findCachedOrCreateNew(TripleAWarClubForumPoster.class));
		m_forumPosterEditor.setBeans(forumPosters);
		
		// now get the poster stored in the save game
		final IForumPoster forumPoster = (IForumPoster) data.getProperties().get(PBEMMessagePoster.FORUM_POSTER_PROP_NAME);
		if (forumPoster != null)
		{
			// if we have a cached version, use the credentials from this, as each player has different forum login
			final IForumPoster cached = (IForumPoster) LocalBeanCache.getInstance().getSerializable(forumPoster.getClass().getCanonicalName());
			if (cached != null)
			{
				forumPoster.setUsername(cached.getUsername());
				forumPoster.setPassword(cached.getPassword());
			}
			m_forumPosterEditor.setSelectedBean(forumPoster);
		}
	}
	
	private void loadWebPosters(final GameData data)
	{
		final List<IWebPoster> webPosters = new ArrayList<IWebPoster>();
		webPosters.add((IWebPoster) findCachedOrCreateNew(NullWebPoster.class));
		final TripleAWebPoster poster = (TripleAWebPoster) findCachedOrCreateNew(TripleAWebPoster.class);
		poster.setParties(data.getPlayerList().getNames());
		webPosters.add(poster);
		m_webPosterEditor.setBeans(webPosters);
		
		// now get the poster stored in the save game
		final IWebPoster webPoster = (IWebPoster) data.getProperties().get(PBEMMessagePoster.WEB_POSTER_PROP_NAME);
		if (webPoster != null)
		{
			poster.addToAllHosts(webPoster.getHost());
			webPoster.setAllHosts(poster.getAllHosts());
			m_webPosterEditor.setSelectedBean(webPoster);
		}
	}
	
	/**
	 * Configures the list of Email senders. If the game was saved we use this email sender.
	 * Since passwords are not stored in save games, the LocalBeanCache is checked
	 * 
	 * @param data
	 *            the game data
	 */
	private void loadEmailSender(final GameData data)
	{
		// The list of email, either loaded from cache or created
		final List<IEmailSender> emailSenders = new ArrayList<IEmailSender>();
		emailSenders.add((IEmailSender) findCachedOrCreateNew(NullEmailSender.class));
		emailSenders.add((IEmailSender) findCachedOrCreateNew(GmailEmailSender.class));
		emailSenders.add((IEmailSender) findCachedOrCreateNew(HotmailEmailSender.class));
		emailSenders.add((IEmailSender) findCachedOrCreateNew(GenericEmailSender.class));
		m_emailSenderEditor.setBeans(emailSenders);
		
		// now get the sender from the save game, update it with credentials from the cache, and set it
		final IEmailSender sender = (IEmailSender) data.getProperties().get(PBEMMessagePoster.EMAIL_SENDER_PROP_NAME);
		if (sender != null)
		{
			final IEmailSender cached = (IEmailSender) LocalBeanCache.getInstance().getSerializable(sender.getClass().getCanonicalName());
			if (cached != null)
			{
				sender.setUserName(cached.getUserName());
				sender.setPassword(cached.getPassword());
			}
			m_emailSenderEditor.setSelectedBean(sender);
		}
	}
	
	/**
	 * finds a cached instance of the give type. If a cached version is not available a new one is created
	 * 
	 * @param theClassType
	 *            the type of class
	 * @return a IBean either loaded from the cache or created
	 */
	private IBean findCachedOrCreateNew(final Class<? extends IBean> theClassType)
	{
		IBean cached = LocalBeanCache.getInstance().getSerializable(theClassType.getCanonicalName());
		if (cached == null)
		{
			try
			{
				cached = theClassType.newInstance();
			} catch (final Exception e)
			{
				throw new RuntimeException("Bean of type " + theClassType + " doesn't have public default constructor, error: " + e.getMessage());
			}
		}
		return cached;
	}
	
	public void shutDown()
	{
		m_gameSelectorModel.deleteObserver(this);
	}
	
	/**
	 * Called when the current game changes
	 */
	@Override
	public void cancel()
	{
		m_gameSelectorModel.deleteObserver(this);
	}
	
	/**
	 * Called when the observers detect change, to see if the game is in a startable state
	 * 
	 * @return
	 */
	@Override
	public boolean canGameStart()
	{
		if (m_gameSelectorModel.getGameData() == null)
			return false;
		
		final boolean diceServerValid = m_diceServerEditor.isBeanValid();
		final boolean summaryValid = m_forumPosterEditor.isBeanValid();
		final boolean webSiteValid = m_webPosterEditor.isBeanValid();
		final boolean emailValid = m_emailSenderEditor.isBeanValid();
		final boolean pbemReady = diceServerValid && summaryValid && emailValid && webSiteValid && m_gameSelectorModel.getGameData() != null;
		if (!pbemReady)
			return false;
		
		// make sure at least 1 player is enabled
		for (final PBEMLocalPlayerComboBoxSelector player : m_playerTypes)
		{
			if (player.isPlayerEnabled())
				return true;
		}
		return false;
	}
	
	@Override
	public void postStartGame()
	{
		// // store the dice server
		final GameData data = m_gameSelectorModel.getGameData();
		data.getProperties().set(DICE_ROLLER, m_diceServerEditor.getBean());
		
		// store the Turn Summary Poster
		final IForumPoster poster = (IForumPoster) m_forumPosterEditor.getBean();
		if (poster != null)
		{
			IForumPoster summaryPoster = poster;
			// clone the poster, the remove sensitive info, and put the clone into the game data
			// this was the sensitive info is not stored in the save game, but the user cache still has the password
			summaryPoster = summaryPoster.doClone();
			summaryPoster.clearSensitiveInfo();
			data.getProperties().set(PBEMMessagePoster.FORUM_POSTER_PROP_NAME, summaryPoster);
		}
		
		// store the email poster
		IEmailSender sender = (IEmailSender) m_emailSenderEditor.getBean();
		if (sender != null)
		{
			// create a clone, delete the sensitive information in the clone, and use it in the game
			// the locally cached version still has the password so the user doesn't have to enter it every time
			sender = sender.doClone();
			sender.clearSensitiveInfo();
			data.getProperties().set(PBEMMessagePoster.EMAIL_SENDER_PROP_NAME, sender);
		}
		
		// store the web site poster
		IWebPoster webPoster = (IWebPoster) m_webPosterEditor.getBean();
		if (webPoster != null)
		{
			webPoster = webPoster.doClone();
			webPoster.clearSensitiveInfo();
			data.getProperties().set(PBEMMessagePoster.WEB_POSTER_PROP_NAME, webPoster);
		}
		
		// store whether we are a pbem game or not, whether we are capable of posting a game save
		if (poster != null || sender != null || webPoster != null)
		{
			data.getProperties().set(PBEMMessagePoster.PBEM_GAME_PROP_NAME, true);
		}
	}
	
	/**
	 * Is called in response to the GameSelectionModel being updated. It means the we have to reload the form
	 * 
	 * @param o
	 *            always null
	 * @param arg
	 *            always null
	 */
	public void update(final Observable o, final Object arg)
	{
		
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					loadAll();
					layoutComponents();
				}
			});
			return;
		}
		else
		{
			loadAll();
			layoutComponents();
		}
	}
	
	/**
	 * Called when the user hits play
	 */
	@Override
	public ILauncher getLauncher()
	{
		// update local cache and write to disk before game starts
		final IForumPoster poster = (IForumPoster) m_forumPosterEditor.getBean();
		if (poster != null)
		{
			LocalBeanCache.getInstance().storeSerializable(poster.getClass().getCanonicalName(), poster);
		}
		
		final IEmailSender sender = (IEmailSender) m_emailSenderEditor.getBean();
		if (sender != null)
		{
			LocalBeanCache.getInstance().storeSerializable(sender.getClass().getCanonicalName(), sender);
		}
		
		final IWebPoster web = (IWebPoster) m_webPosterEditor.getBean();
		if (web != null)
		{
			LocalBeanCache.getInstance().storeSerializable(web.getClass().getCanonicalName(), web);
		}
		
		final IRemoteDiceServer server = (IRemoteDiceServer) m_diceServerEditor.getBean();
		LocalBeanCache.getInstance().storeSerializable(server.getDisplayName(), server);
		
		LocalBeanCache.getInstance().writeToDisk();
		
		// create local launcher
		final String gameUUID = (String) m_gameSelectorModel.getGameData().getProperties().get(GameData.GAME_UUID);
		final PBEMDiceRoller randomSource = new PBEMDiceRoller((IRemoteDiceServer) m_diceServerEditor.getBean(), gameUUID);
		final Map<String, String> playerTypes = new HashMap<String, String>();
		final Map<String, Boolean> playersEnabled = new HashMap<String, Boolean>();
		for (final PBEMLocalPlayerComboBoxSelector player : m_playerTypes)
		{
			playerTypes.put(player.getPlayerName(), player.getPlayerType());
			playersEnabled.put(player.getPlayerName(), player.isPlayerEnabled());
		}
		
		final PlayerListing pl = new PlayerListing(null, playersEnabled, playerTypes, m_gameSelectorModel.getGameData().getGameVersion(), m_gameSelectorModel.getGameName(),
					m_gameSelectorModel.getGameRound(), null, null); // we don't need the playerToNode list, the disable-able players, or the alliances list, for a local game
		return new LocalLauncher(m_gameSelectorModel, randomSource, pl);
	}
	
	
	// -----------------------------------------------------------------------
	// inner classes
	// -----------------------------------------------------------------------
	
	/**
	 * A property change listener that notify our observers
	 */
	private class NotifyingPropertyChangeListener implements PropertyChangeListener
	{
		public void propertyChange(final PropertyChangeEvent evt)
		{
			notifyObservers();
		}
	}
	
	public String getPlayerType(final String playerName)
	{
		for (final PBEMLocalPlayerComboBoxSelector item : m_playerTypes)
		{
			if (item.getPlayerName().equals(playerName))
				return item.getPlayerType();
		}
		throw new IllegalStateException("No player found:" + playerName);
	}
	
	private void layoutPlayerPanel(final SetupPanel parent)
	{
		final GameData data = m_gameSelectorModel.getGameData();
		m_localPlayerPanel.removeAll();
		m_playerTypes.clear();
		m_localPlayerPanel.setLayout(new GridBagLayout());
		if (data == null)
		{
			m_localPlayerPanel.add(new JLabel("No game selected!"));
			return;
		}
		final Collection<String> disableable = data.getPlayerList().getPlayersThatMayBeDisabled();
		final HashMap<String, Boolean> playersEnablementListing = data.getPlayerList().getPlayersEnabledListing();
		final Map<String, String> reloadSelections = PlayerID.currentPlayers(data);
		final String[] playerTypes = data.getGameLoader().getServerPlayerTypes();
		final String[] playerNames = data.getPlayerList().getNames();
		// if the xml was created correctly, this list will be in turn order. we want to keep it that way.
		int gridx = 0;
		int gridy = 0;
		if (!disableable.isEmpty() || playersEnablementListing.containsValue(Boolean.FALSE))
		{
			final JLabel enableLabel = new JLabel("Use");
			enableLabel.setForeground(Color.black);
			m_localPlayerPanel.add(enableLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		}
		final JLabel nameLabel = new JLabel("Name");
		nameLabel.setForeground(Color.black);
		m_localPlayerPanel.add(nameLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		final JLabel typeLabel = new JLabel("Type");
		typeLabel.setForeground(Color.black);
		m_localPlayerPanel.add(typeLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		final JLabel allianceLabel = new JLabel("Alliance");
		allianceLabel.setForeground(Color.black);
		m_localPlayerPanel.add(allianceLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 7, 5, 5), 0, 0));
		for (int i = 0; i < playerNames.length; i++)
		{
			final PBEMLocalPlayerComboBoxSelector selector = new PBEMLocalPlayerComboBoxSelector(playerNames[i], reloadSelections, disableable, playersEnablementListing,
						data.getAllianceTracker().getAlliancesPlayerIsIn(data.getPlayerList().getPlayerID(playerNames[i])), playerTypes, parent);
			m_playerTypes.add(selector);
			selector.layout(++gridy, m_localPlayerPanel);
		}
		m_localPlayerPanel.validate();
		m_localPlayerPanel.invalidate();
	}
}


class PBEMLocalPlayerComboBoxSelector
{
	private final JCheckBox m_enabledCheckBox;
	private final String m_playerName;
	private final JComboBox m_playerTypes;
	private final String m_playerAlliances;
	private boolean m_enabled = true;
	private final JLabel m_name;
	private final JLabel m_alliances;
	private final Collection<String> m_disableable;
	private final String[] m_types;
	private final SetupPanel m_parent;
	
	PBEMLocalPlayerComboBoxSelector(final String playerName, final Map<String, String> reloadSelections, final Collection<String> disableable, final HashMap<String, Boolean> playersEnablementListing,
				final Collection<String> playerAlliances, final String[] types, final SetupPanel parent)
	{
		m_playerName = playerName;
		m_name = new JLabel(m_playerName + ":");
		m_enabledCheckBox = new JCheckBox();
		m_enabledCheckBox.addActionListener(m_disablePlayerActionListener);
		m_enabledCheckBox.setSelected(playersEnablementListing.get(playerName));
		m_enabledCheckBox.setEnabled(disableable.contains(playerName));
		m_disableable = disableable;
		m_parent = parent;
		m_types = types;
		m_playerTypes = new JComboBox(types);
		String previousSelection = reloadSelections.get(playerName);
		if (previousSelection.equalsIgnoreCase("Client"))
			previousSelection = types[0];
		if (!(previousSelection.equals("no_one")) && Arrays.asList(types).contains(previousSelection))
		{
			m_playerTypes.setSelectedItem(previousSelection);
		}
		else if (m_playerName.startsWith("Neutral") || playerName.startsWith("AI"))
		{
			m_playerTypes.setSelectedItem(types[Math.max(0, Math.min(types.length - 1, 3))]); // the 4th in the list should be Pro AI (Hard AI)
		}
		// we do not set the default for the combobox because the default is the top item, which in this case is human
		if (playerAlliances.contains(playerName))
			m_playerAlliances = "";
		else
			m_playerAlliances = playerAlliances.toString();
		m_alliances = new JLabel(m_playerAlliances.toString());
		setWidgetActivation();
	}
	
	public void layout(final int row, final Container container)
	{
		int gridx = 0;
		if (!m_disableable.isEmpty())
		{
			container.add(m_enabledCheckBox, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		}
		container.add(m_name, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		container.add(m_playerTypes, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		container.add(m_alliances, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 7, 5, 5), 0, 0));
	}
	
	public String getPlayerName()
	{
		return m_playerName;
	}
	
	public String getPlayerType()
	{
		return (String) m_playerTypes.getSelectedItem();
	}
	
	public boolean isPlayerEnabled()
	{
		return m_enabledCheckBox.isSelected();
	}
	
	private void setWidgetActivation()
	{
		m_name.setEnabled(m_enabled);
		m_alliances.setEnabled(m_enabled);
		m_enabledCheckBox.setEnabled(m_disableable.contains(m_playerName));
		m_parent.notifyObservers();
	}
	
	private final ActionListener m_disablePlayerActionListener = new ActionListener()
	{
		public void actionPerformed(final ActionEvent e)
		{
			if (m_enabledCheckBox.isSelected())
			{
				m_enabled = true;
				m_playerTypes.setSelectedItem(m_types[0]); // the 1st in the list should be human
			}
			else
			{
				m_enabled = false;
				m_playerTypes.setSelectedItem(m_types[Math.max(0, Math.min(m_types.length - 1, 1))]); // the 2nd in the list should be Weak AI
			}
			setWidgetActivation();
		}
	};
}
