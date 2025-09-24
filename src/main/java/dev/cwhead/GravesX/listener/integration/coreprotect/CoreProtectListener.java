package dev.cwhead.GravesX.listener.integration.coreprotect;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.event.*;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * @deprecated Unmaintained greedware plugin.
 * Listens for events related to graves and logs interactions with CoreProtect.
 * This listener integrates with CoreProtect to log removal and placement of blocks,
 * as well as container transactions when graves are interacted with or affected by events.
 *
 */
@Deprecated
public class CoreProtectListener implements Listener {
    private final Graves plugin;

    /**
     * @deprecated Unmaintained greedware plugin.
     * Constructs a new {@code CoreProtectListener}.
     *
     * @param plugin the {@link Graves} plugin instance
     */
    @Deprecated
    public CoreProtectListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * @deprecated Unmaintained greedware plugin.
     * Logs removal data to CoreProtect when a grave is broken.
     *
     * @param e the {@link GraveBreakEvent} event
     */
    @Deprecated
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveBreak(GraveBreakEvent e) {
        if (e.isCancelled()) return;
        if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable

        final CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
        try {
            e.getBlock();
            coreProtect.logRemoval(
                    e.getPlayer().getName(),
                    e.getGrave().getLocationDeath(),
                    e.getBlock().getType(),
                    e.getBlock().getBlockData()
            );
        } catch (NullPointerException ignored) {
            coreProtect.logContainerTransaction(
                    e.getPlayer().getName(),
                    e.getGrave().getLocationDeath()
            );
        }

        plugin.debugMessage("Logged CoreProtect removal data for " + e.getGrave().getLocationDeath() + ".", 5);
    }

    /**
     * @deprecated Unmaintained greedware plugin.
     * Logs interaction and removal data to CoreProtect when a grave is auto-looted.
     *
     * @param e the {@link GraveAutoLootEvent} event
     */
    @Deprecated
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveAutoLoot(GraveAutoLootEvent e) {
        if (e.isCancelled()) return;
        if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable

        final CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
        coreProtect.logContainerTransaction(
                e.getPlayer() != null ? e.getPlayer().getName() : null,
                e.getGrave().getLocationDeath()
        );
        try {
            e.getBlock();
            coreProtect.logRemoval(
                    e.getPlayer() != null ? e.getPlayer().getName() : null,
                    e.getGrave().getLocationDeath(),
                    e.getBlock().getType(),
                    e.getBlock().getBlockData()
            );
        } catch (NullPointerException ignored) {
            coreProtect.logContainerTransaction(
                    e.getPlayer() != null ? e.getPlayer().getName() : null,
                    e.getGrave().getLocationDeath()
            );
        }

        plugin.debugMessage("Logged CoreProtect interaction and removal data for " + e.getGrave().getLocationDeath() + ".", 5);
    }

    /**
     * @deprecated Unmaintained greedware plugin.
     * Logs placement data to CoreProtect when a grave block is placed.
     *
     * @param e the {@link GraveBlockPlaceEvent} event
     */
    @Deprecated
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveBlockPlace(GraveBlockPlaceEvent e) {
        if (e.isCancelled()) return;
        if (!plugin.getIntegrationManager().hasCoreProtect()) return;

        final CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
        try {
            e.getBlock();
            coreProtect.logPlacement(
                    e.getPlayer() != null ? e.getPlayer().getName() : null,
                    e.getGrave().getLocationDeath(),
                    e.getBlock().getType(),
                    e.getBlock().getBlockData()
            );
        } catch (NullPointerException ignored) {
            coreProtect.logContainerTransaction(
                    e.getPlayer() != null ? e.getPlayer().getName() : null,
                    e.getGrave().getLocationDeath()
            );
        }

        plugin.debugMessage("Logged CoreProtect placement data for " + e.getGrave().getLocationDeath() + ".", 5);
    }

    /**
     * @deprecated Unmaintained greedware plugin.
     * Logs interaction data to CoreProtect when a grave is closed.
     *
     * @param e the {@link GraveCloseEvent} event
     */
    @Deprecated
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveClose(GraveCloseEvent e) {
        if (e.isCancelled()) return;
        if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable

        final CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
        coreProtect.logContainerTransaction(
                e.getPlayer().getName(),
                e.getGrave().getLocationDeath()
        );

        plugin.debugMessage("Logged CoreProtect interaction data for " + e.getGrave().getLocationDeath() + ".", 5);
    }

    /**
     * @deprecated Unmaintained greedware plugin.
     * Logs interaction data to CoreProtect when a grave is opened.
     *
     * @param e the {@link GraveCloseEvent} event
     */
    @Deprecated
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveOpen(GraveCloseEvent e) {
        if (e.isCancelled()) return;
        if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable

        final CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
        coreProtect.logContainerTransaction(
                e.getPlayer().getName(),
                e.getGrave().getLocationDeath()
        );

        plugin.debugMessage("Logged CoreProtect interaction data for " + e.getGrave().getLocationDeath() + ".", 5);
    }

    /**
     * @deprecated Unmaintained greedware plugin.
     * Logs removal data to CoreProtect when a grave explodes.
     *
     * @param e the {@link GraveExplodeEvent} event
     */
    @Deprecated
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveExplode(GraveExplodeEvent e) {
        if (e.isCancelled()) return;
        if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable

        final CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
        try {
            e.getBlock();
            e.getPlayer();
            coreProtect.logRemoval(
                    e.getPlayer().getName(),
                    e.getGrave().getLocationDeath(),
                    e.getBlock().getType(),
                    e.getBlock().getBlockData()
            );
        } catch (NullPointerException ignored) {
            e.getPlayer();
            coreProtect.logContainerTransaction(
                    e.getPlayer().getName(),
                    e.getGrave().getLocationDeath()
            );
        }

        plugin.debugMessage("Logged CoreProtect removal data for " + e.getGrave().getLocationDeath() + ".", 5);
    }
}