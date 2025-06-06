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
     * Apre il menu principale dei kit
     */
    public void openKitMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "Gestione Kit");

        // Informazioni sul kit attuale
        ItemStack kitInfo = new ItemStack(Material.BOOK);
        ItemMeta kitInfoMeta = kitInfo.getItemMeta();
        kitInfoMeta.setDisplayName(ChatColor.GOLD + "Informazioni Kit");

        List<String> kitInfoLore = new ArrayList<>();
        KitManager.KitInfo kitData = plugin.getKitManager().getPlayerKitInfo(player);

        if (kitData != null) {
            kitInfoLore.add(ChatColor.GREEN + "✓ Hai un kit salvato");
            kitInfoLore.add(ChatColor.GRAY + "Ultimo aggiornamento: " + kitData.getFormattedLastUpdate());
        } else {
            kitInfoLore.add(ChatColor.RED + "✗ Nessun kit salvato");
            kitInfoLore.add(ChatColor.GRAY + "Crea il tuo primo kit!");
        }

        kitInfoMeta.setLore(kitInfoLore);
        kitInfo.setItemMeta(kitInfoMeta);
        gui.setItem(4, kitInfo);

        // Bottone per modificare il kit
        ItemStack editKit = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta editKitMeta = editKit.getItemMeta();
        editKitMeta.setDisplayName(ChatColor.YELLOW + "Modifica Kit");

        List<String> editKitLore = new ArrayList<>();
        editKitLore.add(ChatColor.GRAY + "Clicca per modificare il tuo kit");
        editKitLore.add(ChatColor.GRAY + "Potrai personalizzare:");
        editKitLore.add(ChatColor.GRAY + "• Armatura");
        editKitLore.add(ChatColor.GRAY + "• Oggetti nell'inventario");
        editKitLore.add(ChatColor.GRAY + "• Hotbar");

        editKitMeta.setLore(editKitLore);
        editKit.setItemMeta(editKitMeta);
        gui.setItem(11, editKit);

        // Bottone per caricare il kit
        ItemStack loadKit = new ItemStack(Material.CHEST);
        ItemMeta loadKitMeta = loadKit.getItemMeta();
        loadKitMeta.setDisplayName(ChatColor.GREEN + "Carica Kit");

        List<String> loadKitLore = new ArrayList<>();
        if (plugin.getKitManager().hasPlayerKit(player)) {
            loadKitLore.add(ChatColor.GRAY + "Carica il tuo kit salvato");
            loadKitLore.add(ChatColor.GRAY + "nell'inventario attuale");
        } else {
            loadKitLore.add(ChatColor.RED + "Nessun kit da caricare");
        }

        loadKitMeta.setLore(loadKitLore);
        loadKit.setItemMeta(loadKitMeta);
        gui.setItem(13, loadKit);

        // Bottone per eliminare il kit
        ItemStack deleteKit = new ItemStack(Material.BARRIER);
        ItemMeta deleteKitMeta = deleteKit.getItemMeta();
        deleteKitMeta.setDisplayName(ChatColor.RED + "Elimina Kit");

        List<String> deleteKitLore = new ArrayList<>();
        deleteKitLore.add(ChatColor.GRAY + "Elimina il tuo kit salvato");
        deleteKitLore.add(ChatColor.RED + "ATTENZIONE: Questa azione è irreversibile!");

        deleteKitMeta.setLore(deleteKitLore);
        deleteKit.setItemMeta(deleteKitMeta);
        gui.setItem(15, deleteKit);

        // Bottone per chiudere
        ItemStack close = new ItemStack(Material.IRON_DOOR);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.GRAY + "Chiudi");
        close.setItemMeta(closeMeta);
        gui.setItem(22, close);

        player.openInventory(gui);
    }

    /**
     * Apre l'editor del kit
     */
    public void openKitEditor(Player player) {
        // Crea una sessione di editing
        KitEditSession session = new KitEditSession(player);
        editSessions.put(player.getUniqueId(), session);

        // Carica il kit attuale se esiste
        if (plugin.getKitManager().hasPlayerKit(player)) {
            plugin.getKitManager().loadPlayerKit(player);
        }

        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Editor Kit - " + player.getName());

        // Copia l'inventario del giocatore nella GUI
        copyPlayerInventoryToGUI(player, gui);

        // Bottoni di controllo
        setupControlButtons(gui);

        player.openInventory(gui);
        player.sendMessage(ChatColor.YELLOW + "Editor kit aperto! Modifica il tuo equipaggiamento e clicca 'Salva' quando hai finito.");
    }

    /**
     * Copia l'inventario del giocatore nella GUI
     */
    private void copyPlayerInventoryToGUI(Player player, Inventory gui) {
        // Armatura (slot 45-48)
        gui.setItem(45, player.getInventory().getBoots());
        gui.setItem(46, player.getInventory().getLeggings());
        gui.setItem(47, player.getInventory().getChestplate());
        gui.setItem(48, player.getInventory().getHelmet());

        // Inventario principale (slot 9-44)
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            gui.setItem(i + 9, item);
        }

        // Hotbar (slot 0-8) - copiato nei primi 9 slot della GUI
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            gui.setItem(i, item);
        }
    }

    /**
     * Configura i bottoni di controllo
     */
    private void setupControlButtons(Inventory gui) {
        // Bottone salva
        ItemStack saveButton = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta saveMeta = saveButton.getItemMeta();
        saveMeta.setDisplayName(ChatColor.GREEN + "Salva Kit");
        List<String> saveLore = new ArrayList<>();
        saveLore.add(ChatColor.GRAY + "Clicca per salvare il kit");
        saveMeta.setLore(saveLore);
        saveButton.setItemMeta(saveMeta);
        gui.setItem(53, saveButton);

        // Bottone annulla
        ItemStack cancelButton = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Annulla");
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ChatColor.GRAY + "Esci senza salvare");
        cancelMeta.setLore(cancelLore);
        cancelButton.setItemMeta(cancelMeta);
        gui.setItem(52, cancelButton);

        // Bottone reset
        ItemStack resetButton = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta resetMeta = resetButton.getItemMeta();
        resetMeta.setDisplayName(ChatColor.YELLOW + "Reset");
        List<String> resetLore = new ArrayList<>();
        resetLore.add(ChatColor.GRAY + "Svuota tutto l'inventario");
        resetMeta.setLore(resetLore);
        resetButton.setItemMeta(resetMeta);
        gui.setItem(51, resetButton);

        // Separatori
        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta separatorMeta = separator.getItemMeta();
        separatorMeta.setDisplayName(" ");
        separator.setItemMeta(separatorMeta);
        gui.setItem(49, separator);
        gui.setItem(50, separator);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Menu principale dei kit
        if (title.equals(ChatColor.DARK_BLUE + "Gestione Kit")) {
            event.setCancelled(true);
            handleKitMenuClick(player, event.getSlot());
            return;
        }

        // Editor del kit
        if (title.startsWith(ChatColor.DARK_GREEN + "Editor Kit")) {
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
        if (title.startsWith(ChatColor.DARK_GREEN + "Editor Kit")) {
            KitEditSession session = editSessions.remove(player.getUniqueId());
            if (session != null && !session.wasSaved()) {
                player.sendMessage(ChatColor.YELLOW + "Editor kit chiuso senza salvare le modifiche.");
            }
        }
    }

    /**
     * Gestisce i click nel menu principale
     */
    private void handleKitMenuClick(Player player, int slot) {
        switch (slot) {
            case 11: // Modifica Kit
                player.closeInventory();
                openKitEditor(player);
                break;

            case 13: // Carica Kit
                if (plugin.getKitManager().hasPlayerKit(player)) {
                    plugin.getKitManager().loadPlayerKit(player);
                    player.closeInventory();
                } else {
                    player.sendMessage(ChatColor.RED + "Non hai un kit salvato da caricare!");
                }
                break;

            case 15: // Elimina Kit
                if (plugin.getKitManager().hasPlayerKit(player)) {
                    plugin.getKitManager().deletePlayerKit(player);
                    player.closeInventory();
                    // Riapri il menu per aggiornare le informazioni
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openKitMenu(player), 1L);
                } else {
                    player.sendMessage(ChatColor.RED + "Non hai un kit da eliminare!");
                }
                break;

            case 22: // Chiudi
                player.closeInventory();
                break;
        }
    }

    /**
     * Gestisce i click nell'editor del kit
     */
    private void handleKitEditorClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();

        // Bottoni di controllo
        if (slot >= 49) {
            event.setCancelled(true);

            switch (slot) {
                case 53: // Salva
                    saveKitFromGUI(player, event.getInventory());
                    break;

                case 52: // Annulla
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Modifiche annullate.");
                    break;

                case 51: // Reset
                    resetKitEditor(event.getInventory());
                    player.sendMessage(ChatColor.YELLOW + "Inventario svuotato.");
                    break;
            }
        }
        // Altrimenti permetti la modifica normale dell'inventario
    }

    /**
     * Salva il kit dalla GUI
     */
    private void saveKitFromGUI(Player player, Inventory gui) {
        // Copia il contenuto della GUI nell'inventario del giocatore
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Armatura
        player.getInventory().setBoots(gui.getItem(45));
        player.getInventory().setLeggings(gui.getItem(46));
        player.getInventory().setChestplate(gui.getItem(47));
        player.getInventory().setHelmet(gui.getItem(48));

        // Inventario principale
        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, gui.getItem(i + 9));
        }

        // Salva il kit
        plugin.getKitManager().savePlayerKit(player);

        // Marca la sessione come salvata
        KitEditSession session = editSessions.get(player.getUniqueId());
        if (session != null) {
            session.setSaved(true);
        }

        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Kit salvato con successo!");
    }

    /**
     * Svuota l'editor del kit
     */
    private void resetKitEditor(Inventory gui) {
        // Svuota tutto tranne i bottoni di controllo
        for (int i = 0; i < 49; i++) {
            gui.setItem(i, null);
        }
    }

    /**
     * Classe per gestire le sessioni di editing
     */
    private static class KitEditSession {
        private final Player player;
        private boolean saved = false;

        public KitEditSession(Player player) {
            this.player = player;
        }

        public boolean wasSaved() { return saved; }
        public void setSaved(boolean saved) { this.saved = saved; }
        public Player getPlayer() { return player; }
    }
}