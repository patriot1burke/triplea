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
package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transports;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.LoadGameUtil;

import java.util.Collection;

import junit.framework.TestCase;

public class UnitsThatCantFightUtilTest extends TestCase
{
	public void testNoSuicideAttacksAA50AtStart()
	{
		// at the start of the game, there are no suicide attacks
		final GameData data = LoadGameUtil.loadGame("World War II v3 1941 Test", "ww2v3_1941_test.xml");
		final Collection<Territory> territories = new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
		assertTrue(territories.isEmpty());
	}
	
	public void testSuicideAttackInAA50()
	{
		final GameData data = LoadGameUtil.loadGame("World War II v3 1941 Test", "ww2v3_1941_test.xml");
		// add a german sub to sz 12
		final Territory sz12 = territory("12 Sea Zone", data);
		addTo(sz12, transports(data).create(1, germans(data)));
		final Collection<Territory> territories = new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
		assertTrue(territories.contains(sz12));
	}
	
	public void testSuicideAttackInAA50WithTransportedUnits()
	{
		final GameData data = LoadGameUtil.loadGame("World War II v3 1941 Test", "ww2v3_1941_test.xml");
		// add a german sub to sz 12
		final Territory sz12 = territory("12 Sea Zone", data);
		addTo(sz12, transports(data).create(1, germans(data)));
		final Collection<Territory> territories = new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
		assertTrue(territories.contains(sz12));
	}
	
	public void testSuicideAttackInRevised()
	{
		final GameData data = LoadGameUtil.loadGame("World War II Revised Test", "revised_test.xml");
		final Territory sz15 = territory("15 Sea Zone", data);
		addTo(sz15, transports(data).create(1, germans(data)));
		final Collection<Territory> territories = new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
		assertTrue(territories.contains(sz15));
	}
}
