package com.ranull.graves.inventory;

import com.ranull.graves.type.Grave;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a menu for a specific grave, displayed in an inventory.
 * Implements InventoryHolder to manage inventory display.
 */
public class GraveMenu implements InventoryHolder {
    private final Grave grave;
    private Inventory inventory;

    /**
     * Constructs a new GraveMenu instance for the specified grave.
     *
     * @param grave The Grave object associated with this menu.
     */
    public GraveMenu(Grave grave) {
        this.grave = grave;
    }

    /**
     * Gets the inventory associated with this GraveMenu.
     *
     * @return The Inventory object.
     */
    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets the inventory for this GraveMenu.
     *
     * @param inventory The Inventory object to set.
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Gets the Grave object associated with this menu.
     *
     * @return The Grave object.
     */
    public Grave getGrave() {
        return grave;
    }
}