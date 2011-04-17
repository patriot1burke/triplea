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

package games.strategy.triplea.Dynamix_AI.Others;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;

/**
 *
 * @author Stephen
 */
public class TerritoryStatus
{
    public TerritoryStatus()
    {
    }
    public boolean WasAttacked_LandGrab = false;
    public boolean WasAttacked_Stabalize = false;
    public boolean WasAttacked_Offensive = false;
    public boolean WasAttacked_Trade = false;
    public boolean WasReinforced_Block = false;
    public boolean WasReinforced_Stabalize = false;
    public boolean WasReinforced_Frontline = false;
    public boolean WasRetreatedFrom = false;

    public boolean WasAttacked()
    {
        return WasAttacked_LandGrab || WasAttacked_Offensive || WasAttacked_Stabalize || WasAttacked_Trade;
    }

    public boolean WasReinforced()
    {
        return WasReinforced_Block || WasReinforced_Frontline || WasReinforced_Stabalize;
    }

    public void NotifyTaskPerform(CM_Task task)
    {
        if (task.GetTaskType() == task.GetTaskType().LandGrab)
            WasAttacked_LandGrab = true;
        else if (task.GetTaskType() == task.GetTaskType().Attack_Stabilize)
            WasAttacked_Stabalize = true;
        else if (task.GetTaskType() == task.GetTaskType().Attack_Offensive)
            WasAttacked_Offensive = true;
        else if (task.GetTaskType() == task.GetTaskType().Attack_Trade)
            WasAttacked_Trade = true;
    }

    public void NotifyTaskPerform(NCM_Task task)
    {
        if (task.GetTaskType() == task.GetTaskType().Reinforce_Block)
            WasReinforced_Block = true;
        else if (task.GetTaskType() == task.GetTaskType().Reinforce_FrontLine)
            WasReinforced_Frontline = true;
        else if (task.GetTaskType() == task.GetTaskType().Reinforce_Stabilize)
            WasReinforced_Stabalize = true;
    }
}
