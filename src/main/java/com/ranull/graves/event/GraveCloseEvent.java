package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveCloseEvent class represents an event that occurs when an inventory
 * associated with a grave is closed. This event extends the InventoryCloseEvent
 * and includes additional information about the grave.
 */
public class GraveCloseEvent extends InventoryCloseEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Grave grave;

    /**
     * Constructs a new GraveCloseEvent.
     *
     * @param inventoryView The inventory view that is being closed.
     * @param grave         The grave associated with the inventory view.
     */
    public GraveCloseEvent(InventoryView inventoryView, Grave grave) {
        super(inventoryView);
        this.grave = grave;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The list of handlers.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The list of handlers.
     */
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the grave associated with the event.
     *
     * @return The grave associated with the event.
     */
    public Grave getGrave() {
        return grave;
    }
}