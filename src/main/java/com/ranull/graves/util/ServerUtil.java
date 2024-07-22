package com.ranull.graves.util;

import com.ranull.graves.Graves;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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
        stringList.add("NMS Version: " + plugin.getServer().getClass().getPackage().getName().split("\\.")[3]);
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
        stringList.add(plugin.getDescription().getName() + " Config Base64: "
                + Base64.getEncoder().encodeToString(plugin.getConfig().saveToString().getBytes()));

        // Join all information into a single string separated by new lines
        return String.join("\n", stringList);
    }
}