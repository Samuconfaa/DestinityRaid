package it.samuconfaa.destinityRaid;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyManager {
    private final DestinityRaid plugin;
    private PartiesAPI partiesAPI;
    private boolean partiesEnabled = false;

    public PartyManager(DestinityRaid plugin) {
        this.plugin = plugin;
        initializePartiesAPI();
    }

    private void initializePartiesAPI() {
        if (Bukkit.getPluginManager().getPlugin("Parties") != null) {
            try {
                partiesAPI = Parties.getApi();
                partiesEnabled = true;
                plugin.getLogger().info("Integrazione con Parties abilitata!");
            } catch (Exception e) {
                plugin.getLogger().warning("Impossibile collegare Parties API: " + e.getMessage());
                partiesEnabled = false;
            }
        } else {
            plugin.getLogger().warning("Plugin Parties non trovato! Funzionalità party disabilitate.");
            partiesEnabled = false;
        }
    }

    public boolean isPartiesEnabled() {
        return partiesEnabled;
    }

    public boolean isPlayerInParty(Player player) {
        if (!partiesEnabled) return false;

        PartyPlayer partyPlayer = partiesAPI.getPartyPlayer(player.getUniqueId());
        return partyPlayer != null && partyPlayer.getPartyId() != null;
    }

    public boolean isPlayerPartyLeader(Player player) {
        if (!partiesEnabled) return false;

        PartyPlayer partyPlayer = partiesAPI.getPartyPlayer(player.getUniqueId());
        if (partyPlayer == null || partyPlayer.getPartyId() == null) {
            return false;
        }

        Party party = partiesAPI.getParty(partyPlayer.getPartyId());
        return party != null && party.getLeader() != null && party.getLeader().equals(player.getUniqueId());
    }

    public int getPartySize(Player player) {
        if (!partiesEnabled) return 1;

        PartyPlayer partyPlayer = partiesAPI.getPartyPlayer(player.getUniqueId());
        if (partyPlayer == null || partyPlayer.getPartyId() == null) {
            return 1;
        }

        Party party = partiesAPI.getParty(partyPlayer.getPartyId());
        return party != null ? party.getMembers().size() : 1;
    }

    public List<Player> getPartyMembers(Player player) {
        List<Player> members = new ArrayList<>();

        if (!partiesEnabled) {
            members.add(player);
            return members;
        }

        PartyPlayer partyPlayer = partiesAPI.getPartyPlayer(player.getUniqueId());
        if (partyPlayer == null || partyPlayer.getPartyId() == null) {
            members.add(player);
            return members;
        }

        Party party = partiesAPI.getParty(partyPlayer.getPartyId());
        if (party != null) {
            for (UUID memberUUID : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    members.add(member);
                }
            }
        }

        return members;
    }

    public boolean areAllPartyMembersOnline(Player player) {
        if (!partiesEnabled) return true;

        PartyPlayer partyPlayer = partiesAPI.getPartyPlayer(player.getUniqueId());
        if (partyPlayer == null || partyPlayer.getPartyId() == null) {
            return true;
        }

        Party party = partiesAPI.getParty(partyPlayer.getPartyId());
        if (party != null) {
            for (UUID memberUUID : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member == null || !member.isOnline()) {
                    return false;
                }
            }
        }

        return true;
    }

    public String validatePartyForRaid(Player player) {
        // Se Parties non è abilitato, permetti sempre l'accesso
        if (!partiesEnabled) {
            return null; // null = nessun errore
        }

        // Controlla se il giocatore è in una party
        if (!isPlayerInParty(player)) {
            return ChatColor.translateAlternateColorCodes('&',
                    ConfigurationManager.getPartyNotFoundMessage());
        }

        // Controlla se è il leader (se richiesto)
        if (ConfigurationManager.isRequirePartyLeader() && !isPlayerPartyLeader(player)) {
            return ChatColor.translateAlternateColorCodes('&',
                    ConfigurationManager.getNotPartyLeaderMessage());
        }

        // Controlla il numero minimo di membri
        int partySize = getPartySize(player);
        int minMembers = ConfigurationManager.getPartyMinMembers();
        if (partySize < minMembers) {
            return ChatColor.translateAlternateColorCodes('&',
                    ConfigurationManager.getPartyTooSmallMessage()
                            .replace("{min}", String.valueOf(minMembers)));
        }

        // Controlla il numero massimo di membri
        int maxMembers = ConfigurationManager.getPartyMaxMembers();
        if (partySize > maxMembers) {
            return ChatColor.translateAlternateColorCodes('&',
                    ConfigurationManager.getPartyTooBigMessage()
                            .replace("{max}", String.valueOf(maxMembers)));
        }

        // Controlla se tutti i membri sono online
        if (!areAllPartyMembersOnline(player)) {
            return ChatColor.translateAlternateColorCodes('&',
                    ConfigurationManager.getPartyMembersNotOnlineMessage());
        }

        return null; // Tutto ok
    }

    public void teleportPartyMembers(Player player, org.bukkit.Location location) {
        List<Player> members = getPartyMembers(player);

        for (Player member : members) {
            member.teleport(location);
        }
    }

    public void sendMessageToParty(Player player, String message) {
        List<Player> members = getPartyMembers(player);

        for (Player member : members) {
            member.sendMessage(message);
        }
    }
}