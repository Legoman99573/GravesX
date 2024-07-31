package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveCloseEvent class represents an event that occurs when an inventory
 * associated with a grave is closed. This event extends the InventoryCloseEvent
 * and includes additional information about the grave.
 */
public class GraveCloseEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new GraveCloseEvent.
     *
     * @param inventoryView The inventory view that is being closed.
     * @param player The player breaking the block.
     * @param grave         The grave associated with the inventory view.
     */
    public GraveCloseEvent(InventoryView inventoryView, Grave grave, Player player) {
        super(grave, null, grave.getLocation(), inventoryView, null, null, null, player);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}