package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import dev.cwhead.GravesX.exception.GravesXEventNullPointerException;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an event that occurs when a grave explodes.
 * <p>
 * This event extends {@link GraveEvent} and is cancellable, allowing event listeners
 * to prevent the explosion from occurring if necessary.
 * </p>
 */
public class GraveExplodeEvent extends GraveEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The entity that caused the explosion, if any.
     */
    private final @Nullable Entity entity;

    /**
     * Constructs a new {@code GraveExplodeEvent}.
     *
     * @param location The location where the explosion occurs.
     * @param entity   The entity that caused the explosion, if any. This may be {@code null}
     *                 if no specific entity caused the explosion.
     * @param grave    The grave that is exploding.
     */
    public GraveExplodeEvent(@NotNull Location location, @Nullable Entity entity, @NotNull Grave grave) {
        super(Objects.requireNonNull(grave, "grave"), Objects.requireNonNull(location, "location"), null, null);
        this.entity = entity;
    }

    /**
     * @return {@code true} if there is a source entity for this explosion.
     */
    public boolean hasEntity() {
        return entity != null;
    }

    /**
     * Gets the entity that caused the explosion.
     *
     * @return The entity that caused the explosion.
     * @throws GravesXEventNullPointerException if no source entity is present.
     */
    public @NotNull Entity getEntity() {
        if (entity == null) {
            throw new GravesXEventNullPointerException(this, "entity");
        }
        return entity;
    }

    /**
     @return {@code true} if the source entity is a {@link Player}.
     */
    public boolean hasPlayer() {
        return entity instanceof Player;
    }

    /**
     * Gets the player that caused the explosion.
     *
     * @return The player that caused the explosion.
     * @throws GravesXEventNullPointerException if the source is not a player or absent.
     */
    public @NotNull Player getPlayer() {
        if (!(entity instanceof Player p)) {
            throw new GravesXEventNullPointerException(this, "player");
        }
        return p;
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