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
package net.dmulloy2.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dmulloy2.SwornPlugin;
import net.dmulloy2.chat.BaseComponent;
import net.dmulloy2.chat.ChatUtil;
import net.dmulloy2.chat.ClickEvent;
import net.dmulloy2.chat.ComponentBuilder;
import net.dmulloy2.chat.HoverEvent;
import net.dmulloy2.chat.HoverEvent.Action;
import net.dmulloy2.chat.TextComponent;
import net.dmulloy2.types.CommandVisibility;
import net.dmulloy2.types.IPermission;
import net.dmulloy2.types.StringJoiner;
import net.dmulloy2.util.FormatUtil;
import net.dmulloy2.util.ListUtil;
import net.dmulloy2.util.NumberUtil;
import net.dmulloy2.util.Util;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * Represents a commmand. This class provides useful methods for execution,
 * permission and argument manipulation, and messaging.
 *
 * @author dmulloy2
 */

public abstract class Command implements CommandExecutor
{
	protected final SwornPlugin plugin;

	protected CommandSender sender;
	protected Player player;
	protected String args[];

	protected String name;
	protected String description;

	protected IPermission permission;
	protected CommandVisibility visibility = CommandVisibility.PERMISSION;

	protected List<SubCommand> subCommands;
	protected Command parent;

	protected List<Syntax> syntaxes;
	protected List<String> aliases;

	protected boolean mustBePlayer;
	protected boolean usesPrefix;

	public Command(SwornPlugin plugin)
	{
		this.plugin = plugin;
		this.aliases = new ArrayList<>();
		this.subCommands = new ArrayList<>();
		this.syntaxes = new ArrayList<>();
		syntaxes.add(new Syntax());
	}

	// ---- Execution

