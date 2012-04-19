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
/*
 * Resource.java
 * 
 * Created on October 13, 2001, 9:33 AM
 */
package games.strategy.engine.data;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class Resource extends NamedAttachable
{
	private static final long serialVersionUID = 7471431759007499935L;
	
	/**
	 * Creates new Resource
	 * 
	 * @param name
	 *            name of the resource
	 * @param data
	 *            game data
	 */
	public Resource(final String name, final GameData data)
	{
		super(name, data);
	}
}
