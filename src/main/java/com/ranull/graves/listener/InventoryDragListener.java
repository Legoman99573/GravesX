package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.compatibility.CompatibilityInventoryView;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.InventoryUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

/**
 * Listener for handling InventoryClickEvent to manage interactions with grave inventories
 * and prevent actions in grave lists.
 */
public class InventoryDragListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs an InventoryDragListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public InventoryDragListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the InventoryClickEvent to update grave inventories and manage grave lists.
     * Updates the grave inventory in the database when a Grave inventory is interacted with,
     * and cancels the event if interacting with a GraveList inventory.
     *
     * @param event The InventoryClickEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView inventoryView = event.getView();
        Inventory topInventory = CompatibilityInventoryView.getTopInventory(inventoryView);

        InventoryHolder inventoryHolder = topInventory.getHolder();

        if (isGraveInventory(inventoryHolder)) {
            handleGraveInventoryInteraction((Grave) inventoryHolder);
        } else if (isGraveListInventory(inventoryHolder)) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if the inventory holder is a Grave.
     *
     * @param inventoryHolder The inventory holder to check.
     * @return True if the inventory holder is a Grave, false otherwise.
     */
    private boolean isGraveInventory(InventoryHolder inventoryHolder) {
        return inventoryHolder instanceof Grave;
    }

    /**
     * Checks if the inventory holder is a GraveList.
     *
     * @param inventoryHolder The inventory holder to check.
     * @return True if the inventory holder is a GraveList, false otherwise.
     */
    private boolean isGraveListInventory(InventoryHolder inventoryHolder) {
        return inventoryHolder instanceof GraveList;
    }

    /**
     * Handles interactions with Grave inventories by updating the grave inventory in the database.
     *
     * @param grave The grave whose inventory was interacted with.
     */
    private void handleGraveInventoryInteraction(Grave grave) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                plugin.getDataManager().updateGrave(grave, "inventory",
                        InventoryUtil.inventoryToString(grave.getInventory())), 1L);
    }
}