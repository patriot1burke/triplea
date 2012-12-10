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
package games.strategy.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * 
 * Blocking JOptionPane calls that do their work in
 * the swing event thread (to be thread safe).
 * 
 * @author Sean Bridges
 */
public class EventThreadJOptionPane
{
	public static void showMessageDialog(final Component parentComponent, final Object message, final String title, final int messageType)
	{
		EventThreadJOptionPane.showMessageDialog(parentComponent, message, title, messageType, false);
	}
	
	private static JScrollPane createJLabelInScrollPane(final String message)
	{
		final JLabel label = new JLabel(message);
		final JScrollPane scroll = new JScrollPane(label);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
		final int availWidth = screenResolution.width - 40;
		final int availHeight = screenResolution.height - 140;
		// add 20 for scrollbars
		final int newWidth = (scroll.getPreferredSize().width > availWidth ? availWidth : (scroll.getPreferredSize().width + (scroll.getPreferredSize().height > availHeight ? 20 : 0)));
		final int newHeight = (scroll.getPreferredSize().height > availHeight ? availHeight : (scroll.getPreferredSize().height + (scroll.getPreferredSize().width > availWidth ? 20 : 0)));
		scroll.setPreferredSize(new Dimension(newWidth, newHeight));
		// scroll.setMaximumSize(new Dimension(newWidth, newHeight));
		return scroll;
	}
	
	public static void showMessageDialog(final Component parentComponent, final Object message, final String title, final int messageType, final boolean useJLabel)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			JOptionPane.showMessageDialog(parentComponent, useJLabel ? createJLabelInScrollPane((String) message) : message, title, messageType);
			return;
		}
		final CountDownLatch latch = new CountDownLatch(1);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				JOptionPane.showMessageDialog(parentComponent, useJLabel ? createJLabelInScrollPane((String) message) : message, title, messageType);
				latch.countDown();
			}
		});
		boolean done = false;
		while (!done)
		{
			try
			{
				latch.await();
				done = true;
			} catch (final InterruptedException e)
			{
				// ignore
			}
		}
		return;
	}
	
	public static void showMessageDialog(final Frame parentComponent, final String message)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			JOptionPane.showMessageDialog(parentComponent, message);
			return;
		}
		final CountDownLatch latch = new CountDownLatch(1);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				JOptionPane.showMessageDialog(parentComponent, message);
				latch.countDown();
			}
		});
		boolean done = false;
		while (!done)
		{
			try
			{
				latch.await();
				done = true;
			} catch (final InterruptedException e)
			{
				// ignore
			}
		}
		return;
	}
	
	public static void showMessageDialog(final Component parentComponent, final Object message) throws HeadlessException
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			JOptionPane.showMessageDialog(parentComponent, message);
			return;
		}
		final CountDownLatch latch = new CountDownLatch(1);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				JOptionPane.showMessageDialog(parentComponent, message);
				latch.countDown();
			}
		});
		boolean done = false;
		while (!done)
		{
			try
			{
				latch.await();
				done = true;
			} catch (final InterruptedException e)
			{
				// ignore
			}
		}
		return;
	}
	
	public static void showMessageDialog(final Component parentComponent, final Object message, final String title, final int messageType, final Icon icon)
	{
		showOptionDialog(parentComponent, message, title, JOptionPane.DEFAULT_OPTION, messageType, icon, null, null);
	}
	
	public static int showOptionDialog(final Component parentComponent, final Object message, final String title, final int optionType, final int messageType, final Icon icon, final Object[] options,
				final Object initialValue)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			return JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue);
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger rVal = new AtomicInteger();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				rVal.set(JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue));
				latch.countDown();
			}
		});
		boolean done = false;
		while (!done)
		{
			try
			{
				latch.await();
				done = true;
			} catch (final InterruptedException e)
			{
				// ignore
			}
		}
		return rVal.get();
	}
	
	public static int showConfirmDialog(final Component parentComponent, final Object message, final String title, final int optionType) throws HeadlessException
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			return JOptionPane.showConfirmDialog(parentComponent, message, title, optionType);
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger rVal = new AtomicInteger();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				rVal.set(JOptionPane.showConfirmDialog(parentComponent, message, title, optionType));
				latch.countDown();
			}
		});
		boolean done = false;
		while (!done)
		{
			try
			{
				latch.await();
				done = true;
			} catch (final InterruptedException e)
			{
				// ignore
			}
		}
		return rVal.get();
	}
}
