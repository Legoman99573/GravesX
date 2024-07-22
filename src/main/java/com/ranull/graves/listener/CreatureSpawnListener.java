package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Listens for CreatureSpawnEvent to handle entity spawning data.
 */
public class CreatureSpawnListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new CreatureSpawnListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public CreatureSpawnListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles CreatureSpawnEvent to store the spawn reason of the entity.
     *
     * @param event The CreatureSpawnEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Ensure the entity is not dead
        if (!event.getEntity().isDead()) {
            // Store the spawn reason of the entity
            plugin.getEntityManager().setDataString(event.getEntity(),
                    "spawnReason", event.getSpawnReason().name());
        }
    }
}