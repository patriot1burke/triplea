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
 * GameObjectInoutStreamFactory.java
 *
 * Created on January 1, 2002, 4:50 PM
 */

package games.strategy.engine.framework;

import java.io.*;

import games.strategy.net.IObjectStreamFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectInputStream;
import games.strategy.engine.data.GameObjectOutputStream;

/**
 *
 * @author  Sean Bridges
 */
public class GameObjectStreamFactory implements IObjectStreamFactory
{
	private GameData m_data;

    public GameObjectStreamFactory(GameData data)
	{
		m_data = data;
    }

	public ObjectInputStream create(InputStream stream) throws IOException
	{
		return new GameObjectInputStream(this, stream);
	}

	public ObjectOutputStream create(OutputStream stream) throws IOException
	{
		return new GameObjectOutputStream(stream);
	}

	public void setData(GameData data)
	{
		m_data = data;
	}

	public GameData getData()
	{
		return m_data;
	}
}
