package dev.cwhead.GravesX.listener.integration.craftengine;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.integration.CraftEngine;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class CraftEngineBlockListener implements Listener {
    private final Graves plugin;
    private final CraftEngine craftEngine;

    public CraftEngineBlockListener(Graves plugin, CraftEngine craftEngine) {
        this.plugin = plugin;
        this.craftEngine = craftEngine;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!craftEngine.isCustomBlock(block)) {
            return;
        }

        Grave grave = getGrave(block);

        if (grave == null) {
            return;
        }

        event.setDropItems(false);

        Player player = event.getPlayer();

        if (!plugin.getConfigManager().getConfigSection("grave.break", grave).getBoolean("grave.break", true)) {
            event.setCancelled(true);
            return;
        }

        if (!plugin.getEntityManager().canOpenGrave(player, grave)) {
            plugin.getEntityManager().sendMessage("message.protection", player, player.getLocation(), grave);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isMainHandInteraction(event)
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        Block block = event.getClickedBlock();

        if (!craftEngine.isCustomBlock(block)) {
            return;
        }

        Grave grave = getGrave(block);

        if (grave == null) {
            return;
        }

        event.setCancelled(true);
        plugin.getSchedulerManager().runTaskLater(() ->
                plugin.getGraveManager().openGrave(event.getPlayer(), block.getLocation(), grave), 1L);
    }

    private boolean isMainHandInteraction(PlayerInteractEvent event) {
        return !plugin.getVersionManager().hasSecondHand()
                || event.getHand() == EquipmentSlot.HAND;
    }

    private Grave getGrave(Block block) {
        if (block == null) {
            return null;
        }

        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);
        return grave != null ? grave : plugin.getCacheManager().getGrave(block);
    }
}