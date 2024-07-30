package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GraveZombieSpawnEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new GraveZombieSpawnEvent.
     *
     * @param targetEntity    The entity that the zombie is targeting.
     * @param location        The location where the zombie is spawning.
     * @param grave           The grave associated with the event.
     */
    public GraveZombieSpawnEvent(Location location, LivingEntity targetEntity, Grave grave) {
        super(grave, null, location, null, null, null, targetEntity, null);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}