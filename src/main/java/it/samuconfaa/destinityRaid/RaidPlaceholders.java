package it.samuconfaa.destinityRaid;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RaidPlaceholders extends PlaceholderExpansion {
    private final DestinityRaid plugin;

    public RaidPlaceholders(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "destinityraid";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Questo è necessario perché il placeholder non dovrebbe scomparire al reload
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        RaidStatsManager statsManager = plugin.getRaidStatsManager();

        if (player == null) {
            return "Giocatore non trovato";
        }

        String playerName = player.getName();

        // Placeholder per raid totali: %destinityraid_total_raids%
        if (params.equals("total_raids")) {
            return String.valueOf(statsManager.getPlayerTotalRaids(playerName));
        }

        // Placeholder per raid in un mondo specifico: %destinityraid_world_[MONDO]_raids%
        if (params.startsWith("world_") && params.endsWith("_raids")) {
            String worldKey = params.substring(6, params.length() - 6); // Rimuove "world_" e "_raids"
            return String.valueOf(statsManager.getPlayerWorldRaids(playerName, worldKey));
        }

        // Placeholder per tempo migliore in un mondo: %destinityraid_world_[MONDO]_best%
        if (params.startsWith("world_") && params.endsWith("_best")) {
            String worldKey = params.substring(6, params.length() - 5); // Rimuove "world_" e "_best"
            return statsManager.getPlayerBestTime(playerName, worldKey);
        }

        // Placeholder per classifiche raid totali: %destinityraid_leaderboard_total_[POSIZIONE]_name%
        if (params.startsWith("leaderboard_total_") && params.endsWith("_name")) {
            try {
                String posStr = params.substring(18, params.length() - 5);
                int position = Integer.parseInt(posStr) - 1; // Le posizioni iniziano da 1
                List<RaidStatsManager.PlayerRankingEntry> leaderboard = statsManager.getTotalRaidsLeaderboard(50);

                if (position >= 0 && position < leaderboard.size()) {
                    return leaderboard.get(position).getPlayerName();
                }
                return "-";
            } catch (NumberFormatException e) {
                return "ERRORE";
            }
        }

        // Placeholder per valori classifica raid totali: %destinityraid_leaderboard_total_[POSIZIONE]_value%
        if (params.startsWith("leaderboard_total_") && params.endsWith("_value")) {
            try {
                String posStr = params.substring(18, params.length() - 6);
                int position = Integer.parseInt(posStr) - 1;
                List<RaidStatsManager.PlayerRankingEntry> leaderboard = statsManager.getTotalRaidsLeaderboard(50);

                if (position >= 0 && position < leaderboard.size()) {
                    return String.valueOf(leaderboard.get(position).getValue());
                }
                return "-";
            } catch (NumberFormatException e) {
                return "ERRORE";
            }
        }

        // Placeholder per classifiche tempi migliori: %destinityraid_leaderboard_[MONDO]_[POSIZIONE]_name%
        if (params.startsWith("leaderboard_") && params.contains("_") && params.endsWith("_name")) {
            try {
                String[] parts = params.split("_");
                if (parts.length >= 4) {
                    String worldKey = parts[1];
                    int position = Integer.parseInt(parts[2]) - 1;
                    List<RaidStatsManager.PlayerRankingEntry> leaderboard = statsManager.getWorldBestTimesLeaderboard(worldKey, 50);

                    if (position >= 0 && position < leaderboard.size()) {
                        return leaderboard.get(position).getPlayerName();
                    }
                }
                return "-";
            } catch (NumberFormatException e) {
                return "ERRORE";
            }
        }

        // Placeholder per valori classifica tempi migliori: %destinityraid_leaderboard_[MONDO]_[POSIZIONE]_time%
        if (params.startsWith("leaderboard_") && params.contains("_") && params.endsWith("_time")) {
            try {
                String[] parts = params.split("_");
                if (parts.length >= 4) {
                    String worldKey = parts[1];
                    int position = Integer.parseInt(parts[2]) - 1;
                    List<RaidStatsManager.PlayerRankingEntry> leaderboard = statsManager.getWorldBestTimesLeaderboard(worldKey, 50);

                    if (position >= 0 && position < leaderboard.size()) {
                        return leaderboard.get(position).getFormattedValue();
                    }
                }
                return "-";
            } catch (NumberFormatException e) {
                return "ERRORE";
            }
        }

        // Placeholder per posizione del giocatore nella classifica totale: %destinityraid_rank_total%
        if (params.equals("rank_total")) {
            List<RaidStatsManager.PlayerRankingEntry> leaderboard = statsManager.getTotalRaidsLeaderboard(1000);
            for (int i = 0; i < leaderboard.size(); i++) {
                if (leaderboard.get(i).getPlayerName().equalsIgnoreCase(playerName)) {
                    return String.valueOf(i + 1);
                }
            }
            return "Non classificato";
        }

        // Placeholder per posizione del giocatore nella classifica di un mondo: %destinityraid_rank_[MONDO]%
        if (params.startsWith("rank_") && !params.equals("rank_total")) {
            String worldKey = params.substring(5);
            List<RaidStatsManager.PlayerRankingEntry> leaderboard = statsManager.getWorldBestTimesLeaderboard(worldKey, 1000);
            for (int i = 0; i < leaderboard.size(); i++) {
                if (leaderboard.get(i).getPlayerName().equalsIgnoreCase(playerName)) {
                    return String.valueOf(i + 1);
                }
            }
            return "Non classificato";
        }

        return null; // Placeholder non riconosciuto
    }
}