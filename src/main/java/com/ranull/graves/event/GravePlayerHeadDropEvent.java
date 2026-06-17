package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GravePlayerHeadDropEvent} instead. Will be removed in 2027.4.9.1.
 * Represents an event that occurs when a Players Head is added to a grave.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEvent} and is cancellable, allowing event listeners
 * to prevent Player Heads from being included in graves.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
public class GravePlayerHeadDropEvent extends dev.cwhead.GravesX.event.GravePlayerHeadDropEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GravePlayerHeadDropEvent} instead.
     * Constructs a new {@code GravePlayerHeadDropEvent}.
     *
     * @param grave    The grave associated with the event.
     * @param location The location where the player head will be dropped.
     * @param entity   The entity for which the player head will be dropped (nullable).
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
    public GravePlayerHeadDropEvent(@NotNull Grave grave, @NotNull Location location, @Nullable Entity entity) {
        super(grave, location, entity);
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