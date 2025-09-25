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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages location-related operations for graves.
 */
public final class LocationManager {
    /**
     * The main plugin instance associated with Graves.
     * <p>
     * This {@link Graves} instance represents the core plugin that this Graves is part of. It provides access
     * to the plugin's functionality, configuration, and other services.
     * </p>
     */
    private final Graves plugin;

    /**
     * Initializes a new instance of the LocationManager class.
     *
     * @param plugin The plugin instance.
     */
    public LocationManager(final Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets the last solid location of an entity.
     *
     * @param entity   The entity.
     * @param location The location.
     */
    public void setLastSolidLocation(final Entity entity, final Location location) {
        plugin.getCacheManager().getLastLocationMap().put(entity.getUniqueId(), location);
    }

    /**
     * Gets the last solid location of an entity.
     *
     * @param entity The entity.
     * @return The last solid location.
     */
    public Location getLastSolidLocation(final Entity entity) {
        final Location location = plugin.getCacheManager().getLastLocationMap().get(entity.getUniqueId());

        return location != null && location.getWorld() != null
                && location.getWorld().equals(entity.getWorld())
                && location.getBlock().getRelative(BlockFace.DOWN).getType().isSolid() ? location : null;
    }

    /**
     * Removes the last solid location of an entity.
     *
     * @param entity The entity.
     */
    public void removeLastSolidLocation(final Entity entity) {
        plugin.getCacheManager().getLastLocationMap().remove(entity.getUniqueId());
    }

    /**
     * Gets a safe teleport location.
     *
     * @param entity   The entity.
     * @param location The location.
     * @param grave    The grave.
     * @param plugin   The plugin instance.
     * @return The safe teleport location.
     */
    public Location getSafeTeleportLocation(final Entity entity, final Location location, final Grave grave, final Graves plugin) {
        if (location.getWorld() != null) {
            if (plugin.getConfig("teleport.unsafe", grave).getBoolean("teleport.unsafe")
                    || isLocationSafePlayer(location)) {
                return location;
            } else if (plugin.getConfig("teleport.top", grave).getBoolean("teleport.top")) {
                final Location topLocation = getTop(location, entity, grave);

                if (topLocation != null && isLocationSafePlayer(topLocation) && topLocation.getWorld() != null) {
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
     * @param livingEntity The living entity.
     * @param location     The location.
     * @param grave        The grave.
     * @return The safe grave location.
     */
    public Location getSafeGraveLocation(final LivingEntity livingEntity, Location location, final Grave grave) {
        location = LocationUtil.roundLocation(location);

        if (location == null || location.getWorld() == null) {
            return getVoid(location, livingEntity, grave);
        }

        final Block block = location.getBlock();

        if (isLocationSafeGrave(location)) {
            return getGround(location, livingEntity, grave);
        }

        int attempts = 10;
        while (attempts > 0) {
            final int randomX = ThreadLocalRandom.current().nextInt(3) - 1; // -1, 0, 1
            final int randomZ = ThreadLocalRandom.current().nextInt(3) - 1;

            if (randomX != 0 || randomZ != 0) {
                Location newLocation = location.clone().add(randomX, 0, randomZ);
                newLocation = LocationUtil.roundLocation(newLocation);
                newLocation = findGround(newLocation);

                if (isLocationSafeGrave(newLocation)) {
                    return newLocation;
                }
            }

            attempts--;
        }

        if (isVoid(location) || !isInsideBorder(location)) {
            return getVoid(location, livingEntity, grave);
        }

        if (MaterialUtil.isLava(block.getType())) {
            return getLavaTop(location, livingEntity, grave);
        }

        if (MaterialUtil.isWater(block.getType())) {
            return getWaterTop(location, livingEntity, grave);
        }

        if (MaterialUtil.isAir(block.getType()) || MaterialUtil.isWater(block.getType())) {
            if (plugin.getConfig("placement.ground", grave).getBoolean("placement.ground")) {
                return getGround(location, livingEntity, grave);
            }
        } else {
            return getRoof(location, livingEntity, grave);
        }

        return getVoid(location, livingEntity, grave);
    }

    /**
     * Finds the nearest solid ground below the given location.
     * Searches downward from the starting location until a solid block is found or the search limit is reached.
     *
     * @param location The starting location.
     * @return The location on solid ground, or the original location if no ground is found within the search limit.
     */
    private Location findGround(final Location location) {
        if (location == null) return null;
        final World world = location.getWorld();
        if (world == null) return location;

        final int minY = world.getMinHeight();
        final int maxY = world.getMaxHeight();

        int y = Math.min(location.getBlockY(), maxY - 1);
        if (y <= minY) y = minY + 1;

        final int x = location.getBlockX();
        final int z = location.getBlockZ();

        while (y > minY) {
            final Block current = world.getBlockAt(x, y, z);
            final Block below = world.getBlockAt(x, y - 1, z);

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
     * @param location The base location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The found top location, or null if no suitable location is found.
     */
    public Location getTop(final Location location, final Entity entity, final Grave grave) {
        if (location.getWorld() == null) {
            return null;
        }

        final int maxY = location.getWorld().getMaxHeight();
        int startY = location.getBlockY();

        if (startY >= maxY) startY = maxY - 1;

        return findLocationDownFromY(location, entity, startY, grave);
    }

    /**
     * Gets the roof location for placement.
     *
     * @param location The location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The roof location.
     */
    public Location getRoof(final Location location, final Entity entity, final Grave grave) {
        return findLocationUpFromY(location, entity, location.getBlockY(), grave);
    }

    /**
     * Gets the ground location for placement.
     *
     * @param location The location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The ground location.
     */
    public Location getGround(final Location location, final Entity entity, final Grave grave) {
        return findLocationDownFromY(location, entity, location.getBlockY(), grave);
    }

    /**
     * Attempts to find a safe grave location by searching downward from a specified Y-coordinate.
     *
     * The search will:
     * - Start at the given Y level.
     * - Look for either lava or water (and use getLavaTop or getWaterTop), or a safe and grave-free location.
     * - Stop once the world’s minimum height is reached.
     *
     * @param location The base location.
     * @param entity   The entity.
     * @param y        The starting Y-coordinate.
     * @param grave    The grave.
     * @return A safe downward location, or null if none found.
     */
    private Location findLocationDownFromY(final Location location, final Entity entity, final int y, final Grave grave) {
        if (location.getWorld() == null) return null;

        final World world = location.getWorld();
        final int minY = getMinHeight(location);

        final Location checkLoc = location.clone();
        checkLoc.setY(y);

        final boolean allowNetherRoof = plugin.getConfig("placement.nether-roof", grave).getBoolean("placement.nether-roof");
        if (world.getEnvironment() == World.Environment.NETHER && !allowNetherRoof && checkLoc.getY() > 126) {
            checkLoc.setY(126);
        }

        while (checkLoc.getY() >= minY) {
            final Material blockType = checkLoc.getBlock().getType();

            if (MaterialUtil.isLava(blockType)) {
                return getLavaTop(checkLoc, entity, grave);
            } else if (MaterialUtil.isWater(blockType)) {
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
     * The search will:
     * - Start at the given Y level.
     * - Look for either lava or water (and use getLavaTop or getWaterTop), or a safe and grave-free location.
     * - Stop once the world’s max height is reached.
     *
     * @param location The base location.
     * @param entity   The entity.
     * @param y        The starting Y-coordinate.
     * @param grave    The grave.
     * @return A safe upward location, or null if none found.
     */
    private Location findLocationUpFromY(final Location location, final Entity entity, final int y, final Grave grave) {
        if (location.getWorld() == null) return null;

        final World world = location.getWorld();
        final int maxY = world.getMaxHeight();

        final Location checkLoc = location.clone();
        checkLoc.setY(y);

        while (checkLoc.getY() <= maxY) {
            final Material blockType = checkLoc.getBlock().getType();

            if (MaterialUtil.isLava(blockType)) {
                return getLavaTop(checkLoc, entity, grave);
            } else if (MaterialUtil.isWater(blockType)) {
                return getWaterTop(checkLoc, entity, grave);
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
     * @param location The location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The void location.
     */
    public Location getVoid(Location location, final Entity entity, final Grave grave) {
        if (!plugin.getConfig("placement.void", grave).getBoolean("placement.void")) {
            return null;
        }

        location = location.clone();

        if (plugin.getConfig("placement.void-smart", grave).getBoolean("placement.void-smart")) {
            final Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);
            if (solidLocation != null) {
                return !hasGrave(solidLocation) ? solidLocation : getRoof(solidLocation, entity, grave);
            }
        }

        if (location.getWorld() == null) return null;

        final World world = location.getWorld();
        final World.Environment environment = world.getEnvironment();

        if (environment == World.Environment.THE_END) {
            final Location endCandidate = endVoidScan(location, grave);
            if (endCandidate != null) return endCandidate;
        }

        final boolean skipRoof = (environment == World.Environment.NETHER) && !plugin.getConfig("placement.nether-roof", grave).getBoolean("placement.nether-roof");

        if (!skipRoof) {
            final Location roof = getRoof(location, entity, grave);
            if (roof != null) return roof;
        }

        final int minY = getMinHeight(location);
        final Location bottom = new Location(world, location.getX(), minY, location.getZ());
        final Block block = bottom.getBlock();
        if (MaterialUtil.isAir(block.getType()) || !block.getType().isSolid()) {
            bottom.setY(minY + 1);
        }
        return bottom;
    }

    /**
     * End-specific void handling:
     * - If the current column has land, returns null to allow standard flow.
     * - Otherwise, scans outward for the nearest island; places grave 1 block above with optional support block beneath.
     * - If none found, uses a fallback Y with optional support block.
     */
    private Location endVoidScan(final Location location, final Grave grave) {
        final World world = location.getWorld();
        if (world == null) return null;

        final int minY = getMinHeight(location);
        final int originX = location.getBlockX();
        final int originZ = location.getBlockZ();

        boolean columnHasLand = false;
        for (int y = Math.min(location.getBlockY(), world.getMaxHeight() - 1); y >= minY; y--) {
            final Material m = world.getBlockAt(originX, y, originZ).getType();
            if (!MaterialUtil.isAir(m) && m.isSolid()) { columnHasLand = true; break; }
        }
        if (columnHasLand) return null;

        final int searchRadius = plugin.getConfig("placement.end.search-radius", grave)
                .getInt("placement.end.search-radius", 96);

        final boolean allowVoidBlock = plugin.getConfig("placement.allow-void-block", grave)
                .getBoolean("placement.allow-void-block", true);

        final Material voidBlock;
        if (allowVoidBlock) {
            final String voidBlockName = plugin.getConfig("placement.void-block", grave)
                    .getString("placement.void-block", "DIRT");
            Material parsed = null;
            try {
                if (!voidBlockName.isEmpty()) {
                    parsed = Material.matchMaterial(voidBlockName.toUpperCase());
                }
            } catch (Throwable ignored) { /* ignore */ }
            voidBlock = (parsed != null && parsed.isBlock()) ? parsed : Material.DIRT;
        } else {
            voidBlock = null;
        }

        // Expand in a diamond/ring pattern
        for (int r = 1; r <= searchRadius; r++) {
            // Top & Bottom edges
            for (int dx = -r; dx <= r; dx++) {
                final int xc = originX + dx;
                final int[] zs = new int[]{originZ - r, originZ + r};
                for (final int zc : zs) {
                    final int hy = world.getHighestBlockYAt(xc, zc);
                    if (hy <= minY) continue;

                    final Block base = world.getBlockAt(xc, hy, zc);
                    if (!base.getType().isSolid()) continue;

                    final Block space1 = base.getRelative(0, 1, 0);
                    final Block space2 = base.getRelative(0, 2, 0);
                    if (!MaterialUtil.isAir(space1.getType()) || !MaterialUtil.isAir(space2.getType())) continue;

                    final Location candidate = new Location(world, xc + 0.5, hy + 2, zc + 0.5);
                    if (hasGrave(candidate)) continue;

                    if (allowVoidBlock) {
                        setBlockTypeNoPhysicsSafely(space1, voidBlock);
                    }
                    return candidate;
                }
            }

            for (int dz = -r + 1; dz <= r - 1; dz++) {
                final int zc = originZ + dz;
                final int[] xs = new int[]{originX - r, originX + r};
                for (final int xc : xs) {
                    final int hy = world.getHighestBlockYAt(xc, zc);
                    if (hy <= minY) continue;

                    final Block base = world.getBlockAt(xc, hy, zc);
                    if (!base.getType().isSolid()) continue;

                    final Block space1 = base.getRelative(0, 1, 0);
                    final Block space2 = base.getRelative(0, 2, 0);
                    if (!MaterialUtil.isAir(space1.getType()) || !MaterialUtil.isAir(space2.getType())) continue;

                    final Location candidate = new Location(world, xc + 0.5, hy + 2, zc + 0.5);
                    if (hasGrave(candidate)) continue;

                    if (allowVoidBlock) {
                        setBlockTypeNoPhysicsSafely(space1, voidBlock);
                    }
                    return candidate;
                }
            }
        }

        final int fallbackY = plugin.getConfig("placement.end.fallback-y", grave).getInt("placement.end.fallback-y", Math.max(64, minY + 1));
        final Block support = world.getBlockAt(originX, fallbackY, originZ);
        if (allowVoidBlock) {
            setBlockTypeNoPhysicsSafely(support, (voidBlock != null ? voidBlock : Material.DIRT));
        }
        return new Location(world, originX + 0.5, fallbackY + 1, originZ + 0.5);
    }

    /**
     * Gets the top location above lava for placement.
     *
     * @param location The location to check.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The lava top location, or null if no valid location is found.
     */
    public Location getLavaTop(final Location location, final Entity entity, final Grave grave) {
        if (plugin.getConfig("placement.lava-smart", grave).getBoolean("placement.lava-smart")) {
            final Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);

            if (solidLocation != null) {
                return !hasGrave(solidLocation) ? solidLocation : getRoof(solidLocation, entity, grave);
            }
        }

        if (plugin.getConfig("placement.lava-top", grave).getBoolean("placement.lava-top")) {
            final Location checkLoc = location.clone();

            if (checkLoc.getWorld() != null) {
                final int maxHeight = checkLoc.getWorld().getMaxHeight();

                while (checkLoc.getBlock().getType() == Material.LAVA && checkLoc.getY() < maxHeight) {
                    checkLoc.add(0, 1, 0);
                }

                while (checkLoc.getY() < maxHeight) {
                    final Block block = checkLoc.getBlock();

                    if (MaterialUtil.isAir(block.getType()) && !plugin.getCompatibility().hasTitleData(block)) {
                        return checkLoc;
                    }

                    checkLoc.add(0, 1, 0);
                }
            }
        }

        return null;
    }

    /**
     * Gets the top location above water for placement.
     *
     * @param location The location to check.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The water top location, or null if no valid location is found.
     */
    public Location getWaterTop(final Location location, final Entity entity, final Grave grave) {
        if (plugin.getConfig("placement.water-smart", grave).getBoolean("placement.water-smart")) {
            final Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);

            if (solidLocation != null) {
                return !hasGrave(solidLocation) ? solidLocation : getRoof(solidLocation, entity, grave);
            }
        }

        if (plugin.getConfig("placement.water-top", grave).getBoolean("placement.water-top")) {
            final Location checkLoc = location.clone();

            if (checkLoc.getWorld() != null) {
                final int maxHeight = checkLoc.getWorld().getMaxHeight();

                // Search upwards until we reach a block that is no longer water
                while (checkLoc.getBlock().getType() == Material.WATER && checkLoc.getY() < maxHeight) {
                    checkLoc.add(0, 1, 0);
                }

                // Once we exit the water, check if the space above is air and suitable for placement
                while (checkLoc.getY() < maxHeight) {
                    final Block block = checkLoc.getBlock();

                    if (MaterialUtil.isAir(block.getType()) && !plugin.getCompatibility().hasTitleData(block)) {
                        return checkLoc; // Return the valid air location above the water
                    }

                    checkLoc.add(0, 1, 0);
                }
            }
        }

        return null; // Return null if no valid location is found
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
    public boolean canBuild(final LivingEntity livingEntity, final Location location, final List<String> permissionList) {
        final Plugin landProtectionAddonPlugin = plugin.getServer().getPluginManager().getPlugin("GravesXAddon-LandProtection");
        if (landProtectionAddonPlugin != null && landProtectionAddonPlugin.isEnabled()) return true;

        if (livingEntity instanceof final Player player) {
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
     * A location is considered safe if:
     * - It is inside the world border.
     * - The current block and block above are not solid or lava.
     * - The block below is solid and not lava.
     *
     * @param location The location to check.
     * @return True if the location is safe; otherwise, false.
     */
    public boolean isLocationSafePlayer(final Location location) {
        if (!isInsideBorder(location)) {
            return false;
        }

        final Block block = location.getBlock();
        final Block blockAbove = block.getRelative(BlockFace.UP);
        final Block blockBelow = block.getRelative(BlockFace.DOWN);

        final Material type = block.getType();
        final Material aboveType = blockAbove.getType();
        final Material belowType = blockBelow.getType();

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
        location = LocationUtil.roundLocation(location);
        final Block block = location.getBlock();

        return isInsideBorder(location) && MaterialUtil.isSafeNotSolid(block.getType())
                && MaterialUtil.isSafeSolid(block.getRelative(BlockFace.DOWN).getType());
    }

    /**
     * Determines if a location has a grave.
     *
     * @param location The location.
     * @return True if the location has a grave, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasGrave(final Location location) {
        return plugin.getDataManager().hasChunkData(location)
                && plugin.getDataManager().getChunkData(location).getBlockDataMap().containsKey(location);
    }

    /**
     * Determines if a location is inside the world border.
     *
     * For versions prior to 1.12, the world border is ignored and this always returns true.
     *
     * @param location The location to check.
     * @return True if the location is inside the world border; otherwise, false.
     */
    public boolean isInsideBorder(final Location location) {
        if (plugin.getVersionManager().is_v1_7()
                || plugin.getVersionManager().is_v1_8()
                || plugin.getVersionManager().is_v1_9()
                || plugin.getVersionManager().is_v1_10()
                || plugin.getVersionManager().is_v1_11()) {
            return true;
        }

        final World world = location.getWorld();
        return world != null && world.getWorldBorder().isInside(location);
    }

    /**
     * Determines if the specified location is in the void.
     * A location is considered in the void if it's below the world's minimum height
     * or above its maximum build height.
     *
     * @param location The location to check.
     * @return True if the location is in the void; otherwise, false.
     */
    public boolean isVoid(final Location location) {
        if (location == null || location.getWorld() == null) {
            return true;
        }

        final int y = location.getBlockY();
        final World world = location.getWorld();

        return y < getMinHeight(location) || y > world.getMaxHeight();
    }

    /**
     * Gets the minimum height for a location.
     *
     * @param location The location.
     * @return The minimum height.
     */
    public int getMinHeight(final Location location) {
        return location.getWorld() != null && plugin.getVersionManager().hasMinHeight()
                ? location.getWorld().getMinHeight() : 0;
    }

    /**
     * Sets a block type without physics using GravesX's Universal/Folia Scheduler.
     * <p>
     * Uses {@code plugin.getGravesXScheduler()} to run the change at the block's region.
     * If the scheduler is unavailable (legacy servers), falls back to a sync Bukkit task.
     * </p>
     *
     * @param block block to change
     * @param type  material to set
     */
    private void setBlockTypeNoPhysicsSafely(final Block block, final Material type) {
        if (block == null || type == null) return;

        final Runnable action = () -> {
            try {
                block.setType(type, false);
            } catch (Throwable ignored) {
            }
        };

        final var scheduler = plugin.getGravesXScheduler();
        if (scheduler != null) {
            scheduler.execute(block.getLocation(), action);
        } else {
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }
}