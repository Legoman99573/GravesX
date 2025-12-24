package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.MaterialUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Manages location-related operations for graves.
 */
public class LocationManager {

    /**
     * The main plugin instance associated with Graves.
     */
    private final Graves plugin;

    /**
     * Initializes a new instance of the LocationManager class.
     *
     * @param plugin The plugin instance.
     */
    public LocationManager(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets the last solid location of an entity.
     *
     * @param entity   The entity.
     * @param location The location.
     */
    public void setLastSolidLocation(Entity entity, Location location) {
        if (entity == null || location == null) return;
        plugin.getCacheManager().getLastLocationMap().put(entity.getUniqueId(), location);
    }

    /**
     * Gets the last solid location of an entity.
     *
     * <p>Returns {@code null} if:</p>
     * <ul>
     *     <li>No last location is stored,</li>
     *     <li>The stored location is in a different world than the entity's current world,</li>
     *     <li>The block below the stored location is not considered a safe solid block.</li>
     * </ul>
     *
     * @param entity The entity.
     * @return The last solid location, or {@code null} if none is valid.
     */
    public Location getLastSolidLocation(Entity entity) {
        if (entity == null) return null;

        Location location = plugin.getCacheManager().getLastLocationMap().get(entity.getUniqueId());
        if (location == null) return null;

        World w = location.getWorld();
        if (w == null) return null;
        if (!w.equals(entity.getWorld())) return null;

        Block below = location.getBlock().getRelative(BlockFace.DOWN);
        return MaterialUtil.isSafeSolid(below.getType()) ? location : null;
    }

    /**
     * Removes the last solid location of an entity.
     *
     * @param entity The entity.
     */
    public void removeLastSolidLocation(Entity entity) {
        if (entity == null) return;
        plugin.getCacheManager().getLastLocationMap().remove(entity.getUniqueId());
    }

    /**
     * Gets a safe teleport location.
     *
     * <p>This method attempts to return a location that is safe for a player/entity to teleport to:</p>
     * <ul>
     *     <li>If {@code teleport.unsafe} is enabled, the provided location is returned as-is (as long as it has a world).</li>
     *     <li>Otherwise, the location must pass {@link #isLocationSafePlayer(Location)}.</li>
     *     <li>If unsafe and {@code teleport.top} is enabled, a top-safe candidate is searched and returned if valid.</li>
     * </ul>
     *
     * @param entity   The entity.
     * @param location The location.
     * @param grave    The grave.
     * @param plugin   The plugin instance.
     * @return The safe teleport location, or {@code null} if none is found/allowed.
     */
    public Location getSafeTeleportLocation(Entity entity, Location location, Grave grave, Graves plugin) {
        if (location == null) return null;

        if (location.getWorld() != null) {
            if (plugin.getConfig("teleport.unsafe", grave).getBoolean("teleport.unsafe")
                    || isLocationSafePlayer(location)) {
                return location;
            } else if (plugin.getConfig("teleport.top", grave).getBoolean("teleport.top")) {
                Location topLocation = getTop(location, entity, grave);

                if (topLocation != null && topLocation.getWorld() != null && isLocationSafePlayer(topLocation)) {
                    plugin.getEntityManager().sendMessage("message.teleport-top", entity, topLocation, grave);
                    return topLocation;
                }
            }
        }

        return null;
    }

    /**
     * Gets a safe grave location.
     *
     * <p>The method attempts placement in the following order:</p>
     * <ol>
     *     <li>Use the provided location if it is safe for both grave and player and does not already contain a grave.</li>
     *     <li>If in void or outside border: use {@link #getVoid(Location, Entity, Grave)}.</li>
     *     <li>If inside lava: attempt {@link #getLavaTop(Location, Entity, Grave)}.</li>
     *     <li>Otherwise:
     *         <ul>
     *             <li>If current block is air/water: optionally use {@link #getGround(Location, Entity, Grave)} when {@code placement.ground} is enabled.</li>
     *             <li>If current block is solid: use {@link #getRoof(Location, Entity, Grave)}.</li>
     *         </ul>
     *     </li>
     *     <li>As a final fallback, try {@link #getVoid(Location, Entity, Grave)} again (config-dependent).</li>
     * </ol>
     *
     * <p>All returned locations are rounded using {@link LocationUtil#roundLocation(Location)}.</p>
     *
     * @param livingEntity The living entity.
     * @param location     The location.
     * @param grave        The grave.
     * @return The safe grave location, or {@code null} if no suitable location is found.
     */
    public Location getSafeGraveLocation(LivingEntity livingEntity, Location location, Grave grave) {
        if (location == null) return null;

        location = LocationUtil.roundLocation(location);
        if (location == null || location.getWorld() == null) return null;

        Block block = location.getBlock();
        Location candidate = null;

        if (isLocationSafeGraveAndPlayer(location)) {
            return LocationUtil.roundLocation(location);
        }

        if (isVoid(location) || !isInsideBorder(location)) {
            candidate = getVoid(location, livingEntity, grave);
        } else if (MaterialUtil.isLava(block.getType())) {
            candidate = getLavaTop(location, livingEntity, grave);
        } else {
            Location graveLocation = (MaterialUtil.isAir(block.getType()) || MaterialUtil.isWater(block.getType()))
                    ? (plugin.getConfig("placement.ground", grave).getBoolean("placement.ground")
                    ? getGround(location, livingEntity, grave)
                    : null)
                    : getRoof(location, livingEntity, grave);

            if (graveLocation == null
                    && (MaterialUtil.isAir(block.getType()) || MaterialUtil.isWater(block.getType()))
                    && plugin.getConfig("placement.ground", grave).getBoolean("placement.ground")) {
                graveLocation = findGround(location);
            }

            if (graveLocation != null) {
                candidate = graveLocation;
            }
        }

        if (isLocationSafeGraveAndPlayer(candidate)) {
            return LocationUtil.roundLocation(candidate);
        }

        Location voidLocation = getVoid(location, livingEntity, grave);
        if (isLocationSafeGraveAndPlayer(voidLocation)) {
            return LocationUtil.roundLocation(voidLocation);
        }

        return null;
    }

    /**
     * Finds the nearest solid ground below the given location.
     * Searches downward from the starting location until a solid block is found or the search limit is reached.
     *
     * <p>A ground position is considered found when the block below is a safe solid block and the current block
     * is passable (or air), allowing an entity/grave to occupy the space.</p>
     *
     * @param location The starting location.
     * @return The location on solid ground, or the original location if no ground is found within the search limit.
     */
    private Location findGround(Location location) {
        if (location == null) return null;
        World world = location.getWorld();
        if (world == null) return location;

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        int y = Math.min(location.getBlockY(), maxY - 1);
        if (y <= minY) y = minY + 1;

        int x = location.getBlockX();
        int z = location.getBlockZ();

        while (y > minY) {
            Block current = world.getBlockAt(x, y, z);
            Block below = world.getBlockAt(x, y - 1, z);

            if (MaterialUtil.isSafeSolid(below.getType())
                    && (current.isPassable() || MaterialUtil.isAir(current.getType()))) {
                return new Location(world, location.getX(), y, location.getZ());
            }
            y--;
        }

        return location;
    }

    /**
     * Finds the top location for placement, searching downward from the given location's Y-coordinate.
     *
     * <p>This method starts scanning from the world's max build height downwards (or from the provided
     * location's Y if the world is unavailable) and returns the first safe position it finds.</p>
     *
     * @param location The base location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The found top location, or {@code null} if no suitable location is found.
     */
    public Location getTop(Location location, Entity entity, Grave grave) {
        if (location == null) return null;
        World world = location.getWorld();
        int startY = (world != null) ? world.getMaxHeight() : location.getBlockY();
        return findLocationDownFromY(location, entity, startY, grave);
    }

    /**
     * Gets the roof location for placement.
     *
     * <p>This searches upward starting just above the current Y level to find a safe position
     * suitable for a grave.</p>
     *
     * @param location The location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The roof location, or {@code null} if none is found.
     */
    public Location getRoof(Location location, Entity entity, Grave grave) {
        if (location == null) return null;
        return findLocationUpFromY(location, entity, location.getBlockY() + 1, grave);
    }

    /**
     * Gets the ground location for placement.
     *
     * <p>This searches downward from the current Y level to find a safe position suitable for a grave.</p>
     *
     * @param location The location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The ground location, or {@code null} if none is found.
     */
    public Location getGround(Location location, Entity entity, Grave grave) {
        if (location == null) return null;
        return findLocationDownFromY(location, entity, location.getBlockY(), grave);
    }

    /**
     * Attempts to find a safe grave location by searching downward from a specified Y-coordinate.
     *
     * <p>The search will:</p>
     * <ul>
     *     <li>Start at the given Y level (clamped to world min/max heights),</li>
     *     <li>Detect lava/water columns and attempt to place above them via {@link #getLavaTop(Location, Entity, Grave)}
     *     or {@link #getWaterTop(Location, Entity, Grave)},</li>
     *     <li>Otherwise, return the first location that is safe for a grave and does not already contain a grave,</li>
     *     <li>Stop once the world’s minimum height is reached.</li>
     * </ul>
     *
     * @param location The base location.
     * @param entity   The entity.
     * @param y        The starting Y-coordinate.
     * @param grave    The grave.
     * @return A safe downward location, or {@code null} if none found.
     */
    private Location findLocationDownFromY(Location location, Entity entity, int y, Grave grave) {
        if (location == null || location.getWorld() == null) return null;

        Location checkLoc = location.clone();
        World world = checkLoc.getWorld();

        int minY = getMinHeight(checkLoc);
        int maxY = world.getMaxHeight();

        int startY = Math.min(y, maxY);
        if (startY < minY) startY = minY;

        checkLoc.setY(startY);

        while (checkLoc.getBlockY() >= minY) {
            Material type = checkLoc.getBlock().getType();

            if (MaterialUtil.isLava(type)) {
                return getLavaTop(checkLoc, entity, grave);
            } else if (MaterialUtil.isWater(type)) {
                return getWaterTop(checkLoc, entity, grave);
            } else if (isLocationSafeGrave(checkLoc) && !hasGrave(checkLoc)) {
                return checkLoc;
            }

            checkLoc.subtract(0, 1, 0);
        }

        return null;
    }

    /**
     * Attempts to find a safe grave location by searching upward from a specified Y-coordinate.
     *
     * <p>The search will:</p>
     * <ul>
     *     <li>Start at the given Y level,</li>
     *     <li>Detect lava/water columns and attempt to place above them via {@link #getLavaTop(Location, Entity, Grave)}
     *     or {@link #getWaterTop(Location, Entity, Grave)},</li>
     *     <li>Otherwise, return the first location that is safe for a grave and does not already contain a grave,</li>
     *     <li>Stop once the world’s max height is reached.</li>
     * </ul>
     *
     * @param location The base location.
     * @param entity   The entity.
     * @param y        The starting Y-coordinate.
     * @param grave    The grave.
     * @return A safe upward location, or {@code null} if none found.
     */
    private Location findLocationUpFromY(Location location, Entity entity, int y, Grave grave) {
        if (location == null || location.getWorld() == null) return null;

        World world = location.getWorld();
        int maxY = world.getMaxHeight();

        Location checkLoc = location.clone();
        checkLoc.setY(y);

        while (checkLoc.getY() <= maxY) {
            Material blockType = checkLoc.getBlock().getType();

            if (MaterialUtil.isLava(blockType)) {
                Location above = checkLoc.clone().add(0, 1, 0);
                Location lavaTop = getLavaTop(above, entity, grave);
                if (lavaTop != null) return lavaTop;
            } else if (MaterialUtil.isWater(blockType)) {
                Location above = checkLoc.clone().add(0, 1, 0);
                Location waterTop = getWaterTop(above, entity, grave);
                if (waterTop != null) return waterTop;
            } else if (isLocationSafeGrave(checkLoc) && !hasGrave(checkLoc)) {
                return checkLoc;
            }

            checkLoc.add(0, 1, 0);
        }

        return null;
    }

    /**
     * Gets the void location for placement.
     *
     * <p>This handles placement when the target location is in the void, outside the world border,
     * or otherwise unsuitable. Behavior is controlled by config:</p>
     * <ul>
     *     <li>{@code placement.void}: enables/disables void placement behavior.</li>
     *     <li>{@code placement.void-smart}: attempts to use {@link #getLastSolidLocation(Entity)} first.</li>
     *     <li>{@code placement.nether-roof}: if disabled in the Nether, roof scanning may be skipped.</li>
     *     <li>{@code placement.void-land-scan-radius}: radius (in blocks) to scan for a valid surface.</li>
     * </ul>
     *
     * <p>In the End, specialized island scanning may be performed via {@link #endVoidScan(Location, Grave)}.</p>
     *
     * @param location The location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The void location, or {@code null} if void placement is disabled, no world is available,
     *         or no suitable candidate was found.
     */
    public Location getVoid(Location location, Entity entity, Grave grave) {
        if (location == null) return null;

        if (!plugin.getConfig("placement.void", grave).getBoolean("placement.void")) {
            return null;
        }

        Location loc = location.clone();
        if (loc.getWorld() == null) return null;

        World world = loc.getWorld();
        World.Environment environment = world.getEnvironment();

        final boolean skipRoof = (environment == World.Environment.NETHER)
                && !plugin.getConfig("placement.nether-roof", grave).getBoolean("placement.nether-roof");

        if (plugin.getConfig("placement.void-smart", grave).getBoolean("placement.void-smart")) {
            Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);
            if (solidLocation != null && solidLocation.getWorld() != null) {
                if (!hasGrave(solidLocation)) {
                    return solidLocation;
                }
                if (!skipRoof) {
                    Location roof = getRoof(solidLocation, entity, grave);
                    if (roof != null && !hasGrave(roof)) return roof;
                }
            }
        }

        if (environment == World.Environment.THE_END) {
            Location endCandidate = endVoidScan(loc, grave);
            if (endCandidate != null && !hasGrave(endCandidate)) return endCandidate;
        }

        if (!skipRoof) {
            Location roof = getRoof(loc, entity, grave);
            if (roof != null && !hasGrave(roof)) return roof;
        }

        int radius = plugin.getConfig("placement.void-land-scan-radius", grave)
                .getInt("placement.void-land-scan-radius", 128);

        if (radius < 0) radius = 0;
        if (radius > 512) radius = 512;

        final int minY = getMinHeight(loc);
        final int startX = loc.getBlockX();
        final int startZ = loc.getBlockZ();

        int checksRemaining = 4096;

        BiFunction<Integer, Integer, Location> tryColumn = (x, z) -> {
            int cx = x >> 4;
            int cz = z >> 4;

            if (!world.isChunkLoaded(cx, cz)) return null;

            int y = world.getHighestBlockYAt(x, z);
            if (y < minY) return null;

            Block top = world.getBlockAt(x, y, z);
            Material topType = top.getType();
            if (topType.isSolid() && !(skipRoof && topType.name().contains("BEDROCK"))) {
                return new Location(world, x + 0.5, y + 1.0, z + 0.5, loc.getYaw(), loc.getPitch());
            }

            for (int yy = y - 1; yy >= minY; yy--) {
                Block ground = world.getBlockAt(x, yy, z);
                Material type = ground.getType();

                if (!type.isSolid()) continue;
                if (skipRoof && type.name().contains("BEDROCK")) continue;

                return new Location(world, x + 0.5, yy + 1.0, z + 0.5, loc.getYaw(), loc.getPitch());
            }

            return null;
        };

        Location candidate = tryColumn.apply(startX, startZ);
        checksRemaining--;
        if (candidate != null && !hasGrave(candidate)) return candidate;

        outer:
        for (int r = 1; r <= radius; r++) {

            for (int dx = -r; dx <= r; dx++) {
                if (checksRemaining-- <= 0) break outer;
                candidate = tryColumn.apply(startX + dx, startZ - r);
                if (candidate != null && !hasGrave(candidate)) return candidate;

                if (checksRemaining-- <= 0) break outer;
                candidate = tryColumn.apply(startX + dx, startZ + r);
                if (candidate != null && !hasGrave(candidate)) return candidate;
            }

            for (int dz = -r + 1; dz <= r - 1; dz++) {
                if (checksRemaining-- <= 0) break outer;
                candidate = tryColumn.apply(startX - r, startZ + dz);
                if (candidate != null && !hasGrave(candidate)) return candidate;

                if (checksRemaining-- <= 0) break outer;
                candidate = tryColumn.apply(startX + r, startZ + dz);
                if (candidate != null && !hasGrave(candidate)) return candidate;
            }
        }

        return null;
    }

