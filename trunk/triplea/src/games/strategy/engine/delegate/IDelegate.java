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
 * Delegate.java
 * 
 * Created on October 13, 2001, 4:27 PM
 */
package games.strategy.engine.delegate;

import games.strategy.engine.message.IRemote;

import java.io.Serializable;

/**
 * 
 * 
 * A section of code that implements game logic.
 * The delegate should be deterministic. All random events should be
 * obtained through calls to the delegateBridge.
 * 
 * Delegates make changes to gameData by calling the addChange method in DelegateBridge.
 * 
 * All delegates must have a zero argument constructor, due to reflection constraints.
 * The delegate will be initialized with a call of initialize(..) before used.
 * 
 * Delegates start executing with the start method, and stop with the end message.
 * 
 * Delegates can be made accessible to players through implementing an IRemote,
 * and will be called through RemoteMessenger.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public interface IDelegate
{
	/**
	 * Uses name as the internal unique name and displayName for display to users
	 */
	public void initialize(final String name, final String displayName);
	
	/**
	 * Called before the delegate will run and before "start" is called.
	 * 
	 * @param iDelegateBridge
	 *            the IDelegateBridge
	 */
	public void setDelegateBridgeAndPlayer(final IDelegateBridge iDelegateBridge);
	
	/**
	 * Called before the delegate will run.
	 */
	public void start();
	
	/**
	 * Called before the delegate will stop running.
	 */
	public void end();
	
	public String getName();
	
	public String getDisplayName();
	
	public IDelegateBridge getBridge();
	
	/**
	 * @return state of the Delegate
	 */
	public Serializable saveState();
	
	/**
	 * @param state
	 *            the delegates state
	 */
	public void loadState(final Serializable state);
	
	/**
	 * @return the remote type of this delegate for use
	 *         by a RemoteMessenger (Class must be an interface that extends IRemote.
	 *         If the return value is null, then it indicates that this
	 *         delegate should not be used as in IRemote.)
	 */
	public Class<? extends IRemote> getRemoteType();
	
	/**
	 * Do we have any user-interface things to do in this delegate or not?
	 * Example: In the "place delegate" if we have units to place or have already placed some units then this should return true,
	 * and if we have nothing to place then this should return false;
	 * Example2: In a "move delegate" if we have either moved units already or have units with movement left, then this should return true,
	 * and if we have no units to move or undo-move, then this should return false.
	 * 
	 * Because communication over the network can take a while, this should only be called from the server game.
	 * 
	 * @return should we run the delegate in order to receive user input, or not?
	 */
	public boolean delegateCurrentlyRequiresUserInput();
}
