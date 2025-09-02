package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.compatibility.CompatibilityInventoryView;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
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
        if (isGraveInventory(event) && isPlayer(event.getPlayer())) {
            Player player = (Player) event.getPlayer();
            Entity entity = event.getPlayer();
            Inventory topInventory = CompatibilityInventoryView.getTopInventory(event);
            Grave grave = (Grave) topInventory.getHolder();

            // Call the custom GraveCloseEvent
            callGraveCloseEvent(event, grave, player, entity);

            if (grave != null && isEmptyGrave(grave)) {
                handleEmptyGrave(event, player, grave, entity);
            }

            // Play a sound effect related to closing the inventory
            plugin.getEntityManager().playWorldSound("sound.close", player, grave);
        }
    }

    /**
     * Checks if the event's inventory holder is a grave.
     *
     * @param event The InventoryCloseEvent.
     * @return True if the inventory holder is a grave, false otherwise.
     */
    private boolean isGraveInventory(InventoryCloseEvent event) {
        Inventory topInventory = CompatibilityInventoryView.getTopInventory(event);
        return topInventory.getHolder() instanceof Grave;
    }

    /**
     * Checks if the entity is a player.
     *
     * @param entity The entity to check.
     * @return True if the entity is a player, false otherwise.
     */
    private boolean isPlayer(Object entity) {
        return entity instanceof Player;
    }

    /**
     * Calls the custom GraveCloseEvent.
     *
     * @param event  The InventoryCloseEvent.
     * @param grave  The grave associated with the inventory.
     * @param player The player who closed the inventory.
     */
    private void callGraveCloseEvent(InventoryCloseEvent event, Grave grave, Player player, Entity entity) {
        dev.cwhead.GravesX.event.GraveCloseEvent modern =
                new dev.cwhead.GravesX.event.GraveCloseEvent(event.getView(), grave, player);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveCloseEvent legacy =
                new com.ranull.graves.event.GraveCloseEvent(event.getView(), grave, player);
        plugin.getServer().getPluginManager().callEvent(legacy);
    }


    /**
     * Calls the custom GraveCloseEvent.
     *
     * @param event  The InventoryCloseEvent.
     * @param grave  The grave associated with the inventory.
     * @param player The player who closed the inventory.
     */
    private void callGraveLootedEvent(InventoryCloseEvent event, Grave grave, Player player, Entity entity) {
        dev.cwhead.GravesX.event.GraveLootedEvent modern =
                new dev.cwhead.GravesX.event.GraveLootedEvent(event.getView(), grave, player);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveLootedEvent legacy =
                new com.ranull.graves.event.GraveLootedEvent(event.getView(), grave, player);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {
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

    /**
     * Checks if the grave is empty.
     *
     * @param grave The grave to check.
     * @return True if the grave is empty, false otherwise.
     */
    private boolean isEmptyGrave(Grave grave) {
        return grave.getItemAmount() <= 0;
    }

    /**
     * Handles actions for an empty grave.
     *
     * @param player The player who closed the inventory.
     * @param grave  The empty grave.
     */
    private void handleEmptyGrave(InventoryCloseEvent event, Player player, Grave grave, Entity entity) {
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
                    Objects.requireNonNull(location.getWorld()).spawnParticle(Particle.valueOf("EXPLOSION_HUGE"), location, 1);
                    try {
                        location.getWorld().playSound(location, Sound.valueOf("ENTITY_GENERIC_EXPLODE"), 1.0f, 1.0f);
                    } catch (Exception e) {
                        location.getWorld().playSound(location, Sound.valueOf("EXPLODE"), 1.0f, 1.0f); // pre 1.9
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