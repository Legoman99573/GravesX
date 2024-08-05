package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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
     * This method checks if the plugin's update check is enabled and if the player has the
     * permission to receive update notifications. If so, it runs an asynchronous task to
     * fetch the latest version of the plugin and compares it with the player's current version.
     *
     * If the player's version is outdated, a message is sent to the player indicating the
     * current version, the latest version, and a link to the Spigot resource page.
     *
     * The comparison is handled carefully to ensure proper handling of version format errors.
     *
     * @param event The PlayerJoinEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (shouldCheckForUpdates(player)) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String latestVersion = plugin.getLatestVersion();

                if (latestVersion != null) {
                    notifyPlayerIfOutdated(player, latestVersion);
                }
            });
        }
    }

    /**
     * Checks if updates should be checked for the player.
     *
     * @param player The player to check.
     * @return True if updates should be checked, false otherwise.
     */
    private boolean shouldCheckForUpdates(Player player) {
        return plugin.getConfig().getBoolean("settings.update.check") && plugin.hasGrantedPermission("graves.update.notify", player);
    }

    /**
     * Notifies the player if their plugin version is outdated.
     *
     * @param player         The player to notify.
     * @param latestVersion The latest version of the plugin.
     */
    private void notifyPlayerIfOutdated(Player player, String latestVersion) {
        try {
            double currentVersion = Double.parseDouble(plugin.getVersion());
            double newVersion = Double.parseDouble(latestVersion);
            int comparisonResult = compareVersions(String.valueOf(currentVersion), String.valueOf(newVersion));

            if (comparisonResult > 0) {
                sendOutdatedVersionMessage(player, currentVersion, newVersion);
            } else if (comparisonResult < 0) {
                sendDevelopmentVersionMessage(player, currentVersion);
            }
        } catch (NumberFormatException exception) {
            if (isDifferentVersion(plugin.getVersion(), latestVersion)) {
                sendOutdatedVersionMessage(player, plugin.getVersion(), latestVersion);
            }
        }
    }

    /**
     * Compares two version strings.
     *
     * @param version1 The first version string.
     * @param version2 The second version string.
     * @return A negative integer, zero, or a positive integer as the first version is less than, equal to, or greater than the second version.
     */
    private int compareVersions(String version1, String version2) {
        String[] levels1 = version1.split("\\.");
        String[] levels2 = version2.split("\\.");

        int length = Math.max(levels1.length, levels2.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < levels1.length ? Integer.parseInt(levels1[i]) : 0;
            int v2 = i < levels2.length ? Integer.parseInt(levels2[i]) : 0;
            if (v1 < v2) {
                return -1;
            }
            if (v1 > v2) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Sends a message to the player indicating that their plugin version is outdated.
     *
     * @param player         The player to notify.
     * @param currentVersion The current version of the plugin.
     * @param latestVersion  The latest version of the plugin.
     */
    private void sendOutdatedVersionMessage(Player player, double currentVersion, double latestVersion) {
        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                + "Outdated version detected " + currentVersion
                + ", latest version is " + latestVersion
                + ", https://www.spigotmc.org/resources/" + plugin.getSpigotID() + "/");
    }

    /**
     * Sends a message to the player indicating that they are using a development version of the plugin.
     *
     * @param player         The player to notify.
     * @param currentVersion The current version of the plugin.
     */
    private void sendDevelopmentVersionMessage(Player player, double currentVersion) {
        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                + "Development version detected " + currentVersion
                + ", Report any bugs to https://discord.ranull.com/");
    }

    /**
     * Sends a message to the player indicating that their plugin version is outdated.
     *
     * @param player         The player to notify.
     * @param currentVersion The current version of the plugin.
     * @param latestVersion  The latest version of the plugin.
     */
    private void sendOutdatedVersionMessage(Player player, String currentVersion, String latestVersion) {
        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                + "Outdated version detected " + currentVersion
                + ", latest version is " + latestVersion
                + ", https://www.spigotmc.org/resources/" + plugin.getSpigotID() + "/");
    }

    /**
     * Checks if the current version is different from the latest version.
     *
     * @param currentVersion The current version.
     * @param latestVersion  The latest version.
     * @return True if the versions are different, false otherwise.
     */
    private boolean isDifferentVersion(String currentVersion, String latestVersion) {
        return !currentVersion.equalsIgnoreCase(latestVersion) && !latestVersion.equalsIgnoreCase("4.9");
    }
}