package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an event that occurs after a grave creation attempt has completed.
 *
 * <p>This is a post event: it fires after {@link GraveCreateEvent} and after placement has been attempted.
 * It is primarily intended for addons/integrations that want to react to the outcome.</p>
 *
 * <p>The placed location may be {@code null} if the grave was not placed (e.g. failed placement, void death
 * with safe-location disabled, or placement handled elsewhere).</p>
 */
public class GravePostCreateEvent extends GraveEntityEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The final location the grave was placed at, or {@code null} if not placed.
     */
    private final @Nullable Location placedLocation;

    /**
     * Constructs a new {@code GravePostCreateEvent}.
     *
     * @param entity         The entity the grave belongs to.
     * @param grave          The grave that was created for this death.
     * @param placedLocation The final placement location, or {@code null} if not placed.
     */
    public GravePostCreateEvent(@NotNull Entity entity, @NotNull Grave grave, @Nullable Location placedLocation) {
        super(grave,
                entity,
                grave.getLocationDeath(),
                null,
                null,
                (entity instanceof LivingEntity) ? (LivingEntity) entity : null,
                null);

        this.placedLocation = placedLocation;
    }

    /**
     * Gets the final placement location.
     *
     * @return The location the grave was placed at, or {@code null} if not placed.
     */
    public @Nullable Location getPlacedLocation() {
        return placedLocation;
    }

    /**
     * Checks whether the grave was successfully placed.
     *
     * @return {@code true} if {@link #getPlacedLocation()} is non-null.
     */
    public boolean isPlaced() {
        return placedLocation != null;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
