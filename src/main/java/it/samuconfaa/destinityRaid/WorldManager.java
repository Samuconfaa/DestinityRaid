package it.samuconfaa.destinityRaid;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldManager {
    private Map<String, UUID> occupiedWorlds = new HashMap<>();
    private Map<UUID, Long> raidStartTimes = new HashMap<>();

    public boolean isWorldOccupied(String worldKey) {
        return occupiedWorlds.containsKey(worldKey);
    }

    public void occupyWorld(String worldKey, Player player) {
        occupiedWorlds.put(worldKey, player.getUniqueId());
        raidStartTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void freeWorld(String worldKey) {
        UUID playerUUID = occupiedWorlds.get(worldKey);
        occupiedWorlds.remove(worldKey);
        if (playerUUID != null) {
            raidStartTimes.remove(playerUUID);
        }
    }

    public UUID getWorldOccupant(String worldKey) {
        return occupiedWorlds.get(worldKey);
    }

    public String getOccupiedWorldByPlayer(UUID playerUUID) {
        for (Map.Entry<String, UUID> entry : occupiedWorlds.entrySet()) {
            if (entry.getValue().equals(playerUUID)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public long getRaidStartTime(UUID playerUUID) {
        return raidStartTimes.getOrDefault(playerUUID, 0L);
    }
}