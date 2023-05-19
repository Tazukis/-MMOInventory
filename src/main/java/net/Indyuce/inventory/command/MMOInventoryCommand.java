package net.Indyuce.inventory.command;

import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.gui.PlayerInventoryView;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MMOInventoryCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// Open own inventory
		if (args.length < 1 && sender instanceof Player) {
			if (!sender.hasPermission("mmoinventory.open")) {
				sender.sendMessage(MMOInventory.plugin.getTranslation("not-enough-perms"));
				return true;
			}

			new PlayerInventoryView((Player) sender).open();
			return true;
		}

		if (args.length == 0)
			return true;

		// Reload command
		if (args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("mmoinventory.admin")) {
				sender.sendMessage(MMOInventory.plugin.getTranslation("not-enough-perms"));
				return true;
			}

			MMOInventory.plugin.reload();
			sender.sendMessage(MMOInventory.plugin.getTranslation("reload").replace("{version}", MMOInventory.plugin.getDescription().getVersion()));
			return true;
		}
/*
		if (args[0].equalsIgnoreCase("exportdata")) {
			if (!sender.hasPermission("mmoinventory.admin")) {
				sender.sendMessage(MMOInventory.plugin.getTranslation("not-enough-perms"));
				return true;
			}

			new DataExport();
		}*/

		// Open inventory of someone else
		if (!sender.hasPermission("mmoinventory.open.other")) {
			sender.sendMessage(MMOInventory.plugin.getTranslation("not-enough-perms"));
			return true;
		}

		Player target = Bukkit.getPlayer(args[0]);
		if (target == null) {
			sender.sendMessage(MMOInventory.plugin.getTranslation("wrong-player").replace("{arg}", args[0]));
			return true;
		}

		new PlayerInventoryView((Player) sender, target).open();
		return true;
	}
}
