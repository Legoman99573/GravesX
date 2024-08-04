package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.type.Graveyard;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Listener for handling PlayerInteractEvent to interact with graves, graveyards, and compasses.
 */
public class PlayerInteractListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a PlayerInteractListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerInteractListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerInteractEvent when a player interacts with blocks or items.
     *
     * This method processes interactions with:
     * - Graves: Opens a grave if the player interacts with a block or adjacent block that represents a grave.
     * - Graveyards: Modifies the graveyard if the player interacts with blocks while holding a bone, depending on
     *   the current state of the graveyard modification.
     * - Compasses: Updates or removes the compass item based on the grave it is tracking.
     *
     * The event is only processed if:
     * - The hand used for the interaction is the main hand (or the plugin version does not support a second hand).
     * - The player is not in Spectator mode (if the server version is 1.7).
     *
     * @param event The PlayerInteractEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (isMainHandInteraction(event) && isNotSpectatorMode(player)) {
            if (event.getClickedBlock() != null) {
                handleBlockInteraction(event, player);
            }
            if (event.getItem() != null) {
                handleCompassInteraction(event, player);
            }
        }
    }

    /**
     * Checks if the interaction is performed with the main hand.
     *
     * @param event The PlayerInteractEvent.
     * @return True if the interaction is performed with the main hand, false otherwise.
     */
    private boolean isMainHandInteraction(PlayerInteractEvent event) {
        return !plugin.getVersionManager().hasSecondHand() || (event.getHand() != null && event.getHand() == EquipmentSlot.HAND);
    }

    /**
     * Checks if the player is not in Spectator mode.
     *
     * @param player The player to check.
     * @return True if the player is not in Spectator mode, false otherwise.
     */
    private boolean isNotSpectatorMode(Player player) {
        return plugin.getVersionManager().is_v1_7() || player.getGameMode() != GameMode.SPECTATOR;
    }

    /**
     * Handles interactions with blocks, including graves and graveyards.
     *
     * @param event  The PlayerInteractEvent.
     * @param player The player interacting with the block.
     */
    private void handleBlockInteraction(PlayerInteractEvent event, Player player) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return; // Exit early if block is null
        }

        if (event.useInteractedBlock() != Event.Result.DENY && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            handleGraveInteraction(event, player, block);
        }
        if (event.getItem() != null && event.getItem().getType() == Material.BONE && plugin.getGraveyardManager().isModifyingGraveyard(player)) {
            handleGraveyardModification(event, player, block);
        }
    }

    /**
     * Handles interactions with graves.
     *
     * @param event  The PlayerInteractEvent.
     * @param player The player interacting with the block.
     * @param block  The block being interacted with.
     */
    private void handleGraveInteraction(PlayerInteractEvent event, Player player, Block block) {
        if (block == null) {
            return; // Exit early if block is null
        }

        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

        if (grave == null) {
            Block blockRelative = block.getRelative(event.getBlockFace());

            if (!blockRelative.getType().isSolid()) {
                grave = plugin.getBlockManager().getGraveFromBlock(blockRelative);
            }
        }

        if (grave != null) {
            try {
                event.setCancelled(plugin.getGraveManager().openGrave(player, block.getLocation(), grave));
            } catch (NullPointerException e) {
                plugin.getLogger().severe("Failed to open grave: " + e.getMessage());
                plugin.logStackTrace(e);
            }
        }
    }

    /**
     * Handles interactions for modifying graveyards.
     *
     * @param event  The PlayerInteractEvent.
     * @param player The player interacting with the block.
     * @param block  The block being interacted with.
     */
    private void handleGraveyardModification(PlayerInteractEvent event, Player player, Block block) {
        Graveyard graveyard = plugin.getGraveyardManager().getModifyingGraveyard(player);
        Location location = block.getLocation().clone();
        Location locationRelative = block.getRelative(event.getBlockFace()).getLocation().clone();
        Location clickedLocation = Objects.requireNonNull(event.getClickedBlock()).getLocation();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (graveyard.hasGraveLocation(location)) {
                plugin.getGraveyardManager().removeLocationInGraveyard(player, location, graveyard);
                plugin.getCacheManager().removeRightClickedBlock(player.getName(), clickedLocation);
            } else if (graveyard.hasGraveLocation(locationRelative)) {
                plugin.getGraveyardManager().removeLocationInGraveyard(player, locationRelative, graveyard);
                plugin.getCacheManager().removeRightClickedBlock(player.getName(), clickedLocation);
            } else {
                if (plugin.getGraveyardManager().isLocationInGraveyard(locationRelative, graveyard)) {
                    plugin.getGraveyardManager().addLocationInGraveyard(player, locationRelative, graveyard);
                    plugin.getCacheManager().addRightClickedBlock(player.getName(), clickedLocation);
                } else {
                    player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Can't set location outside graveyard " + graveyard.getName());
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Can't break while modifying graveyard " + graveyard.getName());
        }

        event.setCancelled(true);
    }

    /**
     * Handles interactions with compasses to update or remove them based on the graves they are tracking.
     *
     * @param event  The PlayerInteractEvent.
     * @param player The player interacting with the item.
     */
    private void handleCompassInteraction(PlayerInteractEvent event, Player player) {
        ItemStack itemStack = event.getItem();

        if (itemStack == null) {
            return; // Exit early if itemStack is null
        }

        UUID uuid = plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack);

        if (uuid != null) {
            if (plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);
                List<Location> locationList = plugin.getGraveManager().getGraveLocationList(player.getLocation(), grave);

                if (!locationList.isEmpty()) {
                    Location location = locationList.get(0);
                    if (event.getClickedBlock() != null && plugin.getLocationManager().hasGrave(event.getClickedBlock().getLocation())
                            && player.getInventory().getItemInMainHand().getType().toString().toLowerCase().contains("compass")) {
                        player.getInventory().remove(itemStack);
                        player.closeInventory(); // Close the player's inventory
                        player.openInventory(player.getInventory()); // Reopen the player's inventory
                    } else {
                        player.getInventory().setItem(player.getInventory().getHeldItemSlot(),
                                plugin.getEntityManager().createGraveCompass(player, location, grave));
                        plugin.getEntityManager().runFunction(player, plugin.getConfig("compass.function", grave).getString("compass.function"), grave);
                    }
                } else {
                    player.getInventory().remove(itemStack);
                    player.closeInventory(); // Close the player's inventory
                    player.openInventory(player.getInventory()); // Reopen the player's inventory
                }
            } else {
                player.getInventory().remove(itemStack);
                player.closeInventory(); // Close the player's inventory
                player.openInventory(player.getInventory()); // Reopen the player's inventory
            }
            event.setCancelled(true);
        }
    }
}