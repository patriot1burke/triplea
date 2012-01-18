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
/*
 * Util.java
 * 
 * Created on November 13, 2001, 1:57 PM
 */
package games.strategy.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Some utility methods for dealing with collections.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class Util
{
	/**
	 * return a such that a exists in c1 and a exists in c2.
	 * always returns a new collection.
	 */
	public static <T> List<T> intersection(final Collection<T> c1, final Collection<T> c2)
	{
		if (c1 == null || c2 == null)
			return new ArrayList<T>();
		if (c1.size() == 0 || c2.size() == 0)
			return new ArrayList<T>();
		final List<T> intersection = new ArrayList<T>();
		for (final T current : c1)
		{
			if (c2.contains(current))
				intersection.add(current);
		}
		return intersection;
	}
	
	/**
	 * Equivalent to !intersection(c1,c2).isEmpty(), but more effecient.
	 * 
	 * @return true if some element in c1 is in c2
	 */
	public static <T> boolean someIntersect(final Collection<T> c1, final Collection<T> c2)
	{
		if (c1.isEmpty())
			return false;
		if (c2.isEmpty())
			return false;
		final Iterator<T> iter = c1.iterator();
		while (iter.hasNext())
		{
			if (c2.contains(iter.next()))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns a such that a exists in c1 but not in c2.
	 * Always returns a new collection.
	 */
	public static <T> List<T> difference(final Collection<T> c1, final Collection<T> c2)
	{
		if (c1 == null || c1.size() == 0)
			return new ArrayList<T>(0);
		if (c2 == null || c2.size() == 0)
			return new ArrayList<T>(c1);
		final List<T> difference = new ArrayList<T>();
		for (final T current : c1)
		{
			if (!c2.contains(current))
				difference.add(current);
		}
		return difference;
	}
	
	/**
	 * true if for each a in c1, a exists in c2,
	 * and if for each b in c2, b exist in c1
	 * and c1 and c2 are the same size.
	 * Note that (a,a,b) (a,b,b) are equal.
	 */
	public static <T> boolean equals(final Collection<T> c1, final Collection<T> c2)
	{
		if (c1 == null || c2 == null)
			return c1 == c2;
		if (c1.size() != c2.size())
			return false;
		if (c1 == c2)
			return true;
		if (!c1.containsAll(c2))
			return false;
		if (!c2.containsAll(c1))
			return false;
		return true;
	}
	
	/**
	 * returns a list of everything in source, with the first count units moved to the end
	 */
	public static <T> List<T> shiftElementsToEnd(final List<T> source, final int count)
	{
		final ArrayList<T> rVal = new ArrayList<T>(source.size());
		for (int i = count; i < source.size(); i++)
		{
			rVal.add(source.get(i));
		}
		for (int i = 0; i < count; i++)
		{
			rVal.add(source.get(i));
		}
		if (source.size() != rVal.size())
			throw new IllegalStateException("Didnt work for: " + count + " " + source + " : " + rVal);
		return rVal;
	}
	
	/** Creates new Util */
	private Util()
	{
	}
	
	/**
	 * allow multiple fully qualified email adresses seperated by spaces, or a blank string
	 */
	public static boolean isMailValid(final String emailAddress)
	{
		final String QUOTEDSTRING = "\"(?:[^\"\\\\]|\\\\\\p{ASCII})*\"";
		final String ATOM = "[^()<>@,;:\\\\\".\\[\\] \\x28\\p{Cntrl}]+";
		final String WORD = "(?:" + ATOM + "|" + QUOTEDSTRING + ")";
		final String SUBDOMAIN = "(?:" + ATOM + "|\\[(?:[^\\[\\]\\\\]|\\\\\\p{ASCII})*\\])";
		final String DOMAIN = SUBDOMAIN + "(?:\\." + SUBDOMAIN + ")*";
		final String LOCALPART = WORD + "(?:\\." + WORD + ")*";
		final String EMAIL = LOCALPART + "@" + DOMAIN;
		// String regex = "(\\s*[\\w\\.-]+@\\w+\\.[\\w\\.]+\\s*)*";
		final String regex = "(\\s*" + EMAIL + "\\s*)*";
		return emailAddress.matches(regex);
	}
	
	public static String createUniqueTimeStamp()
	{
		final long time = System.currentTimeMillis();
		while (time == System.currentTimeMillis())
		{
			try
			{
				Thread.sleep(1);
			} catch (final InterruptedException e)
			{
			}
		}
		return "" + System.currentTimeMillis();
	}
	
	public static <T> void reorder(final List<T> reorder, final List<T> order)
	{
		final IntegerMap<T> map = new IntegerMap<T>();
		for (final T o : order)
		{
			map.put(o, order.indexOf(o));
		}
		Collections.sort(reorder, new Comparator<T>()
		{
			public int compare(final T o1, final T o2)
			{
				// get int returns 0 if no value
				final int v1 = map.getInt(o1);
				final int v2 = map.getInt(o2);
				if (v1 > v2)
				{
					return 1;
				}
				else if (v1 == v2)
				{
					return 0;
				}
				else
				{
					return -1;
				}
			}
		});
	}
}
