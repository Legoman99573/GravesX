package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.compatibility.CompatibilityInventoryView;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilitySoundEnum;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Objects;

/**
 * Listener for handling InventoryCloseEvent to manage actions when a grave inventory is closed.
 */
public class InventoryCloseListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs an InventoryCloseListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public InventoryCloseListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the InventoryCloseEvent to perform actions when a grave inventory is closed.
     * Calls a custom GraveCloseEvent and manages the state of the grave based on its item amount.
     *
     * @param event The InventoryCloseEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory topInventory = CompatibilityInventoryView.getTopInventory(event);
        if (!(topInventory.getHolder() instanceof Grave grave)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        try {
            plugin.getCacheManager().stopViewingGrave(grave.getUUID(), player.getUniqueId());
        } catch (Throwable ignored) {
            // If cache manager isn't available for some reason, don't break close handling.
        }

        // Fire close events (modern + legacy)
        callGraveCloseEvent(event, grave, player);

        // If empty, handle as "looted"
        if (isEmptyGrave(grave)) {
            handleEmptyGrave(event, player, grave);
        }

        // Play a sound related to closing the inventory
        plugin.getEntityManager().playWorldSound("sound.close", player, grave);
    }

    /**
     * Fires the custom GraveCloseEvent (modern + legacy).
     */
    private void callGraveCloseEvent(InventoryCloseEvent event, Grave grave, Player player) {
        dev.cwhead.GravesX.event.GraveCloseEvent modern =
                new dev.cwhead.GravesX.event.GraveCloseEvent(event.getView(), grave, player);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveCloseEvent legacy =
                new com.ranull.graves.event.GraveCloseEvent(event.getView(), grave, player);
        plugin.getServer().getPluginManager().callEvent(legacy);
    }

    /**
     * @return True if the grave has no items remaining.
     */
    private boolean isEmptyGrave(Grave grave) {
        return grave.getItemAmount() <= 0;
    }

    /**
     * Handles actions for an empty (fully looted) grave.
     */
    private void handleEmptyGrave(InventoryCloseEvent event, Player player, Grave grave) {
        dev.cwhead.GravesX.event.GraveLootedEvent modern =
                new dev.cwhead.GravesX.event.GraveLootedEvent(event.getView(), grave, player);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveLootedEvent legacy =
                new com.ranull.graves.event.GraveLootedEvent(event.getView(), grave, player);
        plugin.getServer().getPluginManager().callEvent(legacy);

        grave.getInventory().getViewers().remove(player);

        if (!(modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon())) {
            plugin.getEntityManager().runCommands("event.command.loot", player, player.getLocation(), grave);
            plugin.getEntityManager().sendMessage("message.looted", player, player.getLocation(), grave);

            plugin.getEntityManager().spawnZombie(grave.getLocationDeath(), player, player, grave);

            if (plugin.getConfig("drop.looted-explosion-effect", grave).getBoolean("drop.looted-explosion-effect", false)) {
                try {
                    Location location = grave.getLocationDeath();
                    Objects.requireNonNull(location.getWorld()).spawnParticle(plugin.getVersionManager().getParticleForVersion("EXPLOSION"), location, 1);
                    try {
                        location.getWorld().playSound(location, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("ENTITY_GENERIC_EXPLODE")), 1.0f, 1.0f);
                    } catch (Exception e) {
                        location.getWorld().playSound(location, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("EXPLODE")), 1.0f, 1.0f); // pre 1.9
                    }
                } catch (Exception ignored) {
                    // ignored
                }
            }

            plugin.getGraveManager().giveGraveExperience(player, grave);
            plugin.getGraveManager().removeGrave(grave);

            if (plugin.getIntegrationManager().hasNoteBlockAPI()) {
                if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForPlayer(player)) {
                    plugin.getIntegrationManager().getNoteBlockAPI().stopSongForPlayer(player);
                }
                if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForAllPlayers()) {
                    plugin.getIntegrationManager().getNoteBlockAPI().stopSongForAllPlayers();
                }
            }
        }
    }
}