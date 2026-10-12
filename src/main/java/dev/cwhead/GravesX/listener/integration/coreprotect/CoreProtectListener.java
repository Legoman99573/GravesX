package dev.cwhead.GravesX.listener.integration.coreprotect;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.event.*;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Logs GravesX events to CoreProtect.
 *
 * @deprecated Unmaintained greedware plugin.
 */
@Deprecated
public class CoreProtectListener implements Listener {

    private static final String SYSTEM_ACTOR = "GravesX";

    private final Graves plugin;

    /**
     * Creates the CoreProtect listener.
     *
     * @param plugin GravesX plugin
     * @deprecated Unmaintained greedware plugin.
     */
    @Deprecated
    public CoreProtectListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the CoreProtect API.
     *
     * @return CoreProtect API, or null
     */
    private CoreProtectAPI getCoreProtect() {
        if (!plugin.getIntegrationManager().hasCoreProtect()) {
            return null;
        }

        return plugin.getIntegrationManager()
                .getCoreProtect()
                .getCoreProtectAPI();
    }

    /**
     * Gets the event location.
     *
     * @param event grave event
     * @return event location
     */
    private Location getEventLocation(GraveEvent event) {
        if (event.hasLocation()) {
            return event.getLocation();
        }

        return event.getGrave().getLocationDeath();
    }

    /**
     * Gets the event actor.
     *
     * @param event grave entity event
     * @return actor name
     */
    private String getActor(GraveEntityEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player player) {
            return player.getName();
        }

        if (event.hasLivingEntity()) {
            LivingEntity livingEntity = event.getLivingEntity();

            if (livingEntity instanceof Player player) {
                return player.getName();
            }

            return livingEntity.getName();
        }

        if (entity != null) {
            return entity.getName();
        }

