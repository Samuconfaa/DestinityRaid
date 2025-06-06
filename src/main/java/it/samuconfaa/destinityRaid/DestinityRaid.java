package it.samuconfaa.destinityRaid;

import org.bukkit.plugin.java.JavaPlugin;

public final class DestinityRaid extends JavaPlugin {
    private ConfigurationManager configManager;
    private WorldManager worldManager;
    private RaidStatsManager raidStatsManager;
    private PartyManager partyManager;
    private DeathManager deathManager;
    private KitManager kitManager;
    private static DestinityRaid instance;

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

        // Registra eventi e comandi
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(deathManager, this);
        getCommand("raid").setExecutor(new RaidCommand(this));
        getCommand("kit").setExecutor(new KitCommand(this));

        getLogger().info("DestinityRaid plugin abilitato!");
    }

    @Override
    public void onDisable() {
        if (raidStatsManager != null) {
            raidStatsManager.saveStats();
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
}