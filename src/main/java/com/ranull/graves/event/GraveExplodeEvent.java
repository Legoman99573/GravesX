package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveExplodeEvent} instead.
 * Represents an event that occurs when a grave explodes.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEvent} and is cancellable, allowing event listeners
 * to prevent the explosion from occurring if necessary.
 * </p>
 */
@Deprecated (since = "4.9.9.1", forRemoval = true)
public class GraveExplodeEvent extends dev.cwhead.GravesX.event.GraveExplodeEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveExplodeEvent} instead.
     * Constructs a new {@code GraveExplodeEvent}.
     *
     * @param location The location where the explosion occurs.
     * @param entity   The entity that caused the explosion, if any. This may be {@code null}
     *                 if no specific entity caused the explosion.
     * @param grave    The grave that is exploding.
     */
    @Deprecated (since = "4.9.9.1", forRemoval = true)
    public GraveExplodeEvent(@NotNull Location location, @Nullable Entity entity, @NotNull Grave grave) {
        super(location, entity, grave);
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
