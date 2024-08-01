package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that is triggered when a player teleports to a grave.
 * <p>
 * This event is fired when a player teleports to a specified location associated with a grave.
 * It extends from the {@link GraveEvent} class, inheriting the basic event properties.
 * </p>
 */
public class GraveTeleportEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new {@code GraveTeleportEvent}.
     *
     * @param grave    The grave associated with the event.
     * @param location The location to which the player is teleporting.
     * @param entity   The entity who is teleporting to the grave.
     */
    public GraveTeleportEvent(Grave grave, Entity entity) {
        super(grave, entity, grave.getLocation(), null, null, null, null, null, null);
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list for this event.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
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
}