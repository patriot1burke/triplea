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
package games.strategy.engine.random;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * 
 * Its a bit messy, but the threads are a pain to deal with We want to be able
 * to call this from any thread, and have a dialog that doesnt close until the
 * dice roll finishes. If there is an error we wait until we get a good roll
 * before returning.
 * 
 * @author George_H
 */
public class PBEMDiceRoller implements IRandomSource
{
	private final String m_gameUUID;
	private final IRemoteDiceServer m_remoteDiceServer;
	private static Frame s_focusWindow;
	
	/*
	 * If the game has multiple frames, allows the ui to 
	 * set what frame should be the parent of the dice rolling window
	 * if set to null, or not set, we try to guess by finding the currently 
	 * focused window (or a visble window if none are focused).
	 */
	public static void setFocusWindow(final Frame w)
	{
		s_focusWindow = w;
	}
	
	public PBEMDiceRoller(final IRemoteDiceServer diceServer, final String gameUUID)
	{
		m_remoteDiceServer = diceServer;
		m_gameUUID = gameUUID;
	}
	
	/**
	 * Do a test roll, leaving the dialog open after the roll is done.
	 */
	public void test()
	{
		// TODO: do a test based on data.getDiceSides()
		final HttpDiceRollerDialog dialog = new HttpDiceRollerDialog(getFocusedFrame(), 6, 1, "Test", m_remoteDiceServer, "test-roll");
		dialog.setTest();
		dialog.roll();
	}
	
	/**
	 * getRandom
	 */
	public int[] getRandom(final int max, final int count, final String annotation) throws IllegalStateException
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			final AtomicReference<int[]> result = new AtomicReference<int[]>();
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						result.set(getRandom(max, count, annotation));
					}
				});
			} catch (final InterruptedException e)
			{
				throw new IllegalStateException(e);
			} catch (final InvocationTargetException e)
			{
				throw new IllegalStateException(e);
			}
			return result.get();
		}
		final HttpDiceRollerDialog dialog = new HttpDiceRollerDialog(getFocusedFrame(), max, count, annotation, m_remoteDiceServer, m_gameUUID);
		dialog.roll();
		return dialog.getDiceRoll();
	}
	
	private Frame getFocusedFrame()
	{
		if (s_focusWindow != null)
			return s_focusWindow;
		final Frame[] frames = Frame.getFrames();
		Frame rVal = null;
		for (int i = 0; i < frames.length; i++)
		{
			// find the window with focus, failing that, get something that is
			// visible
			if (frames[i].isFocused())
			{
				rVal = frames[i];
			}
			else if (rVal == null && frames[i].isVisible())
			{
				rVal = frames[i];
			}
		}
		return rVal;
	}
	
	/**
	 * getRandom
	 * 
	 * @param max
	 *            int
	 * @param annotation
	 *            String
	 * @return int
	 */
	public int getRandom(final int max, final String annotation) throws IllegalStateException
	{
		return getRandom(max, 1, annotation)[0];
	}
}


/**
 * The dialog that will show while the dice are rolling
 */
class HttpDiceRollerDialog extends JDialog
{
	private static final long serialVersionUID = -4802403913826489223L;
	private final JButton m_exitButton = new JButton("Exit");
	private final JButton m_reRollButton = new JButton("Roll Again");
	private final JButton m_okButton = new JButton("OK");
	private final JTextArea m_text = new JTextArea();
	private int[] m_diceRoll;
	private final int m_count;
	private final int m_sides;
	private final String m_subjectMessage;
	
	private final String m_gameID;
	private final IRemoteDiceServer m_diceServer;
	private final String m_gameUUID;
	private final Object m_lock = new Object();
	public boolean m_test = false;
	private final JPanel m_buttons = new JPanel();
	private Window m_owner;
	
