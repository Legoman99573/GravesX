package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import dev.cwhead.GravesX.manager.SafeLocationManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * Manages location-related operations for graves and safe teleportation.
 */
public final class LocationManager {

    private final Graves plugin;

    public LocationManager(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Convenience accessor for the GravesX safe location manager.
     *
     * @return the active {@link SafeLocationManager} instance from the plugin
     */
    private SafeLocationManager safe() {
        return plugin.getSafeLocationManager();
    }

    /**
     * Stores the last known "solid ground" location for an entity.
     *
     * @param entity   entity to track
     * @param location last known solid location to store
     */
    public void setLastSolidLocation(Entity entity, Location location) {
        safe().setLastSolidLocation(entity, location);
    }

    /**
     * Returns the entity's last known solid location, if still valid.
     *
     * @param entity entity whose last solid location is requested
     * @return the last solid location, or {@code null} if none is stored, or it is no longer valid
     */
    public Location getLastSolidLocation(Entity entity) {
        return safe().getLastSolidLocation(entity);
    }

    /**
     * Removes any stored last-solid location for the given entity.
     *
     * @param entity entity whose cached last-solid location should be removed
     */
    public void removeLastSolidLocation(Entity entity) {
        safe().removeLastSolidLocation(entity);
    }

    /**
     * Resolves a safe teleport location for an entity.
     *
     * @param entity   entity being teleported
     * @param location requested teleport destination
     * @param grave    associated grave (for config scoping)
     * @param plugin   plugin instance (kept for API compatibility; forwarded to the delegate)
     * @return a safe teleport location, or {@code null} if none could be resolved
     */
    @SuppressWarnings("unused")
    public Location getSafeTeleportLocation(Entity entity, Location location, Grave grave, Graves plugin) {
        return safe().getSafeTeleportLocation(entity, location, grave, plugin);
    }

    /**
     * Resolves a safe location for grave placement for a dead entity.
     *
     * @param livingEntity dead entity (player or mob)
     * @param location     original death location
     * @param grave        grave being created
     * @return resolved grave location, or {@code null} if none could be resolved
     */
    public Location getSafeGraveLocation(LivingEntity livingEntity, Location location, Grave grave) {
        return safe().getSafeGraveLocation(livingEntity, location, grave);
    }

    /**
     * Determines whether a location is safe for a player to stand/teleport at.
     *
     * @param location location to test
     * @return {@code true} if the location is safe for a player, otherwise {@code false}
     */
    public boolean isLocationSafePlayer(Location location) {
        return safe().isLocationSafePlayer(location);
    }

    /**
     * Determines whether a location is safe for placing a grave block.
     *
     * @param location location to test
     * @return {@code true} if the location is safe for a grave, otherwise {@code false}
     */
    public boolean isLocationSafeGrave(Location location) {
        return safe().isLocationSafeGrave(location);
    }

    /**
     * Checks whether a persisted grave exists at the given location.
     *
     * @param location location to test
     * @return {@code true} if a grave is present, otherwise {@code false}
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasGrave(Location location) {
        return safe().hasGrave(location);
    }

    /**
     * Determines whether a location is inside the world border (or always true on older versions
     * where borders may not exist).
     *
     * @param location location to test
     * @return {@code true} if the location is inside the border (or border checks are ignored), otherwise {@code false}
     */
    public boolean isInsideBorder(Location location) {
        return safe().isInsideBorder(location);
    }

    /**
     * Determines whether a location is considered void (below min height or above max height).
     *
     * @param location location to test
     * @return {@code true} if void, otherwise {@code false}
     */
    public boolean isVoid(Location location) {
        return safe().isVoid(location);
    }

    /**
     * Returns the minimum build height for the given location's world.
     *
     * @param location location whose world is inspected
     * @return minimum height, or {@code 0} if unavailable
     */
    public int getMinHeight(Location location) {
        return safe().getMinHeight(location);
    }

    /**
     * If a cached grave already exists at the provided location, finds a nearby alternative location.
     *
     * @param livingEntity the entity that owns the grave (not used; retained for API parity)
     * @param location     desired grave location
     * @param grave        the grave being placed (not used; retained for API parity)
     * @return a new safe location if the original is occupied by a cached grave; otherwise the original rounded location;
     *         or {@code null} if no suitable alternative could be found
     */
    public Location getNewLocationIfCachedGraveExists(LivingEntity livingEntity, Location location, Grave grave) {
        String prefix = "[LocationManager] getNewLocationIfCachedGraveExists: ";

        if (location == null) {
            plugin.debugMessage(prefix + "input location is null.", 1);
            return null;
        }

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null || rounded.getWorld() == null) {
            plugin.debugMessage(prefix + "rounded or world is null.", 1);
            return null;
        }

        if (!hasCachedGraveAt(rounded)) {
            plugin.debugMessage(prefix + "no cached grave at " + fmtLoc(rounded) + ", using original.", 1);
            return rounded;
        }

        plugin.debugMessage(prefix + "cached grave exists at " + fmtLoc(rounded) + ", searching nearby.", 1);

        World world = rounded.getWorld();
        int bx = rounded.getBlockX();
        int by = rounded.getBlockY();
        int bz = rounded.getBlockZ();

        int[] dyOrder = new int[]{0, 1, -1};

        for (int dy : dyOrder) {
            int y = by + dy;

            Location north = new Location(world, bx, y, bz - 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(north) && !hasCachedGraveAt(north)) return LocationUtil.roundLocation(north);

            Location south = new Location(world, bx, y, bz + 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(south) && !hasCachedGraveAt(south)) return LocationUtil.roundLocation(south);

            Location west = new Location(world, bx - 1, y, bz, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(west) && !hasCachedGraveAt(west)) return LocationUtil.roundLocation(west);

            Location east = new Location(world, bx + 1, y, bz, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(east) && !hasCachedGraveAt(east)) return LocationUtil.roundLocation(east);

            Location nw = new Location(world, bx - 1, y, bz - 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(nw) && !hasCachedGraveAt(nw)) return LocationUtil.roundLocation(nw);

            Location ne = new Location(world, bx + 1, y, bz - 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(ne) && !hasCachedGraveAt(ne)) return LocationUtil.roundLocation(ne);

            Location sw = new Location(world, bx - 1, y, bz + 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(sw) && !hasCachedGraveAt(sw)) return LocationUtil.roundLocation(sw);

            Location se = new Location(world, bx + 1, y, bz + 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(se) && !hasCachedGraveAt(se)) return LocationUtil.roundLocation(se);
        }

        plugin.debugMessage(prefix + "no suitable nearby location found; returning null.", 1);
        return null;
    }

