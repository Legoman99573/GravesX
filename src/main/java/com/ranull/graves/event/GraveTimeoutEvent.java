package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveTimeoutEvent class represents an event that occurs when a grave times out.
 * This event is cancellable, meaning it can be prevented from occurring by event listeners.
 */
public class GraveTimeoutEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new GraveTimeoutEvent.
     *
     * @param grave The grave that is timing out.
     */
    public GraveTimeoutEvent(Grave grave) {
        super(grave, null, grave.getLocationDeath(), null, null, null, null, null);
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}