    /**
     * End-specific void handling.
     *
     * <p>This method is invoked when in {@link World.Environment#THE_END} and the current column appears
     * to have no land. It attempts to locate the nearest island surface within a configured radius and place
     * the grave above it.</p>
     *
     * <p>Key behaviors:</p>
     * <ul>
     *     <li>If the current X/Z column has solid land anywhere down to min height, returns {@code null} to allow standard flow.</li>
     *     <li>Otherwise, scans outward for the nearest island surface; places grave 1 block above with optional support block.</li>
     *     <li>If none found, uses a fallback Y with optional support block.</li>
     * </ul>
     *
     * @param location The starting location (used for origin X/Z and world reference).
     * @param grave    The grave.
     * @return A placement location in the End void scenario, or {@code null} if the column has land (meaning normal flow should proceed).
     */
    private Location endVoidScan(Location location, Grave grave) {
        World world = location.getWorld();
        if (world == null) return null;

        int minY = getMinHeight(location);
        int originX = location.getBlockX();
        int originZ = location.getBlockZ();

        boolean columnHasLand = false;
        for (int y = Math.min(location.getBlockY(), world.getMaxHeight() - 1); y >= minY; y--) {
            Material m = world.getBlockAt(originX, y, originZ).getType();
            if (!MaterialUtil.isAir(m) && m.isSolid()) {
                columnHasLand = true;
                break;
            }
        }
        if (columnHasLand) return null;

        int searchRadius = plugin.getConfig("placement.end.search-radius", grave)
                .getInt("placement.end.search-radius", 96);

        boolean allowVoidBlock = plugin.getConfig("placement.allow-void-block", grave)
                .getBoolean("placement.allow-void-block", true);

        Material voidBlock;
        if (allowVoidBlock) {
            String voidBlockName = plugin.getConfig("placement.void-block", grave)
                    .getString("placement.void-block", "DIRT");
            Material parsed = null;
            try {
                if (voidBlockName != null && !voidBlockName.isEmpty()) {
                    parsed = Material.matchMaterial(voidBlockName.toUpperCase());
                }
            } catch (Throwable ignored) { /* ignore */ }
            voidBlock = (parsed != null && parsed.isBlock()) ? parsed : Material.DIRT;
        } else {
            voidBlock = null;
        }

        for (int r = 1; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                int xc = originX + dx;
                int[] zs = new int[]{originZ - r, originZ + r};
                for (int zc : zs) {
                    int hy = world.getHighestBlockYAt(xc, zc);
                    if (hy <= minY) continue;

                    Block base = world.getBlockAt(xc, hy, zc);
                    if (!base.getType().isSolid()) continue;

                    Block space1 = base.getRelative(0, 1, 0);
                    Block space2 = base.getRelative(0, 2, 0);
                    if (!MaterialUtil.isAir(space1.getType()) || !MaterialUtil.isAir(space2.getType())) continue;

                    Location candidate = new Location(world, xc + 0.5, hy + 2, zc + 0.5);
                    if (hasGrave(candidate)) continue;

                    if (allowVoidBlock) {
                        setBlockTypeNoPhysicsSafely(space1, voidBlock);
                    }
                    return candidate;
                }
            }

            for (int dz = -r + 1; dz <= r - 1; dz++) {
                int zc = originZ + dz;
                int[] xs = new int[]{originX - r, originX + r};
                for (int xc : xs) {
                    int hy = world.getHighestBlockYAt(xc, zc);
                    if (hy <= minY) continue;

                    Block base = world.getBlockAt(xc, hy, zc);
                    if (!base.getType().isSolid()) continue;

                    Block space1 = base.getRelative(0, 1, 0);
                    Block space2 = base.getRelative(0, 2, 0);
                    if (!MaterialUtil.isAir(space1.getType()) || !MaterialUtil.isAir(space2.getType())) continue;

                    Location candidate = new Location(world, xc + 0.5, hy + 2, zc + 0.5);
                    if (hasGrave(candidate)) continue;

                    if (allowVoidBlock) {
                        setBlockTypeNoPhysicsSafely(space1, voidBlock);
                    }
                    return candidate;
                }
            }
        }

