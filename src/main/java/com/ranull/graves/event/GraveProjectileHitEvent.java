package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveProjectileHitEvent} instead. Will be removed in 2027.4.9.1.
 * Represents an event that occurs when a grave is hit with a projectile.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEntityEvent} and provides information about the grave
 * that is hit with a projectile.
 * </p>
 */
@Deprecated(since = "4.9.9.1", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
public class GraveProjectileHitEvent extends dev.cwhead.GravesX.event.GraveProjectileHitEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveProjectileHitEvent} instead.
     * Constructs a new {@code GraveProjectileHitEvent}.
     *
     * @param location The location of the event.
     * @param player   The player involved in the event, if any.
     * @param grave    The grave associated with the event.
     * @param entity   The projectile entity involved in the event.
     * @param block    The block involved in the event, if any.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
    public GraveProjectileHitEvent(@NotNull Location location, @NotNull Player player, @NotNull Grave grave, @NotNull Entity entity, @Nullable Block block) {
        super(location, player, grave, entity, block);
    }

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveProjectileHitEvent} instead.
     * Constructs a new {@code GraveProjectileHitEvent}.
     *
     * @param location     The location of the event.
     * @param livingEntity The livingEntity involved in the event, if any.
     * @param grave        The grave associated with the event.
     * @param entity       The projectile entity involved in the event.
     * @param block        The block involved in the event, if any.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
    public GraveProjectileHitEvent(@NotNull Location location, @NotNull LivingEntity livingEntity, @NotNull Grave grave, @NotNull Entity entity, @Nullable Block block) {
        super(location, livingEntity, grave, entity, block);
    }

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveProjectileHitEvent} instead.
     * Constructs a new {@code GraveProjectileHitEvent}.
     *
     * @param location The location of the event.
     * @param grave    The grave associated with the event.
     * @param entity   The projectile entity involved in the event.
     * @param block    The block involved in the event, if any.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.4.9.1")
    public GraveProjectileHitEvent(@NotNull Location location, @NotNull Grave grave, @NotNull Entity entity, @Nullable Block block) {
        super(location, grave, entity, block);
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