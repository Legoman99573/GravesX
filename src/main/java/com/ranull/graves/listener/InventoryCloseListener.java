package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveCloseEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

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
            Grave grave = (Grave) event.getInventory().getHolder();

            // Call the custom GraveCloseEvent
            callGraveCloseEvent(event, grave, player, entity);

            if (grave != null && isEmptyGrave(grave)) {
                handleEmptyGrave(player, grave);
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
        return event.getInventory().getHolder() instanceof Grave;
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
        GraveCloseEvent graveCloseEvent = new GraveCloseEvent(event.getView(), grave, player);
        GraveCloseEvent graveCloseEventLegacy = new GraveCloseEvent(grave, event.getView(), entity);
        plugin.getServer().getPluginManager().callEvent(graveCloseEvent);
        plugin.getServer().getPluginManager().callEvent(graveCloseEventLegacy);
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
    private void handleEmptyGrave(Player player, Grave grave) {
        // Remove the player from the grave's viewers
        grave.getInventory().getViewers().remove(player);

        // Execute commands and send messages related to the grave
        plugin.getEntityManager().runCommands("event.command.loot", player, player.getLocation(), grave);
        plugin.getEntityManager().sendMessage("message.loot", player, player.getLocation(), grave);

        // Spawn a zombie at the grave's death location
        plugin.getEntityManager().spawnZombie(grave.getLocationDeath(), player, player, grave);

        // Award experience and remove the grave
        plugin.getGraveManager().giveGraveExperience(player, grave);
        plugin.getGraveManager().removeGrave(grave);
    }
}