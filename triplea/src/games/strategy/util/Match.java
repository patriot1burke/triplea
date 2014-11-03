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
 * Match.java
 * 
 * Created on November 8, 2001, 4:12 PM
 */
package games.strategy.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A utilty for seeing which elements in a collection satisfy a given condition.
 * <p>
 * An instance of match allows you to test that an object matches some condition.
 * <p>
 * Static utility methods allow you to find what elements in a collection satisfy a match, <br>
 * count the number of matches, see if any elements match etc.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public abstract class Match<T>
{
	/**
	 * A match that always returns true.
	 */
	@SuppressWarnings("rawtypes")
	public static final Match ALWAYS_MATCH = new AlwaysMatch();
	/**
	 * A match that always returns false.
	 */
	@SuppressWarnings("rawtypes")
	public static final Match NEVER_MATCH = new NeverMatch();
	
	public final static <T> Match<T> getAlwaysMatch()
	{
		return new AlwaysMatch<T>();
	}
	
	public final static <T> Match<T> getNeverMatch()
	{
		return new NeverMatch<T>();
	}
	
	/**
	 * Returns the elements of the collection that match.
	 */
	public final static <T> List<T> getMatches(final Collection<T> collection, final Match<T> aMatch)
	{
		final List<T> matches = new ArrayList<T>();
		for (final T current : collection)
		{
			if (aMatch.match(current))
			{
				matches.add(current);
			}
		}
		return matches;
	}
	
	/**
	 * Only returns the first n matches.
	 * If n matches cannot be found will return all matches that
	 * can be found.
	 */
	public static final <T> List<T> getNMatches(final Collection<T> collection, final int max, final Match<T> aMatch)
	{
		if (max == 0 || collection.isEmpty())
		{
			return Collections.emptyList();
		}
		if (max < 0)
		{
			throw new IllegalArgumentException("max must be positive, instead its:" + max);
		}
		final List<T> matches = new ArrayList<T>(Math.min(max, collection.size()));
		for (final T current : collection)
		{
			if (aMatch.match(current))
			{
				matches.add(current);
			}
			if (matches.size() == max)
			{
				return matches;
			}
		}
		return matches;
	}
	
	/**
	 * returns true if all elements in the collection match.
	 */
	public final static <T> boolean allMatch(final Collection<T> collection, final Match<T> aMatch)
	{
		if (collection.isEmpty())
		{
			return false;
		}
		for (final T current : collection)
		{
			if (!aMatch.match(current))
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns true if any matches could be found.
	 */
	public static final <T> boolean someMatch(final Collection<T> collection, final Match<T> aMatch)
	{
		if (collection.isEmpty())
		{
			return false;
		}
		for (final T current : collection)
		{
			if (aMatch.match(current))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if no matches could be found.
	 */
	public static final <T> boolean noneMatch(final Collection<T> collection, final Match<T> aMatch)
	{
		return !someMatch(collection, aMatch);
	}
	
	/**
	 * Returns the number of matches found.
	 */
	public static final <T> int countMatches(final Collection<T> collection, final Match<T> aMatch)
	{
		int count = 0;
		for (final T current : collection)
		{
			if (aMatch.match(current))
			{
				count++;
			}
		}
		return count;
	}
	
	/**
	 * return the keys where the value keyed by the key matches valueMatch
	 */
	public static <K, V> Set<K> getKeysWhereValueMatch(final Map<K, V> aMap, final Match<V> valueMatch)
	{
		final Set<K> rVal = new HashSet<K>();
		final Iterator<K> keys = aMap.keySet().iterator();
		while (keys.hasNext())
		{
			final K key = keys.next();
			final V value = aMap.get(key);
			if (valueMatch.match(value))
			{
				rVal.add(key);
			}
		}
		return rVal;
	}
	
	/**
	 * Subclasses must override this method.
	 * Returns true if the object matches some condition.
	 */
	public abstract boolean match(T o);
	
	public final Match<T> invert()
	{
		return new InverseMatch<T>(this);
	}
}


class NeverMatch<T> extends Match<T>
{
	@Override
	public boolean match(final T o)
	{
		return false;
	}
}


class AlwaysMatch<T> extends Match<T>
{
	@Override
	public boolean match(final T o)
	{
		return true;
	}
}
