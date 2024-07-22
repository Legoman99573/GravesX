package com.ranull.graves.inventory;

import com.ranull.graves.type.Grave;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Represents a list of graves that can be displayed in an inventory.
 * Implements InventoryHolder to manage inventory display.
 */
public class GraveList implements InventoryHolder {
    private final UUID uuid;
    private final List<Grave> graveList;
    private Inventory inventory;

    /**
     * Constructs a new GraveList instance with the given UUID and list of graves.
     *
     * @param uuid       The UUID associated with this GraveList.
     * @param graveList  The list of graves to be managed.
     */
    public GraveList(UUID uuid, List<Grave> graveList) {
        this.uuid = uuid;
        this.graveList = graveList;
    }

    /**
     * Gets the inventory associated with this GraveList.
     *
     * @return The Inventory object.
     */
    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets the inventory for this GraveList.
     *
     * @param inventory The Inventory object to set.
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Gets the UUID associated with this GraveList.
     *
     * @return The UUID.
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Gets the Grave object at the specified slot in the grave list.
     *
     * @param slot The slot index to retrieve the grave from.
     * @return The Grave object at the specified slot, or null if the slot is invalid.
     */
    public Grave getGrave(int slot) {
        return slot >= 0 && graveList.size() > slot ? graveList.get(slot) : null;
    }
}