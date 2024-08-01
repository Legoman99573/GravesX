package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveCreateEvent class represents an event where a grave is created for an entity.
 * This event is cancellable, meaning it can be prevented from occurring by event listeners.
 */
public class GraveCreateEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new GraveCreateEvent.
     *
     * @param entity The entity for which the grave is being created.
     * @param grave  The grave being created.
     */
    public GraveCreateEvent(Entity entity, Grave grave) {
        super(grave, entity, null, null, null, null, null, null);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}