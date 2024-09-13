package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveExplodeEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.List;

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

        // Check for nearby graves within the explosion radius
        List<Grave> nearbyGraves = plugin.getGraveManager().getAllGraves();

        if (nearbyGraves != null) {
            for (Grave grave : nearbyGraves) {
                Location graveLocation = grave.getLocationDeath();
                double distance = explosionLocation.distance(graveLocation);

                // Get the protection radius from the configuration
                int protectionRadius = plugin.getConfig("grave.protection-radius", grave).getInt("grave.protection-radius", 0);

                try {
                    // If the explosion is within the protection radius, cancel the explosion
                    if (protectionRadius > 0 && distance <= protectionRadius + 15) {
                        event.setCancelled(true);
                        return;
                    }
                } catch (IllegalArgumentException ignored) {

                }

                // If the explosion is allowed, handle the grave explosion
                if (shouldExplode(grave)) {
                    handleGraveExplosion(event, grave, graveLocation);
                }
            }
        }
    }

    /**
     * Checks if the grave should explode based on the configuration.
     *
     * @param grave The grave to check.
     * @return True if the grave should explode, false otherwise.
     */
    private boolean shouldExplode(Grave grave) {
        return plugin.getConfig("grave.explode", grave).getBoolean("grave.explode", false);
    }

    /**
     * Handles the explosion of a grave.
     *
     * @param event        The ExplosionPrimeEvent.
     * @param grave        The grave associated with the explosion.
     * @param graveLocation The location of the grave.
     */
    private void handleGraveExplosion(ExplosionPrimeEvent event, Grave grave, Location graveLocation) {
        // Trigger the custom GraveExplodeEvent
        GraveExplodeEvent graveExplodeEvent = new GraveExplodeEvent(graveLocation, event.getEntity(), grave);
        plugin.getServer().getPluginManager().callEvent(graveExplodeEvent);

        // Check if the custom event was cancelled
        if (!graveExplodeEvent.isCancelled()) {
            // Handle the grave explosion based on the plugin's configuration
            if (plugin.getConfig("drop.explode", grave).getBoolean("drop.explode", false)) {
                plugin.getGraveManager().breakGrave(graveLocation, grave);
            } else {
                plugin.getGraveManager().removeGrave(grave);
            }

            // Execute effects and commands based on the explosion
            plugin.getGraveManager().playEffect("effect.loot", graveLocation, grave);
            plugin.getEntityManager().runCommands("event.command.explode", event.getEntity(), graveLocation, grave);

            if (plugin.getConfig("zombie.explode", grave).getBoolean("zombie.explode", false)) {
                plugin.getEntityManager().spawnZombie(graveLocation, grave);
            }
        } else {
            event.setCancelled(true);  // Cancel explosion if custom event was cancelled
        }
    }
}