package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import dev.cwhead.GravesX.exception.GravesXNullPointerException;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

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
        super(Objects.requireNonNull(grave, "grave"), Objects.requireNonNull(location, "location"), null, grave.getLocationDeath().getBlock());
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
     * Returns whether the target is a {@link Player}.
     *
     * @return {@code true} if the target is a player; otherwise {@code false}.
     */
    public boolean hasPlayerTarget() {
        return targetEntity instanceof Player;
    }

    /**
     * Returns the targeted {@link Player}.
     *
     * @return the player being targeted.
     * @throws GravesXNullPointerException if the target is not a player.
     */
    public @NotNull Player getTargetPlayer() {
        if (!(targetEntity instanceof Player p)) {
            throw new GravesXNullPointerException(this, "player");
        }
        return p;
    }

    /**
     * Returns the targeted player's name.
     *
     * @return the player's current name.
     * @throws GravesXNullPointerException if the target is not a player.
     */
    public @NotNull String getTargetPlayerName() {
        return getTargetPlayer().getName();
    }

    /**
     * Returns the targeted player's UUID.
     *
     * @return the player's unique identifier.
     * @throws GravesXNullPointerException if the target is not a player.
     */
    public @NotNull UUID getTargetPlayerUniqueId() {
        return getTargetPlayer().getUniqueId();
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