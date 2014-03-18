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
 * ProductionPanel.java
 * 
 * Created on November 7, 2001, 10:19 AM
 */
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.util.IntegerMap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * 
 */
public class ProductionPanel extends JPanel
{
	private static final long serialVersionUID = -1539053979479586609L;
	private final JFrame m_owner = null;
	private JDialog m_dialog;
	// Edwin: made these protected so the class can be extended
	protected final IUIContext m_uiContext;
	protected List<Rule> m_rules = new ArrayList<Rule>();
	protected JLabel m_left = new JLabel();
	protected JButton m_done;
	protected PlayerID m_id;
	private boolean m_bid;
	protected GameData m_data;
	
	public static IntegerMap<ProductionRule> getProduction(final PlayerID id, final JFrame parent, final GameData data, final boolean bid, final IntegerMap<ProductionRule> initialPurchase,
				final IUIContext context)
	{
		return new ProductionPanel(context).show(id, parent, data, bid, initialPurchase);
	}
	
	/**
	 * Shows the production panel, and returns a map of selected rules.
	 */
	public IntegerMap<ProductionRule> show(final PlayerID id, final JFrame parent, final GameData data, final boolean bid, final IntegerMap<ProductionRule> initialPurchase)
	{
		if (!(parent == m_owner))
			m_dialog = null;
		if (m_dialog == null)
			initDialog(parent);
		this.m_bid = bid;
		this.m_data = data;
		this.initRules(id, data, initialPurchase);
		this.initLayout(id);
		this.calculateLimits();
		m_dialog.pack();
		m_dialog.setLocationRelativeTo(parent);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_done.requestFocusInWindow();
			}
		});
		m_dialog.setVisible(true);
		m_dialog.dispose();
		return getProduction();
	}
	
	// this method can be accessed by subclasses
	protected List<Rule> getRules()
	{
		return m_rules;
	};
	
	private void initDialog(final JFrame root)
	{
		m_dialog = new JDialog(root, "Produce", true);
		m_dialog.getContentPane().add(this);
		final Action closeAction = new AbstractAction("")
		{
			private static final long serialVersionUID = -3984693668306307869L;
			
			public void actionPerformed(final ActionEvent e)
			{
				m_dialog.setVisible(false);
			}
		};
		// close the window on escape
		// this is mostly for developers, makes it much easier to quickly cycle through steps
		final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		final String key = "production.panel.close.prod.popup";
		m_dialog.getRootPane().getActionMap().put(key, closeAction);
		m_dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
	}
	
	/** Creates new ProductionPanel */
	// the constructor can be accessed by subclasses
	protected ProductionPanel(final IUIContext uiContext)
	{
		m_uiContext = uiContext;
	}
	
	// made this protected so can be extended by edit production panel
	protected void initRules(final PlayerID player, final GameData data, final IntegerMap<ProductionRule> initialPurchase)
	{
		m_data.acquireReadLock();
		try
		{
			m_id = player;
			for (final ProductionRule productionRule : player.getProductionFrontier())
			{
				final Rule rule = new Rule(productionRule, player);
				final int initialQuantity = initialPurchase.getInt(productionRule);
				rule.setQuantity(initialQuantity);
				m_rules.add(rule);
			}
		} finally
		{
			m_data.releaseReadLock();
		}
	}
	
	// Edwin: made this protected so the class can be extended
	protected void initLayout(final PlayerID id)
	{
		final Insets nullInsets = new Insets(0, 0, 0, 0);
		this.removeAll();
		this.setLayout(new GridBagLayout());
		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		final ResourceCollection totalWithoutTechTokensOrVPs = new ResourceCollection(getResources());
		m_data.acquireReadLock();
		try
		{
			totalWithoutTechTokensOrVPs.removeAllOfResource(m_data.getResourceList().getResource(Constants.VPS));
			totalWithoutTechTokensOrVPs.removeAllOfResource(m_data.getResourceList().getResource(Constants.TECH_TOKENS));
		} finally
		{
			m_data.releaseReadLock();
		}
		final JLabel legendLabel = new JLabel("<html>Attack/Defense/Movement. &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; (Total Resources: " + totalWithoutTechTokensOrVPs.toString()
					+ ")</html>");
		this.add(legendLabel, new GridBagConstraints(0, 0, 30, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0));
		int rows = m_rules.size() / 7;
		rows = Math.max(2, rows);
		for (int x = 0; x < m_rules.size(); x++)
		{
			panel.add(m_rules.get(x).getPanelComponent(), new GridBagConstraints(x / rows, (x % rows), 1, 1, 10, 10, GridBagConstraints.EAST, GridBagConstraints.BOTH, nullInsets, 0, 0));
		}
		final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
		final int availHeight = screenResolution.height - 80;
		final int availWidth = screenResolution.width - 30;
		final int availHeightRules = availHeight - 116;
		final int availWidthRules = availWidth - 16;
		final JScrollPane scroll = new JScrollPane(panel);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setPreferredSize(new Dimension((scroll.getPreferredSize().width > availWidthRules ? availWidthRules : scroll.getPreferredSize().width
					+ (scroll.getPreferredSize().height > availHeightRules ? 20 : 0)), (scroll.getPreferredSize().height > availHeightRules ? availHeightRules : scroll.getPreferredSize().height
					+ (scroll.getPreferredSize().width > availWidthRules ? 20 : 0))));
		this.add(scroll, new GridBagConstraints(0, 1, 30, 1, 100, 100, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(8, 8, 8, 4), 0, 0));
		// final int startY = m_rules.size() / rows;
		this.add(m_left, new GridBagConstraints(0, 2, 30, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 8, 0, 12), 0, 0));
		m_done = new JButton(m_done_action);
		this.add(m_done, new GridBagConstraints(0, 3, 30, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 8, 0), 0, 0));
		this.setMaximumSize(new Dimension(availWidth, availHeight));
	}
	
	// This method can be overridden by subclasses
	protected void setLeft(final ResourceCollection left, final int totalUnits)
	{
		final ResourceCollection leftWithoutTechTokensOrVPs = new ResourceCollection(left);
		m_data.acquireReadLock();
		try
		{
			leftWithoutTechTokensOrVPs.removeAllOfResource(m_data.getResourceList().getResource(Constants.VPS));
			leftWithoutTechTokensOrVPs.removeAllOfResource(m_data.getResourceList().getResource(Constants.TECH_TOKENS));
		} finally
		{
			m_data.releaseReadLock();
		}
		m_left.setText(totalUnits + " total units purchased.  You have " + leftWithoutTechTokensOrVPs.toString() + " left");
	}
	
	Action m_done_action = new AbstractAction("Done")
	{
		private static final long serialVersionUID = -72826856616395760L;
		
		public void actionPerformed(final ActionEvent e)
		{
			m_dialog.setVisible(false);
		}
	};
	
	private IntegerMap<ProductionRule> getProduction()
	{
		final IntegerMap<ProductionRule> prod = new IntegerMap<ProductionRule>();
		for (final Rule rule : m_rules)
		{
			final int quantity = rule.getQuantity();
			if (quantity != 0)
			{
				prod.put(rule.getProductionRule(), quantity);
			}
		}
		return prod;
	}
	
	// This method can be overridden by subclasses
	protected void calculateLimits()
	{
		// final IntegerMap<Resource> cost;
		final ResourceCollection resources = getResources();
		final ResourceCollection spent = new ResourceCollection(m_data);
		int totalUnits = 0;
		for (final Rule current : m_rules)
		{
			spent.add(current.getCost(), current.getQuantity());
			totalUnits += current.getQuantity() * current.getProductionRule().getResults().totalValues();
		}
		final ResourceCollection leftToSpend = resources.difference(spent);
		setLeft(leftToSpend, totalUnits);
		
		for (final Rule current : m_rules)
		{
			int max = leftToSpend.fitsHowOften(current.getCost());
			max += current.getQuantity();
			current.setMax(max);
		}
	}
	
	protected ResourceCollection getResources()
	{
		if (m_bid)
		{
			// TODO bid only allows you to add PU's to the bid... maybe upgrading Bids so multiple resources can be given?
			final String propertyName = m_id.getName() + " bid";
			final int bid = m_data.getProperties().get(propertyName, 0);
			final ResourceCollection bidCollection = new ResourceCollection(m_data);
			m_data.acquireReadLock();
			try
			{
				bidCollection.addResource(m_data.getResourceList().getResource(Constants.PUS), bid);
			} finally
			{
				m_data.releaseReadLock();
			}
			return bidCollection;
		}
		else
		{
			if (m_id == null || m_id.isNull())
			{
				return new ResourceCollection(m_data);
			}
			return m_id.getResources();
		}
	}
	
	
	class Rule
	{
		private final IntegerMap<Resource> m_cost;
		private int m_quantity;
		private final ProductionRule m_rule;
		private final PlayerID m_id;
		private final Set<ScrollableTextField> m_textFields = new HashSet<ScrollableTextField>();
		
		protected JPanel getPanelComponent()
		{
			final JPanel panel = new JPanel();
			/*String eol = "  ";
			try {
				eol = System.getProperty("line.separator");
			} catch (Exception e) { }*/
			final ScrollableTextField i_text = new ScrollableTextField(0, Integer.MAX_VALUE);
			i_text.setValue(m_quantity);
			panel.setLayout(new GridBagLayout());
			final JLabel info = new JLabel("  ");
			final JLabel name = new JLabel("  ");
			final Color defaultForegroundLabelColor = name.getForeground();
			Icon icon = null;
			final StringBuilder tooltip = new StringBuilder();
			final Set<NamedAttachable> results = new HashSet<NamedAttachable>(m_rule.getResults().keySet());
			final Iterator<NamedAttachable> iter = results.iterator();
			while (iter.hasNext())
			{
				final NamedAttachable resourceOrUnit = iter.next();
				if (resourceOrUnit instanceof UnitType)
				{
					final UnitType type = (UnitType) resourceOrUnit;
					icon = m_uiContext.getUnitImageFactory().getIcon(type, m_id, m_data, false, false);
					final UnitAttachment attach = UnitAttachment.get(type);
					final int attack = attach.getAttack(m_id);
					final int movement = attach.getMovement(m_id);
					final int defense = attach.getDefense(m_id);
					info.setText(attack + "/" + defense + "/" + movement);
					tooltip.append(type.getName() + ": " + type.getTooltip(m_id, true));
					name.setText(type.getName());
					if (attach.getConsumesUnits() != null && attach.getConsumesUnits().totalValues() == 1)
						name.setForeground(Color.CYAN);
					else if (attach.getConsumesUnits() != null && attach.getConsumesUnits().totalValues() > 1)
						name.setForeground(Color.BLUE);
					else
						name.setForeground(defaultForegroundLabelColor);
				}
				else if (resourceOrUnit instanceof Resource)
				{
					final Resource resource = (Resource) resourceOrUnit;
					icon = m_uiContext.getResourceImageFactory().getIcon(resource, m_data, true);
					info.setText("resource");
					tooltip.append(resource.getName() + ": resource");
					name.setText(resource.getName());
					name.setForeground(Color.GREEN);
				}
				if (iter.hasNext())
				{
					tooltip.append("<br /><br /><br /><br />");
				}
			}
			final int numberOfUnitsGiven = m_rule.getResults().totalValues();
			String text;
			if (numberOfUnitsGiven > 1)
				text = "<html> x " + ResourceCollection.toStringForHTML(m_cost, m_data) + "<br>" + "for " + numberOfUnitsGiven + "<br>" + " units</html>";
			else
				text = "<html> x " + ResourceCollection.toStringForHTML(m_cost, m_data) + "</html>";
			final JLabel label = new JLabel(text, icon, SwingConstants.LEFT);
			final String toolTipText = "<html>" + tooltip.toString() + "</html>";
			info.setToolTipText(toolTipText);
			label.setToolTipText(toolTipText);
			final int space = 8;
			// change name color for 'upgrades and consumes' unit types
			panel.add(name, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 0, 0, 0), 0, 0));
			panel.add(label, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space, space), 0, 0));
			panel.add(info, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space, space), 0, 0));
			panel.add(i_text, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, space, space, space), 0, 0));
			i_text.addChangeListener(m_listener);
			m_textFields.add(i_text);
			panel.setBorder(new EtchedBorder());
			return panel;
		}
		
		Rule(final ProductionRule rule, final PlayerID id)
		{
			m_rule = rule;
			m_cost = rule.getCosts();
			m_id = id;
		}
		
		IntegerMap<Resource> getCost()
		{
			return m_cost;
		}
		
		int getQuantity()
		{
			return m_quantity;
		}
		
		void setQuantity(final int quantity)
		{
			m_quantity = quantity;
			for (final ScrollableTextField textField : m_textFields)
			{
				if (textField.getValue() != quantity)
					textField.setValue(quantity);
			}
		}
		
		ProductionRule getProductionRule()
		{
			return m_rule;
		}
		
		void setMax(final int max)
		{
			for (final ScrollableTextField textField : m_textFields)
			{
				textField.setMax(max);
			}
		}
		
		private final ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener()
		{
			public void changedValue(final ScrollableTextField stf)
			{
				if (stf.getValue() != m_quantity)
				{
					m_quantity = stf.getValue();
					calculateLimits();
					for (final ScrollableTextField textField : m_textFields)
					{
						if (!stf.equals(textField))
							textField.setValue(m_quantity);
					}
				}
			}
		};
	}
}
