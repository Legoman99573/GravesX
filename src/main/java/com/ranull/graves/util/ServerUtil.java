package com.ranull.graves.util;

import com.ranull.graves.Graves;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
        stringList.add("Java Information:");
        stringList.add("Java Version: " + getSystemProperty("java.version"));
        stringList.add("Java Vendor: " + getSystemProperty("java.vendor"));
        stringList.add("Java Vendor URL: " + getSystemProperty("java.vendor.url"));
        stringList.add("Java Home: " + getSystemProperty("java.home"));
        stringList.add("Java VM Specification Version: " + getSystemProperty("java.vm.specification.version"));
        stringList.add("Java VM Specification Vendor: " + getSystemProperty("java.vm.specification.vendor"));
        stringList.add("Java VM Specification Name: " + getSystemProperty("java.vm.specification.name"));
        stringList.add("Java VM Version: " + getSystemProperty("java.vm.version"));
        stringList.add("Java VM Vendor: " + getSystemProperty("java.vm.vendor"));
        stringList.add("Java VM Name: " + getSystemProperty("java.vm.name"));
        stringList.add("");

        stringList.add("Operating System Information:");
        stringList.add("OS Name: " + getOsName());
        stringList.add("OS Version: " + getSystemProperty("os.version"));
        stringList.add("OS Architecture: " + getSystemProperty("os.arch"));
        stringList.add("User Name: " + getSystemProperty("user.name"));
        stringList.add("User Home: " + getSystemProperty("user.home"));
        stringList.add("User Directory: " + getSystemProperty("user.dir"));
        // Check for top-level access
        if (isRunningAsRoot()) {
            stringList.add("WARNING: This " + plugin.getServer().getName() + " server is running with top-level access (root/administrator)");
            plugin.getLogger().warning("This server is running with top-level access (root/administrator), which is unsafe and can lead to security vulnerabilities. We recommend creating a user account or running the server in a rootless Docker container.");
        }
        stringList.add("");

        stringList.add("System RAM Information:");
        Runtime runtime = Runtime.getRuntime();
        stringList.add("Max Memory: " + formatBytes(runtime.maxMemory()));
        stringList.add("Total Memory: " + formatBytes(runtime.totalMemory()));
        stringList.add("Free Memory: " + formatBytes(runtime.freeMemory()));
        stringList.add("Used Memory: " + formatBytes(runtime.totalMemory() - runtime.freeMemory()));
        stringList.add("");

        stringList.add("System Disk Space Information:");
        File root = new File("/");
        stringList.add("Total Space: " + formatBytes(root.getTotalSpace()));
        stringList.add("Free Space: " + formatBytes(root.getFreeSpace()));
        stringList.add("Usable Space: " + formatBytes(root.getUsableSpace()));
        stringList.add("");

        stringList.add("Minecraft Server Information:");
        stringList.add("Implementation Name: " + plugin.getServer().getName());
        stringList.add("Implementation Version: " + plugin.getServer().getVersion());
        stringList.add("Bukkit Version: " + plugin.getServer().getBukkitVersion());
        try {
            stringList.add("NMS Version: " + plugin.getServer().getClass().getPackage().getName().split("\\.")[3]);

        } catch (Exception e) {
            stringList.add("NMS Version: " + Bukkit.getServer().getVersion());
        }
        stringList.add("Player Count: " + plugin.getServer().getOnlinePlayers().size());
        stringList.add("Player List: " + plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.joining(", ")));
        stringList.add("Plugin Count: " + plugin.getServer().getPluginManager().getPlugins().length);
        stringList.add("Plugin List: " + Arrays.stream(plugin.getServer().getPluginManager().getPlugins())
                .map(Plugin::getName)
                .collect(Collectors.joining(", ")));
        stringList.add("");

        stringList.add("Graves Information:");
        // Add plugin-specific information
        stringList.add(plugin.getDescription().getName() + " Version: "
                + plugin.getDescription().getVersion());

        if (plugin.getVersionManager().hasAPIVersion()) {
            stringList.add(plugin.getDescription().getName() + " API Version: "
                    + plugin.getDescription().getAPIVersion());
        }
        stringList.add(plugin.getDescription().getName() + " Database Type: " + plugin.getConfig().getString("settings.storage.type", "SQLITE").toUpperCase());
        if (plugin.getIntegrationManager().hasVaultPermProvider()) {
            stringList.add(plugin.getDescription().getName() + " Permissions Provider: Vault");
        } else {
            stringList.add(plugin.getDescription().getName() + " Permissions Provider: Bukkit");
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
        String password2 = config.getString("settings.storage.postgresql.password", "");
        if (!password.isEmpty()) {
            String maskedPassword = password2.replaceAll(".", "*");
            configString = configString.replace(password2, maskedPassword);
        }
        String password3 = config.getString("settings.storage.h2.password", "");
        if (!password.isEmpty()) {
            String maskedPassword = password3.replaceAll(".", "*");
            configString = configString.replace(password3, maskedPassword);
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
        String pre = ("KMGTPE").charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static String getSystemProperty(String key) {
        String value = System.getProperty(key);
        return value != null ? value : "Unknown";
    }

    private static boolean isRunningAsRoot() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return isRunningAsWindowsAdmin();
        } else {
            return isRunningAsUnixRoot();
        }
    }

    private static boolean isRunningAsWindowsAdmin() {
        try {
            Process process = Runtime.getRuntime().exec("net session");
            process.getOutputStream().close();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isRunningAsUnixRoot() {
        return "0".equals(System.getProperty("user.name"));
    }

    /**
     * Gets the detailed OS name from the system files.
     *
     * @return A string with the OS name.
     */
    private static String getOsName() {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("linux")) {
            return getLinuxOsName();
        }
        return osName;
    }

    /**
     * Reads the OS name from the /etc/os-release file on Unix-like systems.
     *
     * @return A string with the OS name.
     */
    private static String getLinuxOsName() {
        String osReleaseFile = "/etc/os-release";
        try (BufferedReader reader = new BufferedReader(new FileReader(osReleaseFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("PRETTY_NAME=")) {
                    return line.split("=")[1].replaceAll("\"", "");
                }
            }
        } catch (IOException meh) {
            // Just return as Linux. No need to error out.
        }
        return "Linux";
    }
}