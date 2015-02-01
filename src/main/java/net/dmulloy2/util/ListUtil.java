/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

/**
 * Util dealing with Lists.
 *
 * @author dmulloy2
 */

public class ListUtil
{
	private static final Object EMPTY = new Object();

	/**
	 * Constructs a new {@link List} from an existing {@link Collection}. This
	 * helps with concurrency problems. Changes to the returned list will not be
	 * reflected in the original collection. The use of this method is generally
	 * not performance-effective.
	 * <p>
	 * TODO: Find a better solution.
	 *
	 * @param coll Base Collection
	 * @return The List
	 */
	public static <T> List<T> newList(Collection<? extends T> coll)
	{
		Validate.notNull(coll, "coll cannot be null!");
		return new ArrayList<>(coll);
	}

	/**
	 * Removes duplicate entries from a {@link List}. Retains order.
	 *
	 * @param list List to remove duplicate entries from
	 * @return The list, without duplicate entries
	 */
	public static <T> List<T> removeDuplicates(List<T> list)
	{
		Validate.notNull(list, "list cannot be null!");

		Map<T, Object> map = new LinkedHashMap<>();

		for (T element : list)
			map.put(element, EMPTY);

		return new ArrayList<>(map.keySet());
	}

	/**
	 * Constructs a new {@link List} paramaterized with <code>T</code>.
	 *
	 * @param objects Array of <code>T</code> to create the list with
	 * @return a new {@link List} from the given objects
	 * @see {@link Arrays#asList(Object...)}
	 */
	@SafeVarargs
	public static <T> List<T> toList(T... objects)
	{
		Validate.notNull(objects, "objects cannot be null!");
		return Arrays.asList(objects);
	}
}