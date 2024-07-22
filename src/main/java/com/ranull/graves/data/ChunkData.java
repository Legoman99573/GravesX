package com.ranull.graves.data;

import org.bukkit.Location;
import org.bukkit.World;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents data for a specific chunk in the game world, including block and entity data.
 */
public class ChunkData implements Serializable {
    private final World world;
    private final int x;
    private final int z;
    private final Map<Location, BlockData> blockDataMap;
    private final Map<UUID, EntityData> entityDataMap;

    /**
     * Constructs a new ChunkData instance based on a location.
     *
     * @param location The location within the chunk.
     */
    public ChunkData(Location location) {
        this.world = location.getWorld();
        this.x = location.getBlockX() >> 4;
        this.z = location.getBlockZ() >> 4;
        this.blockDataMap = new HashMap<>();
        this.entityDataMap = new HashMap<>();
    }

    /**
     * Gets the world of the chunk.
     *
     * @return The world of the chunk.
     */
    public World getWorld() {
        return world;
    }

    /**
     * Gets the x-coordinate of the chunk.
     *
     * @return The x-coordinate of the chunk.
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the z-coordinate of the chunk.
     *
     * @return The z-coordinate of the chunk.
     */
    public int getZ() {
        return z;
    }

    /**
     * Checks if the chunk has any block or entity data.
     *
     * @return True if the chunk has data, false otherwise.
     */
    public boolean hasData() {
        return !blockDataMap.isEmpty() || !entityDataMap.isEmpty();
    }

    /**
     * Checks if the chunk is currently loaded.
     *
     * @return True if the chunk is loaded, false otherwise.
     */
    public boolean isLoaded() {
        return world != null && world.isChunkLoaded(x, z);
    }

    /**
     * Gets the location of the chunk.
     *
     * @return The location of the chunk.
     */
    public Location getLocation() {
        return new Location(world, x << 4, 0, z << 4);
    }

    /**
     * Gets the map of block data within the chunk.
     *
     * @return The map of block data.
     */
    public Map<Location, BlockData> getBlockDataMap() {
        return blockDataMap;
    }

    /**
     * Adds block data to the chunk.
     *
     * @param blockData The block data to add.
     */
    public void addBlockData(BlockData blockData) {
        blockDataMap.put(blockData.getLocation(), blockData);
    }

    /**
     * Removes block data from the chunk based on the location.
     *
     * @param location The location of the block data to remove.
     */
    public void removeBlockData(Location location) {
        blockDataMap.remove(location);
    }

    /**
     * Gets the map of entity data within the chunk.
     *
     * @return The map of entity data.
     */
    public Map<UUID, EntityData> getEntityDataMap() {
        return entityDataMap;
    }

    /**
     * Adds entity data to the chunk.
     *
     * @param entityData The entity data to add.
     */
    public void addEntityData(EntityData entityData) {
        entityDataMap.put(entityData.getUUIDEntity(), entityData);
    }

    /**
     * Removes entity data from the chunk based on the entity's UUID.
     *
     * @param entityData The entity data to remove.
     */
    public void removeEntityData(EntityData entityData) {
        entityDataMap.remove(entityData.getUUIDEntity());
    }
}