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
 * TestDelegate.java
 * 
 * Created on October 22, 2001, 9:39 AM
 */
package games.strategy.engine.xml;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.message.IRemote;

import java.io.Serializable;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          A simple dumb delegate, dont acutally call these methods.
 *          Simply to satisfy the interface requirements for testing.
 */
public final class TestDelegate extends AbstractDelegate
{
	public TestDelegate()
	{
	}
	
	public boolean supportsTransactions()
	{
		return false;
	}
	
	public void initialize(final String name)
	{
		m_name = name;
	}
	
	@Override
	public void initialize(final String name, final String displayName)
	{
		m_name = name;
	}
	
	public void startTransaction()
	{
	}
	
	public void rollback()
	{
	}
	
	public void commit()
	{
	}
	
	public boolean inTransaction()
	{
		return false;
	}
	
	@Override
	public String getName()
	{
		return m_name;
	}
	
	public void cancelTransaction()
	{
	}
	
	@Override
	public void end()
	{
	}
	
	@Override
	public String getDisplayName()
	{
		return "displayName";
	}
	
	@Override
	public Class<IRemote> getRemoteType()
	{
		return IRemote.class;
	}
	
	/**
	 * Returns the state of the Delegate.
	 */
	@Override
	public Serializable saveState()
	{
		return null;
	}
	
	/**
	 * Loads the delegates state
	 */
	@Override
	public void loadState(final Serializable state)
	{
	}
	
	public boolean delegateCurrentlyRequiresUserInput()
	{
		return true;
	}
}
