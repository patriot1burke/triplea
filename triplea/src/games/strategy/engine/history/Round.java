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


package games.strategy.engine.history;


public class Round extends IndexedHistoryNode
{
    private final int m_RoundNo;

    Round(int round, int changeStartIndex)
    {
        super("Round :" + round, changeStartIndex, true);
        m_RoundNo = round;
    }

     public int getRoundNo()
     {
         return m_RoundNo;
     }

     public SerializationWriter getWriter()
     {
         return new RoundHistorySerializer(m_RoundNo);
     }

 }

 class RoundHistorySerializer implements SerializationWriter
 {
    private int m_roundNo;
     
    
    
    public RoundHistorySerializer(int roundNo)
    {
        m_roundNo = roundNo;
    }
    
    public void write(HistoryWriter writer)
    {
        writer.startNextRound(m_roundNo);
    }
     
 }