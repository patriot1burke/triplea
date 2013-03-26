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
package games.strategy.sound;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.framework.GameRunner;
import games.strategy.triplea.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Utility for loading and playing sound clips.
 * 
 * Stores a preference in the user preferences for being silent.
 * The property will persist and be reloaded after the virtual machine
 * has been stopped and restarted.
 * 
 * <br>
 * <br>
 * <br>
 * 
 * <br>
 * <br>
 * How it works: <br>
 * <b>Sound.Default.Folder</b>=ww2 <br>
 * This is the "key" that tells the engine which sound folder to use as the DEFAULT sound folder. <br>
 * The default folders are as follows: <br>
 * "<b>ww2</b>" (which should cover ww1 - ww2 - ww3 sounds), <br>
 * "<b>preindustrial</b>" (anything from muskets/cannons (1500) to right before ww1 (1900), <br>
 * "<b>classical</b>" (the ancient era, anything before cannons became a mainstay (10,000 bce - 1500 ad) <br>
 * "<b>future</b>" (sci-fi, spaceships, lasers, etc) <br>
 * <br>
 * After this, you can specify specific sounds if you want, using the "sound key location" (aka: sound map folder). <br>
 * The sound key location is the exact folder name for a sound you want, located under the "generic" folder.
 * What I mean by this is that all sound key locations that triplea supports, are the names of all the folders in the "assets/sounds/generic/" folder. <br>
 * example: <br>
 * <b>battle_aa_miss</b>=ww2/battle_aa_miss;future/battle_aa_miss/battle_aa_miss_01_ufo_flyby.wav <br>
 * "battle_aa_miss" is one of the folders under "generic", therefore it is a "sound location key" <br>
 * We can set this equal to any list of sounds paths, each separated by a semicolon (;). The engine will pick one at random each time we need to play this sound. <br>
 * The "sound path" can be a "folder" or a "file". If it is a folder, we will use all the sounds in that folder.
 * If it is a file, we will only use that file. We can use a file and folder and another file and another folder, all together. <br>
 * Example: "<b>ww2/battle_aa_miss</b>" is the sound path for a folder, so we will use all the sounds in that folder.
 * "<b>future/battle_aa_miss/battle_aa_miss_01_ufo_flyby.wav</b>" is a specific file, so we will use just this file.
 * Because we use both of these together, the engine will make a list of all the files in that folder, plus that single file we specified,
 * then it will randomly pick one of this whole list every time it needs to play the "battle_aa_miss" sound. <br>
 * <br>
 * So, lets say that you want to play 2 sounds, for the "battle_land" sound key.
 * One of them is located at "tripleainstallfolder/assets/sounds/generic/battle_land_01_angry_drumming_noise.wav".
 * The other is located at "tripleainstallfolder/assets/sounds/classical/battle_land_02_war_trumpets.wav". Then the entry would look like this: <br>
 * battle_land=generic/battle_land_01_angry_drumming_noise.wav;classical/battle_land_02_war_trumpets.wav <br>
 * If you wanted it to also play every single sound in the "tripleainstallfolder/assets/sounds/ww2/battle_land/" folder, then you would add that folder to path: <br>
 * battle_land=generic/battle_land_01_angry_drumming_noise.wav;classical/battle_land_02_war_trumpets.wav;ww2/battle_land <br>
 * <br>
 * Furthermore, we can customize the sound key by adding "_nationName" onto the end of it. So if you want a specific sound for a german land attack, then use: <br>
 * battle_land<b>_Germans</b>=misc/battle_land/battle_land_Germans_panzers_and_yelling_in_german.wav <br>
 * You can use nation specific sound keys for almost all sounds, though things like game_start, or chat_message, will never use them. <br>
 * <br>
 * <br>
 * <br>
 * <br>
 * <b>You do not need to specify every single "sound key". This is why/because we have the "Sound.Default.Folder".</b> <br>
 * <br>
 * The logic is as follows: <br>
 * Engine needs to play the "game_start" sound. <br>
 * 1. Check for a sound.properties file. <br>
 * 2. If none exists, pretend that one exists and that it only contains this line: "Sound.Default.Folder=ww2" <br>
 * 3. Look in the sound.properties file for the specific sound key "game_start" <br>
 * 4. Create a list of all sounds that the key includes.
 * If no key, then just use all the sounds in "Sound.Default.Folder/sound_key/" (which for us would be "ww2/game_start/" folder). <br>
 * 5. If no sounds are found, then use all the sounds located at "generic/sound_key/" (which for us would be "generic/game_start").
 * (if any sounds are found in step 4 above, then we ignore the generic folder completely) <br>
 * 6. Randomize the list's order, then pick one, and play the sound.
 * 
 * 
 * @author veqryn & frigoref
 */
public class ClipPlayer
{
	private static ClipPlayer s_clipPlayer;
	private boolean m_beSilent = false;
	private final HashSet<String> m_mutedClips = new HashSet<String>();
	private final HashMap<String, List<File>> m_sounds = new HashMap<String, List<File>>();
	private final ResourceLoader m_resourceLoader;
	private final Set<String> m_subFolders = new HashSet<String>();
	private final ClipCache m_clipCache = new ClipCache(24); // MacOS and Linux can only handle 30 or 32 sound files being open at same time, so we'll be safe and pick 24
	
	// standard settings
	private static final String ASSETS_SOUNDS_FOLDER = "sounds";
	private static final String SOUND_PREFERENCE_GLOBAL_SWITCH = "beSilent2";
	private static final String SOUND_PREFERENCE_PREFIX = "sound_";
	
	public static synchronized ClipPlayer getInstance()
	{
		if (s_clipPlayer == null)
		{
			s_clipPlayer = new ClipPlayer(ResourceLoader.getMapResourceLoader(null, true));
			SoundPath.preLoadSounds(SoundPath.SoundType.GENERAL);
		}
		return s_clipPlayer;
	}
	
	public static synchronized ClipPlayer getInstance(final ResourceLoader resourceLoader, final GameData data)
	{
		// make a new clip player if we switch resource loaders (ie: if we switch maps)
		if (s_clipPlayer == null || s_clipPlayer.m_resourceLoader != resourceLoader)
		{
			// stop and close any playing clips
			if (s_clipPlayer != null)
				s_clipPlayer.m_clipCache.removeAll();
			// make a new clip player with our new resource loader
			s_clipPlayer = new ClipPlayer(resourceLoader, data);
			SoundPath.preLoadSounds(SoundPath.SoundType.GENERAL);
		}
		return s_clipPlayer;
	}
	
	private ClipPlayer(final ResourceLoader resourceLoader)
	{
		m_resourceLoader = resourceLoader;
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		m_beSilent = prefs.getBoolean(SOUND_PREFERENCE_GLOBAL_SWITCH, false);
		final HashSet<String> choices = SoundPath.getAllSoundOptions();
		/* until we get better sounds, all sounds start as muted, except for Slapping
		choices.remove(SoundPath.CLIP_CHAT_SLAP);
		final boolean slapMuted = prefs.getBoolean(SOUND_PREFERENCE_PREFIX + SoundPath.CLIP_CHAT_SLAP, false);
		if (slapMuted)
			m_mutedClips.add(SoundPath.CLIP_CHAT_SLAP);*/
		for (final String sound : choices)
		{
			final boolean muted = prefs.getBoolean(SOUND_PREFERENCE_PREFIX + sound, false); // true until we get better sounds
			if (muted)
				m_mutedClips.add(sound);
		}
	}
	
	private ClipPlayer(final ResourceLoader resourceLoader, final GameData data)
	{
		this(resourceLoader);
		for (final PlayerID p : data.getPlayerList().getPlayers())
		{
			m_subFolders.add(p.getName());
		}
	}
	
	/**
	 * If set to true, no sounds will play.
	 * 
	 * This property is persisted using the java.util.prefs API, and will
	 * persist after the vm has stopped.
	 * 
	 * @param aBool
	 *            new value for m_beSilent
	 */
	public static void setBeSilent(final boolean aBool)
	{
		final ClipPlayer clipPlayer = getInstance();
		clipPlayer.m_beSilent = aBool;
		
		final Preferences prefs = Preferences.userNodeForPackage(clipPlayer.getClass());
		prefs.putBoolean(SOUND_PREFERENCE_GLOBAL_SWITCH, clipPlayer.m_beSilent);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static boolean getBeSilent()
	{
		final ClipPlayer clipPlayer = getInstance();
		return clipPlayer.m_beSilent;
	}
	
	// please avoid unnecessary calls of this
	private void putSoundInPreferences(final String clip, final boolean isMuted)
	{
		final ClipPlayer clipPlayer = getInstance();
		final Preferences prefs = Preferences.userNodeForPackage(clipPlayer.getClass());
		prefs.putBoolean(SOUND_PREFERENCE_PREFIX + clip, isMuted);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public ArrayList<IEditableProperty> getSoundOptions(final SoundPath.SoundType sounds)
	{
		return SoundPath.getSoundOptions(sounds);
	}
	
	public boolean isMuted(final String clipName)
	{
		return m_mutedClips.contains(clipName);
	}
	
	public void setMute(final String clipName, final boolean value)
	{
		// we want to avoid unnecessary calls to preferences
		final boolean isCurrentCorrect = m_mutedClips.contains(clipName) == value;
		if (isCurrentCorrect)
			return;
		if (value == true)
			m_mutedClips.add(clipName);
		else
			m_mutedClips.remove(clipName);
		putSoundInPreferences(clipName, value);
	}
	
	/**
	 * 
	 * @param clipName
	 *            String - the file name of the clip
	 * @param subFolder
	 *            String - the name of the player, or null
	 */
	static public void play(final String clipName, final String subFolder)
	{
		getInstance().playClip(clipName, subFolder);
	}
	
	/**
	 * 
	 * @param clipName
	 *            String - the file name of the clip
	 */
	private void playClip(final String clipName, final String subFolder)
	{
		// run in a new thread, so that we do not delay the game
		final Runnable loadSounds = new Runnable()
		{
			public void run()
			{
				final Clip clip = loadClip(clipName, subFolder, false);
				if (clip != null)
				{
					clip.setFramePosition(0);
					clip.loop(0);
				}
			}
		};
		(new Thread(loadSounds, "Triplea sound loader for " + clipName)).start();
	}
	
	/**
	 * To reduce the delay when the clip is first played, we can preload clips here.
	 * 
	 * @param clipName
	 *            name of the clip
	 */
	public void preLoadClip(final String clipName)
	{
		loadClip(clipName, null, true);
		for (final String sub : m_subFolders)
		{
			loadClip(clipName, sub, true);
		}
	}
	
	private Clip loadClip(final String clipName, final String subFolder, final boolean parseThenTestOnly)
	{
		if (m_beSilent || isMuted(clipName))
			return null;
		final Clip clip = loadClipPath(clipName + (subFolder == null ? "" : ("_" + subFolder)), (subFolder != null), parseThenTestOnly);
		if (clip == null)
			return loadClipPath(clipName, false, parseThenTestOnly);
		return clip;
	}
	
	private Clip loadClipPath(final String pathName, final boolean subFolder, final boolean parseThenTestOnly)
	{
		if (!m_sounds.containsKey(pathName))
		{
			// parse sounds for the first time
			parseClipPaths(pathName, subFolder);
		}
		final List<File> availableSounds = m_sounds.get(pathName);
		if (parseThenTestOnly || availableSounds == null || availableSounds.isEmpty())
			return null;
		Collections.shuffle(availableSounds); // we want to pick a random sound from this folder, as users don't like hearing the same ones over and over again
		final File clipFile = availableSounds.get(0);
		return m_clipCache.get(clipFile);
	}
	
	/**
	 * The user may or may not have a sounds.properties file. If they do not, we should have a default folder (ww2) that we use for sounds.
	 * Because we do not want a lot of duplicate sound files, we also have a "generic" sound folder.
	 * If a sound can not be found for a soundpath using the sounds.properties or default folder, then we try to find one in the generic folder.
	 * The sounds.properties file can specify all the sounds to use for a specific sound path (multiple per path).
	 * If there is no key for that path, we try by the default way. <br>
	 * <br>
	 * Example sounds.properties keys:<br>
	 * Sound.Default.Folder=ww2<br>
	 * battle_aa_miss=ww2/battle_aa_miss/battle_aa_miss_01_aa_artillery_and_flyby.wav;ww2/battle_aa_miss/battle_aa_miss_02_just_aa_artillery.wav<br>
	 * phase_purchase_Germans=phase_purchase_Germans/game_start_Germans_01_anthem.wav
	 * 
	 * @param pathName
	 * @param subFolder
	 * @return
	 */
	private void parseClipPaths(final String pathName, final boolean subFolder)
	{
		String resourcePath = SoundProperties.getInstance(m_resourceLoader).getProperty(pathName);
		if (resourcePath == null)
			resourcePath = SoundProperties.getInstance(m_resourceLoader).getDefaultEraFolder() + "/" + pathName;
		// URL uses "/", not File.separator or "\"
		// resourcePath = resourcePath.replace('/', File.separatorChar);
		// resourcePath = resourcePath.replace('\\', File.separatorChar);
		resourcePath = resourcePath.replace('\\', '/');
		final List<File> availableSounds = new ArrayList<File>();
		for (final String path : resourcePath.split(";"))
		{
			availableSounds.addAll(createAndAddClips(ASSETS_SOUNDS_FOLDER + "/" + path));
		}
		if (availableSounds.isEmpty())
		{
			final String genericPath = SoundProperties.GENERIC_FOLDER + "/" + pathName;
			availableSounds.addAll(createAndAddClips(ASSETS_SOUNDS_FOLDER + "/" + genericPath));
		}
		m_sounds.put(pathName, availableSounds);
	}
	
	/**
	 * 
	 * @param resourceAndPathURL
	 *            (URL uses '/', not File.separator or '\')
	 * @return
	 */
	private List<File> createAndAddClips(final String resourceAndPathURL)
	{
		final List<File> availableSounds = new ArrayList<File>();
		final URL thisSoundURL = m_resourceLoader.getResource(resourceAndPathURL);
		if (thisSoundURL == null)
		{
			// if (!subFolder)
			// System.out.println("No Sounds Found For: " + path);
			return availableSounds;
		}
		URI thisSoundURI;
		File thisSound;
		try
		{
			thisSoundURI = thisSoundURL.toURI();
			thisSound = new File(thisSoundURI);
		} catch (final URISyntaxException e)
		{
			thisSound = new File(thisSoundURL.getPath());
		}
		if (thisSound == null || !thisSound.exists())
		{
			return availableSounds;
		}
		if (!thisSound.isDirectory())
		{
			if (!(thisSound.getName().endsWith(".wav") || thisSound.getName().endsWith(".au") || thisSound.getName().endsWith(".aiff") || thisSound.getName().endsWith(".midi")))
				return availableSounds;
			if (testClipSuccessful(thisSound))
				availableSounds.add(thisSound);
		}
		else
		{
			for (final File sound : thisSound.listFiles())
			{
				if (!(sound.getName().endsWith(".wav") || sound.getName().endsWith(".au") || sound.getName().endsWith(".aiff") || sound.getName().endsWith(".midi")))
					continue;
				if (testClipSuccessful(sound))
					availableSounds.add(sound);
			}
		}
		return availableSounds;
	}
	
	static synchronized Clip createClip(final File clipFile, final boolean testOnly)
	{
		try
		{
			final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(clipFile);
			final AudioFormat format = audioInputStream.getFormat();
			final DataLine.Info info = new DataLine.Info(Clip.class, format);
			final Clip clip = (Clip) AudioSystem.getLine(info);
			clip.open(audioInputStream);
			if (!testOnly)
				return clip;
			clip.close();
			return null;
		}
		// these can happen if the sound isnt configured, its not that bad.
		catch (final LineUnavailableException e)
		{
			e.printStackTrace(System.out);
		} catch (final IOException e)
		{
			e.printStackTrace(System.out);
		} catch (final UnsupportedAudioFileException e)
		{
			e.printStackTrace(System.out);
		} catch (final RuntimeException re)
		{
			re.printStackTrace(System.out);
		}
		return null;
	}
	
	static synchronized boolean testClipSuccessful(final File clipFile)
	{
		Clip clip = null;
		boolean successful = false;
		try
		{
			final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(clipFile);
			final AudioFormat format = audioInputStream.getFormat();
			final DataLine.Info info = new DataLine.Info(Clip.class, format);
			clip = (Clip) AudioSystem.getLine(info);
			clip.open(audioInputStream);
			successful = true;
		}
		// these can happen if the sound isnt configured, its not that bad.
		catch (final LineUnavailableException e)
		{
			e.printStackTrace(System.out);
		} catch (final IOException e)
		{
			e.printStackTrace(System.out);
		} catch (final UnsupportedAudioFileException e)
		{
			e.printStackTrace(System.out);
		} catch (final RuntimeException re)
		{
			re.printStackTrace(System.out);
		} finally
		{
			if (clip != null)
			{
				clip.close();
				if (successful)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Simple stupid test to see if it works (and to see if our cache stays at or below its max), and to make sure there are no memory leaks.
	 * 
	 * @param args
	 */
	public static void main(final String[] args)
	{
		getInstance();
		final File root = new File(GameRunner.getRootFolder(), "assets" + File.separator + "sounds");
		for (final File folder : root.listFiles())
		{
			if (!(folder.getName().equals("ww2") || folder.getName().equals("preindustrial") || folder.getName().equals("classical")))
				continue;
			for (final File file : folder.listFiles())
			{
				if (file.getName().indexOf("svn") != -1)
					continue;
				final String name = folder.getName() + "/" + file.getName();
				final List<File> availableSounds = new ArrayList<File>();
				availableSounds.addAll(getInstance().createAndAddClips(ASSETS_SOUNDS_FOLDER + "/" + name));
				getInstance().m_sounds.put(name, availableSounds);
			}
		}
		while (true)
		{
			for (final File folder : root.listFiles())
			{
				if (!(folder.getName().equals("ww2") || folder.getName().equals("preindustrial") || folder.getName().equals("classical")))
					continue;
				for (final File file : folder.listFiles())
				{
					if (file.getName().indexOf("svn") != -1)
						continue;
					play(folder.getName() + "/" + file.getName(), null);
					try
					{
						Thread.sleep(4000);
					} catch (final InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}
}


class ClipCache
{
	private final HashMap<File, Clip> m_clipMap = new HashMap<File, Clip>();
	private final List<File> m_cacheOrder = new ArrayList<File>();
	private final int MAXSIZE;
	
	ClipCache(final int max)
	{
		if (max < 1)
			throw new IllegalArgumentException("ClipCache max must be at least 1");
		MAXSIZE = max;
	}
	
	public synchronized Clip get(final File file)
	{
		Clip clip = m_clipMap.get(file);
		if (clip != null)
		{
			m_cacheOrder.remove(file);
			m_cacheOrder.add(file);
			return clip;
		}
		if (m_clipMap.size() >= MAXSIZE)
		{
			final File leastPlayed = m_cacheOrder.get(0);
			// System.out.println("Removing " + leastPlayed + " and adding " + file);
			final Clip leastClip = m_clipMap.remove(leastPlayed);
			leastClip.stop();
			leastClip.flush();
			leastClip.close();
			m_cacheOrder.remove(leastPlayed);
		}
		clip = ClipPlayer.createClip(file, false);
		m_clipMap.put(file, clip);
		m_cacheOrder.add(file);
		return clip;
	}
	
	public synchronized void removeAll()
	{
		for (final Clip clip : m_clipMap.values())
		{
			clip.stop();
			clip.flush();
			clip.close();
		}
		m_clipMap.clear();
		m_cacheOrder.clear();
	}
}