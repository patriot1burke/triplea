package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * Pro AI battle utilities.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProTerritoryValueUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	
	public ProTerritoryValueUtils(final ProAI ai, final ProUtils utils, final ProBattleUtils battleUtils)
	{
		this.ai = ai;
		this.utils = utils;
		this.battleUtils = battleUtils;
	}
	
	public double findTerritoryAttackValue(final PlayerID player, final Territory t, final double minCostPerHitPoint)
	{
		final GameData data = ai.getGameData();
		
		final int isEnemyFactory = ProMatches.territoryHasInfraFactoryAndIsEnemyLand(player, data).match(t) ? 1 : 0;
		double value = 3 * TerritoryAttachment.getProduction(t) * (isEnemyFactory + 1);
		if (!t.isWater() && t.getOwner().isNull())
		{
			final double strength = battleUtils.estimateStrength(t.getOwner(), t, new ArrayList<Unit>(t.getUnits().getUnits()), new ArrayList<Unit>(), false);
			final double TUVSwing = -(strength / 8) * minCostPerHitPoint; // estimate TUV swing as number of casualties * cost
			value += TUVSwing;
		}
		
		return value;
	}
	
	public Map<Territory, Double> findTerritoryValues(final PlayerID player, final List<Territory> territoriesThatCantBeHeld, final List<Territory> territoriesToAttack)
	{
		final GameData data = ai.getGameData();
		final List<Territory> allTerritories = data.getMap().getTerritories();
		
		// Get all enemy factories and capitals (check if most territories have factories and if so remove them)
		final Set<Territory> enemyCapitalsAndFactories = new HashSet<Territory>();
		enemyCapitalsAndFactories.addAll(Match.getMatches(allTerritories, ProMatches.territoryHasInfraFactoryAndIsEnemyLand(player, data)));
		final int numEnemyLandTerritories = Match.countMatches(allTerritories, ProMatches.territoryIsEnemyLand(player, data));
		if (enemyCapitalsAndFactories.size() * 2 >= numEnemyLandTerritories)
			enemyCapitalsAndFactories.clear();
		enemyCapitalsAndFactories.addAll(utils.getLiveEnemyCapitals(data, player));
		enemyCapitalsAndFactories.removeAll(territoriesToAttack);
		
		// Loop through factories/capitals and find value
		final Map<Territory, Double> enemyCapitalsAndFactoriesMap = new HashMap<Territory, Double>();
		for (final Territory t : enemyCapitalsAndFactories)
		{
			// Get factory production if factory
			int factoryProduction = 0;
			if (ProMatches.territoryHasInfraFactoryAndIsEnemyLand(player, data).match(t))
				factoryProduction = TerritoryAttachment.getProduction(t);
			
			// Get player production if capital
			double playerProduction = 0;
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta != null && ta.isCapital())
				playerProduction = utils.getPlayerProduction(t.getOwner(), data);
			
			// Check if neutral
			final int isNeutral = t.getOwner().isNull() ? 1 : 0;
			
			// Calculate value
			final double value = factoryProduction * 4 / (1 + isNeutral) + playerProduction;
			enemyCapitalsAndFactoriesMap.put(t, value);
		}
		
		// Determine value for land territories
		final Map<Territory, Double> territoryValueMap = new HashMap<Territory, Double>();
		for (final Territory t : allTerritories)
		{
			if (!t.isWater() && !territoriesThatCantBeHeld.contains(t))
			{
				// Determine value based on enemy factory land distance
				double capitalOrFactoryValue = 0;
				for (final Territory enemyCapitalOrFactory : enemyCapitalsAndFactoriesMap.keySet())
				{
					final int distance = data.getMap().getDistance(t, enemyCapitalOrFactory, ProMatches.territoryCanMoveLandUnits(player, data, true));
					if (distance > 0)
						capitalOrFactoryValue += (enemyCapitalsAndFactoriesMap.get(enemyCapitalOrFactory) / Math.pow(2, distance));
				}
				
				// Determine value based on nearby territory production
				double nearbyEnemyValue = 0;
				final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveLandUnits(player, data, true));
				final List<Territory> nearbyEnemyTerritories = Match.getMatches(nearbyTerritories, ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
				nearbyEnemyTerritories.removeAll(territoriesToAttack);
				for (final Territory nearbyEnemyTerritory : nearbyEnemyTerritories)
				{
					final int distance = data.getMap().getDistance(t, nearbyEnemyTerritory, ProMatches.territoryCanMoveLandUnits(player, data, true));
					if (distance > 0)
					{
						final int isNeutral = nearbyEnemyTerritory.getOwner().isNull() ? 1 : 0;
						nearbyEnemyValue += TerritoryAttachment.getProduction(nearbyEnemyTerritory) / (isNeutral + 1) / Math.pow(2, distance);
					}
				}
				final double value = capitalOrFactoryValue + nearbyEnemyValue;
				territoryValueMap.put(t, value);
			}
			else if (!t.isWater())
			{
				territoryValueMap.put(t, 0.0);
			}
		}
		
		// Determine value for water territories
		for (final Territory t : allTerritories)
		{
			if (!territoriesThatCantBeHeld.contains(t) && t.isWater() && !data.getMap().getNeighbors(t, Matches.TerritoryIsWater).isEmpty())
			{
				// Determine value based on enemy factory distance
				double capitalOrFactoryValue = 0;
				for (final Territory enemyCapitalOrFactory : enemyCapitalsAndFactoriesMap.keySet())
				{
					final int distance = data.getMap().getDistance_IgnoreEndForCondition(t, enemyCapitalOrFactory, Matches.TerritoryIsWater);
					if (distance > 0)
						capitalOrFactoryValue += (enemyCapitalsAndFactoriesMap.get(enemyCapitalOrFactory) / Math.pow(3, distance));
				}
				
				// Determine value based on nearby territory production
				double nearbyLandValue = 0;
				final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, 3);
				final List<Territory> nearbyLandTerritories = Match.getMatches(nearbyTerritories, ProMatches.territoryCanMoveLandUnits(player, data, false));
				nearbyLandTerritories.removeAll(territoriesToAttack);
				for (final Territory nearbyLandTerritory : nearbyLandTerritories)
				{
					final int distance = data.getMap().getDistance_IgnoreEndForCondition(t, nearbyLandTerritory, Matches.TerritoryIsWater);
					if (distance > 0 && distance <= 3)
					{
						final int isNeutral = nearbyLandTerritory.getOwner().isNull() ? 1 : 0;
						if (ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld).match(nearbyLandTerritory))
							nearbyLandValue += (double) TerritoryAttachment.getProduction(nearbyLandTerritory) / (isNeutral + 1);
						nearbyLandValue += territoryValueMap.get(nearbyLandTerritory);
					}
				}
				
				final double value = capitalOrFactoryValue + nearbyLandValue;
				territoryValueMap.put(t, value);
			}
			else if (t.isWater())
			{
				territoryValueMap.put(t, 0.0);
			}
		}
		return territoryValueMap;
	}
	
	public Map<Territory, Double> findSeaTerritoryValues(final PlayerID player, final List<Territory> territoriesThatCantBeHeld)
	{
		final GameData data = ai.getGameData();
		final List<Territory> allTerritories = data.getMap().getTerritories();
		
		// Determine value for water territories
		final Map<Territory, Double> territoryValueMap = new HashMap<Territory, Double>();
		for (final Territory t : allTerritories)
		{
			if (!territoriesThatCantBeHeld.contains(t) && t.isWater() && !data.getMap().getNeighbors(t, Matches.TerritoryIsWater).isEmpty())
			{
				// Determine sea value based on nearby convoy production
				double nearbySeaValue = 0;
				final Set<Territory> nearbySeaTerritories = data.getMap().getNeighbors(t, 4, ProMatches.territoryCanMoveSeaUnits(player, data, true));
				final List<Territory> nearbyEnemySeaTerritories = Match.getMatches(nearbySeaTerritories, ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
				for (final Territory nearbyEnemySeaTerritory : nearbyEnemySeaTerritories)
				{
					final int distance = data.getMap().getDistance(t, nearbyEnemySeaTerritory, ProMatches.territoryCanMoveSeaUnits(player, data, true));
					if (distance > 0)
						nearbySeaValue += TerritoryAttachment.getProduction(nearbyEnemySeaTerritory) / Math.pow(2, distance);
				}
				
				// Set final values
				territoryValueMap.put(t, nearbySeaValue);
			}
			else if (t.isWater())
			{
				territoryValueMap.put(t, 0.0);
			}
		}
		return territoryValueMap;
	}
	
}
