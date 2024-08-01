package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listens for EntityDamageByEntityEvent to manage damage to specific entities.
 */
public class EntityDamageByEntityListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new EntityDamageByEntityListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public EntityDamageByEntityListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles EntityDamageByEntityEvent to determine if damage should be cancelled
     * based on the entity type and associated data.
     *
     * @param event The EntityDamageByEntityEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        if (shouldCancelDamage(entity)) {
            event.setCancelled(true);
        }
    }

    /**
     * Determines if damage to the entity should be cancelled.
     *
     * @param entity The entity being damaged.
     * @return True if the damage should be cancelled, false otherwise.
     */
    private boolean shouldCancelDamage(Entity entity) {
        return (entity instanceof ItemFrame || (isVersion1_7OrAbove() && entity instanceof ArmorStand))
                && isAssociatedWithGrave(entity);
    }

    /**
     * Checks if the server version is 1.7 or above.
     *
     * @return True if the server version is 1.7 or above, false otherwise.
     */
    private boolean isVersion1_7OrAbove() {
        return plugin.getVersionManager().is_v1_7();
    }

    /**
     * Checks if the entity is associated with a grave.
     *
     * @param entity The entity to check.
     * @return True if the entity is associated with a grave, false otherwise.
     */
    private boolean isAssociatedWithGrave(Entity entity) {
        return plugin.getEntityDataManager().getGrave(entity) != null;
    }
}