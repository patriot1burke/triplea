package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.dataObjects.TechResults;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2003
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author unascribed
 * @version 1.0
 */
public class TechResultsDisplay extends JPanel
{
	private static final long serialVersionUID = -8303376983862918107L;
	private final IUIContext m_uiContext;
	
	public TechResultsDisplay(final TechResults msg, final IUIContext uiContext, final GameData data)
	{
		m_uiContext = uiContext;
		setLayout(new GridBagLayout());
		add(new JLabel("You got " + msg.getHits() + " hit" + (msg.getHits() != 1 ? "s" : "") + "."), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 0, 5, 0), 0, 0));
		if (msg.getHits() != 0)
		{
			add(new JLabel("Technologies discovered:"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			final JList list = new JList(new Vector<String>(msg.getAdvances()));
			add(list, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
			list.setBackground(this.getBackground());
		}
		final JPanel dice = new JPanel();
		dice.setLayout(new BoxLayout(dice, BoxLayout.X_AXIS));
		final int remainder = msg.getRemainder();
		for (int i = 0; i < msg.getRolls().length; i++)
		{
			// add 1 since dice are 0 based
			final int roll = msg.getRolls()[i] + 1;
			JLabel die;
			if (remainder > 0)
				die = new JLabel(m_uiContext.getDiceImageFactory().getDieIcon(roll, roll <= remainder ? Die.DieType.HIT : Die.DieType.MISS));
			else
				die = new JLabel(m_uiContext.getDiceImageFactory().getDieIcon(roll, roll == data.getDiceSides() ? Die.DieType.HIT : Die.DieType.MISS));
			dice.add(die);
			dice.add(Box.createHorizontalStrut(2));
			dice.setMaximumSize(new Dimension(200, (int) dice.getMaximumSize().getHeight()));
		}
		final JScrollPane diceScroll = new JScrollPane(dice);
		diceScroll.setBorder(null);
		add(diceScroll, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
	}
}
