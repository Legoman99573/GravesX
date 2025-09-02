package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import dev.cwhead.GravesX.exception.GravesXEventNullPointerException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired just before an entity is teleported to a grave.
 * <p>
 * This event extends {@link GraveEntityEvent} and is cancellable, allowing listeners
 * to prevent the teleport.
 * </p>
 */
public class GravePreTeleportEvent extends GraveEntityEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     The entity attempting to teleport (cached locally for convenience helpers).
     */
    private final @NotNull Entity entity;

    /**
     * Constructs a new {@code GravePreTeleportEvent}.
     *
     * @param grave  The grave associated with the event.
     * @param entity The entity who is teleporting to the grave.
     */
    public GravePreTeleportEvent(@NotNull Grave grave, @NotNull Entity entity) {
        super(grave, entity, grave.getLocationDeath(), null, null, (entity instanceof LivingEntity) ? (LivingEntity) entity : null, null);
        this.entity = entity;
    }

    /**
     * Checks if the entity is a player.
     *
     * @return {@code true} if the teleporting entity is a {@link Player}.
     */
    public boolean isPlayer() {
        return entity instanceof Player;
    }

    /**
     * Gets the player.
     *
     * @return The teleporting {@link Player}.
     * @throws GravesXEventNullPointerException if the teleporting entity is not a player.
     */
    public @NotNull Player getPlayer() {
        if (!(entity instanceof Player)) {
            throw new GravesXEventNullPointerException(this, "player");
        }
        return (Player) entity;
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