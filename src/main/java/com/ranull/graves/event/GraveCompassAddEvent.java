package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.exception.GravesXEventIllegalArgumentException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveCompassAddEvent} instead. Will be removed in 4.9.10.1.
 * Represents an event that occurs when a grave compass is added to a users inventory.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GravePlayerEvent} and is cancellable, allowing event listeners
 * to prevent the creation of the grave if necessary.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
public class GraveCompassAddEvent extends dev.cwhead.GravesX.event.GraveCompassAddEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveCompassAddEvent} instead.
     * Constructs a new {@code GraveCompassAddEvent}.
     *
     * @param player The player for which is using the compass.
     * @param grave  The grave being created.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
    public GraveCompassAddEvent(@NotNull Player player, @NotNull Grave grave) {
        super(player, grave);
    }

    /**
     * @deprecated Use {@link #GraveCompassAddEvent(Player, Grave)} instead.
     * Constructs a new {@code GraveCompassAddEvent}.
     *
     * @param entity The entity for which is using the compass.
     * @param grave  The grave being created.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
    public GraveCompassAddEvent(@NotNull Entity entity, @NotNull Grave grave) {
        super(requirePlayer(entity), grave);
    }

    private static @NotNull Player requirePlayer(@NotNull Entity entity) {
        if (entity instanceof Player p) return p;
        throw new GravesXEventIllegalArgumentException("GraveCompassAddEvent requires a Player; got " + entity.getType());
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