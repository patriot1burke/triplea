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
package games.strategy.grid.kingstable.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.annotations.GameProperty;

/**
 * Territory attachment for King's Table.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2012-07-06 21:37:33 +0800 (Fri, 06 Jul 2012) $
 */
public class TerritoryAttachment extends DefaultAttachment
{
	private static final long serialVersionUID = -2114955190688754947L;
	private boolean m_kingsSquare = false;
	private boolean m_kingsExit = false;
	
	/** Creates new TerritoryAttachment */
	public TerritoryAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setKingsSquare(final String value)
	{
		m_kingsSquare = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setKingsSquare(final Boolean value)
	{
		m_kingsSquare = value;
	}
	
	public boolean getKingsSquare()
	{
		return m_kingsSquare;
	}
	
	public void resetKingsSquare()
	{
		m_kingsSquare = false;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setKingsExit(final String value)
	{
		m_kingsExit = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setKingsExit(final Boolean value)
	{
		m_kingsExit = value;
	}
	
	public boolean getKingsExit()
	{
		return m_kingsExit;
	}
	
	public void resetKingsExit()
	{
		m_kingsExit = false;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
