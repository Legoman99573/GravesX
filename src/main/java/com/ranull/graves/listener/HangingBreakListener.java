package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;

/**
 * Listener for handling HangingBreakEvent and conditionally canceling the event.
 */
public class HangingBreakListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a HangingBreakListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public HangingBreakListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the HangingBreakEvent to conditionally cancel the event if the entity is an ItemFrame
     * and associated with a grave.
     *
     * @param event The HangingBreakEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        event.setCancelled(event.getEntity() instanceof ItemFrame && plugin.getEntityDataManager()
                .getGrave(event.getEntity()) != null);
    }
}