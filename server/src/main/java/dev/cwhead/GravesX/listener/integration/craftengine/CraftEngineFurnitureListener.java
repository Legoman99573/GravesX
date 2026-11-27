package dev.cwhead.GravesX.listener.integration.craftengine;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.integration.CraftEngine;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

public class CraftEngineFurnitureListener implements Listener {
    private final Graves plugin;
    private final CraftEngine craftEngine;

    public CraftEngineFurnitureListener(Graves plugin, CraftEngine craftEngine) {
        this.plugin = plugin;
        this.craftEngine = craftEngine;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Grave grave = getFurnitureGrave(event.getEntity());

        if (grave != null && grave.getProtection()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveEvent event) {
        Grave grave = getFurnitureGrave(event.getEntity());

        if (grave != null) {
            plugin.debugMessage("CraftEngine furniture removed for grave " + grave.getUUID(), 1);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Grave grave = getFurnitureGrave(event.getEntity());

        if (grave != null && grave.getProtection()) {
            event.setCancelled(true);
        }
    }

    private Grave getFurnitureGrave(Entity entity) {
        if (entity == null || !craftEngine.isFurnitureEntity(entity)) {
            return null;
        }

        return craftEngine.getGrave(entity);
    }
}