package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Listens for PlayerTeleportEvent to handle interactions with grave blocks and
 * remove specific items from the player's inventory if they teleport into a grave location.
 */
public class PlayerTeleportListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new PlayerTeleportListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerTeleportListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles PlayerTeleportEvent to manage interactions with grave blocks when a player teleports.
     *
     * Checks if the player's new location is within a 15-block radius of any grave and removes
     * specific compass items from their inventory if so.
     *
     * @param event The PlayerTeleportEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location newLocation = event.getTo();

        // Check if the teleport destination is a grave location
        if (isNearGrave(newLocation, player)) {
            removeSpecificCompassNearGrave(player, newLocation);
        }
    }

    /**
     * Checks if the given location is within 15 blocks of any grave.
     *
     * @param location The location to check.
     * @return True if the location is within 15 blocks of any grave, false otherwise.
     */
    private boolean isNearGrave(Location location, Player player) {
        for (Grave grave : plugin.getCacheManager().getGraveMap().values()) {
            Location graveLocation = plugin.getGraveManager().getGraveLocation(player.getLocation(), grave);
            if (graveLocation != null) {
                double distance = location.distance(graveLocation);
                if (distance <= 15) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes a specific type of compass (e.g., RECOVERY_COMPASS) from the player's inventory if within a 10-block radius of a grave.
     *
     * @param player   The player to check.
     * @param location The player's current location.
     */
    private void removeSpecificCompassNearGrave(Player player, Location location) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] items = inventory.getContents();

        // Retrieve the item name from the config or hardcoded
        String compassName = ChatColor.WHITE +  player.getDisplayName() + "'s Grave";

        for (ItemStack item : items) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta itemMeta = item.getItemMeta();
                if (itemMeta != null) {
                    // Check if the item is a compass with the specific name
                    if ((item.getType() == Material.valueOf(String.valueOf(plugin.getVersionManager().getMaterialForVersion("RECOVERY_COMPASS"))))
                            && itemMeta.hasDisplayName()
                            && itemMeta.getDisplayName().equals(compassName)) {

                        UUID graveUUID = getGraveUUIDFromItemStack(item);

                        if (graveUUID != null) {
                            Grave grave = plugin.getCacheManager().getGraveMap().get(graveUUID);
                            if (grave != null && location.getWorld() != null) {
                                Location graveLocation = plugin.getGraveManager().getGraveLocation(player.getLocation(), grave);
                                if (graveLocation != null && location.distance(graveLocation) <= 15) {
                                    // Remove the specific item from the inventory
                                    inventory.remove(item);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves the Grave UUID from the item stack.
     *
     * @param itemStack The item stack to check.
     * @return The UUID of the grave associated with the item stack, or null if not found.
     */
    private UUID getGraveUUIDFromItemStack(ItemStack itemStack) {
        if (itemStack.hasItemMeta()) {
            if (itemStack.getItemMeta() == null) return null;
            String uuidString = itemStack.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING);
            return uuidString != null ? UUID.fromString(uuidString) : null;
        }
        return null;
    }
}