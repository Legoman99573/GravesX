package dev.cwhead.GravesX.listener;

import com.ranull.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

/**
 * Removes orphaned grave holograms as soon as their entities load (1.17+).
 * Kept separate from the chunk listener so older servers never link EntitiesLoadEvent.
 */
public class HologramEntitiesLoadListener implements Listener {
    private final Graves plugin;

    public HologramEntitiesLoadListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        try {
            plugin.getHologramManager().purgeLoadedEntities(event.getEntities());
        } catch (Throwable throwable) {
            plugin.debugMessage("[Cleanup] Entity load hologram sweep failed: " + throwable, 2);
        }
    }
}
