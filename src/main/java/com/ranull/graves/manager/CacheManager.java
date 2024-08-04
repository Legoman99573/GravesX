package com.ranull.graves.manager;

import com.ranull.graves.data.ChunkData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.type.Graveyard;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CacheManager {
    private final Map<UUID, Grave> graveMap;
    private final Map<String, ChunkData> chunkMap;
    private final Map<UUID, Location> lastLocationMap;
    private final Map<UUID, List<ItemStack>> removedItemStackMap;
    private final Map<String, Graveyard> graveyardMap;
    private final Map<String, Location> rightClickedBlocks = new HashMap<>();

    public CacheManager() {
        this.graveMap = new HashMap<>();
        this.chunkMap = new HashMap<>();
        this.lastLocationMap = new HashMap<>();
        this.removedItemStackMap = new HashMap<>();
        this.graveyardMap = new HashMap<>();
    }

    public Map<UUID, Grave> getGraveMap() {
        return graveMap;
    }

    public Map<String, Graveyard> getGraveyardsMap() {
        return graveyardMap;
    }

    public void addRightClickedBlock(String playerName, Location location) {
        rightClickedBlocks.put(playerName, location);
    }

    public Location getRightClickedBlock(String playerName) {
        return rightClickedBlocks.get(playerName);
    }

    public void removeRightClickedBlock(String playerName, Location location) {
        rightClickedBlocks.remove(playerName, location);
    }

    public boolean hasRightClickedBlock(String playerName) {
        return rightClickedBlocks.containsKey(playerName);
    }

    public Map<String, ChunkData> getChunkMap() {
        return chunkMap;
    }

    public Map<UUID, Location> getLastLocationMap() {
        return lastLocationMap;
    }

    public Map<UUID, List<ItemStack>> getRemovedItemStackMap() {
        return removedItemStackMap;
    }
}