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
 * Matches.java
 *
 * Created on November 8, 2001, 4:29 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.util.*;

/**
 * Useful match interfaces.
 * 
 * Rather than writing code like,
 * 
 * 
 * <pre>
 * boolean hasLand = false;
 * Iterator iter = someCollection.iterator();
 * 
 * while(iter.hasNext())
 * {
 * 	  Unit unit = (Unit) iter.next();
 *    UnitAttatchment ua = UnitAttatchment.get(unit.getType());
 *	  if(ua.isAir)
 *	  {
 *	    hasAir = true;
 *	    break;
 *	  }
 * }
 * 
 * </pre>
 * 
 * You can write code like,
 * 
 * boolean hasLand = Match.someMatch(someCollection, Matches.UnitIsAir);
 * 
 * 
 * The benefits should be obvious to any right minded person.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class Matches
{

    public static final Match IsUnit = new Match()
    {
      public boolean match(Object o)
      {
          return o != null && o instanceof Unit;
      }
    };

    public static final Match IsTerritory = new Match()
    {
        public boolean match(Object o)
        {
            return o != null && o instanceof Territory;
        }
    };

	public static final Match UnitIsTwoHit = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.isTwoHit();
		}
	};

	public static final Match UnitIsDamaged = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			return unit.getHits() == 1;
		}
	};

	public static final Match UnitIsSea = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.isSea();
		}
	};

	
	
	
	public static final Match UnitIsSub = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.isSub();
		}
	};

	public static final Match UnitIsNotSub = new InverseMatch(UnitIsSub);

	public static final Match UnitIsDestroyer = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.getIsDestroyer();
		}
	};
	
	public static final Match UnitIsTransport = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.getTransportCapacity() != -1;
		}
	};

	public static final Match UnitIsStrategicBomber = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.isStrategicBomber();
		}
	};


	public static final Match UnitIsNotSea = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return !ua.isSea();
		}
	};

	public static final Match UnitTypeIsSea = new Match()
	{
		public boolean match(Object obj)
		{
			UnitType type = (UnitType) obj;
			UnitAttatchment ua = UnitAttatchment.get(type);
			return ua.isSea();
		}
	};

	public static final Match UnitTypeIsNotSea = new Match()
	{
		public boolean match(Object obj)
		{
			UnitType type = (UnitType) obj;
			UnitAttatchment ua = UnitAttatchment.get(type);
			return !ua.isSea();
		}
	};



	public static final Match UnitIsAir = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.isAir();
		}
	};

	public static final Match UnitIsNotAir = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return !ua.isAir();
		}
	};

	public static Match unitCanBombard(final PlayerID id)
	{
	    return new Match()
	    {
	    
			public boolean match(Object obj)
			{
				Unit unit = (Unit) obj;
				UnitAttatchment ua = UnitAttatchment.get(unit.getType());
				return ua.getCanBombard(id);
			}
	    };
	};

	public static final Match UnitCanBlitz = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.getCanBlitz();
		}
	};

	public static final Match UnitIsDestructible = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return !ua.isFactory() && !ua.isAA();
		}
	};


	public static final Match UnitTypeIsAir = new Match()
	{
		public boolean match(Object obj)
		{
			UnitType type = (UnitType) obj;
			UnitAttatchment ua = UnitAttatchment.get(type);
			return ua.isAir();
		}
	};

	public static final Match UnitTypeIsNotAir = new Match()
	{
		public boolean match(Object obj)
		{
			UnitType type = (UnitType) obj;
			UnitAttatchment ua = UnitAttatchment.get(type);
			return !ua.isAir();
		}
	};

	public static final Match UnitCanLandOnCarrier = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.getCarrierCost() != -1;
		}
	};

	public static final Match UnitIsCarrier = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.getCarrierCapacity() != -1;
		}
	};

	public static final Match UnitCanBeTransported  = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.getTransportCost() != -1;
		}
	};

	public static final Match UnitCanTransport  = new Match()
	{
		public boolean match(Object obj)
		{
			Unit unit = (Unit) obj;
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			return ua.getTransportCapacity() != -1;
		}
	};

	public static final Match UnitTypeCanTransport  = new Match()
	{
		public boolean match(Object obj)
		{
			UnitType type = (UnitType) obj;
			UnitAttatchment ua = UnitAttatchment.get(type);
			return ua.getTransportCapacity() != -1;
		}
	};


	public static final Match UnitTypeCanBeTransported  = new Match()
	{
		public boolean match(Object obj)
		{
			UnitType type = (UnitType) obj;
			UnitAttatchment ua = UnitAttatchment.get(type);
			return ua.getTransportCost() != -1;
		}
	};


	public static final Match UnitTypeIsFactory = new Match()
	{
		public boolean match(Object obj)
		{
			UnitType type = (UnitType) obj;
			UnitAttatchment ua = UnitAttatchment.get(type);
			return ua.isFactory();
		}
	};

	public static final Match UnitIsFactory = new Match()
	{
		public boolean match(Object obj)
		{
			UnitType type = ((Unit) obj).getUnitType();
			UnitAttatchment ua = UnitAttatchment.get(type);
			return ua.isFactory();
		}
	};

	public static final Match UnitIsNotFactory = new InverseMatch(UnitIsFactory);


	public static final Match UnitTypeIsAA = new Match()
	{
		public boolean match(Object obj)
		{
			UnitAttatchment ua = UnitAttatchment.get((UnitType) obj);
			return ua.isAA();
		}
	};


	public static final Match UnitIsAA = new Match()
	{
		public boolean match(Object obj)
		{
			UnitType type = ((Unit) obj).getUnitType();
			UnitAttatchment ua = UnitAttatchment.get(type);
			return ua.isAA();
		}
	};

	public static final Match UnitIsArtillery = new Match()
	{
	    public boolean match(Object obj)
	    {
			UnitType type = ((Unit) obj).getUnitType();
			UnitAttatchment ua = UnitAttatchment.get(type);
			return ua.isArtillery();
	    }
	};
	
	public static final Match UnitIsArtillerySupportable = new Match()
	{
	    public boolean match(Object obj)
	    {
			UnitType type = ((Unit) obj).getUnitType();
			UnitAttatchment ua = UnitAttatchment.get(type);
			return ua.isArtillerySupportable();
	    }
	};

	
	public static final Match TerritoryIsWater = new Match()
	{
		public boolean match(Object o)
		{
			Territory t = (Territory) o;
			return t.isWater();
		}
	};

    public static final Match TerritoryIsLand = new InverseMatch(TerritoryIsWater);

	public static final Match TerritoryIsEmpty = new Match()
	{
		public boolean match(Object o)
		{
			Territory t = (Territory) o;
			return t.getUnits().size() == 0;
		}
	};



	public static Match territoryIsEmptyOfCombatUnits(final GameData data, final PlayerID player)
    {
        return new Match()
        {
            public boolean match(Object o)
            {
                Territory t = (Territory) o;
                CompositeMatch nonCom = new CompositeMatchOr();
                nonCom.add(UnitIsAAOrFactory);
                nonCom.add(alliedUnit(player, data));
                return t.getUnits().allMatch(nonCom);
            }
        };
    }

	public static final Match TerritoryIsNuetral = new Match()
	{
		public boolean match(Object o)
		{
			Territory t = (Territory) o;
			if(t.isWater() )
				return false;
			return t.getOwner().equals(PlayerID.NULL_PLAYERID);
		}
	};

	public static final Match TerritoryIsImpassible = new Match()
	{
		public boolean match(Object o)
		{
			Territory t = (Territory) o;
			if (t.isWater())
            {
              return false;
            }
            else
            {
			  return TerritoryAttatchment.get(t).isImpassible();
            }
		}
	};

    public static final Match BattleIsEmpty = new Match()
	{
		public boolean match(Object o)
		{
			Battle battle = (Battle) o;
			return battle.isEmpty();
		}
	};
	
    public static final Match BattleIsAmphibious = new Match()
	{
		public boolean match(Object o)
		{
			Battle battle = (Battle) o;
			return battle.isAmphibious();
		}
	};


    /**
     *  Match units that have at least lower limit movement
     */
    public static Match unitHasEnoughMovement(final int lowerLimit, final IntegerMap movement)
    {
        return new Match()
        {
            public boolean match(Object o)
            {
                return movement.getInt(o) >= lowerLimit;
            }
        };
    }

	public static Match unitIsOwnedBy(final PlayerID player)
	{
		return new Match()
		{
			public boolean match(Object o)
			{
				Unit unit = (Unit) o;
				return unit.getOwner().equals(player);
			}
		};
	}

    public static Match isTerritoryAllied(final PlayerID player, final GameData data)
    {
        return new Match()
        {
            public boolean match(Object o)
            {
                Territory t = (Territory) o;
                return data.getAllianceTracker().isAllied(player, t.getOwner());
            }
        };
    }


    public static Match isUnitAllied(final PlayerID player, final GameData data)
    {
        return new Match()
        {
            public boolean match(Object o)
            {
                Unit t = (Unit) o;
                return data.getAllianceTracker().isAllied(player, t.getOwner());
            }
        };
    }


	public static Match isTerritoryFriendly(final PlayerID player, final GameData data)
	{
		return new Match()
		{
			public boolean match(Object o)
			{
				Territory t = (Territory) o;

				if(t.isWater())
					return true;
				if(t.getOwner().equals(player))
					return true;
				return data.getAllianceTracker().isAllied(player, t.getOwner());
			}
		};
	}

  public static Match unitIsEnemyAA(final PlayerID player, final GameData data)
  {
    CompositeMatch comp = new CompositeMatchAnd();
    comp.add(UnitIsAA);
    comp.add(enemyUnit(player, data));
    return comp;
  }

  public static Match unitIsInTerritory(final Territory territory)
  {
      return new Match()
      {
          public boolean match(Object o)
          {
              return territory.getUnits().getUnits().contains(o);
          }
      };
  }


	public static Match isTerritoryEnemy(final PlayerID player, final GameData data)
	{
		return new Match()
		{
			public boolean match(Object o)
			{
				Territory t = (Territory) o;
				if(t.isWater())
					return false;

				if(t.getOwner().equals(player))
					return false;
				return !data.getAllianceTracker().isAllied(player, t.getOwner());
			}
		};
	}

	public static Match enemyUnit(PlayerID player, GameData data)
	{
		return new InverseMatch(alliedUnit(player, data));
	}

    public static Match unitOwnedBy(final PlayerID player)
    {
        return new Match()
        {
            public boolean match(Object o)
            {
                Unit unit = (Unit) o;
                return unit.getOwner().equals(player);
            }
        };

    }



	public static Match alliedUnit(final PlayerID player, final GameData data)
	{
		return new Match()
		{
			public boolean match(Object o)
			{
				Unit unit = (Unit) o;
				if(unit.getOwner().equals(player))
					return true;
				return data.getAllianceTracker().isAllied(player, unit.getOwner());
			}
		};
	}

  public static Match territoryIs(final Territory test)
  {

    return new Match()
     {

       public boolean match(Object o)
       {
         Territory t = (Territory) o;
         return t.equals(test);
       }
     };

  }

  public static Match territoryHasUnitsOwnedBy(final PlayerID player)
  {
      final Match unitOwnedBy = unitIsOwnedBy(player);

      return new Match()
  {

    public boolean match(Object o)
    {
      Territory t = (Territory) o;
      return t.getUnits().someMatch(unitOwnedBy);
    }
  };

  }

  public static Match territoryHasEnemyAA(final PlayerID player, final GameData data)
  {


    return new Match()
    {
      Match unitIsEnemyAA = unitIsEnemyAA(player, data);

      public boolean match(Object o)
      {
        Territory t = (Territory) o;
        return t.getUnits().someMatch( unitIsEnemyAA );
      }
    };

  }


	public static Match territoryHasNoEnemyUnits(final PlayerID player, final GameData data)
	{
		return new Match()
		{
			public boolean match(Object o)
			{
				Territory t = (Territory) o;
				return t.getUnits().allMatch( alliedUnit(player,data));
			}
		};

	}

	public static Match territoryHasNonSubmergedEnemyUnits(final PlayerID player, final GameData data)
	{

	    final CompositeMatch match = new CompositeMatchAnd();
	    match.add(enemyUnit(player,data));
	    match.add(new InverseMatch( unitIsSubmerged(data)));
	    
	    return new Match()
		{
		    
			public boolean match(Object o)
			{
				Territory t = (Territory) o;
				return t.getUnits().someMatch( match );
			}
		};

	}

	
	public static Match territoryHasEnemyUnits(final PlayerID player, final GameData data)
	{
		return new Match()
		{
			public boolean match(Object o)
			{
				Territory t = (Territory) o;
				return t.getUnits().someMatch( enemyUnit(player,data));
			}
		};

	}

	public static Match UnitIsLand = new CompositeMatchAnd( UnitIsNotSea, UnitIsNotAir);
	public static Match UnitIsNotLand = new InverseMatch(UnitIsLand);

	public static Match unitIsOfType(final UnitType type)
	{
		return new Match()
		{
			public boolean match(Object o)
			{
				Unit unit =  (Unit) o;
				return unit.getType().equals(type);
			}
		};
	}

	public static Match territoryWasFoughOver(final BattleTracker tracker)
	{
		return new Match()
		{
			public boolean match(Object o)
			{
				Territory t = (Territory) o;
				return tracker.wasBattleFought(t) || tracker.wasBlitzed(t);
			}
		};
	}

	public static Match unitIsSubmerged(final GameData data)
	{
	    return new Match()
	    {
	        public boolean match(Object o)
	        {
	            SubmergedTracker tracker = DelegateFinder.moveDelegate(data).getSubmergedTracker();
	            return tracker.isSuberged((Unit) o);
	        }
	    };
	    
	}
	
	public static final Match UnitIsAAOrFactory = new CompositeMatchOr(UnitIsAA, UnitIsFactory);

	/** Creates new Matches */
	private Matches()
	{
    }

}
