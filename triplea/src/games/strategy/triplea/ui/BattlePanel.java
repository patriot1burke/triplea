/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * BattlePanel.java
 *
 * Created on December 4, 2001, 7:00 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import java.util.List;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.message.*;

/**
 * 
 * UI for fighting battles.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class BattlePanel extends ActionPanel
{

    private static Font BOLD;
    static
    {
        Map atts = new HashMap();
        atts.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        BOLD = new Font(atts);
    }

    private JLabel m_actionLabel = new JLabel();
    private FightBattleDetails m_fightBattleMessage;
    private TripleAFrame m_parent;

    private BattleDisplay m_battleDisplay;
    private JFrame m_battleFrame;

    /** Creates new BattlePanel */
    public BattlePanel(GameData data, MapPanel map, TripleAFrame parent)
    {
        super(data, map);
        m_parent = parent;
    }

    public void display(PlayerID id, Collection battles, Collection bombing)
    {
        super.display(id);
        removeAll();
        m_actionLabel.setText(id.getName() + " battle");
        add(m_actionLabel);
        Iterator iter = battles.iterator();
        while (iter.hasNext())
        {
            Action action = new FightBattleAction((Territory) iter.next(),
                    false);
            add(new JButton(action));
        }

        iter = bombing.iterator();
        while (iter.hasNext())
        {
            Action action = new FightBattleAction((Territory) iter.next(), true);
            add(new JButton(action));
        }
        SwingUtilities.invokeLater(REFRESH);
    }

    public Message battleInfo(BattleInfoMessage msg)
    {
        if (m_battleDisplay != null)
            m_battleDisplay.battleInfo(msg);

        return null;
    }

    public void battleEndMessage(BattleEndMessage message)
    {
        m_battleDisplay.endBattle(message);
        m_battleDisplay = null;
        m_battleFrame.setVisible(false);
        m_battleFrame.dispose();
        m_battleFrame = null;
    }

    public Message listBattle(final BattleStepMessage msg)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            Runnable r = new Runnable()
            {
                public void run()
                {
                    listBattle(msg);
                }
            };
            try
            {
                SwingUtilities.invokeAndWait(r);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JTextArea text = new JTextArea();

        text.setFont(BOLD);
        text.setEditable(false);
        text.setBackground(this.getBackground());
        text.setText(msg.getTitle());
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        panel.add(text, BorderLayout.NORTH);

        getMap().centerOn(msg.getTerritory());
        m_battleDisplay.listBattle(msg);

        return null;
    }

    public Message battleStartMessage(BattleStartMessage msg)
    {
        if (!(m_battleDisplay == null))
        {
            throw new IllegalStateException("Battle display already showing");
        }

        m_battleDisplay = new BattleDisplay(getData(), msg.getTerritory(), msg
                .getAttacker(), msg.getDefender(), msg.getAttackingUnits(), msg
                .getDefendingUnits());

        m_battleFrame = new JFrame(msg.getAttacker().getName() + " attacks "
                + msg.getDefender().getName() + " in "
                + msg.getTerritory().getName());
        m_battleFrame.setIconImage(games.strategy.engine.framework.GameRunner
                .getGameIcon(m_battleFrame));
        m_battleFrame.getContentPane().add(m_battleDisplay);
        m_battleFrame.setSize(750, 500);
        games.strategy.ui.Util.center(m_battleFrame);
        m_battleFrame.show();
        m_battleFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_battleFrame.toFront();
            }
        });

        return null;
    }

    public FightBattleDetails waitForBattleSelection()
    {
        try
        {
            synchronized (getLock())
            {
                getLock().wait();
            }
        } catch (InterruptedException ie)
        {
            waitForBattleSelection();
        }

        if (m_fightBattleMessage != null)
            getMap().centerOn(m_fightBattleMessage.getWhere());

        return m_fightBattleMessage;
    }

    /**
     * Ask user which territory to bombard with a given unit.
     */
    public Territory getBombardment(Unit unit, Territory unitTerritory, Collection territories, boolean noneAvailable)
    {
        BombardComponent comp = new BombardComponent(unit, unitTerritory, territories, noneAvailable);
    
        int option = JOptionPane.NO_OPTION;
        while(option != JOptionPane.OK_OPTION)
        {           
            option = JOptionPane.showConfirmDialog(this, comp,
                "Bombardment Territory Selection", JOptionPane.OK_OPTION);
        }
        return comp.getSelection();
    }

    public void casualtyNotificationMessage(CasualtyNotificationMessage message)
    {
        //if we are playing this player, then dont wait for the user
        //to see the units, since the player selected the units, and knows
        //what they are
        //if all the units to be removed have been calculated automatically
        // then wait so user can see units which have been removed.
        //if no units died, then wait, since the user hasnt had a chance to
        //see the roll
        boolean waitFOrUserInput = !m_parent.playing(message.getPlayer())
                || message.getAutoCalculated() || message.isEmpty();
        m_battleDisplay.casualtyNotificationMessage(message, waitFOrUserInput);
    }

    public CasualtyDetails getCasualties(String step, Collection selectFrom, Map dependents, int count, String message, DiceRoll dice, PlayerID hit, List defaultCasualties)
    {
        //if the battle display is null, then this is a bombing raid
        if (m_battleDisplay == null)
            return getCasualtiesAA(step, selectFrom, dependents, count, message, dice,hit, defaultCasualties);
        else
        {
            m_battleDisplay.setStep(step);
            return m_battleDisplay.getCasualties(step, selectFrom, dependents, count, message, dice,hit, defaultCasualties);
        }
    }

    private CasualtyDetails getCasualtiesAA(String step, Collection selectFrom, Map dependents, int count, String message, DiceRoll dice, PlayerID hit, List defaultCasualties)
    {
        UnitChooser chooser = new UnitChooser(selectFrom, dependents, getData(), false);

        chooser.setTitle(message);
        chooser.setMax(count);

        DicePanel dicePanel = new DicePanel();
        dicePanel.setDiceRoll(dice);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chooser, BorderLayout.CENTER);
        dicePanel.setMaximumSize(new Dimension(450, 600));

        dicePanel.setPreferredSize(new Dimension(300, (int) dicePanel.getPreferredSize()
                .getHeight()));
        panel.add(dicePanel, BorderLayout.SOUTH);

        String[] options = { "OK" };
        JOptionPane.showOptionDialog(getRootPane(), panel, hit.getName()
                + " select casualties", JOptionPane.OK_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, null);
        List killed = chooser.getSelected(false);
        CasualtyDetails response = new CasualtyDetails(killed,
                chooser.getSelectedFirstHit(), false);
        return response;
    }

    public Message battleStringMessage(BattleStringMessage message)
    {
        m_battleDisplay.setStep(message);
        return null;
    }

    public RetreatMessage getRetreat(RetreatQueryMessage rqm)
    {
        return m_battleDisplay.getRetreat(rqm);
    }

    public void notifyRetreat(Collection retreating)
    {
        m_battleDisplay.notifyRetreat(retreating);
    }

    public void bombingResults(BombingResults message)
    {
        m_battleDisplay.bombingResults(message);
    }

    class FightBattleAction extends AbstractAction
    {
        Territory m_territory;
        boolean m_bomb;

        FightBattleAction(Territory battleSite, boolean bomb)
        {
            super((bomb ? "Bombing raid in " : "Battle in ")
                    + battleSite.getName() + "...");
            m_territory = battleSite;
            m_bomb = bomb;
        }

        public void actionPerformed(ActionEvent actionEvent)
        {
            m_fightBattleMessage = new FightBattleDetails(m_bomb, m_territory);
            synchronized (getLock())
            {
                getLock().notify();
            }
        }
    }

    public String toString()
    {
        return "BattlePanel";
    }

    private class BombardComponent extends JPanel
    {

        private JList m_list;

        BombardComponent(Unit unit, Territory unitTerritory, Collection territories, boolean noneAvailable)
        {

            this.setLayout(new BorderLayout());

            String unitName = unit.getUnitType().getName() + " in "
                    + unitTerritory;
            JLabel label = new JLabel("Which territory should " + unitName
                    + " bombard?");
            this.add(label, BorderLayout.NORTH);

            Vector listElements = new Vector(territories);
            if (noneAvailable)
            {
                listElements.add(0, "None");
            }

            m_list = new JList(listElements);
            m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            if (listElements.size() >= 1)
                m_list.setSelectedIndex(0);
            JScrollPane scroll = new JScrollPane(m_list);
            this.add(scroll, BorderLayout.CENTER);
        }

        public Territory getSelection()
        {
            Object selected = m_list.getSelectedValue();
            if (selected instanceof Territory)
            {
                return (Territory) selected;
            }

            return null; // User selected "None" option
        }
    }
}

