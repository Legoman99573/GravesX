package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.exception.GravesXIllegalArgumentException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveLootedEvent} instead. Will be removed in 2027.4.9.1.
 * Represents an event that occurs when an inventory associated with a grave is completely looted.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GravePlayerEvent} and provides information about the grave
 * and the player involved when the inventory is completely looted.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
public class GraveLootedEvent extends dev.cwhead.GravesX.event.GraveLootedEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveLootedEvent} instead.
     * Constructs a new {@code GraveLootedEvent}.
     *
     * @param inventoryView The inventory view that has been fully looted.
     * @param grave         The grave associated with the inventory view.
     * @param player        The player who is closing the inventory.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
    public GraveLootedEvent(@NotNull InventoryView inventoryView,
                            @NotNull Grave grave,
                            @NotNull Player player) {
        super(inventoryView, grave, player);
    }

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveLootedEvent} instead.
     * Constructs a new {@code GraveLootedEvent}.
     *
     * @param inventoryView The inventory view that has been fully looted.
     * @param grave         The grave associated with the inventory view.
     * @param entity        The entity who is closing the inventory.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
    public GraveLootedEvent(@NotNull Grave grave, @NotNull InventoryView inventoryView, @NotNull Entity entity) {
        super(inventoryView, grave, requirePlayer(entity));
    }

    private static @NotNull Player requirePlayer(@NotNull Entity entity) {
        if (entity instanceof Player p) return p;
        throw new GravesXIllegalArgumentException(
                "GraveLootedEvent requires a Player. Received " + entity.getType() + " instead."
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