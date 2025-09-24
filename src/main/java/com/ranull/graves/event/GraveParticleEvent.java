package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GravePlayerEvent;
import dev.cwhead.GravesX.exception.GravesXEventIllegalArgumentException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveParticleEvent} instead. Will be removed in 4.9.10.1.
 * Represents an event that occurs when a particle is spawned to a grave location.
 * <p>
 * This event extends {@link GravePlayerEvent} and is cancellable, allowing event listeners
 * to prevent the creation of the grave if necessary.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
public class GraveParticleEvent extends dev.cwhead.GravesX.event.GraveParticleEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveParticleEvent} instead.
     * Constructs a new {@code GraveParticleEvent}.
     *
     * @param player The player for which is spawning the particles from a compass.
     * @param grave  The grave being created.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
    public GraveParticleEvent(@NotNull Player player, @NotNull Grave grave) {
        super(player, grave);
    }

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveParticleEvent} instead.
     * Constructs a new {@code GraveParticleEvent}.
     *
     * @param entity The entity for which is spawning the particles from a compass.
     * @param grave  The grave being created.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.10.1")
    public GraveParticleEvent(@NotNull Entity entity, @NotNull Grave grave) {
        super(requirePlayer(entity), grave);
    }

    private static @NotNull Player requirePlayer(@NotNull Entity entity) {
        if (entity instanceof Player p) return p;
        throw new GravesXEventIllegalArgumentException(
                "GraveParticleEvent requires a Player. Received " + entity.getType() + " instead."
        );
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