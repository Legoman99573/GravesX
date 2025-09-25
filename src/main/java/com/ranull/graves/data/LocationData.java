package com.ranull.graves.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.NamespacedKey;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents serialized location data including world UUID, coordinates, and orientation.
 */
public class LocationData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The unique identifier for the entity.
     * <p>
     * This {@link UUID} uniquely identifies the entity within the application.
     * </p>
     */
    private final UUID uuid;

    /**
     * Optional world namespaced key for safer cross-platform/world resolution (Paper/Folia).
     * <p>
     * This is used as a secondary lookup if the UUID-based lookup fails.
     * </p>
     */
    private final String worldKey;

    /**
     * Fallback world name (legacy Bukkit).
     */
    private final String worldName;

    /**
     * The yaw (rotation around the vertical axis) of the entity.
     * <p>
     * This {@code float} value represents the yaw of the entity, which controls its horizontal orientation.
     * </p>
     */
    private final float yaw;

    /**
     * The pitch (rotation around the horizontal axis) of the entity.
     * <p>
     * This {@code float} value represents the pitch of the entity, which controls its vertical orientation.
     * </p>
     */
    private final float pitch;

    /**
     * The x-coordinate of the entity's position.
     * <p>
     * This {@code double} value represents the entity's location on the x-axis in the world.
     * </p>
     */
    private final double x;

    /**
     * The y-coordinate of the entity's position.
     * <p>
     * This {@code double} value represents the entity's location on the y-axis in the world.
     * </p>
     */
    private final double y;

    /**
     * The z-coordinate of the entity's position.
     * <p>
     * This {@code double} value represents the entity's location on the z-axis in the world.
     * </p>
     */
    private final double z;

    /**
     * Constructs a new LocationData instance from a given Location.
     *
     * @param location The location to serialize.
     */
    public LocationData(Location location) {
        World w = location.getWorld();
        this.uuid = (w != null) ? w.getUID() : null;
        this.worldKey = (w == null || safeGetKey(w) == null) ? null : Objects.requireNonNull(safeGetKey(w)).toString();
        this.worldName = (w != null) ? w.getName() : null;
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
    }

    /**
     * Converts the serialized data back into a Location object.
     * <p>
     * On Folia and other Paper-family servers, this attempts resolution by world UUID first, then by
     * namespaced key (if present), and finally by legacy world name. This method only resolves the
     * {@link World} reference and constructs a {@link Location}; it does not schedule any thread/region
     * actions. Ensure you <b>use</b> the returned {@link Location} on the correct region thread.
     * </p>
     *
     * @return The deserialized Location, or null if the world cannot be resolved.
     */
    public Location getLocation() {
        World world = null;

        if (uuid != null) {
            world = Bukkit.getWorld(uuid);
        }

        if (world == null && worldKey != null) {
            try {
                NamespacedKey key = NamespacedKey.fromString(worldKey);
                if (key != null) {
                    world = Bukkit.getWorld(String.valueOf(key));
                }
            } catch (Throwable ignored) {
                // Fallback continues to name-based below
            }
        }

        if (world == null && worldName != null) {
            world = Bukkit.getWorld(worldName);
        }

        return (world != null) ? new Location(world, x, y, z, yaw, pitch) : null;
    }

    /**
     * Safely retrieves the world's namespaced key without hard-crashing on non-Paper servers.
     */
    private static NamespacedKey safeGetKey(World world) {
        try {
            return world.getKey();
        } catch (Throwable ignored) {
            return null;
        }
    }
}