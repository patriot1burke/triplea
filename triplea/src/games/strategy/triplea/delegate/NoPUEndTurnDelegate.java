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
 * NOPUEndTurnDelegate.java
 *
 * Created on August 11, 205, 2:16 PM
 */

package games.strategy.triplea.delegate;

import java.util.Collection;
import games.strategy.engine.delegate.AutoSave;

/**
 *
 * @author  Adam Jette
 * @version 1.0
 *
 * At the end of the turn collect NO income.
 */
@AutoSave(afterStepEnd=true)
public class NoPUEndTurnDelegate extends EndTurnDelegate
{
    protected int getProduction(Collection territories)
    {
        return 0;
    } 
}
