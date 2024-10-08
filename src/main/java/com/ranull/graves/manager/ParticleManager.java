package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
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
    public void startParticleTrail(Location startLocation, Location endLocation, Particle particleType, int count, double speed, long durationTicks, UUID playerUUID) {
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

                        startLocation.getWorld().spawnParticle(particleType, startLocation, count, 0, 0, 0, 0);

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
}
