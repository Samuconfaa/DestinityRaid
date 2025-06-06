package it.samuconfaa.destinityRaid;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorldSelectorGUI implements Listener {
    private final DestinityRaid plugin;

    public WorldSelectorGUI(DestinityRaid plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Seleziona Mondo");

        Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();
        int slot = 0;

        for (Map.Entry<String, ConfigurationManager.WorldInfo> entry : worlds.entrySet()) {
            if (slot >= 27) break;

            String worldKey = entry.getKey();
            ConfigurationManager.WorldInfo worldInfo = entry.getValue();

            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + worldInfo.getDisplayName());

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Mondo: " + worldInfo.getWorldName());

                if (plugin.getWorldManager().isWorldOccupied(worldKey)) {
                    lore.add(ChatColor.RED + "Occupato");
                    item.setType(Material.BARRIER);
                } else {
                    lore.add(ChatColor.GREEN + "Disponibile");
                    lore.add(ChatColor.YELLOW + "Clicca per entrare!");
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            gui.setItem(slot, item);
            slot++;
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Seleziona Mondo")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Se ha cliccato su un mondo non disponibile, non fare nulla
        if (clickedItem.getType() == Material.BARRIER) {
            player.sendMessage(ChatColor.RED + "Questo mondo è già occupato!");
            return;
        }

        // Trova il mondo corrispondente al click
        String selectedWorldKey = findWorldKeyByDisplayName(clickedItem);
        if (selectedWorldKey == null) {
            player.sendMessage(ChatColor.RED + "Errore: Mondo non trovato!");
            return;
        }

        player.closeInventory();

        // Prova ad iniziare il raid
        if (attemptRaidStart(player, selectedWorldKey)) {
            player.sendMessage(ChatColor.GREEN + "Teletrasporto in corso...");
        }
    }

    private String findWorldKeyByDisplayName(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();

        for (Map.Entry<String, ConfigurationManager.WorldInfo> entry : worlds.entrySet()) {
            if (entry.getValue().getDisplayName().equals(displayName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean attemptRaidStart(Player player, String worldKey) {
        // Controlla se il mondo è disponibile
        if (plugin.getWorldManager().isWorldOccupied(worldKey)) {
            player.sendMessage(ChatColor.RED + "Questo mondo è già occupato!");
            return false;
        }

        // Controlli per i party utilizzando il metodo di validazione integrato
        String partyValidationError = plugin.getPartyManager().validatePartyForRaid(player);
        if (partyValidationError != null) {
            player.sendMessage(partyValidationError);
            return false;
        }

        // Ottieni i membri del party (se Parties è abilitato) o solo il giocatore
        List<Player> partyMembers = plugin.getPartyManager().getPartyMembers(player);

        // Teletrasporta il giocatore/party
        return teleportPlayersToWorld(player, worldKey, partyMembers);
    }

    private boolean teleportPlayersToWorld(Player leader, String worldKey, List<Player> players) {
        Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();
        ConfigurationManager.WorldInfo worldInfo = worlds.get(worldKey);

        if (worldInfo == null) {
            leader.sendMessage(ChatColor.RED + "Errore: Configurazione mondo non trovata!");
            return false;
        }

        World world = Bukkit.getWorld(worldInfo.getWorldName());
        if (world == null) {
            leader.sendMessage(ChatColor.RED + "Errore: Mondo non trovato sul server!");
            return false;
        }

        Location spawnLocation = new Location(world, worldInfo.getSpawnX(), worldInfo.getSpawnY(), worldInfo.getSpawnZ());

        // Occupa il mondo
        plugin.getWorldManager().occupyWorld(worldKey, leader);

        // Inizia il raid
        plugin.getRaidStatsManager().startRaid(leader, worldKey);

        // Teletrasporta e prepara tutti i giocatori
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.teleport(spawnLocation);

                // Inizializza il sistema di morti per il raid
                plugin.getDeathManager().onRaidStart(player);

                // PRIMA svuota l'inventario, POI equipaggia il kit del giocatore
                player.getInventory().clear();
                player.getInventory().setArmorContents(new ItemStack[4]);

                // Carica il kit personalizzato del giocatore (ora ogni player ha il suo kit)
                plugin.getKitManager().loadPlayerKit(player);

                player.sendMessage(ChatColor.GREEN + "Raid iniziato in " + worldInfo.getDisplayName() + "!");
                player.sendMessage(ChatColor.YELLOW + "Il tuo kit personalizzato è stato equipaggiato!");
                player.sendMessage(ChatColor.YELLOW + "Ricorda: hai solo 2 vite, poi diventerai spettatore!");
            }
        }

        return true;
    }
}