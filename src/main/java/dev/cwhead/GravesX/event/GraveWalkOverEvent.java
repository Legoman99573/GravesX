package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents an event where a grave is walked over.
 * <p>
 * This event extends {@link GraveEntityEvent} and provides information about the entity involved,
 * the location of the grave, and the grave itself.
 * </p>
 */
public class GraveWalkOverEvent extends GraveEntityEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new {@code GraveWalkOverEvent}.
     *
     * @param entity   The entity involved in the walk over process.
     * @param location The location of the grave being walked over.
     * @param grave    The grave that is being walked over and looted.
     */
    public GraveWalkOverEvent(@NotNull Entity entity, @NotNull Location location, @NotNull Grave grave) {
        super(Objects.requireNonNull(grave, "grave"), Objects.requireNonNull(entity, "entity"), Objects.requireNonNull(location, "location"), null, grave.getLocationDeath().getBlock(), (entity instanceof LivingEntity le) ? le : null, null);
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