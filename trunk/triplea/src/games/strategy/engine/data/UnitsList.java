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
package games.strategy.engine.data;

import games.strategy.net.GUID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UnitsList implements java.io.Serializable, Iterable<Unit>
{
	private static final long serialVersionUID = -3134052492257867416L;
	// maps GUID -> Unit
	// TODO - fix this, all units are never gcd
	// note, weak hash maps are not serializable
	private Map<GUID, Unit> m_allUnits;
	
	Unit get(final GUID id)
	{
		return m_allUnits.get(id);
	}
	
	public void put(final Unit unit)
	{
		m_allUnits.put(unit.getID(), unit);
	}
	
	/*
	  * Gets all units currently in the game
	  */
	public Collection<Unit> getUnits()
	{
		return Collections.unmodifiableCollection(m_allUnits.values());
	}
	
	public void refresh()
	{
		m_allUnits = new HashMap<GUID, Unit>();
	}
	
	UnitsList()
	{
		refresh();
	}
	
	public Iterator<Unit> iterator()
	{
		return getUnits().iterator();
	}
}
