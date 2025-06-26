package it.samuconfaa.destinityRaid;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DestinityRaid extends JavaPlugin {
    private ConfigurationManager configManager;
    private WorldManager worldManager;
    private RaidStatsManager raidStatsManager;
    private PartyManager partyManager;
    private DeathManager deathManager;
    private KitManager kitManager;
    private WorldBackupManager worldBackupManager; // NUOVO
    private static DestinityRaid instance;
    private KitGUI kitGUI;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigurationManager(this);
        configManager.loadConfig();

        worldManager = new WorldManager();
        raidStatsManager = new RaidStatsManager(this);
        partyManager = new PartyManager(this);
        deathManager = new DeathManager(this);
        kitManager = new KitManager(this);
        worldBackupManager = new WorldBackupManager(this); // NUOVO
        this.kitManager = new KitManager(this);
        this.kitGUI = new KitGUI(this);

        // Registra eventi e comandi
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(deathManager, this);
        getCommand("raid").setExecutor(new RaidCommand(this));
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("resetstats").setExecutor(new ResetStatsCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));


        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RaidPlaceholders(this).register();
            getLogger().info("PlaceholderAPI expansion registered!");
        } else {
            getLogger().warning("PlaceholderAPI not found!");
        }
        getLogger().info("DestinityRaid plugin abilitato!");


    }

    @Override
    public void onDisable() {
        if (raidStatsManager != null) {
            raidStatsManager.saveStats();
        }

        // NUOVO: Pulisci tutti i backup attivi al shutdown del plugin
        if (worldBackupManager != null) {
            for (String worldKey : worldBackupManager.getActiveBackups().keySet()) {
                worldBackupManager.cleanupBackup(worldKey);
                getLogger().info("Backup pulito per il mondo: " + worldKey);
            }
        }

        getLogger().info("DestinityRaid plugin disabilitato!");
    }

    public static DestinityRaid getInstance() {
        return instance;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public RaidStatsManager getRaidStatsManager() {
        return raidStatsManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public DeathManager getDeathManager() {
        return deathManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public KitGUI getKitGUI() {
        return kitGUI;
    }

    // NUOVO GETTER
    public WorldBackupManager getWorldBackupManager() {
        return worldBackupManager;
    }
}