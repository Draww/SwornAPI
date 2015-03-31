/**
 * SwornAPI - common API for MineSworn and Shadowvolt plugins
 * Copyright (C) 2015 dmulloy2
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.dmulloy2.util;

import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.types.Material;

import org.bukkit.Bukkit;

/**
 * Util dealing with the loss of item id's.
 *
 * @author dmulloy2
 */

public class MaterialUtil
{
	private MaterialUtil() { }

	/**
	 * Gets the {@link org.bukkit.Material} from a given string.
	 *
	 * @param string String to get the Material from
	 * @return The material, or null if not found
	 */
	public static final org.bukkit.Material getMaterial(String string)
	{
		org.bukkit.Material ret = matchMaterial(string);
		if (ret == null && NumberUtil.isInt(string))
			ret = getMaterial(NumberUtil.toInt(string));

		return ret;
	}

	@SuppressWarnings("deprecation") // Bukkit.getUnsafe()
	private static final org.bukkit.Material matchMaterial(String string)
	{
		org.bukkit.Material material = null;

		try
		{
			material = org.bukkit.Material.matchMaterial(string);
		} catch (Throwable ex) { }

		if (material == null)
		{
			try
			{
				// This method never returns null, but if a result is not found, it returns AIR
				org.bukkit.Material internal = Bukkit.getUnsafe().getMaterialFromInternalName(string);
				if (internal != org.bukkit.Material.AIR)
					material = internal;
			} catch (Throwable ex) { }
		}

		return material;
	}

	/**
	 * Returns the {@link org.bukkit.Material} from a given integer.
	 *
	 * @param id Integer to get the Material from
	 * @return Material, or null if not found
	 */
	public static final org.bukkit.Material getMaterial(int id)
	{
		Material mat = Material.getMaterial(id);
		if (mat != null)
			return mat.getBukkitMaterial();

		return null;
	}

	/**
	 * Gets the friendly name of a Material.
	 *
	 * @param mat Material
	 * @return Friendly name
	 */
	public static final String getMaterialName(org.bukkit.Material mat)
	{
		return FormatUtil.getFriendlyName(mat);
	}

	/**
	 * Gets the friendly name of a Material.
	 *
	 * @param name Material name
	 * @return Friendly name, or "null" if not found
	 */
	public static final String getMaterialName(String name)
	{
		org.bukkit.Material mat = getMaterial(name);
		if (mat == null)
			return "null";

		return getMaterialName(mat);
	}

	/**
	 * Converts a list of strings into a list of Materials.
	 *
	 * @param strings List to convert
	 * @return Converted list
	 */
	public static final List<org.bukkit.Material> fromStrings(List<String> strings)
	{
		List<org.bukkit.Material> ret = new ArrayList<>();

		for (String string : strings)
		{
			org.bukkit.Material material = getMaterial(string);
			if (material != null)
				ret.add(material);
		}

		return ret;
	}
}
