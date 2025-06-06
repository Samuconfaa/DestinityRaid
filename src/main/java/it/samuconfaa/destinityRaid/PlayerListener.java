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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
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
            player.getInventory().clear();
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

        // Controlla se sta cliccando con la stella del nether (click destro o sinistro)
        if (item != null && item.getType() == Material.NETHER_STAR &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK ||
                        event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {

            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() &&
                    meta.getDisplayName().equals(ChatColor.GOLD + "Selettore Mondi")) {

                // IMPORTANTE: Cancella l'evento per evitare il comportamento predefinito
                event.setCancelled(true);

                // Previeni qualsiasi altro comportamento
                event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
                event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);

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
            // NUOVO: Controlla se il giocatore è in un mondo che è occupato dal suo party
            String occupiedWorld = findOccupiedWorldByPartyMember(player);

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

                        // Ottieni il leader del party (chi ha occupato il mondo)
                        Player partyLeader = getPartyLeaderForWorld(occupiedWorld);
                        if (partyLeader != null) {
                            // Fine del raid - gestisci tutto il party
                            plugin.getRaidStatsManager().endRaid(partyLeader, occupiedWorld);
                            plugin.getWorldManager().freeWorld(occupiedWorld);

                            // Esegui i comandi della console se configurati
                            executeConsoleCommands(partyLeader, occupiedWorld);

                            // Teletrasporta tutti i membri del party al mondo hub
                            teleportPartyToHub(partyLeader);
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

        // Pulisci i dati del DeathManager
        plugin.getDeathManager().onPlayerQuit(player);

    }

    // Previeni il drop della Nether Star
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        // Controlla se è nel mondo hub
        String hubWorldName = ConfigurationManager.getHubWorldName();
        if (!player.getWorld().getName().equals(hubWorldName)) {
            return;
        }

        // Controlla se sta droppando la Nether Star del selettore
        if (droppedItem.getType() == Material.NETHER_STAR &&
                droppedItem.hasItemMeta() &&
                droppedItem.getItemMeta().hasDisplayName() &&
                droppedItem.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Selettore Mondi")) {

            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Non puoi droppare il selettore mondi!");
        }
    }

    // Previeni lo spostamento della Nether Star dall'inventario
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Controlla se è nel mondo hub
        String hubWorldName = ConfigurationManager.getHubWorldName();
        if (!player.getWorld().getName().equals(hubWorldName)) {
            return;
        }

        // Se sta cliccando su una GUI del plugin, ignora questo controllo
        if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Seleziona Mondo")) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Controlla se sta tentando di spostare la Nether Star dal slot 5
        if (event.getSlot() == 4 && clickedItem != null &&
                clickedItem.getType() == Material.NETHER_STAR &&
                clickedItem.hasItemMeta() &&
                clickedItem.getItemMeta().hasDisplayName() &&
                clickedItem.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Selettore Mondi")) {

            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Non puoi spostare il selettore mondi!");
            return;
        }

        // Controlla se sta tentando di mettere qualcosa nel slot 5 quando c'è già la Nether Star
        if (event.getSlot() == 4 && cursorItem != null && !cursorItem.getType().isAir()) {
            ItemStack slotItem = player.getInventory().getItem(4);
            if (slotItem != null && slotItem.getType() == Material.NETHER_STAR &&
                    slotItem.hasItemMeta() &&
                    slotItem.getItemMeta().hasDisplayName() &&
                    slotItem.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Selettore Mondi")) {

                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Non puoi sostituire il selettore mondi!");
            }
        }
    }

    // NUOVO METODO: Trova il mondo occupato da qualsiasi membro del party del giocatore
    private String findOccupiedWorldByPartyMember(Player player) {
        // Prima controlla se il giocatore stesso ha occupato un mondo
        String occupiedWorld = plugin.getWorldManager().getOccupiedWorldByPlayer(player.getUniqueId());
        if (occupiedWorld != null) {
            return occupiedWorld;
        }

        // Se Parties non è abilitato, non può essere membro di nessun party
        if (!plugin.getPartyManager().isPartiesEnabled()) {
            return null;
        }

        // Ottieni tutti i membri del party del giocatore
        java.util.List<Player> partyMembers = plugin.getPartyManager().getPartyMembers(player);

        // Controlla se qualche membro del party ha occupato un mondo
        for (Player member : partyMembers) {
            if (member != null && !member.equals(player)) {
                occupiedWorld = plugin.getWorldManager().getOccupiedWorldByPlayer(member.getUniqueId());
                if (occupiedWorld != null) {
                    // Verifica che il giocatore sia nello stesso mondo del mondo occupato
                    Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();
                    ConfigurationManager.WorldInfo worldInfo = worlds.get(occupiedWorld);
                    if (worldInfo != null && player.getWorld().getName().equals(worldInfo.getWorldName())) {
                        return occupiedWorld;
                    }
                }
            }
        }

        return null;
    }

    // NUOVO METODO: Ottieni il leader del party che ha occupato il mondo
    private Player getPartyLeaderForWorld(String worldKey) {
        java.util.UUID occupantUUID = plugin.getWorldManager().getWorldOccupant(worldKey);
        if (occupantUUID != null) {
            return org.bukkit.Bukkit.getPlayer(occupantUUID);
        }
        return null;
    }

    private void giveCompass(Player player) {
        ItemStack compass = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Selettore Mondi");
            // Aggiungi una descrizione per rendere l'item più chiaro
            meta.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "Clicca per aprire il menu",
                    ChatColor.GRAY + "di selezione mondi"
            ));
            compass.setItemMeta(meta);
        }

        // Rimuovi eventuali stelle del nether esistenti e aggiungine una nuova al slot 5
        player.getInventory().remove(Material.NETHER_STAR);
        player.getInventory().setItem(4, compass);
    }

    private void executeConsoleCommands(Player player, String worldKey) {
        // Esegui prima i comandi globali
        java.util.List<String> globalCommands = ConfigurationManager.getGlobalConsoleCommands();
        if (globalCommands != null && !globalCommands.isEmpty()) {
            for (String command : globalCommands) {
                String processedCommand = command
                        .replace("{player}", player.getName())
                        .replace("{world}", worldKey)
                        .replace("{uuid}", player.getUniqueId().toString());

                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processedCommand);
                plugin.getLogger().info("Comando globale eseguito: " + processedCommand);
            }
        }

        // Poi esegui i comandi specifici del mondo
        java.util.List<String> worldCommands = ConfigurationManager.getConsoleCommands(worldKey);
        if (worldCommands != null && !worldCommands.isEmpty()) {
            for (String command : worldCommands) {
                String processedCommand = command
                        .replace("{player}", player.getName())
                        .replace("{world}", worldKey)
                        .replace("{uuid}", player.getUniqueId().toString());

                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processedCommand);
                plugin.getLogger().info("Comando mondo eseguito: " + processedCommand);
            }
        }
    }

    private void teleportPartyToHub(Player completedPlayer) {
        String hubWorldName = ConfigurationManager.getHubWorldName();
        World hubWorld = plugin.getServer().getWorld(hubWorldName);
        if (hubWorld == null) {
            completedPlayer.sendMessage(ChatColor.RED + "Errore: Mondo hub non trovato!");
            return;
        }

        Location hubSpawn = hubWorld.getSpawnLocation();

        // Ottieni tutti i membri del party e teletrasportali
        java.util.List<Player> partyMembers = plugin.getPartyManager().getPartyMembers(completedPlayer);

        for (Player member : partyMembers) {
            member.teleport(hubSpawn);
            member.sendMessage(ChatColor.GREEN + "Raid completato! Siete tornati al mondo hub!");

            // Termina il raid per il membro (ripristina gamemode e pulisce dati)
            plugin.getDeathManager().onRaidEnd(member);

            giveCompass(member);
        }
    }
}