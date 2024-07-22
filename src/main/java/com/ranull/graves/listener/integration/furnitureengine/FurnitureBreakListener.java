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

        if (itemFrame != null) {
            event.setCancelled(furnitureEngine.getGrave(event.getFurnitureLocation(),
                    itemFrame.getUniqueId()) != null);
        }
    }
}