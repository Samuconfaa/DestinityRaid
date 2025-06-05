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

import java.util.List;
import java.util.Map;

public class WorldSelectorGUI implements Listener {
    private final DestinityRaid plugin;

    public WorldSelectorGUI(DestinityRaid plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {
        Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();

        int size = Math.max(9, ((worlds.size() / 9) + 1) * 9);
        Inventory gui = Bukkit.createInventory(null, size, ChatColor.DARK_PURPLE + "Seleziona Mondo");

        int slot = 0;
        for (Map.Entry<String, ConfigurationManager.WorldInfo> entry : worlds.entrySet()) {
            String worldKey = entry.getKey();
            ConfigurationManager.WorldInfo worldInfo = entry.getValue();

            Material material = plugin.getWorldManager().isWorldOccupied(worldKey) ?
                    Material.RED_WOOL : Material.GREEN_WOOL;

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + worldInfo.getDisplayName());

                if (plugin.getWorldManager().isWorldOccupied(worldKey)) {
                    meta.setLore(java.util.Arrays.asList(ChatColor.RED + "Mondo occupato!"));
                } else {
                    meta.setLore(java.util.Arrays.asList(ChatColor.GREEN + "Clicca per entrare!"));
                }

                item.setItemMeta(meta);
            }

            gui.setItem(slot, item);
            slot++;
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Seleziona Mondo")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem.getType() == Material.RED_WOOL) {
                player.sendMessage(ChatColor.RED + "Questo mondo è già occupato!");
                return;
            }

            if (clickedItem.getType() == Material.GREEN_WOOL) {
                // Validazione party prima di permettere l'accesso
                String validationError = plugin.getPartyManager().validatePartyForRaid(player);
                if (validationError != null) {
                    player.sendMessage(validationError);
                    player.closeInventory();
                    return;
                }

                String worldDisplayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

                // Trova il mondo corrispondente
                Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();
                for (Map.Entry<String, ConfigurationManager.WorldInfo> entry : worlds.entrySet()) {
                    ConfigurationManager.WorldInfo worldInfo = entry.getValue();
                    if (worldInfo.getDisplayName().equals(worldDisplayName)) {
                        teleportToWorld(player, entry.getKey(), worldInfo);
                        break;
                    }
                }
            }

            player.closeInventory();
        }
    }

    private void teleportToWorld(Player player, String worldKey, ConfigurationManager.WorldInfo worldInfo) {
        World world = plugin.getServer().getWorld(worldInfo.getWorldName());
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Errore: Il mondo non esiste!");
            return;
        }

        Location spawnLocation = new Location(world, worldInfo.getSpawnX(), worldInfo.getSpawnY(), worldInfo.getSpawnZ());

        // Ottieni tutti i membri del party
        List<Player> partyMembers = plugin.getPartyManager().getPartyMembers(player);

        // Teletrasporta tutti i membri del party
        for (Player member : partyMembers) {
            member.teleport(spawnLocation);
            member.sendMessage(ChatColor.GREEN + "Benvenuto nel mondo: " + worldInfo.getDisplayName());
            member.sendMessage(ChatColor.YELLOW + "Trova il blocco d'oro per completare il raid!");
        }

        // Occupa il mondo con il player che ha avviato il raid
        plugin.getWorldManager().occupyWorld(worldKey, player);
        plugin.getRaidStatsManager().startRaid(player, worldKey);

        // Invia messaggio di conferma al party
        String partyMessage = ChatColor.GREEN + "Raid iniziato nel mondo: " + worldInfo.getDisplayName();
        plugin.getPartyManager().sendMessageToParty(player, partyMessage);
    }
}