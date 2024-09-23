package com.ranull.graves.listener.integration.itemsadder;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * A listener for handling ItemsAdder's {@link FurnitureBreakEvent}.
 * This class is responsible for cancelling the furniture break event for graves,
 * depending on the configuration settings in the Graves plugin.
 */
public class FurnitureBreakListener implements Listener {

    private final Graves plugin;

    /**
     * Constructs a new {@code FurnitureBreakListener}.
     *
     * @param plugin the instance of the {@link Graves} plugin
     */
    public FurnitureBreakListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the {@link FurnitureBreakEvent} for ItemsAdder custom furniture.
     * Cancels the event if the furniture is part of a grave and the configuration
     * for preventing furniture breaking is enabled.
     *
     * @param event the ItemsAdder {@code FurnitureBreakEvent} triggered when
     *              a custom furniture entity is broken
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        Player player = event.getPlayer();

        // If the player has permission to break graves, allow it
        if (plugin.getConfig("grave.break", player).getBoolean("grave.break")) return;

        // Retrieve the grave associated with the furniture entity
        Grave grave = plugin.getEntityDataManager().getGrave(event.getBukkitEntity());

        String furnitureId = event.getNamespacedID();

        // If the furniture is part of a grave and matches the configured ItemsAdder furniture, cancel the event
        if (grave != null) {
            if (plugin.getConfig("itemsadder.furniture.enabled", player).getBoolean("itemsadder.furniture.enabled") &&
                    furnitureId.equals(plugin.getConfig("itemsadder.furniture.name", player).getString("itemsadder.furniture.name"))) {
                event.setCancelled(true);
            }
        }
    }
}