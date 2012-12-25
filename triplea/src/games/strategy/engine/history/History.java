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

/**
 * A history of the game.
 * 
 * Stored as a tree, the data is organized as
 * Root
 * - Round
 * - Step
 * - Event
 * - Child
 * 
 * 
 * 
 * 
 * Round - the current round in the game, eg 1, 2, 3
 * Step - the current step, eg Britian Combat Move
 * Event - an event that happened in the game, eg Russia buys 8 inf.
 * 
 * 
 */
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.history.HistoryPanel;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class History extends DefaultTreeModel implements java.io.Serializable
{
	private final HistoryWriter m_writer = new HistoryWriter(this);
	private final List<Change> m_changes = new ArrayList<Change>();
	private final GameData m_data;
	private HistoryNode m_currentNode;
	
	private void assertCorrectThread()
	{
		if (m_data.areChangesOnlyInSwingEventThread() && !SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Wrong thread");
	}
	
	public History(final GameData data)
	{
		super(new RootHistoryNode("Game History", true));
		m_data = data;
	}
	
	public HistoryWriter getHistoryWriter()
	{
		return m_writer;
	}
	
	HistoryPanel m_panel = null;
	
	public void setTreePanel(final HistoryPanel panel)
	{
		m_panel = panel;
	}
	
	public void goToEnd()
	{
		if (m_panel != null)
			m_panel.goToEnd();
	}
	
	public HistoryNode getLastNode()
	{
		assertCorrectThread();
		return getLastChildInternal((HistoryNode) getRoot());
	}
	
	private HistoryNode getLastChildInternal(final HistoryNode node)
	{
		if (node.getChildCount() == 0)
			return node;
		return getLastChildInternal((HistoryNode) node.getLastChild());
	}
	
	private int getLastChange(final HistoryNode node)
	{
		int rVal;
		if (node == getRoot())
			rVal = 0;
		else if (node instanceof Event)
			rVal = ((Event) node).getChangeEndIndex();
		else if (node instanceof EventChild)
			rVal = ((Event) node.getParent()).getChangeEndIndex();
		else if (node instanceof IndexedHistoryNode)
			rVal = ((IndexedHistoryNode) node).getChangeStartIndex();
		else
			rVal = 0;
		if (rVal == -1)
			return m_changes.size();
		return rVal;
	}
	
	public Change getDelta(final HistoryNode start, final HistoryNode end)
	{
		assertCorrectThread();
		final int firstChange = getLastChange(start);
		final int lastChange = getLastChange(end);
		if (firstChange == lastChange)
			return null;
		final List<Change> changes = m_changes.subList(Math.min(firstChange, lastChange), Math.max(firstChange, lastChange));
		final Change compositeChange = new CompositeChange(changes);
		if (lastChange >= firstChange)
		{
			return compositeChange;
		}
		else
		{
			return compositeChange.invert();
		}
	}
	
	public synchronized void gotoNode(final HistoryNode node)
	{
		assertCorrectThread();
		getGameData().acquireWriteLock();
		try
		{
			if (m_currentNode == null)
				m_currentNode = getLastNode();
			final Change dataChange = getDelta(m_currentNode, node);
			m_currentNode = node;
			if (dataChange != null)
				new ChangePerformer(m_data).perform(dataChange);
		} finally
		{
			getGameData().releaseWriteLock();
		}
	}
	
	public synchronized void removeAllHistoryAfterNode(final HistoryNode removeAfterNode)
	{
		gotoNode(removeAfterNode);
		assertCorrectThread();
		getGameData().acquireWriteLock();
		try
		{
			final int lastChange = getLastChange(removeAfterNode);
			// final List<Change> changesToRemove = m_changes.subList(Math.min(m_changes.size(), lastChange), Math.max(m_changes.size(), lastChange));
			// m_changes.removeAll(changesToRemove);
			while (m_changes.size() > lastChange)
			{
				m_changes.remove(lastChange);
			}
			final List<HistoryNode> nodesToRemove = new ArrayList<HistoryNode>();
			final Enumeration enumeration = ((DefaultMutableTreeNode) this.getRoot()).preorderEnumeration();
			enumeration.nextElement();
			boolean startRemoving = false;
			while (enumeration.hasMoreElements())
			{
				final HistoryNode node = (HistoryNode) enumeration.nextElement();
				if (node instanceof IndexedHistoryNode)
				{
					final int index = ((IndexedHistoryNode) node).getChangeStartIndex();
					if (index >= lastChange)
						startRemoving = true;
					if (startRemoving)
						nodesToRemove.add(node);
				}
			}
			while (!nodesToRemove.isEmpty())
			{
				this.removeNodeFromParent(nodesToRemove.remove(0));
			}
		} finally
		{
			getGameData().releaseWriteLock();
		}
	}
	
	synchronized void changeAdded(final Change aChange)
	{
		m_changes.add(aChange);
		if (m_currentNode == null)
			return;
		if (m_currentNode == getLastNode())
		{
			getGameData().acquireWriteLock();
			try
			{
				new ChangePerformer(m_data).perform(aChange);
			} finally
			{
				getGameData().releaseWriteLock();
			}
		}
	}
	
	private Object writeReplace() throws ObjectStreamException
	{
		return new SerializedHistory(this, m_data, m_changes);
	}
	
	List<Change> getChanges()
	{
		return m_changes;
	}
	
	GameData getGameData()
	{
		return m_data;
	}
}


/**
 * DefaultTreeModel is not serializable across jdk versions
 * Instead we use an instance of this class to store our data
 * 
 */
class SerializedHistory implements Serializable
{
	private static final long serialVersionUID = -5808427923253751651L;
	private final List<SerializationWriter> m_Writers = new ArrayList<SerializationWriter>();
	private final GameData m_data;
	
	public SerializedHistory(final History history, final GameData data, final List<Change> changes)
	{
		m_data = data;
		int changeIndex = 0;
		final Enumeration enumeration = ((DefaultMutableTreeNode) history.getRoot()).preorderEnumeration();
		enumeration.nextElement();
		while (enumeration.hasMoreElements())
		{
			final HistoryNode node = (HistoryNode) enumeration.nextElement();
			// write the changes to the start of the node
			if (node instanceof IndexedHistoryNode)
			{
				while (changeIndex < ((IndexedHistoryNode) node).getChangeStartIndex())
				{
					m_Writers.add(new ChangeSerializationWriter(changes.get(changeIndex)));
					changeIndex++;
				}
			}
			// write the node itself
			m_Writers.add(node.getWriter());
		}
		// write out remaining changes
		while (changeIndex < changes.size())
		{
			m_Writers.add(new ChangeSerializationWriter(changes.get(changeIndex)));
			changeIndex++;
		}
	}
	
	public Object readResolve() throws ObjectStreamException
	{
		final History rVal = new History(m_data);
		final HistoryWriter historyWriter = rVal.getHistoryWriter();
		for (final SerializationWriter element : m_Writers)
		{
			element.write(historyWriter);
		}
		return rVal;
	}
}


class RootHistoryNode extends HistoryNode
{
	private static final long serialVersionUID = 625147613043836829L;
	
	public RootHistoryNode(final String title, final boolean allowsChildren)
	{
		super(title, allowsChildren);
	}
	
	@Override
	public SerializationWriter getWriter()
	{
		throw new IllegalStateException("Not implemented");
	}
}


interface SerializationWriter extends Serializable
{
	public void write(HistoryWriter writer);
}


class ChangeSerializationWriter implements SerializationWriter
{
	private static final long serialVersionUID = -3802807345707883606L;
	private final Change aChange;
	
	public ChangeSerializationWriter(final Change change)
	{
		aChange = change;
	}
	
	public void write(final HistoryWriter writer)
	{
		writer.addChange(aChange);
	}
}
