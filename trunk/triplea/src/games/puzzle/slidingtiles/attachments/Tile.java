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
package games.puzzle.slidingtiles.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.annotations.GameProperty;

/**
 * Represents a sliding tile in a sliding tile game.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class Tile extends DefaultAttachment
{
	private static final long serialVersionUID = 129074481008059616L;
	private int m_value = 0;
	
	// private Rectangle m_location;
	/**
	 * Construct a new tile with no value.
	 */
	public Tile(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	/**
	 * Construct a new tile with the specified value.
	 */
	public Tile(final int value)
	{
		// workaround TODO: check whether the use of this constructor is really necessary
		this("tile", null, null);
		this.m_value = value;
	}
	
	/**
	 * Get the value of this tile.
	 * 
	 * @return the value of this tile
	 */
	public int getValue()
	{
		return m_value;
	}
	
	/**
	 * Set the value of this tile.
	 * 
	 * @param value
	 *            String representation of the int value to store in this tile
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setValue(final String value)
	{
		this.m_value = getInt(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setValue(final Integer value)
	{
		this.m_value = value;
	}
	
	public void resetValue()
	{
		m_value = 0;
	}
	
	/*
	public void setLocation(Rectangle location)
	{
	    m_location = location;
	}
	
	public Rectangle getLocation() 
	{
	    return m_location;
	}
	*/
	@Override
	public int hashCode()
	{
		return m_value;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
