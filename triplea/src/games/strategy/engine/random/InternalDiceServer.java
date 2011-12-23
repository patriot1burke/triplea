package games.strategy.engine.random;

import games.strategy.engine.framework.startup.ui.editors.DiceServerEditor;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.lang.reflect.InvocationTargetException;

/**
 * This is not actually a dice server, it just uses the normal TripleA PlainRandomSource for dice roll
 * This way your dice rolls are not registered anywhere, and you do not rely on any external web based service rolling
 * the dice.
 *
 * Because DiceServers must be serializable read resolve must be implemented
 * @author Klaus Groenbaek
 */
public class InternalDiceServer implements IRemoteDiceServer
{
	private static final long serialVersionUID = -8369097763085658445L;
	//-----------------------------------------------------------------------
	// instance fields 
	//-----------------------------------------------------------------------
	private transient IRandomSource _randomSource;

	//-----------------------------------------------------------------------
	// constructor
	//-----------------------------------------------------------------------

	public InternalDiceServer()
	{
		_randomSource = new PlainRandomSource();
	}


	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------


	public EditorPanel getEditor()
	{
		return new DiceServerEditor(this);
	}

	public boolean sameType(IBean other)
	{
		return other.getClass() == InternalDiceServer.class;
	}

	public String postRequest(int max, int numDice, String subjectMessage, String gameID, String gameUUID) throws IOException
	{
		// the interface is rather stupid, you have to return a string here, which is then passed back in getDice()
		int[] ints = _randomSource.getRandom(max, numDice, "Internal Dice Server");
		StringBuilder sb = new StringBuilder();
		for (int i : ints)
		{
		 	sb.append(i).append(",");
		}

		String intArrayString = sb.substring(0, sb.length()-1);
		return intArrayString;
	}

	public int[] getDice(String string, int count) throws IOException, InvocationTargetException
	{
		String[] strArray = string.split(",");
		int[] intArray = new int[strArray.length];
		for (int i=0; i< strArray.length ;i++) {
			intArray[i] = Integer.parseInt(strArray[i]);
		}
		return intArray;
	}

	public String getDisplayName()
	{
		return "Internal Dice Roller";
	}

	public String getToAddress()
	{
		return null;
	}

	public void setToAddress(String toAddress)
	{

	}

	public String getCcAddress()
	{
		return null;
	}

	public void setCcAddress(String ccAddress)
	{

	}

	public String getInfoText()
	{
		return "Uses the build in TripleA dice roller.\nDice are not logged, and no internet access is required.\nIt is technically possible (for a hacker) to modify the dice rolls.";
	}

	public boolean sendsEmail()
	{
		return false;
	}

	/**
	 * Dice servers has to be serializable, so we need to provide custom serialization since
	 * PlainRandomSource is not serializable
	 * @return a new InternalDiceServer
	 * @throws ObjectStreamException should never occur (unless runtime exceptions is thrown from constructor)
	 */
	public Object readResolve() throws ObjectStreamException {
	 	return new InternalDiceServer();
	}

	public boolean supportsGameId()
	{
		return false;
	}

	public void setGameId(String gameId)
	{

	}

	public String getGameId()
	{
		return null;
	}
}
