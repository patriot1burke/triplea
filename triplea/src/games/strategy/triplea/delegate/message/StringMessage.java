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
 * StringMessage.java
 *
 * Created on November 7, 2001, 11:24 AM
 */

package games.strategy.triplea.delegate.message;

import games.strategy.engine.data.PlayerID;

/**
 *
 * 
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class StringMessage implements games.strategy.engine.message.Message
{
	private final String m_message;
	private final boolean m_error;
	//dont send this message if the given player is playing locally
	private PlayerID m_ignore;
	
	/** Creates new ErrorMessage */
    public StringMessage(String message) 
	{
		this(message, false);
    }
	
	public StringMessage(String message, boolean error)
	{
		m_message = message;
		m_error = error;
	}
	
	/**
	 * If the player is playing locally then dont send the 
	 * message.
	 */
	public void setIgnore(PlayerID id)
	{
		m_ignore = id;
	}
	
	/**
	 * If the player is playing locally then dont send the message.
	 */
	public PlayerID getIgnore()
	{
		return m_ignore;
	}

	public boolean isError()
	{
		return m_error;
	}
	
	public String getMessage()
	{
		return m_message;
	}
	
	public String toString()
	{
		return "String message.  Value:" + m_message + " error:" + m_error;
	}
}
