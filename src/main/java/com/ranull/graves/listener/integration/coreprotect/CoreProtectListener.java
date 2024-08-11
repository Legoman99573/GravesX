package com.ranull.graves.listener.integration.coreprotect;

import com.ranull.graves.Graves;
import com.ranull.graves.event.*;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class CoreProtectListener implements Listener {
    private final Graves plugin;

    public CoreProtectListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveBreak(GraveBreakEvent e) {
        if (!e.isCancelled()) {
            CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
            if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable

            try {
                coreProtect.logRemoval(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath(), e.getBlock() != null ? e.getBlock().getType() : null, e.getBlock().getBlockData());
            } catch (NullPointerException ignored) {
                coreProtect.logContainerTransaction(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath());
            }

            plugin.debugMessage("Logged CoreProtect removal data for " + e.getGrave().getLocationDeath() + ".", 5);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveAutoLoot(GraveAutoLootEvent e) {
        if (!e.isCancelled()) {
            CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
            if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable

            coreProtect.logContainerTransaction(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath());
            try {
                coreProtect.logRemoval(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath(), e.getBlock() != null ? e.getBlock().getType() : null, e.getBlock().getBlockData());
            } catch (NullPointerException ignored) {
                coreProtect.logContainerTransaction(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath());
            }

            plugin.debugMessage("Logged CoreProtect interaction and removal data for " + e.getGrave().getLocationDeath() + ".", 5);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveBlockPlace(GraveBlockPlaceEvent e) {
        if (!e.isCancelled()) {
            CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
            if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable

            try {
                coreProtect.logPlacement(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath(), e.getBlock() != null ? e.getBlock().getType() : null, e.getBlock().getBlockData());
            } catch (NullPointerException ignored) {
                coreProtect.logContainerTransaction(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath());
            }

            plugin.debugMessage("Logged CoreProtect placement data for " + e.getGrave().getLocationDeath() + ".", 5);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveClose(GraveCloseEvent e) {
        if (!e.isCancelled()) {
            CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
            if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable

            coreProtect.logContainerTransaction(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath());

            plugin.debugMessage("Logged CoreProtect interaction data for " + e.getGrave().getLocationDeath() + ".", 5);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveOpen(GraveCloseEvent e) {
        if (!e.isCancelled()) {
            CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
            if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable

            coreProtect.logContainerTransaction(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath());

            plugin.debugMessage("Logged CoreProtect interaction data for " + e.getGrave().getLocationDeath() + ".", 5);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveExplode(GraveExplodeEvent e) {
        if (!e.isCancelled()) {
            CoreProtectAPI coreProtect = plugin.getIntegrationManager().getCoreProtect().getCoreProtectAPI();
            if (!plugin.getIntegrationManager().hasCoreProtect()) return; // incase CoreProtect API is unavailable
            try {
                coreProtect.logRemoval(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath(), e.getBlock() != null ? e.getBlock().getType() : null, e.getBlock().getBlockData());
            } catch (NullPointerException ignored) {
                coreProtect.logContainerTransaction(e.getPlayer() != null ? e.getPlayer().getName() : null, e.getGrave().getLocationDeath());
            }

            plugin.debugMessage("Logged CoreProtect removal data for " + e.getGrave().getLocationDeath() + ".", 5);
        }
    }
}