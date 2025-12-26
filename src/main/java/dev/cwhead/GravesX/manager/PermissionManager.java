package dev.cwhead.GravesX.manager;

import com.ranull.graves.Graves;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Objects;

/**
 * Manages permission checks for GravesX.
 */
public class PermissionManager {

    private final Graves plugin;

    /**
     * Creates a new permission manager.
     *
     * @param plugin the GravesX plugin instance
     */
    public PermissionManager(Graves plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Checks if the specified player has been granted the specified permission.
     *
     * <p>This uses the best available permissions provider in this order:</p>
     * <ul>
     *   <li>LuckPerms (if available)</li>
     *   <li>Vault permissions (if available)</li>
     *   <li>Bukkit's built-in {@link Player#hasPermission(String)} fallback</li>
     * </ul>
     *
     * <p>A debug line is emitted indicating which provider was used and the result.</p>
     *
     * @param permission the permission to check for
     * @param player the player whose permissions are being checked
     * @return {@code true} if the player has the specified permission, {@code false} otherwise
     */
    public boolean hasGrantedPermission(String permission, Player player) {
        plugin.debugMessage("[PermissionManager] hasGrantedPermission(String, Player) called. Player=" + nameOf(player)
                + " | Permission=" + String.valueOf(permission), 4);

        if (player == null) {
            plugin.debugMessage("[PermissionManager] hasGrantedPermission(String, Player) failed: player is null. Returning false.", 4);
            return false;
        }

        boolean result = checkPermission(permission, player);

        plugin.debugMessage("[PermissionManager] hasGrantedPermission(String, Player) result. Player=" + nameOf(player)
                + " | Permission=" + String.valueOf(permission) + " | Result=" + result, 4);

        return result;
    }

    /**
     * Checks if the specified offline player has been granted the specified permission.
     *
     * <p><strong>Note:</strong> Permission checks are only reliably available for online players. Many permission
     * implementations (including Bukkit's built-in permissions) evaluate permissions against an active {@link Player}
     * context, and offline permission lookups may be unsupported, incomplete, or require extra provider-specific work.</p>
     *
     * @param permission the permission to check for
     * @param offlinePlayer the offline player whose permissions are being checked
     * @return {@code true} if the offline player has the specified permission, {@code false} otherwise
     * @deprecated This method is deprecated because permission state is not reliably available for offline players and may
     * return false even when the player would have the permission while online (depending on the active permissions
     * provider). Prefer checking permissions on an online {@link Player} using
     * {@link #hasGrantedPermission(String, Player)}.
     */
    @Deprecated
    public boolean hasGrantedPermission(String permission, OfflinePlayer offlinePlayer) {
        plugin.debugMessage("[PermissionManager] hasGrantedPermission(String, OfflinePlayer) called. OfflinePlayer=" + nameOf(offlinePlayer)
                + " | Permission=" + String.valueOf(permission), 4);

        if (offlinePlayer == null) {
            plugin.debugMessage("[PermissionManager] hasGrantedPermission(String, OfflinePlayer) failed: offlinePlayer is null. Returning false.", 4);
            return false;
        }

        if (offlinePlayer.isOnline()) {
            Player online = offlinePlayer.getPlayer();
            plugin.debugMessage("[PermissionManager] OfflinePlayer is online. Attempting Player path. OfflinePlayer="
                    + nameOf(offlinePlayer) + " | Player=" + nameOf(online), 4);

            if (online != null) {
                boolean result = checkPermission(permission, online);
                plugin.debugMessage("[PermissionManager] hasGrantedPermission(String, OfflinePlayer) (online->player) result. OfflinePlayer="
                        + nameOf(offlinePlayer) + " | Permission=" + String.valueOf(permission) + " | Result=" + result, 4);
                return result;
            }

            plugin.debugMessage("[PermissionManager] OfflinePlayer was marked online but getPlayer() returned null. Falling back to OfflinePlayer provider path.", 4);
        }

        boolean result = checkPermission(permission, offlinePlayer);

        plugin.debugMessage("[PermissionManager] hasGrantedPermission(String, OfflinePlayer) result. OfflinePlayer=" + nameOf(offlinePlayer)
                + " | Permission=" + String.valueOf(permission) + " | Result=" + result, 4);

        return result;
    }

    /**
     * Returns true if the player has the permission OR is op.
     *
     * @param permission permission to check
     * @param player player to check
     * @return true if granted or op
     */
    public boolean hasGrantedPermissionOrOp(String permission, Player player) {
        plugin.debugMessage("[PermissionManager] hasGrantedPermissionOrOp(String, Player) called. Player=" + nameOf(player)
                + " | Permission=" + permission, 4);

        if (player == null) {
            plugin.debugMessage("[PermissionManager] hasGrantedPermissionOrOp(String, Player) failed: player is null. Returning false.", 4);
            return false;
        }

        boolean isOp = player.isOp();
        plugin.debugMessage("[PermissionManager] hasGrantedPermissionOrOp(String, Player) player.isOp()=" + isOp
                + " | Player=" + nameOf(player), 4);

        boolean result = isOp || hasGrantedPermission(permission, player);

        plugin.debugMessage("[PermissionManager] hasGrantedPermissionOrOp(String, Player) result. Player=" + nameOf(player)
                + " | Permission=" + permission + " | Result=" + result, 4);

        return result;
    }

    /**
     * Returns true if the player has at least one of the provided permissions.
     *
     * @param player player to check
     * @param permissions permissions to test
     * @return true if any permission is granted
     */
    public boolean hasAnyGrantedPermission(Player player, String... permissions) {
        plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, String...) called. Player=" + nameOf(player)
                + " | PermissionsCount=" + (permissions == null ? "null" : permissions.length), 4);

