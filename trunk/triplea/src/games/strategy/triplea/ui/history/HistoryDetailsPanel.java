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

package games.strategy.triplea.ui.history;

import javax.swing.*;
import java.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.history.*;
import games.strategy.triplea.delegate.*;
import java.awt.*;
import games.strategy.triplea.ui.*;
import games.strategy.triplea.util.*;
import games.strategy.triplea.delegate.dataObjects.*;

public class HistoryDetailsPanel extends JPanel
{
    private final GameData m_data;

    private JTextArea m_title = new JTextArea();

    private JScrollPane m_scroll = new JScrollPane(m_title);

    private final MapPanel m_mapPanel;

    public HistoryDetailsPanel(GameData data, MapPanel mapPanel)
    {
        m_data = data;
        setLayout(new GridBagLayout());
        m_title.setWrapStyleWord(true);
        m_title.setBackground(this.getBackground());
        m_title.setLineWrap(true);
        m_title.setBorder(null);
        m_title.setEditable(false);
        m_scroll.setBorder(null);
        m_mapPanel = mapPanel;
    }

    @SuppressWarnings("unchecked")
    public void render(HistoryNode node)
    {
        removeAll();
        m_mapPanel.setRoute(null);

        Insets insets = new Insets(5, 0, 0, 0);

        m_title.setText(node.getTitle());
        add(m_scroll,
                new GridBagConstraints(0, 0, 1, 1, 1, 0.1,
                        GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                        insets, 0, 0));

        GridBagConstraints mainConstraints = new GridBagConstraints(0, 1, 1, 1,
                1, 0.9, GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                insets, 0, 0);

        if (node instanceof Renderable)
        {
            Object details = ((Renderable) node).getRenderingData();
            if (details instanceof DiceRoll)
            {
                DicePanel dicePanel = new DicePanel(m_mapPanel.getUIContext());
                dicePanel.setDiceRoll((DiceRoll) details);
                add(dicePanel, mainConstraints);
            } else if (details instanceof MoveDescription)
            {
                MoveDescription moveMessage = (MoveDescription) details;
                renderUnits(mainConstraints, moveMessage.getUnits());
                m_mapPanel.setRoute(moveMessage.getRoute());
                if (!m_mapPanel.isShowing(moveMessage.getRoute().getEnd()))
                    m_mapPanel.centerOn(moveMessage.getRoute().getEnd());
            } else if (details instanceof Collection)
            {
                Collection objects = (Collection)details;
                Iterator objIter = objects.iterator();
                if(objIter.hasNext())
                {
                    Object obj = objIter.next();
                    if(obj instanceof Unit)
                        renderUnits(mainConstraints, objects);
                }
            } else if (details instanceof Territory)
            {
                Territory t = (Territory) details;
                if (!m_mapPanel.isShowing(t))
                    m_mapPanel.centerOn(t);

            }

        }
        add(Box.createGlue());

        validate();
        repaint();

    }

    private void renderUnits(GridBagConstraints mainConstraints,
            Collection<Unit> units)
    {
        Collection unitsCategories = UnitSeperator.categorize(units);
        SimpleUnitPanel unitsPanel = new SimpleUnitPanel(m_mapPanel
                .getUIContext());
        unitsPanel.setUnitsFromCategories(unitsCategories, m_data);
        add(unitsPanel, mainConstraints);
    }

}
