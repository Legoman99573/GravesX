package com.ranull.graves.util;

import com.ranull.graves.Graves;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for gathering server information and generating server dumps.
 */
public final class ServerUtil {

    /**
     * Gathers server information and generates a dump in string format.
     *
     * @param plugin The Graves plugin instance.
     * @return A string containing the server dump information.
     */
    public static String getServerDumpInfo(Graves plugin) {
        List<String> stringList = new ArrayList<>();

        // Add basic server information
        stringList.add("Implementation Name: " + plugin.getServer().getName());
        stringList.add("Implementation Version: " + plugin.getServer().getVersion());
        stringList.add("Bukkit Version: " + plugin.getServer().getBukkitVersion());
        try {
            stringList.add("NMS Version: " + plugin.getServer().getClass().getPackage().getName().split("\\.")[3]);

        } catch (Exception e) {
            stringList.add("NMS Version: " + Bukkit.getServer().getVersion());
        }
        stringList.add("Java Version: " + System.getProperty("java.version"));
        stringList.add("OS Name: " + System.getProperty("os.name"));
        stringList.add("OS Version: " + System.getProperty("os.version"));

        Runtime runtime = Runtime.getRuntime();
        stringList.add("Max Memory (MB): " + (runtime.maxMemory() / (1024 * 1024)));
        stringList.add("Total Memory (MB): " + (runtime.totalMemory() / (1024 * 1024)));
        stringList.add("Free Memory (MB): " + (runtime.freeMemory() / (1024 * 1024)));
        stringList.add("Used Memory (MB): " + ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)));

        // Add storage space statistics
        File root = new File("/");
        stringList.add("Total Space: " + formatBytes(root.getTotalSpace()));
        stringList.add("Free Space: " + formatBytes(root.getFreeSpace()));
        stringList.add("Usable Space: " + formatBytes(root.getUsableSpace()));

        stringList.add("Database Type: " + plugin.getConfig().getString("settings.storage.type", "SQLITE").toUpperCase());
        stringList.add("Player Count: " + plugin.getServer().getOnlinePlayers().size());
        stringList.add("Player List: " + plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.joining(", ")));
        stringList.add("Plugin Count: " + plugin.getServer().getPluginManager().getPlugins().length);
        stringList.add("Plugin List: " + Arrays.stream(plugin.getServer().getPluginManager().getPlugins())
                .map(Plugin::getName)
                .collect(Collectors.joining(", ")));

        // Add plugin-specific information
        stringList.add(plugin.getDescription().getName() + " Version: "
                + plugin.getDescription().getVersion());

        if (plugin.getVersionManager().hasAPIVersion()) {
            stringList.add(plugin.getDescription().getName() + " API Version: "
                    + plugin.getDescription().getAPIVersion());
        }

        stringList.add(plugin.getDescription().getName() + " Config Version: "
                + plugin.getConfig().getInt("config-version"));

        // Replace the password in the config string and encode it back to Base64
        FileConfiguration config = plugin.getConfig();
        String configString = config.saveToString();
        String password = config.getString("settings.storage.mysql.password", "");
        if (!password.isEmpty()) {
            String maskedPassword = password.replaceAll(".", "*");
            configString = configString.replace(password, maskedPassword);
        }
        String configBase64 = Base64.getEncoder().encodeToString(configString.getBytes());
        stringList.add(plugin.getDescription().getName() + " Config Base64: " + configBase64);

        // Join all information into a single string separated by new lines
        return String.join("\n", stringList);
    }

    /**
     * Formats a byte count into a human-readable string with appropriate units.
     *
     * @param bytes The number of bytes.
     * @return A string with the byte count formatted in B, KB, MB, GB, TB, or PB.
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = ("KMGTPE").charAt(exp-1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}