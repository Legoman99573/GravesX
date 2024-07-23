package com.ranull.graves.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents serialized location data including world UUID, coordinates, and orientation.
 */
public class LocationData implements Serializable {
    UUID uuid;
    float yaw;
    float pitch;
    double x;
    double y;
    double z;

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