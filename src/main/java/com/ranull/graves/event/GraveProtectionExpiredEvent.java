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
     * @param entity The entity associated with the event, if applicable.
     */
    public GraveProtectionExpiredEvent(Grave grave, Entity entity) {
        super(grave, entity, grave.getLocationDeath(), null, null, null, null, entity instanceof Player ? (Player) entity : null);
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