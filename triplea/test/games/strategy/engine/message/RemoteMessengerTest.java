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
package games.strategy.engine.message;

import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.MacFinder;
import games.strategy.net.ServerMessenger;
import games.strategy.test.TestUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

/**
 * @author Sean Bridges
 */
public class RemoteMessengerTest extends TestCase
{
	private int SERVER_PORT = -1;
	private IMessenger m_messenger;
	private RemoteMessenger m_remoteMessenger;
	private UnifiedMessengerHub m_hub;
	
	/*
	 * @see TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		// simple set up for non networked testing
		m_messenger = new DummyMessenger();
		m_remoteMessenger = new RemoteMessenger(new UnifiedMessenger(m_messenger));
		SERVER_PORT = TestUtil.getUniquePort();
	}
	
	/*
	 * @see TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		m_messenger = null;
		m_remoteMessenger = null;
	}
	
	public void testRegisterUnregister()
	{
		final TestRemote testRemote = new TestRemote();
		final RemoteName test = new RemoteName(ITestRemote.class, "test");
		m_remoteMessenger.registerRemote(testRemote, test);
		assertTrue(m_remoteMessenger.hasLocalImplementor(test));
		m_remoteMessenger.unregisterRemote(test);
		assertFalse(m_remoteMessenger.hasLocalImplementor(test));
	}
	
	public void testMethodCall()
	{
		final TestRemote testRemote = new TestRemote();
		final RemoteName test = new RemoteName(ITestRemote.class, "test");
		m_remoteMessenger.registerRemote(testRemote, test);
		final ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote(test);
		assertEquals(2, remote.increment(1));
		assertEquals(testRemote.getLastSenderNode(), m_messenger.getLocalNode());
	}
	
	public void testExceptionThrownWhenUnregisteredRemote()
	{
		final TestRemote testRemote = new TestRemote();
		final RemoteName test = new RemoteName(ITestRemote.class, "test");
		m_remoteMessenger.registerRemote(testRemote, test);
		final ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote(test);
		m_remoteMessenger.unregisterRemote("test");
		try
		{
			remote.increment(1);
			fail("No exception thrown");
		} catch (final RemoteNotFoundException rme)
		{
			// this is what we expect
		}
	}
	
	public void testNoRemote()
	{
		final RemoteName test = new RemoteName(ITestRemote.class, "test");
		try
		{
			m_remoteMessenger.getRemote(test);
			final ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote(test);
			remote.testVoid();
			fail("No exception thrown");
		} catch (final RemoteNotFoundException rme)
		{
			// this is what we expect
		}
	}
	
	public void testVoidMethodCall()
	{
		final TestRemote testRemote = new TestRemote();
		final RemoteName test = new RemoteName(ITestRemote.class, "test");
		m_remoteMessenger.registerRemote(testRemote, test);
		final ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote(test);
		remote.testVoid();
	}
	
	public void testException() throws Exception
	{
		final TestRemote testRemote = new TestRemote();
		final RemoteName test = new RemoteName(ITestRemote.class, "test");
		m_remoteMessenger.registerRemote(testRemote, test);
		final ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote(test);
		try
		{
			remote.throwException();
		} catch (final Exception e)
		{
			// this is what we want
			if (e.getMessage().equals(TestRemote.EXCEPTION_STRING))
				return;
			throw e;
		}
		fail("No exception thrown");
	}
	
	public void testRemoteCall() throws Exception
	{
		final RemoteName test = new RemoteName(ITestRemote.class, "test");
		ServerMessenger server = null;
		ClientMessenger client = null;
		try
		{
			server = new ServerMessenger("server", SERVER_PORT);
			server.setAcceptNewConnections(true);
			final String mac = MacFinder.GetHashedMacAddress();
			client = new ClientMessenger("localhost", SERVER_PORT, "client", mac);
			final UnifiedMessenger serverUM = new UnifiedMessenger(server);
			m_hub = serverUM.getHub();
			final RemoteMessenger serverRM = new RemoteMessenger(serverUM);
			final RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger(client));
			// register it on the server
			final TestRemote testRemote = new TestRemote();
			serverRM.registerRemote(testRemote, test);
			// since the registration must go over a socket
			// and through a couple threads, wait for the
			// client to get it
			int waitCount = 0;
			while (!m_hub.hasImplementors(test.getName()) && waitCount < 20)
			{
				waitCount++;
				Thread.sleep(50);
			}
			// call it on the client
			final int rVal = ((ITestRemote) clientRM.getRemote(test)).increment(1);
			assertEquals(2, rVal);
			assertEquals(testRemote.getLastSenderNode(), client.getLocalNode());
		} finally
		{
			if (server != null)
				server.shutDown();
			if (client != null)
				client.shutDown();
		}
	}
	
	public void testRemoteCall2() throws Exception
	{
		final RemoteName test = new RemoteName(ITestRemote.class, "test");
		ServerMessenger server = null;
		ClientMessenger client = null;
		try
		{
			server = new ServerMessenger("server", SERVER_PORT);
			server.setAcceptNewConnections(true);
			final String mac = MacFinder.GetHashedMacAddress();
			client = new ClientMessenger("localhost", SERVER_PORT, "client", mac);
			final RemoteMessenger serverRM = new RemoteMessenger(new UnifiedMessenger(server));
			final TestRemote testRemote = new TestRemote();
			serverRM.registerRemote(testRemote, test);
			final RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger(client));
			// call it on the client
			// should be no need to wait since the constructor should not
			// reutrn until the initial state of the messenger is good
			final int rVal = ((ITestRemote) clientRM.getRemote(test)).increment(1);
			assertEquals(2, rVal);
			assertEquals(testRemote.getLastSenderNode(), client.getLocalNode());
		} finally
		{
			if (server != null)
				server.shutDown();
			if (client != null)
				client.shutDown();
		}
	}
	
	private void sleep(final int ms)
	{
		try
		{
			Thread.sleep(ms);
		} catch (final InterruptedException e)
		{
		}
	}
	
	public void testShutDownClient() throws Exception
	{
		// when the client shutdown, remotes created
		// on the client should not be visible on server
		final RemoteName test = new RemoteName(ITestRemote.class, "test");
		ServerMessenger server = null;
		ClientMessenger client = null;
		try
		{
			server = new ServerMessenger("server", SERVER_PORT);
			server.setAcceptNewConnections(true);
			final String mac = MacFinder.GetHashedMacAddress();
			client = new ClientMessenger("localhost", SERVER_PORT, "client", mac);
			final UnifiedMessenger serverUM = new UnifiedMessenger(server);
			final RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger(client));
			clientRM.registerRemote(new TestRemote(), test);
			serverUM.getHub().waitForNodesToImplement(test.getName(), 200);
			assertTrue(serverUM.getHub().hasImplementors(test.getName()));
			client.shutDown();
			sleep(200);
			assertTrue(!serverUM.getHub().hasImplementors(test.getName()));
		} finally
		{
			if (server != null)
				server.shutDown();
			if (client != null)
				client.shutDown();
		}
	}
	
	public void testMethodReturnsOnWait() throws Exception
	{
		// when the client shutdown, remotes created
		// on the client should not be visible on server
		final RemoteName test = new RemoteName(IFoo.class, "test");
		ServerMessenger server = null;
		ClientMessenger client = null;
		try
		{
			server = new ServerMessenger("server", SERVER_PORT);
			server.setAcceptNewConnections(true);
			final String mac = MacFinder.GetHashedMacAddress();
			client = new ClientMessenger("localhost", SERVER_PORT, "client", mac);
			final UnifiedMessenger serverUM = new UnifiedMessenger(server);
			final RemoteMessenger serverRM = new RemoteMessenger(serverUM);
			final RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger(client));
			final Object lock = new Object();
			final AtomicBoolean started = new AtomicBoolean(false);
			final IFoo foo = new IFoo()
			{
				public void foo()
				{
					synchronized (lock)
					{
						try
						{
							started.set(true);
							lock.wait();
						} catch (final InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
			};
			clientRM.registerRemote(foo, test);
			serverUM.getHub().waitForNodesToImplement(test.getName(), 200);
			assertTrue(serverUM.getHub().hasImplementors(test.getName()));
			final AtomicReference<ConnectionLostException> rme = new AtomicReference<ConnectionLostException>(null);
			final Runnable r = new Runnable()
			{
				public void run()
				{
					try
					{
						final IFoo remoteFoo = (IFoo) serverRM.getRemote(test);
						remoteFoo.foo();
					} catch (final ConnectionLostException e)
					{
						rme.set(e);
					}
				}
			};
			final Thread t = new Thread(r);
			t.start();
			// wait for the thread to start
			while (started.get() == false)
				sleep(1);
			sleep(20);
			// TODO: we are getting a RemoteNotFoundException because the client is disconnecting before the invoke goes out completely
			// Perhaps this situation should be changed to a ConnectionLostException or something else?
			client.shutDown();
			// when the client shutdowns, this should wake up.
			// and an error should be thrown
			// give the thread a chance to execute
			t.join(200);
			synchronized (lock)
			{
				lock.notifyAll();
			}
			assertNotNull(rme.get());
		} finally
		{
			if (server != null)
				server.shutDown();
			if (client != null)
				client.shutDown();
		}
	}
}


interface IFoo extends IRemote
{
	public void foo();
}


interface ITestRemote extends IRemote
{
	public int increment(int testVal);
	
	public void testVoid();
	
	public void throwException() throws Exception;
}


class TestRemote implements ITestRemote
{
	public static final String EXCEPTION_STRING = "AND GO";
	private INode m_senderNode;
	
	public int increment(final int testVal)
	{
		m_senderNode = MessageContext.getSender();
		return testVal + 1;
	}
	
	public void testVoid()
	{
		m_senderNode = MessageContext.getSender();
	}
	
	public void throwException() throws Exception
	{
		throw new Exception(EXCEPTION_STRING);
	}
	
	public INode getLastSenderNode()
	{
		return m_senderNode;
	}
}
