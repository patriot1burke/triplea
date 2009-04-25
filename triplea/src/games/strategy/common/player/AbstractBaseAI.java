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

package games.strategy.common.player;

import games.strategy.triplea.ui.UIContext;

/**
 * 
 * @author Lane Schwartz
 */
public abstract class AbstractBaseAI extends AbstractBasePlayer
{

    /** 
     * @param name - the name of the player.
     */
    public AbstractBaseAI(String name)
    {
        super(name);
    }
    
    protected void pause()
    {
        try
        {
            Thread.sleep(UIContext.getAIPauseDuration());
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    
}
