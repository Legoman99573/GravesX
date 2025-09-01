package dev.cwhead.GravesX.debug;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public final class LateEnableHook implements Listener {
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        KeepInventoryDetector.wrapPlugin(event.getPlugin());
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        KeepInventoryDetector.unhookPlugin(event.getPlugin());
    }
}