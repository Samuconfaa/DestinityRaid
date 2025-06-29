package it.samuconfaa.destinityRaid;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RaidGUICommand implements CommandExecutor {

    private final DestinityRaid plugin;

    public RaidGUICommand(DestinityRaid plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "Questo comando può essere usato solo dai giocatori!");
            return true;
        }

        Player player = (Player) commandSender;
        plugin.getSelectorGUI().openGUI(player);
        return true;
    }
}
