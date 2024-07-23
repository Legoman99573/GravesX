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

        if (plugin.getConfig().getBoolean("settings.update.check") && player.hasPermission("graves.update.notify")) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String latestVersion = plugin.getLatestVersion();

                if (latestVersion != null) {
                    try {
                        double pluginVersion = Double.parseDouble(plugin.getVersion());
                        double pluginVersionLatest = Double.parseDouble(latestVersion);
                        int comparisonResult = compareVersions(String.valueOf(pluginVersion), String.valueOf(pluginVersionLatest));

                        if (comparisonResult > 0) {
                            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Outdated version detected " + pluginVersion
                                    + ", latest version is " + pluginVersionLatest
                                    + ", https://www.spigotmc.org/resources/" + plugin.getSpigotID() + "/");
                        } else if (comparisonResult < 0) {
                            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Development version detected " + pluginVersion
                                    + ", Report any bugs to https://discord.ranull.com/");
                        }
                    } catch (NumberFormatException exception) {
                        if (!plugin.getVersion().equalsIgnoreCase(latestVersion) && !latestVersion.equalsIgnoreCase("4.9")) {
                            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Outdated version detected " + plugin.getVersion()
                                    + ", latest version is " + latestVersion + ", https://www.spigotmc.org/resources/"
                                    + plugin.getSpigotID() + "/");
                        }
                    }
                }
            });
        }
    }

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
}