package dev.cwhead.GravesX.manager;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves safe teleport and grave placement locations.
 * Prefers fluid surface placement when configured.
 */
public final class SafeLocationManager {

    private final Graves plugin;
    private static final List<String> DEFAULT_POWDER_SNOW_BLOCKS = List.of(
            "POWDER_SNOW"
    );
    private static final List<String> DEFAULT_NON_FULL_SUPPORT_BLOCKS = List.of(
            "*_SLAB",
            "*_CARPET",
            "STEP",
            "DOUBLE_STEP",
            "CARPET",
            "LEGACY_STEP",
            "LEGACY_DOUBLE_STEP",
            "LEGACY_CARPET",
            "SCAFFOLDING",
            "LEGACY_SCAFFOLDING",
            "TRIPWIRE",
            "TRIPWIRE_HOOK",
            "LEGACY_TRIPWIRE",
            "LEGACY_TRIPWIRE_HOOK"
    );
    private static final List<String> DEFAULT_REPLACE_AVOIDED_BLOCKS = List.of(
            "*_SLAB",
            "*_CARPET",
            "*_PRESSURE_PLATE",
            "*_BUTTON",
            "*_SIGN",
            "*_WALL_SIGN",
            "*_BANNER",
            "*_WALL_BANNER",
            "*_TORCH",
            "STEP",
            "DOUBLE_STEP",
            "CARPET",
            "LEGACY_STEP",
            "LEGACY_DOUBLE_STEP",
            "LEGACY_CARPET",
            "SNOW",
            "LEGACY_SNOW",
            "REDSTONE_WIRE",
            "LEGACY_REDSTONE_WIRE",
            "TORCH",
            "LEGACY_TORCH"
    );

    public SafeLocationManager(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Explains why a particular grave placement location was chosen.
     */
    public enum GravePlacementReason {

        /**
         * Tripped when the original death location is already clear, inside the border,
         * not fluid, has safe solid support below, and does not already contain a grave.
         * Causes the grave to be placed at the original rounded death location without
         * running additional vertical fallback searches.
         */
        ORIGINAL_SAFE,

        /**
         * Tripped when upward vertical scanning finds the closest valid grave location.
         * Causes the grave to be placed above the original or candidate base location.
         */
        ROOF,

        /**
         * Tripped when downward vertical scanning finds the closest valid grave location.
         * Causes the grave to be placed below the original or candidate base location.
         */
        GROUND,

        /**
         * Tripped when the death was in water, or directly above water, and the player's
         * cached last-solid location is still clear and valid. Causes the grave to be
         * placed at that last-solid location before water surface or bottom fallbacks run.
         */
        WATER_SMART,

        /**
         * Tripped when the death location is air directly above water and that air block
         * can safely hold the grave. Causes the grave to be placed at the death block,
         * above the water, instead of scanning through the water column.
         */
        WATER_ABOVE,

        /**
         * Tripped when water-top placement is enabled and a clear air block is found
         * above the contiguous water column. Causes the grave to be placed on the water
         * surface rather than at the underwater death location.
         */
        WATER_TOP,

        /**
         * Tripped when water-bottom placement is enabled and the bottom of the contiguous
         * water column is safe for grave placement. Causes the grave to be placed at the
         * bottom water block location.
         */
        WATER_BOTTOM,

        /**
         * Tripped when the death was in lava, or directly above lava, and the player's
         * cached last-solid location is still clear and valid. Causes the grave to be
         * placed at that last-solid location before lava surface fallback runs.
         */
        LAVA_SMART,

        /**
         * Tripped when the death location is air directly above lava and that air block
         * can safely hold the grave. Causes the grave to be placed at the death block,
         * above the lava, instead of scanning through the lava column.
         */
        LAVA_ABOVE,

        /**
         * Tripped when lava-top placement is enabled and a clear air block is found above
         * the contiguous lava column. Causes the grave to be placed on the lava surface
         * rather than at the lava death location.
         */
        LAVA_TOP,

        /**
         * Tripped when both the death block and the block above it are solid, indicating
         * a two-block wall suffocation death. Causes the grave to use a cleared last-solid
         * location first, then the closest valid location above or below the suffocation
         * column.
         */
        SUFFOCATION,

        /**
         * Tripped when the death block or the block below it is powder snow. Causes the
         * grave to use a cleared last-solid location first, then the first clear block
         * above the contiguous powder snow column.
         */
        POWDER_SNOW,

        /**
         * Tripped when the block below the death location is a non-full support, such as
         * a slab, carpet, scaffolding, or tripwire. Causes the grave to be placed in the
         * clear block above that support instead of treating the support as normal full
         * ground.
         */
        NON_FULL_SUPPORT,

        /**
         * Tripped when the death block itself is solid, but the case is not fluid, powder
         * snow, or two-block suffocation. Causes the grave to be placed at the closest
         * valid location above or below the blocked feet position.
         */
        FEET_BLOCKED,

        /**
         * Tripped when the death block is clear but the block above it is solid. Causes
         * the grave to be placed at the closest valid location above or below the blocked
         * head position.
         */
        HEAD_BLOCKED,

        /**
         * Tripped when the death block is a small or special block that should be
         * preserved, such as carpet, snow layer, pressure plate, sign, banner, redstone
         * wire, button, or torch. Causes the grave to be placed one block above when that
         * space is clear.
         */
        REPLACE_AVOIDED,

        /**
         * Tripped when the original death location is above the world's maximum build
         * height. Causes the grave to be placed at the first valid location found by
         * scanning downward from the build limit.
         */
        ABOVE_BUILD_LIMIT,

        /**
         * Tripped when the death is above the configured Nether roof limit and Nether roof
         * placement fallback is required. Causes the grave to be placed on or above the
         * roof fallback location when below-roof placement is not available.
         */
        NETHER_ROOF,

        /**
         * Tripped when the death is in the void or outside the world border and a valid
         * location is found in the same X/Z column. Causes the grave to be placed at the
         * closest safe column location.
         */
        VOID_COLUMN,

        /**
         * Tripped when the death is in the void or outside the world border and the best
         * valid same-column location is at the world's minimum height. Causes the grave
         * to be placed at that minimum-height safe location.
         */
        VOID_MIN_HEIGHT,

        /**
         * Tripped when no configured special-case, vertical, fluid, void, or border
         * placement candidate can be resolved. Causes the original rounded death location
         * to be returned as the final fallback.
         */
        FALLBACK_ORIGINAL
    }

    /**
     * Grave placement result.
     */
    public static class GravePlacementResult {
        /**
         * Grave placement location result.
         */
        private final Location location;
        /**
         * Grave placement reason result.
         */
        private final GravePlacementReason reason;

        /**
         * Private Grave Placement constructor.
         *
         * @param location The location.
         * @param reason   The reason.
         */
        private GravePlacementResult(Location location, GravePlacementReason reason) {
            this.location = location;
            this.reason = reason;
        }

        /**
         * Gets the location.
         *
         * @return The location.
         */
        public Location getLocation() {
            return location;
        }

        /**
         * Gets the reason.
         *
         * @return The reason.
         */
        public GravePlacementReason getReason() {
            return reason;
        }

        /**
         * Creates a result.
         *
         * @param location The location.
         * @param reason   The reason.
         * @return The result.
         */
        public static GravePlacementResult of(Location location, GravePlacementReason reason) {
            return new GravePlacementResult(location, reason);
        }
    }

    /**
     * Placement candidate.
     */
    private static final class Candidate {
        /**
         * Location for a candidate.
         */
        final Location loc;
        /**
         * Grave Placement reason for a candidate.
         */
        final GravePlacementReason reason;

        /**
         * Private Candidate constructor.
         *
         * @param loc    The location.
         * @param reason The reason.
         */
        private Candidate(Location loc, GravePlacementReason reason) {
            this.loc = loc;
            this.reason = reason;
        }
    }

    /**
     * Returns the configured Nether roof Y limit for the given world.
     */
    public int getNetherRoofLimit(World world, Grave grave) {
        return getNetherRoofYInternal(world, grave);
    }

    /**
     * Returns whether the location is above the configured Nether roof limit.
     */
    public boolean isAboveNetherRoof(Location location, Grave grave) {
        return isAboveNetherRoofInternal(location, grave);
    }

    /**
     * Stores the last solid location for an entity.
     *
     * @param entity   The entity.
     * @param location The location.
     */
    public void setLastSolidLocation(Entity entity, Location location) {
        if (entity == null || location == null) return;
        plugin.getCacheManager().getLastLocationMap().put(entity.getUniqueId(), location.clone());
    }

    /**
     * Gets the last solid location for an entity.
     *
     * @param entity The entity.
     * @return The location, or null if not available.
     */
    public Location getLastSolidLocation(Entity entity) {
        if (entity == null) return null;

        Location location = plugin.getCacheManager().getLastLocationMap().get(entity.getUniqueId());
        if (location == null || location.getWorld() == null) return null;

        entity.getWorld();
        if (!location.getWorld().equals(entity.getWorld())) return null;

        Block feet = location.getBlock();
        if (MaterialUtil.isWater(feet.getType()) || MaterialUtil.isLava(feet.getType())) return null;

        Block below = location.getBlock().getRelative(BlockFace.DOWN);
        if (!below.getType().isSolid()) return null;

        return location.clone();
    }

    /**
     * Removes the last solid location for an entity.
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
     * @param entity   The entity.
     * @param location The location.
     * @param grave    The grave.
     * @param plugin   The plugin.
     * @return The safe location, or null if not found.
     */
    public Location getSafeTeleportLocation(Entity entity, Location location, Grave grave, Graves plugin) {
        if (location == null || entity == null) return null;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null || rounded.getWorld() == null) return null;

        String prefix = "[SafeLocationManager] getSafeTeleportLocation: ";

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
     * Resolves the highest safe teleport location in the same column.
     *
     * @param base The base location.
     * @return The safe location, or null if not found.
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
            if (isLocationSafePlayer(candidate)) return candidate;
        }

