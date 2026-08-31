package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.util.StringUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

/**
 * Listener for handling PlayerJoinEvent to notify players about plugin updates.
 */
public class PlayerJoinListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a PlayerJoinListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerJoinListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerJoinEvent to notify players about available plugin updates.
     *
     * @param event The PlayerJoinEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (shouldCheckForUpdates(player)) {
            plugin.getSchedulerManager().runTaskAsynchronously(
                    () -> notifyPlayerIfOutdated(player)
            );
        }
    }

    /**
     * Checks if updates should be checked for the player.
     *
     * @param player The player to check.
     * @return True if updates should be checked, false otherwise.
     */
    private boolean shouldCheckForUpdates(Player player) {
        return plugin.getConfig().getBoolean("settings.update.check")
                && plugin.getPermissionManager()
                .hasGrantedPermission("graves.update.notify", player);
    }

    /**
     * Notifies the player about the current plugin version status.
     *
     * @param player The player to notify.
     */
    private void notifyPlayerIfOutdated(Player player) {
        String latestVersion = plugin.getLatestVersion();
        String installedVersion = plugin.getDescription().getVersion();

        if (latestVersion == null || latestVersion.isBlank()) {
            plugin.getLogger().warning(
                    "Unable to check for a GravesX update: latest version is unavailable."
            );
            return;
        }

        if (installedVersion == null || installedVersion.isBlank()) {
            plugin.getLogger().warning(
                    "Unable to check for a GravesX update: installed version is unavailable."
            );
            return;
        }

        String prefix = plugin.getConfigManager()
                .getConfigSection("message.prefix", player)
                .getString("message.prefix");

        if (prefix == null) {
            prefix = "";
        }

        final int comparisonResult;

        try {
            comparisonResult = compareVersions(installedVersion, latestVersion);
        } catch (NumberFormatException exception) {
            plugin.getLogger().warning(
                    "Unable to compare GravesX versions: installed='"
                            + installedVersion
                            + "', latest='"
                            + latestVersion
                            + "'."
            );
            return;
        }

        if (comparisonResult < 0) {
            sendVersionMessage(
                    player,
                    prefix,
                    "message.grave-plugin-version-outdated",
                    latestVersion
            );
        } else if (comparisonResult > 0) {
            sendVersionMessage(
                    player,
                    prefix,
                    "message.grave-plugin-version-development",
                    latestVersion
            );
        } else {
            sendVersionMessage(
                    player,
                    prefix,
                    "message.grave-plugin-version-latest",
                    null
            );
        }
    }

    /**
     * Sends a configured version message to a player.
     *
     * @param player The player receiving the message.
     * @param prefix The configured message prefix.
     * @param path The configuration path.
     * @param latestVersion The latest public version, if applicable.
     */
    private void sendVersionMessage(
            Player player,
            String prefix,
            String path,
            String latestVersion
    ) {
        var configSection = plugin.getConfigManager()
                .getConfigSection(path, player);

        if (configSection == null) {
            return;
        }

        if (path.endsWith("outdated") || path.endsWith("development")) {
            List<String> messages = configSection.getStringList(path);

            for (String message : messages) {
                if (latestVersion != null) {
                    message = message.replace("%public-version", latestVersion);
                }

                sendMessage(player, prefix + message);
            }
        } else {
            String message = configSection.getString(path);

            if (message != null) {
                sendMessage(player, prefix + message);
            }
        }
    }

    /**
     * Sends a message using MiniMessage when available.
     *
     * @param player The player receiving the message.
     * @param message The message to send.
     */
    private void sendMessage(Player player, String message) {
        String parsed = StringUtil.parseString(message, player, plugin);

        if (plugin.getIntegrationManager().hasMiniMessage()) {
            player.sendMessage(MiniMessage.parseString(parsed));
        } else {
            player.sendMessage(parsed);
        }
    }

    /**
     * Compares two GravesX version strings.
     *
     * @param version1 The first version string.
     * @param version2 The second version string.
     * @return A negative integer, zero, or a positive integer as the first
     * version is less than, equal to, or greater than the second version.
     */
    private int compareVersions(String version1, String version2) {
        if (version1 == null || version2 == null) {
            throw new NumberFormatException("Version cannot be null");
        }

        version1 = version1.trim();
        version2 = version2.trim();

        if (version1.isEmpty() || version2.isEmpty()) {
            throw new NumberFormatException("Version cannot be empty");
        }

        Version first = parseVersion(version1);
        Version second = parseVersion(version2);

        int length = Math.max(first.parts.length, second.parts.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < first.parts.length ? first.parts[i] : 0;
            int v2 = i < second.parts.length ? second.parts[i] : 0;

            if (v1 < v2) {
                return -1;
            }

            if (v1 > v2) {
                return 1;
            }
        }

        if (first.development != second.development) {
            return first.development ? -1 : 1;
        }

        if (first.development) {
            return Integer.compare(first.build, second.build);
        }

        return 0;
    }

    /**
     * Parses a GravesX version string.
     *
     * @param version The version string.
     * @return Parsed version information.
     */
    private Version parseVersion(String version) {
        String baseVersion = version;
        int build = 0;
        boolean development = false;

        int buildIndex = version.indexOf("-build");

        if (buildIndex >= 0) {
            development = true;

            baseVersion = version.substring(0, buildIndex);

            String buildString = version.substring(
                    buildIndex + "-build".length()
            );

            if (buildString.isEmpty()) {
                throw new NumberFormatException(
                        "Development build number is missing: " + version
                );
            }

            build = Integer.parseInt(buildString);
        }

        if (baseVersion.isEmpty()) {
            throw new NumberFormatException(
                    "Base version is missing: " + version
            );
        }

        String[] parts = baseVersion.split("\\.");

        int[] numericParts = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isBlank()) {
                throw new NumberFormatException(
                        "Invalid version component: " + version
                );
            }

            numericParts[i] = Integer.parseInt(parts[i]);
        }

        return new Version(numericParts, development, build);
    }

    /**
     * Represents a parsed GravesX version.
     */
    private static final class Version {
        private final int[] parts;
        private final boolean development;
        private final int build;

        private Version(int[] parts, boolean development, int build) {
            this.parts = parts;
            this.development = development;
            this.build = build;
        }
    }
}