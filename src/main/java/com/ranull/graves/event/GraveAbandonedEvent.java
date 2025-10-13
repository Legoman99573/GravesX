package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveAbandonedEvent} instead. Will be removed in 4.9.15.1.
 * Represents an event that occurs when a grave is abandoned.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEvent} and provides information about the grave
 * that is abandoned.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
public class GraveAbandonedEvent extends dev.cwhead.GravesX.event.GraveAbandonedEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveAbandonedEvent} instead.
     * Constructs a new {@code GraveAbandonedEvent}.
     *
     * @param grave The grave that is abandoned. The location of the grave at the time
     *              of abandonment is automatically set from the grave's death location.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
    public GraveAbandonedEvent(@NotNull Grave grave) {
        super(grave);
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
