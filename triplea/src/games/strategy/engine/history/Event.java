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
package games.strategy.engine.history;

public class Event extends IndexedHistoryNode implements Renderable
{
	private static final long serialVersionUID = -8382102990360177484L;
	private final String m_description;
	// additional data used for rendering this event
	private Object m_renderingData;
	
	public String getDescription()
	{
		return m_description;
	}
	
	Event(final String description, final int changeStartIndex)
	{
		super(description, changeStartIndex, true);
		m_description = description;
	}
	
	public Object getRenderingData()
	{
		return m_renderingData;
	}
	
	public void setRenderingData(final Object data)
	{
		m_renderingData = data;
	}
	
	@Override
	public SerializationWriter getWriter()
	{
		return new EventHistorySerializer(m_description, m_renderingData);
	}
}


class EventHistorySerializer implements SerializationWriter
{
	private static final long serialVersionUID = 6404070330823708974L;
	private final String m_eventName;
	private final Object m_renderingData;
	
	public EventHistorySerializer(final String eventName, final Object renderingData)
	{
		m_eventName = eventName;
		m_renderingData = renderingData;
	}
	
	public void write(final HistoryWriter writer)
	{
		writer.startEvent(m_eventName);
		if (m_renderingData != null)
			writer.setRenderingData(m_renderingData);
	}
}
