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

/**
 * Manages location-related operations for graves.
 */
public class LocationManager {
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
        plugin.getCacheManager().getLastLocationMap().put(entity.getUniqueId(), location);
    }

    /**
     * Gets the last solid location of an entity.
     *
     * @param entity The entity.
     * @return The last solid location.
     */
    public Location getLastSolidLocation(Entity entity) {
        Location location = plugin.getCacheManager().getLastLocationMap().get(entity.getUniqueId());

        return location != null && location.getWorld() != null
                && location.getWorld().equals(entity.getWorld())
                && location.getBlock().getRelative(BlockFace.DOWN).getType().isSolid() ? location : null;
    }

    /**
     * Removes the last solid location of an entity.
     *
     * @param entity The entity.
     */
    public void removeLastSolidLocation(Entity entity) {
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
    public Location getSafeTeleportLocation(Entity entity, Location location, Grave grave, Graves plugin) {
        if (location.getWorld() != null) {
            if (plugin.getConfig("teleport.unsafe", grave).getBoolean("teleport.unsafe")
                    || isLocationSafePlayer(location)) {
                return location;
            } else if (plugin.getConfig("teleport.top", grave).getBoolean("teleport.top")) {
                Location topLocation = getTop(location, entity, grave);

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
    public Location getSafeGraveLocation(LivingEntity livingEntity, Location location, Grave grave) {
        location = LocationUtil.roundLocation(location);

        if (location.getWorld() != null) {
            Block block = location.getBlock();

            if (!hasGrave(location) && isLocationSafeGrave(location)) {
                return location;
            } else {
                if (isVoid(location) || !isInsideBorder(location)) {
                    return getVoid(location, livingEntity, grave);
                } else if (MaterialUtil.isLava(block.getType())) {
                    return getLavaTop(location, livingEntity, grave);
                } else {
                    Location graveLocation = (MaterialUtil.isAir(block.getType())
                            || MaterialUtil.isWater(block.getType()))
                            ? (plugin.getConfig("placement.ground", grave)
                            .getBoolean("placement.ground") ? getGround(location, livingEntity, grave) : null)
                            : getRoof(location, livingEntity, grave);

                    if (graveLocation != null) {
                        return graveLocation;
                    }
                }
            }
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
     * @param location The base location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The found top location, or null if no suitable location is found.
     */
    public Location getTop(Location location, Entity entity, Grave grave) {
        return findLocationDownFromY(location, entity, location.getWorld() != null
                ? location.getWorld().getMaxHeight() : location.getBlockY(), grave);
    }

    /**
     * Gets the roof location for placement.
     *
     * @param location The location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The roof location.
     */
    public Location getRoof(Location location, Entity entity, Grave grave) {
        return findLocationUpFromY(location, entity, location.getBlockY() + 1, grave);
    }

    /**
     * Gets the ground location for placement.
     *
     * @param location The location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The ground location.
     */
    public Location getGround(Location location, Entity entity, Grave grave) {
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
    private Location findLocationDownFromY(Location location, Entity entity, int y, Grave grave) {
        location = location.clone();
        int counter = 0;

        location.setY(y);

        if (location.getWorld() != null) {
            while (counter <= (getMinHeight(location) * -1) + location.getWorld().getMaxHeight()) {
                if (MaterialUtil.isLava(location.getBlock().getType())) {
                    return getLavaTop(location, entity, grave);
                } else if (isLocationSafeGrave(location) && !hasGrave(location)) {
                    return location;
                }

                location.subtract(0, 1, 0);
                counter++;
            }
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
    private Location findLocationUpFromY(Location location, Entity entity, int y, Grave grave) {
        if (location.getWorld() == null) return null;

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
     * @param location The location.
     * @param entity   The entity.
     * @param grave    The grave.
     * @return The void location.
     */
    public Location getVoid(Location location, Entity entity, Grave grave) {
        if (!plugin.getConfig("placement.void", grave).getBoolean("placement.void")) {
            return null;
        }

        location = location.clone();

        if (plugin.getConfig("placement.void-smart", grave).getBoolean("placement.void-smart")) {
            Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);
            if (solidLocation != null) {
                return !hasGrave(solidLocation) ? solidLocation : getRoof(solidLocation, entity, grave);
            }
        }

        if (location.getWorld() == null) return null;

        World world = location.getWorld();
        World.Environment environment = world.getEnvironment();

        if (environment == World.Environment.THE_END) {
            Location endCandidate = endVoidScan(location, grave);
            if (endCandidate != null) return endCandidate;
        }

        boolean skipRoof = (environment == World.Environment.NETHER) && !plugin.getConfig("placement.nether-roof", grave).getBoolean("placement.nether-roof");

        if (!skipRoof) {
            Location roof = getRoof(location, entity, grave);
            if (roof != null) return roof;
        }

        int minY = getMinHeight(location);
        Location bottom = new Location(world, location.getX(), minY, location.getZ());
        Block block = bottom.getBlock();
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
    private Location endVoidScan(Location location, Grave grave) {
        World world = location.getWorld();
        if (world == null) return null;

        int minY = getMinHeight(location);
        int originX = location.getBlockX();
        int originZ = location.getBlockZ();

        boolean columnHasLand = false;
        for (int y = Math.min(location.getBlockY(), world.getMaxHeight() - 1); y >= minY; y--) {
            Material m = world.getBlockAt(originX, y, originZ).getType();
            if (!MaterialUtil.isAir(m) && m.isSolid()) { columnHasLand = true; break; }
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

        int fallbackY = plugin.getConfig("placement.end.fallback-y", grave).getInt("placement.end.fallback-y", Math.max(64, minY + 1));
        Block support = world.getBlockAt(originX, fallbackY, originZ);
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
    public Location getLavaTop(Location location, Entity entity, Grave grave) {
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

            if (checkLoc.getWorld() != null) {
                int maxHeight = checkLoc.getWorld().getMaxHeight();

                while (checkLoc.getBlock().getType() == Material.LAVA && checkLoc.getY() < maxHeight) {
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
    public Location getWaterTop(Location location, Entity entity, Grave grave) {
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

        if (plugin.getConfig("placement.water-top", grave).getBoolean("placement.water-top")) {
            Location checkLoc = location.clone();

            if (checkLoc.getWorld() != null) {
                int maxHeight = checkLoc.getWorld().getMaxHeight();

                while (checkLoc.getBlock().getType() == Material.WATER && checkLoc.getY() < maxHeight) {
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
     * A location is considered safe if:
     * - It is inside the world border.
     * - The current block and block above are not solid or lava.
     * - The block below is solid and not lava.
     *
     * @param location The location to check.
     * @return True if the location is safe; otherwise, false.
     */
    public boolean isLocationSafePlayer(Location location) {
        if (!isInsideBorder(location)) {
            return false;
        }

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
     * Checks whether the given blocks are bedrock or the block below is surrounded by bedrock.
     *
     * @param block The main block at the grave location.
     * @param below The block directly below the grave location.
     * @return True if the block or its surroundings are bedrock-related; otherwise false.
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
    public boolean isInsideBorder(Location location) {
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
     * A location is considered in the void if it's below the world's minimum height
     * or above its maximum build height.
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