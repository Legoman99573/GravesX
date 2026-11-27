package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import dev.cwhead.GravesX.exception.GravesXNullPointerException;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an event that occurs when a Players Head is added to a grave.
 * <p>
 * This event extends {@link GraveEvent} and is cancellable, allowing event listeners
 * to prevent Player Heads from being included in graves.
 * </p>
 */
public class GravePlayerHeadDropEvent extends GraveEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /** The entity for which the player head will be dropped (optional). */
    private final @Nullable Entity entity;

    /**
     * Constructs a new {@code GravePlayerHeadDropEvent}.
     *
     * @param grave    The grave associated with the event.
     * @param location The location where the player head will be dropped.
     * @param entity   The entity for which the player head will be dropped (nullable).
     */
    public GravePlayerHeadDropEvent(@NotNull Grave grave, @NotNull Location location, @Nullable Entity entity) {
        super(Objects.requireNonNull(grave, "grave"), Objects.requireNonNull(location, "location"), null, null);
        this.entity = entity;
    }

    /**
     * Checks of there is an entity present.
     *
     * @return true if an entity is present on this event.
     */
    public boolean hasEntity() {
        return entity != null;
    }

    /**
     * Gets the entity for which the player head will be dropped.
     *
     * @return The entity.
     * @throws GravesXNullPointerException if no entity is present.
     */
    public @NotNull Entity getEntity() {
        if (entity == null)
            throw new GravesXNullPointerException(this, "entity");
        return entity;
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