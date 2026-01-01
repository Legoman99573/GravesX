package com.ranull.graves.data;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;

import java.io.Serial;
import java.util.UUID;

/**
 * Represents data for a hologram entity associated with a grave, including its line number.
 */
public class HologramData extends EntityData {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Line index for this hologram entry.
     */
    private final int line;

    /**
     * Cached chunk coordinates for quick region/chunk checks without depending on live Chunk objects.
     */
    private final int chunkX;

    /**
     * Cached chunk coordinates for quick region/chunk checks without depending on live Chunk objects.
     */
    private final int chunkZ;

    /**
     * Constructs a new HologramData instance.
     *
     * @param location   The location of the hologram.
     * @param uuidEntity The UUID of the hologram entity.
     * @param uuidGrave  The UUID of the associated grave.
     * @param line       The line number of the hologram.
     */
    public HologramData(Location location, UUID uuidEntity, UUID uuidGrave, int line) {
        super(location, uuidEntity, uuidGrave, Type.HOLOGRAM);
        this.line = line;

        // Cache chunk coords at creation time. Location is expected to be non-null in practice,
        // but keep safe defaults.
        if (location != null) {
            this.chunkX = location.getBlockX() >> 4;
            this.chunkZ = location.getBlockZ() >> 4;
        } else {
            this.chunkX = 0;
            this.chunkZ = 0;
        }
    }

    /**
     * Gets the line number of the hologram.
     *
     * @return The line number of the hologram.
     */
    public int getLine() {
        return line;
    }

    /**
     * Gets the chunk X coordinate where this hologram resides.
     *
     * @return chunk X
     */
    public int getChunkX() {
        return chunkX;
    }

    /**
     * Gets the chunk Z coordinate where this hologram resides.
     *
     * @return chunk Z
     */
    public int getChunkZ() {
        return chunkZ;
    }

    /**
     * Returns true if the hologram's chunk is currently loaded.
     * This does not force-load the chunk.
     *
     * @return true if loaded, otherwise false
     */
    public boolean isChunkLoaded() {
        Location loc = getLocation();
        World world = (loc != null) ? loc.getWorld() : null;
        if (world == null) {
            return false;
        }
        return world.isChunkLoaded(getChunkX(), getChunkZ());
    }

    /**
     * Returns the chunk if loaded, otherwise null. Does not load the chunk.
     *
     * @return loaded chunk or null
     */
    public Chunk getLoadedChunkOrNull() {
        Location loc = getLocation();
        World world = (loc != null) ? loc.getWorld() : null;
        if (world == null || !world.isChunkLoaded(getChunkX(), getChunkZ())) {
            return null;
        }
        return world.getChunkAt(getChunkX(), getChunkZ());
    }
}