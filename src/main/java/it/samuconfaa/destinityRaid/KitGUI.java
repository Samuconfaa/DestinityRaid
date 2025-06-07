package it.samuconfaa.destinityRaid;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KitGUI implements Listener {
    private final DestinityRaid plugin;
    private final Map<UUID, KitEditSession> editSessions = new HashMap<>();

    public KitGUI(DestinityRaid plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Apre il menu principale dei kit con layout elegante
     */
    public void openKitMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, ChatColor.DARK_PURPLE + "✦ " + ChatColor.GOLD + "Kit Manager" + ChatColor.DARK_PURPLE + " ✦");

        // Riempi i bordi con vetro decorativo
        fillBorders(gui);

        // Informazioni sul kit attuale - Centro superiore
        ItemStack kitInfo = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta kitInfoMeta = kitInfo.getItemMeta();
        kitInfoMeta.setDisplayName(ChatColor.GOLD + "⚡ " + ChatColor.BOLD + "Stato Kit");

        List<String> kitInfoLore = new ArrayList<>();
        KitManager.KitInfo kitData = plugin.getKitManager().getPlayerKitInfo(player);

        if (kitData != null) {
            kitInfoLore.add(ChatColor.GREEN + "✓ Kit salvato e pronto");
            kitInfoLore.add("");
            kitInfoLore.add(ChatColor.GRAY + "Ultimo aggiornamento:");
            kitInfoLore.add(ChatColor.WHITE + "  " + kitData.getFormattedLastUpdate());
            kitInfoLore.add("");
            kitInfoLore.add(ChatColor.AQUA + "Il tuo equipaggiamento personalizzato");
            kitInfoLore.add(ChatColor.AQUA + "è stato salvato con successo!");
        } else {
            kitInfoLore.add(ChatColor.RED + "✗ Nessun kit salvato");
            kitInfoLore.add("");
            kitInfoLore.add(ChatColor.GRAY + "Crea il tuo primo kit personalizzato");
            kitInfoLore.add(ChatColor.GRAY + "per iniziare l'avventura!");
            kitInfoLore.add("");
            kitInfoLore.add(ChatColor.YELLOW + "⚡ Personalizza il tuo equipaggiamento!");
        }

        kitInfoMeta.setLore(kitInfoLore);
        kitInfo.setItemMeta(kitInfoMeta);
        gui.setItem(13, kitInfo); // Centro superiore

        // Bottone per modificare il kit - Sinistra
        ItemStack editKit = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta editKitMeta = editKit.getItemMeta();
        editKitMeta.setDisplayName(ChatColor.YELLOW + "⚒ " + ChatColor.BOLD + "Modifica Kit");

        List<String> editKitLore = new ArrayList<>();
        editKitLore.add(ChatColor.GRAY + "Apri l'editor per personalizzare");
        editKitLore.add(ChatColor.GRAY + "il tuo equipaggiamento");
        editKitLore.add("");
        editKitLore.add(ChatColor.WHITE + "Puoi personalizzare:");
        editKitLore.add(ChatColor.BLUE + "  ⚔ Armatura completa");
        editKitLore.add(ChatColor.GREEN + "  🎒 Inventario principale");
        editKitLore.add(ChatColor.GOLD + "  ⭐ Barra degli oggetti");
        editKitLore.add("");

        if (plugin.getKitManager().hasPlayerKit(player)) {
            editKitLore.add(ChatColor.LIGHT_PURPLE + "Il tuo kit salvato verrà caricato");
            editKitLore.add(ChatColor.LIGHT_PURPLE + "nell'editor per le modifiche");
        } else {
            editKitLore.add(ChatColor.AQUA + "Il tuo inventario attuale verrà");
            editKitLore.add(ChatColor.AQUA + "copiato come base di partenza");
        }

        editKitMeta.setLore(editKitLore);
        editKit.setItemMeta(editKitMeta);
        gui.setItem(20, editKit); // Sinistra

        // Bottone per eliminare il kit - Destra
        ItemStack deleteKit = new ItemStack(plugin.getKitManager().hasPlayerKit(player) ?
                Material.TNT : Material.BARRIER);
        ItemMeta deleteKitMeta = deleteKit.getItemMeta();
        deleteKitMeta.setDisplayName(ChatColor.RED + "🗑 " + ChatColor.BOLD + "Elimina Kit");

        List<String> deleteKitLore = new ArrayList<>();
        if (plugin.getKitManager().hasPlayerKit(player)) {
            deleteKitLore.add(ChatColor.GRAY + "Rimuovi definitivamente");
            deleteKitLore.add(ChatColor.GRAY + "il tuo kit salvato");
            deleteKitLore.add("");
            deleteKitLore.add(ChatColor.RED + "⚠ " + ChatColor.BOLD + "ATTENZIONE" + ChatColor.RED + " ⚠");
            deleteKitLore.add(ChatColor.RED + "Questa azione è irreversibile!");
            deleteKitLore.add("");
            deleteKitLore.add(ChatColor.DARK_RED + "Tutto il tuo equipaggiamento");
            deleteKitLore.add(ChatColor.DARK_RED + "personalizzato andrà perso");
        } else {
            deleteKitLore.add(ChatColor.GRAY + "Nessun kit da eliminare");
            deleteKitLore.add("");
            deleteKitLore.add(ChatColor.DARK_GRAY + "Crea prima un kit per poterlo");
            deleteKitLore.add(ChatColor.DARK_GRAY + "eliminare in seguito");
        }

        deleteKitMeta.setLore(deleteKitLore);
        deleteKit.setItemMeta(deleteKitMeta);
        gui.setItem(24, deleteKit); // Destra

        // Bottone per chiudere - Basso centro
        ItemStack close = new ItemStack(Material.RED_STAINED_GLASS);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.GRAY + "✖ " + ChatColor.BOLD + "Chiudi");
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatColor.DARK_GRAY + "Torna al gioco");
        closeMeta.setLore(closeLore);
        close.setItemMeta(closeMeta);
        gui.setItem(31, close); // Basso centro

        player.openInventory(gui);
    }

    /**
     * Riempie i bordi della GUI con vetro decorativo
     */
    private void fillBorders(Inventory gui) {
        // Vetro principale per i bordi
        ItemStack borderItem = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderItem.setItemMeta(borderMeta);

        // Vetro decorativo per gli angoli
        ItemStack accentItem = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        ItemMeta accentMeta = accentItem.getItemMeta();
        accentMeta.setDisplayName(" ");
        accentItem.setItemMeta(accentMeta);

        // Riempi tutti i bordi
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, borderItem); // Bordo superiore
            gui.setItem(i + 27, borderItem); // Bordo inferiore
        }

        // Bordi laterali
        gui.setItem(9, borderItem);
        gui.setItem(17, borderItem);
        gui.setItem(18, borderItem);
        gui.setItem(26, borderItem);

        // Angoli decorativi
        gui.setItem(0, accentItem);
        gui.setItem(8, accentItem);
        gui.setItem(27, accentItem);
        gui.setItem(35, accentItem);

        // Elementi decorativi aggiuntivi
        ItemStack decorItem = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decorItem.getItemMeta();
        decorMeta.setDisplayName(" ");
        decorItem.setItemMeta(decorMeta);

        gui.setItem(4, decorItem); // Centro superiore
        gui.setItem(22, decorItem); // Centro
    }

    /**
     * Apre l'editor del kit
     */
    public void openKitEditor(Player player) {
        // Crea una sessione di editing con backup dell'inventario attuale
        KitEditSession session = new KitEditSession(player);
        editSessions.put(player.getUniqueId(), session);

        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "⚒ Editor Kit - " + ChatColor.GOLD + player.getName());

        // Se il giocatore ha un kit salvato, caricalo nell'editor
        // Altrimenti usa l'inventario attuale
        if (plugin.getKitManager().hasPlayerKit(player)) {
            loadSavedKitIntoEditor(player, gui);
            player.sendMessage(ChatColor.GREEN + "✓ Il tuo kit salvato è stato caricato nell'editor!");
        } else {
            copyPlayerInventoryToGUI(player, gui);
            player.sendMessage(ChatColor.GOLD + "✓ Il tuo inventario attuale è stato copiato nell'editor.");
        }

        // Bottoni di controllo
        setupControlButtons(gui);

        player.openInventory(gui);
        player.sendMessage(ChatColor.YELLOW + "⚒ Editor kit aperto! Modifica il tuo equipaggiamento e clicca " +
                ChatColor.GREEN + "'Salva'" + ChatColor.YELLOW + " quando hai finito.");
    }

    /**
     * Copia l'inventario del giocatore nella GUI - VERSIONE CORRETTA
     */
    private void copyPlayerInventoryToGUI(Player player, Inventory gui) {
        // Prima pulisci la GUI (esclusi i bottoni di controllo)
        for (int i = 0; i < 50; i++) {
            gui.setItem(i, null);
        }

        // Armatura (slot 45-48)
        gui.setItem(45, cloneItem(player.getInventory().getBoots()));
        gui.setItem(46, cloneItem(player.getInventory().getLeggings()));
        gui.setItem(47, cloneItem(player.getInventory().getChestplate()));
        gui.setItem(48, cloneItem(player.getInventory().getHelmet()));

        // Inventario principale (slot 9-44) - Copia dall'inventario del giocatore
        // Gli slot 9-35 dell'inventario del giocatore vanno negli slot 18-44 della GUI
        for (int i = 9; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            gui.setItem(i + 9, cloneItem(item)); // 9->18, 10->19, ..., 35->44
        }

        // Hotbar (slot 0-8) - copiato nei primi 9 slot della GUI
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            gui.setItem(i, cloneItem(item));
        }
    }

    /**
     * Clona un ItemStack in modo sicuro
     */
    private ItemStack cloneItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        return item.clone();
    }

    /**
     * Carica il kit salvato nell'editor - VERSIONE CORRETTA
     */
    private void loadSavedKitIntoEditor(Player player, Inventory gui) {
        // Salva l'inventario attuale del giocatore
        ItemStack[] originalContents = player.getInventory().getContents().clone();
        ItemStack[] originalArmor = player.getInventory().getArmorContents().clone();

        // Pulisci temporaneamente l'inventario del giocatore
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Carica il kit nell'inventario del giocatore temporaneamente
        plugin.getKitManager().loadPlayerKit(player);

        // Ora copia dall'inventario del giocatore alla GUI
        copyPlayerInventoryToGUI(player, gui);

        // Ripristina immediatamente l'inventario originale del giocatore
        player.getInventory().setContents(originalContents);
        player.getInventory().setArmorContents(originalArmor);
    }

    /**
     * Configura i bottoni di controllo con design migliorato
     */
    private void setupControlButtons(Inventory gui) {
        // Bottone reset
        ItemStack resetButton = new ItemStack(Material.ORANGE_CONCRETE);
        ItemMeta resetMeta = resetButton.getItemMeta();
        resetMeta.setDisplayName(ChatColor.GOLD + "🔄 " + ChatColor.BOLD + "Reset");
        List<String> resetLore = new ArrayList<>();
        resetLore.add(ChatColor.GRAY + "Svuota completamente l'inventario");
        resetLore.add(ChatColor.YELLOW + "Rimuove tutti gli oggetti dall'editor");
        resetLore.add("");
        resetLore.add(ChatColor.RED + "⚠ Non salva automaticamente");
        resetMeta.setLore(resetLore);
        resetButton.setItemMeta(resetMeta);
        gui.setItem(50, resetButton);

        // Separatore decorativo
        ItemStack separator = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta separatorMeta = separator.getItemMeta();
        separatorMeta.setDisplayName(" ");
        separator.setItemMeta(separatorMeta);
        gui.setItem(51, separator);

        // Bottone annulla
        ItemStack cancelButton = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "✖ " + ChatColor.BOLD + "Annulla");
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ChatColor.GRAY + "Esci senza salvare le modifiche");
        cancelLore.add("");
        cancelLore.add(ChatColor.RED + "Tutte le modifiche andranno perse!");
        cancelLore.add(ChatColor.YELLOW + "Il tuo inventario verrà ripristinato");
        cancelMeta.setLore(cancelLore);
        cancelButton.setItemMeta(cancelMeta);
        gui.setItem(52, cancelButton);

        // Bottone salva
        ItemStack saveButton = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta saveMeta = saveButton.getItemMeta();
        saveMeta.setDisplayName(ChatColor.GREEN + "✓ " + ChatColor.BOLD + "Salva Kit");
        List<String> saveLore = new ArrayList<>();
        saveLore.add(ChatColor.GRAY + "Salva l'equipaggiamento personalizzato");
        saveLore.add("");
        saveLore.add(ChatColor.GREEN + "Sostituisce il kit precedente");
        saveLore.add(ChatColor.AQUA + "Il tuo inventario attuale rimane invariato");
        saveMeta.setLore(saveLore);
        saveButton.setItemMeta(saveMeta);
        gui.setItem(53, saveButton);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Menu principale dei kit
        if (title.equals(ChatColor.DARK_PURPLE + "✦ " + ChatColor.GOLD + "Kit Manager" + ChatColor.DARK_PURPLE + " ✦")) {
            event.setCancelled(true);
            handleKitMenuClick(player, event.getSlot());
            return;
        }

        // Editor del kit
        if (title.startsWith(ChatColor.DARK_GREEN + "⚒ Editor Kit")) {
            handleKitEditorClick(player, event);
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        // Se chiude l'editor senza salvare
        if (title.startsWith(ChatColor.DARK_GREEN + "⚒ Editor Kit")) {
            KitEditSession session = editSessions.remove(player.getUniqueId());
            if (session != null && !session.wasSaved()) {
                // Ripristina l'inventario originale
                session.restorePlayerInventory();
                player.sendMessage(ChatColor.YELLOW + "⚒ Editor kit chiuso. Inventario ripristinato.");
            }
        }
    }

    /**
     * Gestisce i click nel menu principale
     */
    private void handleKitMenuClick(Player player, int slot) {
        switch (slot) {
            case 20: // Modifica Kit
                player.closeInventory();
                openKitEditor(player);
                break;

            case 24: // Elimina Kit
                if (plugin.getKitManager().hasPlayerKit(player)) {
                    plugin.getKitManager().deletePlayerKit(player);
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "✓ Kit eliminato con successo!");
                    // Riapri il menu per aggiornare le informazioni
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openKitMenu(player), 2L);
                } else {
                    player.sendMessage(ChatColor.RED + "✗ Non hai un kit da eliminare!");
                }
                break;

            case 31: // Chiudi
                player.closeInventory();
                player.sendMessage(ChatColor.GRAY + "Menu chiuso.");
                break;
        }
    }

    /**
     * Gestisce i click nell'editor del kit
     */
    private void handleKitEditorClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();

        // Bottoni di controllo
        if (slot >= 50) {
            event.setCancelled(true);

            switch (slot) {
                case 50: // Reset
                    resetKitEditor(event.getInventory());
                    player.sendMessage(ChatColor.GOLD + "🔄 Inventario dell'editor svuotato.");
                    break;

                case 52: // Annulla
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "✖ Modifiche annullate, inventario ripristinato.");
                    break;

                case 53: // Salva
                    saveKitFromGUI(player, event.getInventory());
                    break;
            }
        }
        // Altrimenti permetti la modifica normale dell'inventario
    }

    /**
     * Salva il kit dalla GUI - VERSIONE CORRETTA
     */
    private void saveKitFromGUI(Player player, Inventory gui) {
        // Salva l'inventario attuale del giocatore
        ItemStack[] originalContents = player.getInventory().getContents().clone();
        ItemStack[] originalArmor = player.getInventory().getArmorContents().clone();

        // Pulisci l'inventario del giocatore
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Armatura (dalla GUI all'inventario del giocatore)
        player.getInventory().setHelmet(cloneItem(gui.getItem(48)));
        player.getInventory().setChestplate(cloneItem(gui.getItem(47)));
        player.getInventory().setLeggings(cloneItem(gui.getItem(46)));
        player.getInventory().setBoots(cloneItem(gui.getItem(45)));

        // Hotbar (slot 0-8 della GUI vanno negli slot 0-8 dell'inventario)
        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, cloneItem(gui.getItem(i)));
        }

        // Inventario principale (slot 18-44 della GUI vanno negli slot 9-35 dell'inventario)
        for (int i = 18; i < 45; i++) {
            int targetSlot = i - 9; // 18->9, 19->10, ..., 44->35
            if (targetSlot >= 9 && targetSlot < 36) {
                player.getInventory().setItem(targetSlot, cloneItem(gui.getItem(i)));
            }
        }

        // Salva il kit
        plugin.getKitManager().savePlayerKit(player);

        // Ripristina l'inventario originale del giocatore
        player.getInventory().setContents(originalContents);
        player.getInventory().setArmorContents(originalArmor);

        // Marca la sessione come salvata
        KitEditSession session = editSessions.get(player.getUniqueId());
        if (session != null) {
            session.setSaved(true);
        }

        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "✓ Kit salvato con successo!");
        player.sendMessage(ChatColor.AQUA + "Il tuo equipaggiamento personalizzato è ora pronto!");
    }

    /**
     * Svuota l'editor del kit
     */
    private void resetKitEditor(Inventory gui) {
        // Svuota tutto tranne i bottoni di controllo
        for (int i = 0; i < 50; i++) {
            gui.setItem(i, null);
        }
    }

    /**
     * Classe per gestire le sessioni di editing
     */
    private static class KitEditSession {
        private final Player player;
        private final ItemStack[] originalInventory;
        private final ItemStack[] originalArmor;
        private boolean saved = false;

        public KitEditSession(Player player) {
            this.player = player;
            // Salva backup dell'inventario originale
            this.originalInventory = player.getInventory().getContents().clone();
            this.originalArmor = player.getInventory().getArmorContents().clone();
        }

        public boolean wasSaved() { return saved; }
        public void setSaved(boolean saved) { this.saved = saved; }
        public Player getPlayer() { return player; }

        public void restorePlayerInventory() {
            if (player != null && player.isOnline()) {
                player.getInventory().setContents(originalInventory);
                player.getInventory().setArmorContents(originalArmor);
            }
        }
    }
}