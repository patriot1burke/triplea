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

package games.strategy.engine.data.properties;

import javax.swing.JComponent;

public abstract class AEditableProperty implements IEditableProperty, java.io.Serializable, Comparable
{
  private final String m_name;

  public AEditableProperty(String name)
  {
    m_name = name;
  }

  public String getName()
  {
    return m_name;
  }

  public JComponent getViewComponent()
  {
    JComponent rVal = getEditorComponent();
    rVal.setEnabled(false);
    return rVal;
  }
  
  public int hashCode()
  {
    return m_name.hashCode();
  }
  
  public boolean equals(Object other)
  {
    if(other instanceof AEditableProperty)
    {
      return ((AEditableProperty) other).m_name.equals(this.m_name);
    }
    else
      return false;
  }

  public int compareTo(Object other)
  {
    if(other instanceof AEditableProperty)
    {
      return this.m_name.compareTo( ((AEditableProperty) other).getName());
    }
    return -1;
  }

}