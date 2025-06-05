package it.samuconfaa.destinityRaid;

import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RaidCommand implements CommandExecutor {
    private final DestinityRaid plugin;

    public RaidCommand(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (label.equalsIgnoreCase("raid")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "=== Comandi Raid ===");
                sender.sendMessage(ChatColor.YELLOW + "/raid reload - Ricarica la configurazione");
                sender.sendMessage(ChatColor.YELLOW + "/raid stats [giocatore] - Mostra statistiche raid");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("raid.reload")) {
                    ConfigurationManager.reloadConfig();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationManager.GetReload()));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationManager.GetNoReloadPermission()));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("stats")) {
                if (sender.hasPermission("raid.stats")) {
                    String targetPlayer = args.length > 1 ? args[1] : sender.getName();
                    showRaidStats(sender, targetPlayer);
                } else {
                    sender.sendMessage(ChatColor.RED + "Non hai il permesso per vedere le statistiche!");
                }
                return true;
            }
        }
        return false;
    }

    private void showRaidStats(CommandSender sender, String playerName) {
        ConfigurationSection raidsSection = plugin.getRaidStatsManager().getStatsConfig().getConfigurationSection("raids");

        if (raidsSection == null) {
            sender.sendMessage(ChatColor.RED + "Nessuna statistica trovata!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Statistiche Raid per " + playerName + " ===");

        Set<String> raidIds = raidsSection.getKeys(false);
        List<RaidInfo> playerRaids = new ArrayList<>();

        for (String raidId : raidIds) {
            ConfigurationSection raid = raidsSection.getConfigurationSection(raidId);
            if (raid != null) {
                // Controlla se il giocatore è il leader
                String leader = raid.getString("leader", "");
                boolean isLeader = leader.equalsIgnoreCase(playerName);

                // Controlla se il giocatore è tra i membri
                boolean isMember = false;
                ConfigurationSection membersSection = raid.getConfigurationSection("members");
                if (membersSection != null) {
                    for (String memberKey : membersSection.getKeys(false)) {
                        String memberName = membersSection.getString(memberKey + ".name", "");
                        if (memberName.equalsIgnoreCase(playerName)) {
                            isMember = true;
                            break;
                        }
                    }
                }

                // Se è leader o membro, aggiungi alle statistiche
                if (isLeader || isMember) {
                    String world = raid.getString("world", "Sconosciuto");
                    String startTime = raid.getString("start_time", "Sconosciuto");
                    String endTime = raid.getString("end_time", "Sconosciuto");
                    String duration = raid.getString("duration_formatted", "Sconosciuto");
                    int partySize = raid.getInt("party_size", 1);

                    playerRaids.add(new RaidInfo(world, startTime, endTime, duration, leader, partySize, isLeader));
                }
            }
        }

        if (playerRaids.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Nessun raid trovato per " + playerName);
        } else {
            // Ordina i raid per data (più recenti prima)
            playerRaids.sort((a, b) -> b.startTime.compareTo(a.startTime));

            for (RaidInfo raidInfo : playerRaids) {
                sender.sendMessage(ChatColor.YELLOW + "Mondo: " + ChatColor.WHITE + raidInfo.world);
                sender.sendMessage(ChatColor.YELLOW + "Inizio: " + ChatColor.WHITE + raidInfo.startTime);
                sender.sendMessage(ChatColor.YELLOW + "Fine: " + ChatColor.WHITE + raidInfo.endTime);
                sender.sendMessage(ChatColor.YELLOW + "Durata: " + ChatColor.GREEN + raidInfo.duration);

                if (raidInfo.isLeader) {
                    sender.sendMessage(ChatColor.YELLOW + "Ruolo: " + ChatColor.GOLD + "Leader");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Ruolo: " + ChatColor.BLUE + "Membro");
                    sender.sendMessage(ChatColor.YELLOW + "Leader: " + ChatColor.WHITE + raidInfo.leader);
                }

                sender.sendMessage(ChatColor.YELLOW + "Party: " + ChatColor.AQUA + raidInfo.partySize + " membri");
                sender.sendMessage(ChatColor.GRAY + "------------------------");
            }

            sender.sendMessage(ChatColor.GOLD + "Totale raid completati: " + playerRaids.size());

            // Statistiche aggiuntive
            long totalRaidsAsLeader = playerRaids.stream().filter(r -> r.isLeader).count();
            long totalRaidsAsMember = playerRaids.size() - totalRaidsAsLeader;

            if (totalRaidsAsLeader > 0) {
                sender.sendMessage(ChatColor.GOLD + "Come leader: " + totalRaidsAsLeader);
            }
            if (totalRaidsAsMember > 0) {
                sender.sendMessage(ChatColor.BLUE + "Come membro: " + totalRaidsAsMember);
            }
        }
    }

    // Classe helper per organizzare le informazioni dei raid
    private static class RaidInfo {
        final String world;
        final String startTime;
        final String endTime;
        final String duration;
        final String leader;
        final int partySize;
        final boolean isLeader;

        RaidInfo(String world, String startTime, String endTime, String duration, String leader, int partySize, boolean isLeader) {
            this.world = world;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
            this.leader = leader;
            this.partySize = partySize;
            this.isLeader = isLeader;
        }
    }
}