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
 * TechActivationDelegate.java
 * 
 * Created on December 7, 2004, 9:55 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.attatchments.TriggerAttachment;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Logic for activating tech rolls. This delegate requires the
 * TechnologyDelegate to run correctly.
 * 
 * @author Ali Ibrahim
 * @version 1.0
 */
public class TechActivationDelegate extends BaseDelegate
{
	/** Creates new TechActivationDelegate */
	public TechActivationDelegate()
	{
	}
	
	/**
	 * Called before the delegate will run. In this class, this does all the
	 * work.
	 */
	
	@Override
	public void start(IDelegateBridge aBridge)
	{
		super.start(aBridge);
		GameData data = getData();
		// Activate techs
		Map<PlayerID, Collection<TechAdvance>> techMap = DelegateFinder.techDelegate(data).getAdvances();
		Collection<TechAdvance> advances = techMap.get(m_player);
		if ((advances != null) && (advances.size() > 0))
		{
			// Start event
			m_bridge.getHistoryWriter().startEvent(m_player.getName() + " activating " + advancesAsString(advances));
			
			Iterator<TechAdvance> techsIter = advances.iterator();
			while (techsIter.hasNext())
			{
				TechAdvance advance = techsIter.next();
				// advance.perform(m_bridge.getPlayerID(), m_bridge, m_data);
				TechTracker.addAdvance(m_player, m_bridge, advance);
			}
		}
		// empty
		techMap.put(m_player, null);
		if (games.strategy.triplea.Properties.getTriggers(data))
		{
			TriggerAttachment.triggerTechChange(m_player, aBridge, null, null);
			TriggerAttachment.triggerSupportChange(m_player, aBridge, null, null);
			TriggerAttachment.triggerUnitPropertyChange(m_player, aBridge, null, null);
		}
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	// Return string representing all advances in collection
	private String advancesAsString(Collection<TechAdvance> advances)
	{
		Iterator<TechAdvance> iter = advances.iterator();
		int count = advances.size();
		StringBuilder text = new StringBuilder();
		
		while (iter.hasNext())
		{
			TechAdvance advance = iter.next();
			text.append(advance.getName());
			count--;
			if (count > 1)
				text.append(", ");
			if (count == 1)
				text.append(" and ");
		}
		return text.toString();
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */

	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return null;
	}
	
}
