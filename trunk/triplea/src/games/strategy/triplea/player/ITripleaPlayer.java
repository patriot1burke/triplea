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

package games.strategy.triplea.player;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IRemote;
import games.strategy.net.*;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;

/**
 * Interface the TriplePlayer presents to Delegates through IRemoteMessenger
 * 
 * @author Sean Bridges
 */
public interface ITripleaPlayer extends IRemote
{

    /**
     * Select casualties
     * 
     * @param selectFrom - the units to select casualties from
     * @param dependents - dependents of the units to select from
     * @param count - the number of casualties to select
     * @param message - ui message to display
     * @param hit - the player hit
     * @param dice - the dice rolled for the casualties 
     * @param defaultCasualties - default casualties as selected by the game
     * @param battleID - the battle we are fighting in, may be null if this is an aa casualty selection during a move
     * @return the selected casualties
     * 
     * Added new collection autoKilled to handle killing units prior to casualty selection
     */

    public CasualtyDetails selectCasualties(
            Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents,  int count, String message, DiceRoll dice, PlayerID hit, List<Unit> defaultCasualties, GUID battleID     
    );
    /**
     * Select a fixed dice roll
     * 
     * @param numDice - the number of dice rolls
     * @param hitAt - the lowest roll that constitutes a hit (0 for none)
     * @param hitOnlyIfEquals - whether to count rolls greater than hitAt as hits
     * @param title - the title for the DiceChooser
     * @return the resulting dice array
     */
    public int[] selectFixedDice(int numDice, int hitAt, boolean hitOnlyIfEquals, String title);
    
    /**
     * Select the territory to bombard with the bombarding capable unit (eg battleship)
     * 
     * @param unit - the bombarding unit
     * @param unitTerritory - where the bombarding unit is
     * @param territories - territories where the unit can bombard
     * @param noneAvailable 
     * @return the Territory to bombard in, null if the unit should not bombard
     */
    public Territory selectBombardingTerritory(
            Unit unit,
            Territory unitTerritory,
            Collection<Territory> territories,
            boolean noneAvailable       
    );
    
    /**
     * Ask if the player wants to attack lone subs
     * 
     * @param unitTerritory - where the potential battle is
     */
    public boolean selectAttackSubs(
            Territory unitTerritory    
    );
    
    /**
     * Ask if the player wants to attack lone transports
     * 
     * @param unitTerritory - where the potential battle is
     */
    public boolean selectAttackTransports(
            Territory unitTerritory    
    );

    /**
     * Ask if the player wants to attack units
     * 
     * @param unitTerritory - where the potential battle is
     */
    public boolean selectAttackUnits(
            Territory unitTerritory    
    );

    /**
     * Ask if the player wants to shore bombard
     * 
     * @param unitTerritory - where the potential battle is
     */
    public boolean selectShoreBombard(
            Territory unitTerritory    
    );
    
    /**
     * Report an error to the user. 
     * 
     * @param report that an error occurred
     */
    public void reportError(String error);
    
    /**
     * report a message to the user
     * @param message
     */
    public void reportMessage(String message);
    
    /**
     * One or more bombers have just moved into a territory where a strategic bombing
     * raid can be conducted, should the bomber bomb? 
     */
    public boolean shouldBomberBomb(Territory territory);
    
    /**
     * Choose where my rockets should fire
     * 
     * @param candidates  - a collection of Territories,  the possible territories to attack
     * @param from - where the rockets are launched from, null for WW2V1 rules
     * @return the territory to attack, null if no territory should be attacked
     */
    public Territory whereShouldRocketsAttack(Collection<Territory> candidates, Territory from);
    
    /**
     * get the fighters to move to a newly produced carrier
     * 
     * @param fightersThatCanBeMoved - the fighters that can be moved
     * @param from - the territory containing the factory
     * @return - the fighters to move
     */
    public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(Collection<Unit> fightersThatCanBeMoved, Territory from);
 
    /**
     * Some carriers were lost while defending.  We must select where to land
     * some air units.
     * 
     * @param candidates -  a list of territories - these are the places where air units can land
     * @return - the territory to land the fighters in, must be non null
     */
    public Territory selectTerritoryForAirToLand(Collection<Territory> candidates);

    
    /**
     * The attempted move will incur aa fire, confirm that you still want to move
     * 
     * @param  aaFiringTerritories - the territories where aa will fire
     */
    public boolean confirmMoveInFaceOfAA(Collection<Territory> aaFiringTerritories);

    /**
     * The attempted move will kill some air units
     * 
     */
    public boolean confirmMoveKamikaze();

    /**
     * The attempted move will kill some units
     * 
     */
    public boolean confirmMoveHariKari();

    /**
     * 
     * Ask the player if he wishes to retreat.
     * 
     * @param battleID - the battle
     * @param submerge - is submerging possible
     * @param possibleTerritories - where the player can retreat to
     * @param message - user displayable message
     * @return the territory to retreat to, or null if the player doesnt wish to retreat
     */
    public Territory retreatQuery(GUID battleID, boolean submerge, Collection<Territory> possibleTerritories, String message);

    
    /**
     * Allows the user to pause and confirm enemy casualties
     * 
     * @param battleId
     * @param message
     */
    public void confirmEnemyCasualties(GUID battleId, String message, PlayerID hitPlayer);
    
    public void confirmOwnCasualties(GUID battleId, String message);

    public PlayerID getID();

}