        if (player == null) {
            plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, String...) failed: player is null. Returning false.", 4);
            return false;
        }
        if (permissions == null || permissions.length == 0) {
            plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, String...) failed: permissions is null/empty. Returning false.", 4);
            return false;
        }

        for (String perm : permissions) {
            boolean granted = hasGrantedPermission(perm, player);
            plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, String...) checked. Player=" + nameOf(player)
                    + " | Permission=" + perm + " | Granted=" + granted, 4);
            if (granted) {
                plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, String...) result: true (matched). Player=" + nameOf(player), 4);
                return true;
            }
        }

        plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, String...) result: false (no matches). Player=" + nameOf(player), 4);
        return false;
    }

    /**
     * Returns true if the player has all the provided permissions.
     *
     * @param player player to check
     * @param permissions permissions to test
     * @return true if all permissions are granted
     */
    public boolean hasAllGrantedPermissions(Player player, String... permissions) {
        plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, String...) called. Player=" + nameOf(player)
                + " | PermissionsCount=" + (permissions == null ? "null" : permissions.length), 4);

        if (player == null) {
            plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, String...) failed: player is null. Returning false.", 4);
            return false;
        }
        if (permissions == null || permissions.length == 0) {
            plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, String...) failed: permissions is null/empty. Returning false.", 4);
            return false;
        }

        for (String perm : permissions) {
            boolean granted = hasGrantedPermission(perm, player);
            plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, String...) checked. Player=" + nameOf(player)
                    + " | Permission=" + perm + " | Granted=" + granted, 4);
            if (!granted) {
                plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, String...) result: false (missing). Player=" + nameOf(player), 4);
                return false;
            }
        }

        plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, String...) result: true (all granted). Player=" + nameOf(player), 4);
        return true;
    }

    /**
     * Returns true if the player has at least one of the provided permissions.
     *
     * @param player player to check
     * @param permissions permissions to test
     * @return true if any permission is granted
     */
    public boolean hasAnyGrantedPermission(Player player, Collection<String> permissions) {
        plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, Collection) called. Player=" + nameOf(player)
                + " | PermissionsCount=" + (permissions == null ? "null" : permissions.size()), 4);

        if (player == null) {
            plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, Collection) failed: player is null. Returning false.", 4);
            return false;
        }
        if (permissions == null || permissions.isEmpty()) {
            plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, Collection) failed: permissions is null/empty. Returning false.", 4);
            return false;
        }

        for (String perm : permissions) {
            boolean granted = hasGrantedPermission(perm, player);
            plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, Collection) checked. Player=" + nameOf(player)
                    + " | Permission=" + String.valueOf(perm) + " | Granted=" + granted, 4);
            if (granted) {
                plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, Collection) result: true (matched). Player=" + nameOf(player), 4);
                return true;
            }
        }

        plugin.debugMessage("[PermissionManager] hasAnyGrantedPermission(Player, Collection) result: false (no matches). Player=" + nameOf(player), 4);
        return false;
    }

    /**
     * Returns true if the player has all the provided permissions.
     *
     * @param player player to check
     * @param permissions permissions to test
     * @return true if all permissions are granted
     */
    public boolean hasAllGrantedPermissions(Player player, Collection<String> permissions) {
        plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, Collection) called. Player=" + nameOf(player)
                + " | PermissionsCount=" + (permissions == null ? "null" : permissions.size()), 4);

        if (player == null) {
            plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, Collection) failed: player is null. Returning false.", 4);
            return false;
        }
        if (permissions == null || permissions.isEmpty()) {
            plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, Collection) failed: permissions is null/empty. Returning false.", 4);
            return false;
        }

        for (String perm : permissions) {
            boolean granted = hasGrantedPermission(perm, player);
            plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, Collection) checked. Player=" + nameOf(player)
                    + " | Permission=" + String.valueOf(perm) + " | Granted=" + granted, 4);
            if (!granted) {
                plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, Collection) result: false (missing). Player=" + nameOf(player), 4);
                return false;
            }
        }

        plugin.debugMessage("[PermissionManager] hasAllGrantedPermissions(Player, Collection) result: true (all granted). Player=" + nameOf(player), 4);
        return true;
    }

    /**
     * Returns the name of the active permission provider used for checks.
     *
     * @return provider name (LuckPerms, Vault, Bukkit)
     */
    public String getActivePermissionProviderName() {
        String provider;
        if (plugin.getIntegrationManager().hasLuckPermsHandler()) {
            provider = "LuckPerms";
        } else if (plugin.getIntegrationManager().hasVault() || plugin.getIntegrationManager().hasVaultPermProvider()) {
            provider = "Vault";
        } else {
            provider = "Bukkit";
        }

        plugin.debugMessage("[PermissionManager] getActivePermissionProviderName() -> " + provider, 4);
        return provider;
    }

    /**
     * @return true if LuckPerms is being used for permission checks
     */
    public boolean isUsingLuckPerms() {
        boolean value = plugin.getIntegrationManager().hasLuckPermsHandler();
        plugin.debugMessage("[PermissionManager] isUsingLuckPerms() -> " + value, 4);
        return value;
    }

    /**
     * @return true if Vault permissions are being used for permission checks
     */
    public boolean isUsingVaultPermissions() {
        boolean value = plugin.getIntegrationManager().hasVault() || plugin.getIntegrationManager().hasVaultPermProvider();
        plugin.debugMessage("[PermissionManager] isUsingVaultPermissions() -> " + value, 4);
        return value;
    }

    /**
     * Performs an online permission lookup using the best available provider.
     *
     * <p>This method is the internal implementation behind the public {@code hasGrantedPermission(...)} methods.
     * It normalizes the permission string (trimming and rejecting blank values), then checks it using the first
     * available provider in this order:</p>
     *
     * <ul>
     *   <li>LuckPerms (if available)</li>
     *   <li>Vault permissions (if available)</li>
     *   <li>Bukkit {@link Player#hasPermission(String)} fallback</li>
     * </ul>
     *
     * <p>Debug output is emitted for:</p>
     * <ul>
     *   <li>entry + arguments</li>
     *   <li>normalization failures</li>
     *   <li>provider used</li>
     *   <li>final result</li>
     * </ul>
     *
     * @param permission the permission node to check (may be null/blank)
     * @param player the online player context to evaluate permissions against
     * @return {@code true} if granted by the active provider, otherwise {@code false}
     */
    private boolean checkPermission(String permission, Player player) {
        plugin.debugMessage("[PermissionManager] checkPermission(String, Player) called. Player=" + nameOf(player)
                + " | Permission=" + permission, 4);

        String perm = normalizePermission(permission);
        if (perm == null) {
            plugin.debugMessage("[PermissionManager] checkPermission(String, Player) failed: permission is null/blank. Player="
                    + nameOf(player) + " | Returning false.", 4);
            return false;
        }

        boolean hasPermission;
        String provider;

        if (isUsingLuckPerms()) {
            provider = "LuckPerms";
            hasPermission = plugin.getIntegrationManager().getLuckPermsHandler().hasPermission(player, perm);
            plugin.debugMessage("[LuckPerms] Player: " + player.getName() + " | Permission: " + perm + " | Has Permission: " + hasPermission, 4);
        } else if (isUsingVaultPermissions()) {
            provider = "Vault";
            hasPermission = plugin.getIntegrationManager().getVault().hasPermission(player, perm);
            plugin.debugMessage("[Vault] Player: " + player.getName() + " | Permission: " + perm + " | Has Permission: " + hasPermission, 4);
        } else {
            provider = "Bukkit";
            hasPermission = player.hasPermission(perm);
            plugin.debugMessage("[Bukkit] Player: " + player.getName() + " | Permission: " + perm + " | Has Permission: " + hasPermission, 4);
        }

        plugin.debugMessage("[PermissionManager] checkPermission(String, Player) result. Player=" + nameOf(player)
                + " | Permission=" + perm + " | Provider=" + provider + " | Result=" + hasPermission, 4);

        return hasPermission;
    }

    /**
     * Performs an offline permission lookup using the best available provider.
     *
     * <p>This is primarily intended for providers that explicitly support offline permission queries
     * (e.g., LuckPerms, some Vault-backed permission implementations).</p>
     *
     * <p><strong>Important:</strong> If no offline-capable provider is available, this method cannot reliably
     * determine permissions and will default to {@code false} (with debug output explaining why).</p>
     *
     * <p>Debug output is emitted for:</p>
     * <ul>
     *   <li>entry + arguments</li>
     *   <li>normalization failures</li>
     *   <li>provider used (or fallback behavior)</li>
     *   <li>final result</li>
     * </ul>
     *
     * @param permission the permission node to check (may be null/blank)
     * @param offlinePlayer the offline player whose permission is being checked
     * @return {@code true} if granted by an offline-capable provider, otherwise {@code false}
     */
    private boolean checkPermission(String permission, OfflinePlayer offlinePlayer) {
        plugin.debugMessage("[PermissionManager] checkPermission(String, OfflinePlayer) called. OfflinePlayer=" + nameOf(offlinePlayer)
                + " | Permission=" + String.valueOf(permission), 4);

        String perm = normalizePermission(permission);
        if (perm == null) {
            plugin.debugMessage("[PermissionManager] checkPermission(String, OfflinePlayer) failed: permission is null/blank. OfflinePlayer="
                    + nameOf(offlinePlayer) + " | Returning false.", 4);
            return false;
        }

        boolean hasPermission;
        String provider;

        if (isUsingLuckPerms()) {
            provider = "LuckPerms";
            hasPermission = plugin.getIntegrationManager().getLuckPermsHandler().hasPermission(offlinePlayer, perm);
            plugin.debugMessage("[LuckPerms] Offline Player: " + safeName(offlinePlayer) + " | Permission: " + perm + " | Has Permission: " + hasPermission, 4);
        } else if (isUsingVaultPermissions()) {
            provider = "Vault";
            hasPermission = plugin.getIntegrationManager().getVault().hasPermission(offlinePlayer, perm);
            plugin.debugMessage("[Vault] Offline Player: " + safeName(offlinePlayer) + " | Permission: " + perm + " | Has Permission: " + hasPermission, 4);
        } else {
            provider = "Bukkit";
            hasPermission = false;
            plugin.debugMessage("[Bukkit] Failed to get offline player permission provider. Assuming player doesn't have permission.", 4);
            plugin.debugMessage("[Bukkit] Offline Player: " + safeName(offlinePlayer) + " | Permission: " + perm + " | Has Permission: " + hasPermission, 4);
        }

        plugin.debugMessage("[PermissionManager] checkPermission(String, OfflinePlayer) result. OfflinePlayer=" + nameOf(offlinePlayer)
                + " | Permission=" + perm + " | Provider=" + provider + " | Result=" + hasPermission, 4);

        return hasPermission;
    }

    /**
     * Normalizes a permission node for comparison.
     *
     * <p>This trims leading/trailing whitespace and rejects null/blank strings.
     * Returning {@code null} indicates an invalid permission input and callers should treat it as "not granted".</p>
     *
     * @param permission raw permission node
     * @return the trimmed permission node, or {@code null} if null/blank
     */
    private String normalizePermission(String permission) {
        if (permission == null) {
            plugin.debugMessage("[PermissionManager] normalizePermission(String) -> null (permission is null)", 4);
            return null;
        }

        String perm = permission.trim();
        if (perm.isEmpty()) {
            plugin.debugMessage("[PermissionManager] normalizePermission(String) -> null (permission is blank)", 4);
            return null;
        }

        plugin.debugMessage("[PermissionManager] normalizePermission(String) -> " + perm, 4);
        return perm;
    }

    /**
     * Formats a {@link Player} for debug logs.
     *
     * <p>Includes name and UUID when available, and returns {@code "null"} if the player reference is null.</p>
     *
     * @param player the player instance
     * @return a debug-friendly string for logs
     */
    private String nameOf(Player player) {
        return player == null ? "null" : (player.getName() + " (" + player.getUniqueId() + ")");
    }

    /**
     * Formats an {@link OfflinePlayer} for debug logs.
     *
     * <p>Uses {@link #safeName(OfflinePlayer)} to avoid null player names, and returns {@code "null"} if
     * the offline player reference is null.</p>
     *
     * @param offlinePlayer the offline player instance
     * @return a debug-friendly string for logs
     */
    private String nameOf(OfflinePlayer offlinePlayer) {
        return offlinePlayer == null ? "null" : (safeName(offlinePlayer) + " (" + offlinePlayer.getUniqueId() + ")");
    }

    /**
     * Returns a safe display name for an {@link OfflinePlayer}.
     *
     * <p>{@link OfflinePlayer#getName()} can be null (for example if the player has never joined, or the server
     * does not have cached name data). This helper ensures log output remains readable.</p>
     *
     * @param offlinePlayer the offline player
     * @return the player's name, {@code "unknown"} if name is null, or {@code "null"} if the reference is null
     */
    private String safeName(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            return "null";
        }
        String name = offlinePlayer.getName();
        return name == null ? "unknown" : name;
    }
}
