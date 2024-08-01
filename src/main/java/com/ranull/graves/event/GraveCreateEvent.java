package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that occurs when a grave is created for an entity.
 * <p>
 * This event extends {@link GraveEvent} and is cancellable, allowing event listeners
 * to prevent the creation of the grave if necessary.
 * </p>
 */
public class GraveCreateEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new {@code GraveCreateEvent}.
     *
     * @param entity The entity for which the grave is being created.
     * @param grave  The grave being created.
     */
    public GraveCreateEvent(Entity entity, Grave grave) {
        super(grave, entity, null, null, null, null, null, null, null);
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