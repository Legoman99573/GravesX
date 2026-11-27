package dev.cwhead.GravesX.api.permission;

import com.ranull.graves.Graves;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

/**
 * Permission helpers exposed through the GravesX API.
 *
 * <p>This API delegates to GravesX's {@link dev.cwhead.GravesX.manager.PermissionManager} to ensure
 * permission checks follow the same provider order and debug logging behavior used internally.</p>
 *
 * <p>Provider order:</p>
 * <ul>
 *   <li>LuckPerms (if available)</li>
 *   <li>Vault permissions (if available)</li>
 *   <li>Bukkit fallback (online only)</li>
 * </ul>
 */
public final class PermissionAPI {

    private final Graves plugin;

    /**
     * Creates a new permission API.
     *
     * @param plugin the active GravesX plugin instance
     */
    public PermissionAPI(@NotNull Graves plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        plugin.debugMessage("[PermissionAPI] PermissionAPI initialized.", 4);
    }

    /**
     * Checks whether an online player has a permission using the active permission provider.
     *
     * @param permission permission node
     * @param player online player
     * @return true if granted
     */
    public boolean hasGrantedPermission(@NotNull String permission, @NotNull Player player) {
        plugin.debugMessage("[PermissionAPI] hasGrantedPermission(String, Player) called. Player=" + player.getName()
                + " | Permission=" + permission, 4);

        boolean result = plugin.getPermissionManager().hasGrantedPermission(permission, player);

        plugin.debugMessage("[PermissionAPI] hasGrantedPermission(String, Player) result. Player=" + player.getName()
                + " | Permission=" + permission + " | Result=" + result, 4);

        return result;
    }

    /**
     * Checks whether an offline player has a permission using an offline-capable provider.
     *
     * <p>See the deprecated offline method in {@link dev.cwhead.GravesX.manager.PermissionManager} for
     * reliability caveats; this method delegates to that same behavior to keep results consistent.</p>
     *
     * @param permission permission node
     * @param offlinePlayer offline player
     * @return true if granted (if determinable by provider), otherwise false
     */
    @SuppressWarnings("deprecation")
    public boolean hasGrantedPermission(@NotNull String permission, @NotNull OfflinePlayer offlinePlayer) {
        plugin.debugMessage("[PermissionAPI] hasGrantedPermission(String, OfflinePlayer) called. OfflinePlayer=" + nameOf(offlinePlayer)
                + " | Permission=" + permission, 4);

        boolean result = plugin.getPermissionManager().hasGrantedPermission(permission, offlinePlayer);

        plugin.debugMessage("[PermissionAPI] hasGrantedPermission(String, OfflinePlayer) result. OfflinePlayer=" + nameOf(offlinePlayer)
                + " | Permission=" + permission + " | Result=" + result, 4);

        return result;
    }

    /**
     * Returns true if the player has the permission OR is op.
     *
     * @param permission permission node
     * @param player online player
     * @return true if op or granted
     */
    public boolean hasGrantedPermissionOrOp(@NotNull String permission, @NotNull Player player) {
        plugin.debugMessage("[PermissionAPI] hasGrantedPermissionOrOp(String, Player) called. Player=" + player.getName()
                + " | Permission=" + permission + " | isOp=" + player.isOp(), 4);

        boolean result = plugin.getPermissionManager().hasGrantedPermissionOrOp(permission, player);

        plugin.debugMessage("[PermissionAPI] hasGrantedPermissionOrOp(String, Player) result. Player=" + player.getName()
                + " | Permission=" + permission + " | Result=" + result, 4);

        return result;
    }

    /**
     * Returns true if the player has at least one of the provided permissions.
     *
     * @param player online player
     * @param permissions permissions to test
     * @return true if any granted
     */
    public boolean hasAnyGrantedPermission(@NotNull Player player, @NotNull String... permissions) {
        plugin.debugMessage("[PermissionAPI] hasAnyGrantedPermission(Player, String...) called. Player=" + player.getName()
                + " | PermissionsCount=" + (permissions == null ? "null" : permissions.length), 4);

        boolean result = plugin.getPermissionManager().hasAnyGrantedPermission(player, permissions);

        plugin.debugMessage("[PermissionAPI] hasAnyGrantedPermission(Player, String...) result. Player=" + player.getName()
                + " | Result=" + result, 4);

        return result;
    }

