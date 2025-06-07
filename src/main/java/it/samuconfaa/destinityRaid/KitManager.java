package it.samuconfaa.destinityRaid;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {
    private final DestinityRaid plugin;
    private File kitsFile;
    private FileConfiguration kitsConfig;

    public KitManager(DestinityRaid plugin) {
        this.plugin = plugin;
        loadKitsFile();
    }

    private void loadKitsFile() {
        kitsFile = new File(plugin.getDataFolder(), "player_kits.yml");
        if (!kitsFile.exists()) {
            try {
                kitsFile.createNewFile();
                plugin.getLogger().info("File player_kits.yml creato!");
            } catch (IOException e) {
                plugin.getLogger().severe("Impossibile creare il file player_kits.yml!");
            }
        }
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
    }

    /**
     * Salva il kit del giocatore dal suo inventario attuale
     */
    public void savePlayerKit(Player player) {
        String playerUUID = player.getUniqueId().toString();
        String basePath = "players." + playerUUID;

        PlayerInventory inventory = player.getInventory();

        // Salva informazioni base
        kitsConfig.set(basePath + ".name", player.getName());
        kitsConfig.set(basePath + ".last_update", System.currentTimeMillis());

        // Salva armatura
        kitsConfig.set(basePath + ".armor.helmet", itemToString(inventory.getHelmet()));
        kitsConfig.set(basePath + ".armor.chestplate", itemToString(inventory.getChestplate()));
        kitsConfig.set(basePath + ".armor.leggings", itemToString(inventory.getLeggings()));
        kitsConfig.set(basePath + ".armor.boots", itemToString(inventory.getBoots()));

        // Salva contenuto inventario (slot 0-35)
        List<String> inventoryItems = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            inventoryItems.add(itemToString(item));
        }
        kitsConfig.set(basePath + ".inventory", inventoryItems);

        // Salva hotbar separatamente per chiarezza
        List<String> hotbarItems = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            hotbarItems.add(itemToString(item));
        }
        kitsConfig.set(basePath + ".hotbar", hotbarItems);

        saveKitsFile();
        player.sendMessage(ChatColor.GREEN + "Il tuo kit è stato salvato!");
    }

    /**
     * Carica e applica il kit del giocatore
     */
    public void loadPlayerKit(Player player) {
        String playerUUID = player.getUniqueId().toString();
        String basePath = "players." + playerUUID;

        if (!kitsConfig.contains(basePath)) {
            // Se non ha un kit salvato, dagli il kit predefinito dalla config
            giveDefaultKit(player);
            return;
        }

        PlayerInventory inventory = player.getInventory();

        // Pulisci inventario
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);

        // Carica armatura
        inventory.setHelmet(parseItemString(kitsConfig.getString(basePath + ".armor.helmet")));
        inventory.setChestplate(parseItemString(kitsConfig.getString(basePath + ".armor.chestplate")));
        inventory.setLeggings(parseItemString(kitsConfig.getString(basePath + ".armor.leggings")));
        inventory.setBoots(parseItemString(kitsConfig.getString(basePath + ".armor.boots")));

        // Carica inventario
        List<String> inventoryItems = kitsConfig.getStringList(basePath + ".inventory");
        for (int i = 0; i < Math.min(inventoryItems.size(), 36); i++) {
            ItemStack item = parseItemString(inventoryItems.get(i));
            if (item != null) {
                inventory.setItem(i, item);
            }
        }

        player.sendMessage(ChatColor.GREEN + "Kit caricato!");
    }

    /**
     * Controlla se il giocatore ha un kit salvato
     */
    public boolean hasPlayerKit(Player player) {
        String playerUUID = player.getUniqueId().toString();
        return kitsConfig.contains("players." + playerUUID);
    }

    /**
     * Ottiene informazioni sul kit del giocatore
     */
    public KitInfo getPlayerKitInfo(Player player) {
        String playerUUID = player.getUniqueId().toString();
        String basePath = "players." + playerUUID;

        if (!kitsConfig.contains(basePath)) {
            return null;
        }

        String name = kitsConfig.getString(basePath + ".name", player.getName());
        long lastUpdate = kitsConfig.getLong(basePath + ".last_update", 0);

        return new KitInfo(name, lastUpdate);
    }

    /**
     * Elimina il kit del giocatore
     */
    public void deletePlayerKit(Player player) {
        String playerUUID = player.getUniqueId().toString();
        String basePath = "players." + playerUUID;

        kitsConfig.set(basePath, null);
        saveKitsFile();

        player.sendMessage(ChatColor.YELLOW + "Il tuo kit è stato eliminato!");
    }

    /**
     * Applica il kit predefinito al giocatore dalla configurazione
     */
    private void giveDefaultKit(Player player) {
        // Controlla se il kit predefinito è abilitato
        if (!ConfigurationManager.isDefaultKitEnabled()) {
            player.sendMessage(ChatColor.YELLOW + "Nessun kit predefinito disponibile.");
            return;
        }

        PlayerInventory inventory = player.getInventory();

        // Pulisci inventario
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);

        // Applica armatura dal config
        Map<String, String> armor = ConfigurationManager.getDefaultKitArmor();
        if (armor.get("helmet") != null) {
            inventory.setHelmet(parseItemString(armor.get("helmet")));
        }
        if (armor.get("chestplate") != null) {
            inventory.setChestplate(parseItemString(armor.get("chestplate")));
        }
        if (armor.get("leggings") != null) {
            inventory.setLeggings(parseItemString(armor.get("leggings")));
        }
        if (armor.get("boots") != null) {
            inventory.setBoots(parseItemString(armor.get("boots")));
        }

        // Applica oggetti dall'inventario predefinito
        List<String> defaultItems = ConfigurationManager.getDefaultKitItems();
        int slot = 0;
        for (String itemString : defaultItems) {
            if (slot >= 36) break; // Non superare i 36 slot dell'inventario

            ItemStack item = parseItemString(itemString);
            if (item != null) {
                inventory.setItem(slot, item);
                slot++;
            }
        }

        String kitName = ConfigurationManager.getDefaultKitDisplayName();
        player.sendMessage(ChatColor.GOLD + "✓ Ti è stato dato il kit: " + ChatColor.YELLOW + kitName);
        player.sendMessage(ChatColor.AQUA + "Personalizzalo e salvalo usando /kit!");
    }

    /**
     * Applica il kit predefinito e lo salva automaticamente per il giocatore
     * Utilizzato quando un giocatore entra per la prima volta
     */
    public void giveAndSaveDefaultKit(Player player) {
        // Applica il kit predefinito
        giveDefaultKit(player);

        // Salva automaticamente il kit appena applicato
        if (ConfigurationManager.isDefaultKitEnabled()) {
            savePlayerKit(player);

            String kitName = ConfigurationManager.getDefaultKitDisplayName();
            player.sendMessage(ChatColor.GREEN + "✓ Il kit " + ChatColor.YELLOW + kitName +
                    ChatColor.GREEN + " è stato salvato come tuo kit personalizzato!");
            player.sendMessage(ChatColor.GRAY + "Puoi modificarlo in qualsiasi momento con " +
                    ChatColor.WHITE + "/kit" + ChatColor.GRAY + "!");
        }
    }

    /**
     * Converte un ItemStack in stringa per il salvataggio
     */
    private String itemToString(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "AIR:0:0";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(item.getType().name()).append(":");
        sb.append(item.getAmount()).append(":");
        sb.append("0"); // Durability placeholder

        // Aggiungi incantesimi se presenti
        if (!item.getEnchantments().isEmpty()) {
            sb.append(":");
            List<String> enchants = new ArrayList<>();
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                enchants.add(entry.getKey().getKey().getKey() + ":" + entry.getValue());
            }
            sb.append(String.join(",", enchants));
        }

        return sb.toString();
    }

    /**
     * Converte una stringa in ItemStack
     */
    private ItemStack parseItemString(String itemString) {
        if (itemString == null || itemString.isEmpty() || itemString.startsWith("AIR:")) {
            return null;
        }

        String[] parts = itemString.split(":");
        if (parts.length < 3) return null;

        try {
            Material material = Material.valueOf(parts[0].toUpperCase());
            int amount = Integer.parseInt(parts[1]);

            ItemStack item = new ItemStack(material, amount);

            // Applica incantesimi se presenti
            if (parts.length > 3 && !parts[3].isEmpty()) {
                String[] enchantments = parts[3].split(",");
                for (String enchantPart : enchantments) {
                    String[] enchantData = enchantPart.split(":");
                    if (enchantData.length == 2) {
                        try {
                            Enchantment enchantment = Enchantment.getByKey(
                                    org.bukkit.NamespacedKey.minecraft(enchantData[0].toLowerCase())
                            );
                            if (enchantment != null) {
                                int level = Integer.parseInt(enchantData[1]);
                                item.addUnsafeEnchantment(enchantment, level);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Incantesimo non valido: " + enchantPart);
                        }
                    }
                }
            }

            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Errore nel parsing dell'item: " + itemString);
            return null;
        }
    }

    /**
     * Salva il file dei kit
     */
    private void saveKitsFile() {
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossibile salvare il file player_kits.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Ottiene la lista di tutti i giocatori con kit salvati
     */
    public List<String> getPlayersWithKits() {
        List<String> players = new ArrayList<>();
        ConfigurationSection playersSection = kitsConfig.getConfigurationSection("players");

        if (playersSection != null) {
            for (String uuid : playersSection.getKeys(false)) {
                String playerName = kitsConfig.getString("players." + uuid + ".name");
                if (playerName != null) {
                    players.add(playerName);
                }
            }
        }

        return players;
    }

    /**
     * Classe per informazioni sul kit
     */
    public static class KitInfo {
        private final String playerName;
        private final long lastUpdate;

        public KitInfo(String playerName, long lastUpdate) {
            this.playerName = playerName;
            this.lastUpdate = lastUpdate;
        }

        public String getPlayerName() { return playerName; }
        public long getLastUpdate() { return lastUpdate; }

        public String getFormattedLastUpdate() {
            if (lastUpdate == 0) return "Mai";

            long diff = System.currentTimeMillis() - lastUpdate;
            long days = diff / (24 * 60 * 60 * 1000);
            long hours = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
            long minutes = (diff % (60 * 60 * 1000)) / (60 * 1000);

            if (days > 0) return days + " giorni fa";
            if (hours > 0) return hours + " ore fa";
            if (minutes > 0) return minutes + " minuti fa";
            return "Pochi secondi fa";
        }
    }
}