        return SYSTEM_ACTOR;
    }

    /**
     * Logs a GravesX action marker.
     *
     * @param coreProtect CoreProtect API
     * @param action action name
     * @param location action location
     */
    private void logMarker(CoreProtectAPI coreProtect, String action, Location location) {
        if (coreProtect == null || location == null) {
            return;
        }

        coreProtect.logInteraction(SYSTEM_ACTOR + "-" + action, location);
    }

    /**
     * Logs an actor interaction and action.
     *
     * @param coreProtect CoreProtect API
     * @param action action name
     * @param actor actor name
     * @param location action location
     */
    private void logInteraction(CoreProtectAPI coreProtect, String action, String actor, Location location) {
        if (coreProtect == null || location == null) {
            return;
        }

        if (actor != null && !actor.isBlank()) {
            coreProtect.logInteraction(actor, location);
        }

        logMarker(coreProtect, action, location);
    }

    /**
     * Logs a block placement.
     *
     * @param coreProtect CoreProtect API
     * @param actor actor name
     * @param block placed block
     */
    private void logPlacement(CoreProtectAPI coreProtect, String actor, Block block) {
        if (coreProtect == null || block == null) {
            return;
        }

        coreProtect.logPlacement(actor != null ? actor : SYSTEM_ACTOR, block.getLocation(), block.getType(), block.getBlockData());
    }

    /**
     * Logs a block removal.
     *
     * @param coreProtect CoreProtect API
     * @param actor actor name
     * @param block removed block
     */
    private void logRemoval(CoreProtectAPI coreProtect, String actor, Block block) {
        if (coreProtect == null || block == null) {
            return;
        }

        coreProtect.logRemoval(actor != null ? actor : SYSTEM_ACTOR, block.getLocation(), block.getType(), block.getBlockData());
    }

    /**
     * Logs a block removal snapshot.
     *
     * @param coreProtect CoreProtect API
     * @param actor actor name
     * @param location block location
     * @param material block material
     * @param blockData block data
     */
    private void logRemoval(CoreProtectAPI coreProtect, String actor, Location location, Material material, BlockData blockData) {
        if (coreProtect == null || location == null || material == null)
            return;

        coreProtect.logRemoval(actor != null ? actor : SYSTEM_ACTOR, location, material, blockData);
    }

    /**
     * Checks whether a grave block was destroyed.
     *
     * @param coreProtect CoreProtect API
     * @param actor actor name
     * @param action action name
     * @param originalBlock original block
     */
    private void checkForDestroyedBlock(CoreProtectAPI coreProtect, String actor, String action, Block originalBlock) {
        if (coreProtect == null || originalBlock == null)
            return;

        final Location location = originalBlock.getLocation().clone();

        final Material originalMaterial = originalBlock.getType();

        final BlockData originalData = originalBlock.getBlockData().clone();

        if (originalMaterial.isAir())
            return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {

            Block currentBlock = location.getBlock();

            boolean changed = currentBlock.getType() != originalMaterial || !currentBlock.getBlockData().equals(originalData);

            if (!changed)
                return;

            logRemoval(coreProtect, actor, location, originalMaterial, originalData);

            logMarker(coreProtect, action, location);

            plugin.debugMessage("CoreProtect: grave block at " + location + " was destroyed by " + actor + ".", 1);
        });
    }

    /**
     * Logs grave block placement.
     *
     * @param event grave block place event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveBlockPlace(GraveBlockPlaceEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        if (!event.hasBlock())
            return;

        String actor = SYSTEM_ACTOR;

        if (event.hasPlayer()) {
            actor = event.getPlayer().getName();
        } else if (event.hasEntity()) {
            actor = event.getEntity().getName();
        }

        logPlacement(coreProtect, actor, event.getBlock());

        plugin.debugMessage("CoreProtect: logged grave block placement at " + event.getBlock().getLocation() + ".", 1);
    }

    /**
     * Logs completed grave creation.
     *
     * @param event grave post-create event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGravePostCreate(GravePostCreateEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null || !event.isPlaced())
            return;

        Location location = event.getPlacedLocation();

        logMarker(coreProtect, "CREATED", location);

        plugin.debugMessage("CoreProtect: grave creation completed at " + location + ".", 1);
    }

    /**
     * Logs grave opening.
     *
     * @param event grave open event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveOpen(GraveOpenEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        logInteraction(coreProtect, "OPENED", event.getPlayer().getName(), location);

        plugin.debugMessage("CoreProtect: " + event.getPlayer().getName() + " opened grave at " + location + ".", 1);
    }

    /**
     * Logs virtual grave opening.
     *
     * @param event grave virtual-open event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveVirtualOpen(GraveVirtualOpenEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        String actor = getActor(event);

        logInteraction(coreProtect, "VIRTUAL-OPENED", actor, location);

        plugin.debugMessage("CoreProtect: " + actor + " virtually opened grave at " + location + ".", 1);
    }

    /**
     * Logs grave closing.
     *
     * @param event grave close event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveClose(GraveCloseEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        logInteraction(coreProtect, "CLOSED", event.getPlayer().getName(), location);

        plugin.debugMessage("CoreProtect: " + event.getPlayer().getName() + " closed grave at " + location + ".", 1);
    }

    /**
     * Logs a grave item transaction.
     *
     * @param event grave item-take event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveItemTake(GraveItemTakeEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Player player = event.getPlayer();

        Location location = getEventLocation(event);

        coreProtect.logContainerTransaction(player.getName(), location);

        plugin.debugMessage("CoreProtect: started grave inventory transaction for " + player.getName() + " at " + location + ".", 1);
    }

    /**
     * Logs a fully looted grave.
     *
     * @param event grave looted event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveLooted(GraveLootedEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        String actor = event.getPlayer().getName();

        logInteraction(coreProtect, "LOOTED", actor, location);

        plugin.debugMessage("CoreProtect: " + actor + " completely looted grave at " + location + ".", 1);
    }

    /**
     * Logs automatic grave looting.
     *
     * @param event grave auto-loot event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveAutoLoot(GraveAutoLootEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        String actor = getActor(event);

        coreProtect.logContainerTransaction(actor, location);

        logInteraction(coreProtect, "AUTO-LOOT", actor, location);

        plugin.debugMessage("CoreProtect: " + actor + " auto-looted grave at " + location + ".", 1);
    }

    /**
     * Logs grave breaking.
     *
     * @param event grave break event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveBreak(GraveBreakEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        String actor = event.getPlayer().getName();

        Location location = getEventLocation(event);

        if (event.hasBlock()) {
            logRemoval(coreProtect, actor, event.getBlock());
        }

        logMarker(coreProtect, "BROKEN", location);

        plugin.debugMessage("CoreProtect: " + actor + " broke grave at " + location + ".", 1);
    }

    /**
     * Logs grave explosions.
     *
     * @param event grave explode event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveExplode(GraveExplodeEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        String actor = "Explosion";

        if (event.hasPlayer()) {
            actor = event.getPlayer().getName();
        } else if (event.hasEntity()) {
            actor = event.getEntity().getName();
        }

        logInteraction(coreProtect, "EXPLODED", actor, location);

        plugin.debugMessage("CoreProtect: grave exploded at " + location + " caused by " + actor + ".", 1);
    }

    /**
     * Logs projectile hits on graves.
     *
     * @param event grave projectile-hit event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveProjectileHit(GraveProjectileHitEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        String actor = getActor(event);

        logInteraction(coreProtect, "PROJECTILE-HIT", actor, location);

        if (event.hasBlock()) {
            checkForDestroyedBlock(coreProtect, actor, "PROJECTILE-DESTROYED", event.getBlock());
        }

        plugin.debugMessage("CoreProtect: " + actor + " hit grave at " + location + " with " + event.getEntity().getType() + ".", 1);
    }

    /**
     * Logs piston interaction with graves.
     *
     * @param event grave piston-extend event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGravePistonExtend(GravePistonExtendEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        String actor = "GravesX-Piston";

        logMarker(coreProtect, "PISTON", location);

        Block affectedBlock = null;

        for (Block movedBlock : event.getMovedBlocks()) {
            Location movedLocation = movedBlock.getLocation();

            if (sameBlockLocation(movedLocation, location)) {
                affectedBlock = movedBlock;
                break;
            }
        }

        if (affectedBlock == null && location.getWorld() != null) {

            Block candidate = location.getBlock();

            if (!candidate.getType().isAir()) {
                affectedBlock = candidate;
            }
        }

        if (affectedBlock != null) {
            checkForDestroyedBlock(coreProtect, actor, "PISTON-DESTROYED", affectedBlock);
        }

        plugin.debugMessage("CoreProtect: piston at " + event.getPistonBlock().getLocation() + " affected grave at " + location + " toward " + event.getDirection() + ".", 1);
    }

    /**
     * Checks whether two locations share a block.
     *
     * @param first first location
     * @param second second location
     * @return true if the block matches
     */
    private boolean sameBlockLocation(Location first, Location second) {
        if (first == null || second == null)
            return false;

        if (first.getWorld() == null || second.getWorld() == null)
            return false;

        return first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    /**
     * Logs grave expiration.
     *
     * @param event grave expired event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveExpired(GraveExpiredEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        logMarker(coreProtect, "EXPIRED", location);

        plugin.debugMessage("CoreProtect: grave expired at " + location + ".", 1);
    }

    /**
     * Logs abandoned graves.
     *
     * @param event grave abandoned event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveAbandoned(GraveAbandonedEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        logMarker(coreProtect, "ABANDONED", location);

        plugin.debugMessage("CoreProtect: grave abandoned at " + location + ".", 1);
    }

    /**
     * Logs grave protection creation.
     *
     * @param event protection create event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveProtectionCreate(GraveProtectionCreateEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        String actor = getActor(event);

        logInteraction(coreProtect, "PROTECTION-CREATED", actor, location);

        plugin.debugMessage("CoreProtect: grave protection created at " + location + ".", 1);
    }

    /**
     * Logs grave protection expiration.
     *
     * @param event protection expired event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraveProtectionExpired(GraveProtectionExpiredEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        Location location = getEventLocation(event);

        logMarker(coreProtect, "PROTECTION-EXPIRED", location);

        plugin.debugMessage("CoreProtect: grave protection expired at " + location + ".", 1);
    }

    /**
     * Logs completed grave teleports.
     *
     * @param event grave post-teleport event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGravePostTeleport(GravePostTeleportEvent event) {
        CoreProtectAPI coreProtect = getCoreProtect();

        if (coreProtect == null)
            return;

        String actor = getActor(event);

        Location graveLocation = event.getGrave().getLocationDeath();

        logInteraction(coreProtect, "TELEPORTED", actor, graveLocation);

        plugin.debugMessage("CoreProtect: " + actor + " successfully teleported from " + event.getFrom() + " to " + event.getTo() + " for grave at " + graveLocation + ".", 1);
    }
}