    /**
     * Checks whether a cached grave (from {@code CacheManager#getGraveMap()}) exists at the given block coordinates.
     *
     * @param location location to test
     * @return {@code true} if a cached grave exists at that exact block position, otherwise {@code false}
     */
    public boolean hasCachedGraveAt(Location location) {
        if (location == null) return false;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null || rounded.getWorld() == null) return false;

        final int x = rounded.getBlockX();
        final int y = rounded.getBlockY();
        final int z = rounded.getBlockZ();

        var graveMap = plugin.getCacheManager().getGraveMap();
        if (graveMap == null || graveMap.isEmpty()) return false;

        for (Grave cached : graveMap.values()) {
            if (cached == null) continue;

            Location gl = cached.getLocationDeath();
            if (gl == null) continue;

            Location gr = LocationUtil.roundLocation(gl);
            if (gr == null || gr.getWorld() == null) continue;

            if (!gr.getWorld().equals(rounded.getWorld())) continue;

            if (gr.getBlockX() == x && gr.getBlockY() == y && gr.getBlockZ() == z) {
                return true;
            }
        }

        return false;
    }

    /**
     * Combined safety check used by cached-grave collision resolution:
     *
     * @param location candidate location
     * @return {@code true} if safe for both grave and player and does not already contain a grave, otherwise {@code false}
     */
    private boolean isLocationSafeGraveAndPlayer(Location location) {
        if (location == null) return false;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null || rounded.getWorld() == null) return false;

        if (isVoid(rounded) || !isInsideBorder(rounded)) return false;

        return !hasGrave(rounded) && isLocationSafeGrave(rounded) && isLocationSafePlayer(rounded);
    }

    /**
     * Formats a location for debug output using world name and block coordinates.
     *
     * @param loc location to format
     * @return a compact debug string, or {@code "null"} if the location or world is null
     */
    private String fmtLoc(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return loc.getWorld().getName()
                + " x=" + loc.getBlockX()
                + " y=" + loc.getBlockY()
                + " z=" + loc.getBlockZ();
    }
}