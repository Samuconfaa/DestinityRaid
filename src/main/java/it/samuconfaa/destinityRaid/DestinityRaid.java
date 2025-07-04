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
    private  WorldSelectorGUI selectorGUI;
    private DestinityRaidPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigurationManager(this);
        configManager.loadConfig();

        worldManager = new WorldManager();
        raidStatsManager = new RaidStatsManager(this);
        partyManager = new PartyManager(this);
        selectorGUI = new WorldSelectorGUI(this);
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
        getCommand("raidgui").setExecutor(new RaidGUICommand(this));

        setupPlaceholders();

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

        // Gestione sicura dell'unregister dei placeholder
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
                getLogger().info("PlaceholderAPI expansion unregistered successfully.");
            } catch (Exception e) {
                getLogger().warning("Error unregistering PlaceholderAPI expansion: " + e.getMessage());
            }
        }

        getLogger().info("DestinityRaid plugin disabilitato!");
    }

    private void setupPlaceholders() {
        try {
            // Verifica se PlaceholderAPI è presente e abilitato
            if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                getLogger().info("PlaceholderAPI not found, placeholder integration disabled.");
                return;
            }

            // Aspetta un tick prima di registrare i placeholder per evitare problemi di inizializzazione
            Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    placeholderExpansion = new DestinityRaidPlaceholderExpansion(this);

                    if (placeholderExpansion.register()) {
                        getLogger().info("PlaceholderAPI integration enabled!");
                    } else {
                        getLogger().warning("Failed to register PlaceholderAPI expansion!");
                        placeholderExpansion = null;
                    }
                } catch (Exception e) {
                    getLogger().severe("Error registering PlaceholderAPI expansion: " + e.getMessage());
                    placeholderExpansion = null;
                }
            }, 1L);

        } catch (Exception e) {
            getLogger().severe("Error during PlaceholderAPI setup: " + e.getMessage());
            placeholderExpansion = null;
        }
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

    public WorldSelectorGUI getSelectorGUI() {
        return selectorGUI;
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