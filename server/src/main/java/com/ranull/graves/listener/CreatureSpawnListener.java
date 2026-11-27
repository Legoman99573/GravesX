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
        if (shouldStoreSpawnReason(event)) {
            storeSpawnReason(event);
        }
    }

    /**
     * Checks if the spawn reason of the entity should be stored.
     *
     * @param event The CreatureSpawnEvent.
     * @return True if the spawn reason should be stored, false otherwise.
     */
    private boolean shouldStoreSpawnReason(CreatureSpawnEvent event) {
        return !event.getEntity().isDead();
    }

    /**
     * Stores the spawn reason of the entity.
     *
     * @param event The CreatureSpawnEvent.
     */
    private void storeSpawnReason(CreatureSpawnEvent event) {
        plugin.getEntityManager().setDataString(event.getEntity(), "spawnReason", event.getSpawnReason().name());
    }
}