package games.strategy.engine.delegate;

import games.strategy.engine.GameOverException;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.message.MessengerException;
import games.strategy.triplea.util.WrappedInvocationHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages when delegates are allowed to execute.
 * <p>
 * 
 * When saving a game, we want to ensure that no delegate is executing, otherwise the delegate could modify the state of the game while the game is being saved, resulting in an invalid save game.
 * <p>
 * 
 * This class effectivly keeps a count of how many threads are executing in the delegates, and provides a way of blocking further threads from starting execution in a delegate.
 * <p>
 * 
 * 
 * @author sgb
 */
public class DelegateExecutionManager
{
	private final Logger sm_logger = Logger.getLogger(DelegateExecutionManager.class.getName());
	/*
	 * Delegate execution can be thought of as a read/write lock.
	 * Many delegates can be executing at one time (to execute you acquire the read lock), but
	 * only 1 block can be held (the block is equivalent to the read lock).
	 * 
	 */
	private final ReentrantReadWriteLock m_readWriteLock = new ReentrantReadWriteLock();
	private final ThreadLocal<Boolean> m_currentThreadHasReadLock = new ThreadLocal<Boolean>();
	private volatile boolean m_isGameOver = false;
	
	public void setGameOver()
	{
		m_isGameOver = true;
	}
	
	/**
	 * 
	 * When this method returns true, threads will not be able to enter delegates until
	 * a call to resumeDelegateExecution is made.
	 * <p>
	 * 
	 * When delegateExecution is blocked, it also blocks subsequent cals to blockDelegateExecution(...)
	 * <p>
	 * 
	 * If timeToWaitMS is > 0, we will give up trying to block delegate execution after timeTiWaitMS has elapsed.
	 * <p>
	 * 
	 * @param timeToWait
	 * @return
	 */
	public boolean blockDelegateExecution(final int timeToWaitMS) throws InterruptedException
	{
		final boolean rVal = m_readWriteLock.writeLock().tryLock(timeToWaitMS, TimeUnit.MILLISECONDS);
		if (!rVal)
		{
			if (sm_logger.isLoggable(Level.FINE))
			{
				sm_logger.fine("Could not block delegate execution. Read Lock count: " + m_readWriteLock.getReadLockCount()
							+ " Write Hold count: " + m_readWriteLock.getWriteHoldCount() + " Queue Length: " + m_readWriteLock.getQueueLength()
							+ " Current Thread Has Lock: " + m_readWriteLock.isWriteLockedByCurrentThread() + " Has Queued Threads: " + m_readWriteLock.hasQueuedThreads()
							+ " Is Write Locked: " + m_readWriteLock.isWriteLocked() + " toString: " + m_readWriteLock.toString());
			}
			else
			{
				HeadlessGameServer.log("Could not block delegate execution. Read Lock count: " + m_readWriteLock.getReadLockCount()
							+ " Write Hold count: " + m_readWriteLock.getWriteHoldCount() + " Queue Length: " + m_readWriteLock.getQueueLength()
							+ " Current Thread Has Lock: " + m_readWriteLock.isWriteLockedByCurrentThread() + " Has Queued Threads: " + m_readWriteLock.hasQueuedThreads()
							+ " Is Write Locked: " + m_readWriteLock.isWriteLocked() + " toString: " + m_readWriteLock.toString());
			}
		}
		else
		{
			if (sm_logger.isLoggable(Level.FINE))
			{
				sm_logger.fine(Thread.currentThread().getName() + " block delegate execution.");
			}
		}
		return rVal;
	}
	
	/**
	 * Allow delegate execution to resume.
	 * 
	 */
	public void resumeDelegateExecution()
	{
		if (sm_logger.isLoggable(Level.FINE))
		{
			sm_logger.fine(Thread.currentThread().getName() + " resumes delegate execution.");
		}
		m_readWriteLock.writeLock().unlock();
	}
	
	private boolean currentThreadHasReadLock()
	{
		return m_currentThreadHasReadLock.get() == Boolean.TRUE;
	}
	
	/**
	 * Used to create an object the exits delegate execution.
	 * <p>
	 * 
	 * Objects on this method will decrement the thread lock count when called, and will increment it again when execution is finished.
	 */
	public Object createOutboundImplementation(final Object implementor, final Class<?>[] interfaces)
	{
		assertGameNotOver();
		final InvocationHandler ih = new InvocationHandler()
		{
			public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
			{
				assertGameNotOver();
				final boolean threadLocks = currentThreadHasReadLock();
				if (threadLocks)
					leaveDelegateExecution();
				try
				{
					return method.invoke(implementor, args);
				} catch (final MessengerException me)
				{
					throw new GameOverException("Game Over!");
				} catch (final InvocationTargetException ite)
				{
					assertGameNotOver();
					throw ite;
				} finally
				{
					if (threadLocks)
						enterDelegateExecution();
				}
			}
		};
		return Proxy.newProxyInstance(implementor.getClass().getClassLoader(), interfaces, ih);
	}
	
	private void assertGameNotOver()
	{
		if (m_isGameOver)
			throw new GameOverException("Game Over");
	}
	
	/**
	 * Use to create an object that begins delegate execution.
	 * <p>
	 * 
	 * Objects on this method will increment the thread lock count when called, and will decrement it again when execution is finished.
	 */
	public Object createInboundImplementation(final Object implementor, final Class<?>[] interfaces)
	{
		assertGameNotOver();
		final InvocationHandler ih = new WrappedInvocationHandler(implementor)
		{
			@Override
			public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
			{
				if (super.shouldHandle(method, args))
					return super.handle(method, args);
				assertGameNotOver();
				enterDelegateExecution();
				try
				{
					return method.invoke(implementor, args);
				} catch (final InvocationTargetException ite)
				{
					assertGameNotOver();
					throw ite.getCause();
				} catch (final RuntimeException re)
				{
					assertGameNotOver();
					throw re;
				} finally
				{
					leaveDelegateExecution();
				}
			}
		};
		return Proxy.newProxyInstance(implementor.getClass().getClassLoader(), interfaces, ih);
	}
	
	public void leaveDelegateExecution()
	{
		if (sm_logger.isLoggable(Level.FINE))
		{
			sm_logger.fine(Thread.currentThread().getName() + " leaves delegate execution.");
		}
		m_readWriteLock.readLock().unlock();
		m_currentThreadHasReadLock.set(null);
	}
	
	public void enterDelegateExecution()
	{
		if (sm_logger.isLoggable(Level.FINE))
		{
			sm_logger.fine(Thread.currentThread().getName() + " enters delegate execution.");
		}
		if (currentThreadHasReadLock())
			throw new IllegalStateException("Already locked?");
		m_readWriteLock.readLock().lock();
		m_currentThreadHasReadLock.set(Boolean.TRUE);
	}
}
