package it.samuconfaa.destinityRaid;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Map;

public class PlayerListener implements Listener {
    private final DestinityRaid plugin;

    public PlayerListener(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String hubWorldName = ConfigurationManager.getHubWorldName();

        if (player.getWorld().getName().equals(hubWorldName)) {
            giveCompass(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Controlla se il giocatore è nel mondo hub
        String hubWorldName = ConfigurationManager.getHubWorldName();
        if (!player.getWorld().getName().equals(hubWorldName)) {
            return;
        }

        // Controlla se sta cliccando con la bussola (click destro o sinistro)
        if (item != null && item.getType() == Material.COMPASS &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK ||
                        event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {

            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() &&
                    meta.getDisplayName().equals(ChatColor.GOLD + "Selettore Mondi")) {

                event.setCancelled(true);
                new WorldSelectorGUI(plugin).openGUI(player);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Controlla se il giocatore si è mosso su un nuovo blocco
        if (to == null || (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        // Controlla se il blocco sotto i piedi è oro
        Location blockBelow = to.clone().subtract(0, 1, 0);
        if (blockBelow.getBlock().getType() == Material.GOLD_BLOCK) {
            String occupiedWorld = plugin.getWorldManager().getOccupiedWorldByPlayer(player.getUniqueId());
            if (occupiedWorld != null) {
                // Verifica se è il giusto blocco d'oro per questo mondo
                Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();
                ConfigurationManager.WorldInfo worldInfo = worlds.get(occupiedWorld);

                if (worldInfo != null) {
                    double exitX = worldInfo.getExitX();
                    double exitY = worldInfo.getExitY();
                    double exitZ = worldInfo.getExitZ();

                    // Controlla se è vicino alle coordinate d'uscita (tolleranza di 1 blocco)
                    if (Math.abs(blockBelow.getX() - exitX) <= 1 &&
                            Math.abs(blockBelow.getY() - exitY) <= 1 &&
                            Math.abs(blockBelow.getZ() - exitZ) <= 1) {

                        // Fine del raid
                        plugin.getRaidStatsManager().endRaid(player, occupiedWorld);
                        plugin.getWorldManager().freeWorld(occupiedWorld);

                        // Teletrasporta il giocatore al mondo hub
                        String hubWorldName = ConfigurationManager.getHubWorldName();
                        World hubWorld = plugin.getServer().getWorld(hubWorldName);
                        if (hubWorld != null) {
                            Location hubSpawn = hubWorld.getSpawnLocation();
                            player.teleport(hubSpawn);
                            player.sendMessage(ChatColor.GREEN + "Hai completato il raid! Sei tornato al mondo hub!");
                            giveCompass(player);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String occupiedWorld = plugin.getWorldManager().getOccupiedWorldByPlayer(player.getUniqueId());
        if (occupiedWorld != null) {
            // Segna il raid come interrotto
            plugin.getRaidStatsManager().endRaid(player, occupiedWorld);
            plugin.getWorldManager().freeWorld(occupiedWorld);
        }
    }

    private void giveCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Selettore Mondi");
            compass.setItemMeta(meta);
        }

        // Rimuovi eventuali bussole esistenti e aggiungine una nuova
        player.getInventory().remove(Material.COMPASS);
        player.getInventory().addItem(compass);
    }
}