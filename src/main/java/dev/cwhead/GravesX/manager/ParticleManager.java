package dev.cwhead.GravesX.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.ColorUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

/**
 * Spawns particles to graves.
 */
public class ParticleManager {
    private final Graves plugin;
    private final HashMap<UUID, Long> cooldowns;

    public ParticleManager(Graves plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
    }

    /**
     * Plays a particle trail to the end destination.
     * @param startLocation The start location.
     * @param endLocation   The end location.
     * @param particleType  The particle to spawn.
     * @param count         How many particles should spawn.
     * @param speed         The speed the particles take in the direction from startLocation to endLocation.
     * @param durationTicks The duration, in ticks, that the particle trail will last for.
     * @param playerUUID    The UUID of the player triggering the effect.
     */
    public void startCompassParticleTrail(Location startLocation, Location endLocation, Particle particleType, int count, double speed, long durationTicks, UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        if (cooldowns.containsKey(playerUUID)) {
            long lastUsed = cooldowns.get(playerUUID);
            if (currentTime - lastUsed < durationTicks) {
                return;
            }
        }

        cooldowns.put(playerUUID, currentTime);

        try {
            startLocation.add(0.0, 2.0, 0.0);
            endLocation.add(0.5, 0.3, 0.5);
            Vector direction = endLocation.clone().subtract(startLocation).toVector().normalize();

            new BukkitRunnable() {
                long ticksElapsed = 0;

                @Override
                public void run() {
                    try {
                        if (ticksElapsed >= durationTicks || startLocation.distance(endLocation) < speed) {
                            cancel();
                            return;
                        }

                        Objects.requireNonNull(startLocation.getWorld()).spawnParticle(particleType, startLocation, count, 0, 0, 0, 0);

                        startLocation.add(direction.clone().multiply(speed));

                        ticksElapsed++;
                    } catch (Exception e) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // ignored
        }
    }

    /**
     * Return your Graves BlockData for this grave if available. If the grave has no attached BlockData,
     * synthesize one from config as a fallback so the pipeline is consistent.
     */
    public BlockData getGravesBlockData(Grave grave, Location loc) {
        try {
            var m = grave.getClass().getMethod("getBlockData");
            Object val = m.invoke(grave);
            if (val instanceof BlockData) {
                return (BlockData) val;
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            plugin.debugMessage("getBlockData() reflect failed: " + t.getMessage(), 2);
        }

        String mat = plugin.getConfig("particle.block-material", grave)
                .getString("particle.block-material", "STONE");
        String data = plugin.getConfig("particle.block-data", grave)
                .getString("particle.block-data", "");

        java.util.UUID gid = null;
        try {
            var m = grave.getClass().getMethod("getUUID");
            Object v = m.invoke(grave);
            if (v instanceof java.util.UUID) gid = (java.util.UUID) v;
        } catch (Throwable ignored) {}

        return new com.ranull.graves.data.BlockData(loc, gid, mat, data
        );
    }

    /**
     * Convert your Graves BlockData to Bukkit BlockData for vanilla particle APIs.
     * Uses replaceData if present; otherwise uses replaceMaterial.
     */
    public org.bukkit.block.data.BlockData toBukkitBlockData(com.ranull.graves.data.BlockData gravesBD) {
        if (gravesBD == null) return null;

        String data = safe(gravesBD.getReplaceData());
        String matName = safe(gravesBD.getReplaceMaterial());
        try {
            if (!data.isEmpty()) {
                return Bukkit.createBlockData(data);
            }
        } catch (IllegalArgumentException ex) {
            plugin.debugMessage("Invalid replaceData for particle: " + data, 1);
        }

        Material m = Material.matchMaterial(matName.isEmpty() ? "STONE" : matName);
        if (m == null || !m.isBlock()) {
            plugin.debugMessage("Invalid replaceMaterial for particle: " + matName + " (defaulting to STONE)", 1);
            m = Material.STONE;
        }
        return m.createBlockData();
    }

    /**
     * Parse an ItemStack from config (material only; amount 1).
     * @return ItemStack or STONE if invalid.
     */
    public ItemStack parseItemStack(Grave grave) {
        String matName = plugin.getConfig("particle.item", grave).getString("particle.item", "STONE");
        Material m = Material.matchMaterial(matName);
        if (m == null || m.isAir()) {
            plugin.debugMessage("Invalid particle.item: " + matName + " (defaulting to STONE)", 1);
            m = Material.STONE;
        }
        return new ItemStack(m, 1);
    }

    /**
     * Resolve a Bukkit Color from text, with a default fallback.
     * Accepts names ("RED"), hex ("#FF0000"), or "r,g,b".
     */
    public Color safeColor(String input, Color def) {
        Color c = ColorUtil.getColor(input);
        return (c != null) ? c : def;
    }

    public Vibration buildVibrationSingle(Graves plugin, Location graveLoc, Grave grave) {
        if (graveLoc == null || graveLoc.getWorld() == null) return null;
        World world = graveLoc.getWorld();

        String destMode = plugin.getConfig("particle.vibration.destination", grave)
                .getString("particle.vibration.destination", "block")
                .toLowerCase(java.util.Locale.ROOT);

        Player source = findSourcePlayer(plugin, grave, graveLoc, 5);
        if (source == null) {
            Location origin = center(graveLoc);
            Location dest = origin.clone().add(0, 0.001, 0);
            return new Vibration(origin, new Vibration.Destination.BlockDestination(dest), 5);
        }

        Location origin = source.getLocation();
        Location graveCenter = center(graveLoc);

        if ("entity".equals(destMode)) {
            Player destEntity = pickRandomNearbyPlayer(world, graveCenter, 5);
            if (destEntity == null) destEntity = source;
            return new org.bukkit.Vibration(origin, new Vibration.Destination.EntityDestination(destEntity), 5);
        } else {
            return new org.bukkit.Vibration(origin, new Vibration.Destination.BlockDestination(graveCenter), 5);
        }
    }

    /**
     * Build a Vibration instance from config, using block or entity destination.
     * Falls back to a block destination at the origin if parsing fails.
     */
    public void spawnVibrationBounce(Graves plugin, Location graveLoc, Grave grave,
                                            Particle particle, int count) {
        if (graveLoc == null || graveLoc.getWorld() == null) return;
        World world = graveLoc.getWorld();

        String destMode = plugin.getConfig("particle.vibration.destination", grave)
                .getString("particle.vibration.destination", "block")
                .toLowerCase(java.util.Locale.ROOT);

        Player source = findSourcePlayer(plugin, grave, graveLoc, 5);
        if (source == null) {
            var vib = buildVibrationSingle(plugin, graveLoc, grave);
            world.spawnParticle(particle, graveLoc, count, vib);
            return;
        }

        Location originNow = source.getLocation();
        Location graveCenter = center(graveLoc);

        if ("entity".equals(destMode)) {
            Player destEntity = pickRandomNearbyPlayer(world, graveCenter, 5);
            if (destEntity == null) destEntity = source;

            var forward = new org.bukkit.Vibration(
                    originNow,
                    new org.bukkit.Vibration.Destination.EntityDestination(destEntity),
                    5
            );
            world.spawnParticle(particle, originNow, count, forward);

            Player backFrom = destEntity;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location backOrigin = backFrom.getLocation();
                var back = new org.bukkit.Vibration(
                        backOrigin,
                        new org.bukkit.Vibration.Destination.EntityDestination(source),
                        5
                );
                world.spawnParticle(particle, backOrigin, count, back);
            }, 5);

        } else {
            var forward = new Vibration(
                    originNow,
                    new Vibration.Destination.BlockDestination(graveCenter),
                    5
            );
            world.spawnParticle(particle, originNow, count, forward);

            Location playerBlockCenter = center(originNow);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                var back = new org.bukkit.Vibration(
                        graveCenter,
                        new org.bukkit.Vibration.Destination.BlockDestination(playerBlockCenter),
                        5
                );
                world.spawnParticle(particle, graveCenter, count, back);
            }, 5);
        }
    }

    /**
     * Try to construct Particle.Trail reflectively to avoid compile-time dependency.
     * Supports (int duration, double spread), (int), and no-arg constructors.
     */
    public Object parseTrailData(Graves plugin, Grave grave) {
        int duration = plugin.getConfig("particle.trail.duration-ticks", grave)
                .getInt("particle.trail.duration-ticks", 20);
        double spread = plugin.getConfig("particle.trail.spread", grave)
                .getDouble("particle.trail.spread", 0.02D);

        try {
            Class<?> trailClass = Class.forName("org.bukkit.Particle$Trail");
            for (java.lang.reflect.Constructor<?> c : trailClass.getConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 2 && p[0] == int.class && p[1] == double.class) {
                    return c.newInstance(duration, spread);
                }
                if (p.length == 1 && p[0] == int.class) {
                    return c.newInstance(duration);
                }
            }
            return trailClass.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            plugin.debugMessage("TRAIL data type not available: " + t.getMessage(), 2);
            return null;
        }
    }

    private static Player findSourcePlayer(Graves plugin, Grave grave, Location ref, int radius) {
        java.util.UUID owner = tryResolveOwnerUUID(plugin, grave);
        if (owner != null) {
            Player p = org.bukkit.Bukkit.getPlayer(owner);
            if (p != null && p.isOnline() && sameWorld(p, ref)) return p;
        }
        double r2 = radius * (double) radius;
        Player best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Player p : ref.getWorld().getPlayers()) {
            if (!p.isOnline()) continue;
            double d2 = p.getLocation().distanceSquared(ref);
            if (d2 <= r2 && d2 < bestD2) { bestD2 = d2; best = p; }
        }
        return best;
    }

    private static Player pickRandomNearbyPlayer(World world, Location center, int radius) {
        java.util.List<Player> candidates = world.getPlayers().stream()
                .filter(Player::isOnline)
                .filter(p -> p.getLocation().distanceSquared(center) <= radius * (double) radius)
                .collect(java.util.stream.Collectors.toList());
        if (candidates.isEmpty()) return null;
        return candidates.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private static java.util.UUID tryResolveOwnerUUID(Graves plugin, Grave grave) {
        String[] candidates = {"getPlayerUUID", "getOwnerUUID", "getOwnerId", "getOwner"};
        for (String name : candidates) {
            try {
                var m = grave.getClass().getMethod(name);
                Object v = m.invoke(grave);
                if (v instanceof java.util.UUID) return (java.util.UUID) v;
                if (v instanceof org.bukkit.OfflinePlayer) return ((org.bukkit.OfflinePlayer) v).getUniqueId();
                if (v instanceof String) return java.util.UUID.fromString((String) v);
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                plugin.debugMessage("Owner UUID resolve failed via " + name + ": " + t.getMessage(), 2);
            }
        }
        return null;
    }

    private static boolean sameWorld(Player p, Location ref) {
        return p.getWorld().equals(ref.getWorld());
    }

    private static Location center(Location l) {
        return new Location(l.getWorld(), l.getBlockX() + 0.5, l.getBlockY() + 0.5, l.getBlockZ() + 0.5);
    }


    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
