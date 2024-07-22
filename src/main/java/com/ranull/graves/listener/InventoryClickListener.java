package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.InventoryUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener for handling InventoryClickEvent to manage grave-related inventory interactions.
 */
public class InventoryClickListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs an InventoryClickListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public InventoryClickListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the InventoryClickEvent to perform actions based on the type of inventory holder.
     * Updates grave inventories and handles interactions with GraveList and GraveMenu inventories.
     *
     * @param event The InventoryClickEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();

        if (inventoryHolder != null) {
            if (inventoryHolder instanceof Grave) {
                Grave grave = (Grave) inventoryHolder;

                // Schedule a task to update the grave's inventory in the data manager
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        plugin.getDataManager().updateGrave(grave, "inventory",
                                InventoryUtil.inventoryToString(grave.getInventory())), 1L);
            } else if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();

                if (inventoryHolder instanceof GraveList) {
                    GraveList graveList = (GraveList) event.getInventory().getHolder();
                    Grave grave = graveList.getGrave(event.getSlot());

                    if (grave != null) {
                        // Run function associated with the clicked slot in GraveList
                        plugin.getEntityManager().runFunction(player, plugin.getConfig("gui.menu.list.function", grave)
                                .getString("gui.menu.list.function", "menu"), grave);
                        plugin.getGUIManager().setGraveListItems(graveList.getInventory(), graveList.getUUID());
                    }

                    event.setCancelled(true);
                } else if (inventoryHolder instanceof GraveMenu) {
                    GraveMenu graveMenu = (GraveMenu) event.getInventory().getHolder();
                    Grave grave = graveMenu.getGrave();

                    if (grave != null) {
                        // Run function associated with the clicked slot in GraveMenu
                        plugin.getEntityManager().runFunction(player,
                                plugin.getConfig("gui.menu.grave.slot." + event.getSlot() + ".function", grave)
                                        .getString("gui.menu.grave.slot." + event.getSlot()
                                                + ".function", "none"), grave);
                        plugin.getGUIManager().setGraveMenuItems(graveMenu.getInventory(), grave);
                    }

                    event.setCancelled(true);
                }
            }
        }
    }
}