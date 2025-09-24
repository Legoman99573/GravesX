package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import dev.cwhead.GravesX.exception.GravesXEventIllegalArgumentException;
import dev.cwhead.GravesX.exception.GravesXEventMethodNotSupportedException;
import dev.cwhead.GravesX.exception.GravesXEventNullPointerException;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an event that occurs when an Obituary is added to a grave.
 * <p>
 * This event extends {@link GraveEvent} and is cancellable, allowing event listeners
 * to prevent obituaries from being included in graves.
 * </p>
 */
public class GraveObituaryAddEvent extends GraveEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The entity for which the grave is being created
     */
    private final @Nullable Entity entity;

    /**
     * Constructs a new {@code GraveObituaryAddEvent}.
     *
     * @param grave    The grave associated with the event.
     * @param location The location associated with this obituary addition.
     * @param entity   The entity for which the grave is being created (nullable).
     */
    public GraveObituaryAddEvent(@NotNull Grave grave, @NotNull Location location, @Nullable Entity entity) {
        super(Objects.requireNonNull(grave, "grave"), Objects.requireNonNull(location, "location"), null, null);
        this.entity = entity;
    }

    /**
     * Checks if there is an entity.
     *
     * @return true if an entity is present on this event.
     */
    public boolean hasEntity() {
        return entity != null;
    }

    /**
     * Gets the entity for which the grave is being created.
     *
     * @return The entity.
     * @throws GravesXEventMethodNotSupportedException if no entity is present.
     */
    public @NotNull Entity getEntity() {
        if (entity == null)
            throw new GravesXEventNullPointerException(this, "entity");
        return entity;
    }

    /**
     * Checks if the grave is a player.
     *
     * @return true if player, false otherwise
     */
    public boolean hasPlayer() {
        if (entity instanceof Player p) {
            Player player = p.getPlayer();
            return player != null;
        }
        return false;
    }

    /**
     * Gets the entity for which the grave is being created.
     *
     * @return The entity.
     * @throws GravesXEventNullPointerException if player is null
     * @throws GravesXEventIllegalArgumentException if entity pulled was not a player
     */
    public @NotNull Player getPlayer() {
        if (entity instanceof Player p) {
            Player player = p.getPlayer();
            if (player != null) {
                return player;
            }
            throw new GravesXEventNullPointerException(this, "player");
        }
        EntityType entityType = (entity != null) ? entity.getType() : EntityType.UNKNOWN;
        throw new GravesXEventIllegalArgumentException("GraveObituaryAddEvent made a call to get player. Received " + entityType + " instead.");
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