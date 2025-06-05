package it.samuconfaa.destinityRaid;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            }
        }

        // Salva anche il numero totale di membri per facilità di lettura
        statsConfig.set(basePath + ".party_size", partyMembers.size());

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