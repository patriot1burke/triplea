package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.PlayerID;

import java.util.List;

public class TechResults implements java.io.Serializable
{
	private static final long serialVersionUID = 5574673305892105782L;
	private int[] m_rolls;
	private int m_hits;
	private int m_remainder = 0;
	// a list of Strings
	private List<String> m_advances;
	private PlayerID m_playerID;
	private String m_errorString;
	
	public TechResults(final String errorString)
	{
		m_errorString = errorString;
	}
	
	/**
	 * @return whether there was an error
	 */
	public boolean isError()
	{
		return m_errorString != null;
	}
	
	/**
	 * @return string error or null if no error occurred (use isError to see if there was an error)
	 */
	public String getErrorString()
	{
		return m_errorString;
	}
	
	/**
	 * 
	 * @param rolls
	 *            rolls
	 * @param remainder
	 *            remainder
	 * @param hits
	 *            number of hits
	 * @param advances
	 *            a List of Strings
	 * @param id
	 *            player id
	 */
	public TechResults(final int[] rolls, final int remainder, final int hits, final List<String> advances, final PlayerID id)
	{
		m_rolls = rolls;
		m_remainder = remainder;
		m_hits = hits;
		m_advances = advances;
		m_playerID = id;
	}
	
	public int getHits()
	{
		return m_hits;
	}
	
	public int getRemainder()
	{
		return m_remainder;
	}
	
	public PlayerID getPlayer()
	{
		return m_playerID;
	}
	
	public int[] getRolls()
	{
		return m_rolls;
	}
	
	/**
	 * 
	 * @return a List of Strings
	 */
	public List<String> getAdvances()
	{
		return m_advances;
	}
}
