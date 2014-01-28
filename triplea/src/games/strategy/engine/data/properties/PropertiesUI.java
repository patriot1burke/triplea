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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PropertiesUI extends JPanel
{
	private static final long serialVersionUID = 3870459799384582310L;
	private final List<? extends IEditableProperty> m_properties;
	private int m_nextRow;
	private int m_labelColumn;
	
	public static void main(final String[] args)
	{
		final GameProperties properties = new GameProperties(null);
		properties.addEditableProperty(new BooleanProperty("bool1 default false", null, false));
		properties.addEditableProperty(new BooleanProperty("bool2 default true", null, true));
		properties.addEditableProperty(new StringProperty("String", null, "default"));
		properties.addEditableProperty(new NumberProperty("Number [10,20]", null, 20, 12, 15));
		final Collection<String> listValues = new ArrayList<String>();
		listValues.add("apples");
		listValues.add("oranges");
		listValues.add("bananas");
		properties.addEditableProperty(new ComboProperty<String>("List", null, "apples", listValues));
		final PropertiesUI ui = new PropertiesUI(properties, true);
		final JFrame frame = new JFrame();
		frame.getContentPane().add(ui);
		frame.pack();
		frame.setVisible(true);
	}
	
	public PropertiesUI(final GameProperties gameProperties, final boolean editable)
	{
		this(gameProperties.getEditableProperties(), editable);
	}
	
	public PropertiesUI(final List<? extends IEditableProperty> properties, final boolean editable)
	{
		init();
		m_properties = properties;
		for (final IEditableProperty property : m_properties)
		{
			// Limit it to 14 rows then start a new column
			// Don't know if this is the most elegant solution, but it works.
			if (m_nextRow >= 15)
			{
				m_labelColumn += 2;
				m_nextRow = 0;
			}
			if (editable)
				addItem(property.getName(), property.getEditorComponent(), property.getDescription(), property.getRowsNeeded());
			else
				addItem(property.getName(), property.getViewComponent(), property.getDescription(), property.getRowsNeeded());
		}
	}
	
	private void init()
	{
		setLayout(new GridBagLayout());
		// Create a blank label to use as a vertical fill so that the
		// label/item pairs are aligned to the top of the panel and are not
		// grouped in the centre if the parent component is taller than
		// the preferred size of the panel.
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 99;
		constraints.insets = new Insets(10, 0, 0, 0);
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.VERTICAL;
		final JLabel verticalFillLabel = new JLabel();
		add(verticalFillLabel, constraints);
	}
	
	private void addItem(final String labelText, final JComponent item, final String tooltip, final int rowsNeeded)
	{
		// Create the label and its constraints
		final JLabel label = new JLabel(labelText);
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		// labelConstraints.gridx = 0;
		labelConstraints.gridx = m_labelColumn;
		labelConstraints.gridy = m_nextRow;
		labelConstraints.gridheight = rowsNeeded;
		labelConstraints.insets = new Insets(10, 10, 0, 0);
		labelConstraints.anchor = GridBagConstraints.NORTHEAST;
		labelConstraints.fill = GridBagConstraints.NONE;
		add(label, labelConstraints);
		// Add the component with its constraints
		final GridBagConstraints itemConstraints = new GridBagConstraints();
		// itemConstraints.gridx = 1;
		itemConstraints.gridx = m_labelColumn + 1;
		itemConstraints.gridy = m_nextRow;
		itemConstraints.gridheight = rowsNeeded;
		itemConstraints.insets = new Insets(10, 10, 0, 10);
		itemConstraints.weightx = 1.0;
		itemConstraints.anchor = GridBagConstraints.WEST;
		// itemConstraints.fill = GridBagConstraints.HORIZONTAL;
		itemConstraints.fill = GridBagConstraints.NONE;
		add(item, itemConstraints);
		if (tooltip != null && tooltip.length() > 0)
		{
			label.setToolTipText(tooltip);
			item.setToolTipText(tooltip);
		}
		m_nextRow += rowsNeeded;
	}
}
