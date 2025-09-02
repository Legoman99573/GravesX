package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.exception.GravesXEventIllegalArgumentException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveCloseEvent} instead.
 * Represents an event that occurs when an inventory associated with a grave is closed.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEvent} and provides information about the grave
 * and the player involved when the inventory is closed.
 * </p>
 */
@Deprecated (since = "4.9.9.1", forRemoval = true)
public class GraveCloseEvent extends dev.cwhead.GravesX.event.GraveCloseEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveCloseEvent} instead.
     * Constructs a new {@code GraveCloseEvent}.
     *
     * @param inventoryView The inventory view that is being closed.
     * @param grave         The grave associated with the inventory view.
     * @param player        The player who is closing the inventory.
     */
    @Deprecated (since = "4.9.9.1", forRemoval = true)
    public GraveCloseEvent(@NotNull InventoryView inventoryView, @NotNull Grave grave, @NotNull Player player) {
        super(inventoryView, grave, player);
    }

    /**
     * @deprecated Use {@link #GraveCloseEvent(InventoryView, Grave, Player)} instead.
     * Constructs a new {@code GraveCloseEvent}.
     *
     * @param grave         The grave associated with the inventory view.
     * @param inventoryView The inventory view that is being closed.
     * @param entity        The entity who is closing the inventory.
     */
    @Deprecated (since = "4.9.9.1", forRemoval = true)
    public GraveCloseEvent(@NotNull Grave grave, @NotNull InventoryView inventoryView, @NotNull Entity entity) {
        super(inventoryView, grave, requirePlayer(entity));
    }

    private static @NotNull Player requirePlayer(@NotNull Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        throw new GravesXEventIllegalArgumentException("GraveCloseEvent requires a Player. Received " + entity.getType() + " instead");
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
