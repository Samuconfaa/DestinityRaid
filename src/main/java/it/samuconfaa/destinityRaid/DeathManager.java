package it.samuconfaa.destinityRaid;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathManager implements Listener {
    private final DestinityRaid plugin;
    // Traccia quante volte un giocatore è morto durante un raid
    private Map<UUID, Integer> playerDeaths = new HashMap<>();
    // Traccia in quale mondo il giocatore è morto
    private Map<UUID, String> playerDeathWorlds = new HashMap<>();

    public DeathManager(DestinityRaid plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Controlla se il giocatore è in un raid
        String occupiedWorld = findOccupiedWorldByPartyMember(player);
        if (occupiedWorld == null) {
            return; // Non è in un raid, comportamento normale
        }

        UUID playerUUID = player.getUniqueId();
        int currentDeaths = playerDeaths.getOrDefault(playerUUID, 0);
        currentDeaths++;
        playerDeaths.put(playerUUID, currentDeaths);
        playerDeathWorlds.put(playerUUID, occupiedWorld);

        if (currentDeaths == 1) {
            // Prima morte - messaggio di avvertimento
            player.sendMessage(ChatColor.YELLOW + "Attenzione! Hai " + ChatColor.RED + "1 vita rimanente" +
                    ChatColor.YELLOW + ". Se muori di nuovo diventerai spettatore!");
        } else if (currentDeaths >= 2) {
            // Seconda morte o più - diventa spettatore
            player.sendMessage(ChatColor.RED + "Hai esaurito le tue vite! Sei ora in modalità spettatore.");

            // Informa il party
            String partyLeader = getPartyLeaderForWorld(occupiedWorld);
            if (partyLeader != null) {
                Player leader = plugin.getServer().getPlayer(partyLeader);
                if (leader != null) {
                    plugin.getPartyManager().sendMessageToParty(leader,
                            ChatColor.GRAY + player.getName() + " è diventato spettatore dopo aver esaurito le vite!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        String deathWorld = playerDeathWorlds.get(playerUUID);
        if (deathWorld == null) {
            return; // Non è morto in un raid
        }

        int deaths = playerDeaths.getOrDefault(playerUUID, 0);
        if (deaths >= 2) {
            // Metti in spectator mode dopo il respawn
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(ChatColor.GRAY + "Sei in modalità spettatore. Puoi seguire i tuoi compagni di squadra!");
            }, 1L); // Aspetta 1 tick per assicurarsi che il respawn sia completato
        }
    }

    // Metodo chiamato quando un giocatore inizia un raid
    public void onRaidStart(Player player) {
        UUID playerUUID = player.getUniqueId();
        playerDeaths.put(playerUUID, 0);
        playerDeathWorlds.remove(playerUUID);

        // Assicurati che sia in survival mode
        player.setGameMode(GameMode.SURVIVAL);
    }

    // Metodo chiamato quando un raid finisce
    public void onRaidEnd(Player player) {
        UUID playerUUID = player.getUniqueId();
        playerDeaths.remove(playerUUID);
        playerDeathWorlds.remove(playerUUID);

        // Ripristina la modalità survival
        player.setGameMode(GameMode.SURVIVAL);
    }

    // Pulisce i dati quando un giocatore lascia il server
    public void onPlayerQuit(Player player) {
        UUID playerUUID = player.getUniqueId();
        playerDeaths.remove(playerUUID);
        playerDeathWorlds.remove(playerUUID);
    }

    // Ottieni il numero di morti di un giocatore nel raid corrente
    public int getPlayerDeaths(Player player) {
        return playerDeaths.getOrDefault(player.getUniqueId(), 0);
    }

    // Controlla se un giocatore è in modalità spettatore per aver esaurito le vite
    public boolean isPlayerSpectatorFromDeaths(Player player) {
        return playerDeaths.getOrDefault(player.getUniqueId(), 0) >= 2;
    }

    // Metodi helper copiati da PlayerListener per trovare il mondo occupato
    private String findOccupiedWorldByPartyMember(Player player) {
        // Prima controlla se il giocatore stesso ha occupato un mondo
        String occupiedWorld = plugin.getWorldManager().getOccupiedWorldByPlayer(player.getUniqueId());
        if (occupiedWorld != null) {
            return occupiedWorld;
        }

        // Se Parties non è abilitato, non può essere membro di nessun party
        if (!plugin.getPartyManager().isPartiesEnabled()) {
            return null;
        }

        // Ottieni tutti i membri del party del giocatore
        java.util.List<Player> partyMembers = plugin.getPartyManager().getPartyMembers(player);

        // Controlla se qualche membro del party ha occupato un mondo
        for (Player member : partyMembers) {
            if (member != null && !member.equals(player)) {
                occupiedWorld = plugin.getWorldManager().getOccupiedWorldByPlayer(member.getUniqueId());
                if (occupiedWorld != null) {
                    // Verifica che il giocatore sia nello stesso mondo del mondo occupato
                    java.util.Map<String, ConfigurationManager.WorldInfo> worlds = ConfigurationManager.getWorlds();
                    ConfigurationManager.WorldInfo worldInfo = worlds.get(occupiedWorld);
                    if (worldInfo != null && player.getWorld().getName().equals(worldInfo.getWorldName())) {
                        return occupiedWorld;
                    }
                }
            }
        }

        return null;
    }

    private String getPartyLeaderForWorld(String worldKey) {
        java.util.UUID occupantUUID = plugin.getWorldManager().getWorldOccupant(worldKey);
        if (occupantUUID != null) {
            Player leader = org.bukkit.Bukkit.getPlayer(occupantUUID);
            return leader != null ? leader.getName() : null;
        }
        return null;
    }
}