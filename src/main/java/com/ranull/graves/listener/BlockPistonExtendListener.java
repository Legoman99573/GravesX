package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilitySoundEnum;
import dev.cwhead.GravesX.event.GravePistonExtendEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

import java.util.List;
import java.util.Objects;

/**
 * Listens for BlockPistonExtendEvent to prevent pistons from moving blocks that are graves or are near holograms of graves.
 */
public class BlockPistonExtendListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new BlockPistonExtendListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public BlockPistonExtendListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles BlockPistonExtendEvent to prevent pistons from extending if they are moving a grave block or a block near a grave hologram unless configured otherwise.
     *
     * @param event The BlockPistonExtendEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        BlockFace direction = event.getDirection();
        Block piston = event.getBlock();
        List<Block> blocks = event.getBlocks();

        for (Block pushedBlock : blocks) {
            Block destination = pushedBlock.getRelative(direction);

            Grave grave = plugin.getBlockManager().getGraveFromBlock(destination);
            if (grave != null) {
                handleGravePistonMove(event, grave, piston, direction, blocks);
                return;
            }
        }
    }

    private void handleGravePistonMove(BlockPistonExtendEvent event, Grave grave, Block piston, BlockFace direction, List<Block> blocks) {
        boolean allowPush = plugin.getConfig("drop.piston-extend", grave).getBoolean("drop.piston-extend", true);
        plugin.debugMessage("allowPush value for grave at " + grave.getLocationDeath() + " is: " + allowPush, 2);

        if (!allowPush) {
            plugin.debugMessage("Push is forbidden for grave at " + grave.getLocationDeath() + " with piston at " + piston.getLocation(), 2);
            event.setCancelled(true);
            return;
        } else {
            plugin.debugMessage("Push is allowed for grave at " + grave.getLocationDeath() + " with piston at " + piston.getLocation(), 2);
        }

        GravePistonExtendEvent modern = new GravePistonExtendEvent(grave, piston.getLocation(), piston, direction, blocks);
        plugin.debugMessage("Created modern GravePistonExtendEvent for grave at " + grave.getLocationDeath() + " with piston at " + piston.getLocation(), 2);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GravePistonExtendEvent legacy = new com.ranull.graves.event.GravePistonExtendEvent(grave, piston.getLocation(), piston, direction, blocks);
        plugin.debugMessage("Created legacy GravePistonExtendEvent for grave at " + grave.getLocationDeath() + " with piston at " + piston.getLocation(), 2);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {
            plugin.debugMessage("Piston move allowed for grave at " + grave.getLocationDeath() + ". Breaking grave...", 2);

            try {
                Location loc = grave.getLocationDeath();
                Objects.requireNonNull(loc.getWorld()).spawnParticle(Particle.valueOf("EXPLOSION_HUGE"), loc, 1);
                try {
                    loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("ENTITY_GENERIC_EXPLODE")), 1.0f, 1.0f);
                } catch (Exception e) {
                    loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("EXPLODE")), 1.0f, 1.0f); // pre 1.9
                }
            } catch (Exception ignored) {
                // ignored
            }

            plugin.getGraveManager().breakGrave(grave.getLocationDeath(), grave);
            plugin.getGraveManager().closeGrave(grave);
            plugin.getGraveManager().playEffect("effect.loot", piston.getLocation(), grave);
            plugin.getEntityManager().runCommands("event.command.pistonextend", piston.getType().name(), piston.getLocation(), grave);
        } else {
            plugin.debugMessage("Piston move for grave at " + grave.getLocationDeath() + " was cancelled or is an addon. No action taken.", 2);
        }
    }
}