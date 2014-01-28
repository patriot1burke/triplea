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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * A string property with a simple text field editor
 */
public class StringProperty extends AEditableProperty
{
	private static final long serialVersionUID = 4382624884674152208L;
	private String m_value;
	
	public StringProperty(final String name, final String description, final String defaultValue)
	{
		super(name, description);
		m_value = defaultValue;
	}
	
	public JComponent getEditorComponent()
	{
		final JTextField text = new JTextField(m_value);
		text.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_value = text.getText();
			}
		});
		text.addFocusListener(new FocusListener()
		{
			public void focusGained(final FocusEvent e)
			{
			}
			
			public void focusLost(final FocusEvent e)
			{
				m_value = text.getText();
			}
		});
		final Dimension ourMinimum = new Dimension(80, 20);
		text.setMinimumSize(ourMinimum);
		text.setPreferredSize(ourMinimum);
		return text;
	}
	
	public Object getValue()
	{
		return m_value;
	}
	
	public void setValue(final Object value) throws ClassCastException
	{
		m_value = (String) value;
	}
	
	public boolean validate(final Object value)
	{
		if (value == null)
			return true;
		if (value instanceof String)
			return true;
		return false;
	}
}
