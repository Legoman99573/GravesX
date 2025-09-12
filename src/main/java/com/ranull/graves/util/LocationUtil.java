package com.ranull.graves.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Utility class for handling location-related operations.
 */
public final class LocationUtil {

    /**
     * Rounds the given location's coordinates to the nearest whole numbers.
     *
     * @param location The location to be rounded. May be null.
     * @return A new location with rounded coordinates, or null if the input is null.
     */
    @Nullable
    public static Location roundLocation(@Nullable Location location) {
        if (location == null) return null;
        World w = location.getWorld();
        if (w == null) return null;

        double x = Math.round(location.getX());
        double y = Math.round(location.getY());
        double z = Math.round(location.getZ());

        return new Location(w, x, y, z);
    }

    /**
     * Converts a Location object to a string representation.
     *
     * @param location The location to be converted.
     * @return A string representation of the location in the format "world|x|y|z".
     */
    public static String locationToString(Location location) {
        return location.getWorld() != null ? location.getWorld().getName() + "|" + location.getBlockX()
                + "|" + location.getBlockY() + "|" + location.getBlockZ() : null;
    }

    /**
     * Converts a chunk's location to a string representation.
     *
     * @param location The location within the chunk.
     * @return A string representation of the chunk in the format "world|chunkX|chunkZ".
     */
    public static String chunkToString(Location location) {
        return location.getWorld() != null ? location.getWorld().getName() + "|" + (location.getBlockX() >> 4)
                + "|" + (location.getBlockZ() >> 4) : null;
    }

    /**
     * Converts a chunk string representation back to a Location object.
     *
     * @param string The string representation of the chunk in the format "world|chunkX|chunkZ".
     * @return A Location object representing the chunk.
     */
    public static Location chunkStringToLocation(String string) {
        String[] strings = string.split("\\|");

        return new Location(Bukkit.getServer().getWorld(strings[0]), Integer.parseInt(strings[1]) << 4,
                0, Integer.parseInt(strings[2]) << 4);
    }

    /**
     * Converts a string representation of a location back to a Location object.
     *
     * @param string The string representation of the location in the format "world|x|y|z".
     * @return A Location object.
     */
    public static Location stringToLocation(String string) {
        String[] strings = string.split("\\|");

        return new Location(Bukkit.getServer().getWorld(strings[0]), Integer.parseInt(strings[1]),
                Integer.parseInt(strings[2]), Integer.parseInt(strings[3]));
    }

    /**
     * Finds the closest location to a given base location from a list of locations.
     *
     * @param locationBase The base location to compare against.
     * @param locationList The list of locations to search through.
     * @return The closest location to the base location, or null if the list is empty.
     */
    public static Location getClosestLocation(Location locationBase, List<Location> locationList) {
        Location locationClosest = null;

        for (Location location : locationList) {
            if (locationClosest == null) {
                locationClosest = location;
            } else if (location.distanceSquared(locationBase) < locationClosest.distanceSquared(locationBase)) {
                locationClosest = location;
            }
        }

        return locationClosest;
    }

    /**
     * Deserializes a location from a string format.
     * The expected format is "world,x,y,z,pitch,yaw".
     *
     * @param serializedLocation The serialized location string.
     * @return The deserialized Location object, or null if the format is invalid.
     */
    public static Location deserializeLocation(String serializedLocation) {
        if (serializedLocation == null || serializedLocation.isEmpty()) {
            return null;
        }

        String[] parts = serializedLocation.split(",");
        if (parts.length != 6) {
            return null;
        }

        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                return null;
            }

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float pitch = Float.parseFloat(parts[4]);
            float yaw = Float.parseFloat(parts[5]);

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null; // Invalid number format
        }
    }
}