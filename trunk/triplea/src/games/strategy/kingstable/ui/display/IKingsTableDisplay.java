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


package games.strategy.kingstable.ui.display;

import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplay;

import java.util.Collection;

/**
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public interface IKingsTableDisplay extends IDisplay
{
    /**
     * Graphically notify the user of the current game status.
     * @param error the status message to display
     */ 
    public void setStatus(String status);
    
    /**
     * Set the game over status for this display to <code>true</code>.
     */
    public void setGameOver();//CountDownLatch waiting);
    
    /**
     * Ask the user interface for this display to process a play and zero or more captures.
     * 
     * @param start <code>Territory</code> where the play began
     * @param end <code>Territory</code> where the play ended
     * @param captured <code>Collection</code> of <code>Territory</code>s whose pieces were captured during the play
     */
    public void performPlay(Territory start, Territory end, Collection<Territory> captured);
}
