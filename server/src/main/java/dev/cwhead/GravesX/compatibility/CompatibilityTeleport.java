package dev.cwhead.GravesX.compatibility;

import com.ranull.graves.Graves;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class CompatibilityTeleport {

    private static final AtomicReference<Method> TELEPORT_ASYNC = new AtomicReference<>();
    private static final AtomicReference<Method> ENTITY_TELEPORT_ASYNC = new AtomicReference<>();

    private CompatibilityTeleport() {}

    /**
     * Teleports a player using the PLUGIN teleport cause.
     *
     * @param player player to teleport
     * @param target destination
     * @param plugin owning plugin
     * @return future indicating whether the player reached the destination
     */
    public static CompletableFuture<Boolean> teleportSafely(Player player, Location target, Graves plugin) {
        return teleportSafely(player, target, PlayerTeleportEvent.TeleportCause.PLUGIN, plugin);
    }

    /**
     * Teleports a player using the requested teleport cause.
     *
     * @param player player to teleport
     * @param target destination
     * @param cause teleport cause
     * @param plugin owning plugin
     * @return future indicating whether the player reached the destination
     */
    public static CompletableFuture<Boolean> teleportSafely(Player player, Location target, PlayerTeleportEvent.TeleportCause cause, Graves plugin) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        Location safeTarget = target == null ? null : target.clone();

        if (player == null || plugin == null || safeTarget == null || safeTarget.getWorld() == null || cause == null) {
            result.complete(false);
            return result;
        }

        try {
            plugin.getSchedulerManager().execute(player, () -> {
                if (result.isDone()) {
                    return;
                }

                if (!player.isOnline() || !player.isValid()) {
                    result.complete(false);
                    return;
                }

                Method method = teleportAsync();

                if (method != null) {
                    try {
                        Object returned = method.invoke(player, safeTarget, cause);

                        if (returned instanceof CompletableFuture<?> future) {
                            future.whenComplete((success, exception) -> {
                                if (exception != null || !Boolean.TRUE.equals(success)) {
                                    result.complete(false);
                                    return;
                                }

                                verifyDestination(player, safeTarget, plugin, result);
                            });

                            return;
                        }
                    } catch (InvocationTargetException exception) {
                        // The async invocation failed. Try only a
                        // scheduler-safe synchronous fallback.
                    } catch (ReflectiveOperationException exception) {
                        // Method invocation is unavailable.
                    }
                }

                fallbackPlayerTeleport(player, safeTarget, cause, plugin, result);
            });
        } catch (Throwable exception) {
            result.completeExceptionally(exception);
        }

        return result;
    }

    /**
     * Teleports a non-player entity.
     *
     * @param entity entity to teleport
     * @param target destination
     * @param plugin owning plugin
     * @return future indicating whether the entity reached the destination
     */
    public static CompletableFuture<Boolean> teleportSafely(Entity entity, Location target, Graves plugin) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        Location safeTarget = target == null ? null : target.clone();

        if (entity == null || plugin == null || safeTarget == null || safeTarget.getWorld() == null) {
            result.complete(false);
            return result;
        }

        try {
            plugin.getSchedulerManager().execute(entity, () -> {
                if (result.isDone()) {
                    return;
                }

                if (!entity.isValid()) {
                    result.complete(false);
                    return;
                }

                Method method = entityTeleportAsync();

                if (method != null) {
                    try {
                        Object returned = method.invoke(entity, safeTarget);

                        if (returned instanceof CompletableFuture<?> future) {
                            future.whenComplete((success, exception) ->
                                    result.complete(exception == null && Boolean.TRUE.equals(success)));
                            return;
                        }
                    } catch (ReflectiveOperationException exception) {
                        // Fall through to the legacy path below.
                    }
                }

                // Legacy (non-Folia) servers only. Folia always provides teleportAsync.
                if (!canSynchronouslyTeleport(plugin, safeTarget)) {
                    result.complete(false);
                    return;
                }

                try {
                    boolean success = legacyTeleport(entity, safeTarget, null);

                    result.complete(success && isAtTarget(entity, safeTarget));
                } catch (Throwable exception) {
                    result.completeExceptionally(exception);
                }
            });
        } catch (Throwable exception) {
            result.completeExceptionally(exception);
        }

        return result;
    }

    /**
     * Gets and caches Entity#teleportAsync(Location) when the running
     * server API provides it (Paper and Folia).
     *
     * @return async teleport method, or null when unavailable
     */
    public static Method entityTeleportAsync() {
        Method method = ENTITY_TELEPORT_ASYNC.get();

        if (method != null) {
            return method;
        }

        try {
            method = Entity.class.getMethod("teleportAsync", Location.class);

            ENTITY_TELEPORT_ASYNC.compareAndSet(null, method);

            return ENTITY_TELEPORT_ASYNC.get();
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    /**
     * Synchronous teleport for legacy Spigot servers that have no teleportAsync.
     * Invoked reflectively so it can never be linked on region-threaded servers,
     * where a synchronous teleport is rejected.
     */
    private static boolean legacyTeleport(Entity entity, Location target, PlayerTeleportEvent.TeleportCause cause)
            throws ReflectiveOperationException {
        Object returned;

        if (cause != null && entity instanceof Player) {
            returned = Player.class
                    .getMethod("teleport", Location.class, PlayerTeleportEvent.TeleportCause.class)
                    .invoke(entity, target, cause);
        } else {
            returned = Entity.class.getMethod("teleport", Location.class).invoke(entity, target);
        }

        return Boolean.TRUE.equals(returned);
    }

    /**
     * Gets and caches Player#teleportAsync(Location, TeleportCause)
     * when the running server API provides it.
     *
     * @return async teleport method, or null when unavailable
     */
    public static Method teleportAsync() {
        Method method = TELEPORT_ASYNC.get();

        if (method != null) {
            return method;
        }

        try {
            method = Player.class.getMethod("teleportAsync", Location.class, PlayerTeleportEvent.TeleportCause.class);

            TELEPORT_ASYNC.compareAndSet(null, method);

            return TELEPORT_ASYNC.get();
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    /**
     * Performs a synchronous player teleport only when the current
     * scheduler context can safely access the destination.
     */
    private static void fallbackPlayerTeleport(Player player, Location target, PlayerTeleportEvent.TeleportCause cause, Graves plugin, CompletableFuture<Boolean> result) {
        if (!canSynchronouslyTeleport(plugin, target)) {
            result.complete(false);
            return;
        }

        try {
            boolean success = legacyTeleport(player, target, cause);

            result.complete(success && isAtTarget(player, target));
        } catch (Throwable exception) {
            result.completeExceptionally(exception);
        }
    }

    /**
     * Checks whether synchronous teleportation is safe in the current scheduler context.
     */
    private static boolean canSynchronouslyTeleport(Graves plugin, Location target) {
        if (plugin == null || target == null || target.getWorld() == null) {
            return false;
        }

        return plugin.getSchedulerManager().isTickThread() || plugin.getSchedulerManager().isRegionThread(target);
    }

    /**
     * Verifies a player's destination on their owning entity scheduler.
     */
    private static void verifyDestination(Player player, Location target, Graves plugin, CompletableFuture<Boolean> result) {
        try {
            plugin.getSchedulerManager().execute(player, () -> {
                if (result.isDone()) {
                    return;
                }

                try {
                    result.complete(player.isOnline() && player.isValid() && isAtTarget(player, target));
                } catch (Throwable exception) {
                    result.completeExceptionally(exception);
                }
            });
        } catch (Throwable exception) {
            result.completeExceptionally(exception);
        }
    }

    /**
     * Checks whether a player reached the requested destination.
     */
    private static boolean isAtTarget(Player player, Location target) {
        if (player == null || target == null || target.getWorld() == null) {
            return false;
        }

        Location current = player.getLocation();

        if (current.getWorld() == null || current.getWorld() != target.getWorld()) {
            return false;
        }

        return current.distanceSquared(target) <= 0.01;
    }

    /**
     * Checks whether an entity reached the requested destination.
     */
    private static boolean isAtTarget(Entity entity, Location target) {
        if (entity == null || target == null || target.getWorld() == null) {
            return false;
        }

        Location current = entity.getLocation();

        if (current.getWorld() == null || current.getWorld() != target.getWorld()) {
            return false;
        }

        return current.distanceSquared(target) <= 0.01;
    }
}