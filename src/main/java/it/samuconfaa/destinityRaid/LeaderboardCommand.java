package it.samuconfaa.destinityRaid;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class LeaderboardCommand implements CommandExecutor {
    private final DestinityRaid plugin;

    public LeaderboardCommand(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cQuesto comando può essere usato solo dai giocatori!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Mostra la classifica generale
            showTotalRaidsLeaderboard(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "total":
            case "generale":
                showTotalRaidsLeaderboard(player);
                break;

            case "world":
            case "mondo":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /leaderboard mondo <nome_mondo>");
                    showAvailableWorlds(player);
                    return true;
                }
                String worldKey = args[1];
                showWorldLeaderboard(player, worldKey);
                break;

            case "help":
            case "aiuto":
                showHelp(player);
                break;

            default:
                player.sendMessage("§cComando non riconosciuto. Usa §e/leaderboard help §cper vedere i comandi disponibili.");
                break;
        }

        return true;
    }

    private void showTotalRaidsLeaderboard(Player player) {
        List<RaidStatsManager.PlayerRankingEntry> leaderboard = plugin.getRaidStatsManager().getTotalRaidsLeaderboard(10);

        player.sendMessage("§6§l=== CLASSIFICA RAID TOTALI ===");
        player.sendMessage("");

        if (leaderboard.isEmpty()) {
            player.sendMessage("§7Nessun dato disponibile.");
            return;
        }

        for (int i = 0; i < leaderboard.size(); i++) {
            RaidStatsManager.PlayerRankingEntry entry = leaderboard.get(i);
            String position = String.valueOf(i + 1);
            String playerName = entry.getPlayerName();
            String raids = String.valueOf(entry.getValue());

            String color = getPositionColor(i + 1);
            player.sendMessage(color + position + ". §f" + playerName + " §7- §e" + raids + " raid");
        }

        // Mostra la posizione del giocatore se non è nella top 10
        showPlayerRank(player, leaderboard, player.getName());
        player.sendMessage("");
        player.sendMessage("§7Usa §e/leaderboard mondo <nome> §7per vedere le classifiche per mondo specifico.");
    }

    private void showWorldLeaderboard(Player player, String worldKey) {
        // Verifica se il mondo esiste nella configurazione
        Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();
        if (!worlds.containsKey(worldKey)) {
            player.sendMessage("§cMondo non trovato! Mondi disponibili:");
            showAvailableWorlds(player);
            return;
        }

        ConfigurationManager.WorldInfo worldInfo = worlds.get(worldKey);
        List<RaidStatsManager.PlayerRankingEntry> leaderboard = plugin.getRaidStatsManager().getWorldBestTimesLeaderboard(worldKey, 10);

        player.sendMessage("§6§l=== CLASSIFICA " + worldInfo.getDisplayName().toUpperCase() + " ===");
        player.sendMessage("§7Tempi migliori:");
        player.sendMessage("");

        if (leaderboard.isEmpty()) {
            player.sendMessage("§7Nessun dato disponibile per questo mondo.");
            return;
        }

        for (int i = 0; i < leaderboard.size(); i++) {
            RaidStatsManager.PlayerRankingEntry entry = leaderboard.get(i);
            String position = String.valueOf(i + 1);
            String playerName = entry.getPlayerName();
            String time = entry.getFormattedValue();

            String color = getPositionColor(i + 1);
            player.sendMessage(color + position + ". §f" + playerName + " §7- §e" + time);
        }

        // Mostra la posizione del giocatore se non è nella top 10
        showPlayerWorldRank(player, worldKey, leaderboard, player.getName());
        player.sendMessage("");
        player.sendMessage("§7Il tuo miglior tempo: §e" + plugin.getRaidStatsManager().getPlayerBestTime(player.getName(), worldKey));
    }

    private void showAvailableWorlds(Player player) {
        Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();
        player.sendMessage("§7Mondi disponibili:");
        for (Map.Entry<String, ConfigurationManager.WorldInfo> entry : worlds.entrySet()) {
            player.sendMessage("§7- §e" + entry.getKey() + " §7(" + entry.getValue().getDisplayName() + ")");
        }
    }

    private void showPlayerRank(Player player, List<RaidStatsManager.PlayerRankingEntry> leaderboard, String playerName) {
        // Controlla se il giocatore è già nella top 10
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getPlayerName().equalsIgnoreCase(playerName)) {
                return; // Il giocatore è già mostrato nella top 10
            }
        }

        // Cerca la posizione del giocatore nella classifica completa
        List<RaidStatsManager.PlayerRankingEntry> fullLeaderboard = plugin.getRaidStatsManager().getTotalRaidsLeaderboard(1000);
        for (int i = 0; i < fullLeaderboard.size(); i++) {
            if (fullLeaderboard.get(i).getPlayerName().equalsIgnoreCase(playerName)) {
                player.sendMessage("");
                player.sendMessage("§7La tua posizione: §e" + (i + 1) + "° §7con §e" + fullLeaderboard.get(i).getValue() + " raid");
                return;
            }
        }

        // Il giocatore non ha ancora completato raid
        player.sendMessage("");
        player.sendMessage("§7Non hai ancora completato nessun raid!");
    }

    private void showPlayerWorldRank(Player player, String worldKey, List<RaidStatsManager.PlayerRankingEntry> leaderboard, String playerName) {
        // Controlla se il giocatore è già nella top 10
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getPlayerName().equalsIgnoreCase(playerName)) {
                return; // Il giocatore è già mostrato nella top 10
            }
        }

        // Cerca la posizione del giocatore nella classifica completa
        List<RaidStatsManager.PlayerRankingEntry> fullLeaderboard = plugin.getRaidStatsManager().getWorldBestTimesLeaderboard(worldKey, 1000);
        for (int i = 0; i < fullLeaderboard.size(); i++) {
            if (fullLeaderboard.get(i).getPlayerName().equalsIgnoreCase(playerName)) {
                player.sendMessage("");
                player.sendMessage("§7La tua posizione: §e" + (i + 1) + "° §7con §e" + fullLeaderboard.get(i).getFormattedValue());
                return;
            }
        }
    }

    private String getPositionColor(int position) {
        switch (position) {
            case 1:
                return "§6§l"; // Oro per il primo posto
            case 2:
                return "§7§l"; // Argento per il secondo posto
            case 3:
                return "§c§l"; // Bronzo per il terzo posto
            default:
                return "§e"; // Giallo per gli altri
        }
    }

    private void showHelp(Player player) {
        player.sendMessage("§6§l=== COMANDI LEADERBOARD ===");
        player.sendMessage("");
        player.sendMessage("§e/leaderboard §7- Mostra la classifica generale");
        player.sendMessage("§e/leaderboard total §7- Mostra la classifica generale");
        player.sendMessage("§e/leaderboard mondo <nome> §7- Mostra la classifica di un mondo specifico");
        player.sendMessage("§e/leaderboard help §7- Mostra questo messaggio");
        player.sendMessage("");
        player.sendMessage("§7Esempi:");
        player.sendMessage("§7- §e/leaderboard mondo mondo1");
        player.sendMessage("§7- §e/leaderboard total");
    }
}