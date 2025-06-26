package it.samuconfaa.destinityRaid;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * PlaceholderExpansion per DestinityRaid
 *
 * Placeholder disponibili:
 * - %destinityraid_leaderboard_[mondo]_[posizione]_name% - Nome del giocatore in quella posizione
 * - %destinityraid_leaderboard_[mondo]_[posizione]_time% - Tempo del giocatore in quella posizione
 * - %destinityraid_player_[mondo]_best_time% - Miglior tempo del giocatore per quel mondo
 * - %destinityraid_player_[mondo]_completions% - Numero di completamenti del giocatore per quel mondo
 * - %destinityraid_player_total_raids% - Totale raid completati dal giocatore
 * - %destinityraid_player_rank_[mondo]% - Posizione del giocatore nella classifica di quel mondo
 * - %destinityraid_player_rank_total% - Posizione del giocatore nella classifica generale
 */
public class DestinityRaidPlaceholderExpansion extends PlaceholderExpansion {

    private final DestinityRaid plugin;

    public DestinityRaidPlaceholderExpansion(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "destinityraid";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SamuConfaa";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params == null) {
            return null;
        }

        // Placeholder per le classifiche dei mondi
        // Formato: leaderboard_[mondo]_[posizione]_name o leaderboard_[mondo]_[posizione]_time
        if (params.startsWith("leaderboard_")) {
            return handleLeaderboardPlaceholder(params);
        }

        // Placeholder per le statistiche del giocatore
        // Formato: player_[mondo]_best_time, player_[mondo]_completions, etc.
        if (params.startsWith("player_") && player != null) {
            return handlePlayerPlaceholder(player, params);
        }

        return null;
    }

    /**
     * Gestisce i placeholder delle classifiche
     * Esempi:
     * - leaderboard_mondo1_1_name -> Nome del primo classificato in mondo1
     * - leaderboard_mondo1_1_time -> Tempo del primo classificato in mondo1
     */
    private String handleLeaderboardPlaceholder(String params) {
        String[] parts = params.split("_");
        if (parts.length != 4) {
            return "Formato errato";
        }

        String worldKey = parts[1];
        String positionStr = parts[2];
        String type = parts[3]; // "name" o "time"

        try {
            int position = Integer.parseInt(positionStr);
            if (position < 1) {
                return "Posizione non valida";
            }

            // Ottieni la classifica per il mondo specificato
            List<RaidStatsManager.PlayerRankingEntry> leaderboard =
                    plugin.getRaidStatsManager().getWorldBestTimesLeaderboard(worldKey, position);

            if (leaderboard.size() < position) {
                return "N/A"; // Nessun giocatore in quella posizione
            }

            RaidStatsManager.PlayerRankingEntry entry = leaderboard.get(position - 1);

            switch (type.toLowerCase()) {
                case "name":
                    return entry.getPlayerName();
                case "time":
                    return entry.getFormattedValue();
                default:
                    return "Tipo non valido";
            }

        } catch (NumberFormatException e) {
            return "Posizione non valida";
        }
    }

    /**
     * Gestisce i placeholder delle statistiche del giocatore
     */
    private String handlePlayerPlaceholder(Player player, String params) {
        String[] parts = params.split("_");

        if (parts.length == 2) {
            // Placeholder generali del giocatore
            switch (parts[1]) {
                case "total":
                    if (parts.length > 2 && parts[2].equals("raids")) {
                        return String.valueOf(plugin.getRaidStatsManager().getPlayerTotalRaids(player.getName()));
                    }
                    break;
                case "rank":
                    if (parts.length > 2) {
                        if (parts[2].equals("total")) {
                            return String.valueOf(getPlayerTotalRank(player.getName()));
                        } else {
                            // Rank per un mondo specifico
                            String worldKey = parts[2];
                            return String.valueOf(getPlayerWorldRank(player.getName(), worldKey));
                        }
                    }
                    break;
            }
        } else if (parts.length >= 3) {
            String worldKey = parts[1];
            String statType = parts[2];

            switch (statType) {
                case "best":
                    if (parts.length > 3 && parts[3].equals("time")) {
                        return plugin.getRaidStatsManager().getPlayerBestTime(player.getName(), worldKey);
                    }
                    break;
                case "completions":
                    return String.valueOf(plugin.getRaidStatsManager().getPlayerWorldRaids(player.getName(), worldKey));
                case "rank":
                    return String.valueOf(getPlayerWorldRank(player.getName(), worldKey));
            }
        }

        // Gestione alternativa per formati specifici
        if (params.equals("player_total_raids")) {
            return String.valueOf(plugin.getRaidStatsManager().getPlayerTotalRaids(player.getName()));
        }

        if (params.equals("player_rank_total")) {
            return String.valueOf(getPlayerTotalRank(player.getName()));
        }

        // Formato: player_[mondo]_best_time
        if (params.matches("player_\\w+_best_time")) {
            String worldKey = params.replace("player_", "").replace("_best_time", "");
            return plugin.getRaidStatsManager().getPlayerBestTime(player.getName(), worldKey);
        }

        // Formato: player_[mondo]_completions
        if (params.matches("player_\\w+_completions")) {
            String worldKey = params.replace("player_", "").replace("_completions", "");
            return String.valueOf(plugin.getRaidStatsManager().getPlayerWorldRaids(player.getName(), worldKey));
        }

        // Formato: player_rank_[mondo]
        if (params.startsWith("player_rank_")) {
            String worldKey = params.replace("player_rank_", "");
            if (!worldKey.equals("total")) {
                return String.valueOf(getPlayerWorldRank(player.getName(), worldKey));
            }
        }

        return null;
    }

    /**
     * Ottiene il rank del giocatore nella classifica generale
     */
    private int getPlayerTotalRank(String playerName) {
        List<RaidStatsManager.PlayerRankingEntry> leaderboard =
                plugin.getRaidStatsManager().getTotalRaidsLeaderboard(1000);

        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getPlayerName().equalsIgnoreCase(playerName)) {
                return i + 1;
            }
        }
        return -1; // Giocatore non trovato
    }

    /**
     * Ottiene il rank del giocatore nella classifica di un mondo specifico
     */
    private int getPlayerWorldRank(String playerName, String worldKey) {
        List<RaidStatsManager.PlayerRankingEntry> leaderboard =
                plugin.getRaidStatsManager().getWorldBestTimesLeaderboard(worldKey, 1000);

        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getPlayerName().equalsIgnoreCase(playerName)) {
                return i + 1;
            }
        }
        return -1; // Giocatore non trovato
    }
}