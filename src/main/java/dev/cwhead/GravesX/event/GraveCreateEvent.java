package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that occurs when a grave is created for an entity.
 * <p>
 * This event extends {@link GraveEntityEvent} and is cancellable, allowing event listeners
 * to prevent the creation of the grave if necessary.
 * </p>
 */
public class GraveCreateEvent extends GraveEntityEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new {@code GraveCreateEvent}.
     *
     * @param entity The entity for which the grave is being created.
     * @param grave  The grave being created.
     */
    public GraveCreateEvent(@NotNull Entity entity, @NotNull Grave grave) {
        super(grave, entity, grave.getLocationDeath(), null, null, (entity instanceof LivingEntity) ? (LivingEntity) entity : null, null);
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