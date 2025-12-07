package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GravePreExplodeEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.List;
import java.util.Objects;

/**
 * Listens for ExplosionPrimeEvent to notify when an explosion will affect graves.
 * Fires a GravePreExplodeEvent once for each grave that will be inside the blast radius.
 *
 * <p>Listeners of GravePreExplodeEvent can:</p>
 * <ul>
 *     <li>Cancel the event to cancel the explosion entirely.</li>
 *     <li>Modify the explosion radius.</li>
 *     <li>Modify the explosion location/world.</li>
 * </ul>
 *
 * <p>Any changes to radius/location/world are propagated back to the original
 * ExplosionPrimeEvent and the source entity.</p>
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
     * Handles ExplosionPrimeEvent to notify when an explosion will affect graves.
     *
     * @param event The ExplosionPrimeEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        Entity sourceEntity = event.getEntity();

        Location explosionLocation = sourceEntity.getLocation().clone();

        List<Grave> allGraves = plugin.getGraveManager().getAllGraves();
        if (allGraves == null || allGraves.isEmpty()) {
            return;
        }

        float blastRadius = event.getRadius();
        float blastRadiusSquared = blastRadius * blastRadius;

        boolean cancelExplosion = false;

        for (Grave grave : allGraves) {
            Location graveLocation = grave.getLocationDeath();
            if (graveLocation == null) {
                continue;
            }

            if (!Objects.equals(graveLocation.getWorld(), explosionLocation.getWorld())) {
                continue;
            }

            if (explosionLocation.distanceSquared(graveLocation) <= blastRadiusSquared) {
                GravePreExplodeEvent preEvent = new GravePreExplodeEvent(grave, explosionLocation, sourceEntity, blastRadius);

                plugin.getServer().getPluginManager().callEvent(preEvent);

                if (preEvent.isCancelled()) {
                    cancelExplosion = true;
                    break;
                }

                float newRadius = preEvent.getRadius();
                if (newRadius != blastRadius) {
                    blastRadius = newRadius;
                    blastRadiusSquared = newRadius * newRadius;
                    event.setRadius(newRadius);
                }

                Location newExplosionLocation = preEvent.getExplosionLocation();
                if (!newExplosionLocation.equals(explosionLocation)) {
                    explosionLocation = newExplosionLocation.clone();

                    if (!sourceEntity.getLocation().equals(newExplosionLocation)) {
                        sourceEntity.teleport(newExplosionLocation);
                    }
                }
            }
        }

        if (cancelExplosion) {
            event.setCancelled(true);
        }
    }
}