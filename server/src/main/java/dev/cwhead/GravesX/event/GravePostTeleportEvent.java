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
 * Triggered after an entity has successfully teleported as part of a grave teleport.
 * <p>
 * If cancelled, GravesX should attempt to roll the entity back to the "from" location.
 * </p>
 */
public class GravePostTeleportEvent extends GraveEntityEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location from;

    private final Location to;
    /**
     * Constructs a new {@code GravePostTeleportEvent}.
     *
     * @param grave  the grave associated with the teleport
     * @param entity the entity that teleported
     * @param from   the location before teleport
     * @param to     the location after teleport
     */
    public GravePostTeleportEvent(@NotNull Grave grave,
                                  @NotNull Entity entity,
                                  @NotNull Location from,
                                  @NotNull Location to) {
        super(
                Objects.requireNonNull(grave, "grave"),
                Objects.requireNonNull(entity, "entity"),
                Objects.requireNonNull(to, "to"),
                null,
                null,
                (entity instanceof LivingEntity le) ? le : null,
                null
        );

        this.from = Objects.requireNonNull(from, "from").clone();
        this.to = to.clone();
    }

    /**
     * Location before teleport (cloned).
     */
    public @NotNull Location getFrom() {
        return from.clone();
    }

    /**
     * Location after teleport (cloned).
     */
    public @NotNull Location getTo() {
        return to.clone();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}