package it.samuconfaa.destinityRaid;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetStatsCommand implements CommandExecutor {
    private final DestinityRaid plugin;

    public ResetStatsCommand(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("raid.stats.reset")) {
            sender.sendMessage("§cNon hai il permesso per eseguire questo comando!");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUso corretto: /resetstats <giocatore>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        // Se il giocatore non è online, prova a cercare nelle statistiche salvate
        if (target == null) {
            // Cerca il giocatore nelle statistiche per nome
            boolean found = plugin.getRaidStatsManager().resetPlayerStatsByName(targetName);
            if (found) {
                sender.sendMessage("§aStatistiche di §e" + targetName + " §aresettate con successo!");
            } else {
                sender.sendMessage("§cGiocatore §e" + targetName + " §cnon trovato o senza statistiche!");
            }
        } else {
            // Il giocatore è online, usa il suo UUID
            boolean reset = plugin.getRaidStatsManager().resetPlayerStats(target);
            if (reset) {
                sender.sendMessage("§aStatistiche di §e" + target.getName() + " §aresettate con successo!");
                target.sendMessage("§6Le tue statistiche raid sono state resettate da un amministratore.");
            } else {
                sender.sendMessage("§cNessuna statistica trovata per §e" + target.getName() + "§c!");
            }
        }

        return true;
    }
}