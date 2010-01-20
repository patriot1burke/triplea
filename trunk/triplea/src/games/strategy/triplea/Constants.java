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
 * Constants.java
 *
 * Created on November 8, 2001, 3:28 PM
 */

package games.strategy.triplea;

/**
 *
 * Constants used throughout the game.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface Constants
{
  //Player names
  public static final String AMERICANS= "Americans";
  public static final String BRITISH  = "British";
  public static final String GERMANS  = "Germans";
  public static final String JAPANESE = "Japanese";
  public static final String RUSSIANS = "Russians";
  public static final String ITALIANS = "Italians";
  public static final String CHINESE = "Chinese";

  public static final String UNIT_ATTATCHMENT_NAME = "unitAttatchment";
  public static final String TECH_ATTATCHMENT_NAME = "techAttatchment";
  public static final String TERRITORY_ATTATCHMENT_NAME = "territoryAttatchment";
  public static final String RULES_ATTATCHMENT_NAME = "rulesAttatchment";
  public static final String RULES_OBJECTIVE_PREFIX = "objectiveAttachment";
  public static final String PLAYER_ATTATCHMENT_NAME = "playerAttatchment";
  public static final String CANAL_ATTATCHMENT_PREFIX = "canalAttatchment";
  public static final String PUS = "PUs";
  public static final String TECH_TOKENS = "techTokens";
  public static final String VPS = "VPs";
  public static final int    MAX_DICE = 6;
  public static final String NEUTRAL_CHARGE_PROPERTY = "neutralCharge";
  public static final String FACTORIES_PER_COUNTRY_PROPERTY ="maxFactoriesPerTerritory";
  public static final String TWO_HIT_BATTLESHIP_PROPERTY = "Two hit battleship";
  public static final String ALWAYS_ON_AA_PROPERTY = "Always on AA";
  //allows lhtr carrier/fighter production
  public static final String LHTR_CARRIER_PRODUCTION_RULES = "LHTR Carrier production rules";
  //Break up fighter/carrier production into atomic units
  //allow fighters to be placed on newly produced carriers
  public static final String CAN_PRODUCE_FIGHTERS_ON_CARRIERS = "Produce fighters on carriers";
  public static final String PRODUCE_NEW_FIGHTERS_ON_OLD_CARRIERS = "Produce new fighters on old carriers";
  public static final String MOVE_EXISTING_FIGHTERS_TO_NEW_CARRIERS = "Move existing fighters to new carriers";
  public static final String LAND_EXISTING_FIGHTERS_ON_NEW_CARRIERS = "Land existing fighters on new carriers";
  
  
  public static final String HEAVY_BOMBER_DICE_ROLLS = "Heavy Bomber Dice Rolls";
  public static final String TWO_HIT_BATTLESHIPS_REPAIR_EACH_TURN = "Battleships repair at end of round";
  public static final String WW2V2 = "WW2V2";
  public static final String TOTAL_VICTORY = "Total Victory";
  public static final String HONORABLE_SURRENDER = "Honorable Surrender";
  public static final String PROJECTION_OF_POWER = "Projection of Power";  
  public static final String ALL_ROCKETS_ATTACK = "All Rockets Attack";  
  public static final String ROCKETS_CAN_VIOLATE_NEUTRALITY = "Rockets Can Violate Neutrality";
  public static final String ROCKETS_CAN_FLY_OVER_IMPASSABLES = "Rockets Can Fly Over Impassables";
  public static final String NEUTRALS_ARE_IMPASSABLE = "Neutrals Are Impassable";
  public static final String NEUTRALS_ARE_BLITZABLE = "Neutrals Are Blitzable";  
  public static final String PARTIAL_AMPHIBIOUS_RETREAT = "Partial Amphibious Retreat";
  public static final String PREVIOUS_UNITS_FIGHT = "Previous Units Fight";
  
  /**
   * These are the individual rules from a game (All default to FALSE)
   */
  public static final String PLACEMENT_RESTRICTED_BY_FACTORY = "Placement Restricted By Factory";
  public static final String SELECTABLE_TECH_ROLL = "Selectable Tech Roll";
  public static final String WW2V3_Tech_Model = "WW2V3 Tech Model";  
  public static final String TECH_DEVELOPMENT = "Tech Development";  
  public static final String TRANSPORT_UNLOAD_RESTRICTED = "Transport Restricted Unload";
  public static final String RANDOM_AA_CASUALTIES = "Random AA Casualties";
  public static final String ROLL_AA_INDIVIDUALLY = "Roll AA Individually";  
  public static final String LIMIT_SBR_DAMAGE_TO_PRODUCTION = "Limit SBR Damage To Factory Production";
  public static final String LIMIT_ROCKET_DAMAGE_TO_PRODUCTION = "Limit SBR Damage To Factory Production";
  public static final String SBR_VICTORY_POINTS = "SBR Victory Points";
  public static final String ROCKET_ATTACK_PER_FACTORY_RESTRICTED = "Rocket Attack Per Factory Restricted";
  public static final String LIMIT_SBR_DAMAGE_PER_TURN = "Limit SBR Damage Per Turn";
  public static final String LIMIT_ROCKET_DAMAGE_PER_TURN = "Limit Rocket Damage Per Turn";
  public static final String ALLIED_AIR_DEPENDENTS = "Allied Air Dependents";
  public static final String DEFENDING_SUBS_SNEAK_ATTACK = "Defending Subs Sneak Attack";
  public static final String ATTACKER_RETREAT_PLANES = "Attacker Retreat Planes";
  public static final String NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE_RESTRICTED = "Naval Bombard Casualties Return Fire Restricted";
  public static final String SURVIVING_AIR_MOVE_TO_LAND = "Surviving Air Move To Land";
  public static final String BLITZ_THROUGH_FACTORIES_AND_AA_RESTRICTED = "Blitz Through Factories And AA Restricted";
  public static final String AIR_ATTACK_SUB_RESTRICTED = "Air Attack Sub Restricted";
  /**
   * End individual rules (All default to FALSE)
   */
  

  /**
   * These are the individual rules for TripleA WW2V3 (All default to FALSE)
   */
  public static final String NATIONAL_OBJECTIVES = "National Objectives";
  public static final String SUB_CONTROL_SEA_ZONE_RESTRICTED = "Sub Control Sea Zone Restricted";
  public static final String UNIT_PLACEMENT_IN_ENEMY_SEAS = "Unit Placement In Enemy Seas";  
  public static final String TRANSPORT_CONTROL_SEA_ZONE = "Transport Control Sea Zone";
  public static final String PRODUCTION_PER_X_TERRITORIES_RESTRICTED = "Production Per X Territories Restricted";
  public static final String PLACE_IN_ANY_TERRITORY = "Place in Any Territory";
  public static final String UNIT_PLACEMENT_PER_TERRITORY_RESTRICTED = "Unit Placement Per Territory Restricted";  
  public static final String MOVEMENT_BY_TERRITORY_RESTRICTED = "Movement By Territory Restricted";
  public static final String TRANSPORT_CASUALTIES_RESTRICTED = "Transport Casualties Restricted";
  public static final String SUB_RETREAT_BEFORE_BATTLE = "Sub Retreat Before Battle"; // may be SUBMERSIBLE_SUBS below
  public static final String SUB_RETREAT_DD_RESTRICTED = "Sub Retreat DD Restricted"; // may not be needed
  public static final String SHORE_BOMBARD_PER_GROUND_UNIT_RESTRICTED = "Shore Bombard Per Ground Unit Restricted";
  public static final String SBR_AFFECTS_UNIT_PRODUCTION = "SBR Affects Unit Production";
  public static final String AA_TERRITORY_RESTRICTED = "AA Territory Restricted";
  public static final String MULTIPLE_AA_PER_TERRITORY = "Multiple AA Per Territory";
  public static final String IGNORE_TRANSPORT_IN_MOVEMENT = "Ignore Transport In Movement";
  public static final String IGNORE_SUB_IN_MOVEMENT = "Ignore Sub In Movement";  
  public static final String HARI_KARI_UNITS = "Hari-Kari Units";
  public static final String CONTINUOUS_RESEARCH = "Continuous Research";
  public static final String WW2V3_LAND_PRODUCTION = "WW2V3 Land Production";
  public static final String WW2V3_AIR_NAVAL = "WW2V3 Air Naval";
  public static final String HARI_KARI = "Hari-Kari Units";  
  public static final String UNPLACED_UNITS_LIVE = "Unplaced units live when not placed";
  /**
   * End individual rules for TripleA WW2V3 (All default to FALSE)
   */

  public static final String PRODUCTION_PER_VALUED_TERRITORY_RESTRICTED = "Production Per Valued Territory Restricted";
  public static final String CHOOSE_AA = "Choose AA Casualties";
  public static final String PACIFIC_THEATER = "Pacific Theater";
  public static final String WW2V3 = "WW2V3";  
  public static final String EUROPE_THEATER = "Europe Theater";
  public static final String ECONOMIC_VICTORY = "Economic Victory";
  
  public static final String SUBMERSIBLE_SUBS = "Submersible Subs";
  public static final String TWO_HIT = "isTwoHit";
  public static final String ORIGINAL_OWNER = "originalOwner";
  public static final String USE_DESTROYERS_AND_ARTILLERY = "Use Destroyers and Artillery";
  public static final String USE_SHIPYARDS = "Use Shipyards";
  public static final String LOW_LUCK = "Low Luck";
  public static final String PU_CAP = "Territory Turn Limit";
  public static final String KAMIKAZE = "Kamikaze Airplanes";
  public static final String LHTR_HEAVY_BOMBERS = "LHTR Heavy Bombers";
  public static final String EDIT_MODE = "EditMode";
  
  //by defaul this is 0, but for lhtr, it is 1
  public static final String SUPER_SUB_DEFENSE_BONUS = "Super Sub Defence Bonus";
  
  public static final int TECH_ROLL_COST = 5;

  public static final String INFANTRY_TYPE = "infantry";
  public static final String ARMOUR_TYPE = "armour";
  public static final String TRANSPORT_TYPE = "transport";
  public static final String SUBMARINE_TYPE = "submarine";
  public static final String BATTLESHIP_TYPE = "battleship";
  public static final String CARRIER_TYPE = "carrier";
  public static final String FIGHTER_TYPE = "fighter";
  public static final String BOMBER_TYPE = "bomber";
  public static final String FACTORY_TYPE = "factory";
  public static final String AAGUN_TYPE = "aaGun";
  public static final String ARTILLERY = "artillery";
  public static final String DESTROYER = "destroyer";

  public static final String LARGE_MAP_FILENAME = "largeMap.gif";
  public static final String SMALL_MAP_FILENAME = "smallMap.jpeg";
  public static final String MAP_NAME = "mapName";
  public static final String DISPLAY_SEA_NAMES = "Display Sea Names";
  
  public static final String SHOW_ENEMY_CASUALTIES_USER_PREF = "ShowEnemyCasualties";

}
