package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The GraveExplodeEvent class represents an event where a grave explodes.
 * This event is cancellable, meaning it can be prevented from occurring by event listeners.
 */
public class GraveExplodeEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new GraveExplodeEvent.
     *
     * @param location The location of the explosion.
     * @param entity   The entity that caused the explosion, if any.
     * @param grave    The grave that is exploding.
     */
    public GraveExplodeEvent(Location location, @Nullable Entity entity, Grave grave) {
        super(grave, entity, location, null, null, null, null, null);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}