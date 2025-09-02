package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveCreateEvent} instead.
 * Represents an event that occurs when a grave is created for an entity.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEntityEvent} and is cancellable, allowing event listeners
 * to prevent the creation of the grave if necessary.
 * </p>
 */
@Deprecated (since = "4.9.9.1", forRemoval = true)
public class GraveCreateEvent extends dev.cwhead.GravesX.event.GraveCreateEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveCreateEvent} instead.
     * Constructs a new {@code GraveCreateEvent}.
     *
     * @param entity The entity for which the grave is being created.
     * @param grave  The grave being created.
     */
    @Deprecated (since = "4.9.9.1", forRemoval = true)
    public GraveCreateEvent(@NotNull Entity entity, @NotNull Grave grave) {
        super(entity, grave);
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
