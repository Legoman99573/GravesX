package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveCloseEvent;
import com.ranull.graves.type.Grave;
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
        if (event.getInventory().getHolder() instanceof Grave && event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            Grave grave = (Grave) event.getInventory().getHolder();
            GraveCloseEvent graveCloseEvent = new GraveCloseEvent(event.getView(), grave);

            // Call the custom GraveCloseEvent
            plugin.getServer().getPluginManager().callEvent(graveCloseEvent);

            if (grave.getItemAmount() <= 0) {
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

            // Play a sound effect related to closing the inventory
            plugin.getEntityManager().playWorldSound("sound.close", player, grave);
        }
    }
}