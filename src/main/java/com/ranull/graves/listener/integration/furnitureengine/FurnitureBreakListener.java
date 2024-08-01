package com.ranull.graves.listener.integration.furnitureengine;

import com.mira.furnitureengine.events.FurnitureBreakEvent;
import com.ranull.graves.integration.FurnitureEngine;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listens for FurnitureBreakEvent and checks if the furniture being broken is associated with a grave.
 */
public class FurnitureBreakListener implements Listener {
    private final FurnitureEngine furnitureEngine;

    /**
     * Constructs a new FurnitureBreakListener with the specified FurnitureEngine instance.
     *
     * @param furnitureEngine The FurnitureEngine instance to use.
     */
    public FurnitureBreakListener(FurnitureEngine furnitureEngine) {
        this.furnitureEngine = furnitureEngine;
    }

    /**
     * Handles FurnitureBreakEvent. Cancels the event if the furniture being broken is associated with a grave.
     *
     * @param event The FurnitureBreakEvent to handle.
     */
    @EventHandler
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        ItemFrame itemFrame = furnitureEngine.getItemFrame(event.getFurnitureLocation());

        if (itemFrame != null && isFurnitureAssociatedWithGrave(event, itemFrame)) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if the furniture being broken is associated with a grave.
     *
     * @param event     The FurnitureBreakEvent.
     * @param itemFrame The ItemFrame being broken.
     * @return True if the furniture is associated with a grave, false otherwise.
     */
    private boolean isFurnitureAssociatedWithGrave(FurnitureBreakEvent event, ItemFrame itemFrame) {
        return furnitureEngine.getGrave(event.getFurnitureLocation(), itemFrame.getUniqueId()) != null;
    }
}