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
import org.bukkit.potion.PotionEffect;

import java.util.List;

/**
 * Handles player respawns and performs grave-related actions:
 * <ul>
 *   <li>Runs a configured function shortly after respawn.</li>
 *   <li>Optionally grants temporary potion effects if the grave is recent.</li>
 *   <li>Optionally gives a compass pointing to the grave.</li>
 * </ul>
 * All world/player inventory interactions are deferred to the respawn location's region
 * to ensure correct, thread-safe execution order after the player is placed in the world.
 */
public class PlayerRespawnListener implements Listener {
    private final Graves plugin;

    public PlayerRespawnListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final List<String> permissionList = plugin.getPermissionList(player);
        final List<Grave> graveList = plugin.getGraveManager().getGraveList(player);

        if (graveList.isEmpty()) return;

        final Grave grave = graveList.get(graveList.size() - 1);
        final Location respawnLoc = event.getRespawnLocation().clone();

        scheduleRespawnFunction(player, permissionList, grave, respawnLoc);

        applyPotionEffectIfWithinTime(player, permissionList, grave, respawnLoc);

        if (shouldGiveCompass(player, permissionList, grave)) {
            giveCompassToPlayer(event, player, grave, respawnLoc);
        }
    }

    /**
     * Schedules the configured respawn function to run one tick after respawn on the
     * player's respawn location region.
     */
    private void scheduleRespawnFunction(Player player, List<String> permissionList, Grave grave, Location respawnLoc) {
        plugin.getGravesXScheduler().runTaskLater(() ->
                        plugin.getGravesXScheduler().execute(respawnLoc, () ->
                                plugin.getEntityManager().runFunction(
                                        player,
                                        plugin.getConfig("respawn.function", player, permissionList)
                                                .getString("respawn.function", "none"),
                                        grave
                                )
                        )
                , 1L);
    }

    /**
     * Applies temporary potion effects if the grave's lived time is within the configured limit.
     * Executed one tick after respawn on the respawn location region.
     */
    private void applyPotionEffectIfWithinTime(Player player, List<String> permissionList, Grave grave, Location respawnLoc) {
        plugin.getGravesXScheduler().runTaskLater(() ->
                        plugin.getGravesXScheduler().execute(respawnLoc, () -> {
                            boolean enabled = plugin.getConfig("respawn.potion-effect", player, permissionList)
                                    .getBoolean("respawn.potion-effect");
                            boolean hasPerm = plugin.hasGrantedPermission("graves.potion-effect", player);
                            if (!enabled || !hasPerm) return;

                            long limitMs = plugin.getConfig("respawn.potion-effect-time-limit", player, permissionList)
                                    .getInt("respawn.potion-effect-time-limit") * 1000L;
                            if (grave.getLivedTime() > limitMs) return;

                            int durationTicks = plugin.getConfig("respawn.potion-effect-duration", player, permissionList)
                                    .getInt("respawn.potion-effect-duration") * 20;

                            PotionEffect resist = new PotionEffect(
                                    plugin.getVersionManager().getPotionEffectTypeFromVersion("RESISTANCE"),
                                    durationTicks, 4
                            );
                            PotionEffect fireResist = new PotionEffect(
                                    plugin.getVersionManager().getPotionEffectTypeFromVersion("FIRE_RESISTANCE"),
                                    durationTicks, 0
                            );
                            player.addPotionEffect(resist);
                            player.addPotionEffect(fireResist);
                        })
                , 1L);
    }

    /**
     * Returns true if a compass should be given: compass feature available, enabled,
     * and within the configured time window relative to the grave's lived time.
     */
    private boolean shouldGiveCompass(Player player, List<String> permissionList, Grave grave) {
        return plugin.getVersionManager().hasCompassMeta()
                && plugin.getConfig("respawn.compass", player, permissionList).getBoolean("respawn.compass")
                && grave.getLivedTime() <= plugin.getConfig("respawn.compass-time", player, permissionList)
                .getInt("respawn.compass-time") * 1000L;
    }

    /**
     * Creates and gives the grave compass one tick after respawn, executing on the respawn
     * location region to safely read locations and modify the player's inventory.
     */
    private void giveCompassToPlayer(PlayerRespawnEvent event, Player player, Grave grave, Location respawnLoc) {
        plugin.getGravesXScheduler().runTaskLater(() ->
                        plugin.getGravesXScheduler().execute(respawnLoc, () -> {
                            List<Location> targets = plugin.getGraveManager().getGraveLocationList(
                                    event.getRespawnLocation(), grave
                            );

                            if (targets.isEmpty()) return;

                            ItemStack compass = plugin.getEntityManager().createGraveCompass(player, targets.get(0), grave);
                            if (compass != null) {
                                player.getInventory().addItem(compass);
                            }
                        })
                , 1L);
    }
}