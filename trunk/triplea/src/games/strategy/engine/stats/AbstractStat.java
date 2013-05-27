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
package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;

/**
 * Returns an intelligent formatter, and returns value for alliances
 * by summing our value for all players in the alliance.
 * 
 * 
 * @author Sean Bridges
 */
public abstract class AbstractStat implements IStat
{
	protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.##");
	
	public double getValue(final String alliance, final GameData data)
	{
		final Iterator<PlayerID> iter = data.getAllianceTracker().getPlayersInAlliance(alliance).iterator();
		double rVal = 0;
		while (iter.hasNext())
		{
			final PlayerID player = iter.next();
			rVal += getValue(player, data);
		}
		return rVal;
	}
	
	public NumberFormat getFormatter()
	{
		return DECIMAL_FORMAT;
	}
	
	protected static Resource getResourcePUs(final GameData data)
	{
		Resource pus = null;
		try
		{
			data.acquireReadLock();
			pus = data.getResourceList().getResource(Constants.PUS);
		} finally
		{
			data.releaseReadLock();
		}
		return pus;
	}
}
