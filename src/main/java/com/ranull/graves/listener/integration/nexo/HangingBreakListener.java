package com.ranull.graves.listener.integration.nexo;

import com.ranull.graves.integration.Nexo;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;

/**
 * Listens for HangingBreakEvent and cancels the event if the entity being broken is an ItemFrame associated with a grave.
 */
public class HangingBreakListener implements Listener {
    private final Nexo nexo;

    /**
     * Constructs a new HangingBreakListener with the specified Nexo instance.
     *
     * @param nexo The Nexo instance to use.
     */
    public HangingBreakListener(Nexo nexo) {
        this.nexo = nexo;
    }

    /**
     * Handles HangingBreakEvent. If the entity being broken is an ItemFrame and is associated with a grave,
     * it cancels the event.
     *
     * @param event The HangingBreakEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (isItemFrameAndHasGrave(event)) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if the entity is an ItemFrame and has an associated grave.
     *
     * @param event The HangingBreakEvent.
     * @return True if the entity is an ItemFrame and has an associated grave, false otherwise.
     */
    private boolean isItemFrameAndHasGrave(HangingBreakEvent event) {
        return event.getEntity() instanceof ItemFrame && nexo.getGrave(event.getEntity()) != null;
    }
}