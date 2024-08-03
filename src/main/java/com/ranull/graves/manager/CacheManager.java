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

/**
 * The CacheManager class is responsible for managing various caches used in the plugin.
 */
public final class CacheManager {
    private final Map<UUID, Grave> graveMap;
    private final Map<String, ChunkData> chunkMap;
    private final Map<UUID, Location> lastLocationMap;
    private final Map<UUID, List<ItemStack>> removedItemStackMap;
    private final Map<String, Graveyard> graveyardMap;

    /**
     * Initializes a new instance of the CacheManager class.
     */
    public CacheManager() {
        this.graveMap = new HashMap<>();
        this.chunkMap = new HashMap<>();
        this.lastLocationMap = new HashMap<>();
        this.removedItemStackMap = new HashMap<>();
        this.graveyardMap = new HashMap<>();
    }

    /**
     * Gets the map of graves, keyed by their UUIDs.
     *
     * @return A map containing UUIDs and their corresponding graves.
     */
    public Map<UUID, Grave> getGraveMap() {
        return graveMap;
    }

    /**
     * Gets the map of graves, keyed by their UUIDs.
     *
     * @return A map containing UUIDs and their corresponding graves.
     */
    public Map<String, Graveyard> getGraveyardsMap() {
        return graveyardMap;
    }

    /**
     * Gets the map of chunk data, keyed by chunk identifiers.
     *
     * @return A map containing chunk identifiers and their corresponding chunk data.
     */
    public Map<String, ChunkData> getChunkMap() {
        return chunkMap;
    }

    /**
     * Gets the map of last known locations, keyed by player UUIDs.
     *
     * @return A map containing UUIDs and their corresponding last known locations.
     */
    public Map<UUID, Location> getLastLocationMap() {
        return lastLocationMap;
    }

    /**
     * Gets the map of removed item stacks, keyed by player UUIDs.
     *
     * @return A map containing UUIDs and their corresponding lists of removed item stacks.
     */
    public Map<UUID, List<ItemStack>> getRemovedItemStackMap() {
        return removedItemStackMap;
    }
}
