/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.chat;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import junit.framework.TestCase;

public class ChatPanelTest extends TestCase
{
	public void testTrim() throws Exception
	{
		final StyledDocument doc = new DefaultStyledDocument();
		final StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < 10; i++)
		{
			buffer.append("\n");
		}
		doc.insertString(0, buffer.toString(), null);
		ChatMessagePanel.trimLines(doc, 20);
		assertEquals(doc.getLength(), 10);
		ChatMessagePanel.trimLines(doc, 10);
		assertEquals(doc.getLength(), 10);
		ChatMessagePanel.trimLines(doc, 5);
		assertEquals(doc.getLength(), 5);
		ChatMessagePanel.trimLines(doc, 1);
		assertEquals(doc.getLength(), 1);
	}
}
