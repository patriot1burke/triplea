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
 * MoveValidatorTest.java
 *
 * Created on November 8, 2001, 5:00 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public class MoveValidatorTest extends DelegateTest
{
		
	/** Creates new PlaceDelegateTest */
    public MoveValidatorTest(String name) 
	{
		super(name);
    }

	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(MoveValidatorTest.class);
		
		return suite;
	}

	public void setUp() throws Exception
	{
		super.setUp();
	}
	
	
    public void testHasEnoughMovement()
	{
		
		List<Unit> units = bomber.create(3, british);
        TripleAUnit.get(units.get(0)).setAlreadyMoved(2);
        TripleAUnit.get(units.get(1)).setAlreadyMoved(1);
		
		assertTrue(MoveValidator.hasEnoughMovement(units, 2));
	}
	
	public void testHasWater()
	{
		Route route = new Route();
		route.setStart(eastMediteranean);
		assertTrue(MoveValidator.hasWater(route));
		
		route = new Route();
		route.setStart(eastAfrica);
		assertTrue(!MoveValidator.hasWater(route));
		
		route.add(kenya);
		assertTrue(!MoveValidator.hasWater(route));
		
		route.add(eastMediteranean);
		assertTrue(MoveValidator.hasWater(route));
	}
	
	public void testNotEnoughMovement()
	{
		
		Collection units = bomber.create(3, british);
		Object[] objs = units.toArray();
		assertTrue(MoveValidator.hasEnoughMovement(units, 6));
		assertTrue(!MoveValidator.hasEnoughMovement(units, 7));
		
        ((TripleAUnit) objs[1]).setAlreadyMoved(1);		
		assertTrue(!MoveValidator.hasEnoughMovement(units, 6));
		
        ((TripleAUnit) objs[1]).setAlreadyMoved(2);
		assertTrue(!MoveValidator.hasEnoughMovement(units, 5));
	}	
	
	public void testEnemyUnitsInPath()
	{
		//japanese unit in congo
		Route bad = new Route();
		//the empty case
		assertTrue(MoveValidator.onlyAlliedUnitsOnPath(bad, british, m_data));
		
		bad.add(egypt);
		bad.add(congo);
		bad.add(kenya);
		
		assertTrue(!MoveValidator.onlyAlliedUnitsOnPath(bad, british, m_data));
		
		Route good = new Route();
		good.add(egypt);
		good.add(kenya);
		assertTrue(MoveValidator.onlyAlliedUnitsOnPath(good, british, m_data));

		//at end so should still be good
		good.add(congo);
		assertTrue(MoveValidator.onlyAlliedUnitsOnPath(good, british, m_data));	
	}
	
	public void testHasNeutralBeforEnd()
	{
		Route route = new Route();
		route.add(egypt);
		assertTrue(!MoveValidator.hasNeutralBeforeEnd(route));
		
		//nuetral
		route.add(westAfrica);
		assertTrue(!MoveValidator.hasNeutralBeforeEnd(route));
		
		route.add(libya);
		assertTrue(MoveValidator.hasNeutralBeforeEnd(route));
	}
	
	public void testHasUnitsThatCantGoOnWater()
	{
		Collection units = new ArrayList();
		units.addAll( infantry.create(1,british));
		units.addAll( armour.create(1,british));
		units.addAll( transport.create(1,british));
		units.addAll( fighter.create(1,british));
		assertTrue(! MoveValidator.hasUnitsThatCantGoOnWater(units));
		
		assertTrue( MoveValidator.hasUnitsThatCantGoOnWater( factory.create(1,british)));
	}
		
	public void testCarrierCapacity()
	{
		Collection units = carrier.create(5,british);
		assertEquals(10, MoveValidator.carrierCapacity(units));
	}
	
	public void testCarrierCost()
	{
		Collection units = fighter.create(5,british);
		assertEquals(5, MoveValidator.carrierCost(units));
	}
	
	public void testGetLeastMovement()
	{
		
		Collection collection = bomber.create(1, british);
		
		assertEquals( MoveValidator.getLeastMovement(collection), 6);
		
		
		Object[] objs = collection.toArray();
        ((TripleAUnit) objs[0]).setAlreadyMoved(1);
		
		
		assertEquals( MoveValidator.getLeastMovement(collection), 5);
		
		collection.addAll(factory.create(2,british));
		assertEquals( MoveValidator.getLeastMovement(collection), 0);			
	}
		
	public void testCanLand()
	{
		Collection units = fighter.create(4, british);
		//2 carriers in red sea
		assertTrue(MoveValidator.canLand(units, redSea, british, m_data));
		//britian owns egypt
		assertTrue(MoveValidator.canLand(units, egypt, british, m_data));
		//only 2 carriers
		Collection tooMany = fighter.create(6, british);
		assertTrue(!MoveValidator.canLand(tooMany, redSea, british, m_data));
		
		//nowhere to land
		assertTrue(!MoveValidator.canLand(units, japanSeaZone, british, m_data));
		//nuetral
		assertTrue(!MoveValidator.canLand(units, westAfrica,  british, m_data));
	}
	
	public void testCanLandInfantry()
	{
		try
		{
			Collection units = infantry.create(1, british);
			MoveValidator.canLand(units, redSea, british, m_data);
		} catch(IllegalArgumentException e)
		{
			return;
		}
		fail("No exception thrown");

	}

	public void testCanLandBomber()
	{		
		Collection units = bomber.create(1, british);
		assertTrue(!MoveValidator.canLand(units, redSea, british, m_data));
	}

	public void testHasSomeLand()
	{
		Collection units = transport.create(3,british);
		assertTrue(! MoveValidator.hasSomeLand(units));
		
		units.addAll( infantry.create(2,british));
		assertTrue( MoveValidator.hasSomeLand(units));
	}
	
}
