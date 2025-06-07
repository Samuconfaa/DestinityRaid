package it.samuconfaa.destinityRaid;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class StatsCommand implements CommandExecutor {
    private final DestinityRaid plugin;

    public StatsCommand(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cQuesto comando può essere usato solo dai giocatori!");
            return true;
        }

        Player player = (Player) sender;
        String targetName;

        if (args.length == 0) {
            // Mostra le statistiche del giocatore stesso
            targetName = player.getName();
        } else {
            // Mostra le statistiche di un altro giocatore
            targetName = args[0];

            // Controlla se il giocatore target esiste (online o offline)
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                // Il giocatore non è online, controlla se ha mai giocato
                if (plugin.getRaidStatsManager().getPlayerTotalRaids(targetName) == 0) {
                    player.sendMessage("§cGiocatore §e" + targetName + " §cnon trovato o senza statistiche!");
                    return true;
                }
            } else {
                targetName = target.getName(); // Usa il nome corretto (maiuscole/minuscole)
            }
        }

        showPlayerStats(player, targetName);
        return true;
    }

    private void showPlayerStats(Player viewer, String targetName) {
        RaidStatsManager statsManager = plugin.getRaidStatsManager();

        // Ottieni statistiche base
        int totalRaids = statsManager.getPlayerTotalRaids(targetName);

        if (totalRaids == 0) {
            viewer.sendMessage("§c" + targetName + " non ha ancora completato nessun raid!");
            return;
        }

        // Header
        if (targetName.equalsIgnoreCase(viewer.getName())) {
            viewer.sendMessage("§6§l=== LE TUE STATISTICHE RAID ===");
        } else {
            viewer.sendMessage("§6§l=== STATISTICHE DI " + targetName.toUpperCase() + " ===");
        }
        viewer.sendMessage("");

        // Statistiche generali
        viewer.sendMessage("§e§lStatistiche Generali:");
        viewer.sendMessage("§7• Raid completati: §e" + totalRaids);

        // Posizione nella classifica generale
        String totalRank = getTotalRank(targetName);
        viewer.sendMessage("§7• Posizione classifica: §e" + totalRank);
        viewer.sendMessage("");

        // Statistiche per mondo
        viewer.sendMessage("§e§lStatistiche per Mondo:");
        Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();

        boolean hasWorldStats = false;
        for (Map.Entry<String, ConfigurationManager.WorldInfo> entry : worlds.entrySet()) {
            String worldKey = entry.getKey();
            ConfigurationManager.WorldInfo worldInfo = entry.getValue();

            int worldRaids = statsManager.getPlayerWorldRaids(targetName, worldKey);
            String bestTime = statsManager.getPlayerBestTime(targetName, worldKey);

            if (worldRaids > 0) {
                hasWorldStats = true;
                String worldRank = getWorldRank(targetName, worldKey);

                viewer.sendMessage("§6" + worldInfo.getDisplayName() + ":");
                viewer.sendMessage("  §7• Raid completati: §e" + worldRaids);
                viewer.sendMessage("  §7• Tempo migliore: §e" + bestTime);
                viewer.sendMessage("  §7• Posizione: §e" + worldRank);
                viewer.sendMessage("");
            }
        }

        if (!hasWorldStats) {
            viewer.sendMessage("§7Nessuna statistica per mondo disponibile.");
            viewer.sendMessage("");
        }

        // Footer con suggerimenti
        if (targetName.equalsIgnoreCase(viewer.getName())) {
            viewer.sendMessage("§7Usa §e/leaderboard §7per vedere le classifiche!");
        } else {
            viewer.sendMessage("§7Usa §e/stats §7per vedere le tue statistiche");
            viewer.sendMessage("§7o §e/leaderboard §7per le classifiche!");
        }
    }

    private String getTotalRank(String playerName) {
        var leaderboard = plugin.getRaidStatsManager().getTotalRaidsLeaderboard(1000);
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getPlayerName().equalsIgnoreCase(playerName)) {
                return (i + 1) + "°";
            }
        }
        return "Non classificato";
    }

    private String getWorldRank(String playerName, String worldKey) {
        var leaderboard = plugin.getRaidStatsManager().getWorldBestTimesLeaderboard(worldKey, 1000);
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getPlayerName().equalsIgnoreCase(playerName)) {
                return (i + 1) + "°";
            }
        }
        return "Non classificato";
    }
}