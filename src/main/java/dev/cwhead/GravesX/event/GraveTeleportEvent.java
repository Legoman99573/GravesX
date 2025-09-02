package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that is triggered when a player teleports to a grave.
 * <p>
 * This event is fired when a player teleports to a specified location associated with a grave.
 * It extends from the {@link GraveEntityEvent} class, inheriting the basic event properties.
 * </p>
 */
public class GraveTeleportEvent extends GraveEntityEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new {@code GraveTeleportEvent}.
     *
     * @param grave  The grave associated with the event.
     * @param entity The entity who is teleporting to the grave.
     */
    public GraveTeleportEvent(@NotNull Grave grave, @NotNull Entity entity) {
        super(grave, entity, grave.getLocationDeath(), null, null, (entity instanceof LivingEntity) ? (LivingEntity) entity : null, null
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