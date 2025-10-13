package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveWalkOverEvent} instead. Will be removed in 4.9.15.1.
 * Represents an event where a grave is walked over.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEntityEvent} and provides information about the entity involved,
 * the location of the grave, and the grave itself.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
public class GraveWalkOverEvent extends dev.cwhead.GravesX.event.GraveWalkOverEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveWalkOverEvent} instead.
     * Constructs a new {@code GraveWalkOverEvent}.
     *
     * @param entity   The entity involved in the walk over process.
     * @param location The location of the grave being walked over.
     * @param grave    The grave that is being walked over and looted.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.15.1")
    public GraveWalkOverEvent(@NotNull Entity entity, @NotNull Location location, @NotNull Grave grave) {
        super(entity, location, grave);
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