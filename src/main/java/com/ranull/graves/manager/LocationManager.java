package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Manages location-related operations for graves and safe teleportation.
 */
public final class LocationManager {

    private final Graves plugin;

    public LocationManager(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Stores the last solid location for an entity in the cache map.
     *
     * @param entity   entity to track
     * @param location last known solid location
     */
    public void setLastSolidLocation(Entity entity, Location location) {
        if (entity == null || location == null) return;
        plugin.getCacheManager().getLastLocationMap().put(entity.getUniqueId(), location.clone());
    }

    /**
     * Gets the last solid location of an entity if still valid.
     *
     * @param entity entity whose last solid location is requested
     * @return valid last solid location, or {@code null} if none
     */
    public Location getLastSolidLocation(Entity entity) {
        if (entity == null) return null;

        Location location = plugin.getCacheManager().getLastLocationMap().get(entity.getUniqueId());
        if (location == null || location.getWorld() == null) {
            return null;
        } else {
            entity.getWorld();
        }

        if (!location.getWorld().equals(entity.getWorld())) return null;

        Block below = location.getBlock().getRelative(BlockFace.DOWN);
        if (!below.getType().isSolid()) return null;

        return location.clone();
    }

    /**
     * Removes the last solid location for an entity from the cache map.
     *
     * @param entity entity whose cached location should be removed
     */
    public void removeLastSolidLocation(Entity entity) {
        if (entity == null) return;
        plugin.getCacheManager().getLastLocationMap().remove(entity.getUniqueId());
    }

    /**
     * Resolves a safe teleport location for an entity.
     *
     * @param entity   entity being teleported
     * @param location requested teleport location
     * @param grave    associated grave (for config scoping)
     * @param plugin   plugin instance (unused, kept for API compatibility)
     * @return safe teleport location, or {@code null} if none found
     */
    @SuppressWarnings("unused")
    public Location getSafeTeleportLocation(Entity entity, Location location, Grave grave, Graves plugin) {
        if (location == null || entity == null) return null;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null || rounded.getWorld() == null) return null;

        String prefix = "[LocationManager] getSafeTeleportLocation: ";

        if (plugin.getConfigManager().getConfigSection("teleport.unsafe", grave).getBoolean("teleport.unsafe")) {
            plugin.debugMessage(prefix + "teleport.unsafe=true, using requested location as-is.", 1);
            return rounded;
        }

        if (isLocationSafePlayer(rounded)) {
            plugin.debugMessage(prefix + "requested location already safe for player, using as-is.", 1);
            return rounded;
        }

        if (plugin.getConfigManager().getConfigSection("teleport.top", grave).getBoolean("teleport.top")) {

            Location top = resolveTeleportTop(rounded);
            if (top != null) {
                plugin.debugMessage(prefix + "resolved top teleport location to " + fmtLoc(top) + ".", 1);
                if (entity instanceof Player player) {
                    plugin.getEntityManager().sendMessage("message.teleport-top", player, top, grave);
                }
                return top;
            }

            plugin.debugMessage(prefix + "no safe top teleport location found.", 1);
        }

        plugin.debugMessage(prefix + "no safe teleport location could be resolved, returning null.", 1);
        return null;
    }

