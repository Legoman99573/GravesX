package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilityParticleEnum;
import dev.cwhead.GravesX.compatibility.CompatibilitySoundEnum;
import dev.cwhead.GravesX.event.GraveExplodeEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.List;
import java.util.Objects;

/**
 * Listens for ExplosionPrimeEvent to handle interactions with grave blocks when an explosion is triggered.
 */
public class ExplosionPrimeListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new ExplosionPrimeListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public ExplosionPrimeListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles ExplosionPrimeEvent to manage grave interactions when an explosion is initiated near graves.
     *
     * @param event The ExplosionPrimeEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        Location explosionLocation = event.getEntity().getLocation();
        List<Grave> nearbyGraves = plugin.getGraveManager().getAllGraves();

        if (nearbyGraves == null || nearbyGraves.isEmpty()) {
            return;
        }

        for (Grave grave : nearbyGraves) {
            Location graveLocation = grave.getLocationDeath();
            int protectionRadius = plugin.getConfig("grave.protection-radius", grave)
                    .getInt("grave.protection-radius", 0);
            boolean shouldProtectRadius = plugin.getConfig("grave.should-protect-radius", grave)
                    .getBoolean("grave.should-protect-radius", false);

            boolean cancelExplosion = false;

            // Always protect the grave block itself
            if (explosionLocation.getBlock().getLocation().equals(graveLocation.getBlock().getLocation())) {
                cancelExplosion = true;
            }

            // Optionally protect blocks within the cube radius
            if (!cancelExplosion && shouldProtectRadius && isWithinCube(explosionLocation, graveLocation, protectionRadius)) {
                cancelExplosion = true;
            }

            if (cancelExplosion) {
                event.setCancelled(true);
                return;
            }

            // If grave is allowed to explode, trigger grave explosion
            if (shouldExplode(grave)) {
                handleGraveExplosion(event, grave, graveLocation);
            }
        }
    }

    /**
     * Checks if the explosion location is within a cube around the grave location.
     *
     * @param loc1   The explosion location.
     * @param loc2   The grave location.
     * @param radius The protection radius.
     * @return True if within the cube, false otherwise.
     */
    private boolean isWithinCube(Location loc1, Location loc2, int radius) {
        int dx = Math.abs(loc1.getBlockX() - loc2.getBlockX());
        int dy = Math.abs(loc1.getBlockY() - loc2.getBlockY());
        int dz = Math.abs(loc1.getBlockZ() - loc2.getBlockZ());
        return dx <= radius && dy <= radius && dz <= radius;
    }

    /**
     * Checks if the grave should explode based on the configuration.
     *
     * @param grave The grave to check.
     * @return True if the grave should explode, false otherwise.
     */
    private boolean shouldExplode(Grave grave) {
        return plugin.getConfig("grave.explode", grave)
                .getBoolean("grave.explode", false);
    }

    /**
     * Handles the explosion of a grave.
     *
     * @param event         The ExplosionPrimeEvent.
     * @param grave         The grave associated with the explosion.
     * @param graveLocation The location of the grave.
     */
    private void handleGraveExplosion(ExplosionPrimeEvent event, Grave grave, Location graveLocation) {
        GraveExplodeEvent modern = new GraveExplodeEvent(graveLocation, event.getEntity(), grave);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveExplodeEvent legacy = new com.ranull.graves.event.GraveExplodeEvent(graveLocation, event.getEntity(), grave);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon()) {
            event.setCancelled(true);
            return;
        }

        Location effectiveLoc = graveLocation;
        try {
            if (modern.hasLocation()) {
                effectiveLoc = modern.getLocation();
            } else {
                legacy.getLocation();
                effectiveLoc = legacy.getLocation();
            }
        } catch (Throwable ignored) {
            //ignored
        }

        if (plugin.getConfig("drop.looted-explosion-effect", grave).getBoolean("drop.looted-explosion-effect", false)) {
            try {
                Location location = grave.getLocationDeath();
                Objects.requireNonNull(location.getWorld()).spawnParticle(plugin.getVersionManager().getParticleForVersion("EXPLOSION"), location, 1);
                try {
                    location.getWorld().playSound(location, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("ENTITY_GENERIC_EXPLODE")), 1.0f, 1.0f);
                } catch (Exception e) {
                    location.getWorld().playSound(location, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("EXPLODE")), 1.0f, 1.0f); // pre 1.9
                }
            } catch (Exception ignored) {
                //ignored
            }
        }

        if (plugin.getConfig("drop.explode", grave).getBoolean("drop.explode", false)) {
            plugin.getGraveManager().breakGrave(effectiveLoc, grave);
        } else {
            plugin.getGraveManager().removeGrave(grave);
        }

        plugin.getGraveManager().playEffect("effect.loot", effectiveLoc, grave);
        plugin.getEntityManager().runCommands("event.command.explode", event.getEntity(), effectiveLoc, grave);

        if (plugin.getConfig("zombie.explode", grave).getBoolean("zombie.explode", false)) {
            plugin.getEntityManager().spawnZombie(effectiveLoc, grave);
        }
    }
}