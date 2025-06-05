package it.samuconfaa.destinityRaid;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigurationManager {
    private static DestinityRaid plugin;
    private static FileConfiguration config;
    private static File configFile;

    public ConfigurationManager(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public static void reloadConfig(){
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static String GetNoReloadPermission(){
        return config.getString("messaggi.no_permission", "&cNon hai il permesso per eseguire questo comando!");
    }

    public static String GetReload(){
        return config.getString("messaggi.reload_done", "&aConfig ricaricata con successo!");
    }

    // Metodi per i messaggi delle party
    public static String getPartyNotFoundMessage() {
        return config.getString("messaggi.party_not_found", "&cNon sei in una party o la party non è stata trovata!");
    }

    public static String getPartyTooSmallMessage() {
        return config.getString("messaggi.party_too_small", "&cLa tua party deve avere almeno {min} membri per iniziare un raid!");
    }

    public static String getPartyTooBigMessage() {
        return config.getString("messaggi.party_too_big", "&cLa tua party ha troppi membri! Massimo {max} membri per raid.");
    }

    public static String getNotPartyLeaderMessage() {
        return config.getString("messaggi.not_party_leader", "&cSolo il leader della party può iniziare un raid!");
    }

    public static String getPartyMembersNotOnlineMessage() {
        return config.getString("messaggi.party_members_not_online", "&cTutti i membri della party devono essere online per iniziare un raid!");
    }

    // Metodi per la configurazione delle party
    public static int getPartyMinMembers() {
        return config.getInt("party.min_members", 2);
    }

    public static int getPartyMaxMembers() {
        return config.getInt("party.max_members", 6);
    }

    public static boolean isRequirePartyLeader() {
        return config.getBoolean("party.require_leader", true);
    }

    public static String getHubWorldName() {
        return config.getString("hub.world_name", "world");
    }

    public static Map<String, WorldInfo> getWorlds() {
        Map<String, WorldInfo> worlds = new HashMap<>();
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");

        if (worldsSection != null) {
            Set<String> worldKeys = worldsSection.getKeys(false);
            for (String key : worldKeys) {
                ConfigurationSection worldSection = worldsSection.getConfigurationSection(key);
                if (worldSection != null) {
                    String worldName = worldSection.getString("world_name");
                    String displayName = worldSection.getString("display_name", key);
                    double spawnX = worldSection.getDouble("spawn.x", 0);
                    double spawnY = worldSection.getDouble("spawn.y", 64);
                    double spawnZ = worldSection.getDouble("spawn.z", 0);
                    double exitX = worldSection.getDouble("exit.x", 0);
                    double exitY = worldSection.getDouble("exit.y", 64);
                    double exitZ = worldSection.getDouble("exit.z", 0);

                    WorldInfo worldInfo = new WorldInfo(worldName, displayName, spawnX, spawnY, spawnZ, exitX, exitY, exitZ);
                    worlds.put(key, worldInfo);
                }
            }
        }
        return worlds;
    }

    // Nuovo metodo per ottenere i comandi console per un mondo specifico
    public static List<String> getConsoleCommands(String worldKey) {
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldKey);
            if (worldSection != null) {
                return worldSection.getStringList("console_commands");
            }
        }
        return null;
    }

    // Metodo per ottenere i comandi console globali (eseguiti per tutti i mondi)
    public static List<String> getGlobalConsoleCommands() {
        return config.getStringList("global_console_commands");
    }

    public static class WorldInfo {
        private String worldName;
        private String displayName;
        private double spawnX, spawnY, spawnZ;
        private double exitX, exitY, exitZ;

        public WorldInfo(String worldName, String displayName, double spawnX, double spawnY, double spawnZ, double exitX, double exitY, double exitZ) {
            this.worldName = worldName;
            this.displayName = displayName;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.spawnZ = spawnZ;
            this.exitX = exitX;
            this.exitY = exitY;
            this.exitZ = exitZ;
        }

        // Getters
        public String getWorldName() { return worldName; }
        public String getDisplayName() { return displayName; }
        public double getSpawnX() { return spawnX; }
        public double getSpawnY() { return spawnY; }
        public double getSpawnZ() { return spawnZ; }
        public double getExitX() { return exitX; }
        public double getExitY() { return exitY; }
        public double getExitZ() { return exitZ; }
    }
}