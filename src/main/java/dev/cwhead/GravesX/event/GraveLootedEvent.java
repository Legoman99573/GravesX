package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GravePlayerEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that occurs when an inventory associated with a grave is completely looted.
 * <p>
 * This event extends {@link GravePlayerEvent} and provides information about the grave
 * and the player involved when the inventory is completely looted.
 * </p>
 */
public class GraveLootedEvent extends GravePlayerEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new {@code GraveLootedEvent}.
     *
     * @param inventoryView The inventory view that has been fully looted.
     * @param grave         The grave associated with the inventory view.
     * @param player        The player who is closing the inventory.
     */
    public GraveLootedEvent(@NotNull InventoryView inventoryView, @NotNull Grave grave, @NotNull Player player) {
        super(grave, player, grave.getLocationDeath(), null, null, inventoryView, player, null);
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