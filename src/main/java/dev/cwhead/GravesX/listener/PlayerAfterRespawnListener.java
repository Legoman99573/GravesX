package dev.cwhead.GravesX.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs grave-related actions after a player respawns:
 * - executes a configured function
 * - optional potion effects
 * - optional grave compass
 *
 * Uses Paper's PlayerPostRespawnEvent when available, falls back to Bukkit's
 * PlayerRespawnEvent, and finally to a short post-death poll if neither fires.
 * All work that touches the world/inventory runs via the respawn-location region.
 */
public final class PlayerAfterRespawnListener implements Listener {
    private final Graves plugin;

    // De-dupe window so logic doesn't run twice if multiple paths fire close together
    private final Map<UUID, Long> lastHandled = new ConcurrentHashMap<>();
    private static final long DEDUPE_MS = 1500L;

    public PlayerAfterRespawnListener(Graves plugin) {
        this.plugin = plugin;
    }

    // Preferred: Paper's event after the player is fully respawned
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostRespawn(PlayerPostRespawnEvent event) {
        final Player player = event.getPlayer();
        final Location respawnLoc = event.getRespawnedLocation();
        if (dedupe(player)) return;
        handleAfterRespawn(player, respawnLoc);
    }

    // Also handle Bukkit's event if it fires
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBukkitRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final Location respawnLoc = event.getRespawnLocation();
        if (dedupe(player)) return;
        handleAfterRespawn(player, respawnLoc);
    }

    // Fallback: post-death polling until the player is alive again
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final long start = System.currentTimeMillis();

        final MyScheduledTask[] ref = new MyScheduledTask[1];
        ref[0] = plugin.getGravesXScheduler().runTaskTimer(() -> {
            if (System.currentTimeMillis() - start > 10_000L) {
                ref[0].cancel();
                return;
            }
            if (!player.isDead()) {
                if (!dedupe(player)) {
                    handleAfterRespawn(player, player.getLocation());
                }
                ref[0].cancel();
            }
        }, 20L, 20L);
    }

    private void handleAfterRespawn(Player player, Location respawnLoc) {
        final List<String> perms = plugin.getPermissionList(player);
        final List<Grave> graves = plugin.getGraveManager().getGraveList(player);
        if (graves.isEmpty()) return;

        final Grave grave = graves.get(graves.size() - 1);
        final Location anchor = (respawnLoc != null ? respawnLoc : player.getLocation()).clone();

        plugin.getGravesXScheduler().runTaskLater(() ->
                        plugin.getGravesXScheduler().execute(anchor, () ->
                                plugin.getEntityManager().runFunction(
                                        player,
                                        plugin.getConfig("respawn.function", player, perms).getString("respawn.function", "none"),
                                        grave
                                )
                        )
                , 1L);

        plugin.getGravesXScheduler().runTaskLater(() ->
                        plugin.getGravesXScheduler().execute(anchor, () -> {
                            boolean enabled = plugin.getConfig("respawn.potion-effect", player, perms)
                                    .getBoolean("respawn.potion-effect");
                            boolean hasPerm = plugin.hasGrantedPermission("graves.potion-effect", player);
                            if (!enabled || !hasPerm) return;

                            long limitMs = plugin.getConfig("respawn.potion-effect-time-limit", player, perms)
                                    .getInt("respawn.potion-effect-time-limit") * 1000L;
                            if (grave.getLivedTime() > limitMs) return;

                            int dur = plugin.getConfig("respawn.potion-effect-duration", player, perms)
                                    .getInt("respawn.potion-effect-duration") * 20;

                            PotionEffect resist = new PotionEffect(
                                    plugin.getVersionManager().getPotionEffectTypeFromVersion("RESISTANCE"), dur, 4);
                            PotionEffect fireResist = new PotionEffect(
                                    plugin.getVersionManager().getPotionEffectTypeFromVersion("FIRE_RESISTANCE"), dur, 0);
                            player.addPotionEffect(resist);
                            player.addPotionEffect(fireResist);
                        })
                , 1L);

        if (shouldGiveCompass(player, perms, grave)) {
            plugin.getGravesXScheduler().runTaskLater(() ->
                            plugin.getGravesXScheduler().execute(anchor, () -> {
                                List<Location> locs = plugin.getGraveManager().getGraveLocationList(anchor, grave);
                                if (locs.isEmpty()) return;
                                ItemStack compass = plugin.getEntityManager().createGraveCompass(player, locs.get(0), grave);
                                if (compass != null) player.getInventory().addItem(compass);
                            })
                    , 1L);
        }
    }

    private boolean shouldGiveCompass(Player player, List<String> permissionList, Grave grave) {
        return plugin.getVersionManager().hasCompassMeta()
                && plugin.getConfig("respawn.compass", player, permissionList).getBoolean("respawn.compass")
                && grave.getLivedTime() <= plugin.getConfig("respawn.compass-time", player, permissionList)
                .getInt("respawn.compass-time") * 1000L;
    }

    private boolean dedupe(Player p) {
        long now = System.currentTimeMillis();
        Long prev = lastHandled.put(p.getUniqueId(), now);
        return prev != null && (now - prev) < DEDUPE_MS;
    }
}