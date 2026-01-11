package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilityParticleEnum;
import dev.cwhead.GravesX.event.GraveCompassUseEvent;
import dev.cwhead.GravesX.event.GraveParticleEvent;
import org.bukkit.*;
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
 * Listener for handling PlayerInteractEvent to interact with graves and compasses.
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

        if (!isMainHandInteraction(event) || !isNotSpectatorMode(player)) return;

        if (event.getClickedBlock() != null) {
            handleBlockInteraction(event, player);
        }
        if (event.getItem() != null) {
            handleCompassInteraction(event, player);
        }
    }

    /**
     * Checks if the interaction is performed with the main hand.
     *
     * @param event The PlayerInteractEvent.
     * @return True if the interaction is performed with the main hand, false otherwise.
     */
    private boolean isMainHandInteraction(PlayerInteractEvent event) {
        return !plugin.getVersionManager().hasSecondHand()
                || (event.getHand() != null && event.getHand() == EquipmentSlot.HAND);
    }

    /**
     * Checks if the player is not in Spectator mode. If they have bypass perms, they can get around it.
     *
     * @param player The player to check.
     * @return True if the player is not in Spectator mode, false otherwise.
     */
    private boolean isNotSpectatorMode(Player player) {
        if (plugin.getVersionManager().is_v1_7()) return true;

        if (plugin.getPermissionManager().hasGrantedPermission("graves.spectator.bypass", player.getPlayer())) return true;

        return Objects.requireNonNull(player.getPlayer()).getGameMode() != GameMode.SPECTATOR;
    }

    /**
     * Handles interactions with blocks, including graves.
     *
     * @param event  The PlayerInteractEvent.
     * @param player The player interacting with the block.
     */
    private void handleBlockInteraction(PlayerInteractEvent event, Player player) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (event.useInteractedBlock() != Event.Result.DENY && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            handleGraveInteraction(event, player, block);
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
        Grave grave = plugin.getCacheManager().getGrave(block);

        if (grave == null) {
            Block blockRelative = block.getRelative(event.getBlockFace());
            if (!blockRelative.getType().isSolid()) {
                grave = plugin.getCacheManager().getGrave(blockRelative);
            }
        }

        if (grave == null) return;

        event.setCancelled(true);
        Grave finalGrave = grave;
        try {
            plugin.getSchedulerManager().runTaskLater(() ->
                    plugin.getGraveManager().openGrave(player, block.getLocation(), finalGrave), 1L);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to open grave at x:" + block.getX() + " y:" + block.getY() + " z:" + block.getZ());
            plugin.logStackTrace(e);
        }
    }

    /**
     * Handles interactions with compasses to update or remove them based on the graves they are tracking.
     *
     * @param event  The PlayerInteractEvent.
     * @param player The player interacting with the item.
     */
    private void handleCompassInteraction(PlayerInteractEvent event, Player player) {
        ItemStack itemStack = event.getItem();
        if (itemStack == null) return;

        UUID uuid = plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack);
        if (uuid == null) return;

        if (!plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
            player.getInventory().remove(itemStack);
            event.setCancelled(true);
            return;
        }

        Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);
        List<Location> locationList = plugin.getGraveManager().getGraveLocationList(player.getLocation(), grave);

        if (locationList.isEmpty()) {
            player.getInventory().remove(itemStack);
            event.setCancelled(true);
            return;
        }

        Location location = locationList.get(0);

        if (event.getClickedBlock() != null
                && plugin.getLocationManager().hasGrave(event.getClickedBlock().getLocation())
                && player.getInventory().getItemInMainHand().getType().toString().toLowerCase().contains("compass")) {
            player.getInventory().remove(itemStack);
            event.setCancelled(true);
            return;
        }

        ItemStack graveCompass = plugin.getEntityManager().createGraveCompass(player, location, grave);
        if (graveCompass == null || graveCompass.getItemMeta() == null || graveCompass.getItemMeta().getDisplayName().contains("Abandoned")) {
            player.getInventory().remove(itemStack);
            event.setCancelled(true);
            return;
        }

        GraveCompassUseEvent modernUse = new GraveCompassUseEvent(player, grave);
        plugin.getServer().getPluginManager().callEvent(modernUse);

        com.ranull.graves.event.GraveCompassUseEvent legacyUse = new com.ranull.graves.event.GraveCompassUseEvent(player, grave);
        plugin.getServer().getPluginManager().callEvent(legacyUse);

        boolean blockedUse = modernUse.isCancelled() || modernUse.isAddon() || legacyUse.isCancelled() || legacyUse.isAddon();

        if (!blockedUse) {
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), graveCompass);

            plugin.getEntityManager().runFunction(
                    player,
                    Objects.requireNonNull(plugin.getConfigManager().getConfigSection("compass.function", grave).getString("compass.function")),
                    grave
            );

            if (plugin.getConfigManager().getConfigSection("compass.particles.enabled", grave).getBoolean("compass.particles.enabled")) {
                GraveParticleEvent modernParticle = new GraveParticleEvent(player, grave);
                plugin.getServer().getPluginManager().callEvent(modernParticle);

                com.ranull.graves.event.GraveParticleEvent legacyParticle = new com.ranull.graves.event.GraveParticleEvent(player, grave);
                plugin.getServer().getPluginManager().callEvent(legacyParticle);

                boolean blockedParticle = modernParticle.isCancelled() || modernParticle.isAddon()
                        || legacyParticle.isCancelled() || legacyParticle.isAddon();

                if (!blockedParticle) {
                    plugin.getParticleManager().startCompassParticleTrail(
                            player.getLocation(),
                            grave.getLocationDeath(),
                            plugin.getVersionManager().getParticleForVersion(
                                    Objects.requireNonNull(plugin.getConfigManager().getConfigSection("compass.particles.particle", grave)
                                            .getString("compass.particles.particle")).toUpperCase()
                            ),
                            plugin.getConfigManager().getConfigSection("compass.particles.count", grave)
                                    .getInt("compass.particles.count", 5),
                            plugin.getConfigManager().getConfigSection("compass.particles.speed", grave)
                                    .getDouble("compass.particles.speed", 0.3D),
                            plugin.getConfigManager().getConfigSection("compass.particles.duration", grave)
                                    .getInt("compass.particles.duration"),
                            player.getUniqueId()
                    );
                }
            }
        }

        event.setCancelled(true);
    }
}