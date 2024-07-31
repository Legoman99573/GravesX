package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * The GraveProtectionExpiredEvent class represents an event that occurs when the protection of a grave expires.
 */
public class GraveProtectionExpiredEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new GraveProtectionExpiredEvent.
     *
     * @param grave The grave whose protection is expiring.
     */
    public GraveProtectionExpiredEvent(Grave grave) {
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