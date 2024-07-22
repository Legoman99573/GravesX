package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Listener for handling PlayerRespawnEvent to manage grave-related functionality upon player respawn.
 */
public class PlayerRespawnListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a PlayerRespawnListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerRespawnListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerRespawnEvent to perform actions related to graves when a player respawns.
     *
     * This method:
     * - Runs a scheduled task to execute a function configured for respawn events.
     * - Checks if a compass should be given to the player based on the respawn time and config settings.
     *
     * @param event The PlayerRespawnEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        List<String> permissionList = plugin.getPermissionList(player);
        List<Grave> graveList = plugin.getGraveManager().getGraveList(player);

        if (!graveList.isEmpty()) {
            Grave grave = graveList.get(graveList.size() - 1);

            // Schedule a function to run after player respawn
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getEntityManager().runFunction(player, plugin
                        .getConfig("respawn.function", player, permissionList)
                        .getString("respawn.function", "none"), grave);
            }, 1L);

            // Check if a compass should be given to the player
            if (plugin.getVersionManager().hasCompassMeta()
                    && plugin.getConfig("respawn.compass", player, permissionList)
                    .getBoolean("respawn.compass")
                    && grave.getLivedTime() <= plugin.getConfig("respawn.compass-time", player, permissionList)
                    .getInt("respawn.compass-time") * 1000L) {
                List<Location> locationList = plugin.getGraveManager()
                        .getGraveLocationList(event.getRespawnLocation(), grave);

                if (!locationList.isEmpty()) {
                    ItemStack itemStack = plugin.getEntityManager().createGraveCompass(player, locationList.get(0), grave);

                    if (itemStack != null) {
                        player.getInventory().addItem(itemStack);
                    }
                }
            }
        }
    }
}