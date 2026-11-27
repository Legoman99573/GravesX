package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an event that occurs when a grave is hit with a projectile.
 * <p>
 * This event extends {@link GraveEntityEvent} and provides information about the grave
 * that is hit with a projectile.
 * </p>
 */
public class GraveProjectileHitEvent extends GraveEntityEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new {@code GraveProjectileHitEvent}.
     *
     * @param location The location of the event.
     * @param player   The player involved in the event, if any.
     * @param grave    The grave associated with the event.
     * @param entity   The projectile entity involved in the event.
     * @param block    The block involved in the event, if any.
     */
    public GraveProjectileHitEvent(@NotNull Location location,
                                   @NotNull Player player,
                                   @NotNull Grave grave,
                                   @NotNull Entity entity,
                                   @Nullable Block block) {
        super(grave, entity, location, null, block, player, null);
    }

    /**
     * Constructs a new {@code GraveProjectileHitEvent}.
     *
     * @param location     The location of the event.
     * @param livingEntity The livingEntity involved in the event, if any.
     * @param grave        The grave associated with the event.
     * @param entity       The projectile entity involved in the event.
     * @param block        The block involved in the event, if any.
     */
    public GraveProjectileHitEvent(@NotNull Location location,
                                   @NotNull LivingEntity livingEntity,
                                   @NotNull Grave grave,
                                   @NotNull Entity entity,
                                   @Nullable Block block) {
        super(grave, entity, location, null, block, livingEntity, null);
    }

    /**
     * Constructs a new {@code GraveProjectileHitEvent}.
     *
     * @param location The location of the event.
     * @param grave    The grave associated with the event.
     * @param entity   The projectile entity involved in the event.
     * @param block    The block involved in the event, if any.
     */
    public GraveProjectileHitEvent(@NotNull Location location,
                                   @NotNull Grave grave,
                                   @NotNull Entity entity,
                                   @Nullable Block block) {
        super(grave, entity, location, null, block, null, null);
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