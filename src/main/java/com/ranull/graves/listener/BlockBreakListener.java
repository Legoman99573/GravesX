package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveAutoLootEvent;
import com.ranull.graves.event.GraveBreakEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Listens for BlockBreakEvent to handle interactions with grave blocks.
 */
public class BlockBreakListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new BlockBreakListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public BlockBreakListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles BlockBreakEvent to manage grave interactions when a block is broken.
     *
     * @param event The BlockBreakEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

        if (grave != null) {
            // Check if breaking the grave is allowed
            if (plugin.getConfig("grave.break", grave).getBoolean("grave.break")) {
                if (plugin.getEntityManager().canOpenGrave(player, grave)) {
                    GraveBreakEvent graveBreakEvent = new GraveBreakEvent(block, player, grave);

                    // Set whether items should be dropped
                    graveBreakEvent.setDropItems(plugin.getConfig("drop.break", grave).getBoolean("drop.break"));
                    plugin.getServer().getPluginManager().callEvent(graveBreakEvent);

                    if (!graveBreakEvent.isCancelled()) {
                        if (plugin.getConfig("drop.auto-loot.enabled", grave).getBoolean("drop.auto-loot.enabled")) {
                            player.sendMessage("here");
                            GraveAutoLootEvent graveAutoLootEvent = new GraveAutoLootEvent(player, block.getLocation(), grave);
                            if (!graveAutoLootEvent.isCancelled()) {
                                plugin.getGraveManager().autoLootGrave(player, block.getLocation(), grave);

                                if (graveBreakEvent.isDropItems() && plugin.getConfig("drop.auto-loot.break", grave)
                                        .getBoolean("drop.auto-loot.break")) {
                                    plugin.getGraveManager().breakGrave(block.getLocation(), grave);
                                } else {
                                    event.setCancelled(true);
                                    return;
                                }
                            }
                        } else if (graveBreakEvent.isDropItems()) {
                            plugin.getGraveManager().breakGrave(block.getLocation(), grave);
                        } else {
                            plugin.getGraveManager().removeGrave(grave);
                        }

                        if (graveBreakEvent.getBlockExp() > 0) {
                            plugin.getGraveManager().dropGraveExperience(block.getLocation(), grave);
                        }

                        plugin.getGraveManager().closeGrave(grave);
                        plugin.getGraveManager().playEffect("effect.loot", block.getLocation(), grave);
                        plugin.getEntityManager().spawnZombie(block.getLocation(), player, player, grave);
                        plugin.getEntityManager().runCommands("event.command.break", player, block.getLocation(), grave);
                    } else {
                        event.setCancelled(true);
                    }
                } else {
                    plugin.getEntityManager().sendMessage("message.protection", player, player.getLocation(), grave);
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }
}