	/**
	 * 
	 * @param owner
	 *            owner frame
	 * @param sides
	 *            the number of sides on the dice
	 * @param count
	 *            the number of dice rolled
	 * @param subjectMessage
	 *            the subject for the email the dice roller will send (if it sends emails)
	 * @param diceServer
	 *            the dice server implementation
	 * @param gameUUID
	 *            the TripleA game UUID or null
	 */
	public HttpDiceRollerDialog(final Frame owner, final int sides, final int count, final String subjectMessage,
				final IRemoteDiceServer diceServer, final String gameUUID)
	{
		super(owner, "Dice roller", true);
		m_owner = owner;
		m_sides = sides;
		m_count = count;
		m_subjectMessage = subjectMessage;
		m_gameID = diceServer.getGameId() == null ? "" : diceServer.getGameId();
		m_diceServer = diceServer;
		m_gameUUID = gameUUID;
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		m_exitButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				System.exit(-1);
			}
		});
		m_exitButton.setEnabled(false);
		m_reRollButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				rollInternal();
			}
		});
		m_okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				closeAndReturn();
			}
		});
		m_reRollButton.setEnabled(false);
		getContentPane().setLayout(new BorderLayout());
		m_buttons.add(m_exitButton);
		m_buttons.add(m_reRollButton);
		getContentPane().add(m_buttons, BorderLayout.SOUTH);
		getContentPane().add(new JScrollPane(m_text));
		m_text.setEditable(false);
		setSize(400, 300);
		games.strategy.ui.Util.center(this); // games.strategy.ui.Util
	}
	
	/**
	 * There are three differences when we are testing, 1 dont close the window
	 * when we are done 2 remove the exit button 3 add a close button
	 */
	public void setTest()
	{
		m_test = true;
		m_buttons.removeAll();
		m_buttons.add(m_okButton);
		m_buttons.add(m_reRollButton);
	}
	
	public void appendText(final String aString)
	{
		m_text.setText(m_text.getText() + aString);
	}
	
	public void notifyError()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_exitButton.setEnabled(true);
				m_reRollButton.setEnabled(true);
			}
		});
	}
	
	public int[] getDiceRoll()
	{
		return m_diceRoll;
	}
	
	// should only be called if we are not visible
	// can be called from any thread
	// wont return until the roll is done.
	public void roll() throws IllegalStateException
	{
		// if we are not the event thread, then start again in the event thread
		// pausing this thread until we are done
		if (!SwingUtilities.isEventDispatchThread())
		{
			synchronized (m_lock)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						roll();
					}
				});
				try
				{
					m_lock.wait();
				} catch (final InterruptedException ie)
				{
					ie.printStackTrace();
				}
			}
			return;
		}
		rollInternal();
		setVisible(true);
	}
	
	// should be called from the event thread
	private void rollInternal() throws IllegalStateException
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Wrong thread");
		m_reRollButton.setEnabled(false);
		m_exitButton.setEnabled(false);
		final Thread t = new Thread("Triplea, roll in seperate thread")
		{
			@Override
			public void run()
			{
				rollInSeperateThread();
			}
		};
		t.start();
	}
	
	private void closeAndReturn()
	{
		// releast any threads waiting on the lock
		if (m_lock != null)
		{
			synchronized (m_lock)
			{
				m_lock.notifyAll();
			}
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				setVisible(false);
				m_owner.toFront();
				m_owner = null;
				dispose();
			}
		});
	}
	
	/**
	 * should be called from a thread other than the event thread after we are
	 * open (or at least in the process of opening) will close the window and
	 * notify any waiting threads when completed successfully.
	 * 
	 * Before contacting Irony Dice Server, check if email has a reasonable
	 * valid syntax.
	 * 
	 * 
	 */
	private void rollInSeperateThread() throws IllegalStateException
	{
		if (SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Wrong thread");
		while (!isVisible())
		{
			Thread.yield();
		}
		appendText(m_subjectMessage + "\n");
		appendText("Contacting  " + m_diceServer.getDisplayName() + "\n");
		String text = null;
		try
		{
			text = m_diceServer.postRequest(m_sides, m_count, m_subjectMessage, m_gameID, m_gameUUID);
			if (text.length() == 0)
			{
				appendText("Nothing could be read from dice server\n");
				appendText("Please check your firewall settings");
				notifyError();
			}
			if (!m_test)
				appendText("Contacted :" + text + "\n");
			m_diceRoll = m_diceServer.getDice(text, m_count);
			appendText("Success!");
			if (!m_test)
				closeAndReturn();
		}
		// an error in networking
		catch (final SocketException ex)
		{
			appendText("Connection failure:" + ex.getMessage() + "\n" + "Please ensure your Internet connection is working, and try again.");
			notifyError();
		} catch (final InvocationTargetException e)
		{
			appendText("\nError:" + e.getMessage() + "\n\n");
			appendText("Text from dice server:\n" + text + "\n");
			notifyError();
		} catch (final IOException ex)
		{
			try
			{
				appendText("An error has occured!\n");
				appendText("Possible reasons the error could have happened:\n");
				appendText("  1: An invalid e-mail address\n");
				appendText("  2: Firewall could be blocking TripleA from connecting to the Dice Server\n");
				appendText("  3: The e-mail address does not exist\n");
				appendText("  4: An unknown error, please see the error console and consult the forums for help\n");
				appendText("     Visit http://tripleadev.org  for extra help\n");
				if (text != null)
				{
					appendText("Text from dice server:\n" + text + "\n");
				}
				final StringWriter writer = new StringWriter();
				ex.printStackTrace(new PrintWriter(writer));
				writer.close();
				appendText(writer.toString());
			} catch (final IOException ex1)
			{
				ex1.printStackTrace();
			}
			notifyError();
		}
	}// end of method
}
