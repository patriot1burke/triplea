package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.framework.startup.launcher.ILauncher;

import java.util.List;
import java.util.Observer;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Action;
import javax.swing.JPanel;

public abstract class SetupPanel extends JPanel implements ISetupPanel
{
	private static final long serialVersionUID = 4001323470187210773L;
	private final List<Observer> m_listeners = new CopyOnWriteArrayList<Observer>();
	
	public void addObserver(final Observer observer)
	{
		m_listeners.add(observer);
	}
	
	public void removeObserver(final Observer observer)
	{
		m_listeners.add(observer);
	}
	
	public void notifyObservers()
	{
		for (final Observer observer : m_listeners)
		{
			observer.update(null, null);
		}
	}
	
	/**
	 * Subclasses that have chat override this.
	 */
	public IChatPanel getChatPanel()
	{
		return null;
	}
	
	/**
	 * Cleanup should occur here that occurs when we cancel
	 */
	public abstract void cancel();
	
	/**
	 * Can we start the game?
	 */
	public abstract boolean canGameStart();
	
	public abstract void setWidgetActivation();
	
	public void preStartGame()
	{
	}
	
	public void postStartGame()
	{
	}
	
	public ILauncher getLauncher()
	{
		throw new IllegalStateException("NOt implemented");
	}
	
	public List<Action> getUserActions()
	{
		return null;
	}
}
