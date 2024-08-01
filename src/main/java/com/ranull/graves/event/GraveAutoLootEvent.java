package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GraveAutoLootEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public GraveAutoLootEvent(Entity entity, Location location, Grave grave) {
        super(grave, entity, location, null, null, null, null, null, null);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}