package com.ranull.graves.listener.integration.furnitureengine;

import com.mira.furnitureengine.events.FurnitureInteractEvent;
import com.ranull.graves.Graves;
import com.ranull.graves.integration.FurnitureEngine;
import com.ranull.graves.type.Grave;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listens for FurnitureInteractEvent and handles interactions with furniture that may be associated with a grave.
 */
public class FurnitureInteractListener implements Listener {
    private final Graves plugin;
    private final FurnitureEngine furnitureEngine;

    /**
     * Constructs a new FurnitureInteractListener with the specified Graves plugin and FurnitureEngine instance.
     *
     * @param plugin The Graves plugin instance.
     * @param furnitureEngine The FurnitureEngine instance to use.
     */
    public FurnitureInteractListener(Graves plugin, FurnitureEngine furnitureEngine) {
        this.plugin = plugin;
        this.furnitureEngine = furnitureEngine;
    }

    /**
     * Handles FurnitureInteractEvent. If the furniture being interacted with is associated with a grave,
     * it attempts to open the grave and cancels the event if successful.
     *
     * @param event The FurnitureInteractEvent to handle.
     */
    @EventHandler
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        ItemFrame itemFrame = furnitureEngine.getItemFrame(event.getFurnitureLocation());

        if (itemFrame != null) {
            Grave grave = furnitureEngine.getGrave(itemFrame.getLocation(), itemFrame.getUniqueId());

            if (grave != null) {
                event.setCancelled(plugin.getGraveManager().openGrave(event.getPlayer(), itemFrame.getLocation(), grave));
            }
        }
    }
}