package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that occurs when an inventory associated with a grave is closed.
 * <p>
 * This event extends {@link GraveEvent} and provides information about the grave
 * and the player involved when the inventory is closed.
 * </p>
 */
public class GraveCloseEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new {@code GraveCloseEvent}.
     *
     * @param inventoryView The inventory view that is being closed.
     * @param grave         The grave associated with the inventory view.
     * @param player        The player who is closing the inventory.
     */
    public GraveCloseEvent(InventoryView inventoryView, Grave grave, Player player) {
        super(grave, null, grave.getLocation(), inventoryView, null, null, null, null, player);
    }

    /**
     * @deprecated Use Player instead of Entity
     * Constructs a new {@code GraveCloseEvent}.
     *
     * @param inventoryView The inventory view that is being closed.
     * @param grave         The grave associated with the inventory view.
     * @param entity        The entity who is closing the inventory.
     */
    public GraveCloseEvent(Grave grave, InventoryView inventoryView, Entity entity) {
        super(grave, entity, grave.getLocation(), inventoryView, null, null, null, null, null);
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list for this event.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the static list of handlers for this event.
     *
     * @return The static handler list for this event.
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}