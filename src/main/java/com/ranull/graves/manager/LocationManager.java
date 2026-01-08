package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.MaterialUtil;
import dev.cwhead.GravesX.manager.ChunkManager;
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
import java.util.Map;
import java.util.UUID;
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

        World ew = entity.getWorld();
        if (!w.equals(ew)) return null;

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
            if (plugin.getConfigManager().getConfigSection("teleport.unsafe", grave).getBoolean("teleport.unsafe")
                    || isLocationSafePlayer(location)) {
                return location;
            } else if (plugin.getConfigManager().getConfigSection("teleport.top", grave).getBoolean("teleport.top")) {
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

        if (isLocationSafeGraveAndPlayer(location)) {
            return LocationUtil.roundLocation(location);
        }

        if (isVoid(location) || isInsideBorder(location) || isNetherRoofPlacement(location, grave)) {
            return null;
        }

        Block block = location.getBlock();

        if (MaterialUtil.isLava(block.getType())) {
            Location lavaTop = getLavaTop(location, livingEntity, grave);
            if (isLocationSafeGraveAndPlayer(lavaTop)) {
                return LocationUtil.roundLocation(lavaTop);
            }
        }

        boolean airOrWater = MaterialUtil.isAir(block.getType()) || MaterialUtil.isWater(block.getType());
        boolean useGround = plugin.getConfigManager()
                .getConfigSection("placement.ground", grave)
                .getBoolean("placement.ground");

        Location graveLocation;

        if (airOrWater) {
            graveLocation = useGround ? getGround(location, livingEntity, grave) : null;
        } else {
            graveLocation = getRoof(location, livingEntity, grave);
        }

        if (graveLocation == null && airOrWater && useGround) {
            graveLocation = findGround(location);
        }

        if (isLocationSafeGraveAndPlayer(graveLocation)) {
            return LocationUtil.roundLocation(graveLocation);
        }

        return null;
    }

    /**
     * Determines if a location has a grave using the cache manager grave map.
     *
     * @param location The location.
     * @return True if the location has a cached grave, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasCachedGraveAt(Location location) {
        if (location == null) return false;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null) return false;

        World world = rounded.getWorld();
        if (world == null) return false;

        final int x = rounded.getBlockX();
        final int y = rounded.getBlockY();
        final int z = rounded.getBlockZ();

        Map<UUID, Grave> graveMap = plugin.getCacheManager().getGraveMap();
        if (graveMap == null || graveMap.isEmpty()) return false;

        for (Grave grave : graveMap.values()) {
            if (grave == null) continue;

            Location gl = grave.getLocationDeath();
            if (gl == null) continue;

            Location gr = LocationUtil.roundLocation(gl);
            if (gr == null) continue;

            World gw = gr.getWorld();
            if (gw == null || !gw.equals(world)) continue;

            if (gr.getBlockX() == x && gr.getBlockY() == y && gr.getBlockZ() == z) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a new location if a cached grave already exists at the provided location.
     *
     * <p>This does NOT update the cache or the grave. It only computes a new location that can be used
     * by the caller to move/place the grave elsewhere.</p>
     *
     * @param livingEntity The living entity (used for safe-location logic).
     * @param location     The desired location.
     * @param grave        The grave being placed/moved.
     * @return A new safe location if the desired location is occupied; otherwise the original (rounded) location.
     *         Returns {@code null} if the location is occupied and no suitable alternative could be found.
     */
    public Location getNewLocationIfCachedGraveExists(LivingEntity livingEntity, Location location, Grave grave) {
        if (location == null) return null;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null || rounded.getWorld() == null) return null;

        if (!hasCachedGraveAt(rounded)) {
            return rounded;
        }

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

        return null;
    }

    /**
     * Checks whether placing a grave at the given location would put it on the Nether roof.
     *
     * @param location the candidate grave location (entity/grave position, not block center-only)
     * @return {@code true} if this would be considered a Nether roof placement, otherwise {@code false}
     */
    public boolean isNetherRoofPlacement(Location location, Grave grave) {
        if (location == null) return false;

        World world = location.getWorld();
        if (world == null || world.getEnvironment() != World.Environment.NETHER) {
            return false;
        }

        if (plugin.getConfigManager().getConfigSection("placement.nether-roof", grave).getBoolean("placement.nether-roof", false)) {
            return false;
        }

        return location.getBlockY() >= 127;
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
        if (world == null) return null;

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

        return null;
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
        if (world == null) return null;

        int startY = world.getMaxHeight();
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
        if (location == null || location.getWorld() == null) return null;
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
        if (location == null || location.getWorld() == null) return null;
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
        if (world == null) return null;

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
        if (world == null) return null;

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

        if (plugin.getConfigManager().getConfigSection("placement.lava-smart", grave).getBoolean("placement.lava-smart")) {
            Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);
            if (solidLocation != null && solidLocation.getWorld() != null) {
                if (!hasGrave(solidLocation)) {
                    return solidLocation;
                } else {
                    Location up = solidLocation.clone().add(0, 1, 0);
                    if (up.getWorld() != null && isLocationSafeGrave(up) && !hasGrave(up)) return up;
                }
            }
        }

        if (plugin.getConfigManager().getConfigSection("placement.lava-top", grave).getBoolean("placement.lava-top")) {
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

        if (plugin.getConfigManager().getConfigSection("placement.water-top", grave).getBoolean("placement.water-top")) {
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

        if (plugin.getConfigManager().getConfigSection("placement.water-smart", grave).getBoolean("placement.water-smart")) {
            Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);
            if (solidLocation != null && solidLocation.getWorld() != null) {
                if (!hasGrave(solidLocation)) {
                    return solidLocation;
                } else {
                    Location up = solidLocation.clone().add(0, 1, 0);
                    if (up.getWorld() != null && isLocationSafeGrave(up) && !hasGrave(up)) return up;
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
        if (location == null) return true;

        Plugin landProtectionAddonPlugin = plugin.getServer().getPluginManager().getPlugin("GravesXAddon-LandProtection");
        if (landProtectionAddonPlugin != null && landProtectionAddonPlugin.isEnabled()) return true;

        if (livingEntity instanceof Player player) {
            return (!plugin.getConfigManager().getConfigSection("placement.can-build", player, permissionList).getBoolean("placement.can-build")
                    || plugin.getCompatibility().canBuild(player, location, plugin))
                    && (!plugin.getIntegrationManager().hasProtectionLib()
                    || (!plugin.getConfigManager().getConfigSection("placement.can-build-protectionlib", player, permissionList)
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
     * and must not already contain a grave. Location must also be within world bounds
     * and not in the void.
     *
     * @param location The candidate location.
     * @return True if the location is safe for both grave and player and has no grave.
     */
    private boolean isLocationSafeGraveAndPlayer(Location location) {
        if (location == null) return false;
        location = LocationUtil.roundLocation(location);
        if (location == null || location.getWorld() == null) return false;

        if (isVoid(location) || !isInsideBorder(location)) {
            return false;
        }

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

        if (!plugin.getDataManager().hasChunkData(rounded)) return false;

        var chunkData = plugin.getDataManager().getChunkData(rounded);
        if (chunkData == null || chunkData.getBlockDataMap() == null) return false;

        return chunkData.getBlockDataMap().containsKey(rounded);
    }

    /**
     * Determines if a location is inside the world border.
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

        World world = location.getWorld();
        int y = location.getBlockY();

        int minY = getMinHeight(location);
        int maxY = world.getMaxHeight();

        return y <= minY || y >= maxY;
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

        var scheduler = plugin.getSchedulerManager();
        if (scheduler != null) {
            scheduler.execute(block.getLocation(), action);
        } else {
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }
}