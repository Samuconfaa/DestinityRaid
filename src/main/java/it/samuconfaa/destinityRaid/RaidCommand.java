package it.samuconfaa.destinityRaid;

import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Set;

public class RaidCommand implements CommandExecutor {
    private final DestinityRaid plugin;

    public RaidCommand(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (label.equalsIgnoreCase("raid")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "=== Comandi Raid ===");
                sender.sendMessage(ChatColor.YELLOW + "/raid reload - Ricarica la configurazione");
                sender.sendMessage(ChatColor.YELLOW + "/raid stats [giocatore] - Mostra statistiche raid");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("raid.reload")) {
                    ConfigurationManager.reloadConfig();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationManager.GetReload()));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationManager.GetNoReloadPermission()));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("stats")) {
                if (sender.hasPermission("raid.stats")) {
                    String targetPlayer = args.length > 1 ? args[1] : sender.getName();
                    showRaidStats(sender, targetPlayer);
                } else {
                    sender.sendMessage(ChatColor.RED + "Non hai il permesso per vedere le statistiche!");
                }
                return true;
            }
        }
        return false;
    }

    private void showRaidStats(CommandSender sender, String playerName) {
        ConfigurationSection raidsSection = plugin.getRaidStatsManager().getStatsConfig().getConfigurationSection("raids");

        if (raidsSection == null) {
            sender.sendMessage(ChatColor.RED + "Nessuna statistica trovata!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Statistiche Raid per " + playerName + " ===");

        Set<String> raidIds = raidsSection.getKeys(false);
        int count = 0;

        for (String raidId : raidIds) {
            ConfigurationSection raid = raidsSection.getConfigurationSection(raidId);
            if (raid != null && raid.getString("player", "").equalsIgnoreCase(playerName)) {
                String world = raid.getString("world", "Sconosciuto");
                String startTime = raid.getString("start_time", "Sconosciuto");
                String endTime = raid.getString("end_time", "Sconosciuto");
                String duration = raid.getString("duration_formatted", "Sconosciuto");

                sender.sendMessage(ChatColor.YELLOW + "Mondo: " + ChatColor.WHITE + world);
                sender.sendMessage(ChatColor.YELLOW + "Inizio: " + ChatColor.WHITE + startTime);
                sender.sendMessage(ChatColor.YELLOW + "Fine: " + ChatColor.WHITE + endTime);
                sender.sendMessage(ChatColor.YELLOW + "Durata: " + ChatColor.GREEN + duration);
                sender.sendMessage(ChatColor.GRAY + "------------------------");
                count++;
            }
        }

        if (count == 0) {
            sender.sendMessage(ChatColor.RED + "Nessun raid trovato per " + playerName);
        } else {
            sender.sendMessage(ChatColor.GOLD + "Totale raid completati: " + count);
        }
    }
}