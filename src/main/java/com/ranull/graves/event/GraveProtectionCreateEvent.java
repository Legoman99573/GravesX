package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveProtectionCreateEvent} instead. Will be removed in 4.9.10.1.
 * Represents an event that occurs when grave protection is created.
 * <p>
 * This event extends {@link GraveEntityEvent} and is cancellable, allowing event listeners
 * to prevent creation of grave protection if necessary.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
public class GraveProtectionCreateEvent extends dev.cwhead.GravesX.event.GraveProtectionCreateEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveProtectionCreateEvent} instead.
     * Constructs a new {@code GraveProtectionCreateEvent}.
     *
     * @param entity The entity for which the grave protection is being created.
     * @param grave  The grave being protected.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
    public GraveProtectionCreateEvent(@NotNull Entity entity, @NotNull Grave grave) {
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