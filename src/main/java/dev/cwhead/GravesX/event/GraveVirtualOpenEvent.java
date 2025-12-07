package dev.cwhead.GravesX.event;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a virtual grave open is about to be processed.
 * <p>
 * Extends {@link GraveEntityEvent} and adds:
 * <ul>
 *     <li>The actual distance between the entity and the grave location.</li>
 *     <li>A configurable/overridable maximum distance.</li>
 * </ul>
 * <p>
 * If the event is cancelled, the virtual open will not continue.
 */
public class GraveVirtualOpenEvent extends GraveEntityEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The actual distance between the entity and the grave location.
     */
    private final double distance;

    /**
     * The maximum allowed distance for opening the grave virtually.
     * <p>
     * Convention:
     * <ul>
     *     <li>{@code maxDistance < 0} &rarr; unlimited distance</li>
     * </ul>
     * This value may be modified by listeners.
     */
    private double maxDistance;

    /**
     * Whether this event has been cancelled.
     */
    private boolean cancelled;

    /**
     * Constructs a new {@code GraveVirtualOpenEvent}.
     *
     * @param grave        The grave associated with the event.
     * @param entity       The entity attempting to open the grave (non-null).
     * @param location     The location context of the event (often the entity location).
     * @param blockType    The block type involved, if any.
     * @param block        The block involved, if any.
     * @param livingEntity The living entity associated with the event, if any.
     * @param targetEntity The target entity of the event, if any.
     * @param distance     The actual distance between the entity and the grave location.
     * @param maxDistance  The maximum allowed distance (negative for unlimited).
     */
    public GraveVirtualOpenEvent(
            @NotNull Grave grave,
            @NotNull Entity entity,
            @Nullable Location location,
            @Nullable BlockData.BlockType blockType,
            @Nullable Block block,
            @Nullable LivingEntity livingEntity,
            @Nullable LivingEntity targetEntity,
            double distance,
            double maxDistance
    ) {
        super(grave, entity, location, blockType, block, livingEntity, targetEntity);
        this.distance = distance;
        this.maxDistance = maxDistance;
    }

    /**
     * Gets the actual distance between the entity and the grave location.
     *
     * @return The actual distance.
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Gets the maximum allowed distance for this event.
     * <p>
     * A negative value means unlimited distance.
     *
     * @return The maximum allowed distance.
     */
    public double getMaxDistance() {
        return maxDistance;
    }

    /**
     * Sets the maximum allowed distance for this event.
     * <p>
     * A negative value means unlimited distance.
     *
     * @param maxDistance New maximum distance.
     */
    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
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