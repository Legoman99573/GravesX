package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link dev.cwhead.GravesX.event.GraveAutoLootEvent} instead.
 * Represents an event where a grave is automatically looted.
 * <p>
 * This event extends {@link dev.cwhead.GravesX.event.graveevent.GraveEvent} and provides information about the entity involved,
 * the location of the grave, and the grave itself.
 * </p>
 */
@Deprecated (since = "4.9.9.1", forRemoval = true)
public class GraveAutoLootEvent extends dev.cwhead.GravesX.event.GraveAutoLootEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @deprecated Use {@link dev.cwhead.GravesX.event.GraveAutoLootEvent} instead.
     * Constructs a new {@code GraveAutoLootEvent}.
     *
     * @param entity   The entity involved in the auto-loot process.
     * @param location The location of the grave being looted.
     * @param grave    The grave that is being looted.
     */
    @Deprecated (since = "4.9.9.1", forRemoval = true)
    public GraveAutoLootEvent(@NotNull Entity entity, @NotNull Location location, @NotNull Grave grave) {
        super(entity, location, grave);
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
