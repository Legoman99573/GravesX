package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.StringUtil;
import dev.cwhead.GravesX.event.GraveWalkOverEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Listener for handling PlayerMoveEvent to manage interactions with graves and related mechanics.
 */
public class PlayerMoveListener implements Listener {
    private final Graves plugin;

    private final Map<UUID, Long> compassCheckCooldown = new HashMap<>();

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
     * This method checks if the player has moved and whether the new location is inside a border and safe.
     * It then updates the player's last known solid location if applicable.
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
                    handleGraveAutoLootOnWalk(event, player, location);
                }

                // Remove the specific type of compass if within 10 blocks of a grave
                removeSpecificCompassNearGrave(player, location);
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
        return plugin.getVersionManager().is_v1_7() || Objects.requireNonNull(player.getPlayer()).getGameMode() != GameMode.SPECTATOR;
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
    private void handleGraveAutoLootOnWalk(PlayerMoveEvent event, Player player, Location location) {
        ChunkData chunkData = plugin.getDataManager().getChunkData(location);
        BlockData blockData = getBlockDataFromLocation(chunkData, location);

        if (blockData == null) return;

        Grave grave = plugin.getCacheManager().getGraveMap().get(blockData.getGraveUUID());
        if (grave == null) return;

        if (plugin.getConfig("block.walk-over", grave).getBoolean("block.walk-over")
                && plugin.getEntityManager().canOpenGrave(player, grave)) {

            plugin.getGraveManager().cleanupCompasses(player, grave);

            GraveWalkOverEvent modern = new GraveWalkOverEvent(player, location, grave);
            plugin.getServer().getPluginManager().callEvent(modern);

            com.ranull.graves.event.GraveWalkOverEvent legacy = new com.ranull.graves.event.GraveWalkOverEvent(player, location, grave);
            plugin.getServer().getPluginManager().callEvent(legacy);

            if (!(modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon())) {
                plugin.getGraveManager().autoLootGrave(player, location, grave);

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

    private void removeSpecificCompassNearGrave(Player player, Location location) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();

        // Cooldown check (1 second per player)
        Long last = compassCheckCooldown.get(playerId);
        if (last != null && (now - last) < 1000) {
            return;
        }
        compassCheckCooldown.put(playerId, now);

        PlayerInventory inventory = player.getInventory();
        Material recoveryCompass;

        try {
            recoveryCompass = Material.valueOf(String.valueOf(plugin.getVersionManager().getMaterialForVersion("RECOVERY_COMPASS")));
        } catch (IllegalArgumentException e) {
            return;
        }

        if (location.getWorld() == null) return;

        ItemStack[] items = inventory.getContents();
        if (items == null) return;

        for (ItemStack item : items) {
            if (item == null || item.getType() != recoveryCompass) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) continue;

            UUID graveUUID = getGraveUUIDFromItemStack(item);
            if (graveUUID == null) continue;

            Grave grave = plugin.getCacheManager().getGraveMap().get(graveUUID);
            if (grave == null) continue;

            Location graveLocation = plugin.getGraveManager().getGraveLocation(location, grave);
            if (graveLocation == null || !location.getWorld().equals(graveLocation.getWorld())) continue;

            if (location.distance(graveLocation) > 15) continue;

            String configuredName = plugin.getConfig("compass.name", grave).getString("compass.name");
            if (configuredName == null) continue;

            String expectedName = StringUtil.parseString("&f" + configuredName, grave, plugin);
            if (plugin.getIntegrationManager().hasMiniMessage()) {
                expectedName = MiniMessage.parseString(expectedName);
            }

            if (meta.getDisplayName().equals(expectedName)) {
                inventory.remove(item);
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
        if (!itemStack.hasItemMeta()) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        String uuidString = meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING);
        return uuidString != null ? UUID.fromString(uuidString) : null;
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