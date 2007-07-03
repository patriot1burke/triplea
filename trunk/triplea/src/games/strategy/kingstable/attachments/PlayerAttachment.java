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

package games.strategy.kingstable.attachments;

import games.strategy.engine.data.DefaultAttachment;

/**
 *
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class PlayerAttachment extends DefaultAttachment
{

    private boolean m_needsKing = false;
    private int m_AbSearchDepth = 2;

    /** Creates new PlayerAttachment */
    public PlayerAttachment()
    {
    }

    public void setNeedsKing(String value)
    {
        m_needsKing = getBool(value);
    }

    public boolean needsKing()
    {
        return m_needsKing;
    }

    public void setAlphaBetaSearchDepth(String value)
    {
        m_AbSearchDepth = getInt(value);
    }
    
    public int getAlphaBetaSearchDepth()
    {
        return m_AbSearchDepth;
    }    
    
}
