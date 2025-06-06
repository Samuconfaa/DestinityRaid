package it.samuconfaa.destinityRaid;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {
    private final DestinityRaid plugin;
    private File kitsFile;
    private FileConfiguration kitsConfig;
    private Map<UUID, String> playerSelectedKits = new HashMap<>();

    public KitManager(DestinityRaid plugin) {
        this.plugin = plugin;
        loadKitsFile();
        loadPlayerKits();
    }

    private void loadKitsFile() {
        kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!kitsFile.exists()) {
            try {
                kitsFile.createNewFile();
                // Crea la configurazione di default
                createDefaultKits();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossibile creare il file kits.yml!");
            }
        }
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
    }

    private void createDefaultKits() {
        // Kit Warrior (default)
        createKit("warrior", "Guerriero", true, Arrays.asList(
                "IRON_SWORD:1:0:sharpness:2",
                "IRON_HELMET:1:0",
                "IRON_CHESTPLATE:1:0",
                "IRON_LEGGINGS:1:0",
                "IRON_BOOTS:1:0",
                "COOKED_BEEF:16:0",
                "GOLDEN_APPLE:3:0",
                "SHIELD:1:0"
        ));

        // Kit Archer
        createKit("archer", "Arciere", false, Arrays.asList(
                "BOW:1:0:power:3,infinity:1",
                "ARROW:1:0",
                "LEATHER_HELMET:1:0:protection:2",
                "LEATHER_CHESTPLATE:1:0:protection:2",
                "LEATHER_LEGGINGS:1:0:protection:2",
                "LEATHER_BOOTS:1:0:protection:2,feather_falling:3",
                "COOKED_CHICKEN:12:0",
                "GOLDEN_APPLE:2:0",
                "IRON_SWORD:1:0"
        ));

        // Kit Miner
        createKit("miner", "Minatore", false, Arrays.asList(
                "IRON_PICKAXE:1:0:efficiency:3,unbreaking:2",
                "IRON_SHOVEL:1:0:efficiency:2",
                "IRON_HELMET:1:0",
                "IRON_CHESTPLATE:1:0",
                "IRON_LEGGINGS:1:0",
                "IRON_BOOTS:1:0",
                "TORCH:32:0",
                "COOKED_PORKCHOP:8:0",
                "GOLDEN_APPLE:2:0",
                "STONE_SWORD:1:0"
        ));

        saveKits();
        plugin.getLogger().info("Kit di default creati!");
    }

    private void createKit(String kitId, String displayName, boolean isDefault, List<String> items) {
        String basePath = "kits." + kitId;
        kitsConfig.set(basePath + ".display_name", displayName);
        kitsConfig.set(basePath + ".is_default", isDefault);
        kitsConfig.set(basePath + ".items", items);
    }

    public void giveKit(Player player, String kitId) {
        ConfigurationSection kitSection = kitsConfig.getConfigurationSection("kits." + kitId);
        if (kitSection == null) {
            player.sendMessage(ChatColor.RED + "Kit non trovato!");
            return;
        }

        List<String> itemStrings = kitSection.getStringList("items");
        PlayerInventory inventory = player.getInventory();

        // Pulisci l'inventario prima di dare il kit
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);

        for (String itemString : itemStrings) {
            ItemStack item = parseItemString(itemString);
            if (item != null) {
                // Se è un'armatura, mettila nel slot corretto
                if (isArmor(item.getType())) {
                    equipArmor(player, item);
                } else {
                    // Altrimenti aggiungila all'inventario
                    inventory.addItem(item);
                }
            }
        }

        String displayName = kitSection.getString("display_name", kitId);
        player.sendMessage(ChatColor.GREEN + "Kit " + displayName + " equipaggiato!");
    }

    private ItemStack parseItemString(String itemString) {
        String[] parts = itemString.split(":");
        if (parts.length < 3) return null;

        try {
            Material material = Material.valueOf(parts[0].toUpperCase());
            int amount = Integer.parseInt(parts[1]);
            // parts[2] era per durability in versioni vecchie, ora ignorato

            ItemStack item = new ItemStack(material, amount);

            // Applica incantesimi se presenti
            if (parts.length > 3) {
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

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private void equipArmor(Player player, ItemStack armor) {
        PlayerInventory inventory = player.getInventory();
        String materialName = armor.getType().name();

        if (materialName.endsWith("_HELMET")) {
            inventory.setHelmet(armor);
        } else if (materialName.endsWith("_CHESTPLATE")) {
            inventory.setChestplate(armor);
        } else if (materialName.endsWith("_LEGGINGS")) {
            inventory.setLeggings(armor);
        } else if (materialName.endsWith("_BOOTS")) {
            inventory.setBoots(armor);
        }
    }

    public void setPlayerKit(Player player, String kitId) {
        if (!kitExists(kitId)) {
            player.sendMessage(ChatColor.RED + "Kit non esistente!");
            return;
        }

        playerSelectedKits.put(player.getUniqueId(), kitId);
        savePlayerKits();

        String displayName = getKitDisplayName(kitId);
        player.sendMessage(ChatColor.GREEN + "Kit selezionato: " + displayName);
    }

    public String getPlayerKit(Player player) {
        String kit = playerSelectedKits.get(player.getUniqueId());
        if (kit == null) {
            // Restituisci il kit di default
            return getDefaultKit();
        }
        return kit;
    }

    public String getDefaultKit() {
        ConfigurationSection kitsSection = kitsConfig.getConfigurationSection("kits");
        if (kitsSection != null) {
            for (String kitId : kitsSection.getKeys(false)) {
                if (kitsConfig.getBoolean("kits." + kitId + ".is_default", false)) {
                    return kitId;
                }
            }
        }
        return "warrior"; // Fallback
    }

    public boolean kitExists(String kitId) {
        return kitsConfig.contains("kits." + kitId);
    }

    public String getKitDisplayName(String kitId) {
        return kitsConfig.getString("kits." + kitId + ".display_name", kitId);
    }

    public Set<String> getAvailableKits() {
        ConfigurationSection kitsSection = kitsConfig.getConfigurationSection("kits");
        return kitsSection != null ? kitsSection.getKeys(false) : new HashSet<>();
    }

    public void savePlayerKit(Player player, String kitName) {
        String kitId = kitName.toLowerCase().replaceAll("[^a-z0-9]", "");
        String basePath = "kits." + kitId;

        kitsConfig.set(basePath + ".display_name", kitName);
        kitsConfig.set(basePath + ".is_default", false);

        List<String> items = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();

        // Aggiungi l'armatura
        if (inventory.getHelmet() != null) {
            items.add(itemToString(inventory.getHelmet()));
        }
        if (inventory.getChestplate() != null) {
            items.add(itemToString(inventory.getChestplate()));
        }
        if (inventory.getLeggings() != null) {
            items.add(itemToString(inventory.getLeggings()));
        }
        if (inventory.getBoots() != null) {
            items.add(itemToString(inventory.getBoots()));
        }

        // Aggiungi gli oggetti dall'inventario
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(itemToString(item));
            }
        }

        kitsConfig.set(basePath + ".items", items);
        saveKits();

        player.sendMessage(ChatColor.GREEN + "Kit '" + kitName + "' salvato!");
    }

    private String itemToString(ItemStack item) {
        StringBuilder sb = new StringBuilder();
        sb.append(item.getType().name()).append(":");
        sb.append(item.getAmount()).append(":");
        sb.append("0"); // Durability placeholder

        // Aggiungi incantesimi
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

    private void loadPlayerKits() {
        ConfigurationSection playersSection = kitsConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidString : playersSection.getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(uuidString);
                    String kitId = playersSection.getString(uuidString);
                    playerSelectedKits.put(playerUUID, kitId);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("UUID non valido nel file kits: " + uuidString);
                }
            }
        }
    }

    private void savePlayerKits() {
        for (Map.Entry<UUID, String> entry : playerSelectedKits.entrySet()) {
            kitsConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }
        saveKits();
    }

    private void saveKits() {
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossibile salvare il file kits.yml!");
            e.printStackTrace();
        }
    }

    public void onPlayerQuit(Player player) {
        // Non rimuovere il kit selezionato quando un giocatore lascia il server
        // Il kit rimane salvato per la prossima volta
    }
}