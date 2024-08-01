package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.event.GraveAutoLootEvent;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listener for handling PlayerMoveEvent to manage interactions with graves and related mechanics.
 */
public class PlayerMoveListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a PlayerMoveListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerMoveListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerMoveEvent to manage interactions with graves and update player locations.
     *
     * This method checks if the player has moved and whether the new location is inside a border and safe.
     * It then updates the player's last known solid location if applicable.
     *
     * Additionally, if the player is moving over a location that is known to contain a grave,
     * and if the grave's configuration allows walking over it, the grave is automatically looted
     * if the player is allowed to open it.
     *
     * @param event The PlayerMoveEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (isNotSpectatorMode(player)) {
            if (hasPlayerMoved(event)) {
                Location location = LocationUtil.roundLocation(player.getLocation());

                if (isLocationSafe(location)) {
                    plugin.getLocationManager().setLastSolidLocation(player, location.clone());
                }

                if (isLocationContainingGrave(location)) {
                    handleGraveAutoLoot(event, player, location);
                }
            }
        }
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
     * Checks if the player has moved to a different block.
     *
     * @param event The PlayerMoveEvent.
     * @return True if the player has moved to a different block, false otherwise.
     */
    private boolean hasPlayerMoved(PlayerMoveEvent event) {
        return event.getTo() != null && (event.getTo().getBlockX() != event.getFrom().getBlockX()
                || event.getTo().getBlockY() != event.getFrom().getBlockY()
                || event.getTo().getBlockZ() != event.getFrom().getBlockZ());
    }

    /**
     * Checks if the location is safe for the player.
     *
     * @param location The location to check.
     * @return True if the location is safe, false otherwise.
     */
    private boolean isLocationSafe(Location location) {
        return plugin.getLocationManager().isInsideBorder(location)
                && location.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()
                && plugin.getLocationManager().isLocationSafePlayer(location);
    }

    /**
     * Checks if the location contains a grave.
     *
     * @param location The location to check.
     * @return True if the location contains a grave, false otherwise.
     */
    private boolean isLocationContainingGrave(Location location) {
        return location.getWorld() != null && plugin.getDataManager().hasChunkData(location);
    }

    /**
     * Handles the auto-loot of a grave when a player moves over it.
     *
     * @param event    The PlayerMoveEvent.
     * @param player   The player moving over the grave.
     * @param location The location of the grave.
     */
    private void handleGraveAutoLoot(PlayerMoveEvent event, Player player, Location location) {
        ChunkData chunkData = plugin.getDataManager().getChunkData(location);
        BlockData blockData = getBlockDataFromLocation(chunkData, location);

        if (blockData != null) {
            Grave grave = plugin.getCacheManager().getGraveMap().get(blockData.getGraveUUID());

            if (grave != null && plugin.getConfig("block.walk-over", grave).getBoolean("block.walk-over")
                    && plugin.getEntityManager().canOpenGrave(player, grave)) {
                plugin.getGraveManager().cleanupCompasses(player, grave);
                GraveAutoLootEvent graveAutoLootEvent = new GraveAutoLootEvent(player, location, grave);

                plugin.getServer().getPluginManager().callEvent(graveAutoLootEvent);
                if (!graveAutoLootEvent.isCancelled()) {
                    plugin.getGraveManager().autoLootGrave(player, location, grave);
                }
            }
        }
    }

    /**
     * Retrieves the BlockData from the location within the ChunkData.
     *
     * @param chunkData The ChunkData containing the block data map.
     * @param location  The location to retrieve the block data from.
     * @return The BlockData at the specified location, or null if not found.
     */
    private BlockData getBlockDataFromLocation(ChunkData chunkData, Location location) {
        BlockData blockData = null;

        if (chunkData.getBlockDataMap().containsKey(location)) {
            blockData = chunkData.getBlockDataMap().get(location);
        } else if (chunkData.getBlockDataMap().containsKey(location.clone().add(0, 1, 0))) {
            blockData = chunkData.getBlockDataMap().get(location.clone().add(0, 1, 0));
        } else if (chunkData.getBlockDataMap().containsKey(location.clone().subtract(0, 1, 0))) {
            blockData = chunkData.getBlockDataMap().get(location.clone().subtract(0, 1, 0));
        }

        return blockData;
    }
}
