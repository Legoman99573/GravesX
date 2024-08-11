package com.ranull.graves.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents serialized location data including world UUID, coordinates, and orientation.
 */
public class LocationData implements Serializable {
    /**
     * The unique identifier for the entity.
     * <p>
     * This {@link UUID} uniquely identifies the entity within the application.
     * </p>
     */
    private final UUID uuid;

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
        uuid = location.getWorld() != null ? location.getWorld().getUID() : null;
        yaw = location.getYaw();
        pitch = location.getPitch();
        x = location.getX();
        y = location.getY();
        z = location.getZ();
    }

    /**
     * Converts the serialized data back into a Location object.
     *
     * @return The deserialized Location, or null if the world UUID is not available.
     */
    public Location getLocation() {
        return uuid != null ? new Location(Bukkit.getWorld(uuid), x, y, z, yaw, pitch) : null;
    }
}