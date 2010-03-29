package games.strategy.triplea;

import games.strategy.engine.random.EmailValidatorTest;
import games.strategy.triplea.baseAI.AIUtilsTest;
import games.strategy.triplea.delegate.*;
import games.strategy.triplea.util.UnitAutoChooserTest;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorTest;
import junit.framework.*;

public class TripleATest
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TripleATest.class.getSimpleName());
        suite.addTestSuite(PlaceDelegateTest.class);
        suite.addTestSuite(MoveDelegateTest.class);
        suite.addTestSuite(MoveValidatorTest.class);
        suite.addTestSuite(EmailValidatorTest.class);
        suite.addTestSuite(RevisedTest.class);
        suite.addTestSuite(BigWorldTest.class);
        suite.addTestSuite(WW2V3_41_Test.class);
        suite.addTestSuite(WW2V3_42_Test.class);
        suite.addTestSuite(LHTRTest.class);
        suite.addTestSuite(PacificTest.class);
        suite.addTestSuite(DiceRollTest.class);
        suite.addTestSuite(AIUtilsTest.class);
        suite.addTestSuite(OddsCalculatorTest.class);
        suite.addTestSuite(AirThatCantLandUtilTest.class);
        suite.addTestSuite(UnitAutoChooserTest.class);
        suite.addTestSuite(UnitsThatCantFightUtilTest.class);
        suite.addTestSuite(BattleCalculatorTest.class);
        return suite;
    }

}
