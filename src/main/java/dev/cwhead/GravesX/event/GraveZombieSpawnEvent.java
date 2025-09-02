package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents an event that occurs when a zombie spawns at a grave.
 * <p>
 * This event extends {@link GraveEvent} and provides details about the location of the spawn
 * and the entity that the zombie is targeting.
 * </p>
 */
public class GraveZombieSpawnEvent extends GraveEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     *  The entity that the zombie is targeting.
     */
    private final @NotNull LivingEntity targetEntity;

    /**
     * Constructs a new {@code GraveZombieSpawnEvent}.
     *
     * @param location     The location where the zombie is spawning.
     * @param targetEntity The entity that the zombie is targeting.
     * @param grave        The grave associated with the event.
     */
    public GraveZombieSpawnEvent(@NotNull Location location, @NotNull LivingEntity targetEntity, @NotNull Grave grave) {
        super(grave, location, null, null);
        this.targetEntity = Objects.requireNonNull(targetEntity, "targetEntity");
    }

    /**
     * Gets the Target Entity
     *
     * @return the targeted living entity.
     */
    public @NotNull LivingEntity getTargetEntity() {
        return targetEntity;
    }

    /**
     * Gets the Target Entity Type
     *
     * @return the type of the targeted entity.
     */
    public @NotNull EntityType getTargetEntityType() {
        return targetEntity.getType();
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
