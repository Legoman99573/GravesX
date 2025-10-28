package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilityParticleEnum;
import dev.cwhead.GravesX.compatibility.CompatibilitySoundEnum;
import dev.cwhead.GravesX.event.GraveAutoLootEvent;
import dev.cwhead.GravesX.event.GraveBreakEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Objects;

/**
 * Listens for BlockBreakEvent to handle interactions with grave blocks and
 * prevent block breaking within a specified radius of a grave.
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
     * Handles BlockBreakEvent to manage interactions with grave blocks when a block is broken.
     *
     * Checks if the broken block is associated with a grave. If so, verifies if breaking the grave
     * is allowed and if the player has permission to do so. If not a grave block, it checks if
     * the block is within a 15-block radius of any grave and cancels the event if so.
     *
     * @param event The BlockBreakEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

        if (grave != null) {
            if (isGraveBreakAllowed(grave)) {
                if (plugin.getEntityManager().canOpenGrave(player, grave)) {
                    handleGraveBreak(event, player, block, grave);
                } else {
                    plugin.getEntityManager().sendMessage("message.protection", player, player.getLocation(), grave);
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        } else if (plugin.getGraveManager().isNearGrave(block.getLocation(), player)) {
            event.setCancelled(true);
            plugin.getEntityManager().sendMessage("message.grave-protection-break-deny", player);
        }
    }

    /**
     * Checks if breaking the grave is allowed based on the configuration.
     *
     * @param grave The grave to check.
     * @return True if breaking the grave is allowed, false otherwise.
     */
    private boolean isGraveBreakAllowed(Grave grave) {
        return plugin.getConfig("grave.break", grave).getBoolean("grave.break");
    }

    /**
     * Handles the process of breaking a grave.
     *
     * @param event   The BlockBreakEvent.
     * @param player  The player breaking the block.
     * @param block   The block being broken.
     * @param grave   The grave associated with the block.
     */
    private void handleGraveBreak(BlockBreakEvent event, Player player, Block block, Grave grave) {
        boolean cfgDrop = plugin.getConfig("drop.break", grave).getBoolean("drop.break");

        GraveBreakEvent modern = new GraveBreakEvent(block, player, grave);
        modern.setDropItems(false);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveBreakEvent legacy = new com.ranull.graves.event.GraveBreakEvent(block, player, grave);
        legacy.setDropItems(cfgDrop);
        plugin.getServer().getPluginManager().callEvent(legacy);

        boolean cancelled = modern.isCancelled() || legacy.isCancelled();
        boolean addon = modern.isAddon() || legacy.isAddon();

        event.setDropItems(false);

        if (!cancelled && !addon) {
            boolean dropItemsFinal = modern.isDropItems() && legacy.isDropItems();
            modern.setDropItems(dropItemsFinal);

            if (plugin.getConfig("drop.auto-loot.enabled", grave).getBoolean("drop.auto-loot.enabled")) {
                handleAutoLoot(event, player, block, grave, modern);
            } else if (dropItemsFinal) {
                explodeEffectGrave(grave);
                plugin.getGraveManager().breakGrave(block.getLocation(), grave);
            } else {
                explodeEffectGrave(grave);
                plugin.getGraveManager().removeGrave(grave);
            }

            if (grave.getExperience() > 0) {
                plugin.getGraveManager().dropGraveExperience(block.getLocation(), grave);
            }

            finalizeGraveBreak(player, block, grave);
        } else if (cancelled && !addon) {
            event.setCancelled(true);
        }
    }

    private void explodeEffectGrave(Grave grave) {
        try {
            Location loc = grave.getLocationDeath();

            Objects.requireNonNull(loc.getWorld()).spawnParticle(CompatibilityParticleEnum.valueOf("EXPLOSION"), loc, 1);
            try {
                loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("ENTITY_GENERIC_EXPLODE")), 1.0f, 1.0f);
            } catch (Exception e) {
                loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("EXPLODE")), 1.0f, 1.0f); // pre 1.9
            }
        } catch (Exception ignored) {
            //ignored
        }
    }

    /**
     * Handles the auto-loot process when breaking a grave.
     *
     * @param event           The BlockBreakEvent.
     * @param player          The player breaking the block.
     * @param block           The block being broken.
     * @param grave           The grave associated with the block.
     * @param graveBreakEvent The GraveBreakEvent.
     */
    private void handleAutoLoot(BlockBreakEvent event, Player player, Block block, Grave grave, com.ranull.graves.event.GraveBreakEvent graveBreakEvent) {
        Location hitLoc = block.getLocation();

        GraveAutoLootEvent modern = new GraveAutoLootEvent(player, hitLoc, grave);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveAutoLootEvent legacy = new com.ranull.graves.event.GraveAutoLootEvent(player, hitLoc, grave);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon()) {
            return;
        }

        plugin.getGraveManager().autoLootGrave(player, hitLoc, grave);

        if (graveBreakEvent.isDropItems()
                && plugin.getConfig("drop.auto-loot.break", grave).getBoolean("drop.auto-loot.break")) {
            try {
                Location loc = grave.getLocationDeath();
                Objects.requireNonNull(loc.getWorld()).spawnParticle(CompatibilityParticleEnum.valueOf("EXPLOSION"), loc, 1);
                try {
                    loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("ENTITY_GENERIC_EXPLODE")), 1.0f, 1.0f);
                } catch (Exception e) {
                    loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("EXPLODE")), 1.0f, 1.0f); // pre 1.9
                }
            } catch (Exception ignored) {
                // ignored
            }

            plugin.getGraveManager().breakGrave(hitLoc, grave);

            if (plugin.getIntegrationManager().hasNoteBlockAPI()) {
                if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForPlayer(player)) {
                    plugin.getIntegrationManager().getNoteBlockAPI().stopSongForPlayer(player);
                }
                if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForAllPlayers()) {
                    plugin.getIntegrationManager().getNoteBlockAPI().stopSongForAllPlayers();
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    /**
     * Handles the auto-loot process when breaking a grave.
     *
     * @param event           The BlockBreakEvent.
     * @param player          The player breaking the block.
     * @param block           The block being broken.
     * @param grave           The grave associated with the block.
     * @param graveBreakEvent The GraveBreakEvent.
     */
    private void handleAutoLoot(BlockBreakEvent event, Player player, Block block, Grave grave, GraveBreakEvent graveBreakEvent) {
        Location hitLoc = block.getLocation();

        GraveAutoLootEvent modern = new GraveAutoLootEvent(player, hitLoc, grave);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveAutoLootEvent legacy = new com.ranull.graves.event.GraveAutoLootEvent(player, hitLoc, grave);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon()) {
            return;
        }

        plugin.getGraveManager().autoLootGrave(player, hitLoc, grave);

        if (graveBreakEvent.isDropItems()
                && plugin.getConfig("drop.auto-loot.break", grave).getBoolean("drop.auto-loot.break")) {
            try {
                Location loc = grave.getLocationDeath();
                Objects.requireNonNull(loc.getWorld()).spawnParticle(CompatibilityParticleEnum.valueOf("EXPLOSION"), loc, 1);
                try {
                    loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("ENTITY_GENERIC_EXPLODE")), 1.0f, 1.0f);
                } catch (Exception e) {
                    loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("EXPLODE")), 1.0f, 1.0f); // pre 1.9
                }
            } catch (Exception ignored) {
                // ignored
            }

            plugin.getGraveManager().breakGrave(hitLoc, grave);

            if (plugin.getIntegrationManager().hasNoteBlockAPI()) {
                if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForPlayer(player)) {
                    plugin.getIntegrationManager().getNoteBlockAPI().stopSongForPlayer(player);
                }
                if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForAllPlayers()) {
                    plugin.getIntegrationManager().getNoteBlockAPI().stopSongForAllPlayers();
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    /**
     * Finalizes the process of breaking a grave by closing the grave, playing effects, and running commands.
     *
     * @param player The player breaking the block.
     * @param block  The block being broken.
     * @param grave  The grave associated with the block.
     */
    private void finalizeGraveBreak(Player player, Block block, Grave grave) {
        plugin.getGraveManager().closeGrave(grave);
        plugin.getGraveManager().playEffect("effect.loot", block.getLocation(), grave);
        plugin.getEntityManager().spawnZombie(block.getLocation(), player, player, grave);
        plugin.getEntityManager().runCommands("event.command.break", player, block.getLocation(), grave);
    }
}