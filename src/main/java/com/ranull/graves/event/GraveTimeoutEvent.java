package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveTimeoutEvent} instead. Will be removed in 4.9.15.1.
 * Represents an event that occurs when a grave times out.
 * <p>
 * This event extends {@link GraveEvent} and is cancellable, allowing event listeners
 * to prevent the grave timeout from occurring if necessary.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
public class GraveTimeoutEvent extends dev.cwhead.GravesX.event.GraveTimeoutEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveTimeoutEvent} instead.
     * Constructs a new {@code GraveTimeoutEvent}.
     *
     * @param grave The grave that is timing out. The location of the grave at the time
     *              of timeout is automatically set from the grave's death location.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
    public GraveTimeoutEvent(@NotNull Grave grave) {
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