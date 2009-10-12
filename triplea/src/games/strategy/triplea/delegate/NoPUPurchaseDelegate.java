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
 * NOPUPurchaseDelegate.java
 *
 * Created on August 11, 2005, 10:38 AM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;

import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author  Adam Jette
 * @version 1.0
 *
 * At the end of the turn collect units, not income!
 */
public class NoPUPurchaseDelegate extends PurchaseDelegate
{
    String unitTypeToProduce = Constants.INFANTRY_TYPE;
    private GameData m_data;

    private boolean isPacific;
    private boolean isWW2V3;

    public void start(IDelegateBridge aBridge, GameData gameData)
    {
        super.start(aBridge, gameData);
        m_data = gameData;
        
        isPacific = isPacificTheater();
        isWW2V3 = isWW2V3();

        PlayerID player = aBridge.getPlayerID();
        Collection<Territory> territories = gameData.getMap().getTerritoriesOwnedBy(player);

        if(isPacific || isWW2V3)
            unitTypeToProduce = Constants.INFANTRY_TYPE;
        //unitTypeToProduce = Constants.CHINESE_INFANTRY_TYPE;

        int nUnitsToProduce = getProductionUnits(territories, player);
        Collection<Unit> units = gameData.getUnitTypeList().getUnitType(unitTypeToProduce).create(nUnitsToProduce, player);

        Change productionChange = ChangeFactory.addUnits(player, units);

        String transcriptText = player.getName() + " builds " + nUnitsToProduce + " " + unitTypeToProduce;
        aBridge.getHistoryWriter().startEvent(transcriptText);

        if(productionChange != null)
        {
            aBridge.addChange(productionChange);
        } 
    }

    // this is based off of chinese rules in pacific, they may vary in other games?
    private int getProductionUnits(Collection<Territory> territories, PlayerID player)
    {
        // All territories should be owned by the same player, our PlayerID
        int unitCount = 0;

        if (isProductionPerValuedTerritoryRestricted())
        {
	        Iterator<Territory> territoryIter = territories.iterator();
	        for(int i = 0; (territoryIter.hasNext()); ++i)
	        {
	            Territory current = (Territory) territoryIter.next();

	            TerritoryAttachment ta = TerritoryAttachment.get(current);
	            if(ta.getProduction() > 0)
	                ++unitCount;
	        }
	        
	        if(isPacific)
	            unitCount += getBurmaRoad(player);
        }
        
        if(isProductionPerXTerritoriesRestricted())
        {
        	RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
        	if(ra != null)
        	{
        		int prodPerXTerrs = ra.getProductionPerXTerritories();
        		if (prodPerXTerrs > 0)
        		{
            		int terrCount = territories.size();
            		unitCount = terrCount/prodPerXTerrs;    		        
        		}
        	}
        }
        
        return unitCount;
    }
//TODO COMCO incorporate this into a National Objective
    private int getBurmaRoad(PlayerID player)
    {
        int burmaRoadCount = 0; // only for pacific - should equal 4 for extra inf
        Iterator iter = m_data.getMap().getTerritories().iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            String terrName = current.getName();
            if((terrName.equals("Burma") || terrName.equals("India") || terrName.equals("Yunnan") || terrName.equals("Szechwan")) && m_data.getAllianceTracker().isAllied(current.getOwner(), player))
                ++burmaRoadCount;
        }

        if(burmaRoadCount == 4)
            return 1;
        return 0; 
    }

    private boolean isPacificTheater()
    {
    	return games.strategy.triplea.Properties.getPacificTheater(m_data);
    }

    private boolean isWW2V3()
    {
    	return games.strategy.triplea.Properties.getWW2V3(m_data);
    }

    private boolean isProductionPerValuedTerritoryRestricted()
    {
    	return games.strategy.triplea.Properties.getProductionPerValuedTerritoryRestricted(m_data);
    }

    private boolean isProductionPerXTerritoriesRestricted()
    {
    	return games.strategy.triplea.Properties.getProductionPerXTerritoriesRestricted(m_data);
    }

}
