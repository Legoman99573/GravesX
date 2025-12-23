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
     * and falling back to region-safe {@code teleport} via GravesX scheduler.
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
                            fallbackRegionTeleport(player, safeTarget, cause, plugin).whenComplete((ok2, ex2) -> {
                                completeOnRegion(plugin, safeTarget, result, ex2 == null && Boolean.TRUE.equals(ok2));
                            });
                        } else {
                            plugin.getGravesXScheduler().execute(safeTarget, () -> {
                                completeOnRegion(plugin, safeTarget, result, isAtTarget(player, safeTarget));
                            });
                        }
                    });
                    return result;
                }
            } catch (Throwable ignored) {
            }
        }

        fallbackRegionTeleport(player, safeTarget, cause, plugin).whenComplete((ok, ex) -> {
            completeOnRegion(plugin, safeTarget, result, ex == null && Boolean.TRUE.equals(ok));
        });

        return result;
    }

    /**
     * Runs {@code player.teleport(...)} on the owning region thread using GravesX's scheduler.
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
                    fut.complete(ok && isAtTarget(player, safeTarget));
                } catch (Throwable t) {
                    fut.complete(false);
                }
            });
        } catch (Throwable t) {
            fut.complete(false);
        }
        return fut;
    }

    /**
     * Ensures the returned future completes on the region thread that owns {@code at}.
     * (If already completed, does nothing.)
     */
    private static void completeOnRegion(Graves plugin, Location at, CompletableFuture<Boolean> fut, boolean value) {
        if (fut.isDone()) return;
        try {
            plugin.getGravesXScheduler().execute(at, () -> {
                if (!fut.isDone()) fut.complete(value);
            });
        } catch (Throwable t) {
            if (!fut.isDone()) fut.complete(value);
        }
    }

    /**
     * "Ended up at target" check. Keep this tolerant (yaw/pitch may differ).
     */
    private static boolean isAtTarget(Player player, Location target) {
        if (player == null || target == null || target.getWorld() == null) return false;
        Location now = player.getLocation();
        if (now.getWorld() == null || now.getWorld() != target.getWorld()) return false;

        return now.distanceSquared(target) <= 0.01;
    }
}