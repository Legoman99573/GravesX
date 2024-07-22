package com.ranull.graves.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents serialized location data including world UUID, coordinates, and orientation.
 */
public class LocationData implements Serializable {
    private UUID uuid;
    private float yaw;
    private float pitch;
    private double x;
    private double y;
    private double z;

    /**
     * Constructs a new LocationData instance from a given Location.
     *
     * @param location The location to serialize.
     */
    public LocationData(Location location) {
        this.uuid = location.getWorld() != null ? location.getWorld().getUID() : null;
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
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