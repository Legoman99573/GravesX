package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that occurs when an already abandoned grave's abandonment duration expires.
 * <p>
 * This event extends {@link GraveEvent} and implements {@link Cancellable}, allowing other plugins
 * to prevent the grave from being dropped or removed after abandonment expiry.
 * </p>
 */
public class GraveAbandonedExpiredEvent extends GraveEvent implements Cancellable {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;

    /**
     * Constructs a new {@code GraveAbandonedExpiredEvent}.
     *
     * @param grave The grave that has reached the end of its abandonment timeout.
     */
    public GraveAbandonedExpiredEvent(Grave grave) {
        super(grave, null, grave.getLocationDeath(), null, null, null, null, null, null);
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list for this event.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
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
     * Checks if the event has been cancelled.
     *
     * @return {@code true} if the event is cancelled; {@code false} otherwise.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancellation state of this event.
     *
     * @param cancel {@code true} to cancel the event; {@code false} to allow it.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
