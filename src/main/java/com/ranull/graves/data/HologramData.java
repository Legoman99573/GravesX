package com.ranull.graves.data;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents data for a hologram entity associated with a grave, including its line number.
 */
public class HologramData extends EntityData {

    public enum Backend {
        ARMOR_STAND,
        TEXT_DISPLAY
    }

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
     * Backend used to create this hologram line entity.
     *
     * <p>Old serialized entries (pre-backend field) will deserialize with {@code null},
     * and will be defaulted to {@link Backend#ARMOR_STAND} in {@link #readObject(ObjectInputStream)}.</p>
     */
    private Backend backend;

    /**
     * Constructs a new HologramData instance.
     * Defaults backend to ARMOR_STAND for backwards compatibility.
     *
     * @param location   The location of the hologram.
     * @param uuidEntity The UUID of the hologram entity.
     * @param uuidGrave  The UUID of the associated grave.
     * @param line       The line number of the hologram.
     */
    public HologramData(Location location, UUID uuidEntity, UUID uuidGrave, int line) {
        this(location, uuidEntity, uuidGrave, line, Backend.ARMOR_STAND);
    }

    /**
     * Constructs a new HologramData instance with an explicit backend.
     *
     * @param location   The location of the hologram.
     * @param uuidEntity The UUID of the hologram entity.
     * @param uuidGrave  The UUID of the associated grave.
     * @param line       The line number of the hologram.
     * @param backend    The backend that created the hologram entity.
     */
    public HologramData(Location location, UUID uuidEntity, UUID uuidGrave, int line, Backend backend) {
        super(location, uuidEntity, uuidGrave, Type.HOLOGRAM);
        this.line = line;
        this.backend = (backend != null) ? backend : Backend.ARMOR_STAND;

        if (location != null) {
            this.chunkX = location.getBlockX() >> 4;
            this.chunkZ = location.getBlockZ() >> 4;
        } else {
            this.chunkX = 0;
            this.chunkZ = 0;
        }
    }

    /**
     * Ensures old serialized objects default to ARMOR_STAND.
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.backend == null) {
            this.backend = Backend.ARMOR_STAND;
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
     * Gets the backend used for this hologram entry.
     *
     * @return backend
     */
    public Backend getBackend() {
        return backend;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HologramData other)) return false;

        return this.getUUIDGrave() != null && this.getUUIDGrave().equals(other.getUUIDGrave()) && this.line == other.line && this.backend == other.backend;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUUIDGrave(), line, backend);
    }
}