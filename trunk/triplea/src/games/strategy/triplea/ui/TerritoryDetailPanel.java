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
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorDialog;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.OverlayIcon;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class TerritoryDetailPanel extends AbstractStatPanel
{
	private static final long serialVersionUID = 1377022163587438988L;
	private final IUIContext m_uiContext;
	private final JButton m_showOdds = new JButton("Battle Calculator (Ctrl-B)");
	private Territory m_currentTerritory;
	private final TripleAFrame m_frame;
	private Territory m_new_territory = null; // if not null, shift is pressed
	
	public TerritoryDetailPanel(final MapPanel mapPanel, final GameData data, final IUIContext uiContext, final TripleAFrame frame)
	{
		super(data);
		m_frame = frame;
		m_uiContext = uiContext;
		mapPanel.addMapSelectionListener(new DefaultMapSelectionListener()
		{
			@Override
			public void mouseEntered(final Territory territory)
			{
				if (m_new_territory != null)
				{
					if (territory != null)
						m_new_territory = territory;
				}
				else
					territoryChanged(territory);
			}
		});
		initLayout();
	}
	
	@Override
	protected void initLayout()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(5, 5, 0, 0));
		final String show_battle_calc = "show_battle_calc";
		final Action showBattleCalc = new AbstractAction(show_battle_calc)
		{
			private static final long serialVersionUID = -1863748437390486994L;
			
			public void actionPerformed(final ActionEvent e)
			{
				OddsCalculatorDialog.show(m_frame, m_currentTerritory);
			}
		};
		m_showOdds.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				showBattleCalc.actionPerformed(e);
			}
		});
		final JComponent contentPane = (JComponent) m_frame.getContentPane();
		contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('B', java.awt.event.InputEvent.META_MASK), show_battle_calc);
		contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('B', java.awt.event.InputEvent.CTRL_MASK), show_battle_calc);
		contentPane.getActionMap().put(show_battle_calc, showBattleCalc);
		
		// freeze/unfreeze this panel when shift is pressed/released
		final String freeze_panel = "freeze_panel";
		final Action freezePanel = new AbstractAction(freeze_panel)
		{
			private static final long serialVersionUID = -1863748437390486994L;
			
			public void actionPerformed(final ActionEvent e)
			{
				if (m_new_territory == null && m_currentTerritory != null)
				{
					m_new_territory = m_currentTerritory;
				}
			}
		};
		final String unfreeze_panel = "unfreeze_panel";
		final Action unfreezePanel = new AbstractAction(unfreeze_panel)
		{
			private static final long serialVersionUID = -1863748437390486994L;
			
			public void actionPerformed(final ActionEvent e)
			{
				if (m_new_territory != null)
				{
					if (m_new_territory != null)
						territoryChanged(m_new_territory);
					m_new_territory = null;
				}
			}
		};
		contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK, false), freeze_panel);
		contentPane.getActionMap().put(freeze_panel, freezePanel);
		contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true), unfreeze_panel);
		contentPane.getActionMap().put(unfreeze_panel, unfreezePanel);
	}
	
	@Override
	public void setGameData(final GameData data)
	{
		m_data = data;
		territoryChanged(null);
	}
	
	private void territoryChanged(final Territory territory)
	{
		m_currentTerritory = territory;
		removeAll();
		refresh();
		if (territory == null)
		{
			return;
		}
		add(m_showOdds);
		final TerritoryAttachment ta = TerritoryAttachment.get(territory);
		String labelText;
		if (ta == null)
			labelText = "<html>" + territory.getName() + "<br>Water Territory" + "<br><br></html>";
		else
			labelText = "<html>" + ta.toStringForInfo(true, true) + "<br></html>";
		add(new JLabel(labelText));
		add(new JLabel("Units:"));
		Collection<Unit> unitsInTerritory;
		m_data.acquireReadLock();
		try
		{
			unitsInTerritory = territory.getUnits().getUnits();
		} finally
		{
			m_data.releaseReadLock();
		}
		final JScrollPane scroll = new JScrollPane(unitsInTerritoryPanel(unitsInTerritory, m_uiContext, m_data));
		scroll.setBorder(BorderFactory.createEmptyBorder());
		add(scroll);
		refresh();
	}
	
	private static JPanel unitsInTerritoryPanel(final Collection<Unit> unitsInTerritory, final IUIContext uiContext, final GameData data)
	{
		final JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(2, 20, 2, 2));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		final Set<UnitCategory> units = UnitSeperator.categorize(unitsInTerritory);
		final Iterator<UnitCategory> iter = units.iterator();
		PlayerID currentPlayer = null;
		while (iter.hasNext())
		{
			// seperate players with a seperator
			final UnitCategory item = iter.next();
			if (item.getOwner() != currentPlayer)
			{
				currentPlayer = item.getOwner();
				panel.add(Box.createVerticalStrut(15));
			}
			// TODO Kev determine if we need to identify if the unit is hit/disabled
			final ImageIcon unitIcon = uiContext.getUnitImageFactory().getIcon(item.getType(), item.getOwner(), data, item.hasDamageOrBombingUnitDamage(), item.getDisabled());
			final ImageIcon flagIcon = new ImageIcon(uiContext.getFlagImageFactory().getSmallFlag(item.getOwner()));
			// overlay flag onto upper-right of icon
			final Icon flaggedUnitIcon = new OverlayIcon(unitIcon, flagIcon, unitIcon.getIconWidth() - (flagIcon.getIconWidth() / 2), 0);
			final JLabel label = new JLabel("x" + item.getUnits().size(), flaggedUnitIcon, SwingConstants.LEFT);
			final String toolTipText = "<html>" + item.getType().getName() + ": " + item.getType().getTooltip(currentPlayer, true) + "</html>";
			label.setToolTipText(toolTipText);
			panel.add(label);
		}
		return panel;
	}
	
	private void refresh()
	{
		validate();
		repaint();
	}
}
