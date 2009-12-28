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
 * Route.java
 *
 * Created on October 12, 2001, 5:23 PM
 */

package games.strategy.engine.data;

import java.util.*;

import games.strategy.util.*;

/**
 * 
 * A route between two territories.<p>
 * 
 * A route consists of a start territory, and a sequence of steps.  To create a route
 * do,
 * 
 * <code>
 * Route aRoute = new Route();
 * route.setStart(someTerritory);
 * route.add(anotherTerritory);
 * route.add(yetAnotherTerritory);
 * </code>
 * 
 * 
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 */
public class Route implements java.io.Serializable, Iterable<Territory>
{
    private List<Territory> m_route = new ArrayList<Territory>();

    private Territory m_start;

    
    public Route() {
        
    }
    
  public Route(List<Territory> route) {
        
        setStart(route.get(0));
        if(route.size() == 1) {
            return;
        }
        for(Territory t: route.subList(1, route.size())) {
            add(t);
        }
    }
    
    
    public Route(Territory start, Territory ... route) {
        
        setStart(start);
        for(Territory t: route) {
            add(t);
        }        
    }
    
    
    
    /**
     * Join the two routes. It must be the case that r1.end() equals r2.start()
     * or r1.end() == null and r1.start() equals r2
     * 
     * @return a new Route starting at r1.start() going to r2.end() along r1,
     *         r2, or null if the routes cant be joined it the joining would
     *         form a loop
     * 
     */
    public static Route join(Route r1, Route r2)
    {
        if (r1 == null || r2 == null)
            throw new IllegalArgumentException("route cant be null r1:" + r1 + " r2:" + r2);

        if (r1.getLength() == 0)
        {
            if (!r1.getStart().equals(r2.getStart()))
                throw new IllegalArgumentException("Cannot join, r1 doesnt end where r2 starts. r1:" + r1 + " r2:" + r2);
        } else
        {
            if (!r1.getEnd().equals(r2.getStart()))
                throw new IllegalArgumentException("Cannot join, r1 doesnt end where r2 starts. r1:" + r1 + " r2:" + r2);
        }

        Collection<Territory> c1 = new ArrayList<Territory>(r1.m_route);
        c1.add(r1.getStart());

        Collection<Territory> c2 = new ArrayList<Territory>(r2.m_route);

        if (!Util.intersection(c1, c2).isEmpty())
            return null;

        Route joined = new Route();
        joined.setStart(r1.getStart());

        for (int i = 0; i < r1.getLength(); i++)
        {
            joined.add(r1.at(i));
        }

        for (int i = 0; i < r2.getLength(); i++)
        {
            joined.add(r2.at(i));
        }

        return joined;
    }

    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        Route other = (Route) o;
        if (!(other.getLength() == this.getLength()))
            return false;
        if (!other.getStart().equals(this.getStart()))
            return false;
        return other.getTerritories().equals(this.getTerritories());
    }

    public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * Set the start of this route.
     */
    public void setStart(Territory t)
    {
        if (t == null)
            throw new IllegalStateException("Null territory");

        m_start = t;
    }

    /**
     * Get the start territory for this route.
     */
    public Territory getStart()
    {
        return m_start;
    }

    /**
     * Determines if the route crosses water by checking if any of the
     * territories except the start and end are sea territories.
     * 
     * @return whether the route encounters water other than at the start of the
     *         route.
     */
    public boolean crossesWater()
    {
        boolean startLand = !m_start.isWater();
        boolean overWater = false;
        Iterator<Territory> routeIter = m_route.iterator();
        Territory terr = null;
        while (routeIter.hasNext())
        {
            terr = routeIter.next();
            if (terr.isWater())
            {
                overWater = true;
            }
        }
        
        if(terr == null)
            return false;
        
        // If we started on land, went over water, and ended on land, we cross
        // water.
        return (startLand && overWater && !terr.isWater());
    }

    /**
     * Add the given territory to the end of the route.
     */
    public void add(Territory t)
    {
        if (t == null)
            throw new IllegalStateException("Null territory");
        if(t.equals(m_start) || m_route.contains(t))
            throw new IllegalArgumentException("Loops not allowed in m_routes, route:" + this + " new territory:" + t);

        m_route.add(t);
    }

    /**
     * 
     * @return the number of steps in this route.
     */
    public int getLength()
    {
        return m_route.size();
    }

    /**
     * Get the territory we will be in after the i'th step for this route has been made.
     *  
     */
    public Territory at(int i)
    {
        return m_route.get(i);
    }

    /**
     * Do all territories in this route match the given match?  The start territory
     * is not tested. 
     */
    public boolean allMatch(Match<Territory> aMatch)
    {
        for (int i = 0; i < getLength(); i++)
        {
            if (!aMatch.match(at(i)))
                return false;
        }
        return true;
    }

    /**
     * Do some territories in this route match the given match?  The start territory
     * is not tested. 
     */
    public boolean someMatch(Match<Territory> aMatch)
    {
        for (int i = 0; i < getLength(); i++)
        {
            if (aMatch.match(at(i)))
                return true;
        }
        return false;
    }

    /**
     * Get all territories in this route match the given match?  The start territory
     * is not tested. 
     */
    public Collection<Territory> getMatches(Match<Territory> aMatch)
    {
        return Match.getMatches(m_route, aMatch);
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder("Route:").append(m_start);
        for (int i = 0; i < getLength(); i++)
        {
            buf.append(" -> ");
            buf.append(at(i).getName());           
        }
        return buf.toString();
    }

    /**
     * Returns a collection of all territories in this route, including the start.
     */
    public List<Territory> getTerritories()
    {
        ArrayList<Territory> list = new ArrayList<Territory>(m_route);
        list.add(0, m_start);
        return list;
    }

    /**
     * Get the last territory in the route, this is the destination.
     * If the route consists of only a starting territory, this will return null.
     */
    public Territory getEnd()
    {
        if (m_route.size() == 0)
            return null;
        return m_route.get(m_route.size() - 1);
    }

    /**
     * does this route extend another route
     */
    public boolean extend(Route baseRoute)
    {
        if (!baseRoute.m_start.equals(baseRoute.m_start))
        {
            return false;
        }

        if (baseRoute.getLength() > getLength())
            return false;

        for (int i = 0; i < baseRoute.m_route.size(); i++)
        {
            if (!baseRoute.at(i).equals(at(i)))
                return false;
        }
        return true;

    }

    public Iterator<Territory> iterator() {
        return Collections.unmodifiableList(getTerritories()).iterator();
    }

}
