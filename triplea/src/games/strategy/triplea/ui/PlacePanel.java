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
 * PlacePanel.java
 *
 * Created on December 4, 2001, 7:45 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.*;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class PlacePanel extends ActionPanel
{

    private JLabel actionLabel = new JLabel();

    private PlaceData m_placeData;

    private SimpleUnitPanel m_unitsToPlace;

    private IPlayerBridge m_bridge;

    /** Creates new PlacePanel */
    public PlacePanel(GameData data, MapPanel map)
    {
        super(data, map);
        m_unitsToPlace = new SimpleUnitPanel(map.getUIContext());
    }

    public void display(final PlayerID id)
    {
        super.display(id);
        
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                removeAll();
                actionLabel.setText(id.getName() + " place");
                add(actionLabel);
                add(new JButton(UNDO_PLACE_ACTION));
                add(new JButton(DONE_PLACE_ACTION));
                SwingUtilities.invokeLater(REFRESH);

                add(new JLabel("Units left to place:"));
                add(m_unitsToPlace);
                updateUnits();
                
            }
        });
    }

    private void refreshUndoButton() throws NumberFormatException
    {
        final IAbstractPlaceDelegate placeDel = (IAbstractPlaceDelegate) m_bridge
                .getRemote();
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                UNDO_PLACE_ACTION.setEnabled(placeDel.getPlacementsMade() > 0);
            }
        
        });
        
    }

    private void refreshActionLabelText(final boolean bid)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                actionLabel.setText(getCurrentPlayer().getName() + " place"
                        + (bid ? " for bid" : ""));
            }
        
        });
        
    }

    public PlaceData waitForPlace(boolean bid, IPlayerBridge bridge)
    {
        m_bridge = bridge;
        refreshActionLabelText(bid);
        refreshUndoButton();
        
        getMap().addMapSelectionListener(PLACE_MAP_SELECTION_LISTENER);

        waitForRelease();

        getMap().removeMapSelectionListener(
                PLACE_MAP_SELECTION_LISTENER);
        m_bridge = null;
        
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                removeAll();
                SwingUtilities.invokeLater(REFRESH);
            }
        });
        
        return m_placeData;
    }

    private final AbstractAction UNDO_PLACE_ACTION = new AbstractAction(
            "Undo Last Placement")
    {
        public void actionPerformed(ActionEvent e)
        {
            IAbstractPlaceDelegate placeDel = (IAbstractPlaceDelegate) m_bridge
                    .getRemote();
            placeDel.undoLastPlacement();
            refreshUndoButton();
            updateUnits();
            validate();
        }
    };

    private final AbstractAction DONE_PLACE_ACTION = new AbstractAction("Done")
    {
        public void actionPerformed(ActionEvent e)
        {
            if (getCurrentPlayer().getUnits().size() > 0)
            {
                int option = JOptionPane
                        .showConfirmDialog(
                                (JFrame) getTopLevelAncestor(),
                                "You have not placed all your units yet.  Are you sure you want to end your turn?",
                                "TripleA", JOptionPane.YES_NO_OPTION,
                                JOptionPane.PLAIN_MESSAGE);
                //TODO COMCO add code here to store the units until next time
                if (option != JOptionPane.YES_OPTION)
                    return;
            }

            m_placeData = null;

            release();

        }
    };

    private boolean canProduceFightersOnCarriers()
    {
        return games.strategy.triplea.Properties.getProduce_Fighters_On_Carriers(getData());        
    }

    private boolean canProduceNewFightersOnOldCarriers()
    {    	
        return games.strategy.triplea.Properties.getProduce_New_Fighters_On_Old_Carriers(getData());        
    }
    
    private final MapSelectionListener PLACE_MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
    {
        public void territorySelected(Territory territory, MouseDetails e)
        {
            if (!getActive() || (e.getButton() != MouseEvent.BUTTON1))
                return;

            int maxUnits[] = new int[1];
            Collection<Unit> units = getUnitsToPlace(territory, maxUnits);
            if (units.isEmpty())
                return;

            UnitChooser chooser = new UnitChooser(units, Collections
                    .<Unit, Collection<Unit>> emptyMap(), getData(), false,
                    getMap().getUIContext());
            String messageText = "Place units in " + territory.getName();
            if (maxUnits[0] > 0)
                chooser.setMaxAndShowMaxButton(maxUnits[0]);

            int option = JOptionPane.showOptionDialog(
                    getTopLevelAncestor(), chooser, messageText,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, null, null);
            if (option == JOptionPane.OK_OPTION)
            {
                Collection<Unit> choosen = chooser.getSelected();

                m_placeData = new PlaceData(choosen, territory);
                updateUnits();
                release();
            }
        }
    };

    private Collection<Unit> getUnitsToPlace(Territory territory,
            int maxUnits[])
    {
        
        getData().acquireReadLock();
        try
        {
            // not our territory
            if (!territory.isWater()
                    && !territory.getOwner().equals(getCurrentPlayer()))
                return Collections.emptyList();
    
            // get the units that can be placed on this territory.
            Collection<Unit> units = getCurrentPlayer().getUnits().getUnits();
            if (territory.isWater())
            {
                if (!canProduceFightersOnCarriers() && !canProduceNewFightersOnOldCarriers())
                    units = Match.getMatches(units, Matches.UnitIsSea);
                else
                {
                    CompositeMatch<Unit> unitIsSeaOrCanLandOnCarrier = new CompositeMatchOr<Unit>(
                            Matches.UnitIsSea, Matches.UnitCanLandOnCarrier);
                    units = Match.getMatches(units, unitIsSeaOrCanLandOnCarrier);
                }
            } else
                units = Match.getMatches(units, Matches.UnitIsNotSea);
    
            if (units.isEmpty())
                return Collections.emptyList();
    
            IAbstractPlaceDelegate placeDel = (IAbstractPlaceDelegate) m_bridge
                    .getRemote();
    
            PlaceableUnits production = placeDel
                    .getPlaceableUnits(units, territory);
    
            if (production.isError())
            {
                JOptionPane.showMessageDialog(getTopLevelAncestor(), production
                        .getErrorMessage(), "No units",
                        JOptionPane.INFORMATION_MESSAGE);
                return Collections.emptyList();
            }
    
            maxUnits[0] = production.getMaxUnits();
            return production.getUnits();
        }
        finally 
        {
            getData().releaseReadLock();
        }
    }

    private void updateUnits()
    {
        Collection unitCategories = UnitSeperator.categorize(getCurrentPlayer()
                .getUnits().getUnits());
        m_unitsToPlace.setUnitsFromCategories(unitCategories, getData());
    }

    public String toString()
    {
        return "PlacePanel";
    }

}
