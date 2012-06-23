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
package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ui.NewGameChooser;
import games.strategy.engine.framework.ui.NewGameChooserEntry;
import games.strategy.engine.framework.ui.NewGameChooserModel;
import games.strategy.triplea.ai.Dynamix_AI.Dynamix_AI;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

public class GameSelectorModel extends Observable
{
	/**
	 * Returns the name of the directory within a map's directory where the game xml is held.
	 * Example: returns "games" which would be the games folder of "triplea/maps/someMapFooBar/games"
	 */
	public static final String DEFAULT_GAME_XML_DIRECTORY_NAME = "games";
	/** Returns the folder where maps are held, example: "/maps" */
	public static final File DEFAULT_MAP_DIRECTORY = new File(GameRunner.getRootFolder(), "/maps");
	private static final String DEFAULT_GAME_NAME_PREF = "DefaultGameName2";
	private static final String DEFAULT_GAME_NAME = "Big World : 1942";
	private static final String DEFAULT_GAME_URI_PREF = "DefaultGameURI";
	private static final String DEFAULT_GAME_URI = "";
	private GameData m_data;
	private String m_gameName;
	private String m_gameVersion;
	private String m_gameRound;
	private String m_fileName;
	private boolean m_canSelect = true;
	
	public GameSelectorModel()
	{
		setGameData(null);
	}
	
	public void load(final NewGameChooserEntry entry)
	{
		m_fileName = entry.getLocation();
		setGameData(entry.getGameData());
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		prefs.put(DEFAULT_GAME_NAME_PREF, entry.getGameData().getGameName());
		prefs.put(DEFAULT_GAME_URI_PREF, entry.getURI().toString());
		try
		{
			prefs.flush();
		} catch (final BackingStoreException e)
		{
			// ignore
		}
	}
	
	public void load(final File file, final Component ui)
	{
		final GameDataManager manager = new GameDataManager();
		if (!file.exists())
		{
			error("Could not find file:" + file, ui);
			return;
		}
		if (file.isDirectory())
		{
			error("Cannot load a directory:" + file, ui);
			return;
		}
		GameData newData;
		try
		{
			// if the file name is xml, load it as a new game
			if (file.getName().toLowerCase().endsWith("xml"))
			{
				newData = (new GameParser()).parse(new FileInputStream(file), false);
			}
			// the extension should be tsvg, but
			// try to load it as a saved game whatever the extension
			else
			{
				newData = manager.loadGame(file);
			}
			m_fileName = file.getName();
			setGameData(newData);
		} catch (final EngineVersionException e)
		{
			System.out.println(e.getMessage());
		} catch (final Exception e)
		{
			e.printStackTrace(System.out);
			error(e.getMessage(), ui);
		}
	}
	
	public boolean isSavedGame()
	{
		return !m_fileName.endsWith(".xml");
	}
	
