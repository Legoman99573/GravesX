package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.exception.GravesXEventIllegalArgumentException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveOpenEvent} instead. Will be removed in 4.9.15.1.
 * Represents an event that occurs when a player opens an inventory associated
 * with a grave.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GravePlayerEvent} and includes additional information
 * about the grave and the inventory view that is being opened.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
public class GraveOpenEvent extends dev.cwhead.GravesX.event.GraveOpenEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveOpenEvent} instead.
     * Constructs a new {@code GraveOpenEvent}.
     *
     * @param inventoryView The inventory view that is being opened.
     * @param grave         The grave associated with the inventory view.
     * @param player        The player who is opening the inventory.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
    public GraveOpenEvent(@NotNull InventoryView inventoryView, @NotNull Grave grave, @NotNull Player player) {
        super(inventoryView, grave, player);
    }

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveOpenEvent} instead.
     * Constructs a new {@code GraveOpenEvent}.
     *
     * @param inventoryView The inventory view that is being opened.
     * @param grave         The grave associated with the inventory view.
     * @param entity        The entity who is opening the inventory.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
    public GraveOpenEvent(@NotNull InventoryView inventoryView, @NotNull Grave grave, @NotNull Entity entity) {
        super(inventoryView, grave, requirePlayer(entity));
    }

    private static @NotNull Player requirePlayer(@NotNull Entity entity) {
        if (entity instanceof Player p) return p; // Java 17 pattern matching
        throw new GravesXEventIllegalArgumentException(
                "GraveOpenEvent requires a Player. Received " + entity.getType() + " instead."
        );
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