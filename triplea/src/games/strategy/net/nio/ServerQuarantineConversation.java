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
package games.strategy.net.nio;

import games.strategy.net.ILoginValidator;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;
import games.strategy.net.ServerMessenger;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.nio.ch.SocketAdaptor;

public class ServerQuarantineConversation extends QuarantineConversation
{
	/**
	 * Communication sequence
	 * 1) server reads client name
	 * 2) server sends challenge (or null if no challenge is to be made)
	 * 3) server reads response (or null if no challenge)
	 * 4) server send null then client name and node info on success, or an error message if there is an error
	 * 5) if the client reads an error message, the client sends an acknowledgment (we need to make sur the client gets the message before closing the socket)
	 */
	private static final Logger s_logger = Logger.getLogger(ServerQuarantineConversation.class.getName());
	
	
	private enum STEP
	{
		READ_NAME, READ_MAC, CHALLENGE, ACK_ERROR
	};
	
	private final ILoginValidator m_validator;
	private final SocketChannel m_channel;
	private final NIOSocket m_socket;
	private STEP m_step = STEP.READ_NAME;
	private String m_remoteName;
	private String m_remoteMac;
	private Map<String, String> challenge;
	private final ServerMessenger m_serverMessenger;
	
	public ServerQuarantineConversation(final ILoginValidator validator, final SocketChannel channel, final NIOSocket socket, final ServerMessenger serverMessenger)
	{
		m_validator = validator;
		m_socket = socket;
		m_channel = channel;
		m_serverMessenger = serverMessenger;
	}
	
	public String getRemoteName()
	{
		return m_remoteName;
	}
	
	public String getRemoteMac()
	{
		return m_remoteMac;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public ACTION message(final Object o)
	{
		try
		{
			switch (m_step)
			{
				case READ_NAME:
					// read name, send challent
					m_remoteName = (String) o;
					if (s_logger.isLoggable(Level.FINER))
					{
						s_logger.log(Level.FINER, "read name:" + m_remoteName);
					}
					m_step = STEP.READ_MAC;
					return ACTION.NONE;
				case READ_MAC:
					// read name, send challent
					m_remoteMac = (String) o;
					if (s_logger.isLoggable(Level.FINER))
					{
						s_logger.log(Level.FINER, "read mac:" + m_remoteMac);
					}
					if (m_validator != null)
						challenge = m_validator.getChallengeProperties(m_remoteName, m_channel.socket().getRemoteSocketAddress());
					if (s_logger.isLoggable(Level.FINER))
					{
						s_logger.log(Level.FINER, "writing challenge:" + challenge);
					}
					send((Serializable) challenge);
					m_step = STEP.CHALLENGE;
					return ACTION.NONE;
				case CHALLENGE:
					final Map<String, String> response = (Map) o;
					if (s_logger.isLoggable(Level.FINER))
					{
						s_logger.log(Level.FINER, "read challenge response:" + response);
					}
					if (m_validator != null)
					{
						final String error = m_validator.verifyConnection(challenge, response, m_remoteName, m_remoteMac, m_channel.socket().getRemoteSocketAddress());
						if (s_logger.isLoggable(Level.FINER))
						{
							s_logger.log(Level.FINER, "error:" + error);
						}
						send(error);
						if (error != null)
						{
							m_step = STEP.ACK_ERROR;
							return ACTION.NONE;
						}
					}
					else
					{
						send(null);
					}
					// get a unique name
					m_remoteName = m_serverMessenger.getUniqueName(m_remoteName);
					if (s_logger.isLoggable(Level.FINER))
					{
						s_logger.log(Level.FINER, "Sending name:" + m_remoteName);
					}
					// send the node its name and our name
					send(new String[] { m_remoteName, m_serverMessenger.getLocalNode().getName() });
					// send the node its and our address as we see it
					send(new InetSocketAddress[] { (InetSocketAddress) m_channel.socket().getRemoteSocketAddress(), m_serverMessenger.getLocalNode().getSocketAddress() });
					// Login succeeded, so notify the ServerMessenger about the login with the name, mac, etc.
					m_serverMessenger.NotifyPlayerLogin(m_remoteName, ((SocketAdaptor) m_channel.socket()).getInetAddress().getHostAddress(), m_remoteMac);
					// We are good
					return ACTION.UNQUARANTINE;
				case ACK_ERROR:
					return ACTION.TERMINATE;
				default:
					throw new IllegalStateException("Invalid state");
			}
		} catch (final Throwable t)
		{
			s_logger.log(Level.SEVERE, "Error with connection", t);
			return ACTION.TERMINATE;
		}
	}
	
	private void send(final Serializable object)
	{
		// this messenger is quarantined, so to and from dont matter
		final MessageHeader header = new MessageHeader(Node.NULL_NODE, Node.NULL_NODE, object);
		m_socket.send(m_channel, header);
	}
	
	@Override
	public void close()
	{
	}
}
