package dev.cwhead.GravesX.compatibility;

import com.ranull.graves.Graves;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Teleport compatibility layer for Paper/Folia vs. legacy servers.
 * <p>
 * - Tries {@code Player#teleportAsync(Location, TeleportCause)} if present.
 * - If missing or it fails, falls back to {@code Player#teleport(Location, TeleportCause)}
 *   executed on the owning region thread via GravesX's UniversalScheduler.
 */
public final class CompatibilityTeleport {

    private static final AtomicReference<Method> TELEPORT_ASYNC = new AtomicReference<>();

    private CompatibilityTeleport() {}

    /**
     * Teleport with {@link PlayerTeleportEvent.TeleportCause#PLUGIN}.
     */
    public static CompletableFuture<Boolean> teleportSafely(Player player, Location target, Graves plugin) {
        return teleportSafely(player, target, PlayerTeleportEvent.TeleportCause.PLUGIN, plugin);
    }

    /**
     * Teleport with a specific cause, preferring {@code teleportAsync} when available,
     * and falling back to region-safe {@code teleport} via {@code GravesXScheduler.execute(...)}.
     *
     * @return a future that completes with {@code true} if the player ended up at the target.
     */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Boolean> teleportSafely(Player player,
                                                            Location target,
                                                            PlayerTeleportEvent.TeleportCause cause,
                                                            Graves plugin) {
        final CompletableFuture<Boolean> result = new CompletableFuture<>();

        // Validate and never mutate the original location
        final Location safeTarget = target == null ? null : target.clone();
        if (player == null || plugin == null || safeTarget == null || safeTarget.getWorld() == null) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                        + ChatColor.RESET + "Teleport location unavailable.");
            }
            result.complete(false);
            return result;
        }

        // Try to get/call teleportAsync(Location, TeleportCause) once and cache the Method
        Method m = TELEPORT_ASYNC.get();
        if (m == null) {
            try {
                m = Player.class.getMethod("teleportAsync", Location.class, PlayerTeleportEvent.TeleportCause.class);
                TELEPORT_ASYNC.compareAndSet(null, m);
            } catch (Throwable ignored) {
                // Not available on this platform
            }
        }

        if (m != null) {
            try {
                Object cf = m.invoke(player, safeTarget, cause);
                if (cf instanceof CompletableFuture) {
                    ((CompletableFuture<Boolean>) cf).whenComplete((ok, ex) -> {
                        if (ex != null || Boolean.FALSE.equals(ok)) {
                            // Async path failed → fallback on the region thread
                            fallbackRegionTeleport(player, safeTarget, cause, plugin).whenComplete((ok2, ex2) -> {
                                result.complete(ex2 == null && Boolean.TRUE.equals(ok2));
                            });
                        } else {
                            result.complete(true);
                        }
                    });
                    return result;
                }
            } catch (Throwable ignored) {
            }
        }

        // No teleportAsync available or failed early → fallback
        fallbackRegionTeleport(player, safeTarget, cause, plugin).whenComplete((ok, ex) -> {
            result.complete(ex == null && Boolean.TRUE.equals(ok));
        });

        return result;
    }

    /**
     * Runs {@code player.teleport(...)} on the owning region thread using GravesX's UniversalScheduler.
     */
    private static CompletableFuture<Boolean> fallbackRegionTeleport(Player player,
                                                                     Location safeTarget,
                                                                     PlayerTeleportEvent.TeleportCause cause,
                                                                     Graves plugin) {
        final CompletableFuture<Boolean> fut = new CompletableFuture<>();
        try {
            plugin.getGravesXScheduler().execute(safeTarget, () -> {
                try {
                    boolean ok = player.teleport(safeTarget, cause);
                    fut.complete(ok);
                } catch (Throwable t) {
                    fut.complete(false);
                }
            });
        } catch (Throwable t) {
            fut.complete(false);
        }
        return fut;
    }
}