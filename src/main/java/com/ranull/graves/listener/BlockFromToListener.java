package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveAutoLootEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

import java.util.Objects;

/**
 * Listens for BlockFromToEvent to prevent water or lava from flowing over grave blocks.
 */
public class BlockFromToListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new BlockFromToListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public BlockFromToListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles BlockFromToEvent to prevent fluid from flowing into grave blocks.
     *
     * @param event The BlockFromToEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        // Check if the destination block of the fluid is a grave
        if (plugin.getBlockManager().getGraveFromBlock(event.getToBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void autoLootTest(GraveAutoLootEvent e) {
        if (e instanceof Player) {
            Player player = ((Player) e).getPlayer();
            assert player != null;
            if (!Objects.equals(e.getGrave().getOwnerName(), player.getName())) {
                e.setCancelled(true);
            }
        }
    }
}