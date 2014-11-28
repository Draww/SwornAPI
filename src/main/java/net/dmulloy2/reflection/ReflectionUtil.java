/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import net.dmulloy2.types.Versioning.Version;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Util for dealing with Java Reflection.
 *
 * @author dmulloy2
 */

public class ReflectionUtil
{
	private ReflectionUtil() { }

	private static final String NMS = "net.minecraft.server";
	private static final String OBC = "org.bukkit.craftbukkit";
	private static String VERSION;

	/**
	 * Attempts to get the NMS (net.minecraft.server) class with a given name.<br>
	 * While in theory this allows the crossing of versions, it is important to
	 * note that internal classes are frequently removed and/or reofbuscated.
	 *
	 * @param name Class Name
	 * @return NMS class, or null if none exists
	 */
	public static final Class<?> getNMSClass(String name)
	{
		Validate.notNull(name, "name cannot be null!");

		if (VERSION == null)
		{
			// Lazy-load VERSION
			String serverPackage = Bukkit.getServer().getClass().getPackage().getName();
			VERSION = serverPackage.substring(serverPackage.lastIndexOf('.') + 1);
		}

		name = NMS + "." + VERSION + "." + name;

		try
		{
			return Class.forName(name);
		} catch (Throwable ex) { }
		return null;
	}

	/**
	 * Attempts to get the OBC (org.bukkit.craftbukkit) class with a given name.
	 *
	 * @param name Class Name
	 * @return OBC class, or null if none exists
	 */
	public static final Class<?> getOBCClass(String name)
	{
		Validate.notNull(name, "name cannot be null!");

		if (VERSION == null)
		{
			// Lazy-load VERSION
			String serverPackage = Bukkit.getServer().getClass().getPackage().getName();
			VERSION = serverPackage.substring(serverPackage.lastIndexOf('.') + 1);
		}

		name = OBC + "." + VERSION + "." + name;

		try
		{
			return Class.forName(name);
		} catch (Throwable ex) { }
		return null;
	}

	/**
	 * Gets a {@link Field} in a given {@link Class} object.
	 *
	 * @param clazz Class object
	 * @param name Field name
	 * @return The field, or null if none exists.
	 */
	public static final Field getField(Class<?> clazz, String name)
	{
		Validate.notNull(clazz, "clazz cannot be null!");
		Validate.notNull(name, "name cannot be null!");

		try
		{
			Field field = clazz.getDeclaredField(name);
			if (field != null)
				return field;

			return clazz.getField(name);
		} catch (Throwable ex) { }
		return null;
	}

	/**
	 * Gets a {@link Method} in a given {@link Class} object with the specified
	 * arguments.
	 *
	 * @param clazz Class object
	 * @param name Method name
	 * @param params Parameters
	 * @return The method, or null if none exists
	 */
	public static final Method getMethod(Class<?> clazz, String name, Class<?>... params)
	{
		Validate.notNull(clazz, "clazz cannot be null!");
		Validate.notNull(name, "name cannot be null!");
		if (params == null)
			params = new Class<?>[0];

		for (Method method : clazz.getMethods())
		{
			if (method.getName().equals(name) && Arrays.equals(params, method.getParameterTypes()))
				return method;
		}

		return null;
	}

	/**
	 * Gets a {@link Method} in a given {@link Class} object.
	 *
	 * @param clazz Class object
	 * @param name Method name
	 * @return The method, or null if none exists
	 */
	public static final Method getMethod(Class<?> clazz, String name)
	{
		Validate.notNull(clazz, "clazz cannot be null!");
		Validate.notNull(name, "name cannot be null!");

		for (Method method : clazz.getMethods())
		{
			if (method.getName().equals(name))
				return method;
		}

		return null;
	}

	/**
	 * Gets the handle of a given object. This only works for classes that
	 * declare the getHandle() method, like CraftPlayer.
	 *
	 * @param object Object to get the handle for
	 * @return The handle, or null if none exists
	 */
	public static final Object getHandle(Object object)
	{
		Validate.notNull(object, "object cannot be null!");

		Method getHandle = getMethod(object.getClass(), "getHandle");
		Validate.notNull(getHandle, object.getClass() + " does not declare a getHandle() method!");

		try
		{
			return getHandle.invoke(object, new Object[0]);
		} catch (Throwable ex) { }
		return null;
	}

	// ---- Versioning
	// TODO: Keep up with MC releases

	private static final List<Version> SUPPORTED_VERSIONS = Arrays.asList(Version.MC_17);

	/**
	 * Whether or not a {@link Player} can reliably be sent packets. This works
	 * by checking the player's client {@link Version} against the supported
	 * version.
	 *
	 * @param player Player to check
	 * @return True if they can reliably be sent packets, false if not
	 */
	public static final boolean isReflectionSupported(Player player)
	{
		return SUPPORTED_VERSIONS.contains(getClientVersion(player));
	}

	/**
	 * Returns the client {@link Version} a given {@link Player} is using.
	 *
	 * @param player Player to get version for
	 * @return Their client version
	 */
	public static final Version getClientVersion(Player player)
	{
		Validate.notNull(player, "player cannot be null!");

		try
		{
			Object handle = getHandle(player);
			Field playerConnectionField = getField(handle.getClass(), "playerConnection");
			Object playerConnection = playerConnectionField.get(handle);

			Field networkManagerField = getField(playerConnection.getClass(), "networkManager");
			Object networkManager = networkManagerField.get(playerConnection);

			Method getVersion = getMethod(networkManager.getClass(), "getVersion", new Class<?>[0]);
			int version = (int) getVersion.invoke(networkManager);
			switch (version)
			{
				case 4:
				case 5:
					return Version.MC_17;
				case 47:
					return Version.MC_18;
				default:
					return Version.UNKNOWN;
			}
		} catch (Throwable ex) { }
		return Version.UNKNOWN;
	}
}