    /**
     * Returns true if the player has all of the provided permissions.
     *
     * @param player online player
     * @param permissions permissions to test
     * @return true if all granted
     */
    public boolean hasAllGrantedPermissions(@NotNull Player player, @NotNull String... permissions) {
        plugin.debugMessage("[PermissionAPI] hasAllGrantedPermissions(Player, String...) called. Player=" + player.getName()
                + " | PermissionsCount=" + (permissions == null ? "null" : permissions.length), 4);

        boolean result = plugin.getPermissionManager().hasAllGrantedPermissions(player, permissions);

        plugin.debugMessage("[PermissionAPI] hasAllGrantedPermissions(Player, String...) result. Player=" + player.getName()
                + " | Result=" + result, 4);

        return result;
    }

    /**
     * Returns true if the player has at least one of the provided permissions.
     *
     * @param player online player
     * @param permissions permissions to test
     * @return true if any granted
     */
    public boolean hasAnyGrantedPermission(@NotNull Player player, @NotNull Collection<String> permissions) {
        plugin.debugMessage("[PermissionAPI] hasAnyGrantedPermission(Player, Collection) called. Player=" + player.getName()
                + " | PermissionsCount=" + (permissions.isEmpty() ? "null" : permissions.size()), 4);

        boolean result = plugin.getPermissionManager().hasAnyGrantedPermission(player, permissions);

        plugin.debugMessage("[PermissionAPI] hasAnyGrantedPermission(Player, Collection) result. Player=" + player.getName()
                + " | Result=" + result, 4);

        return result;
    }

    /**
     * Returns true if the player has all of the provided permissions.
     *
     * @param player online player
     * @param permissions permissions to test
     * @return true if all granted
     */
    public boolean hasAllGrantedPermissions(@NotNull Player player, @NotNull Collection<String> permissions) {
        plugin.debugMessage("[PermissionAPI] hasAllGrantedPermissions(Player, Collection) called. Player=" + player.getName()
                + " | PermissionsCount=" + (permissions.isEmpty() ? "null" : permissions.size()), 4);

        boolean result = plugin.getPermissionManager().hasAllGrantedPermissions(player, permissions);

        plugin.debugMessage("[PermissionAPI] hasAllGrantedPermissions(Player, Collection) result. Player=" + player.getName()
                + " | Result=" + result, 4);

        return result;
    }

    /**
     * Returns the provider currently being used for permission checks.
     *
     * @return provider name (LuckPerms, Vault, Bukkit)
     */
    public @NotNull String getActivePermissionProviderName() {
        plugin.debugMessage("[PermissionAPI] getActivePermissionProviderName() called.", 4);

        String provider = plugin.getPermissionManager().getActivePermissionProviderName();

        plugin.debugMessage("[PermissionAPI] getActivePermissionProviderName() result -> " + provider, 4);
        return provider;
    }

    /**
     * @return true if LuckPerms is being used for permission checks
     */
    public boolean isUsingLuckPerms() {
        plugin.debugMessage("[PermissionAPI] isUsingLuckPerms() called.", 4);

        boolean value = plugin.getPermissionManager().isUsingLuckPerms();

        plugin.debugMessage("[PermissionAPI] isUsingLuckPerms() result -> " + value, 4);
        return value;
    }

    /**
     * @return true if Vault permissions are being used for permission checks
     */
    public boolean isUsingVaultPermissions() {
        plugin.debugMessage("[PermissionAPI] isUsingVaultPermissions() called.", 4);

        boolean value = plugin.getPermissionManager().isUsingVaultPermissions();

        plugin.debugMessage("[PermissionAPI] isUsingVaultPermissions() result -> " + value, 4);
        return value;
    }

    /**
     * Formats an {@link OfflinePlayer} for debug logs.
     *
     * <p>This avoids null names from {@link OfflinePlayer#getName()} (for players that have never joined or
     * where the server has no cached name data).</p>
     *
     * @param offlinePlayer offline player to format
     * @return a debug-friendly name, never null
     */
    private @NotNull String nameOf(@NotNull OfflinePlayer offlinePlayer) {
        String name = offlinePlayer.getName();
        return name == null ? "unknown" : name;
    }
}