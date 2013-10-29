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
package games.strategy.grid.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.ui.IGridPlayData;

/**
 * Implementing class is responsible for performing a turn in a Kings Table game.
 * 
 * @author Lane Schwartz (original), Veqryn (abstraction)
 * @version $LastChangedDate: 2012-12-25 06:54:45 +0800 (Tue, 25 Dec 2012) $
 */
public interface IGridPlayDelegate extends IRemote, IDelegate
{
	public String play(IGridPlayData play);
	
	public void signalStatus(String status);
}
