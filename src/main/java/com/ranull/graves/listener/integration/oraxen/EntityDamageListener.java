package com.ranull.graves.listener.integration.oraxen;

import com.ranull.graves.integration.Oraxen;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * @deprecated Recommend Nexo as a replacement.
 * Listens for EntityDamageEvent and cancels damage if the entity is an ItemFrame associated with a grave.
 */
public class EntityDamageListener implements Listener {
    private final Oraxen oraxen;

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Constructs a new EntityDamageListener with the specified Oraxen instance.
     *
     * @param oraxen The Oraxen instance to use.
     */
    public EntityDamageListener(Oraxen oraxen) {
        this.oraxen = oraxen;
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Handles EntityDamageEvent. If the entity being damaged is an ItemFrame and is associated with a grave,
     * it cancels the damage event.
     *
     * @param event The EntityDamageEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (isItemFrameAndHasGrave(event)) {
            event.setCancelled(true);
        }
    }

    /**
     * @deprecated Recommend Nexo as a replacement.
     * Checks if the entity is an ItemFrame and has an associated grave.
     *
     * @param event The EntityDamageEvent.
     * @return True if the entity is an ItemFrame and has an associated grave, false otherwise.
     */
    private boolean isItemFrameAndHasGrave(EntityDamageEvent event) {
        return event.getEntity() instanceof ItemFrame && oraxen.getGrave(event.getEntity()) != null;
    }
}