package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.ui.NewGameChooser;
import games.strategy.engine.framework.ui.NewGameChooserEntry;
import games.strategy.engine.framework.ui.SaveGameFileChooser;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class GameSelectorPanel extends JPanel implements Observer
{
	private static final long serialVersionUID = -4598107601238030020L;
	private JLabel m_nameText;
	private JLabel m_versionText;
	private JLabel m_fileNameLabel;
	private JLabel m_fileNameText;
	private JLabel m_nameLabel;
	private JLabel m_versionLabel;
	private JLabel m_roundLabel;
	private JLabel m_roundText;
	private JButton m_loadSavedGame;
	private JButton m_loadNewGame;
	private JButton m_gameOptions;
	private final GameSelectorModel m_model;
	private final IGamePropertiesCache m_gamePropertiesCache = new FileBackedGamePropertiesCache();
	private final Map<String, Object> m_originalPropertiesMap = new HashMap<String, Object>();
	
	public GameSelectorPanel(final GameSelectorModel model)
	{
		m_model = model;
		m_model.addObserver(this);
		final GameData data = model.getGameData();
		if (data != null)
		{
			setOriginalPropertiesMap(data);
			m_gamePropertiesCache.loadCachedGamePropertiesInto(data);
		}
		createComponents();
		layoutComponents();
		setupListeners();
		setWidgetActivation();
		updateGameData();
	}
	
	private void updateGameData()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					updateGameData();
				}
			});
			return;
		}
		m_nameText.setText(m_model.getGameName());
		m_versionText.setText(m_model.getGameVersion());
		m_roundText.setText(m_model.getGameRound());
		String fileName = m_model.getFileName();
		if (fileName != null && fileName.length() > 1)
		{
			try
			{
				fileName = URLDecoder.decode(fileName, "UTF-8");
			} catch (final IllegalArgumentException e)
			{// ignore
			} catch (final UnsupportedEncodingException e)
			{// ignore
			}
		}
		m_fileNameText.setText(getFormattedFileNameText(fileName, Math.max(22, 3 + m_nameText.getText().length() + m_nameLabel.getText().length())));
		m_fileNameText.setToolTipText(fileName);
	}
	
	/**
	 * Formats the file name text to two lines.
	 * The separation focuses on the second line being at least the filename while the first line
	 * should show the the path including '...' in case it does not fit
	 * 
	 * @param fileName
	 *            full file name
	 * @param maxLength
	 *            maximum number of characters per line
	 * @return filename formatted file name - in case it is too long (> maxLength) to two lines
	 */
	private String getFormattedFileNameText(final String fileName, final int maxLength)
	{
		if (fileName.length() <= maxLength)
		{
			return fileName;
		}
		int cutoff = fileName.length() - maxLength;
		String secondLine = fileName.substring(cutoff);
		if (secondLine.contains("/"))
		{
			cutoff += secondLine.indexOf("/") + 1;
		}
		secondLine = fileName.substring(cutoff);
		String firstLine = fileName.substring(0, cutoff);
		
		if (firstLine.length() > maxLength)
		{
			firstLine = firstLine.substring(0, maxLength - 4);
			if (firstLine.contains("/"))
			{
				cutoff = firstLine.lastIndexOf("/") + 1;
				firstLine = firstLine.substring(0, cutoff) + ".../";
			}
			else
				firstLine = firstLine + "...";
		}
		return "<html><p>" + firstLine + "<br/>" + secondLine + "</p></html>";
	}
	
	private void createComponents()
	{
		m_nameLabel = new JLabel("Game Name:");
		m_versionLabel = new JLabel("Game Version:");
		m_roundLabel = new JLabel("Game Round:");
		m_fileNameLabel = new JLabel("File Name:");
		m_nameText = new JLabel();
		m_versionText = new JLabel();
		m_roundText = new JLabel();
		m_fileNameText = new JLabel();
		m_loadNewGame = new JButton("Choose Game...");
		m_loadNewGame.setToolTipText("<html>Select a game from all the maps/games that come with TripleA, <br>and the ones you have downloaded.</html>");
		m_loadSavedGame = new JButton("Load Saved Game...");
		m_loadSavedGame.setToolTipText("Load a previously saved game, or an autosave.");
		m_gameOptions = new JButton("Game Options...");
		m_gameOptions.setToolTipText("<html>Set options for the currently selected game, <br>such as enabling/disabling Low Luck, or Technology, etc.</html>");
	}
	
	private void layoutComponents()
	{
		setLayout(new GridBagLayout());
		add(m_nameLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 3, 5), 0, 0));
		add(m_nameText, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 3, 0), 0, 0));
		add(m_versionLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 3, 5), 0, 0));
		add(m_versionText, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 3, 0), 0, 0));
		add(m_roundLabel, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 3, 5), 0, 0));
		add(m_roundText, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 3, 0), 0, 0));
		add(m_fileNameLabel, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 10, 3, 5), 0, 0));
		add(m_fileNameText, new GridBagConstraints(0, 4, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 3, 5), 0, 0));
		add(m_loadNewGame, new GridBagConstraints(0, 5, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(25, 10, 10, 10), 0, 0));
		add(m_loadSavedGame, new GridBagConstraints(0, 6, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 10, 10), 0, 0));
		add(m_gameOptions, new GridBagConstraints(0, 7, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(25, 10, 10, 10), 0, 0));
		// spacer
		add(new JPanel(), new GridBagConstraints(0, 8, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
	}
	
	private void setupListeners()
	{
		m_loadNewGame.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (canSelectLocalGameData())
				{
					selectGameFile(false);
				}
				else if (canChangeHostBotGameData())
				{
					final ClientModel clientModelForHostBots = m_model.getClientModelForHostBots();
					if (clientModelForHostBots != null)
					{
						clientModelForHostBots.getHostBotSetMapClientAction(GameSelectorPanel.this).actionPerformed(e);
					}
				}
			}
		});
		m_loadSavedGame.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (canSelectLocalGameData())
				{
					selectGameFile(true);
				}
				else if (canChangeHostBotGameData())
				{
					final ClientModel clientModelForHostBots = m_model.getClientModelForHostBots();
					if (clientModelForHostBots != null)
					{
						final JPopupMenu menu = new JPopupMenu();
						menu.add(clientModelForHostBots.getHostBotChangeGameToSaveGameClientAction(GameSelectorPanel.this));
						menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE));
						menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE2));
						menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_ODD));
						menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_EVEN));
						menu.add(clientModelForHostBots.getHostBotGetGameSaveClientAction(GameSelectorPanel.this));
						final Point point = m_loadSavedGame.getLocation();
						menu.show(GameSelectorPanel.this, point.x + m_loadSavedGame.getWidth(), point.y);
					}
				}
			}
		});
		m_gameOptions.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (canSelectLocalGameData())
				{
					selectGameOptions();
				}
				else if (canChangeHostBotGameData())
				{
					final ClientModel clientModelForHostBots = m_model.getClientModelForHostBots();
					if (clientModelForHostBots != null)
					{
						clientModelForHostBots.getHostBotChangeGameOptionsClientAction(GameSelectorPanel.this).actionPerformed(e);
					}
				}
			}
		});
	}
	
	private void setOriginalPropertiesMap(final GameData data)
	{
		m_originalPropertiesMap.clear();
		if (data != null)
		{
			for (final IEditableProperty property : data.getProperties().getEditableProperties())
			{
				m_originalPropertiesMap.put(property.getName(), property.getValue());
			}
		}
	}
	
	private void selectGameOptions()
	{
		// backup current game properties before showing dialog
		final Map<String, Object> currentPropertiesMap = new HashMap<String, Object>();
		for (final IEditableProperty property : m_model.getGameData().getProperties().getEditableProperties())
		{
			currentPropertiesMap.put(property.getName(), property.getValue());
		}
		
		final PropertiesUI panel = new PropertiesUI(m_model.getGameData().getProperties(), true);
		final JScrollPane scroll = new JScrollPane(panel);
		scroll.setBorder(null);
		scroll.getViewport().setBorder(null);
		
		final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
		final String ok = "OK";
		final String cancel = "Cancel";
		final String makeDefault = "Make Default";
		final String reset = "Reset";
		pane.setOptions(new Object[] { ok, makeDefault, reset, cancel });
		final JDialog window = pane.createDialog(JOptionPane.getFrameForComponent(this), "Game Options");
		window.setVisible(true);
		
		final Object buttonPressed = pane.getValue();
		if (buttonPressed == null || buttonPressed.equals(cancel))
		{
			// restore properties, if cancel was pressed, or window was closed
			final Iterator<IEditableProperty> itr = m_model.getGameData().getProperties().getEditableProperties().iterator();
			while (itr.hasNext())
			{
				final IEditableProperty property = itr.next();
				property.setValue(currentPropertiesMap.get(property.getName()));
			}
		}
		else if (buttonPressed.equals(reset))
		{
			if (!m_originalPropertiesMap.isEmpty())
			{
				// restore properties, if cancel was pressed, or window was closed
				final Iterator<IEditableProperty> itr = m_model.getGameData().getProperties().getEditableProperties().iterator();
				while (itr.hasNext())
				{
					final IEditableProperty property = itr.next();
					property.setValue(m_originalPropertiesMap.get(property.getName()));
				}
				selectGameOptions();
				return;
			}
		}
		else if (buttonPressed.equals(makeDefault))
		{
			m_gamePropertiesCache.cacheGameProperties(m_model.getGameData());
		}
		else
		{
			// ok was clicked, and we have modified the properties already
		}
	}
	
	private void setWidgetActivation()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					setWidgetActivation();
				}
			});
			return;
		}
		final boolean canSelectGameData = canSelectLocalGameData();
		final boolean canChangeHostBotGameData = canChangeHostBotGameData();
		m_loadSavedGame.setEnabled(canSelectGameData || canChangeHostBotGameData);
		m_loadNewGame.setEnabled(canSelectGameData || canChangeHostBotGameData);
		// Disable game options if there are none.
		if (canChangeHostBotGameData || (canSelectGameData && m_model.getGameData() != null && m_model.getGameData().getProperties().getEditableProperties().size() > 0))
			m_gameOptions.setEnabled(true);
		else
			m_gameOptions.setEnabled(false);
		// we don't want them starting new games if we are an old jar
		if (GameRunner2.areWeOldExtraJar())
		{
			m_loadNewGame.setEnabled(false);
			// m_loadSavedGame.setEnabled(false);
			m_loadNewGame.setToolTipText("This is disabled on older engine jars, please start new games with the latest version of TripleA.");
			// m_loadSavedGame.setToolTipText("This is disabled on older engine jars, please open savegames from the latest version of TripleA.");
		}
	}
	
	private boolean canSelectLocalGameData()
	{
		return m_model != null && m_model.canSelect();
	}
	
	private boolean canChangeHostBotGameData()
	{
		return m_model != null && m_model.isHostHeadlessBot();
	}
	
	public void update(final Observable o, final Object arg)
	{
		updateGameData();
		setWidgetActivation();
	}
	
	public static File selectGameFile(final Component parent)
	{
		if (GameRunner.isMac())
		{
			final FileDialog fileDialog = new FileDialog(JOptionPane.getFrameForComponent(parent));
			fileDialog.setMode(FileDialog.LOAD);
			SaveGameFileChooser.ensureDefaultDirExists();
			fileDialog.setDirectory(SaveGameFileChooser.DEFAULT_DIRECTORY.getPath());
			fileDialog.setFilenameFilter(new FilenameFilter()
			{
				public boolean accept(final File dir, final String name)
				{
					// the extension should be .tsvg, but find svg extensions as well
					// also, macs download the file as tsvg.gz, so accept that as well
					return name.endsWith(".tsvg") || name.endsWith(".svg") || name.endsWith("tsvg.gz");
				}
			});
			fileDialog.setVisible(true);
			final String fileName = fileDialog.getFile();
			final String dirName = fileDialog.getDirectory();
			if (fileName == null)
				return null;
			else
			{
				final File f = new File(dirName, fileName);
				return f;
			}
		}
		else
		{
			// Non-Mac platforms should use the normal Swing JFileChooser
			final JFileChooser fileChooser = SaveGameFileChooser.getInstance();
			final int rVal = fileChooser.showOpenDialog(JOptionPane.getFrameForComponent(parent));
			if (rVal != JFileChooser.APPROVE_OPTION)
				return null;
			return fileChooser.getSelectedFile();
		}
	}
	
	private void selectGameFile(final boolean saved)
	{
		// For some strange reason,
		// the only way to get a Mac OS X native-style file dialog
		// is to use an AWT FileDialog instead of a Swing JDialog
		if (saved)
		{
			final File file = selectGameFile(GameRunner.isMac() ? MainFrame.getInstance() : JOptionPane.getFrameForComponent(this));
			if (file == null || !file.exists())
				return;
			m_model.load(file, this);
			setOriginalPropertiesMap(m_model.getGameData());
		}
		else
		{
			final NewGameChooserEntry entry = NewGameChooser.chooseGame(JOptionPane.getFrameForComponent(this), m_model.getGameName());
			if (entry != null)
			{
				if (!entry.isGameDataLoaded())
				{
					try
					{
						entry.fullyParseGameData();
					} catch (final GameParseException e)
					{
						entry.delayParseGameData();
						NewGameChooser.getNewGameChooserModel().removeEntry(entry);
						return;
					}
				}
				m_model.load(entry);
				setOriginalPropertiesMap(m_model.getGameData());
				// only for new games, not saved games, we set the default options, and set them only once (the first time it is loaded)
				m_gamePropertiesCache.loadCachedGamePropertiesInto(m_model.getGameData());
			}
		}
	}
	
	public static void main(final String[] args)
	{
		final JFrame f = new JFrame();
		final GameSelectorModel model = new GameSelectorModel();
		model.loadDefaultGame(f);
		f.getContentPane().add(new GameSelectorPanel(model));
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.pack();
		f.setVisible(true);
	}
}
