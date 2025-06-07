package it.samuconfaa.destinityRaid;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RaidStatsManager {
    private final DestinityRaid plugin;
    private File statsFile;
    private FileConfiguration statsConfig;
    private Map<UUID, Long> activeRaids = new HashMap<>();
    private Map<UUID, List<Player>> activeRaidParties = new HashMap<>();

    public RaidStatsManager(DestinityRaid plugin) {
        this.plugin = plugin;
        loadStatsFile();
    }

    private void loadStatsFile() {
        statsFile = new File(plugin.getDataFolder(), "raid_stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossibile creare il file raid_stats.yml!");
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    public void startRaid(Player player, String worldKey) {
        long startTime = System.currentTimeMillis();
        activeRaids.put(player.getUniqueId(), startTime);

        // Ottieni tutti i membri della party e salvali
        List<Player> partyMembers = plugin.getPartyManager().getPartyMembers(player);
        activeRaidParties.put(player.getUniqueId(), partyMembers);

        plugin.getLogger().info("Raid iniziato per " + player.getName() + " nel mondo " + worldKey +
                " con " + partyMembers.size() + " membri");
    }

    public void endRaid(Player player, String worldKey) {
        UUID playerUUID = player.getUniqueId();
        Long startTime = activeRaids.get(playerUUID);
        List<Player> partyMembers = activeRaidParties.get(playerUUID);

        if (startTime == null) {
            plugin.getLogger().warning("Nessun raid attivo trovato per " + player.getName());
            return;
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Rimuovi il raid attivo
        activeRaids.remove(playerUUID);
        activeRaidParties.remove(playerUUID);

        // Se non ci sono membri della party salvati, usa solo il player corrente
        if (partyMembers == null || partyMembers.isEmpty()) {
            partyMembers = List.of(player);
        }

        // Salva le statistiche per tutti i membri della party
        saveRaidStats(partyMembers, worldKey, startTime, endTime, duration, player.getName());

        // Mostra il tempo a tutti i membri della party
        String formattedTime = formatTime(duration);
        for (Player member : partyMembers) {
            if (member != null && member.isOnline()) {
                member.sendMessage("§6Raid completato in: §e" + formattedTime);
            }
        }

        plugin.getLogger().info("Raid completato per la party di " + player.getName() +
                " nel mondo " + worldKey + " in " + formattedTime +
                " (" + partyMembers.size() + " membri)");
    }

    private void saveRaidStats(List<Player> partyMembers, String worldKey, long startTime, long endTime, long duration, String leaderName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String startDate = dateFormat.format(new Date(startTime));
        String endDate = dateFormat.format(new Date(endTime));

        // Genera un ID unico per questo raid basato sul leader e timestamp
        String raidId = leaderName + "_" + worldKey + "_" + startTime;

        // Salva i dati generali del raid
        String basePath = "raids." + raidId;
        statsConfig.set(basePath + ".leader", leaderName);
        statsConfig.set(basePath + ".world", worldKey);
        statsConfig.set(basePath + ".start_time", startDate);
        statsConfig.set(basePath + ".end_time", endDate);
        statsConfig.set(basePath + ".duration_ms", duration);
        statsConfig.set(basePath + ".duration_formatted", formatTime(duration));

        // Salva tutti i membri della party
        for (int i = 0; i < partyMembers.size(); i++) {
            Player member = partyMembers.get(i);
            if (member != null) {
                String memberPath = basePath + ".members." + i;
                statsConfig.set(memberPath + ".name", member.getName());
                statsConfig.set(memberPath + ".uuid", member.getUniqueId().toString());

                // Aggiorna le statistiche personali del giocatore
                updatePlayerStats(member, worldKey, duration);
            }
        }

        // Salva anche il numero totale di membri per facilità di lettura
        statsConfig.set(basePath + ".party_size", partyMembers.size());

        saveStats();
    }

    // Aggiorna le statistiche personali di un giocatore
    private void updatePlayerStats(Player player, String worldKey, long duration) {
        String playerPath = "player_stats." + player.getUniqueId();

        // Aggiorna nome giocatore (nel caso sia cambiato)
        statsConfig.set(playerPath + ".name", player.getName());

        // Aggiorna il numero totale di raid completati
        int totalRaids = statsConfig.getInt(playerPath + ".total_raids", 0);
        statsConfig.set(playerPath + ".total_raids", totalRaids + 1);

        // Aggiorna raid completati per questo mondo
        int worldRaids = statsConfig.getInt(playerPath + ".worlds." + worldKey + ".completed", 0);
        statsConfig.set(playerPath + ".worlds." + worldKey + ".completed", worldRaids + 1);

        // Aggiorna il tempo migliore per questo mondo
        long currentBest = statsConfig.getLong(playerPath + ".worlds." + worldKey + ".best_time", Long.MAX_VALUE);
        if (duration < currentBest) {
            statsConfig.set(playerPath + ".worlds." + worldKey + ".best_time", duration);
            statsConfig.set(playerPath + ".worlds." + worldKey + ".best_time_formatted", formatTime(duration));
        }

        // Aggiorna l'ultimo raid completato
        statsConfig.set(playerPath + ".last_raid", System.currentTimeMillis());
    }

    // Reset delle statistiche di un giocatore (usando l'oggetto Player)
    public boolean resetPlayerStats(Player player) {
        String playerPath = "player_stats." + player.getUniqueId();
        if (statsConfig.contains(playerPath)) {
            statsConfig.set(playerPath, null);
            saveStats();
            return true;
        }
        return false;
    }

    // Reset delle statistiche di un giocatore (usando il nome)
    public boolean resetPlayerStatsByName(String playerName) {
        ConfigurationSection playerStatsSection = statsConfig.getConfigurationSection("player_stats");
        if (playerStatsSection == null) return false;

        for (String uuidString : playerStatsSection.getKeys(false)) {
            String name = statsConfig.getString("player_stats." + uuidString + ".name");
            if (playerName.equalsIgnoreCase(name)) {
                statsConfig.set("player_stats." + uuidString, null);
                saveStats();
                return true;
            }
        }
        return false;
    }

    // Ottieni il numero totale di raid completati da un giocatore
    public int getPlayerTotalRaids(String playerName) {
        ConfigurationSection playerStatsSection = statsConfig.getConfigurationSection("player_stats");
        if (playerStatsSection == null) return 0;

        for (String uuidString : playerStatsSection.getKeys(false)) {
            String name = statsConfig.getString("player_stats." + uuidString + ".name");
            if (playerName.equalsIgnoreCase(name)) {
                return statsConfig.getInt("player_stats." + uuidString + ".total_raids", 0);
            }
        }
        return 0;
    }

    // Ottieni il tempo migliore di un giocatore per un mondo specifico
    public String getPlayerBestTime(String playerName, String worldKey) {
        ConfigurationSection playerStatsSection = statsConfig.getConfigurationSection("player_stats");
        if (playerStatsSection == null) return "Nessun record";

        for (String uuidString : playerStatsSection.getKeys(false)) {
            String name = statsConfig.getString("player_stats." + uuidString + ".name");
            if (playerName.equalsIgnoreCase(name)) {
                String bestTime = statsConfig.getString("player_stats." + uuidString + ".worlds." + worldKey + ".best_time_formatted");
                return bestTime != null ? bestTime : "Nessun record";
            }
        }
        return "Nessun record";
    }

    // Ottieni il numero di raid completati da un giocatore per un mondo specifico
    public int getPlayerWorldRaids(String playerName, String worldKey) {
        ConfigurationSection playerStatsSection = statsConfig.getConfigurationSection("player_stats");
        if (playerStatsSection == null) return 0;

        for (String uuidString : playerStatsSection.getKeys(false)) {
            String name = statsConfig.getString("player_stats." + uuidString + ".name");
            if (playerName.equalsIgnoreCase(name)) {
                return statsConfig.getInt("player_stats." + uuidString + ".worlds." + worldKey + ".completed", 0);
            }
        }
        return 0;
    }

    // Ottieni la classifica per raid totali completati
    public List<PlayerRankingEntry> getTotalRaidsLeaderboard(int limit) {
        List<PlayerRankingEntry> leaderboard = new ArrayList<>();
        ConfigurationSection playerStatsSection = statsConfig.getConfigurationSection("player_stats");
        if (playerStatsSection == null) return leaderboard;

        for (String uuidString : playerStatsSection.getKeys(false)) {
            String name = statsConfig.getString("player_stats." + uuidString + ".name");
            int totalRaids = statsConfig.getInt("player_stats." + uuidString + ".total_raids", 0);

            if (totalRaids > 0) {
                leaderboard.add(new PlayerRankingEntry(name, totalRaids, ""));
            }
        }

        return leaderboard.stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Ottieni la classifica per tempi migliori in un mondo specifico
    public List<PlayerRankingEntry> getWorldBestTimesLeaderboard(String worldKey, int limit) {
        List<PlayerRankingEntry> leaderboard = new ArrayList<>();
        ConfigurationSection playerStatsSection = statsConfig.getConfigurationSection("player_stats");
        if (playerStatsSection == null) return leaderboard;

        for (String uuidString : playerStatsSection.getKeys(false)) {
            String name = statsConfig.getString("player_stats." + uuidString + ".name");
            long bestTime = statsConfig.getLong("player_stats." + uuidString + ".worlds." + worldKey + ".best_time", Long.MAX_VALUE);
            String bestTimeFormatted = statsConfig.getString("player_stats." + uuidString + ".worlds." + worldKey + ".best_time_formatted");

            if (bestTime != Long.MAX_VALUE && bestTimeFormatted != null) {
                leaderboard.add(new PlayerRankingEntry(name, (int) bestTime, bestTimeFormatted));
            }
        }

        return leaderboard.stream()
                .sorted((a, b) -> Integer.compare(a.getValue(), b.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Classe per le voci della classifica
    public static class PlayerRankingEntry {
        private final String playerName;
        private final int value;
        private final String formattedValue;

        public PlayerRankingEntry(String playerName, int value, String formattedValue) {
            this.playerName = playerName;
            this.value = value;
            this.formattedValue = formattedValue;
        }

        public String getPlayerName() { return playerName; }
        public int getValue() { return value; }
        public String getFormattedValue() { return formattedValue; }
    }

    public void saveStats() {
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossibile salvare il file raid_stats.yml!");
            e.printStackTrace();
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public FileConfiguration getStatsConfig() {
        return statsConfig;
    }
}