/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * GameData.java
 *
 * Created on October 14, 2001, 7:11 AM
 */

package games.strategy.engine.data;

import java.util.*;

import java.util.concurrent.locks.*;

import games.strategy.engine.data.events.*;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.*;
import games.strategy.util.*;
import games.strategy.engine.history.*;

/**
 *
 * Central place to find all the information for a running game.
 * Using this object you can find the territores, connections, production rules,
 * unit types...<p>
 * 
 * Threading.  The game data, and all parts of the game data (such as Territories, Players, Units...) are 
 * protected by a read/write lock.  If you are reading the game data, you should read while you
 * have the read lock as below. <p>
 * 
 * <code>
 * data.acquireReadLock();
 * try
 * {
 *   //read data here
 * }
 * finally
 * {
 *   data.releaseReadLock();
 * }
 * </code>
 * 
 * The exception is delegates within a start(), end() or any method called from an IGamePlayer through
 * the delgates remote interface.  The delegate will have a read lock for the duration of those methods.<p>
 * 
 * Non engine code must NOT acquire the games writeLock().  All changes to game Data must be made through a
 * DelegateBridge or through a History object.<p>
 * 
 * 
 * 
 * 
 * 
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class GameData implements java.io.Serializable
{
    private final ReadWriteLock m_readWriteLock = new ReentrantReadWriteLock();

    private transient boolean m_forceInSwingEventThread = false;
    
    private String m_gameName;
	private Version m_gameVersion;

	private transient ListenerList<TerritoryListener> m_territoryListeners = new ListenerList<TerritoryListener>();
	private transient ListenerList<GameDataChangeListener> m_dataChangeListeners = new ListenerList<GameDataChangeListener>();

	private final AllianceTracker m_alliances = new AllianceTracker(this);
	private final DelegateList m_delegateList;
	private final GameMap m_map = new GameMap(this);
	private final PlayerList m_playerList = new PlayerList(this);
	private final ProductionFrontierList m_productionFrontierList = new ProductionFrontierList(this);
	private final ProductionRuleList m_productionRuleList = new ProductionRuleList(this);
	private final ResourceList m_resourceList = new ResourceList(this);
	private final GameSequence m_sequence = new GameSequence(this);
	private final UnitTypeList m_unitTypeList = new UnitTypeList(this);
	private final GameProperties m_properties = new GameProperties(this);
	private final UnitsList m_unitsList = new UnitsList();

	private IGameLoader m_loader;
	private final History m_gameHistory = new History(this);

	/** Creates new GameData */
	public GameData()
	{
		m_delegateList = new DelegateList(this);
	}

    /**
     * Return the GameMap.  The game map allows you to list the territories in the game, and 
     * to see which territory is connected to which.
     * 
     * @return the map for this game.
     */
	public GameMap getMap()
	{
		return m_map;
	}

    /**
     * 
     * @return a collection of all units in the game
     */
	UnitsList getUnits()
	{
	    return m_unitsList;
	}

    /**
     * Get the list of Players in the game.
     */
	public PlayerList getPlayerList()
	{
		return m_playerList;
	}

    /**
     * Get the list of resources available in the game.
     */
	public ResourceList getResourceList()
	{
		return m_resourceList;
	}

    /**
     * Get the list of production Frontiers for this game.
     */
	public ProductionFrontierList getProductionFrontierList()
	{
		return m_productionFrontierList;
	}

    /**
     * Get the list of Production Rules for the game.
     */
	public ProductionRuleList getProductionRuleList()
	{
		return m_productionRuleList;
	}

    /**
     * Get the Alliance Tracker for the game.
     */
	public AllianceTracker getAllianceTracker()
	{
		return m_alliances;
	}
    
    /**
     * Should we throw an error if changes to this game data are made outside of the swing
     * event thread.
     */
    public boolean areChangesOnlyInSwingEventThread()
    {
        return m_forceInSwingEventThread;
    }

    /**
     * If set to true, then we will throw an error when the game data is changed outside
     * the swing event thread.
     */
    public void forceChangesOnlyInSwingEventThread()
    {
        m_forceInSwingEventThread = true;
    }

	public GameSequence getSequence()
	{
		return m_sequence;
	}

	public UnitTypeList getUnitTypeList()
	{
		return m_unitTypeList;
	}

	public DelegateList getDelegateList()
	{
		return m_delegateList;
	}

    /**
     * 
     */
	public UnitHolder getUnitHolder(String name, String type)
	{
		if(type.equals(UnitHolder.PLAYER))
            return m_playerList.getPlayerID(name);
        else if(type.equals(UnitHolder.TERRITORY))
			return m_map.getTerritory(name);
        else
            throw new IllegalStateException("Invalid type:" + type);
		
	}

	public GameProperties getProperties()
	{
		return m_properties;
	}

	public void addTerritoryListener(TerritoryListener listener)
	{
		m_territoryListeners.add(listener);
	}

	public void removeTerritoryListener(TerritoryListener listener)
	{
		m_territoryListeners.remove(listener);
	}

	public void addDataChangeListener(GameDataChangeListener listener)
	{
		m_dataChangeListeners.add(listener);
	}

	public void removeDataChangeListener(GameDataChangeListener listener)
	{
		m_dataChangeListeners.remove(listener);
	}


	void notifyTerritoryUnitsChanged(Territory t)
	{
		Iterator<TerritoryListener> iter = m_territoryListeners.iterator();
		while(iter.hasNext())
		{
			TerritoryListener listener = iter.next();
			listener.unitsChanged(t);
		}
	}

	void notifyTerritoryOwnerChanged(Territory t)
	{
		Iterator<TerritoryListener> iter = m_territoryListeners.iterator();
		while(iter.hasNext())
		{
			TerritoryListener listener = iter.next();
			listener.ownerChanged(t);
		}
	}

	void notifyGameDataChanged(Change aChange)
	{
		Iterator<GameDataChangeListener> iter = m_dataChangeListeners.iterator();
		while(iter.hasNext())
		{
			GameDataChangeListener listener = iter.next();
			listener.gameDataChanged(aChange);
		}
	}

	public IGameLoader getGameLoader()
	{
		return m_loader;
	}

	void setGameLoader(IGameLoader loader)
	{
		m_loader = loader;
	}

	void setGameVersion(Version version)
	{
		m_gameVersion = version;
	}

	public Version getGameVersion()
	{
		return m_gameVersion;
	}

	void setGameName(String gameName)
	{
		m_gameName = gameName;
	}

	public String getGameName()
	{
		return m_gameName;
	}

    public History getHistory()
    {
        return m_gameHistory;
    }

    /**
     * Not to be called by mere mortals.
     */
	public void postDeSerialize()
	{
		m_territoryListeners = new ListenerList<TerritoryListener>();
		m_dataChangeListeners = new ListenerList<GameDataChangeListener>();
		
	}
	
	/**
	 * No changes to the game data should be made unless this lock is held.
	 * calls to acquire lock will block if the lock is held, and will be held 
	 * until the release method is called
	 * 
	 */
	public void aquireReadLock()
	{
        //this can happen in very odd cirumcstances while deserializing
        if(m_readWriteLock == null)
            return;
        
        m_readWriteLock.readLock().lock();
	}
	
	
	public void releaseReadLock()
	{
        //this can happen in very odd cirumcstances while deserializing
        if(m_readWriteLock == null)
            return;
        
        m_readWriteLock.readLock().unlock();
	}


    /**
     * No changes to the game data should be made unless this lock is held.
     * calls to acquire lock will block if the lock is held, and will be held 
     * until the release method is called
     * 
     */
    public void aquireWriteLock()
    {   
        //this can happen in very odd cirumcstances while deserializing
        if(m_readWriteLock == null)
            return;
        m_readWriteLock.writeLock().lock();
    }
    
    
    public void releaseWriteLock()
    {
        //this can happen in very odd cirumcstances while deserializing
        if(m_readWriteLock == null)
            return;
        
        m_readWriteLock.writeLock().unlock();
    }

    
	
}
