/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.types;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author dmulloy2
 */

@Getter
@AllArgsConstructor
public enum PotionType
{
	FIRE_RESISTANCE("fireres"),
	INSTANT_DAMAGE("damage"),
	INSTANT_HEAL("heal"),
	INVISIBILITY("invis"),
	NIGHT_VISION("nvg"),
	POISON("poison"),
	REGEN("regen"),
	SLOWNESS("slow"),
	SPEED("speed"),
	STRENGTH("strength"),
	WATER("water"),
	WEAKNESS("weak");

	private final String name;

	public static String toName(PotionEffectType effect)
	{
		for (PotionType e : PotionType.values())
		{
			if (e.toString().equals(effect.getName()))
				return e.name;
		}

		return "";
	}

	public static String toString(Collection<PotionEffect> effects)
	{
		StringBuilder result = new StringBuilder();
		for (PotionEffect effect : effects)
		{
			result.append(PotionType.toName(effect.getType()) + ", ");
		}

		if (result.lastIndexOf(",") >= 0)
		{
			result.deleteCharAt(result.lastIndexOf(","));
			result.deleteCharAt(result.lastIndexOf(" "));
		}

		return result.toString();
	}
}