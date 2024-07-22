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

                        if (pluginVersionLatest != 4.9 && pluginVersion < pluginVersionLatest) {
                            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Outdated version detected " + pluginVersion
                                    + ", latest version is " + pluginVersionLatest
                                    + ", https://www.spigotmc.org/resources/" + plugin.getSpigotID() + "/");
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
}