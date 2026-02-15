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

    public SafeLocationManager(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Explains why a particular grave placement location was chosen.
     */
    public enum GravePlacementReason {

        /** Original location was already a valid, empty, non-fluid placement. */
        ORIGINAL_SAFE,

        /** Higher safe placement found by vertical scanning. */
        ROOF,

        /** Lower safe placement found by vertical scanning. */
        GROUND,

        /** Water case: derived from player's last solid location. */
        WATER_SMART,

        /** Water case: death was in air directly above water. */
        WATER_ABOVE,

        /** Water case: first AIR block above contiguous water column. */
        WATER_TOP,

        /** Water case: bottom-most water block in the contiguous column. */
        WATER_BOTTOM,

        /** Lava case: derived from player's last solid location. */
        LAVA_SMART,

        /** Lava case: death was in air directly above lava. */
        LAVA_ABOVE,

        /** Lava case: first AIR block above contiguous lava column. */
        LAVA_TOP,

        /** Origin was above world build height; placed below build limit. */
        ABOVE_BUILD_LIMIT,

        /** Nether roof fallback when no below-roof placement was found. */
        NETHER_ROOF,

        /** Void/outside-border case: safe point found in the Y column. */
        VOID_COLUMN,

        /** Void/outside-border case: safe point found at minimum height. */
        VOID_MIN_HEIGHT,

        /** No valid candidates; returned original. */
        FALLBACK_ORIGINAL
    }

    /**
     * Result of resolving a grave placement.
     */
    public static final class GravePlacementResult {
        private final Location location;
        private final GravePlacementReason reason;

        private GravePlacementResult(Location location, GravePlacementReason reason) {
            this.location = location;
            this.reason = reason;
        }

        public Location getLocation() {
            return location;
        }

        public GravePlacementReason getReason() {
            return reason;
        }

        public static GravePlacementResult of(Location location, GravePlacementReason reason) {
            return new GravePlacementResult(location, reason);
        }
    }

    /**
     * Internal candidate used during selection.
     */
    private static final class Candidate {
        final Location loc;
        final GravePlacementReason reason;

        Candidate(Location loc, GravePlacementReason reason) {
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

    public void setLastSolidLocation(Entity entity, Location location) {
        if (entity == null || location == null) return;
        plugin.getCacheManager().getLastLocationMap().put(entity.getUniqueId(), location.clone());
    }

    public Location getLastSolidLocation(Entity entity) {
        if (entity == null) return null;

        Location location = plugin.getCacheManager().getLastLocationMap().get(entity.getUniqueId());
        if (location == null || location.getWorld() == null) return null;

        entity.getWorld();
        if (!location.getWorld().equals(entity.getWorld())) return null;

        Block below = location.getBlock().getRelative(BlockFace.DOWN);
        if (!below.getType().isSolid()) return null;

        return location.clone();
    }

    public void removeLastSolidLocation(Entity entity) {
        if (entity == null) return;
        plugin.getCacheManager().getLastLocationMap().remove(entity.getUniqueId());
    }

    @SuppressWarnings("unused")
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

    public Location getSafeGraveLocation(LivingEntity livingEntity, Location location, Grave grave) {
        GravePlacementResult r = resolveSafeGravePlacement(livingEntity, location, grave);
        return r != null ? r.getLocation() : null;
    }

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

        if (directlyAboveWater) {
            Location above = centerOnBlock(origin);
            if (above != null && !isVoid(above) && isInsideBorder(above) && !hasGrave(above) && isLocationSafeGraveAboveFluid(above, true)) {
                candidates.add(new Candidate(above, GravePlacementReason.WATER_ABOVE));
            }
        }

        if (directlyAboveLava) {
            Location above = centerOnBlock(origin);
            if (above != null && !isVoid(above) && isInsideBorder(above) && !hasGrave(above) && isLocationSafeGraveAboveFluid(above, false)) {
                candidates.add(new Candidate(above, GravePlacementReason.LAVA_ABOVE));
            }
        }

        if (inWaterOrAbove) {
            boolean waterTop = plugin.getConfigManager().getConfigSection("placement.water-top", grave).getBoolean("placement.water-top");

            if (waterTop) {
                if (directlyAboveWater) {
                    Location above = centerOnBlock(origin);
                    if (above != null && !isVoid(above) && isInsideBorder(above) && !hasGrave(above) && isLocationSafeGraveAboveFluid(above, true)) {
                        plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.WATER_ABOVE + " loc=" + fmtLoc(above) + " distSq=" + distSq(origin, above) + ".", 1);
                        return GravePlacementResult.of(above, GravePlacementReason.WATER_ABOVE);
                    }
                }

                Location start = MaterialUtil.isWater(originType) ? origin : origin.clone().add(0.0, -1.0, 0.0);
                Location surface = resolveFluidSurfaceTop(start, grave, true);
                if (surface != null) {
                    plugin.debugMessage(prefix + "CHOSEN=" + GravePlacementReason.WATER_TOP + " loc=" + fmtLoc(surface) + " distSq=" + distSq(origin, surface) + ".", 1);
                    return GravePlacementResult.of(surface, GravePlacementReason.WATER_TOP);
                }

                plugin.debugMessage(prefix + "water-top enabled but no AIR surface found; continuing.", 1);
            }

            addWaterCandidates(candidates, origin, livingEntity, grave, useGround, useRoof);
        } else if (inLavaOrAbove) {
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

            addLavaCandidates(candidates, origin, livingEntity, grave, useGround, useRoof);
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

    private void addWaterCandidates(List<Candidate> out, Location origin, LivingEntity livingEntity, Grave grave, boolean useGround, boolean useRoof) {
        boolean waterSmart = plugin.getConfigManager().getConfigSection("placement.water-smart", grave).getBoolean("placement.water-smart");

        if (waterSmart) {
            Location smart = resolveSmartFromLastSolid(livingEntity, origin, grave, useGround, useRoof);
            if (smart != null) out.add(new Candidate(smart, GravePlacementReason.WATER_SMART));
        }

        if (useGround) {
            Location bottom = findWaterBottom(origin);
            if (bottom != null && !hasGrave(bottom) && isLocationSafeGrave(bottom)) {
                out.add(new Candidate(bottom, GravePlacementReason.WATER_BOTTOM));
            }
        }
    }

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

    private double distSq(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return (dx * dx) + (dy * dy) + (dz * dz);
    }

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

    private Location resolveGroundLocation(Location base, Location origin, Grave grave) {
        if (base == null || base.getWorld() == null) return null;

        Location down = searchDownForSafeGrave(base, grave, base.getBlockY());
        if (down != null) return down;

        return searchUpForSafeGrave(base, origin, grave, base.getBlockY() + 1);
    }

    private Location resolveRoofLocation(Location base, Location origin, Grave grave) {
        if (base == null || base.getWorld() == null) return null;

        Location up = searchUpForSafeGrave(base, origin, grave, base.getBlockY() + 1);
        if (up != null) return up;

        return searchDownForSafeGrave(base, grave, base.getBlockY());
    }

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

    public boolean isLocationSafeGrave(Location location) {
        if (location == null) return false;

        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null) return false;

        Block block = rounded.getBlock();
        return isInsideBorder(rounded)
                && MaterialUtil.isSafeNotSolid(block.getType())
                && MaterialUtil.isSafeSolid(block.getRelative(BlockFace.DOWN).getType());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasGrave(Location location) {
        if (location == null) return false;
        Location rounded = LocationUtil.roundLocation(location);
        if (rounded == null) return false;

        return plugin.getDataManager().hasChunkData(rounded)
                && plugin.getDataManager().getChunkData(rounded).getBlockDataMap().containsKey(rounded);
    }

    public boolean isInsideBorder(Location location) {
        if (location == null) return false;

        return plugin.getVersionManager().is_v1_7()
                || plugin.getVersionManager().is_v1_8()
                || plugin.getVersionManager().is_v1_9()
                || plugin.getVersionManager().is_v1_10()
                || plugin.getVersionManager().is_v1_11()
                || (location.getWorld() != null && location.getWorld().getWorldBorder().isInside(location));
    }

    public boolean isVoid(Location location) {
        if (location == null || location.getWorld() == null) return true;
        return location.getY() < getMinHeight(location) || location.getY() > location.getWorld().getMaxHeight();
    }

    public int getMinHeight(Location location) {
        return location != null
                && location.getWorld() != null
                && plugin.getVersionManager().hasMinHeight()
                ? location.getWorld().getMinHeight()
                : 0;
    }

    private boolean isNether(World world) {
        return world != null && world.getEnvironment() == World.Environment.NETHER;
    }

    private boolean isAboveNetherRoofInternal(Location loc, Grave grave) {
        if (loc == null || loc.getWorld() == null) return false;
        World w = loc.getWorld();
        return isNether(w) && loc.getBlockY() > getNetherRoofYInternal(w, grave);
    }

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

    private Integer parseIntOrNull(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String fmtLoc(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return loc.getWorld().getName() + " x=" + loc.getBlockX() + " y=" + loc.getBlockY() + " z=" + loc.getBlockZ();
    }
}