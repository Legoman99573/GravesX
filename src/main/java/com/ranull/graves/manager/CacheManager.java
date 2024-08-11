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
    /**
     * A map of grave UUIDs to their corresponding {@link Grave} objects.
     * <p>
     * This {@link Map} associates each {@link UUID} with a {@link Grave} instance, allowing for quick retrieval
     * of grave information based on its unique identifier.
     * </p>
     */
    private final Map<UUID, Grave> graveMap;

    /**
     * A map of chunk identifiers to their corresponding {@link ChunkData} objects.
     * <p>
     * This {@link Map} associates each chunk identifier (as a {@link String}) with {@link ChunkData}, which holds
     * information about the specific chunk.
     * </p>
     */
    private final Map<String, ChunkData> chunkMap;

    /**
     * A map of entity UUIDs to their last known {@link Location}.
     * <p>
     * This {@link Map} tracks the most recent {@link Location} for each entity identified by its {@link UUID}.
     * </p>
     */
    private final Map<UUID, Location> lastLocationMap;

    /**
     * A map of entity UUIDs to lists of removed {@link ItemStack} objects.
     * <p>
     * This {@link Map} associates each entity's {@link UUID} with a {@link List} of {@link ItemStack} objects
     * that have been removed from the entity.
     * </p>
     */
    private final Map<UUID, List<ItemStack>> removedItemStackMap;

    /**
     * A map of graveyard names to their corresponding {@link Graveyard} objects.
     * <p>
     * This {@link Map} associates each graveyard name (as a {@link String}) with a {@link Graveyard} instance,
     * allowing for easy retrieval of graveyard information.
     * </p>
     */
    private final Map<String, Graveyard> graveyardMap;

    /**
     * A map of block identifiers to their corresponding {@link Location} objects where the block was right-clicked.
     * <p>
     * This {@link Map} tracks the locations of blocks that have been right-clicked, identified by a {@link String}
     * representing the block identifier.
     * </p>
     */
    private final Map<String, Location> rightClickedBlocks = new HashMap<>();

    /**
     * Constructs a new {@link CacheManager} with initialized maps.
     * <p>
     * The constructor initializes all the maps used for caching data related to graves, chunks, locations, items,
     * and graveyards.
     * </p>
     */
    public CacheManager() {
        this.graveMap = new HashMap<>();
        this.chunkMap = new HashMap<>();
        this.lastLocationMap = new HashMap<>();
        this.removedItemStackMap = new HashMap<>();
        this.graveyardMap = new HashMap<>();
    }

    /**
     * Returns the map of grave UUIDs to their corresponding {@link Grave} objects.
     * @return the map of graves
     */
    public Map<UUID, Grave> getGraveMap() {
        return graveMap;
    }

    /**
     * Returns the map of graveyard names to their corresponding {@link Graveyard} objects.
     * @return the map of graveyards
     */
    public Map<String, Graveyard> getGraveyardsMap() {
        return graveyardMap;
    }

    /**
     * Adds a right-clicked block location for a specified player.
     * @param playerName the name of the player
     * @param location the location of the right-clicked block
     */
    public void addRightClickedBlock(String playerName, Location location) {
        rightClickedBlocks.put(playerName, location);
    }

    /**
     * Retrieves the location of the right-clicked block for a specified player.
     * @param playerName the name of the player
     * @return the location of the right-clicked block, or {@code null} if not found
     */
    public Location getRightClickedBlock(String playerName) {
        return rightClickedBlocks.get(playerName);
    }

    /**
     * Removes the right-clicked block location for a specified player.
     * @param playerName the name of the player
     * @param location the location of the right-clicked block
     */
    public void removeRightClickedBlock(String playerName, Location location) {
        rightClickedBlocks.remove(playerName, location);
    }

    /**
     * Checks if a right-clicked block location exists for a specified player.
     * @param playerName the name of the player
     * @return {@code true} if the right-clicked block location exists, {@code false} otherwise
     */
    public boolean hasRightClickedBlock(String playerName) {
        return rightClickedBlocks.containsKey(playerName);
    }

    /**
     * Returns the map of chunk identifiers to their corresponding {@link ChunkData} objects.
     * @return the map of chunk data
     */
    public Map<String, ChunkData> getChunkMap() {
        return chunkMap;
    }

    /**
     * Returns the map of entity UUIDs to their last known {@link Location}.
     * @return the map of last known locations
     */
    public Map<UUID, Location> getLastLocationMap() {
        return lastLocationMap;
    }

    /**
     * Returns the map of entity UUIDs to lists of removed {@link ItemStack} objects.
     * @return the map of removed item stacks
     */
    public Map<UUID, List<ItemStack>> getRemovedItemStackMap() {
        return removedItemStackMap;
    }
}