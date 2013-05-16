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
 * ResourceList.java
 * 
 * Created on October 19, 2001, 10:29 AM
 */
package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class ResourceList extends GameDataComponent
{
	private static final long serialVersionUID = -8812702449627698253L;
	private final Map<String, Resource> m_resourceList = new HashMap<String, Resource>();
	
	/* TODO: is this a good way to have a static reference for PUs?
	private static Resource PUresource = null;
	public static Resource getPUresource(final GameData data)
	{
		if (PUresource == null)
		{
			data.acquireReadLock();
			PUresource = data.getResourceList().getResource(Constants.PUS);
			data.releaseReadLock();
		}
		return PUresource;
	}*/
	
	public ResourceList(final GameData data)
	{
		super(data);
	}
	
	protected void addResource(final Resource resource)
	{
		m_resourceList.put(resource.getName(), resource);
	}
	
	public int size()
	{
		return m_resourceList.size();
	}
	
	public Resource getResource(final String name)
	{
		return m_resourceList.get(name);
	}
	
	public List<Resource> getResources()
	{
		return new ArrayList<Resource>(m_resourceList.values());
	}
}
