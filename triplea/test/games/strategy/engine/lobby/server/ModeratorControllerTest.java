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
package games.strategy.engine.lobby.server;

import games.strategy.engine.lobby.server.userDB.BannedIpController;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.Node;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class ModeratorControllerTest extends TestCase
{
	private DummyMessenger m_messenger;
	private ModeratorController m_controller;
	private ConnectionChangeListener m_listener;
	private INode m_adminNode;
	
	@Override
	public void setUp() throws UnknownHostException
	{
		m_messenger = new DummyMessenger();
		m_controller = new ModeratorController(m_messenger, null);
		m_listener = new ConnectionChangeListener();
		m_messenger.addConnectionChangeListener(m_listener);
		final String adminName = Util.createUniqueTimeStamp();
		new DBUserController().createUser(adminName, "n@n.n", MD5Crypt.crypt(adminName), true);
		m_adminNode = new Node(adminName, InetAddress.getLocalHost(), 0);
	}
	
	public void testBoot() throws UnknownHostException
	{
		MessageContext.setSenderNodeForThread(m_adminNode);
		final INode booted = new Node("foo", InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 }), 0);
		m_controller.boot(booted);
		assertTrue(m_listener.getRemoved().contains(booted));
	}
	
	public void testBan() throws UnknownHostException
	{
		final InetAddress bannedAddress = InetAddress.getByAddress(new byte[] { (byte) 10, (byte) 10, (byte) 10, (byte) 10 });
		new BannedIpController().removeBannedIp(bannedAddress.getHostAddress());
		MessageContext.setSenderNodeForThread(m_adminNode);
		/*ServerMessenger serverMessenger = null;
		try
		{
			// ugly work-around
			serverMessenger = new ServerMessenger("dummyServer", 3010);
			serverMessenger.NotifyPlayerLogin("foo", bannedAddress.toString(), "00:00:00:00:00:11");
			final INode modNode = MessageContext.getSender();
			serverMessenger.NotifyPlayerLogin(modNode.getName(), modNode.getAddress().toString(), "00:00:00:00:00:12");
		} catch (final IOException e)
		{
			e.printStackTrace();
		}*/
		final INode booted = new Node("foo", bannedAddress, 0);
		m_controller.banIp(booted, null); // this test is failing because any kind of ban requires a mac address for the logging information, yet this node has no mac address. need to fix this somehow.
		assertTrue(new BannedIpController().isIpBanned(bannedAddress.getHostAddress()).getFirst());
	}
	
	public void testResetUserPassword() throws UnknownHostException
	{
		MessageContext.setSenderNodeForThread(m_adminNode);
		final String newPassword = MD5Crypt.crypt("" + System.currentTimeMillis());
		final String userName = Util.createUniqueTimeStamp();
		new DBUserController().createUser(userName, "n@n.n", newPassword, false);
		final INode node = new Node(userName, InetAddress.getLocalHost(), 0);
		assertNull(m_controller.setPassword(node, newPassword));
		assertTrue(new DBUserController().login(node.getName(), newPassword));
	}
	
	public void testCantResetAdminPassword() throws UnknownHostException
	{
		MessageContext.setSenderNodeForThread(m_adminNode);
		final String newPassword = MD5Crypt.crypt("" + System.currentTimeMillis());
		assertNotNull(m_controller.setPassword(m_adminNode, newPassword));
	}
	
	public void testResetUserPasswordUnknownUser() throws UnknownHostException
	{
		MessageContext.setSenderNodeForThread(m_adminNode);
		final String newPassword = MD5Crypt.crypt("" + System.currentTimeMillis());
		final INode node = new Node(Util.createUniqueTimeStamp(), InetAddress.getLocalHost(), 0);
		assertNotNull(m_controller.setPassword(node, newPassword));
	}
	
	public void testAssertAdmin() throws UnknownHostException
	{
		MessageContext.setSenderNodeForThread(m_adminNode);
		assertTrue(m_controller.isAdmin());
	}
}


class ConnectionChangeListener implements IConnectionChangeListener
{
	final List<INode> m_removed = new ArrayList<INode>();
	
	public void connectionAdded(final INode to)
	{
	}
	
	public void connectionRemoved(final INode to)
	{
		m_removed.add(to);
	}
	
	public List<INode> getRemoved()
	{
		return m_removed;
	}
}
