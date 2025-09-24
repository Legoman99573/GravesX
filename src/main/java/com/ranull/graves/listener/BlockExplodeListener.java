package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilitySoundEnum;
import dev.cwhead.GravesX.event.GraveExplodeEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Listens for BlockExplodeEvent to handle interactions with grave blocks when they are affected by block explosions.
 */
public class BlockExplodeListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new BlockExplodeListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public BlockExplodeListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles BlockExplodeEvent to manage grave interactions when blocks are exploded by other blocks.
     *
     * @param event The BlockExplodeEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        List<Block> affectedBlocks = event.blockList();
        Iterator<Block> iterator = affectedBlocks.iterator();

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

            if (grave != null) {
                boolean shouldProtectRadius = plugin.getConfig("grave.should-protect-radius", grave).getBoolean("grave.should-protect-radius");
                boolean explodeProtection = plugin.getConfig("grave.explode-protection", grave).getBoolean("grave.explode-protection");
                int protectionRadius = plugin.getConfig("grave.protection-radius", grave).getInt("grave.protection-radius");
                Location graveHeadLocation = grave.getLocationDeath();

                if (graveHeadLocation.equals(block.getLocation())) {
                    iterator.remove();
                    continue;
                }

                if (shouldProtectRadius && explodeProtection) {
                    Iterator<Block> protectIterator = affectedBlocks.iterator();
                    while (protectIterator.hasNext()) {
                        Block affectedBlock = protectIterator.next();
                        Location affectedLocation = affectedBlock.getLocation();

                        if (isWithinCube(graveHeadLocation, affectedLocation, protectionRadius)) {
                            protectIterator.remove();
                        }
                    }
                    event.setCancelled(true);
                    return;
                }

                if (shouldExplode(grave)) {
                    handleGraveExplosion(event, iterator, block, grave, block.getLocation());
                }
            }
        }
    }

    /**
     * Checks if a given location is within a cubic protection radius.
     *
     * @param center The center of the cube (grave location).
     * @param target The target location to check.
     * @param radius The protection radius.
     * @return True if the target is inside the cube, false otherwise.
     */
    private boolean isWithinCube(Location center, Location target, int radius) {
        return Math.abs(target.getBlockX() - center.getBlockX()) <= radius
                && Math.abs(target.getBlockY() - center.getBlockY()) <= radius
                && Math.abs(target.getBlockZ() - center.getBlockZ()) <= radius;
    }

    /**
     * Checks if the grave should explode based on the configuration.
     *
     * @param grave The grave to check.
     * @return True if the grave should explode, false otherwise.
     */
    private boolean shouldExplode(Grave grave) {
        return plugin.getConfig("grave.explode", grave).getBoolean("grave.explode");
    }

    /**
     * Handles the explosion of a grave.
     *
     * @param event     The BlockExplodeEvent.
     * @param iterator  The iterator for the blocks in the explosion.
     * @param block     The block that exploded.
     * @param grave     The grave associated with the block.
     * @param location  The location of the grave.
     */
    private void handleGraveExplosion(BlockExplodeEvent event, Iterator<Block> iterator, Block block, Grave grave, Location location) {
        GraveExplodeEvent modern = new GraveExplodeEvent(location, null, grave);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveExplodeEvent legacy =
                new com.ranull.graves.event.GraveExplodeEvent(location, null, grave);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon()) {
            iterator.remove();
            return;
        }

        Location effectiveLoc = location;
        try {
            if (modern.hasLocation()) {
                effectiveLoc = modern.getLocation();
            } else {
                effectiveLoc = legacy.getLocation();
            }
        } catch (Throwable ignored) {
        }

        try {
            Location deathLoc = grave.getLocationDeath();
            Objects.requireNonNull(deathLoc.getWorld())
                    .spawnParticle(plugin.getVersionManager().getParticleForVersion("EXPLOSION"), deathLoc, 1);
            try {
                deathLoc.getWorld().playSound(deathLoc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("ENTITY_GENERIC_EXPLODE")), 1.0f, 1.0f);
            } catch (Exception e) {
                deathLoc.getWorld().playSound(deathLoc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("EXPLODE")), 1.0f, 1.0f);
            }
        } catch (Exception ignored) {
        }

        if (plugin.getConfig("drop.explode", grave).getBoolean("drop.explode")) {
            plugin.getGraveManager().breakGrave(effectiveLoc, grave);
        } else {
            plugin.getGraveManager().removeGrave(grave);
        }

        plugin.getGraveManager().closeGrave(grave);
        plugin.getGraveManager().playEffect("effect.loot", effectiveLoc, grave);
        plugin.getEntityManager().runCommands("event.command.explode", block.getType().name(), effectiveLoc, grave);

        if (plugin.getConfig("zombie.explode", grave).getBoolean("zombie.explode")) {
            plugin.getEntityManager().spawnZombie(effectiveLoc, grave);
        }
    }
}