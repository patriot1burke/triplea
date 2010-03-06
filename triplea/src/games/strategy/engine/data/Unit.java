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
 * Unit.java
 *
 * Created on October 14, 2001, 12:33 PM
 */

package games.strategy.engine.data;

import games.strategy.net.GUID;

import java.io.*;


/**
 *
 * @author  Sean Bridges
 */
public class Unit extends GameDataComponent implements Serializable
{
  private PlayerID m_owner;
  private final GUID m_uid;
  private int m_hits;
  private final UnitType m_type;  

  /**
   * Creates new Unit.  Should use a call to UnitType.create() instead.
   * owner can be null
   */
  protected Unit(UnitType type, PlayerID owner, GameData data)
  {
    super(data);
    if(type == null) {
    	throw new IllegalArgumentException();
    }
    m_type = type;    
    m_uid = new GUID();
    setOwner(owner);
  }
  
  public GUID getID()
  {
    return m_uid;
  }

  public UnitType getType()
  {
    return m_type;
  }

  public UnitType getUnitType()
  {
    return m_type;
  }

  public PlayerID getOwner()
  {
    return m_owner;
  }

  public int getHits()
  {
      return m_hits;
  }

  void setHits(int hits)
  {
      m_hits = hits;
  }

  /**
   * can be null.
   */
  void setOwner(PlayerID player)
  {
    if(player == null)
      player = PlayerID.NULL_PLAYERID;
    m_owner = player;
  }

  public boolean equals(Object o)
  {
    if(! (o instanceof Unit))
      return false;
    
    Unit other = (Unit) o;
    return this.m_uid.equals(other.m_uid);
  }

  public int hashCode()
  {
    return m_uid.hashCode();
  }

  public String toString()
  {
    return m_type.getName() + " owned by " + m_owner.getName();
  }



}
