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
package games.strategy.triplea.util;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;

public class UnitOwner
{
	private final UnitType m_type;
	private final PlayerID m_owner;
	
	public UnitOwner(final Unit unit)
	{
		m_type = unit.getType();
		m_owner = unit.getOwner();
	}
	
	public UnitOwner(final UnitType type, final PlayerID owner)
	{
		m_type = type;
		m_owner = owner;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if (o == null)
			return false;
		if (!(o instanceof UnitOwner))
			return false;
		final UnitOwner other = (UnitOwner) o;
		return other.m_type.equals(this.m_type) && other.m_owner.equals(this.m_owner);
	}
	
	@Override
	public int hashCode()
	{
		return m_type.hashCode() ^ m_owner.hashCode();
	}
	
	@Override
	public String toString()
	{
		return "Unit owner:" + m_owner.getName() + " type:" + m_type.getName();
	}
	
	public UnitType getType()
	{
		return m_type;
	}
	
	public PlayerID getOwner()
	{
		return m_owner;
	}
}
