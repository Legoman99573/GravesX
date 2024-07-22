package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
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

        if (plugin.getVersionManager().is_v1_7() || player.getGameMode() != GameMode.SPECTATOR) {
            if (event.getTo() != null && (event.getTo().getBlockX() != event.getFrom().getBlockX()
                    || event.getTo().getBlockY() != event.getFrom().getBlockY()
                    || event.getTo().getBlockZ() != event.getFrom().getBlockZ())) {
                Location location = LocationUtil.roundLocation(player.getLocation());

                if (plugin.getLocationManager().isInsideBorder(location)
                        && location.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()
                        && plugin.getLocationManager().isLocationSafePlayer(location)) {
                    plugin.getLocationManager().setLastSolidLocation(player, location.clone());
                }

                if (location.getWorld() != null && plugin.getDataManager().hasChunkData(location)) {
                    ChunkData chunkData = plugin.getDataManager().getChunkData(location);
                    BlockData blockData = null;

                    if (chunkData.getBlockDataMap().containsKey(location)) {
                        blockData = chunkData.getBlockDataMap().get(location);
                    } else if (chunkData.getBlockDataMap().containsKey(location.clone().add(0, 1, 0))) {
                        blockData = chunkData.getBlockDataMap().get(location.clone().add(0, 1, 0));
                    } else if (chunkData.getBlockDataMap().containsKey(location.clone().subtract(0, 1, 0))) {
                        blockData = chunkData.getBlockDataMap().get(location.clone().subtract(0, 1, 0));
                    }

                    if (blockData != null && plugin.getCacheManager().getGraveMap()
                            .containsKey(blockData.getGraveUUID())) {
                        Grave grave = plugin.getCacheManager().getGraveMap().get(blockData.getGraveUUID());

                        if (grave != null && plugin.getConfig("block.walk-over", grave).getBoolean("block.walk-over")
                                && plugin.getEntityManager().canOpenGrave(player, grave)) {
                            plugin.getGraveManager().cleanupCompasses(player, grave);
                            plugin.getGraveManager().autoLootGrave(event.getPlayer(), location, grave);
                        }
                    }
                }
            }
        }
    }
}