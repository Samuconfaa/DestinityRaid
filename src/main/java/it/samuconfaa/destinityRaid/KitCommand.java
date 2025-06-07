package it.samuconfaa.destinityRaid;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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

        // Controlla se il giocatore è nel mondo consentito per la modifica dei kit
        String allowedWorld = ConfigurationManager.getKitEditWorld();
        if (!player.getWorld().getName().equals(allowedWorld)) {
            player.sendMessage(ChatColor.RED + "Puoi modificare il tuo kit solo nel mondo: " + allowedWorld + "!");
            return true;
        }

        // Se non ci sono argomenti, apri la GUI
        if (args.length == 0) {
            plugin.getKitGUI().openKitMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gui":
            case "menu":
                plugin.getKitGUI().openKitMenu(player);
                break;


            case "help":
                showKitHelp(player);
                break;

            default:
                showKitHelp(player);
                break;
        }

        return true;
    }

    private void showKitHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Comandi Kit ===");
        player.sendMessage(ChatColor.YELLOW + "/kit - Apre il menu di gestione kit");
        player.sendMessage(ChatColor.YELLOW + "/kit gui - Apre il menu di gestione kit");
        player.sendMessage(ChatColor.YELLOW + "/kit help - Mostra questo aiuto");
        player.sendMessage(ChatColor.GRAY + "Nota: Puoi modificare il kit solo nel mondo: " + ConfigurationManager.getKitEditWorld());
    }
}