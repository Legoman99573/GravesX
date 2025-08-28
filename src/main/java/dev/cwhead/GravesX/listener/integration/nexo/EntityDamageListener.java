package dev.cwhead.GravesX.listener.integration.nexo;

import dev.cwhead.GravesX.integration.Nexo;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Listens for EntityDamageEvent and cancels damage if the entity is an ItemFrame associated with a grave.
 */
public class EntityDamageListener implements Listener {
    private final Nexo nexo;

    /**
     * Constructs a new EntityDamageListener with the specified Nexo instance.
     *
     * @param nexo The Nexo instance to use.
     */
    public EntityDamageListener(Nexo nexo) {
        this.nexo = nexo;
    }

    /**
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
     * Checks if the entity is an ItemFrame and has an associated grave.
     *
     * @param event The EntityDamageEvent.
     * @return True if the entity is an ItemFrame and has an associated grave, false otherwise.
     */
    private boolean isItemFrameAndHasGrave(EntityDamageEvent event) {
        return event.getEntity() instanceof ItemFrame && nexo.getGrave(event.getEntity()) != null;
    }
}