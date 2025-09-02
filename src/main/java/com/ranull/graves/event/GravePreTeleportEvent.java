package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GravePreTeleportEvent} instead.
 * Fired just before an entity is teleported to a grave.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEntityEvent} and is cancellable, allowing listeners
 * to prevent the teleport.
 * </p>
 */
@Deprecated (since = "4.9.9.1", forRemoval = true)
public class GravePreTeleportEvent extends dev.cwhead.GravesX.event.GravePreTeleportEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new {@code GravePreTeleportEvent}.
     *
     * @param grave  The grave associated with the event.
     * @param entity The entity who is teleporting to the grave.
     */
    @Deprecated (since = "4.9.9.1", forRemoval = true)
    public GravePreTeleportEvent(@NotNull Grave grave, @NotNull Entity entity) {
        super(grave, entity);
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