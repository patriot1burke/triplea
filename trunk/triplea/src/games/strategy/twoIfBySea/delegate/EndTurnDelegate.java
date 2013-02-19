package games.strategy.twoIfBySea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;

/**
 * <p>
 * Title: TripleA
 * </p>
 * <p>
 * </p>
 * <p>
 * Copyright (c) 2002
 * </p>
 * <p>
 * </p>
 * 
 * @author Sean Bridges
 * 
 */
public class EndTurnDelegate extends AbstractEndTurnDelegate
{
	protected boolean m_gameOver = false;
	
	public EndTurnDelegate()
	{
	}
	
	@Override
	protected String doNationalObjectivesAndOtherEndTurnEffects(final IDelegateBridge bridge)
	{
		final GameData data = getData();
		final PlayerList playerList = data.getPlayerList();
		final PlayerID british = playerList.getPlayerID(Constants.BRITISH);
		final PlayerID japanese = playerList.getPlayerID(Constants.JAPANESE);
		// Quick check to see who still owns their own capital
		final boolean britain = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(british, data).getOwner().equals(british);
		final boolean japan = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(japanese, data).getOwner().equals(japanese);
		if (!m_gameOver)
		{
			if (britain && !japan)
			{
				m_gameOver = true;
				bridge.getHistoryWriter().startEvent("British win.");
			}
			if (!britain && japan)
			{
				m_gameOver = true;
				bridge.getHistoryWriter().startEvent("Japanese win.");
			}
		}
		return "";
	}
	
	@Override
	protected String addOtherResources(final IDelegateBridge bridge)
	{
		return "";
	}
}
