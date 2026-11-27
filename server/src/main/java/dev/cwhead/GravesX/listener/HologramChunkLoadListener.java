package dev.cwhead.GravesX.listener;

import com.ranull.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Arrays;

/**
 * Removes orphaned grave holograms when a chunk loads on servers that have no
 * EntitiesLoadEvent (before 1.17, where chunk entities are available on chunk load).
 */
public class HologramChunkLoadListener implements Listener {
    private final Graves plugin;

    public HologramChunkLoadListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        try {
            plugin.getHologramManager().purgeLoadedEntities(Arrays.asList(event.getChunk().getEntities()));
        } catch (Throwable throwable) {
            plugin.debugMessage("[Cleanup] Chunk load hologram sweep failed: " + throwable, 2);
        }
    }
}
