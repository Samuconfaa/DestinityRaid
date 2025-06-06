package it.samuconfaa.destinityRaid;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class KitCommand implements CommandExecutor {
    private final DestinityRaid plugin;

    public KitCommand(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Questo comando può essere usato solo dai giocatori!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showKitHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                showAvailableKits(player);
                break;

            case "select":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /kit select <nome_kit>");
                    return true;
                }
                selectKit(player, args[1]);
                break;

            case "current":
                showCurrentKit(player);
                break;

            case "preview":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /kit preview <nome_kit>");
                    return true;
                }
                previewKit(player, args[1]);
                break;

            case "save":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /kit save <nome_kit>");
                    return true;
                }
                saveCurrentKit(player, args[1]);
                break;

            case "equip":
                if (args.length < 2) {
                    // Equipaggia il kit corrente del giocatore
                    equipCurrentKit(player);
                } else {
                    // Equipaggia un kit specifico
                    equipSpecificKit(player, args[1]);
                }
                break;

            default:
                showKitHelp(player);
                break;
        }

        return true;
    }

    private void showKitHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Comandi Kit ===");
        player.sendMessage(ChatColor.YELLOW + "/kit list - Mostra tutti i kit disponibili");
        player.sendMessage(ChatColor.YELLOW + "/kit select <nome> - Seleziona un kit per i raid");
        player.sendMessage(ChatColor.YELLOW + "/kit current - Mostra il tuo kit attuale");
        player.sendMessage(ChatColor.YELLOW + "/kit preview <nome> - Anteprima di un kit");
        player.sendMessage(ChatColor.YELLOW + "/kit save <nome> - Salva il tuo inventario come kit");
        player.sendMessage(ChatColor.YELLOW + "/kit equip [nome] - Equipaggia un kit");
    }

    private void showAvailableKits(Player player) {
        Set<String> kits = plugin.getKitManager().getAvailableKits();

        if (kits.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Nessun kit disponibile!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Kit Disponibili ===");
        String currentKit = plugin.getKitManager().getPlayerKit(player);

        for (String kitId : kits) {
            String displayName = plugin.getKitManager().getKitDisplayName(kitId);
            String marker = kitId.equals(currentKit) ? ChatColor.GREEN + " [SELEZIONATO]" : "";
            player.sendMessage(ChatColor.YELLOW + "- " + displayName + " (" + kitId + ")" + marker);
        }
    }

    private void selectKit(Player player, String kitId) {
        kitId = kitId.toLowerCase();

        if (!plugin.getKitManager().kitExists(kitId)) {
            player.sendMessage(ChatColor.RED + "Kit '" + kitId + "' non trovato!");
            player.sendMessage(ChatColor.GRAY + "Usa '/kit list' per vedere i kit disponibili.");
            return;
        }

        plugin.getKitManager().setPlayerKit(player, kitId);
    }

    private void showCurrentKit(Player player) {
        String currentKit = plugin.getKitManager().getPlayerKit(player);
        String displayName = plugin.getKitManager().getKitDisplayName(currentKit);

        player.sendMessage(ChatColor.GREEN + "Il tuo kit attuale è: " + ChatColor.YELLOW + displayName +
                ChatColor.GREEN + " (" + currentKit + ")");
    }

    private void previewKit(Player player, String kitId) {
        kitId = kitId.toLowerCase();

        if (!plugin.getKitManager().kitExists(kitId)) {
            player.sendMessage(ChatColor.RED + "Kit '" + kitId + "' non trovato!");
            return;
        }

        String displayName = plugin.getKitManager().getKitDisplayName(kitId);
        player.sendMessage(ChatColor.GOLD + "=== Anteprima Kit: " + displayName + " ===");

        // Per ora mostra solo il nome, in futuro si potrebbe implementare
        // una GUI per mostrare tutti gli oggetti del kit
        player.sendMessage(ChatColor.GRAY + "Usa '/kit equip " + kitId + "' per testare questo kit!");
        player.sendMessage(ChatColor.GRAY + "Attenzione: questo sostituirà il tuo inventario attuale!");
    }

    private void saveCurrentKit(Player player, String kitName) {
        // Controlla se il giocatore è nel mondo hub
        String hubWorldName = ConfigurationManager.getHubWorldName();
        if (!player.getWorld().getName().equals(hubWorldName)) {
            player.sendMessage(ChatColor.RED + "Puoi salvare kit solo nel mondo hub!");
            return;
        }

        // Controlla se l'inventario non è vuoto
        if (player.getInventory().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Il tuo inventario è vuoto! Non puoi salvare un kit vuoto.");
            return;
        }

        plugin.getKitManager().savePlayerKit(player, kitName);
    }

    private void equipCurrentKit(Player player) {
        String currentKit = plugin.getKitManager().getPlayerKit(player);
        plugin.getKitManager().giveKit(player, currentKit);
    }

    private void equipSpecificKit(Player player, String kitId) {
        kitId = kitId.toLowerCase();

        if (!plugin.getKitManager().kitExists(kitId)) {
            player.sendMessage(ChatColor.RED + "Kit '" + kitId + "' non trovato!");
            return;
        }

        // Controlla se il giocatore è nel mondo hub
        String hubWorldName = ConfigurationManager.getHubWorldName();
        if (!player.getWorld().getName().equals(hubWorldName)) {
            player.sendMessage(ChatColor.RED + "Puoi equipaggiare kit solo nel mondo hub!");
            return;
        }

        plugin.getKitManager().giveKit(player, kitId);
    }
}