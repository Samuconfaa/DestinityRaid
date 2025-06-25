package it.samuconfaa.destinityRaid;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WorldSelectorGUI implements Listener {
    private final DestinityRaid plugin;
    // Set per tenere traccia dei mondi in fase di caricamento backup
    private final Set<String> worldsLoadingBackup = ConcurrentHashMap.newKeySet();

    public WorldSelectorGUI(DestinityRaid plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "✦ " + ChatColor.GOLD + "Seleziona Mondo Raid" + ChatColor.DARK_PURPLE + " ✦");

        // Decorazioni bordo superiore
        createBorder(gui, 0, 8, Material.PURPLE_STAINED_GLASS_PANE);

        // Decorazioni bordo inferiore
        createBorder(gui, 45, 53, Material.PURPLE_STAINED_GLASS_PANE);

        // Decorazioni laterali
        for (int i = 9; i < 45; i += 9) {
            gui.setItem(i, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, " "));
            gui.setItem(i + 8, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, " "));
        }

        // Titolo centrale nella parte superiore
        ItemStack titleItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta titleMeta = titleItem.getItemMeta();
        if (titleMeta != null) {
            titleMeta.setDisplayName(ChatColor.GOLD + "⚔ " + ChatColor.BOLD + "DESTINY RAIDS" + ChatColor.GOLD + " ⚔");
            List<String> titleLore = new ArrayList<>();
            titleLore.add(ChatColor.GRAY + "Scegli il tuo mondo per iniziare");
            titleLore.add(ChatColor.GRAY + "l'avventura epica!");
            titleMeta.setLore(titleLore);
            titleItem.setItemMeta(titleMeta);
        }
        gui.setItem(4, titleItem);

        Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();

        // Posizioni centrali per i mondi (evitando i bordi)
        int[] worldSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int slotIndex = 0;

        for (Map.Entry<String, ConfigurationManager.WorldInfo> entry : worlds.entrySet()) {
            if (slotIndex >= worldSlots.length) break;

            String worldKey = entry.getKey();
            ConfigurationManager.WorldInfo worldInfo = entry.getValue();
            boolean isOccupied = plugin.getWorldManager().isWorldOccupied(worldKey);
            boolean isLoadingBackup = worldsLoadingBackup.contains(worldKey);

            ItemStack item = createWorldItem(worldInfo, isOccupied, isLoadingBackup);
            gui.setItem(worldSlots[slotIndex], item);
            slotIndex++;
        }

        // Bottone di chiusura
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(ChatColor.RED + "✖ Chiudi Menu");
            List<String> closeLore = new ArrayList<>();
            closeLore.add(ChatColor.GRAY + "Clicca per chiudere il menu");
            closeMeta.setLore(closeLore);
            closeItem.setItemMeta(closeMeta);
        }
        gui.setItem(49, closeItem);

        player.openInventory(gui);
    }

    private void createBorder(Inventory gui, int start, int end, Material material) {
        for (int i = start; i <= end; i++) {
            gui.setItem(i, createDecorativeItem(material, " "));
        }
    }

    private ItemStack createDecorativeItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createWorldItem(ConfigurationManager.WorldInfo worldInfo, boolean isOccupied, boolean isLoadingBackup) {
        ItemStack item;
        List<String> lore = new ArrayList<>();

        if (isLoadingBackup) {
            // Mondo in caricamento backup - lana arancione
            item = new ItemStack(Material.ORANGE_WOOL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "⏳ " + worldInfo.getDisplayName() + ChatColor.GOLD + " ⏳");
                lore.add(ChatColor.GRAY + "Mondo: " + ChatColor.WHITE + worldInfo.getWorldName());
                lore.add("");
                lore.add(ChatColor.GOLD + "⏳ CARICAMENTO BACKUP ⏳");
                lore.add(ChatColor.GRAY + "Il sistema sta preparando");
                lore.add(ChatColor.GRAY + "il mondo per il raid...");
                lore.add("");
                lore.add(ChatColor.YELLOW + "🔄 Attendere prego");
                lore.add(ChatColor.DARK_GRAY + "⚠ Temporaneamente non disponibile");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        } else if (isOccupied) {
            // Mondo occupato - lana rossa
            item = new ItemStack(Material.RED_WOOL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "✗ " + worldInfo.getDisplayName() + ChatColor.RED + " ✗");
                lore.add(ChatColor.GRAY + "Mondo: " + ChatColor.WHITE + worldInfo.getWorldName());
                lore.add("");
                lore.add(ChatColor.RED + "❌ OCCUPATO ❌");
                lore.add(ChatColor.GRAY + "Questo mondo è attualmente");
                lore.add(ChatColor.GRAY + "utilizzato da altri giocatori");
                lore.add("");
                lore.add(ChatColor.DARK_RED + "⚠ Non disponibile");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        } else {
            // Mondo disponibile - lana verde
            item = new ItemStack(Material.GREEN_WOOL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + "✓ " + worldInfo.getDisplayName() + ChatColor.GREEN + " ✓");
                lore.add(ChatColor.GRAY + "Mondo: " + ChatColor.WHITE + worldInfo.getWorldName());
                lore.add("");
                lore.add(ChatColor.GREEN + "✅ DISPONIBILE ✅");
                lore.add(ChatColor.GRAY + "Questo mondo è pronto");
                lore.add(ChatColor.GRAY + "per iniziare il raid!");
                lore.add("");
                lore.add(ChatColor.GOLD + "⚔ Clicca per entrare! ⚔");
                lore.add(ChatColor.YELLOW + "➤ Inizia la tua avventura");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "✦ " + ChatColor.GOLD + "Seleziona Mondo Raid" + ChatColor.DARK_PURPLE + " ✦")) {
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

        // Gestione del bottone di chiusura
        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            player.sendMessage(ChatColor.GRAY + "Menu chiuso.");
            return;
        }

        // Ignora click sui bordi decorativi
        if (clickedItem.getType() == Material.PURPLE_STAINED_GLASS_PANE ||
                clickedItem.getType() == Material.NETHER_STAR) {
            return;
        }

        // Se ha cliccato su un mondo in caricamento backup (lana arancione)
        if (clickedItem.getType() == Material.ORANGE_WOOL) {
            player.sendMessage(ChatColor.GOLD + "⏳ Il sistema sta caricando il backup per questo mondo...");
            player.sendMessage(ChatColor.YELLOW + "Attendere che il processo sia completato prima di entrare.");
            return;
        }

        // Se ha cliccato su un mondo occupato (lana rossa)
        if (clickedItem.getType() == Material.RED_WOOL) {
            player.sendMessage(ChatColor.RED + "⚠ Questo mondo è già occupato da altri giocatori!");
            player.sendMessage(ChatColor.YELLOW + "Prova con un altro mondo disponibile.");
            return;
        }

        // Se ha cliccato su un mondo disponibile (lana verde)
        if (clickedItem.getType() == Material.GREEN_WOOL) {
            String selectedWorldKey = findWorldKeyByDisplayName(clickedItem);
            if (selectedWorldKey == null) {
                player.sendMessage(ChatColor.RED + "❌ Errore: Mondo non trovato!");
                return;
            }

            player.closeInventory();

            // Prova ad iniziare il raid
            if (attemptRaidStart(player, selectedWorldKey)) {
                player.sendMessage(ChatColor.GREEN + "✦ Teletrasporto in corso...");
                player.sendMessage(ChatColor.GOLD + "⚔ Preparati per il raid! ⚔");
            }
        }
    }

    private String findWorldKeyByDisplayName(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        // Rimuovi i simboli decorativi per il confronto
        displayName = displayName.replace("✓ ", "").replace(" ✓", "")
                .replace("✗ ", "").replace(" ✗", "")
                .replace("⏳ ", "").replace(" ⏳", "");

        Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();

        for (Map.Entry<String, ConfigurationManager.WorldInfo> entry : worlds.entrySet()) {
            if (entry.getValue().getDisplayName().equals(displayName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean attemptRaidStart(Player player, String worldKey) {
        // PRIMO: Controlla se il mondo è in caricamento backup
        if (worldsLoadingBackup.contains(worldKey)) {
            player.sendMessage(ChatColor.GOLD + "⏳ Il sistema sta ancora caricando il backup per questo mondo!");
            return false;
        }

        // SECONDO: Controlla se il mondo è disponibile
        if (plugin.getWorldManager().isWorldOccupied(worldKey)) {
            player.sendMessage(ChatColor.RED + "⚠ Questo mondo è già occupato!");
            return false;
        }

        // TERZO: Controlli per i party utilizzando il metodo di validazione integrato
        String partyValidationError = plugin.getPartyManager().validatePartyForRaid(player);
        if (partyValidationError != null) {
            player.sendMessage(partyValidationError);
            return false;
        }

        // QUARTO: OCCUPA IMMEDIATAMENTE IL MONDO per evitare conflitti
        plugin.getWorldManager().occupyWorld(worldKey, player);

        // QUINTO: Ottieni i membri del party (se Parties è abilitato) o solo il giocatore
        List<Player> partyMembers = plugin.getPartyManager().getPartyMembers(player);

        // SESTO: Teletrasporta il giocatore/party (gestione asincrona del backup)
        return teleportPlayersToWorld(player, worldKey, partyMembers);
    }

    private boolean teleportPlayersToWorld(Player leader, String worldKey, List<Player> players) {
        Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();
        ConfigurationManager.WorldInfo worldInfo = worlds.get(worldKey);

        if (worldInfo == null) {
            leader.sendMessage(ChatColor.RED + "❌ Errore: Configurazione mondo non trovata!");
            // Libera il mondo in caso di errore
            plugin.getWorldManager().freeWorld(worldKey);
            return false;
        }

        World world = Bukkit.getWorld(worldInfo.getWorldName());
        if (world == null) {
            leader.sendMessage(ChatColor.RED + "❌ Errore: Mondo non trovato sul server!");
            // Libera il mondo in caso di errore
            plugin.getWorldManager().freeWorld(worldKey);
            return false;
        }

        // Imposta il mondo come "in caricamento backup"
        worldsLoadingBackup.add(worldKey);

        // Informa i giocatori che il processo è iniziato
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.YELLOW + "⏳ Inizializzazione del mondo in corso...");
                player.sendMessage(ChatColor.GRAY + "Questo processo potrebbe richiedere alcuni secondi.");
            }
        }

        // Esegui il backup nel thread principale, poi continua
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Crea backup del mondo (nel thread principale)
                    if (!plugin.getWorldBackupManager().createWorldBackup(worldKey)) {
                        worldsLoadingBackup.remove(worldKey);
                        plugin.getWorldManager().freeWorld(worldKey);
                        leader.sendMessage(ChatColor.RED + "❌ Errore durante la creazione del backup!");
                        leader.sendMessage(ChatColor.RED + "Raid annullato per sicurezza.");
                        return;
                    }

                    // Continua con il setup del mondo
                    completeWorldSetup(leader, worldKey, worldInfo, world, players);

                } catch (Exception e) {
                    // Gestione errori
                    worldsLoadingBackup.remove(worldKey);
                    plugin.getWorldManager().freeWorld(worldKey);
                    leader.sendMessage(ChatColor.RED + "❌ Errore durante l'inizializzazione del mondo!");
                    plugin.getLogger().severe("Errore durante il backup del mondo " + worldKey + ": " + e.getMessage());
                }
            }
        }.runTask(plugin);



        return true;
    }

    private void completeWorldSetup(Player leader, String worldKey, ConfigurationManager.WorldInfo worldInfo, World world, List<Player> players) {
        try {
            // Rimuovi il mondo dalla lista di caricamento
            worldsLoadingBackup.remove(worldKey);

            leader.sendMessage(ChatColor.GREEN + "✓ Backup creato con successo!");

            // Elimina tutte le entità nel mondo (eccetto i giocatori)
            leader.sendMessage(ChatColor.YELLOW + "⏳ Eliminando entità nel mondo...");
            killAllEntitiesInWorld(world);
            leader.sendMessage(ChatColor.GREEN + "✓ Entità eliminate con successo!");

            // Esegui comando /attivaspawner tramite console
            leader.sendMessage(ChatColor.YELLOW + "⏳ Attivando spawner...");
            String command = "attivaspawner " + worldInfo.getWorldName();
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            leader.sendMessage(ChatColor.GREEN + "✓ Spawner attivati con successo!");

            Location spawnLocation = new Location(world, worldInfo.getSpawnX(), worldInfo.getSpawnY(), worldInfo.getSpawnZ());

            // Inizia il raid
            plugin.getRaidStatsManager().startRaid(leader, worldKey);

            // Teletrasporta e prepara tutti i giocatori
            for (Player player : players) {
                if (player != null && player.isOnline()) {
                    player.teleport(spawnLocation);
                    player.heal(100);
                    player.setFoodLevel(20);
                    player.setSaturation(20f);


                    // Inizializza il sistema di morti per il raid
                    plugin.getDeathManager().onRaidStart(player);

                    // PRIMA svuota l'inventario, POI equipaggia il kit del giocatore
                    player.getInventory().clear();
                    player.getInventory().setArmorContents(new ItemStack[4]);

                    // Carica il kit personalizzato del giocatore
                    plugin.getKitManager().loadPlayerKit(player);

                    player.sendMessage(ChatColor.GREEN + "✦ Raid iniziato in " + worldInfo.getDisplayName() + "! ✦");
                    player.sendMessage(ChatColor.YELLOW + "⚔ Il tuo kit personalizzato è stato equipaggiato!");
                    player.sendMessage(ChatColor.GOLD + "⚠ Ricorda: hai solo 2 vite, poi diventerai spettatore!");
                    player.sendMessage(ChatColor.AQUA + "➤ Buona fortuna, guerriero!");
                }
            }

        } catch (Exception e) {
            // In caso di errore, libera il mondo
            worldsLoadingBackup.remove(worldKey);
            plugin.getWorldManager().freeWorld(worldKey);
            leader.sendMessage(ChatColor.RED + "❌ Errore durante la configurazione del mondo!");
            plugin.getLogger().severe("Errore durante la configurazione del mondo " + worldKey + ": " + e.getMessage());
        }
    }

    /**
     * Elimina tutte le entità nel mondo specificato, eccetto i giocatori
     * @param world Il mondo in cui eliminare le entità
     */
    private void killAllEntitiesInWorld(World world) {
        List<Entity> entitiesToRemove = new ArrayList<>();

        // Raccogli tutte le entità che non sono giocatori
        for (Entity entity : world.getEntities()) {
            if (entity.getType() != EntityType.PLAYER) {
                entitiesToRemove.add(entity);
            }
        }

        // Rimuovi tutte le entità raccolte
        for (Entity entity : entitiesToRemove) {
            entity.remove();
        }

        // Log per debug
        plugin.getLogger().info("Eliminate " + entitiesToRemove.size() + " entità nel mondo " + world.getName());
    }

    /**
     * Metodo per verificare se un mondo è in caricamento backup (utile per altre classi)
     * @param worldKey La chiave del mondo
     * @return true se il mondo è in caricamento backup
     */
    public boolean isWorldLoadingBackup(String worldKey) {
        return worldsLoadingBackup.contains(worldKey);
    }

    /**
     * Metodo per rimuovere forzatamente un mondo dalla lista di caricamento
     * (utile in caso di crash o problemi)
     * @param worldKey La chiave del mondo
     */
    public void forceRemoveFromLoading(String worldKey) {
        worldsLoadingBackup.remove(worldKey);
    }
}