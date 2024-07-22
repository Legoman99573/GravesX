package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener for handling various grave-related events and logging messages.
 */
public class GraveTestListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a GraveTestListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public GraveTestListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the GraveCreateEvent to log a message when a grave is created.
     *
     * @param event The GraveCreateEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveCreate(GraveCreateEvent event) {
        plugin.testMessage(plugin.getEntityManager().getEntityName(event.getEntity()) + " created a grave");
        //event.setCancelled(true);
    }

    /**
     * Handles the GraveOpenEvent to log a message when a grave is opened.
     *
     * @param event The GraveOpenEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveOpen(GraveOpenEvent event) {
        plugin.testMessage(event.getPlayer().getName() + " opened " + event.getGrave().getOwnerName() + "'s grave");
        //event.setCancelled(true);
    }

    /**
     * Handles the GraveCloseEvent to log a message when a grave is closed.
     *
     * @param event The GraveCloseEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveClose(GraveCloseEvent event) {
        plugin.testMessage(event.getPlayer().getName() + " closed " + event.getGrave().getOwnerName() + "'s grave");
    }

    /**
     * Handles the GraveBreakEvent to log a message when a grave is broken.
     *
     * @param event The GraveBreakEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveBreak(GraveBreakEvent event) {
        plugin.testMessage(event.getPlayer().getName() + " broke " + event.getGrave().getOwnerName() + "'s grave");
        //event.setExpToDrop(0);
        //event.setDropItems(false);
        //event.setCancelled(true);
    }

    /**
     * Handles the GraveExplodeEvent to log a message when a grave explodes.
     *
     * @param event The GraveExplodeEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveExplode(GraveExplodeEvent event) {
        plugin.testMessage(event.getGrave().getOwnerName() + "'s grave exploded");
        //event.setCancelled(true);
    }

    /**
     * Handles the GraveTimeoutEvent to log a message when a grave times out.
     *
     * @param event The GraveTimeoutEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveTimeout(GraveTimeoutEvent event) {
        plugin.testMessage(event.getGrave().getOwnerName() + "'s grave timed out");
        event.setLocation(event.getLocation());
        //event.setCancelled(true);
    }
}