        int fallbackY = plugin.getConfig("placement.end.fallback-y", grave)
                .getInt("placement.end.fallback-y", Math.max(64, minY + 1));

        Block support = world.getBlockAt(originX, fallbackY, originZ);
        if (allowVoidBlock) {
            setBlockTypeNoPhysicsSafely(support, (voidBlock != null ? voidBlock : Material.DIRT));
        }

        return new Location(world, originX + 0.5, fallbackY + 1, originZ + 0.5);
    }

    /**
     * Gets the top location above lava for placement.
     *
     * <p>Behavior:</p>
     * <ul>
     *     <li>If {@code placement.lava-smart} is enabled, attempts to use the entity's last solid location first.</li>
     *     <li>If {@code placement.lava-top} is enabled, scans upward from the provided location until out of lava,
     *     then searches for the first air block without title/protected data to place into.</li>
     * </ul>
     *
     * @param location The location to check.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The lava top location, or {@code null} if no valid location is found.
     */
    public Location getLavaTop(Location location, Entity entity, Grave grave) {
        if (location == null) return null;

        if (plugin.getConfig("placement.lava-smart", grave).getBoolean("placement.lava-smart")) {
            Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);
            if (solidLocation != null) {
                if (!hasGrave(solidLocation)) {
                    return solidLocation;
                } else {
                    Location up = solidLocation.clone().add(0, 1, 0);
                    if (isLocationSafeGrave(up) && !hasGrave(up)) return up;
                }
            }
        }

        if (plugin.getConfig("placement.lava-top", grave).getBoolean("placement.lava-top")) {
            Location checkLoc = location.clone();
            if (checkLoc.getWorld() == null) return null;

            int maxHeight = checkLoc.getWorld().getMaxHeight();

            while (MaterialUtil.isLava(checkLoc.getBlock().getType()) && checkLoc.getY() < maxHeight) {
                checkLoc.add(0, 1, 0);
            }

            while (checkLoc.getY() < maxHeight) {
                Block block = checkLoc.getBlock();
                if (MaterialUtil.isAir(block.getType()) && !plugin.getCompatibility().hasTitleData(block)) {
                    return checkLoc;
                }
                checkLoc.add(0, 1, 0);
            }
        }

        return null;
    }

    /**
     * Gets the top location above water for placement.
     *
     * <p>Behavior mirrors {@link #getLavaTop(Location, Entity, Grave)}:</p>
     * <ul>
     *     <li>If {@code placement.water-smart} is enabled, attempts to use the entity's last solid location first.</li>
     *     <li>If {@code placement.water-top} is enabled, scans upward out of water and then finds the first air block
     *     without title/protected data.</li>
     * </ul>
     *
     * @param location The location to check.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The water top location, or {@code null} if no valid location is found.
     */
    public Location getWaterTop(Location location, Entity entity, Grave grave) {
        if (location == null) return null;

        if (plugin.getConfig("placement.water-top", grave).getBoolean("placement.water-top")) {
            Location checkLoc = location.clone();
            if (checkLoc.getWorld() == null) return null;

            int maxHeight = checkLoc.getWorld().getMaxHeight();

            while (MaterialUtil.isWater(checkLoc.getBlock().getType()) && checkLoc.getY() < maxHeight) {
                checkLoc.add(0, 1, 0);
            }

            while (checkLoc.getY() < maxHeight) {
                Block block = checkLoc.getBlock();
                if (MaterialUtil.isAir(block.getType()) && !plugin.getCompatibility().hasTitleData(block)) {
                    return checkLoc;
                }
                checkLoc.add(0, 1, 0);
            }
        }

        if (plugin.getConfig("placement.water-smart", grave).getBoolean("placement.water-smart")) {
            Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);
            if (solidLocation != null) {
                if (!hasGrave(solidLocation)) {
                    return solidLocation;
                } else {
                    Location up = solidLocation.clone().add(0, 1, 0);
                    if (isLocationSafeGrave(up) && !hasGrave(up)) return up;
                }
            }
        }

        return null;
    }

    /**
     * Determines if a living entity can build at a specified location.
     *
     * @param livingEntity   The living entity.
     * @param location       The location.
     * @param permissionList The list of permissions.
     * @return True if the entity can build, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canBuild(LivingEntity livingEntity, Location location, List<String> permissionList) {
        Plugin landProtectionAddonPlugin = plugin.getServer().getPluginManager().getPlugin("GravesXAddon-LandProtection");
        if (landProtectionAddonPlugin != null && landProtectionAddonPlugin.isEnabled()) return true;

        if (livingEntity instanceof Player player) {
            return (!plugin.getConfig("placement.can-build", player, permissionList).getBoolean("placement.can-build")
                    || plugin.getCompatibility().canBuild(player, location, plugin))
                    && (!plugin.getIntegrationManager().hasProtectionLib()
                    || (!plugin.getConfig("placement.can-build-protectionlib", player, permissionList)
                    .getBoolean("placement.can-build-protectionlib")
                    || plugin.getIntegrationManager().getProtectionLib().canBuild(location, player)));
        }

        return true;
    }

    /**
     * Determines if a location is safe for a player to spawn or teleport to.
     *
     * @param location The location to check.
     * @return True if the location is safe; otherwise, false.
     */
    public boolean isLocationSafePlayer(Location location) {
        if (location == null || location.getWorld() == null) return false;

        if (!isInsideBorder(location)) return false;

        Block block = location.getBlock();
        Block blockAbove = block.getRelative(BlockFace.UP);
        Block blockBelow = block.getRelative(BlockFace.DOWN);

        Material type = block.getType();
        Material aboveType = blockAbove.getType();
        Material belowType = blockBelow.getType();

        return !type.isSolid()
                && !MaterialUtil.isLava(type)
                && !MaterialUtil.isLava(aboveType)
                && !belowType.isAir()
                && !MaterialUtil.isLava(belowType);
    }

    /**
     * Determines if a location is safe for a grave.
     *
     * @param location The location.
     * @return True if the location is safe, otherwise false.
     */
    public boolean isLocationSafeGrave(Location location) {
        if (location == null) return false;

        location = LocationUtil.roundLocation(location);
        if (location == null) return false;

        World world = location.getWorld();
        if (world == null) return false;
        if (!isInsideBorder(location)) return false;

        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        Block above = block.getRelative(BlockFace.UP);

        if (isBedrockRelated(block, below)) return false;

        Material type = block.getType();
        Material belowType = below.getType();
        Material aboveType = above.getType();

        if (MaterialUtil.isLava(type) || MaterialUtil.isLava(aboveType)
                || MaterialUtil.isWater(type) || MaterialUtil.isWater(aboveType)) {
            return false;
        }

        if (type == Material.NETHER_PORTAL || type == Material.END_PORTAL
                || aboveType == Material.NETHER_PORTAL || aboveType == Material.END_PORTAL) {
            return false;
        }

        if (plugin.getCompatibility().hasTitleData(block)
                || plugin.getCompatibility().hasTitleData(above)) {
            return false;
        }

        return MaterialUtil.isSafeNotSolid(type) && MaterialUtil.isSafeSolid(belowType);
    }

    /**
     * Combined safety check: location must be safe for the grave, safe for the player,
     * and must not already contain a grave.
     *
     * @param location The candidate location.
     * @return True if the location is safe for both grave and player and has no grave.
     */
    private boolean isLocationSafeGraveAndPlayer(Location location) {
        if (location == null) return false;
        location = LocationUtil.roundLocation(location);
        if (location == null) return false;

        return !hasGrave(location)
                && isLocationSafeGrave(location)
                && isLocationSafePlayer(location);
    }

    /**
     * Checks whether the given blocks are bedrock-related.
     */
    private boolean isBedrockRelated(Block block, Block below) {
        return block.getType() == Material.BEDROCK || below.getType() == Material.BEDROCK;
    }

    /**
     * Determines if a location has a grave.
     *
     * @param location The location.
     * @return True if the location has a grave, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasGrave(Location location) {
        if (location == null) return false;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null || rounded.getWorld() == null) return false;

        return plugin.getDataManager().hasChunkData(rounded)
                && plugin.getDataManager().getChunkData(rounded).getBlockDataMap().containsKey(rounded);
    }

    /**
     * Determines if a location is inside the world border.
     *
     * For versions prior to 1.12, the world border is ignored and this always returns true.
     *
     * @param location The location to check.
     * @return True if the location is inside the world border; otherwise, false.
     */
    public boolean isInsideBorder(Location location) {
        if (location == null) return false;

        if (plugin.getVersionManager().is_v1_7()
                || plugin.getVersionManager().is_v1_8()
                || plugin.getVersionManager().is_v1_9()
                || plugin.getVersionManager().is_v1_10()
                || plugin.getVersionManager().is_v1_11()) {
            return true;
        }

        World world = location.getWorld();
        return world != null && world.getWorldBorder().isInside(location);
    }

    /**
     * Determines if the specified location is in the void.
     *
     * @param location The location to check.
     * @return True if the location is in the void; otherwise, false.
     */
    public boolean isVoid(Location location) {
        if (location == null || location.getWorld() == null) {
            return true;
        }

        int y = location.getBlockY();
        World world = location.getWorld();

        return y < getMinHeight(location) || y > world.getMaxHeight();
    }

    /**
     * Gets the minimum height for a location.
     *
     * @param location The location.
     * @return The minimum height.
     */
    public int getMinHeight(Location location) {
        return location != null && location.getWorld() != null && plugin.getVersionManager().hasMinHeight()
                ? location.getWorld().getMinHeight()
                : 0;
    }

    /**
     * Sets a block type without physics using GravesX's Universal/Folia Scheduler.
     *
     * @param block block to change
     * @param type  material to set
     */
    private void setBlockTypeNoPhysicsSafely(Block block, Material type) {
        if (block == null || type == null) return;

        Runnable action = () -> {
            try {
                block.setType(type, false);
            } catch (Throwable ignored) {
            }
        };

        var scheduler = plugin.getGravesXScheduler();
        if (scheduler != null) {
            scheduler.execute(block.getLocation(), action);
        } else {
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }
}
