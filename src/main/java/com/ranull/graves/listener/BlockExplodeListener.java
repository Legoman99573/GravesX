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
                Location graveHeadLocation = grave.getLocationDeath();

                if (graveHeadLocation != null && graveHeadLocation.equals(block.getLocation())) {
                    iterator.remove();
                    continue;
                }

                if (shouldExplode(grave)) {
                    handleGraveExplosion(event, iterator, block, grave, block.getLocation());
                }
            }
        }
    }

    /**
     * Checks if the grave should explode based on the configuration.
     *
     * @param grave The grave to check.
     * @return True if the grave should explode, false otherwise.
     */
    private boolean shouldExplode(Grave grave) {
        return plugin.getConfigManager().getConfigSection("grave.explode", grave).getBoolean("grave.explode");
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
    private void handleGraveExplosion(BlockExplodeEvent event,
                                      Iterator<Block> iterator,
                                      Block block,
                                      Grave grave,
                                      Location location) {
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
            if (modern.hasLocation() && modern.getLocation() != null) {
                effectiveLoc = modern.getLocation();
            } else if (legacy.getLocation() != null) {
                effectiveLoc = legacy.getLocation();
            }
        } catch (Throwable ignored) {
            // ignored
        }

        if (plugin.getConfigManager()
                .getConfigSection("drop.looted-explosion-effect", grave)
                .getBoolean("drop.looted-explosion-effect", false)) {
            try {
                Location deathLoc = grave.getLocationDeath();
                Objects.requireNonNull(deathLoc.getWorld()).spawnParticle(plugin.getVersionManager().getParticleForVersion("EXPLOSION"), deathLoc, 1);
                try {
                    deathLoc.getWorld().playSound(deathLoc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("ENTITY_GENERIC_EXPLODE")), 1.0f, 1.0f);
                } catch (Exception e) {
                    deathLoc.getWorld().playSound(deathLoc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("EXPLODE")), 1.0f, 1.0f);
                }
            } catch (Exception ignored) {
                // ignored
            }
        }

        if (plugin.getConfigManager().getConfigSection("drop.explode", grave).getBoolean("drop.explode")) {
            plugin.getGraveManager().breakGrave(effectiveLoc, grave);
        } else {
            plugin.getGraveManager().removeGrave(grave);
        }

        plugin.getGraveManager().closeGrave(grave);
        plugin.getGraveManager().playEffect("effect.loot", effectiveLoc, grave);
        plugin.getEntityManager().runCommands("event.command.explode", block.getType().name(), effectiveLoc, grave);

        if (plugin.getConfigManager().getConfigSection("zombie.explode", grave).getBoolean("zombie.explode")) {
            plugin.getEntityManager().spawnZombie(effectiveLoc, grave);
        }
    }
}
