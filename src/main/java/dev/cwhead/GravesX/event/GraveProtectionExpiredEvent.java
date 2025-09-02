package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that occurs when grave protection has expired.
 * <p>
 * This event extends {@link GraveEvent} and is cancellable, allowing event listeners
 * to prevent the expiration of grave protection if necessary.
 * </p>
 */
public class GraveProtectionExpiredEvent extends GraveEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new GraveProtectionExpiredEvent.
     *
     * @param grave The grave whose protection is expiring.
     */
    public GraveProtectionExpiredEvent(@NotNull Grave grave) {
        super(grave, grave.getLocationDeath(), null, null);
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
