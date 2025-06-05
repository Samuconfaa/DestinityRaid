package it.samuconfaa.destinityRaid;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RaidStatsManager {
    private final DestinityRaid plugin;
    private File statsFile;
    private FileConfiguration statsConfig;
    private Map<UUID, Long> activeRaids = new HashMap<>();

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
        plugin.getLogger().info("Raid iniziato per " + player.getName() + " nel mondo " + worldKey);
    }

    public void endRaid(Player player, String worldKey) {
        UUID playerUUID = player.getUniqueId();
        Long startTime = activeRaids.get(playerUUID);

        if (startTime == null) {
            plugin.getLogger().warning("Nessun raid attivo trovato per " + player.getName());
            return;
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Rimuovi il raid attivo
        activeRaids.remove(playerUUID);

        // Salva le statistiche
        saveRaidStats(player, worldKey, startTime, endTime, duration);

        // Mostra il tempo al giocatore
        String formattedTime = formatTime(duration);
        player.sendMessage("§6Raid completato in: §e" + formattedTime);

        plugin.getLogger().info("Raid completato per " + player.getName() + " nel mondo " + worldKey + " in " + formattedTime);
    }

    private void saveRaidStats(Player player, String worldKey, long startTime, long endTime, long duration) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String startDate = dateFormat.format(new Date(startTime));
        String endDate = dateFormat.format(new Date(endTime));

        // Genera un ID unico per questo raid
        String raidId = player.getName() + "_" + worldKey + "_" + startTime;

        // Salva i dati nel file YAML
        String basePath = "raids." + raidId;
        statsConfig.set(basePath + ".player", player.getName());
        statsConfig.set(basePath + ".player_uuid", player.getUniqueId().toString());
        statsConfig.set(basePath + ".world", worldKey);
        statsConfig.set(basePath + ".start_time", startDate);
        statsConfig.set(basePath + ".end_time", endDate);
        statsConfig.set(basePath + ".duration_ms", duration);
        statsConfig.set(basePath + ".duration_formatted", formatTime(duration));

        saveStats();
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