	private void error(final String message, final Component ui)
	{
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ui), message, "Could not load Game", JOptionPane.ERROR_MESSAGE);
	}
	
	public synchronized GameData getGameData()
	{
		return m_data;
	}
	
	public void setCanSelect(final boolean aBool)
	{
		synchronized (this)
		{
			m_canSelect = aBool;
		}
		notifyObs();
	}
	
	public synchronized boolean canSelect()
	{
		return m_canSelect;
	}
	
	/**
	 * We dont have a gane data (ie we are a remote player and the data has not been sent yet), but
	 * we still want to display game info
	 */
	public void clearDataButKeepGameInfo(final String gameName, final String gameRound, final String gameVersion)
	{
		synchronized (this)
		{
			m_data = null;
			m_gameName = gameName;
			m_gameRound = gameRound;
			m_gameVersion = gameVersion;
		}
		notifyObs();
	}
	
	public synchronized String getFileName()
	{
		if (m_data == null)
			return "-";
		else
			return m_fileName;
	}
	
	public synchronized String getGameName()
	{
		return m_gameName;
	}
	
	public synchronized String getGameRound()
	{
		return m_gameRound;
	}
	
	public synchronized String getGameVersion()
	{
		return m_gameVersion;
	}
	
	public void setGameData(final GameData data)
	{
		synchronized (this)
		{
			if (data == null)
			{
				m_gameName = m_gameRound = m_gameVersion = "-";
			}
			else
			{
				m_gameName = data.getGameName();
				m_gameRound = "" + data.getSequence().getRound();
				m_gameVersion = data.getGameVersion().toString();
			}
			m_data = data;
		}
		notifyObs();
	}
	
	private void notifyObs()
	{
		super.setChanged();
		super.notifyObservers(m_data);
		super.clearChanged();
	}
	
	private void resetDefaultGame()
	{
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		prefs.put(DEFAULT_GAME_NAME_PREF, DEFAULT_GAME_NAME);
		prefs.put(DEFAULT_GAME_URI_PREF, DEFAULT_GAME_URI);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException e2)
		{ // ignore
		}
	}
	
	public void loadDefaultGame(final Component ui)
	{
		// clear out dynamix's properties (this ended up being the best place to put it, as we have definitely left a game at this point)
		Dynamix_AI.clearCachedGameDataAll();
		loadDefaultGame(ui, false);
	}
	
	/**
	 * 
	 * @param ui
	 * @param forceFactoryDefault
	 *            - False is default behavior and causes the new game chooser model to be cleared (and refreshed if needed).
	 *            True causes the default game preference to be reset, but the model does not get cleared/refreshed (because we only call with 'true' if loading the user preferred map failed).
	 */
	private void loadDefaultGame(final Component ui, final boolean forceFactoryDefault)
	{
		// load the previously saved value
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		if (forceFactoryDefault)
		{
			// we don't refresh the game chooser model because we have just removed a bad map from it
			resetDefaultGame();
		}
		NewGameChooserEntry selectedGame = null;
		// just in case flush doesn't work, we still force it again here
		final String userPreferredDefaultGameURI = (forceFactoryDefault ? DEFAULT_GAME_URI : prefs.get(DEFAULT_GAME_URI_PREF, DEFAULT_GAME_URI));
		if (!forceFactoryDefault && userPreferredDefaultGameURI != null && userPreferredDefaultGameURI.length() > 0)
		{
			// if the user has a preferred URI, then we load it, and don't bother parsing or doing anything with the whole game model list
			try
			{
				final URI defaultURI = new URI(userPreferredDefaultGameURI);
				selectedGame = new NewGameChooserEntry(defaultURI);
			} catch (final Exception e)
			{
				/*try
				{
					userPreferredDefaultGameURI = userPreferredDefaultGameURI.replaceAll(".zip!", ".zip");
					final URI defaultURI2 = new URI(userPreferredDefaultGameURI);
					selectedGame = new NewGameChooserEntry(defaultURI2);
				} catch (final Exception e2)
				{*/
				System.out.println(userPreferredDefaultGameURI + " could not be loaded.");
				loadDefaultGame(ui, true);
				return;
				// }
			}
			if (!selectedGame.isGameDataLoaded())
			{
				try
				{
					selectedGame.fullyParseGameData();
				} catch (final GameParseException e)
				{
					loadDefaultGame(ui, true);
					return;
				}
			}
			// since we are not forceFactoryDefault, and since we are loading purely from the URI without loading the new game chooser model, we might as well refresh it in a separate thread
			new Thread(new Runnable()
			{
				public void run()
				{
					NewGameChooser.refreshNewGameChooserModel();
				}
			}).start();
		}
		else
		{
			if (!forceFactoryDefault)
			{
				// we would rather have their game data refreshed after leaving a game
				NewGameChooser.refreshNewGameChooserModel();
			}
			// just in case flush doesn't work, we still force it again here
			final String userPreferredDefaultGameName = (forceFactoryDefault ? DEFAULT_GAME_NAME : prefs.get(DEFAULT_GAME_NAME_PREF, DEFAULT_GAME_NAME));
			final NewGameChooserModel model = NewGameChooser.getNewGameChooserModel();
			selectedGame = model.findByName(userPreferredDefaultGameName);
			if (selectedGame == null)
			{
				selectedGame = model.findByName(DEFAULT_GAME_NAME);
			}
			if (selectedGame == null && model.size() > 0)
			{
				selectedGame = model.get(0);
			}
			if (selectedGame == null)
			{
				return;
			}
			if (!selectedGame.isGameDataLoaded())
			{
				try
				{
					selectedGame.fullyParseGameData();
				} catch (final GameParseException e)
				{
					// Load real default game...
					selectedGame.delayParseGameData();
					model.removeEntry(selectedGame);
					loadDefaultGame(ui, true);
					return;
				}
			}
		}
		load(selectedGame);
	}
}
