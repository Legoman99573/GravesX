package com.ranull.graves.manager;

import com.ranull.graves.data.ChunkData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CacheManager {
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
     * A map of block identifiers to their corresponding {@link Location} objects where the block was right-clicked.
     * <p>
     * This {@link Map} tracks the locations of blocks that have been right-clicked, identified by a {@link String}
     * representing the block identifier.
     * </p>
     */
    private final Map<String, Location> rightClickedBlocks = new HashMap<>();

    /**
     * A map of grave UUIDs to the UUID of the player currently viewing that grave.
     * <p>
     * Used to prevent multiple players from accessing the same grave at the same time.
     * If a grave UUID is present in this map, the grave is considered "in use" / "locked".
     * </p>
     */
    private final Map<UUID, UUID> graveViewerMap;

    /**
     * Constructs a new {@link CacheManager} with initialized maps.
     * <p>
     * The constructor initializes all the maps used for caching data related to graves, chunks, locations, and items
     * </p>
     */
    public CacheManager() {
        this.graveMap = new HashMap<>();
        this.chunkMap = new HashMap<>();
        this.lastLocationMap = new HashMap<>();
        this.removedItemStackMap = new HashMap<>();
        this.graveViewerMap = new HashMap<>();
    }

    /**
     * Returns the map of grave UUIDs to their corresponding {@link Grave} objects.
     * @return the map of graves
     */
    public Map<UUID, Grave> getGraveMap() {
        return graveMap;
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

    /**
     * Marks a grave as currently being viewed by the given player.
     * <p>
     * If the grave is already being viewed by someone else, this will NOT overwrite the current viewer.
     * Use {@link #canAccessGrave(UUID, UUID)} / {@link #isGraveBeingViewed(UUID)} to check first.
     * </p>
     *
     * @param graveUUID  the grave UUID
     * @param viewerUUID the player's UUID
     * @return {@code true} if the viewer was set (lock acquired), {@code false} if someone else already holds it
     */
    public boolean startViewingGrave(UUID graveUUID, UUID viewerUUID) {
        UUID current = graveViewerMap.get(graveUUID);
        if (current == null || current.equals(viewerUUID)) {
            graveViewerMap.put(graveUUID, viewerUUID);
            return true;
        }
        return false;
    }

    /**
     * Clears the viewer lock for a grave if the given player is the current viewer.
     *
     * @param graveUUID  the grave UUID
     * @param viewerUUID the player's UUID
     */
    public void stopViewingGrave(UUID graveUUID, UUID viewerUUID) {
        UUID current = graveViewerMap.get(graveUUID);
        if (current != null && current.equals(viewerUUID)) {
            graveViewerMap.remove(graveUUID);
        }
    }

    /**
     * Force-clears the viewer lock for a grave (regardless of who is viewing).
     * Useful for cleanup if a viewer disconnects unexpectedly.
     *
     * @param graveUUID the grave UUID
     */
    public void clearGraveViewer(UUID graveUUID) {
        graveViewerMap.remove(graveUUID);
    }

    /**
     * Clears any grave-viewer locks held by the specified player.
     * Useful to call on PlayerQuitEvent.
     *
     * @param viewerUUID the player's UUID
     */
    public void clearAllGraveViewersFor(UUID viewerUUID) {
        graveViewerMap.entrySet().removeIf(e -> viewerUUID.equals(e.getValue()));
    }

    /**
     * Checks whether a grave is currently being viewed by someone.
     *
     * @param graveUUID the grave UUID
     * @return {@code true} if the grave is being viewed, otherwise {@code false}
     */
    public boolean isGraveBeingViewed(UUID graveUUID) {
        return graveViewerMap.containsKey(graveUUID);
    }

    /**
     * Gets the UUID of the player currently viewing a grave, or {@code null} if none.
     *
     * @param graveUUID the grave UUID
     * @return the viewer UUID, or {@code null}
     */
    public UUID getGraveViewer(UUID graveUUID) {
        return graveViewerMap.get(graveUUID);
    }

    /**
     * Checks if a player can access a grave right now.
     * <p>
     * Access is allowed if the grave is not being viewed, or if it is being viewed by the same player.
     * </p>
     *
     * @param graveUUID  the grave UUID
     * @param viewerUUID the player's UUID
     * @return {@code true} if access is allowed, otherwise {@code false}
     */
    public boolean canAccessGrave(UUID graveUUID, UUID viewerUUID) {
        UUID current = graveViewerMap.get(graveUUID);
        return (current == null || current.equals(viewerUUID));
    }

    /**
     * Retrieves a {@link Grave} from the cache by its UUID
     * @param graveUUID the UUID of the grave to retrieve
     * @return the {@link Grave} associated with the UUID provided, or {@code null} if not present
     */
    public Grave getGrave(UUID graveUUID) {
        return graveMap.get(graveUUID);
    }

    /**
     * Convenience method to retrieve a {@link Grave} from the cache based on a {@link Block}.
     * <p>
     * Internally delegates to {@link #getGrave(Location)} using the block's location.
     * </p>
     *
     * @param block the block to check
     * @return the matching {@link Grave}, or {@code null} if none is found
     */
    public Grave getGrave(Block block) {
        for (Grave grave : graveMap.values()) {
            if (grave == null) {
                continue;
            }

            Location graveLocation = grave.getLocationDeath();
            if (graveLocation == null || graveLocation.getWorld() == null) {
                continue;
            }

            if (graveLocation.getWorld().equals(block.getWorld()) && graveLocation.getBlockX() == block.getX() && graveLocation.getBlockY() == block.getY() && graveLocation.getBlockZ() == block.getZ()) {
                return grave;
            }
        }

        return null;
    }

    /**
     * Returns the oldest grave for a given player.
     * @param playerUUID The UUID of the player whose graves to consider.
     * @return The oldest grave for the specified player.
     */
    public Grave getOldestGrave(UUID playerUUID) {
        long oldestTime = Long.MAX_VALUE;
        Grave oldestGrave = null;

        for (Grave cur : graveMap.values()) {
            if (cur.getOwnerUUID().equals(playerUUID)) {
                long curTime = cur.getTimeCreation();
                if (curTime < oldestTime) {
                    oldestTime = curTime;
                    oldestGrave = cur;
                }
            }
        }

        return oldestGrave;
    }

    /**
     * Retrieves the {@link Grave} whose block is located at the given {@link Location}.
     * <p>
     * This compares block coordinates (world + block X/Y/Z) instead of raw double coordinates
     * to ensure it matches the actual block the grave is placed on.
     * </p>
     *
     * @param location the location of the grave block
     * @return the matching {@link Grave}, or {@code null} if none is found
     */
    public Grave getGrave(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        for (Grave grave : graveMap.values()) {
            Location graveLocation = grave.getLocationDeath();
            if (graveLocation == null || graveLocation.getWorld() == null) {
                continue;
            }

            if (graveLocation.getWorld().equals(location.getWorld())
                    && graveLocation.getBlockX() == location.getBlockX()
                    && graveLocation.getBlockY() == location.getBlockY()
                    && graveLocation.getBlockZ() == location.getBlockZ()) {
                return grave;
            }
        }

        return null;
    }
}