package dev.cwhead.GravesX.api.world;

import com.ranull.graves.Graves;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * World/location helper API.
 */
public final class LocationAPI {
    private final Graves plugin;

    public LocationAPI(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Rounds the given location's coordinates to the nearest whole numbers.
     *
     * @param location The location to be rounded.
     * @return A new location with rounded coordinates.
     */
    public Location roundLocation(@NotNull Location location) {
        return LocationUtil.roundLocation(location);
    }

    /**
     * Converts a Location object to a string representation.
     *
     * @param location The location to be converted.
     * @return A string representation of the location in the format "world|x|y|z".
     */
    public String locationToString(@NotNull Location location) {
        return LocationUtil.locationToString(location);
    }

    /**
     * Converts a chunk's location to a string representation.
     *
     * @param location The location within the chunk.
     * @return A string representation of the chunk in the format "world|chunkX|chunkZ".
     */
    public String chunkToString(@NotNull Location location) {
        return LocationUtil.chunkToString(location);
    }

    /**
     * Converts a chunk string representation back to a Location object.
     *
     * @param string The string representation of the chunk in the format "world|chunkX|chunkZ".
     * @return A Location object representing the chunk.
     */
    public Location chunkStringToLocation(@NotNull String string) {
        return LocationUtil.chunkStringToLocation(string);
    }

    /**
     * Converts a string representation of a location back to a Location object.
     *
     * @param string The string representation of the location in the format "world|x|y|z".
     * @return A Location object.
     */
    public Location stringToLocation(@NotNull String string) {
        return LocationUtil.stringToLocation(string);
    }

    /**
     * Finds the closest location to a given base location from a list of locations.
     *
     * @param locationBase The base location to compare against.
     * @param locationList The list of locations to search through.
     * @return The closest location to the base location, or null if the list is empty.
     */
    public Location getClosestLocation(@NotNull Location locationBase, @NotNull List<Location> locationList) {
        return LocationUtil.getClosestLocation(locationBase, locationList);
    }

    /**
     * Simplifies a given BlockFace to one of the four cardinal directions (NORTH, EAST, SOUTH, WEST).
     *
     * @param face The BlockFace to simplify.
     * @return The simplified BlockFace.
     */
    public BlockFace simplifyBlockFace(@NotNull BlockFace face) {
        return BlockFaceUtil.getSimpleBlockFace(face);
    }

    /**
     * Retrieves the Rotation corresponding to a given BlockFace.
     *
     * @param face The BlockFace for which to retrieve the rotation.
     * @return The corresponding Rotation for the specified BlockFace.
     */
    public Rotation getRotationFromBlockFace(@NotNull BlockFace face) {
        return BlockFaceUtil.getBlockFaceRotation(face);
    }
}