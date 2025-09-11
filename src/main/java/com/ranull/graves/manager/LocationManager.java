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
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Random;

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

        if (location.getWorld() == null) {
            return getVoid(location, livingEntity, grave);
        }

        Block block = location.getBlock();

        if (isLocationSafeGrave(location)) {
            return getGround(location, livingEntity, grave);
        }

        Random random = new Random();
        int attempts = 10;
        while (attempts > 0) {
            int randomX = random.nextInt(3) - 1; // Generates -1, 0, or 1
            int randomZ = random.nextInt(3) - 1;

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
     *
     * Searches downward from the starting location until a solid block is found or the search limit is reached.
     *
     * @param location The starting location.
     * @return The location on solid ground, or the original location if no ground is found within the search limit.
     */
    private Location findGround(Location location) {
        if (location.getWorld() == null) {
            return location;
        }

        Location groundLocation = location.clone();
        int maxSearchDistance = groundLocation.getWorld().getMaxHeight() - 1;

        for (int counter = 0; counter < maxSearchDistance; counter++) {
            Block blockBelow = groundLocation.getBlock().getRelative(BlockFace.DOWN);

            if (MaterialUtil.isSafeSolid(blockBelow.getType())) {
                return groundLocation;
            }

            groundLocation.subtract(0, 1, 0);
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
        if (location.getWorld() == null) {
            return null;
        }

        int startY = location.getWorld().getMaxHeight();

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
    public Location getRoof(Location location, Entity entity, Grave grave) {
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
        if (location.getWorld() == null) {
            return null;
        }

        World world = location.getWorld();
        int minY = getMinHeight(location);

        Location checkLoc = location.clone();
        checkLoc.setY(y);

        boolean allowNetherRoof = plugin.getConfig("placement.nether-roof", grave)
                .getBoolean("placement.nether-roof");
        if (world.getEnvironment() == World.Environment.NETHER && !allowNetherRoof && checkLoc.getY() > 126) {
            checkLoc.setY(126);
        }

        while (checkLoc.getY() >= minY) {
            Material blockType = checkLoc.getBlock().getType();

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
    private Location findLocationUpFromY(Location location, Entity entity, int y, Grave grave) {
        if (location.getWorld() == null) {
            return null;
        }

        World world = location.getWorld();
        int maxY = world.getMaxHeight();

        Location checkLoc = location.clone();
        checkLoc.setY(y);

        while (checkLoc.getY() <= maxY) {
            Material blockType = checkLoc.getBlock().getType();

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

        if (location.getWorld() != null) {
            World.Environment environment = location.getWorld().getEnvironment();

            boolean skipRoof = (environment == World.Environment.NETHER)
                    && !plugin.getConfig("placement.nether-roof", grave).getBoolean("placement.nether-roof");

            if (!skipRoof) {
                Location roof = getRoof(location, entity, grave);
                if (roof != null) {
                    return roof;
                }
            }

            // Final fallback to bottom of world
            int minY = getMinHeight(location);
            Location bottom = new Location(location.getWorld(), location.getX(), minY, location.getZ());

            // Optional: ensure it's not inside bedrock or unsafe
            Block block = bottom.getBlock();
            if (MaterialUtil.isAir(block.getType()) || !block.getType().isSolid()) {
                bottom.setY(minY + 1);
            }

            return bottom;
        }

        return null;
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
                return !hasGrave(solidLocation) ? solidLocation : getRoof(solidLocation, entity, grave);
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
                return !hasGrave(solidLocation) ? solidLocation : getRoof(solidLocation, entity, grave);
            }
        }

        if (plugin.getConfig("placement.water-top", grave).getBoolean("placement.water-top")) {
            Location checkLoc = location.clone();

            if (checkLoc.getWorld() != null) {
                int maxHeight = checkLoc.getWorld().getMaxHeight();

                // Search upwards until we reach a block that is no longer water
                while (checkLoc.getBlock().getType() == Material.WATER && checkLoc.getY() < maxHeight) {
                    checkLoc.add(0, 1, 0);
                }

                // Once we exit the water, check if the space above is air and suitable for placement
                while (checkLoc.getY() < maxHeight) {
                    Block block = checkLoc.getBlock();

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
    public boolean canBuild(LivingEntity livingEntity, Location location, List<String> permissionList) {
        Plugin landProtectionAddonPlugin = plugin.getServer().getPluginManager().getPlugin("GravesXAddon-LandProtection");
        if (landProtectionAddonPlugin != null && landProtectionAddonPlugin.isEnabled()) return true;
        if (livingEntity instanceof Player) {
            Player player = (Player) livingEntity;

            return (!plugin.getConfig("placement.can-build", player, permissionList)
                    .getBoolean("placement.can-build")
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
        location = LocationUtil.roundLocation(location);
        if (location == null) return false;
        Block block = location.getBlock();

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
}