	@Override
	public final boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args)
	{
		execute(sender, args);
		return true;
	}

	/**
	 * Executes this command with a given sender and arguments. This method
	 * performs all of the permission and argument length checks before passing
	 * the call to {@link #perform()}.
	 * 
	 * @param sender Sender of this command
	 * @param args Arguments
	 */
	public final void execute(CommandSender sender, String[] args)
	{
		if (! subCommands.isEmpty() && args.length != 0)
		{
			for (SubCommand subCommand : subCommands)
			{
				if (subCommand.argMatchesIdentifier(args[0]))
				{
					args = Arrays.copyOfRange(args, 1, args.length);
					subCommand.execute(sender, args);
					return;
				}
			}
		}

		this.sender = sender;
		this.args = args;
		if (sender instanceof Player)
			player = (Player) sender;

		if (mustBePlayer && ! isPlayer())
		{
			err("You must be a player to perform this command!");
			return;
		}

		syntax:
		{
			for (Syntax syntax : syntaxes)
			{
				if (syntax.requiredSize() <= args.length)
					break syntax;
			}

			invalidSyntax(args);
			return;
		}

		if (! isVisibleTo(sender))
		{
			if (visibility == CommandVisibility.PERMISSION)
			{
				StringJoiner hoverText = new StringJoiner("\n");
				hoverText.append(FormatUtil.format("&4Permission:"));
				hoverText.append(FormatUtil.format("&r{0}", getPermissionString()));

				ComponentBuilder builder = new ComponentBuilder(FormatUtil.format("&cError: &4You do not have "));
				builder.append(FormatUtil.format("&cpermission")).event(new HoverEvent(Action.SHOW_TEXT, hoverText.toString()));
				builder.append(FormatUtil.format(" &4to perform this command!"));
				sendMessage(builder.create());
				return;
			}
			else
			{
				err("You cannot use this command!");
				return;
			}
		}

		try
		{
			perform();
		}
		catch (Throwable ex)
		{
			String stack = Util.getUsefulStack(ex, "executing command " + name);
			plugin.getLogHandler().log(Level.WARNING, stack);

			String error = FormatUtil.format("&cError: &4Encountered an exception executing this command: ");

			ComponentBuilder builder = new ComponentBuilder(error);
			builder.append(FormatUtil.format("&c{0}", ex.toString())).event(new HoverEvent(Action.SHOW_TEXT, stack.replace("\t", "    ")));
			sendMessage(builder.create());
		}
	}

	/**
	 * Performs this command after permission and argument length checks.
	 */
	public abstract void perform();

	/**
	 * Whether or not the sender of this command is a {@link Player}.
	 * 
	 * @return True if they are, false if not.
	 */
	protected final boolean isPlayer()
	{
		return sender instanceof Player;
	}

	// ---- Permission Management

	/**
	 * Whether or not a given command sender has a given permission.
	 * 
	 * @param sender Sender to check
	 * @param permission Permission to check for
	 * @return True if they have it, false if not
	 */
	protected final boolean hasPermission(CommandSender sender, IPermission permission)
	{
		Validate.notNull(sender, "sender cannot be null!");
		Validate.notNull(permission, "permission cannot be null!");

		return plugin.getPermissionHandler().hasPermission(sender, permission);
	}

	/**
	 * Whether or not the sender of this command has a given permission.
	 * 
	 * @param permission Permission to check for
	 * @return True if they have it, false if not.
	 */
	protected final boolean hasPermission(IPermission permission)
	{
		return hasPermission(sender, permission);
	}

	/**
	 * Gets the full permission string of a given permission.
	 * 
	 * @param permission Permission
	 * @return The full string
	 */
	protected final String getPermissionString(IPermission permission)
	{
		Validate.notNull(permission, "permission cannot be null!");

		return plugin.getPermissionHandler().getPermissionString(permission);
	}

	/**
	 * Gets the full permission string of this command's permission.
	 * 
	 * @return The full string
	 */
	public final String getPermissionString()
	{
		return getPermissionString(permission);
	}

	/**
	 * Whether or not this command is visible to a given command sender. The
	 * output depends on {@link #visibility}
	 * 
	 * @param sender Sender to check
	 * @return True if it is, false if not
	 */
	public final boolean isVisibleTo(CommandSender sender)
	{
		Validate.notNull(sender, "sender cannot be null!");
		
		switch (visibility)
		{
			case ALL:
				return true;
			case PERMISSION:
				return hasPermission(sender, permission);
			case OPS:
				return sender.isOp();
			case NONE:
				return false;
			default:
				throw new IllegalStateException("Unsupported command visibility: " + visibility);
		}
	}

	// ---- Messaging

	/**
	 * Sends an error message to the command sender.
	 * 
	 * @param message Message to send
	 * @param args Objects to format in
	 */
	protected final void err(String message, Object... args)
	{
		Validate.notNull(message, "message cannot be null!");
		sendMessage("&cError: &4" + FormatUtil.format(message, args));
	}

	/**
	 * Sends a prefixed message to the command sender.
	 * 
	 * @param message Message to send
	 * @param args Objects to format in
	 */
	protected final void sendpMessage(String message, Object... args)
	{
		Validate.notNull(message, "message cannot be null!");
		sendMessage(plugin.getPrefix() + message, args);
	}

	/**
	 * Sends a message to the command sender.
	 * 
	 * @param message Message to send
	 * @param args Objects to format in
	 */
	protected final void sendMessage(String message, Object... args)
	{
		Validate.notNull(message, "message cannot be null");
		sender.sendMessage(ChatColor.YELLOW + FormatUtil.format(message, args));
	}

	/**
	 * Sends an error message to a given command sender.
	 * 
	 * @param sender Sender to send the message to
	 * @param message Message to send
	 * @param args Objects to format in
	 */
	protected final void err(CommandSender sender, String message, Object... args)
	{
		Validate.notNull(sender, "sender cannot be null!");
		Validate.notNull(message, "message cannot be null!");

		sendMessage(sender, "&cError: &4" + FormatUtil.format(message, args));
	}

	/**
	 * Sends a prefixed message to a given command sender.
	 * 
	 * @param sender Sender to send the message to
	 * @param message Message to send
	 * @param args Objects to format in
	 */
	protected final void sendpMessage(CommandSender sender, String message, Object... args)
	{
		Validate.notNull(sender, "sender cannot be null!");
		Validate.notNull(message, "message cannot be null!");

		sendMessage(sender, plugin.getPrefix() + message, args);
	}

	/**
	 * Sends a message to a given command sender.
	 * 
	 * @param sender Sender to send the message to
	 * @param message Message to send
	 * @param args Objects to format in
	 */
	protected final void sendMessage(CommandSender sender, String message, Object... args)
	{
		Validate.notNull(sender, "sender cannot be null!");
		Validate.notNull(message, "message cannot be null!");

		sender.sendMessage(ChatColor.YELLOW + FormatUtil.format(message, args));
	}

	// ---- Fancy Messaging

	/**
	 * Sends a JSON message to the command sender.
	 * @param components JSON message to send
	 */
	protected final void sendMessage(BaseComponent... components)
	{
		sendMessage(sender, components);
	}

	/**
	 * Sends a JSON message to a given command sender.
	 * @param sender Sender to send the message to
	 * @param components JSON message to send
	 */
	protected final void sendMessage(CommandSender sender, BaseComponent... components)
	{
		ChatUtil.sendMessage(sender, components);
	}

	// ---- Help

	/**
	 * Gets the name of this command
	 * 
	 * @return The name of this command
	 */
	public final String getName()
	{
		return name;
	}

	/**
	 * Gets a list of aliases for this command
	 * 
	 * @return The list of aliases
	 */
	public final List<String> getAliases()
	{
		return aliases;
	}

	/**
	 * Gets a basic usage template for this command
	 * 
	 * @param displayHelp Whether or not to display the discription
	 * @return The usage template
	 */
	public List<String> getUsageTemplate(boolean displayHelp)
	{
		List<String> ret = new ArrayList<>();

		for (int i = 0; i < syntaxes.size(); i++)
		{
			Syntax syntax = syntaxes.get(i);
			StringBuilder line = new StringBuilder();
			line.append("&b/");

			if (plugin.getCommandHandler().usesCommandPrefix() && usesPrefix)
				line.append(plugin.getCommandHandler().getCommandPrefix()).append(" ");

			if (parent != null)
				line.append(parent.getName()).append(" ");

			line.append(name);

			for (Argument arg : syntax)
			{
				if (arg.isRequired())
					line.append(String.format(" &3<%s>", arg.getArgument()));
				else
					line.append(String.format(" &3[%s]", arg.getArgument()));
			}

			if (displayHelp && i == 0)
				line.append(" &e" + description);

			ret.add(FormatUtil.format(line.toString()));
		}

		return ret;
	}

	/**
	 * Gets a fancy usage template for this command
	 * 
	 * @return The usage template
	 */
	public List<BaseComponent[]> getFancyUsageTemplate()
	{
		return getFancyUsageTemplate(false);
	}

	/**
	 * Gets a fancy usage template for this command
	 * 
	 * @param list Whether or not it is part of a list
	 * @return The usage template
	 */
	public List<BaseComponent[]> getFancyUsageTemplate(boolean list)
	{
		List<BaseComponent[]> ret = new ArrayList<>();

		for (int i = 0; i < syntaxes.size(); i++)
		{
			Syntax syntax = syntaxes.get(i);
			StringBuilder templateBuilder = new StringBuilder();
			templateBuilder.append("&b/");

			if (plugin.getCommandHandler().usesCommandPrefix() && usesPrefix)
				templateBuilder.append(plugin.getCommandHandler().getCommandPrefix()).append(" ");

			if (parent != null)
				templateBuilder.append(parent.getName()).append(" ");

			templateBuilder.append(name);

			for (Argument arg : syntax)
			{
				if (arg.isRequired())
					templateBuilder.append(String.format(" &3<%s>", arg.getArgument()));
				else
					templateBuilder.append(String.format(" &3[%s]", arg.getArgument()));
			}

			String template = FormatUtil.format(templateBuilder.toString());
			String prefix = list ? i == 0 ? "- " : "  " : "";
			ComponentBuilder builder = new ComponentBuilder(ChatColor.AQUA + prefix + template);

			StringBuilder hoverTextBuilder = new StringBuilder();
			hoverTextBuilder.append(template + ":\n");

			for (int a = 0; a < syntax.size(); a++)
			{
				Argument arg = syntax.get(a);
				String explanation = arg.getExplanation();
				if (explanation != null)
				{
					String argument = arg.getArgument();
					if (arg.isRequired())
						hoverTextBuilder.append(FormatUtil.format("&3  <{0}>: &e{1}\n", argument, explanation));
					else
						hoverTextBuilder.append(FormatUtil.format("&3  [{0}]: &e{1}\n", argument, explanation));
				}

				if (a != 0 && a == syntax.size() - 1)
					hoverTextBuilder.append("\n");
			}

			StringJoiner description = new StringJoiner("\n");
			for (String s : getDescription())
				description.append(ChatColor.YELLOW + s);
			hoverTextBuilder.append(FormatUtil.format(capitalizeFirst(description.toString())));

			if (permission != null)
			{
				hoverTextBuilder.append("\n\n");
				hoverTextBuilder.append(ChatColor.DARK_RED + "Permission:");
				hoverTextBuilder.append("\n" + getPermissionString());
			}

			String hoverText = hoverTextBuilder.toString();

			HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hoverText));
			builder.event(hoverEvent);

			ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ChatColor.stripColor(template));
			builder.event(clickEvent);

			ret.add(builder.create());
		}

		return ret;
	}

	private List<String> descriptionList;

	/**
	 * Gets the description for this command
	 * @return The description
	 */
	public List<String> getDescription()
	{
		if (descriptionList == null)
			descriptionList = ListUtil.toList(description);
		return descriptionList;
	}

	// ---- Sub Commands

	/**
	 * Adds a sub-command to this command
	 * 
	 * @param command Command to add
	 */
	protected final void addSubCommand(SubCommand command)
	{
		subCommands.add(command);
	}

	/**
	 * Gets this command's parent. Will be null if this command is independent.
	 * 
	 * @return The parent, or null if none
	 */
	protected final Command getParentCommand()
	{
		return parent;
	}

	/**
	 * Whether or not this command has sub-commands
	 * 
	 * @return True if it does, false if not
	 */
	protected final boolean hasSubCommands()
	{
		return ! subCommands.isEmpty();
	}

	/**
	 * Gets a list of sub-commands to this command.
	 * 
	 * @return The list
	 */
	protected final List<SubCommand> getSubCommands()
	{
		return subCommands;
	}

	/**
	 * Gets help for this command's sub-commands.
	 * 
	 * @param displayHelp Whether or not to display the description
	 * @return A list of usage templates
	 */
	protected final List<String> getSubCommandHelp(boolean displayHelp)
	{
		List<String> ret = new ArrayList<>();

		for (SubCommand cmd : getSubCommands())
		{
			ret.addAll(cmd.getUsageTemplate(displayHelp));
		}

		return ret;
	}

	/**
	 * Gets fancy help for this command's sub-commands.
	 * 
	 * @return A list of fancy usage templates
	 */
	public final List<BaseComponent[]> getFancySubCommandHelp()
	{
		return getFancySubCommandHelp(false);
	}

	/**
	 * Gets fancy help for this command's sub-commands.
	 * 
	 * @param list Whether or not they're part of a list
	 * @return A list of fancy usage templates
	 */
	public final List<BaseComponent[]> getFancySubCommandHelp(boolean list)
	{
		List<BaseComponent[]> ret = new ArrayList<>();

		for (SubCommand cmd : getSubCommands())
		{
			ret.addAll(cmd.getFancyUsageTemplate(list));
		}

		return ret;
	}

	// ---- Argument Manipulation

	/**
	 * Whether or not a given array contains a given argument
	 * 
	 * @param arg Argument to search for
	 * @param aliases Aliases to search
	 * @return True if it does, false if not
	 */
	protected final boolean argMatchesAlias(String arg, String... aliases)
	{
		for (String s : aliases)
		{
			if (arg.equalsIgnoreCase(s))
				return true;
		}

		return false;
	}

	/**
	 * Gets an argument as an integer
	 * 
	 * @param arg Argument index
	 * @param msg Whether or not to show an error
	 * @return The integer, or -1 if parsing failed
	 */
	protected final int argAsInt(int arg, boolean msg)
	{
		int ret = -1;
		if (args.length > arg)
			ret = NumberUtil.toInt(args[arg]);

		if (msg && ret == - 1)
			err("&c{0} &4is not a number.", args[arg]);

		return ret;
	}

	/**
	 * Gets an argument as a double
	 * 
	 * @param arg Argument index
	 * @param msg Whether or not to show an error
	 * @return The double, or -1.0D if parsing failed
	 */
	protected final double argAsDouble(int arg, boolean msg)
	{
		double ret = -1.0D;
		if (args.length > arg)
			ret = NumberUtil.toDouble(args[arg]);

		if (msg && ret == -1.0D)
			err("&c{0} &4is not a number.", args[arg]);

		return ret;
	}

	/**
	 * Gets an argument as a boolean
	 * 
	 * @param arg Argument index
	 * @return The boolean
	 */
	protected boolean argAsBoolean(int arg)
	{
		return argAsBoolean(arg, false);
	}

	/**
	 * Gets an argument as a boolean, falling back to the default if it's out of
	 * range
	 * 
	 * @param arg Argument index
	 * @param def Default value
	 * @return The boolean
	 */
	protected boolean argAsBoolean(int arg, boolean def)
	{
		return args.length > arg ? Util.toBoolean(args[arg]) : def;
	}

	/**
	 * Combines the arguments from {@code start} to {@code args.length} with
	 * spaces
	 * 
	 * @param start Starting index
	 * @return The resulting string
	 */
	protected final String getFinalArg(int start)
	{
		StringBuilder ret = new StringBuilder();
		for (int i = start; i < args.length; i++)
		{
			if (i != start)
				ret.append(" ");

			ret.append(args[i]);
		}

		return ret.toString();
	}

	/**
	 * Capitalizes the first letter of a given string
	 * 
	 * @param string String to capitalize
	 * @return The string
	 */
	protected String capitalizeFirst(String string)
	{
		return FormatUtil.capitalizeFirst(string);
	}

	// ---- Utility

	/**
	 * Gets the name of a given command sender. This method supports command
	 * blocks, console, and players.
	 * 
	 * @param sender Sender to get the name of
	 * @return Their name
	 */
	protected final String getName(CommandSender sender)
	{
		if (sender instanceof BlockCommandSender)
		{
			BlockCommandSender commandBlock = (BlockCommandSender) sender;
			Location location = commandBlock.getBlock().getLocation();
			return FormatUtil.format("CommandBlock ({0}, {1}, {2})", location.getBlockX(), location.getBlockY(), location.getBlockZ());
		}
		else if (sender instanceof ConsoleCommandSender)
		{
			return "Console";
		}
		else
		{
			return sender.getName();
		}
	}

	// ---- Syntax

	/**
	 * Displays the invalid syntax message
	 */
	protected final void invalidSyntax()
	{
		invalidSyntax(args);
	}

	/**
	 * Displays the invalid syntax message for a given array of arguments.
	 * 
	 * @param args Arguments
	 */
	protected final void invalidSyntax(String[] args)
	{
		Syntax closest = findClosest(args);
		String invalidSyntax = FormatUtil.format("&cError: &4Invalid syntax! Missing: &c");
		ComponentBuilder builder = new ComponentBuilder(invalidSyntax);

		List<Argument> missing = closest.missingSyntax(args.length);
		for (int i = 0; i < missing.size(); i++)
		{
			Argument arg = missing.get(i);
			String line = "&c" + arg.getArgument();
			if (i != 0)
				line = "&4, " + line;

			builder.append(FormatUtil.format(line));
			String explanation = arg.getExplanation();
			if (explanation != null)
				builder.event(new HoverEvent(Action.SHOW_TEXT, FormatUtil.format("&4{0}:\n&f{1}", arg.getArgument(), explanation)));
		}

		sendMessage(builder.create());
	}

	/**
	 * Finds the closest Syntax match for a given array of arguments.
	 * 
	 * @param args Arguments to find syntax for
	 * @return The syntax, defaulting to {@link defaultSyntax()}
	 */
	private final Syntax findClosest(String[] args)
	{
		if (syntaxes.size() == 1 || args.length == 0)
			return defaultSyntax();

		// Find the closest match
		Syntax closest = null;
		int delta = -1;

		for (Syntax syntax : syntaxes)
		{
			int curDelta = Math.abs(syntax.size() - args.length);
			if (curDelta < delta || curDelta == -1)
			{
				closest = syntax;
				delta = curDelta;
			}

			if (curDelta == 0)
				break;
		}

		return closest != null ? closest : defaultSyntax();
	}

	/**
	 * Gets the default Syntax.
	 * 
	 * @return The default syntax
	 */
	private final Syntax defaultSyntax()
	{
		return syntaxes.get(0);
	}

	/**
	 * Adds a argument to the current syntax.
	 * 
	 * @param arg Argument name
	 * @param explanation Short description for the argument
	 * @param required Whether or not it is required
	 */
	protected final void addArgument(String arg, String explanation, boolean required)
	{
		Syntax syntax = syntaxes.get(syntaxes.size() - 1);
		syntax.add(new Argument(arg, explanation, required));
	}

	/**
	 * Adds a required argument to the current syntax.
	 * 
	 * @param arg Argument name
	 */
	protected final void addRequiredArg(String arg)
	{
		addArgument(arg, null, true);
	}

	/**
	 * Adds a required argument to the current syntax.
	 * 
	 * @param arg Argument name
	 * @param explanation Short description for the argument
	 */
	protected final void addRequiredArg(String arg, String explanation)
	{
		addArgument(arg, explanation, true);
	}

	/**
	 * Adds an optional argument to the current syntax.
	 * 
	 * @param arg Argument name
	 */
	protected final void addOptionalArg(String arg)
	{
		addArgument(arg, null, false);
	}

	/**
	 * Adds an optional argument to the current syntax.
	 * 
	 * @param arg Argument name
	 * @param explanation Short description for the argument
	 */
	protected final void addOptionalArg(String arg, String explanation)
	{
		addArgument(arg, explanation, false);
	}

	@Data
	@AllArgsConstructor
	public class Argument
	{
		private final String argument;
		private final String explanation;
		private final boolean required;
	}

	public class Syntax extends ArrayList<Argument>
	{
		private static final long serialVersionUID = 1L;

		public final int requiredSize()
		{
			int required = 0;
			for (Argument arg : this)
			{
				if (arg.isRequired())
					required++;
			}

			return required;
		}

		public final List<Argument> missingSyntax(int size)
		{
			List<Argument> ret = new ArrayList<>();

			int required = requiredSize();
			for (int i = size; i < required; i++)
				ret.add(get(i, true));

			return ret;
		}

		public final Argument get(int index, boolean required)
		{
			int i = 0;

			for (Argument arg : this)
			{
				if (arg.isRequired() == required)
				{
					if (i == index)
						return arg;
					i++;
				}
			}

			return null;
		}
	}

	/**
	 * Utility class for easily creating multiple sets of Syntax.
	 * 
	 * @author dmulloy2
	 */
	public class SyntaxBuilder
	{
		private final List<Syntax> syntaxes;

		/**
		 * Creates a new SyntaxBuilder
		 */
		public SyntaxBuilder()
		{
			this.syntaxes = new ArrayList<>();
			syntaxes.add(new Syntax());
		}

		/**
		 * Switches to a new Syntax, saving the previous one
		 * 
		 * @return This, for chanining
		 */
		public SyntaxBuilder newSyntax()
		{
			syntaxes.add(new Syntax());
			return this;
		}

		/**
		 * Adds a required argument to the current Syntax
		 * 
		 * @param arg Argument name
		 * @return This, for chaining
		 */
		public SyntaxBuilder requiredArg(String arg)
		{
			add(arg, null, true);
			return this;
		}

		/**
		 * Adds a required argument to the current Syntax
		 * 
		 * @param arg Argument name
		 * @param explanation Short description for the argument
		 * @return This, for chaining
		 */
		public SyntaxBuilder requiredArg(String arg, String explanation)
		{
			add(arg, explanation, true);
			return this;
		}

		/**
		 * Adds an optional argument to the current Syntax
		 * 
		 * @param arg Argument name
		 * @return This, for chaining
		 */
		public SyntaxBuilder optionalArg(String arg)
		{
			add(arg, null, false);
			return this;
		}

		/**
		 * Adds an optional argument to the current Syntax
		 * 
		 * @param arg Argument name
		 * @param explanation Short description for the argument
		 * @return This, for chaining
		 */
		public SyntaxBuilder optionalArg(String arg, String explanation)
		{
			add(arg, explanation, false);
			return this;
		}

		/**
		 * Adds an argument to the current Syntax
		 * 
		 * @param arg Argument name
		 * @param explanation Short description for the argument
		 * @param required Whether or not the argument is required
		 * @return This, for chaining
		 */
		public SyntaxBuilder add(String arg, String explanation, boolean required)
		{
			Syntax current = syntaxes.get(syntaxes.size() - 1);
			current.add(new Argument(arg, explanation, required));
			return this;
		}

		/**
		 * Compiles the Syntaxes into a single list
		 * 
		 * @return The list
		 */
		public List<Syntax> build()
		{
			return syntaxes;
		}
	}
}