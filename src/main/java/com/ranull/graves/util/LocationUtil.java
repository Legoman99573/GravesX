package com.ranull.graves.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;

/**
 * Utility class for handling location-related operations.
 */
public final class LocationUtil {

    /**
     * Rounds the given location's coordinates to the nearest whole numbers.
     *
     * @param location The location to be rounded.
     * @return A new location with rounded coordinates.
     */
    public static Location roundLocation(Location location) {
        return new Location(location.getWorld(), Math.round(location.getBlockX()), Math.round(location.getY()),
                Math.round(location.getBlockZ()));
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
}