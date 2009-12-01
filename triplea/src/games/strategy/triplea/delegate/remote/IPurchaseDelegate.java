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

package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.message.IRemote;
import games.strategy.util.IntegerMap;

import java.util.Map;

/**
 * @author Sean Bridges
 */
public interface IPurchaseDelegate extends IRemote
{
    /**
     * 
     * @param productionRules - units maps ProductionRule -> count
     * @return null if units bought, otherwise an error message
     */
    public String purchase(IntegerMap<ProductionRule> productionRules);
    
    public String purchaseRepair(Map<Territory, IntegerMap<RepairRule>> productionRules);
}
