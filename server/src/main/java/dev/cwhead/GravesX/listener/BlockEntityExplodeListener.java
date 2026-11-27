package dev.cwhead.GravesX.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilitySoundEnum;
import dev.cwhead.GravesX.event.GraveExplodeEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Listens for both BlockExplodeEvent and EntityExplodeEvent to handle
 * interactions with grave blocks when they are affected by explosions.
 */
public class BlockEntityExplodeListener implements Listener {

    private final Graves plugin;

    /**
     * Last time any explosion for graves was processed.
     * Used to avoid double-processing when both events fire for the same explosion.
     */
    private long lastExplosionTime = 0L;

    /**
     * Constructs a new BlockEntityExplodeListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public BlockEntityExplodeListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles BlockExplodeEvent to manage grave interactions when blocks are exploded by other blocks.
     *
     * @param event The BlockExplodeEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (isWithinGlobalCooldown()) {
            plugin.debugMessage("[Explode Debug] BlockExplodeEvent skipped due to 20-tick global cooldown", 2);
            return;
        }

        plugin.debugMessage("[Explode Debug] BlockExplodeEvent fired, affected blocks: " + event.blockList().size(), 2);
        handleExplosion("BlockExplodeEvent", event.blockList(), null);
    }

    /**
     * Handles EntityExplodeEvent to manage grave interactions when blocks are exploded by entities.
     *
     * @param event The EntityExplodeEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isWithinGlobalCooldown()) {
            plugin.debugMessage("[Explode Debug] EntityExplodeEvent skipped due to 20-tick global cooldown", 2);
            return;
        }

        Entity entity = event.getEntity();
        plugin.debugMessage("[Explode Debug] EntityExplodeEvent fired by " + entity.getType()
                + ", affected blocks: " + event.blockList().size(), 2);
        handleExplosion("EntityExplodeEvent", event.blockList(), entity);
    }

    /**
     * Checks and updates a simple global cooldown for grave explosions.
     * If the previous explosion was less than a tick ago, returns true.
     *
     * @return true if we are within cooldown (should skip), false if we should process and update the timer.
     */
    private boolean isWithinGlobalCooldown() {
        long now = System.currentTimeMillis();
        if (now - lastExplosionTime < 20L * 50L) {
            return true;
        }
        lastExplosionTime = now;
        return false;
    }

    /**
     * Shared explosion handler for both block and entity explosions.
     *
     * @param source          String identifier of the event source (for debugging).
     * @param affectedBlocks  The list of blocks affected by the explosion.
     * @param explodingEntity The exploding entity, or null for block-based explosions.
     */
    private void handleExplosion(String source, List<Block> affectedBlocks, Entity explodingEntity) {
        Iterator<Block> iterator = affectedBlocks.iterator();

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

            if (grave == null) {
                continue;
            }

            Location graveHeadLocation = grave.getLocationDeath();

            // If the exploding block is exactly the grave head block, protect it.
            if (graveHeadLocation != null && graveHeadLocation.equals(block.getLocation()) && !shouldExplode(grave)) {
                plugin.debugMessage("[Explode Debug] " + source + ": protecting grave head block for grave "
                        + grave.getUUID(), 2);
                iterator.remove();
                continue;
            }

            plugin.debugMessage("[Explode Debug] " + source + ": processing explosion for grave "
                    + grave.getUUID() + " at " + block.getLocation(), 2);

            handleGraveExplosion(iterator, block, grave, block.getLocation(), explodingEntity);
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
     * @param iterator        The iterator for the blocks in the explosion.
     * @param block           The block that exploded.
     * @param grave           The grave associated with the block.
     * @param location        The location of the grave block.
     * @param explodingEntity The exploding entity, or null for block-based explosions.
     */
    private void handleGraveExplosion(Iterator<Block> iterator, Block block, Grave grave, Location location, Entity explodingEntity) {

        GraveExplodeEvent modern = new GraveExplodeEvent(location, explodingEntity, grave);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveExplodeEvent legacy =
                new com.ranull.graves.event.GraveExplodeEvent(location, explodingEntity, grave);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon()) {
            plugin.debugMessage("[Explode Debug] GraveExplodeEvent cancelled for grave " + grave.getUUID(), 2);
            iterator.remove();
            return;
        }

        Location effectiveLoc = location;
        try {
            if (modern.hasLocation()) {
                effectiveLoc = modern.getLocation();
            } else {
                legacy.getLocation();
                effectiveLoc = legacy.getLocation();
            }
        } catch (Throwable ignored) {
            // ignored
        }

        if (plugin.getConfigManager().getConfigSection("drop.looted-explosion-effect", grave).getBoolean("drop.looted-explosion-effect", false)) {
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

        String sourceName = (explodingEntity != null) ? explodingEntity.getType().name() : block.getType().name();

        plugin.getEntityManager().runCommands("event.command.explode", sourceName, effectiveLoc, grave);

        if (plugin.getConfigManager().getConfigSection("zombie.explode", grave).getBoolean("zombie.explode")) {
            plugin.getEntityManager().spawnZombie(effectiveLoc, grave);
        }
    }
}