    /**
     * Finds a safe teleport "top" location by scanning downward from world max height
     * at the same X/Z as the base location.
     *
     * @param base base location (X/Z used; yaw/pitch preserved)
     * @return safe teleport location or {@code null} if none found
     */
    private Location resolveTeleportTop(Location base) {
        if (base == null || base.getWorld() == null) return null;

        World world = base.getWorld();
        int x = base.getBlockX();
        int z = base.getBlockZ();

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        for (int y = maxY; y >= minY; y--) {
            Location candidate = new Location(world, x + 0.5, y, z + 0.5, base.getYaw(), base.getPitch());
            if (isLocationSafePlayer(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Resolves a safe location to place a grave for a dead entity.
     *
     * @param livingEntity dead entity (player or mob)
     * @param location     original death location
     * @param grave        grave being created
     * @return resolved grave location, or {@code null} if void handling also fails
     */
    public Location getSafeGraveLocation(LivingEntity livingEntity, Location location, Grave grave) {
        String prefix = "[LocationManager] getSafeGraveLocation: ";

        if (location == null) {
            plugin.debugMessage(prefix + "input location is null, returning null.", 1);
            return null;
        }

        Location original = location.clone();
        Location rounded = LocationUtil.roundLocation(location);

        if (rounded == null) {
            plugin.debugMessage(prefix + "rounded location is null, returning null.", 1);
            return null;
        }

        if (rounded.getWorld() == null) {
            plugin.debugMessage(prefix + "world is null for location " + original.toVector() + ", falling back to void resolution.", 1);
            return resolveVoidLocation(original, grave);
        }

        location = rounded;
        World world = location.getWorld();
        Block block = location.getBlock();

        plugin.debugMessage(prefix + "start at " + fmtLoc(location) + " block=" + block.getType(), 1);

        if (isVoid(location) || !isInsideBorder(location)) {
            plugin.debugMessage(prefix + "location is void or outside border, resolving via void resolver.", 1);
            return resolveVoidLocation(location, grave);
        }

        boolean isAir = MaterialUtil.isAir(block.getType());
        boolean isWater = MaterialUtil.isWater(block.getType());
        boolean isLava = MaterialUtil.isLava(block.getType());

        if (!isAir && !isWater && !isLava && !hasGrave(location) && isLocationSafeGrave(location)) {
            plugin.debugMessage(prefix + "location already safe, non-fluid, and empty; using as-is.", 1);
            return location;
        }

        if (isLava) {
            plugin.debugMessage(prefix + "death in lava, delegating to lava resolver.", 1);
            Location lavaLoc = resolveLavaLocation(location, livingEntity, grave);
            if (lavaLoc != null) {
                plugin.debugMessage(prefix + "lava resolver returned " + fmtLoc(lavaLoc) + ", using that.", 1);
                return lavaLoc;
            }

            plugin.debugMessage(prefix + "lava resolver returned null, falling back to void resolver.", 1);
            return resolveVoidLocation(location, grave);
        }

        boolean useGround = plugin.getConfigManager().getConfigSection("placement.ground", grave).getBoolean("placement.ground");

        Location graveLocation = null;

        if (isWater) {
            plugin.debugMessage(prefix + "death in water; placement.ground=" + useGround + ".", 1);
            graveLocation = resolveWaterLocation(location, livingEntity, grave, useGround);
        } else {
            plugin.debugMessage(prefix + "block is " + (isAir ? "air" : "solid/non-fluid") + ", resolving roof first.", 1);

            graveLocation = resolveRoofLocation(location, grave);
            if (graveLocation == null && useGround) {
                plugin.debugMessage(prefix + "roof resolution failed, falling back to ground.", 1);
                graveLocation = resolveGroundLocation(location, grave);
            }
        }

        if (graveLocation != null) {
            plugin.debugMessage(prefix + "resolved grave location to " + fmtLoc(graveLocation) + ".", 1);
            return graveLocation;
        }

        plugin.debugMessage(prefix + "ground/roof/water resolution returned null, delegating to void resolver.", 1);
        Location voidFallback = resolveVoidLocation(location, grave);
        if (voidFallback != null) {
            plugin.debugMessage(prefix + "void resolver returned " + fmtLoc(voidFallback) + ".", 1);
        } else {
            plugin.debugMessage(prefix + "void resolver also returned null; final result is null.", 1);
        }
        return voidFallback;
    }

    /**
     * Attempts to resolve a safe grave location based on the last solid block
     * the player stood on, used by lava-smart and water-smart.
     *
     * @param livingEntity entity that died (only players are supported)
     * @param grave        associated grave
     * @return safe location near the last solid block, or {@code null} if unavailable/unsafe
     */
    private Location resolveSmartFromLastSolid(LivingEntity livingEntity, Grave grave) {
        if (!(livingEntity instanceof Player player)) {
            return null;
        }

        Location lastSolid = getLastSolidLocation(player);
        if (lastSolid == null || lastSolid.getWorld() == null) {
            return null;
        }

        Location base = LocationUtil.roundLocation(lastSolid);
        if (base == null || base.getWorld() == null) {
            return null;
        }

        if (isVoid(base) || !isInsideBorder(base)) {
            return null;
        }

        if (!hasGrave(base) && isLocationSafeGrave(base)) {
            return base;
        }

        Location roof = resolveRoofLocation(base, grave);
        if (roof != null && !hasGrave(roof) && isLocationSafeGrave(roof)) {
            return roof;
        }

        boolean useGround = plugin.getConfigManager().getConfigSection("placement.ground", grave).getBoolean("placement.ground");

        if (useGround) {
            Location ground = resolveGroundLocation(base, grave);
            if (ground != null && !hasGrave(ground) && isLocationSafeGrave(ground)) {
                return ground;
            }
        }

        return null;
    }

    /**
     * Resolves a "ground" location by searching downward first, then upward,
     * for a safe grave position (safe for grave and no grave present).
     */
    private Location resolveGroundLocation(Location base, Grave grave) {
        if (base == null || base.getWorld() == null) return null;

        Location down = searchDownForSafeGrave(base, grave, base.getBlockY());
        if (down != null) return down;

        return searchUpForSafeGrave(base, grave, base.getBlockY() + 1);
    }

    /**
     * Resolves a "roof" location by searching upward first, then downward,
     * for a safe grave position (safe for grave and no grave present).
     */
    private Location resolveRoofLocation(Location base, Grave grave) {
        if (base == null || base.getWorld() == null) return null;

        Location up = searchUpForSafeGrave(base, grave, base.getBlockY() + 1);
        if (up != null) return up;

        return searchDownForSafeGrave(base, grave, base.getBlockY());
    }

    /**
     * Resolves a safe position when the death is in/near water.
     * <ul>
     *   <li>If {@code water-smart} is enabled and the entity is a player, tries last solid location first.</li>
     *   <li>Then always tries roof-like behavior (water-top).</li>
     *   <li>If that fails and {@code useGround} is true, tries bottom of the water column.</li>
     * </ul>
     */
    private Location resolveWaterLocation(Location base, LivingEntity livingEntity, Grave grave, boolean useGround) {
        if (base == null || base.getWorld() == null) return null;

        boolean waterSmart = plugin.getConfigManager().getConfigSection("placement.water-smart", grave).getBoolean("placement.water-smart");

        if (waterSmart) {
            Location smart = resolveSmartFromLastSolid(livingEntity, grave);
            if (smart != null) {
                plugin.debugMessage("[LocationManager] resolveWaterLocation: using water-smart location " + fmtLoc(smart) + ".", 1);
                return smart;
            }
        }

        Location roof = resolveRoofLocation(base, grave);
        if (roof != null) {
            plugin.debugMessage("[LocationManager] resolveWaterLocation: using water-top/roof location " + fmtLoc(roof) + ".", 1);
            return roof;
        }

        if (useGround) {
            Location bottom = findWaterBottom(base);
            if (bottom != null && !hasGrave(bottom) && isLocationSafeGrave(bottom)) {
                plugin.debugMessage("[LocationManager] resolveWaterLocation: using water-bottom location " + fmtLoc(bottom) + ".", 1);
                return bottom;
            }
        }

        plugin.debugMessage("[LocationManager] resolveWaterLocation: no water-smart/top/bottom location found.", 1);
        return null;
    }

    /**
     * Resolves a safe position when the death is in/near lava.
     * <p>
     * Honors:
     * <ul>
     *   <li>{@code placement.lava-smart}: try last solid block the player stood on (players only).</li>
     *   <li>{@code placement.lava-top}: float to the top of the lava column.</li>
     * </ul>
     * </p>
     */
    private Location resolveLavaLocation(Location base, LivingEntity livingEntity, Grave grave) {
        if (base == null || base.getWorld() == null) return null;

        boolean lavaSmart = plugin.getConfigManager().getConfigSection("placement.lava-smart", grave).getBoolean("placement.lava-smart");

        if (lavaSmart) {
            Location smart = resolveSmartFromLastSolid(livingEntity, grave);
            if (smart != null) {
                plugin.debugMessage("[LocationManager] resolveLavaLocation: using lava-smart location " + fmtLoc(smart) + ".", 1);
                return smart;
            }
        }

        boolean lavaTopEnabled = plugin.getConfigManager().getConfigSection("placement.lava-top", grave).getBoolean("placement.lava-top");

        if (!lavaTopEnabled) {
            return null;
        }

        World world = base.getWorld();

        Location liquid = findLiquid(base);
        if (liquid == null || liquid.getWorld() == null) {
            return null;
        }

        Location above = liquid.clone().add(0.0, 1.0, 0.0);
        if (!hasGrave(above) && isLocationSafeGrave(above)) {
            plugin.debugMessage("[LocationManager] resolveLavaLocation: using lava-top location " + fmtLoc(above) + ".", 1);
            return above;
        }

        Location up = searchUpForSafeGrave(above, grave, above.getBlockY());
        if (up != null) {
            plugin.debugMessage("[LocationManager] resolveLavaLocation: lava-top upwards search resolved to " + fmtLoc(up) + ".", 1);
        }
        return up;
    }

    /**
     * Handles void/border placement behavior using the new "column" scanning logic.
     * <ul>
     *     <li>Respects {@code placement.void}.</li>
     *     <li>Scans from world min height upward along the column for a safe grave location.</li>
     *     <li>If none found, attempts final fallback at min height.</li>
     * </ul>
     */
    private Location resolveVoidLocation(Location base, Grave grave) {
        if (base == null || base.getWorld() == null) return null;

        if (!plugin.getConfigManager().getConfigSection("placement.void", grave).getBoolean("placement.void")) {
            plugin.debugMessage("[LocationManager] resolveVoidLocation: placement.void=false, returning null.", 1);
            return null;
        }

        World world = base.getWorld();
        int x = base.getBlockX();
        int z = base.getBlockZ();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        for (int y = minY; y <= maxY; y++) {
            Location candidate = new Location(world, x + 0.5, y, z + 0.5, base.getYaw(), base.getPitch());
            if (!hasGrave(candidate) && isLocationSafeGrave(candidate)) {
                plugin.debugMessage("[LocationManager] resolveVoidLocation: found safe column location at "
                        + fmtLoc(candidate) + ".", 1);
                return candidate;
            }
        }

        Location atMin = new Location(world, x + 0.5, minY, z + 0.5, base.getYaw(), base.getPitch());
        if (!hasGrave(atMin) && isLocationSafeGrave(atMin)) {
            plugin.debugMessage("[LocationManager] resolveVoidLocation: fallback min-height location at "
                    + fmtLoc(atMin) + ".", 1);
            return atMin;
        }

        plugin.debugMessage("[LocationManager] resolveVoidLocation: no safe void-column location found.", 1);
        return null;
    }

    /**
     * Searches downward from the given Y coordinate for a safe grave location
     * at the same X/Z as the base.
     *
     * @param base   base location (X/Z/yaw/pitch used)
     * @param grave  associated grave
     * @param startY initial Y coordinate
     * @return safe grave location or {@code null} if none found
     */
    private Location searchDownForSafeGrave(Location base, Grave grave, int startY) {
        if (base == null || base.getWorld() == null) return null;

        World world = base.getWorld();
        int x = base.getBlockX();
        int z = base.getBlockZ();

        int minY = world.getMinHeight();
        int y = startY;

        while (y >= minY) {
            Location candidate = new Location(world, x + 0.5, y, z + 0.5, base.getYaw(), base.getPitch());
            Block block = candidate.getBlock();

            if (MaterialUtil.isLava(block.getType())) {
                y--;
                continue;
            }

            if (!hasGrave(candidate) && isLocationSafeGrave(candidate)) {
                return candidate;
            }

            y--;
        }

        return null;
    }

    /**
     * Searches upward from the given Y coordinate for a safe grave location
     * at the same X/Z as the base.
     *
     * @param base   base location (X/Z/yaw/pitch used)
     * @param grave  associated grave
     * @param startY initial Y coordinate
     * @return safe grave location or {@code null} if none found
     */
    private Location searchUpForSafeGrave(Location base, Grave grave, int startY) {
        if (base == null || base.getWorld() == null) return null;

        World world = base.getWorld();
        int x = base.getBlockX();
        int z = base.getBlockZ();

        int maxY = world.getMaxHeight() - 1;
        int y = startY;

        while (y <= maxY) {
            Location candidate = new Location(world, x + 0.5, y, z + 0.5, base.getYaw(), base.getPitch());
            Block block = candidate.getBlock();

            if (MaterialUtil.isLava(block.getType())) {
                y++;
                continue;
            }

            if (!hasGrave(candidate) && isLocationSafeGrave(candidate)) {
                return candidate;
            }

            y++;
        }

        return null;
    }

    /**
     * Finds the nearest solid non-air block below the given location and returns its block location.
     *
     * @param loc reference location
     * @return block location of the nearest solid block below, or {@code null} if none
     */
    private Location findNearestGroundBelow(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int y = loc.getBlockY() - 1;
        int minY = world.getMinHeight();

        for (; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (!block.getType().isAir() && block.getType().isSolid()) {
                return block.getLocation();
            }
        }

        return null;
    }

    /**
     * Finds the nearest solid non-air block above the given location and returns its block location.
     *
     * @param loc reference location
     * @return block location of the nearest solid block above, or {@code null} if none
     */
    private Location findNearestBlockAbove(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int y = loc.getBlockY() + 1;
        int maxY = world.getMaxHeight() - 1;

        for (; y <= maxY; y++) {
            Block block = world.getBlockAt(x, y, z);
            if (!block.getType().isAir() && block.getType().isSolid()) {
                return block.getLocation();
            }
        }

        return null;
    }

    /**
     * Finds a "ground" block near the given location by scanning downward first,
     * then upward if nothing is found. Returns the block's location.
     */
    private Location getGround(Location loc) {
        Location down = findNearestGroundBelow(loc);
        if (down != null) return down;
        return findNearestBlockAbove(loc);
    }

    /**
     * Finds the nearest liquid (water/lava) block near the given location, scanning
     * downward first and then upward if needed.
     *
     * @param loc reference location
     * @return liquid block location or {@code null} if none found
     */
    private Location findLiquid(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        for (int y = loc.getBlockY() - 1; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (!type.isAir() && (MaterialUtil.isWater(type) || MaterialUtil.isLava(type))) {
                return block.getLocation();
            }
        }

        for (int y = loc.getBlockY() + 1; y <= maxY; y++) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (!type.isAir() && (MaterialUtil.isWater(type) || MaterialUtil.isLava(type))) {
                return block.getLocation();
            }
        }

        return null;
    }

    /**
     * Finds the bottom-most water block in the water column that intersects the given location.
     * <p>
     * Excludes solid blocks: only continuous water blocks are considered. If a bottom cannot
     * be determined, falls back to the top-most water block in that column.
     *
     * @param loc reference location
     * @return block location of the bottom-most water block, or top-most water block, or {@code null} if no water
     */
    private Location findWaterBottom(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        Location top = null;

        for (int y = loc.getBlockY() - 1; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (!type.isAir() && MaterialUtil.isWater(type)) {
                top = block.getLocation();
                break;
            }
        }

        if (top == null) {
            for (int y = loc.getBlockY() + 1; y <= maxY; y++) {
                Block block = world.getBlockAt(x, y, z);
                Material type = block.getType();
                if (!type.isAir() && MaterialUtil.isWater(type)) {
                    top = block.getLocation();
                    break;
                }
            }
        }

        if (top == null) {
            return null;
        }

        Location bottom = top;
        int startY = top.getBlockY() - 1;

        for (int y = startY; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (MaterialUtil.isWater(block.getType())) {
                bottom = block.getLocation();
            } else {
                break;
            }
        }

        return bottom != null ? bottom : top;
    }

    /**
     * Finds a "void ground" location by scanning upward from world minimum height
     * at the same X/Z as the given location, returning the first solid non-air block.
     *
     * @param loc reference location (X/Z used, Y ignored)
     * @return location of the first solid block above min height, or {@code null} if none
     */
    private Location getVoid(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        for (int y = minY; y <= maxY; y++) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (!type.isAir() && type.isSolid()) {
                return block.getLocation();
            }
        }

        return null;
    }

    /**
     * Checks whether a living entity can build at the specified location,
     * honoring Graves config, protection plugins and ProtectionLib (if present).
     *
     * @param livingEntity   entity attempting to build
     * @param location       target location
     * @param permissionList optional extra permissions
     * @return {@code true} if building is allowed, otherwise {@code false}
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canBuild(LivingEntity livingEntity, Location location, List<String> permissionList) {
        if (livingEntity instanceof Player player) {

            return (!plugin.getConfigManager().getConfigSection("placement.can-build", player, permissionList).getBoolean("placement.can-build")
                    || plugin.getCompatibility().canBuild(player, location, plugin)) && (!plugin.getIntegrationManager().hasProtectionLib()
                    || (!plugin.getConfigManager().getConfigSection("placement.can-build-protectionlib", player, permissionList).getBoolean("placement.can-build-protectionlib")
                    || plugin.getIntegrationManager().getProtectionLib().canBuild(location, player)));
        }

        return true;
    }

    /**
     * Determines whether a location is safe for a player to stand/teleport at.
     *
     * @param location location to test
     * @return {@code true} if safe, {@code false} otherwise
     */
    public boolean isLocationSafePlayer(Location location) {
        if (location == null) return false;
        if (location.getWorld() == null) return false;

        Block block = location.getBlock();

        if (isInsideBorder(location) && !block.getType().isSolid() && !MaterialUtil.isLava(block.getType())) {

            Block blockAbove = block.getRelative(BlockFace.UP);
            Block blockBelow = block.getRelative(BlockFace.DOWN);

            return !block.getType().isSolid()
                    && !MaterialUtil.isLava(blockAbove.getType())
                    && !MaterialUtil.isAir(blockBelow.getType())
                    && !MaterialUtil.isLava(blockBelow.getType());
        }

        return false;
    }

    /**
     * Determines whether a location is safe for placing a grave block.
     *
     * @param location location to test
     * @return {@code true} if safe, {@code false} otherwise
     */
    public boolean isLocationSafeGrave(Location location) {
        if (location == null) return false;

        location = LocationUtil.roundLocation(location);
        if (location == null) return false;

        Block block = location.getBlock();

        return isInsideBorder(location) && MaterialUtil.isSafeNotSolid(block.getType()) && MaterialUtil.isSafeSolid(block.getRelative(BlockFace.DOWN).getType());
    }

    /**
     * Checks whether a grave already exists at the given location.
     *
     * @param location location to test
     * @return {@code true} if a grave is present, {@code false} otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasGrave(Location location) {
        if (location == null) return false;
        return plugin.getDataManager().hasChunkData(location) && plugin.getDataManager().getChunkData(location).getBlockDataMap().containsKey(location);
    }

    /**
     * Determines whether a location is inside the world border (or always true
     * for older versions without borders).
     *
     * @param location location to test
     * @return {@code true} if inside border or border is ignored, {@code false} otherwise
     */
    public boolean isInsideBorder(Location location) {
        if (location == null) return false;

        return plugin.getVersionManager().is_v1_7()
                || plugin.getVersionManager().is_v1_8()
                || plugin.getVersionManager().is_v1_9()
                || plugin.getVersionManager().is_v1_10()
                || plugin.getVersionManager().is_v1_11()
                || (location.getWorld() != null && location.getWorld().getWorldBorder().isInside(location));
    }

    /**
     * Determines whether a location is considered void (below minHeight or above maxHeight).
     *
     * @param location location to test
     * @return {@code true} if void, {@code false} otherwise
     */
    public boolean isVoid(Location location) {
        if (location == null || location.getWorld() == null) return true;

        return location.getY() < getMinHeight(location)
                || location.getY() > location.getWorld().getMaxHeight();
    }

    /**
     * Returns the minimum build height for the given location's world.
     *
     * @param location location whose world is inspected
     * @return minimum height, or 0 if world/minHeight is unavailable
     */
    public int getMinHeight(Location location) {
        return location != null
                && location.getWorld() != null
                && plugin.getVersionManager().hasMinHeight()
                ? location.getWorld().getMinHeight()
                : 0;
    }

    /**
     * Formats a location for debug output (world + block coordinates).
     */
    private String fmtLoc(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return loc.getWorld().getName()
                + " x=" + loc.getBlockX()
                + " y=" + loc.getBlockY()
                + " z=" + loc.getBlockZ();
    }

    /**
     * Returns a new location if a cached grave already exists at the provided location.
     *
     * @param livingEntity the entity that owns the grave (not currently used, but kept for API parity)
     * @param location     the desired grave location
     * @param grave        the grave being placed (not currently used, but kept for API parity)
     * @return a new safe location if the original is occupied in the cache; otherwise the original
     *         rounded location, or {@code null} if no alternative could be found
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
            if (isLocationSafeGraveAndPlayer(north) && !hasCachedGraveAt(north)) {
                plugin.debugMessage(prefix + "found new cached-grave-free location (north): " + fmtLoc(north), 1);
                return LocationUtil.roundLocation(north);
            }

            Location south = new Location(world, bx, y, bz + 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(south) && !hasCachedGraveAt(south)) {
                plugin.debugMessage(prefix + "found new cached-grave-free location (south): " + fmtLoc(south), 1);
                return LocationUtil.roundLocation(south);
            }

            Location west = new Location(world, bx - 1, y, bz, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(west) && !hasCachedGraveAt(west)) {
                plugin.debugMessage(prefix + "found new cached-grave-free location (west): " + fmtLoc(west), 1);
                return LocationUtil.roundLocation(west);
            }

            Location east = new Location(world, bx + 1, y, bz, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(east) && !hasCachedGraveAt(east)) {
                plugin.debugMessage(prefix + "found new cached-grave-free location (east): " + fmtLoc(east), 1);
                return LocationUtil.roundLocation(east);
            }

            Location nw = new Location(world, bx - 1, y, bz - 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(nw) && !hasCachedGraveAt(nw)) {
                plugin.debugMessage(prefix + "found new cached-grave-free location (north-west): " + fmtLoc(nw), 1);
                return LocationUtil.roundLocation(nw);
            }

            Location ne = new Location(world, bx + 1, y, bz - 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(ne) && !hasCachedGraveAt(ne)) {
                plugin.debugMessage(prefix + "found new cached-grave-free location (north-east): " + fmtLoc(ne), 1);
                return LocationUtil.roundLocation(ne);
            }

            Location sw = new Location(world, bx - 1, y, bz + 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(sw) && !hasCachedGraveAt(sw)) {
                plugin.debugMessage(prefix + "found new cached-grave-free location (south-west): " + fmtLoc(sw), 1);
                return LocationUtil.roundLocation(sw);
            }

            Location se = new Location(world, bx + 1, y, bz + 1, rounded.getYaw(), rounded.getPitch());
            if (isLocationSafeGraveAndPlayer(se) && !hasCachedGraveAt(se)) {
                plugin.debugMessage(prefix + "found new cached-grave-free location (south-east): " + fmtLoc(se), 1);
                return LocationUtil.roundLocation(se);
            }
        }

        plugin.debugMessage(prefix + "no suitable nearby location found; returning null.", 1);
        return null;
    }

    /**
     * Checks whether there is a cached grave (from {@link com.ranull.graves.manager.CacheManager#getGraveMap()})
     * at the given location, using rounded block coordinates for comparison.
     *
     * @param location location to test
     * @return {@code true} if a cached grave is at that exact block position, {@code false} otherwise
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
     * Combined safety check: location must be safe for the grave, safe for the player,
     * inside world bounds, not in the void, and must not already contain a grave.
     *
     * @param location candidate location
     * @return {@code true} if safe for both grave and player with no grave present, otherwise {@code false}
     */
    private boolean isLocationSafeGraveAndPlayer(Location location) {
        if (location == null) return false;

        location = LocationUtil.roundLocation(location);
        if (location == null || location.getWorld() == null) return false;

        if (isVoid(location) || !isInsideBorder(location)) {
            return false;
        }

        return !hasGrave(location) && isLocationSafeGrave(location) && isLocationSafePlayer(location);
    }

    /**
     * @deprecated Use {@link #getSafeTeleportLocation(Entity, Location, Grave, Graves)} or
     *             the new internal scanning helpers instead. This method is retained
     *             only for backward compatibility and is not used internally.
     */
    @Deprecated
    public Location getTop(Location location, Entity entity, Grave grave) {
        if (location == null) return null;
        Location rounded = LocationUtil.roundLocation(location);
        return resolveTeleportTop(rounded);
    }

    /**
     * @deprecated Use {@link #resolveRoofLocation(Location, Grave)} via {@link #getSafeGraveLocation(LivingEntity, Location, Grave)}.
     *             This method is retained only for backward compatibility and is not used internally.
     */
    @Deprecated
    public Location getRoof(Location location, Entity entity, Grave grave) {
        if (location == null) return null;
        Location rounded = LocationUtil.roundLocation(location);
        return resolveRoofLocation(rounded, grave);
    }

    /**
     * @deprecated Use {@link #resolveGroundLocation(Location, Grave)} via {@link #getSafeGraveLocation(LivingEntity, Location, Grave)}.
     *             This method is retained only for backward compatibility and is not used internally.
     */
    @Deprecated
    public Location getGround(Location location, Entity entity, Grave grave) {
        if (location == null) return null;
        Location rounded = LocationUtil.roundLocation(location);
        return resolveGroundLocation(rounded, grave);
    }

    /**
     * @deprecated Use {@link #resolveVoidLocation(Location, Grave)} via {@link #getSafeGraveLocation(LivingEntity, Location, Grave)}.
     *             This method is retained only for backward compatibility and is not used internally.
     */
    @Deprecated
    public Location getVoid(Location location, Entity entity, Grave grave) {
        if (location == null) return null;
        Location rounded = LocationUtil.roundLocation(location);
        return resolveVoidLocation(rounded, grave);
    }

    /**
     * @deprecated Use {@link #resolveLavaLocation(Location, LivingEntity, Grave)} via {@link #getSafeGraveLocation(LivingEntity, Location, Grave)}.
     *             This method is retained only for backward compatibility and is not used internally.
     */
    @Deprecated
    public Location getLavaTop(Location location, Entity entity, Grave grave) {
        if (location == null) return null;
        Location rounded = LocationUtil.roundLocation(location);
        LivingEntity living = (entity instanceof LivingEntity) ? (LivingEntity) entity : null;
        return resolveLavaLocation(rounded, living, grave);
    }
}