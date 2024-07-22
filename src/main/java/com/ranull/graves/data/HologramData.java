package com.ranull.graves.data;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Represents data for a hologram entity associated with a grave, including its line number.
 */
public class HologramData extends EntityData {
    private final int line;

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
    }

    /**
     * Gets the line number of the hologram.
     *
     * @return The line number of the hologram.
     */
    public int getLine() {
        return line;
    }
}