        return null;
    }

    /**
     * Gets a safe grave location.
     *
     * @param livingEntity The entity.
     * @param location     The location.
     * @param grave        The grave.
     * @return The safe location, or null if not found.
     */
    public Location getSafeGraveLocation(LivingEntity livingEntity, Location location, Grave grave) {
        GravePlacementResult r = resolveSafeGravePlacement(livingEntity, location, grave);
        return r != null ? r.getLocation() : null;
    }

    /**
     * Resolves a safe grave placement result.
     *
     * @param livingEntity The entity.
     * @param location     The location.
     * @param grave        The grave.
     * @return The placement result, or null if the input location is null.
     */
    public GravePlacementResult resolveSafeGravePlacement(LivingEntity livingEntity, Location location, Grave grave) {
        String prefix = "[SafeLocationManager] resolveSafeGravePlacement: ";

        if (location == null) {
            plugin.debugMessage(prefix + "input location is null; returning null.", 1);
            return null;
        }

        Location origin = LocationUtil.roundLocation(location);
        if (origin == null) origin = location.clone();

        if (origin.getWorld() == null) {
            plugin.debugMessage(prefix + "origin world is null; returning original as-is.", 1);
            return GravePlacementResult.of(origin, GravePlacementReason.FALLBACK_ORIGINAL);
        }

        plugin.debugMessage(prefix + "origin=" + fmtLoc(origin) + " block=" + origin.getBlock().getType(), 1);

        boolean useGround = plugin.getConfigManager().getConfigSection("placement.ground", grave).getBoolean("placement.ground");
        boolean useRoof = plugin.getConfigManager().getConfigSection("placement.roof", grave).getBoolean("placement.roof");
        boolean allowNetherRoof = plugin.getConfigManager().getConfigSection("placement.nether-roof", grave).getBoolean("placement.nether-roof");

        World world = origin.getWorld();
        int worldMaxY = world.getMaxHeight() - 1;

        if (origin.getBlockY() > worldMaxY) {
            Location scanBase = new Location(world, origin.getBlockX() + 0.5, worldMaxY, origin.getBlockZ() + 0.5, origin.getYaw(), origin.getPitch());
            Location down = searchDownForSafeGrave(scanBase, grave, worldMaxY);
            if (down != null) {
                plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.ABOVE_BUILD_LIMIT + " loc=" + fmtLoc(down) + " distSq=" + distSq(scanBase, down) + ".", 1);
                return GravePlacementResult.of(down, GravePlacementReason.ABOVE_BUILD_LIMIT);
            }
            plugin.debugMessage(prefix + "origin above build limit; no safe placement found below build limit; continuing.", 1);
        }

        if (isNether(world) && isAboveNetherRoofInternal(origin, grave) && !allowNetherRoof && !useRoof) {
            int roofY = getNetherRoofYInternal(world, grave);
            plugin.debugMessage(prefix + "nether-roof handling: roofLimitY=" + roofY + " roof=false nether-roof=false.", 1);

            Location scanStart = new Location(world, origin.getBlockX() + 0.5, roofY, origin.getBlockZ() + 0.5, origin.getYaw(), origin.getPitch());

            Location below = searchDownForSafeGrave(scanStart, grave, roofY);
            if (below != null) {
                plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.GROUND + " loc=" + fmtLoc(below) + " distSq=" + distSq(origin, below) + ".", 1);
                return GravePlacementResult.of(below, GravePlacementReason.GROUND);
            }

            int roofTopY = Math.min(worldMaxY, roofY + 1);
            Location roofTop = new Location(world, origin.getBlockX() + 0.5, roofTopY, origin.getBlockZ() + 0.5, origin.getYaw(), origin.getPitch());

            if (!hasGrave(roofTop) && isLocationSafeGrave(roofTop)) {
                plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.NETHER_ROOF + " loc=" + fmtLoc(roofTop) + " distSq=" + distSq(origin, roofTop) + ".", 1);
                return GravePlacementResult.of(roofTop, GravePlacementReason.NETHER_ROOF);
            }

            Location roofUp = searchUpForSafeGrave(roofTop, roofTop, grave, roofTop.getBlockY() + 1);
            if (roofUp != null) {
                plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.NETHER_ROOF + " loc=" + fmtLoc(roofUp) + " distSq=" + distSq(origin, roofUp) + ".", 1);
                return GravePlacementResult.of(roofUp, GravePlacementReason.NETHER_ROOF);
            }

            plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.NETHER_ROOF + " loc=" + fmtLoc(origin) + " distSq=0.0.", 1);
            return GravePlacementResult.of(origin, GravePlacementReason.NETHER_ROOF);
        }

        Location powderSnow = resolvePowderSnowPlacement(livingEntity, origin, grave);
        if (powderSnow != null) {
            plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.POWDER_SNOW + " loc=" + fmtLoc(powderSnow) + " distSq=" + distSq(origin, powderSnow) + ".", 1);
            return GravePlacementResult.of(powderSnow, GravePlacementReason.POWDER_SNOW);
        }

        Candidate nonFullSupport = resolveNonFullSupportPlacement(origin, grave);
        if (nonFullSupport != null) {
            plugin.debugMessage(prefix + "CHOSEN=" + nonFullSupport.reason + " loc=" + fmtLoc(nonFullSupport.loc) + " distSq=" + distSq(origin, nonFullSupport.loc) + ".", 1);
            return GravePlacementResult.of(nonFullSupport.loc, nonFullSupport.reason);
        }

        if (!hasGrave(origin) && isLocationSafeGrave(origin)) {
            Material t = origin.getBlock().getType();
            if (!MaterialUtil.isWater(t) && !MaterialUtil.isLava(t)) {
                plugin.debugMessage(prefix + "origin already safe+empty (non-fluid); using ORIGINAL_SAFE.", 1);
                return GravePlacementResult.of(origin, GravePlacementReason.ORIGINAL_SAFE);
            }
        }

        List<Candidate> candidates = new ArrayList<>();

        if (isVoid(origin) || !isInsideBorder(origin)) {
            plugin.debugMessage(prefix + "origin is void/outside border; computing void candidates only.", 1);
            addVoidCandidates(candidates, origin, grave);
            debugCandidates(prefix, origin, candidates);

            Candidate best = pickClosest(origin, candidates);
            if (best != null) {
                plugin.debugMessage(prefix + "CHOSEN=" + best.reason + " loc=" + fmtLoc(best.loc) + " distSq=" + distSq(origin, best.loc) + ".", 1);
                return GravePlacementResult.of(best.loc, best.reason);
            }

            plugin.debugMessage(prefix + "no void candidates; falling back to original.", 1);
            return GravePlacementResult.of(origin, GravePlacementReason.FALLBACK_ORIGINAL);
        }

        Material originType = origin.getBlock().getType();
        Material belowType = origin.getBlock().getRelative(BlockFace.DOWN).getType();

        boolean directlyAboveWater = MaterialUtil.isAir(originType) && MaterialUtil.isWater(belowType);
        boolean directlyAboveLava = MaterialUtil.isAir(originType) && MaterialUtil.isLava(belowType);

        boolean inWaterOrAbove = MaterialUtil.isWater(originType) || directlyAboveWater;
        boolean inLavaOrAbove = MaterialUtil.isLava(originType) || directlyAboveLava;
        boolean wallSuffocation = isPlacementEnabled("placement.suffocation", grave, true) && isWallSuffocation(origin);

        if (wallSuffocation) {
            Location lastSolid = resolveClearedLastSolid(livingEntity, origin, grave);
            if (lastSolid != null) {
                plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.SUFFOCATION + " loc=" + fmtLoc(lastSolid) + " distSq=" + distSq(origin, lastSolid) + ".", 1);
                return GravePlacementResult.of(lastSolid, GravePlacementReason.SUFFOCATION);
            }

            Location suffocation = resolveSuffocationLocation(origin, grave);
            if (suffocation != null) {
                plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.SUFFOCATION + " loc=" + fmtLoc(suffocation) + " distSq=" + distSq(origin, suffocation) + ".", 1);
                return GravePlacementResult.of(suffocation, GravePlacementReason.SUFFOCATION);
            }

            plugin.debugMessage(prefix + "wall suffocation detected but no above/below placement found; continuing.", 1);
        } else if (isPlacementEnabled("placement.feet-blocked", grave, true) && isFeetBlocked(origin, grave)) {
            Location feetBlocked = resolveBlockedPlacement(origin, grave, GravePlacementReason.FEET_BLOCKED);
            if (feetBlocked != null) {
                plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.FEET_BLOCKED + " loc=" + fmtLoc(feetBlocked) + " distSq=" + distSq(origin, feetBlocked) + ".", 1);
                return GravePlacementResult.of(feetBlocked, GravePlacementReason.FEET_BLOCKED);
            }
        } else if (isPlacementEnabled("placement.head-blocked", grave, true) && isHeadBlocked(origin, grave)) {
            Location headBlocked = resolveBlockedPlacement(origin, grave, GravePlacementReason.HEAD_BLOCKED);
            if (headBlocked != null) {
                plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.HEAD_BLOCKED + " loc=" + fmtLoc(headBlocked) + " distSq=" + distSq(origin, headBlocked) + ".", 1);
                return GravePlacementResult.of(headBlocked, GravePlacementReason.HEAD_BLOCKED);
            }
        } else if (inWaterOrAbove) {
            boolean waterSmart = plugin.getConfigManager()
                    .getConfigSection("placement.water-smart", grave)
                    .getBoolean("placement.water-smart");

            if (waterSmart) {
                Location smart = resolveSmartFromLastSolid(livingEntity, origin, grave, useGround, useRoof);
                if (smart != null) {
                    return GravePlacementResult.of(smart, GravePlacementReason.WATER_SMART);
                }
            }

            if (directlyAboveWater) {
                Location above = centerOnBlock(origin);
                if (above != null && !isVoid(above) && isInsideBorder(above) && !hasGrave(above) && isLocationSafeGraveAboveFluid(above, true)) {
                    plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.WATER_ABOVE + " loc=" + fmtLoc(above) + " distSq=" + distSq(origin, above) + ".", 1);
                    return GravePlacementResult.of(above, GravePlacementReason.WATER_ABOVE);
                }
            }

            boolean waterTop = plugin.getConfigManager().getConfigSection("placement.water-top", grave).getBoolean("placement.water-top");

            if (waterTop) {
                Location start = MaterialUtil.isWater(originType) ? origin : origin.clone().add(0.0, -1.0, 0.0);
                Location surface = resolveFluidSurfaceTop(start, grave, true);
                if (surface != null) {
                    plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.WATER_TOP + " loc=" + fmtLoc(surface) + " distSq=" + distSq(origin, surface) + ".", 1);
                    return GravePlacementResult.of(surface, GravePlacementReason.WATER_TOP);
                } else {
                    plugin.debugMessage(prefix + "water-top enabled but no AIR surface found; continuing.", 1);
                }
            }

            boolean waterBottom = plugin.getConfigManager().getConfigSection("placement.water-bottom", grave).contains("placement.water-bottom")
                    ? plugin.getConfigManager().getConfigSection("placement.water-bottom", grave).getBoolean("placement.water-bottom")
                    : useGround;

            if (waterBottom) {
                Location bottom = findWaterBottom(origin);
                if (bottom != null && !hasGrave(bottom) && isLocationSafeGrave(bottom)) {
                    plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.WATER_BOTTOM + " loc=" + fmtLoc(bottom) + " distSq=" + distSq(origin, bottom) + ".", 1);
                    return GravePlacementResult.of(bottom, GravePlacementReason.WATER_BOTTOM);
                }
            }

            if (useRoof) {
                Location roof = resolveRoofLocation(origin, origin, grave);
                if (roof != null) candidates.add(new Candidate(roof, GravePlacementReason.ROOF));
            }

            if (useGround) {
                Location ground = resolveGroundLocation(origin, origin, grave);
                if (ground != null) candidates.add(new Candidate(ground, GravePlacementReason.GROUND));
            }
        } else if (inLavaOrAbove) {
            boolean lavaSmart = plugin.getConfigManager()
                    .getConfigSection("placement.lava-smart", grave)
                    .getBoolean("placement.lava-smart");

            if (lavaSmart) {
                Location smart = resolveSmartFromLastSolid(livingEntity, origin, grave, useGround, useRoof);
                if (smart != null) {
                    return GravePlacementResult.of(smart, GravePlacementReason.LAVA_SMART);
                }
            }

            boolean lavaTop = plugin.getConfigManager().getConfigSection("placement.lava-top", grave).getBoolean("placement.lava-top");

            if (lavaTop) {
                if (directlyAboveLava) {
                    Location above = centerOnBlock(origin);
                    if (above != null && !isVoid(above) && isInsideBorder(above) && !hasGrave(above) && isLocationSafeGraveAboveFluid(above, false)) {
                        plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.LAVA_ABOVE + " loc=" + fmtLoc(above) + " distSq=" + distSq(origin, above) + ".", 1);
                        return GravePlacementResult.of(above, GravePlacementReason.LAVA_ABOVE);
                    }
                }

                Location start = MaterialUtil.isLava(originType) ? origin : origin.clone().add(0.0, -1.0, 0.0);
                Location surface = resolveFluidSurfaceTop(start, grave, false);
                if (surface != null) {
                    plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.LAVA_TOP + " loc=" + fmtLoc(surface) + " distSq=" + distSq(origin, surface) + ".", 1);
                    return GravePlacementResult.of(surface, GravePlacementReason.LAVA_TOP);
                }

                plugin.debugMessage(prefix + "lava-top enabled but no AIR surface found; continuing.", 1);
            }

            if (useRoof) {
                Location roof = resolveRoofLocation(origin, origin, grave);
                if (roof != null) candidates.add(new Candidate(roof, GravePlacementReason.ROOF));
            }

            if (useGround) {
                Location ground = resolveGroundLocation(origin, origin, grave);
                if (ground != null) candidates.add(new Candidate(ground, GravePlacementReason.GROUND));
            }
        } else {
            if (useRoof) {
                Location roof = resolveRoofLocation(origin, origin, grave);
                if (roof != null) candidates.add(new Candidate(roof, GravePlacementReason.ROOF));
            }

            if (useGround) {
                Location ground = resolveGroundLocation(origin, origin, grave);
                if (ground != null) candidates.add(new Candidate(ground, GravePlacementReason.GROUND));
            }
        }

        debugCandidates(prefix, origin, candidates);

        Candidate best = pickClosest(origin, candidates);
        if (best != null) {
            plugin.debugMessage(prefix + "CHOSEN=" + best.reason + " loc=" + fmtLoc(best.loc) + " distSq=" + distSq(origin, best.loc) + ".", 1);
            return GravePlacementResult.of(best.loc, best.reason);
        }

        plugin.debugMessage(prefix + "all candidates null; falling back to original.", 1);
        return GravePlacementResult.of(origin, GravePlacementReason.FALLBACK_ORIGINAL);
    }

    /**
     * Logs placement candidates for debugging.
     *
     * @param prefix     The message prefix.
     * @param origin     The origin location.
     * @param candidates The candidates.
     */
    private void debugCandidates(String prefix, Location origin, List<Candidate> candidates) {
        if (origin == null || origin.getWorld() == null) return;

        if (candidates == null || candidates.isEmpty()) {
            plugin.debugMessage(prefix + "candidates: (none)", 1);
            return;
        }

        plugin.debugMessage(prefix + "candidates:", 1);
        for (Candidate c : candidates) {
            if (c == null || c.loc == null || c.loc.getWorld() == null) continue;
            if (!Objects.equals(c.loc.getWorld(), origin.getWorld())) continue;

            plugin.debugMessage(prefix
                    + " - " + c.reason
                    + " loc=" + fmtLoc(c.loc)
                    + " distSq=" + distSq(origin, c.loc), 1);
        }
    }

    /**
     * Centers a location on its block.
     *
     * @param loc The location.
     * @return The centered location, or null if invalid.
     */
    private Location centerOnBlock(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        Location rounded = LocationUtil.roundLocation(loc);
        if (rounded == null || rounded.getWorld() == null) return null;

        return new Location(
                rounded.getWorld(),
                rounded.getBlockX() + 0.5,
                rounded.getBlockY(),
                rounded.getBlockZ() + 0.5,
                loc.getYaw(),
                loc.getPitch()
        );
    }

    /**
     * Adds water placement candidates.
     *
     * @param out          The candidate list.
     * @param origin       The origin location.
     * @param livingEntity The entity.
     * @param grave        The grave.
     * @param useGround    Whether ground placement is enabled.
     * @param useRoof      Whether roof placement is enabled.
     */
    private void addWaterCandidates(List<Candidate> out, Location origin, LivingEntity livingEntity, Grave grave, boolean useGround, boolean useRoof) {
        boolean waterSmart = plugin.getConfigManager().getConfigSection("placement.water-smart", grave).getBoolean("placement.water-smart");
        boolean waterBottom = plugin.getConfigManager().getConfigSection("placement.water-bottom", grave).contains("placement.water-bottom")
                ? plugin.getConfigManager().getConfigSection("placement.water-bottom", grave).getBoolean("placement.water-bottom")
                : useGround;

        if (waterSmart) {
            Location smart = resolveSmartFromLastSolid(livingEntity, origin, grave, useGround, useRoof);
            if (smart != null) out.add(new Candidate(smart, GravePlacementReason.WATER_SMART));
        }

        if (waterBottom) {
            Location bottom = findWaterBottom(origin);
            if (bottom != null && !hasGrave(bottom) && isLocationSafeGrave(bottom)) {
                out.add(new Candidate(bottom, GravePlacementReason.WATER_BOTTOM));
            }
        }
    }

    /**
     * Adds lava placement candidates.
     *
     * @param out          The candidate list.
     * @param origin       The origin location.
     * @param livingEntity The entity.
     * @param grave        The grave.
     * @param useGround    Whether ground placement is enabled.
     * @param useRoof      Whether roof placement is enabled.
     */
    private void addLavaCandidates(List<Candidate> out, Location origin, LivingEntity livingEntity, Grave grave, boolean useGround, boolean useRoof) {
        boolean lavaSmart = plugin.getConfigManager().getConfigSection("placement.lava-smart", grave).getBoolean("placement.lava-smart");

        if (lavaSmart) {
            Location smart = resolveSmartFromLastSolid(livingEntity, origin, grave, useGround, useRoof);
            if (smart != null) out.add(new Candidate(smart, GravePlacementReason.LAVA_SMART));
        }

        if (useRoof) {
            Location roof = resolveRoofLocation(origin, origin, grave);
            if (roof != null) out.add(new Candidate(roof, GravePlacementReason.ROOF));
        }

        if (useGround) {
            Location ground = resolveGroundLocation(origin, origin, grave);
            if (ground != null) out.add(new Candidate(ground, GravePlacementReason.GROUND));
        }
    }

    /**
     * Adds void placement candidates.
     *
     * @param out    The candidate list.
     * @param origin The origin location.
     * @param grave  The grave.
     */
    private void addVoidCandidates(List<Candidate> out, Location origin, Grave grave) {
        if (origin == null || origin.getWorld() == null) return;

        if (!plugin.getConfigManager().getConfigSection("placement.void", grave).getBoolean("placement.void")) {
            return;
        }

        World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        Candidate best = null;
        double bestDist = Double.MAX_VALUE;

        for (int y = minY; y <= maxY; y++) {
            Location c = new Location(world, x + 0.5, y, z + 0.5, origin.getYaw(), origin.getPitch());
            if (hasGrave(c) || !isLocationSafeGrave(c)) continue;

            double d = distSq(origin, c);
            if (d < bestDist) {
                bestDist = d;
                best = new Candidate(c, (y == minY) ? GravePlacementReason.VOID_MIN_HEIGHT : GravePlacementReason.VOID_COLUMN);
            }
        }

        if (best != null) out.add(best);
    }

    /**
     * Picks the closest candidate.
     *
     * @param origin     The origin location.
     * @param candidates The candidates.
     * @return The closest candidate, or null if none.
     */
    private Candidate pickClosest(Location origin, List<Candidate> candidates) {
        if (origin == null || origin.getWorld() == null) return null;
        if (candidates == null || candidates.isEmpty()) return null;

        Candidate best = null;
        double bestDist = Double.MAX_VALUE;

        for (Candidate c : candidates) {
            if (c == null || c.loc == null || c.loc.getWorld() == null) continue;
            if (!Objects.equals(c.loc.getWorld(), origin.getWorld())) continue;

            double d = distSq(origin, c.loc);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }

        return best;
    }

    /**
     * Gets the squared distance between two locations.
     *
     * @param a The first location.
     * @param b The second location.
     * @return The squared distance.
     */
    private double distSq(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return (dx * dx) + (dy * dy) + (dz * dz);
    }

    /**
     * Resolves placement for deaths in powder snow.
     *
     * @param livingEntity The entity.
     * @param origin       The death location.
     * @param grave        The grave.
     * @return The resolved location, or null if this is not a powder snow case.
     */
    private Location resolvePowderSnowPlacement(LivingEntity livingEntity, Location origin, Grave grave) {
        if (origin == null || origin.getWorld() == null) return null;
        if (!isPlacementEnabled("placement.powder-snow", grave, true)) return null;

        Block block = origin.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);

        if (!isPowderSnow(block.getType(), grave) && !isPowderSnow(below.getType(), grave)) return null;

        Location lastSolid = resolveClearedLastSolid(livingEntity, origin, grave);
        if (lastSolid != null) return lastSolid;

        Block start = isPowderSnow(block.getType(), grave) ? block : below;
        return resolvePowderSnowSurfaceTop(start.getLocation(), grave);
    }

    /**
     * Resolves the first clear block above a powder snow column.
     *
     * @param originInPowderSnow The location in powder snow.
     * @param grave              The grave.
     * @return The surface location, or null if not found.
     */
    private Location resolvePowderSnowSurfaceTop(Location originInPowderSnow, Grave grave) {
        if (originInPowderSnow == null || originInPowderSnow.getWorld() == null) return null;

        World world = originInPowderSnow.getWorld();
        int x = originInPowderSnow.getBlockX();
        int z = originInPowderSnow.getBlockZ();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        if (isNether(world) && !isAboveNetherRoofInternal(originInPowderSnow, grave)) {
            maxY = Math.min(maxY, getNetherRoofYInternal(world, grave));
        }

        int y = Math.max(originInPowderSnow.getBlockY(), minY);
        if (!isPowderSnow(world.getBlockAt(x, y, z).getType(), grave)) return null;

        while (y <= maxY && isPowderSnow(world.getBlockAt(x, y, z).getType(), grave)) {
            y++;
        }

        if (y < minY || y > maxY) return null;

        Location surface = new Location(world, x + 0.5, y, z + 0.5, originInPowderSnow.getYaw(), originInPowderSnow.getPitch());
        return isClearPlacementAboveSpecialBlock(surface) ? surface : null;
    }

    /**
     * Resolves placement above non-full support blocks without replacing them.
     *
     * @param origin The death location.
     * @return The placement candidate, or null if not applicable.
     */
    private Candidate resolveNonFullSupportPlacement(Location origin, Grave grave) {
        if (origin == null || origin.getWorld() == null) return null;

        Block block = origin.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);

        if (isPlacementEnabled("placement.replace-avoided", grave, true) && isReplacementAvoidedBlock(block.getType(), grave)) {
            Location above = centerOnBlock(origin.clone().add(0.0, 1.0, 0.0));
            if (isClearPlacementAboveSpecialBlock(above)) {
                return new Candidate(above, GravePlacementReason.REPLACE_AVOIDED);
            }
        }

        if (isPlacementEnabled("placement.non-full-support", grave, true) && isNonFullSupport(below.getType(), grave)) {
            Location aboveSupport = centerOnBlock(origin);
            if (isClearPlacementAboveSpecialBlock(aboveSupport)) {
                return new Candidate(aboveSupport, GravePlacementReason.NON_FULL_SUPPORT);
            }
        }

        return null;
    }

    /**
     * Checks if a block can receive a grave above a small or special support block.
     *
     * @param location The candidate location.
     * @return True if the candidate can be used.
     */
    private boolean isClearPlacementAboveSpecialBlock(Location location) {
        if (location == null || location.getWorld() == null) return false;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null || rounded.getWorld() == null) return false;

        return !isVoid(rounded)
                && isInsideBorder(rounded)
                && !hasGrave(rounded)
                && MaterialUtil.isSafeNotSolid(rounded.getBlock().getType());
    }

    /**
     * Checks whether the feet block is occupied but not already handled by special cases.
     *
     * @param location The death location.
     * @return True if the feet block is blocked.
     */
    private boolean isFeetBlocked(Location location, Grave grave) {
        if (location == null || location.getWorld() == null) return false;

        Material type = location.getBlock().getType();
        return type.isSolid()
                && !MaterialUtil.isWater(type)
                && !MaterialUtil.isLava(type)
                && !isPowderSnow(type, grave);
    }

    /**
     * Checks whether the head block is occupied while the feet block is otherwise clear.
     *
     * @param location The death location.
     * @return True if the head block is blocked.
     */
    private boolean isHeadBlocked(Location location, Grave grave) {
        if (location == null || location.getWorld() == null) return false;

        Block block = location.getBlock();
        Material feet = block.getType();
        Material head = block.getRelative(BlockFace.UP).getType();

        return MaterialUtil.isSafeNotSolid(feet)
                && !MaterialUtil.isWater(feet)
                && !MaterialUtil.isLava(feet)
                && !isPowderSnow(feet, grave)
                && head.isSolid()
                && !MaterialUtil.isLava(head);
    }

    /**
     * Resolves a placement above or below an occupied player-space block.
     *
     * @param origin The death location.
     * @param grave  The grave.
     * @param reason The placement reason.
     * @return The resolved location, or null if not found.
     */
    private Location resolveBlockedPlacement(Location origin, Grave grave, GravePlacementReason reason) {
        if (origin == null || origin.getWorld() == null) return null;

        List<Candidate> candidates = new ArrayList<>();

        Location above = searchUpForSafeGrave(origin, origin, grave, origin.getBlockY() + 1);
        if (above != null) candidates.add(new Candidate(above, reason));

        Location below = searchDownForSafeGrave(origin, grave, origin.getBlockY() - 1);
        if (below != null) candidates.add(new Candidate(below, reason));

        Candidate best = pickClosest(origin, candidates);
        return best != null ? best.loc : null;
    }

    /**
     * Checks whether a material is powder snow across supported versions.
     *
     * @param material The material.
     * @return True if powder snow.
     */
    private boolean isPowderSnow(Material material, Grave grave) {
        return materialMatchesConfiguredList(material, "placement.powder-snow-blocks", grave, DEFAULT_POWDER_SNOW_BLOCKS);
    }

    /**
     * Checks whether a block should be preserved by placing the grave above it.
     *
     * @param material The material.
     * @return True if the block should not be replaced directly.
     */
    private boolean isReplacementAvoidedBlock(Material material, Grave grave) {
        return materialMatchesConfiguredList(material, "placement.replace-avoided-blocks", grave, DEFAULT_REPLACE_AVOIDED_BLOCKS);
    }

    /**
     * Checks whether a material is a non-full support block.
     *
     * @param material The material.
     * @return True if non-full support.
     */
    private boolean isNonFullSupport(Material material, Grave grave) {
        return materialMatchesConfiguredList(material, "placement.non-full-support-blocks", grave, DEFAULT_NON_FULL_SUPPORT_BLOCKS);
    }

    /**
     * Checks whether a placement option is enabled.
     *
     * @param path         The config path.
     * @param grave        The grave.
     * @param defaultValue The value to use if the config path is missing.
     * @return True if enabled.
     */
    private boolean isPlacementEnabled(String path, Grave grave, boolean defaultValue) {
        var section = plugin.getConfigManager().getConfigSection(path, grave);
        return section.contains(path) ? section.getBoolean(path) : defaultValue;
    }

    /**
     * Checks whether a material matches a configured placement material list.
     *
     * @param material     The material.
     * @param path         The config path.
     * @param grave        The grave.
     * @param defaultNames The fallback names to use if the config path is missing or empty.
     * @return True if matched.
     */
    private boolean materialMatchesConfiguredList(Material material, String path, Grave grave, List<String> defaultNames) {
        if (material == null) return false;

        var section = plugin.getConfigManager().getConfigSection(path, grave);
        List<String> names = section.contains(path) ? section.getStringList(path) : defaultNames;
        if (names == null || names.isEmpty()) names = defaultNames;

        String materialName = material.name();
        for (String raw : names) {
            if (matchesMaterialPattern(materialName, raw)) return true;
        }

        return false;
    }

    /**
     * Checks whether a material name matches an exact or wildcard pattern.
     *
     * @param materialName The material name.
     * @param rawPattern   The raw configured pattern.
     * @return True if matched.
     */
    private boolean matchesMaterialPattern(String materialName, String rawPattern) {
        if (materialName == null || rawPattern == null) return false;

        String pattern = rawPattern.trim().toUpperCase();
        if (pattern.isEmpty()) return false;

        if ("*".equals(pattern)) return true;

        int star = pattern.indexOf('*');
        if (star < 0) return materialName.equals(pattern);

        String prefix = pattern.substring(0, star);
        String suffix = pattern.substring(star + 1);
        return materialName.startsWith(prefix) && materialName.endsWith(suffix);
    }

    /**
     * Checks whether the death location is inside two solid blocks.
     *
     * @param location The death location.
     * @return True if the current block and block above are solid.
     */
    private boolean isWallSuffocation(Location location) {
        if (location == null || location.getWorld() == null) return false;

        Block block = location.getBlock();
        return block.getType().isSolid()
                && block.getRelative(BlockFace.UP).getType().isSolid();
    }

    /**
     * Resolves the previously stored solid location only when it is now clear for direct placement.
     *
     * @param livingEntity  The entity.
     * @param deathLocation The death location.
     * @param grave         The grave.
     * @return The cleared last solid location, or null if unavailable.
     */
    private Location resolveClearedLastSolid(LivingEntity livingEntity, Location deathLocation, Grave grave) {
        if (!(livingEntity instanceof Player player)) return null;

        Location lastSolid = getLastSolidLocation(player);
        if (lastSolid == null || lastSolid.getWorld() == null) return null;

        if (deathLocation != null
                && deathLocation.getWorld() != null
                && deathLocation.getWorld().equals(lastSolid.getWorld())
                && isNether(lastSolid.getWorld())
                && !isAboveNetherRoofInternal(deathLocation, grave)
                && lastSolid.getBlockY() > getNetherRoofYInternal(lastSolid.getWorld(), grave)) {
            return null;
        }

        Location base = LocationUtil.roundLocation(lastSolid);
        if (base == null || base.getWorld() == null) return null;
        if (isVoid(base) || !isInsideBorder(base)) return null;

        return !hasGrave(base) && isLocationSafeGrave(base) ? base : null;
    }

    /**
     * Resolves a grave location above or below a wall suffocation point.
     *
     * @param origin The suffocation origin.
     * @param grave  The grave.
     * @return The resolved location, or null if not found.
     */
    private Location resolveSuffocationLocation(Location origin, Grave grave) {
        if (origin == null || origin.getWorld() == null) return null;

        List<Candidate> candidates = new ArrayList<>();

        Location above = searchUpForSafeGrave(origin, origin, grave, origin.getBlockY() + 2);
        if (above != null) candidates.add(new Candidate(above, GravePlacementReason.SUFFOCATION));

        Location below = searchDownForSafeGrave(origin, grave, origin.getBlockY() - 1);
        if (below != null) candidates.add(new Candidate(below, GravePlacementReason.SUFFOCATION));

        Candidate best = pickClosest(origin, candidates);
        return best != null ? best.loc : null;
    }

    /**
     * Resolves a placement from the last solid location.
     *
     * @param livingEntity  The entity.
     * @param deathLocation The death location.
     * @param grave         The grave.
     * @param useGround     Whether ground placement is enabled.
     * @param useRoof       Whether roof placement is enabled.
     * @return The resolved location, or null if not found.
     */
    private Location resolveSmartFromLastSolid(LivingEntity livingEntity, Location deathLocation, Grave grave, boolean useGround, boolean useRoof) {
        if (!(livingEntity instanceof Player player)) return null;

        Location lastSolid = getLastSolidLocation(player);
        if (lastSolid == null || lastSolid.getWorld() == null) return null;

        if (deathLocation != null
                && deathLocation.getWorld() != null
                && deathLocation.getWorld().equals(lastSolid.getWorld())
                && isNether(lastSolid.getWorld())
                && !isAboveNetherRoofInternal(deathLocation, grave)
                && lastSolid.getBlockY() > getNetherRoofYInternal(lastSolid.getWorld(), grave)) {
            return null;
        }

        Location base = LocationUtil.roundLocation(lastSolid);
        if (base == null || base.getWorld() == null) return null;

        if (isVoid(base) || !isInsideBorder(base)) return null;

        if (!hasGrave(base) && isLocationSafeGrave(base)) return base;

        if (useRoof) {
            Location roof = resolveRoofLocation(base, deathLocation != null ? deathLocation : base, grave);
            if (roof != null && !hasGrave(roof) && isLocationSafeGrave(roof)) return roof;
        }

        if (useGround) {
            Location ground = resolveGroundLocation(base, deathLocation != null ? deathLocation : base, grave);
            if (ground != null && !hasGrave(ground) && isLocationSafeGrave(ground)) return ground;
        }

        return null;
    }

    /**
     * Resolves a ground placement location.
     *
     * @param base   The base location.
     * @param origin The origin location.
     * @param grave  The grave.
     * @return The resolved location, or null if not found.
     */
    private Location resolveGroundLocation(Location base, Location origin, Grave grave) {
        if (base == null || base.getWorld() == null) return null;

        Location down = searchDownForSafeGrave(base, grave, base.getBlockY());
        if (down != null) return down;

        return searchUpForSafeGrave(base, origin, grave, base.getBlockY() + 1);
    }

    /**
     * Resolves a roof placement location.
     *
     * @param base   The base location.
     * @param origin The origin location.
     * @param grave  The grave.
     * @return The resolved location, or null if not found.
     */
    private Location resolveRoofLocation(Location base, Location origin, Grave grave) {
        if (base == null || base.getWorld() == null) return null;

        Location up = searchUpForSafeGrave(base, origin, grave, base.getBlockY() + 1);
        if (up != null) return up;

        return searchDownForSafeGrave(base, grave, base.getBlockY());
    }

    /**
     * Searches downward for a safe grave location.
     *
     * @param base   The base location.
     * @param grave  The grave.
     * @param startY The starting Y value.
     * @return The safe location, or null if not found.
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
            Material type = block.getType();

            if (MaterialUtil.isWater(type)) {
                Location surface = resolveFluidSurfaceTop(candidate, grave, true);
                if (surface != null) return surface;
            } else if (MaterialUtil.isLava(type)) {
                Location surface = resolveFluidSurfaceTop(candidate, grave, false);
                if (surface != null) return surface;
            }

            if (!hasGrave(candidate) && isLocationSafeGrave(candidate)) {
                return candidate;
            }

            y--;
        }

        return null;
    }

    /**
     * Searches upward for a safe grave location.
     *
     * @param base   The base location.
     * @param origin The origin location.
     * @param grave  The grave.
     * @param startY The starting Y value.
     * @return The safe location, or null if not found.
     */
    private Location searchUpForSafeGrave(Location base, Location origin, Grave grave, int startY) {
        if (base == null || base.getWorld() == null) return null;

        World world = base.getWorld();
        int x = base.getBlockX();
        int z = base.getBlockZ();

        int maxY = world.getMaxHeight() - 1;

        if (isNether(world) && origin != null && !isAboveNetherRoofInternal(origin, grave)) {
            maxY = Math.min(maxY, getNetherRoofYInternal(world, grave));
        }

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
     * Resolves the air block above a fluid column.
     *
     * @param originInFluid The location in fluid.
     * @param grave         The grave.
     * @param water         Whether the fluid is water.
     * @return The surface location, or null if not found.
     */
    private Location resolveFluidSurfaceTop(Location originInFluid, Grave grave, boolean water) {
        if (originInFluid == null || originInFluid.getWorld() == null) return null;

        World world = originInFluid.getWorld();
        int x = originInFluid.getBlockX();
        int z = originInFluid.getBlockZ();

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        if (isNether(world) && !isAboveNetherRoofInternal(originInFluid, grave)) {
            maxY = Math.min(maxY, getNetherRoofYInternal(world, grave));
        }

        int y = Math.max(originInFluid.getBlockY(), minY);

        Material start = world.getBlockAt(x, y, z).getType();
        if (water ? !MaterialUtil.isWater(start) : !MaterialUtil.isLava(start)) return null;

        while (y <= maxY) {
            Material t = world.getBlockAt(x, y, z).getType();
            if (water ? MaterialUtil.isWater(t) : MaterialUtil.isLava(t)) {
                y++;
                continue;
            }
            break;
        }

        if (y < minY || y > maxY) return null;

        Block candidateBlock = world.getBlockAt(x, y, z);
        if (!MaterialUtil.isAir(candidateBlock.getType())) return null;

        Location place = new Location(world, x + 0.5, y, z + 0.5, originInFluid.getYaw(), originInFluid.getPitch());

        if (isVoid(place) || !isInsideBorder(place)) return null;
        if (hasGrave(place)) return null;
        if (!isLocationSafeGraveAboveFluid(place, water)) return null;

        return place;
    }

    /**
     * Checks if a grave location above fluid is safe.
     *
     * @param location The location.
     * @param water    Whether the fluid is water.
     * @return True if safe, otherwise false.
     */
    private boolean isLocationSafeGraveAboveFluid(Location location, boolean water) {
        if (location == null) return false;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null || rounded.getWorld() == null) return false;

        Block block = rounded.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);

        return isInsideBorder(rounded)
                && MaterialUtil.isSafeNotSolid(block.getType())
                && (water ? MaterialUtil.isWater(below.getType()) : MaterialUtil.isLava(below.getType()));
    }

    /**
     * Finds the bottom of a water column.
     *
     * @param loc The location.
     * @return The bottom water location, or null if not found.
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

        if (top == null) return null;

        Location bottom = top;
        for (int y = top.getBlockY() - 1; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (MaterialUtil.isWater(block.getType())) {
                bottom = block.getLocation();
            } else {
                break;
            }
        }

        return bottom;
    }

    /**
     * Checks if an entity can build at a location.
     *
     * @param livingEntity   The entity.
     * @param location       The location.
     * @param permissionList The permission list.
     * @return True if building is allowed, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canBuild(LivingEntity livingEntity, Location location, List<String> permissionList) {
        if (livingEntity instanceof Player player) {
            return (!plugin.getConfigManager().getConfigSection("placement.can-build", player, permissionList).getBoolean("placement.can-build")
                    || plugin.getCompatibility().canBuild(player, location, plugin))
                    && (!plugin.getIntegrationManager().hasProtectionLib()
                    || (!plugin.getConfigManager().getConfigSection("placement.can-build-protectionlib", player, permissionList).getBoolean("placement.can-build-protectionlib")
                    || plugin.getIntegrationManager().getProtectionLib().canBuild(location, player)));
        }
        return true;
    }

    /**
     * Checks if a player location is safe.
     *
     * @param location The location.
     * @return True if safe, otherwise false.
     */
    public boolean isLocationSafePlayer(Location location) {
        if (location == null || location.getWorld() == null) return false;

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
     * Checks if a grave location is safe.
     *
     * @param location The location.
     * @return True if safe, otherwise false.
     */
    public boolean isLocationSafeGrave(Location location) {
        if (location == null) return false;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null) return false;

        Block block = rounded.getBlock();
        return isInsideBorder(rounded)
                && MaterialUtil.isSafeNotSolid(block.getType())
                && MaterialUtil.isSafeSolid(block.getRelative(BlockFace.DOWN).getType());
    }

    /**
     * Checks if a grave exists at a location.
     *
     * @param location The location.
     * @return True if a grave exists, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasGrave(Location location) {
        if (location == null) return false;
        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null) return false;

        return plugin.getDataManager().hasChunkData(rounded)
                && plugin.getDataManager().getChunkData(rounded).getBlockDataMap().containsKey(rounded);
    }

    /**
     * Checks if a location is inside the world border.
     *
     * @param location The location.
     * @return True if inside the border, otherwise false.
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
     * Checks if a location is in the void.
     *
     * @param location The location.
     * @return True if in the void, otherwise false.
     */
    public boolean isVoid(Location location) {
        if (location == null || location.getWorld() == null) return true;
        return location.getY() < getMinHeight(location) || location.getY() > location.getWorld().getMaxHeight();
    }

    /**
     * Gets the minimum height for a location.
     *
     * @param location The location.
     * @return The minimum height.
     */
    public int getMinHeight(Location location) {
        return location != null
                && location.getWorld() != null
                && plugin.getVersionManager().hasMinHeight()
                ? location.getWorld().getMinHeight()
                : 0;
    }

    /**
     * Checks if a world is the Nether.
     *
     * @param world The world.
     * @return True if the world is the Nether, otherwise false.
     */
    private boolean isNether(World world) {
        return world != null && world.getEnvironment() == World.Environment.NETHER;
    }

    /**
     * Checks if a location is above the Nether roof limit.
     *
     * @param loc   The location.
     * @param grave The grave.
     * @return True if above the roof limit, otherwise false.
     */
    private boolean isAboveNetherRoofInternal(Location loc, Grave grave) {
        if (loc == null || loc.getWorld() == null) return false;
        World w = loc.getWorld();
        return isNether(w) && loc.getBlockY() > getNetherRoofYInternal(w, grave);
    }

    /**
     * Gets the Nether roof Y limit for a world.
     *
     * @param world The world.
     * @param grave The grave.
     * @return The roof Y limit.
     */
    private int getNetherRoofYInternal(World world, Grave grave) {
        if (world == null) return 0;

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        List<String> raw = plugin.getConfigManager().getConfigSection("placement.nether-roof-limit", grave).getStringList("placement.nether-roof-limit");

        Integer defaultLimit = null;
        Integer worldLimit = null;

        String wName = world.getName();
        for (String s : raw) {
            if (s == null) continue;
            String v = s.trim();
            if (v.isEmpty()) continue;

            int idx = v.indexOf(':');
            if (idx > 0 && idx < v.length() - 1) {
                if (worldLimit != null) continue;

                String name = v.substring(0, idx).trim();
                String num = v.substring(idx + 1).trim();
                Integer parsed = parseIntOrNull(num);
                if (parsed != null && name.equalsIgnoreCase(wName)) {
                    worldLimit = parsed;
                }
            } else {
                if (defaultLimit != null) continue;

                Integer parsed = parseIntOrNull(v);
                if (parsed != null) {
                    defaultLimit = parsed;
                }
            }
        }

        int roof = worldLimit != null ? worldLimit : (defaultLimit != null ? defaultLimit : maxY);

        if (roof < minY) roof = minY;
        if (roof > maxY) roof = maxY;

        return roof;
    }

    /**
     * Parses an integer value.
     *
     * @param s The string.
     * @return The parsed integer, or null if invalid.
     */
    private Integer parseIntOrNull(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Formats a location for debug output.
     *
     * @param loc The location.
     * @return The formatted location string.
     */
    private String fmtLoc(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return loc.getWorld().getName() + " x=" + loc.getBlockX() + " y=" + loc.getBlockY() + " z=" + loc.getBlockZ();
    }
}
