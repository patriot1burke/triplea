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
package games.strategy.engine.data.properties;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * User editable property representing a color.
 * <p>
 * Presents a clickable label with the currently selected color, through which a color swatch panel is accessable to change the color.
 * 
 * @author Lane O.B. Schwartz
 * @version $LastChangedDate$
 */
public class ColorProperty extends AEditableProperty
{
	// compatible with 0.9.0.2 saved games
	private static final long serialVersionUID = 6826763550643504789L;
	private final int m_max = 0xFFFFFF;
	private final int m_min = 0x000000;
	private Color m_color;
	
	public ColorProperty(final String name, final String description, final int def)
	{
		super(name, description);
		if (def > m_max || def < m_min)
			throw new IllegalArgumentException("Default value out of range");
		m_color = new Color(def);
	}
	
	public ColorProperty(final String name, final String description, final Color def)
	{
		super(name, description);
		if (def == null)
			m_color = Color.black;
		else
			m_color = def;
	}
	
	public Object getValue()
	{
		return m_color;
	}
	
	public void setValue(final Object value) throws ClassCastException
	{
		if (value == null)
			m_color = Color.black;
		else
			m_color = (Color) value;
	}
	
	public JComponent getEditorComponent()
	{
		final JLabel label = new JLabel("        ")
		{
			private static final long serialVersionUID = 3833935337866905836L;
			
			@Override
			public void paintComponent(final Graphics g)
			{
				final Graphics2D g2 = (Graphics2D) g;
				g2.setColor(m_color);
				g2.fill(g2.getClip());
			}
		};
		label.addMouseListener(new MouseListener()
		{
			public void mouseClicked(final MouseEvent e)
			{
				System.out.println("Old color: " + m_color);
				try
				{
					final Color color = JColorChooser.showDialog(label, "Choose color", (m_color == null ? Color.black : m_color));
					if (color != null)
					{
						m_color = color;
						System.out.println("New color: " + m_color);
						// Ask Swing to repaint this label when it's convenient
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								label.repaint();
							}
						});
					}
				} catch (final Exception exception)
				{
					System.err.println(exception.getMessage());
				}
			}
			
			public void mouseEntered(final MouseEvent e)
			{
			}
			
			public void mouseExited(final MouseEvent e)
			{
			}
			
			public void mousePressed(final MouseEvent e)
			{
			}
			
			public void mouseReleased(final MouseEvent e)
			{
			}
		});
		return label;
	}
	
	public boolean validate(final Object value)
	{
		if (value == null)
			return true;
		if (value instanceof Color)
			return true;
		return false;
	}
}
