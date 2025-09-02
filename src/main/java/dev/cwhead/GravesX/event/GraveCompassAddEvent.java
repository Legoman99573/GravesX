package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GravePlayerEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that occurs when a grave compass is added to a users inventory.
 * <p>
 * This event extends {@link GravePlayerEvent} and is cancellable, allowing event listeners
 * to prevent the creation of the grave if necessary.
 * </p>
 */
public class GraveCompassAddEvent extends GravePlayerEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new {@code GraveCompassAddEvent}.
     *
     * @param player The player for which is using the compass.
     * @param grave  The grave being created.
     */
    public GraveCompassAddEvent(@NotNull Player player, @NotNull Grave grave) {
        super(grave, player, grave.getLocationDeath(), null, null, null, player, null);
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