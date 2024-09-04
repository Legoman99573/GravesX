package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.StringUtil;
import org.bukkit.ChatColor;
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
                notifyPlayerIfOutdated(player);
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
        return plugin.getConfig().getBoolean("settings.update.check") && plugin.hasGrantedPermission("graves.update.notify", player.getPlayer());
    }

    /**
     * Notifies the player if their plugin version is outdated.
     *
     * @param player         The player to notify.
     */
    private void notifyPlayerIfOutdated(Player player) {
        String latestVersion = plugin.getLatestVersion();
        String installedVersion = plugin.getDescription().getVersion();
        String prefix = plugin.getConfig("message.prefix", player)
                .getString("message.prefix");
        try {
            int comparisonResult = compareVersions(installedVersion, latestVersion);
            // getLogger().info("Version Comparison Result: " + comparisonResult);

            if (comparisonResult < 0) {
                List<String> stringList = plugin.getConfig("message.grave-plugin-version-outdated", player)
                        .getStringList("message.grave-plugin-version-outdated");
                if (plugin.getIntegrationManager().hasMiniMessage()) {
                    for (String message : stringList) {
                        String toConvert = StringUtil.parseString(prefix + message, player.getPlayer(), plugin);
                        String newString = MiniMessage.parseString(toConvert);
                        player.sendMessage(newString);
                    }
                } else {
                    for (String message : stringList) {
                        player.sendMessage(StringUtil.parseString(prefix + message, player.getPlayer(), plugin));
                    }
                }
            } else if (comparisonResult > 0) {
                List<String> stringList = plugin.getConfig("message.grave-plugin-version-development", player)
                        .getStringList("message.grave-plugin-version-development");
                if (plugin.getIntegrationManager().hasMiniMessage()) {
                    for (String message : stringList) {
                        String toConvert = StringUtil.parseString(prefix + message, player.getPlayer(), plugin);
                        String newString = MiniMessage.parseString(toConvert);
                        player.sendMessage(newString);
                    }
                } else {
                    for (String message : stringList) {
                        player.sendMessage(StringUtil.parseString(prefix + message, player.getPlayer(), plugin));
                    }
                }
            } else {
                String string = plugin.getConfig("message.grave-plugin-version-latest", player)
                        .getString("message.grave-plugin-version-latest");
                if (plugin.getIntegrationManager().hasMiniMessage()) {
                        String toConvert = StringUtil.parseString(prefix + string, player.getPlayer(), plugin);
                        String newString = MiniMessage.parseString(toConvert);
                        player.sendMessage(newString);
                } else {
                    player.sendMessage(StringUtil.parseString(prefix + string, player.getPlayer(), plugin));
                }
            }
        } catch (NumberFormatException exception) {
            List<String> stringList = plugin.getConfig("message.grave-plugin-version-outdated", player)
                    .getStringList("message.grave-plugin-version-outdated");
            if (plugin.getIntegrationManager().hasMiniMessage()) {
                for (String message : stringList) {
                    String toConvert = StringUtil.parseString(prefix + message, player.getPlayer(), plugin);
                    String newString = MiniMessage.parseString(toConvert);
                    player.sendMessage(newString);
                }
            } else {
                for (String message : stringList) {
                    player.sendMessage(StringUtil.parseString(prefix + message, player.getPlayer(), plugin));
                }
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