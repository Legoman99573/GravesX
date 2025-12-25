package dev.cwhead.GravesX.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.util.UUIDUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Debug output manager (info/warn) for console + configured admins.
 *
 * <p>Severity:</p>
 * <ul>
 *   <li>0 = disabled (never prints)</li>
 *   <li>1 = info</li>
 *   <li>2 = warnings</li>
 * </ul>
 *
 * <p>Only one instance is active; creating a new one unloads the previous.</p>
 */
public final class DebugManager {

    /**
     * Tracks the active instance.
     */
    private static final AtomicReference<DebugManager> ACTIVE = new AtomicReference<>();

    /**
     * Internal packages (caller tagging only shows outside these).
     */
    private static final String[] INTERNAL_PACKAGES = new String[] {
            "com.ranull.graves.",
            "dev.cwhead.GravesX.",
            "me.jay.GravesX."
    };

    private final Graves plugin;
    private final AtomicBoolean unloaded = new AtomicBoolean(false);

    /**
     * Creates a new manager and unloads any previous active instance.
     *
     * @param plugin plugin instance
     */
    public DebugManager(Graves plugin) {
        this.plugin = plugin;

        DebugManager previous = ACTIVE.getAndSet(this);
        if (previous != null) {
            previous.unload();
        }
    }

    /**
     * Unloads this manager (disables output).
     */
    public void unload() {
        unloaded.set(true);
        ACTIVE.compareAndSet(this, null);
    }

    /**
     * @return true if unloaded
     */
    public boolean isUnloaded() {
        return unloaded.get();
    }

    /**
     * Checks if a severity should be printed.
     *
     * @param severity 0=disabled, 1=info, 2=warnings
     * @return true if allowed by config
     */
    public boolean isEnabled(int severity) {
        if (unloaded.get()) {
            return false;
        }

        // severity 0 (or anything not 1/2) never prints
        if (severity != 1 && severity != 2) {
            return false;
        }

        int level = plugin.getConfig().getInt("settings.debug.level", 0);

        // clamp to supported range 0..2
        if (level < 0) level = 0;
        if (level > 2) level = 2;

        // level=0 => print nothing, level=1 => only 1, level=2 => 1 and 2
        return level != 0 && severity <= level;
    }

    /**
     * Logs an info message (severity 1).
     *
     * @param message message
     */
    public void info(String message) {
        debug(message, 1, null);
    }

    /**
     * Logs a warning message (severity 2).
     *
     * @param message message
     */
    public void warn(String message) {
        debug(message, 2, null);
    }

    /**
     * Logs a message by severity.
     *
     * @param message message
     * @param severity 0=disabled, 1=info, 2=warnings
     */
    public void debug(String message, int severity) {
        debug(message, severity, null);
    }

    /**
     * Logs a message with an optional throwable.
     *
     * @param message message
     * @param severity 0=disabled, 1=info, 2=warnings
     * @param throwable optional throwable
     */
    public void debug(String message, int severity, Throwable throwable) {
        if (!isEnabled(severity)) {
            return;
        }

        boolean showCaller = plugin.getConfig().getBoolean("settings.debug.show-caller", true);
        boolean showCallerClass = plugin.getConfig().getBoolean("settings.debug.show-caller-class", false);

        CallerInfo caller = (showCaller || showCallerClass) ? resolveTriggeringPluginCaller(showCallerClass) : null;
        if (caller != null && !showCallerClass) {
            // Collapse to plugin-only when class details are disabled.
            caller = caller.toPluginOnly();
        }

        String consolePrefix = buildConsolePrefix(severity, caller);

        if (severity == 2) {
            plugin.getLogger().warning(consolePrefix + message);
        } else {
            plugin.getLogger().info(consolePrefix + message);
        }

        if (throwable != null) {
            plugin.getLogger().warning("Debug stacktrace:");
            plugin.logStackTrace(throwable);
        }

        List<String> admins = plugin.getConfig().getStringList("settings.debug.admin");
        if (admins.isEmpty()) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            sendToAdmins(admins, message, severity, caller);
        } else {
            CallerInfo finalCaller = caller;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!unloaded.get()) {
                    sendToAdmins(admins, message, severity, finalCaller);
                }
            });
        }
    }

    /**
     * Sends a message to online admins.
     *
     * @param admins admin identifiers
     * @param message message
     * @param severity 1=info, 2=warnings
     * @param caller caller tag (or null)
     */
    private void sendToAdmins(List<String> admins, String message, int severity, CallerInfo caller) {
        if (unloaded.get()) {
            return;
        }

        for (String admin : admins) {
            Player player = Bukkit.getPlayer(admin);

            UUID uuid = null;
            try {
                uuid = UUID.fromString(admin);
            } catch (IllegalArgumentException ignored) {
                // not a UUID string
            }

            if (uuid == null) {
                uuid = UUIDUtil.getUUID(admin);
            }

            if (uuid != null) {
                Player uuidPlayer = Bukkit.getPlayer(uuid);
                if (uuidPlayer != null) {
                    player = uuidPlayer;
                }
            }

            if (player == null) {
                continue;
            }

            String labelBase;
            if (plugin.getIntegrationManager() != null && plugin.getIntegrationManager().hasMultiPaper()) {
                labelBase = "Debug (" + plugin.getIntegrationManager().getMultiPaper().getLocalServerName() + ")";
            } else {
                labelBase = "Debug";
            }

            String sevLabel = (severity == 2) ? "WARN" : "INFO";
            ChatColor sevColor = (severity == 2) ? ChatColor.GOLD : ChatColor.RED;

            String callerSuffix = (caller != null && caller.hasAny())
                    ? ChatColor.DARK_GRAY + " [" + caller.toShortString() + "]" + ChatColor.RESET
                    : "";

            player.sendMessage(
                    ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " +
                            sevColor + labelBase + " " + ChatColor.DARK_GRAY + "(" + sevLabel + "):" +
                            ChatColor.RESET + " " + message + callerSuffix
            );
        }
    }

    /**
     * Builds a console prefix.
     *
     * @param severity 1=info, 2=warnings
     * @param caller caller tag (or null)
     * @return prefix
     */
    private String buildConsolePrefix(int severity, CallerInfo caller) {
        String mp = (plugin.getIntegrationManager() != null && plugin.getIntegrationManager().hasMultiPaper())
                ? " (" + plugin.getIntegrationManager().getMultiPaper().getLocalServerName() + ")"
                : "";

        String sevLabel = (severity == 2) ? "WARN" : "INFO";

        if (caller != null && caller.hasAny()) {
            return "Debug" + mp + " [" + sevLabel + " " + caller.toShortString() + "]: ";
        }
        return "Debug" + mp + " [" + sevLabel + "]: ";
    }

    /**
     * Finds the first stack frame that belongs to an external plugin.
     *
     * @param includeClassDetails whether to keep class/method/line details
     * @return caller info or null
     */
    private CallerInfo resolveTriggeringPluginCaller(boolean includeClassDetails) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        for (StackTraceElement e : stack) {
            if (e == null) {
                continue;
            }

            String cn = e.getClassName();

            if (isIgnorableFrame(cn)) {
                continue;
            }

            if (isInternalClass(cn)) {
                continue;
            }

            Class<?> clazz = tryLoadClass(cn);
            if (clazz == null) {
                continue;
            }

            Plugin providing;
            try {
                providing = JavaPlugin.getProvidingPlugin(clazz);
            } catch (IllegalArgumentException ex) {
                continue; // not a plugin-provided class
            } catch (Throwable ignored) {
                continue;
            }

            if (providing.getName().equalsIgnoreCase(plugin.getName())) {
                continue;
            }

            Integer line = (includeClassDetails && e.getLineNumber() > 0) ? e.getLineNumber() : null;

            return new CallerInfo(
                    providing.getName(),
                    includeClassDetails ? cn : null,              // full class path
                    includeClassDetails ? e.getMethodName() : null,
                    line
            );
        }

        return null;
    }

    /**
     * Attempts to load a class for plugin ownership checks.
     *
     * @param className fully qualified class name
     * @return loaded class or null
     */
    private Class<?> tryLoadClass(String className) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                return Class.forName(className, false, cl);
            }
        } catch (Throwable ignored) {
        }

        try {
            return Class.forName(className, false, DebugManager.class.getClassLoader());
        } catch (Throwable ignored) {
        }

        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
        }

        return null;
    }

    /**
     * Checks whether a frame should be ignored (server/tick/scheduler plumbing).
     *
     * @param className fqcn
     * @return true if ignored
     */
    private boolean isIgnorableFrame(String className) {
        return className.equals(Thread.class.getName())
                || className.equals(DebugManager.class.getName())
                || className.startsWith("java.")
                || className.startsWith("jdk.")
                || className.startsWith("sun.")
                || className.startsWith("net.minecraft.")
                || className.startsWith("com.mojang.")
                || className.startsWith("org.bukkit.")
                || className.startsWith("org.bukkit.craftbukkit.")
                || className.startsWith("com.destroystokyo.paper.")
                || className.startsWith("io.papermc.");
    }

    /**
     * Checks if a class is internal.
     *
     * @param className fqcn
     * @return true if internal
     */
    private boolean isInternalClass(String className) {
        for (String p : INTERNAL_PACKAGES) {
            if (className.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * External caller metadata.
     */
    private static final class CallerInfo {
        private final String pluginName;
        private final String className;   // full class path when enabled
        private final String methodName;
        private final Integer line;

        /**
         * Creates caller info.
         *
         * @param pluginName plugin name
         * @param className full class name (optional)
         * @param methodName method name (optional)
         * @param line line number (optional)
         */
        private CallerInfo(String pluginName, String className, String methodName, Integer line) {
            this.pluginName = pluginName;
            this.className = className;
            this.methodName = methodName;
            this.line = line;
        }

        /**
         * @return true if any field is present
         */
        private boolean hasAny() {
            return pluginName != null || className != null || methodName != null;
        }

        /**
         * @return plugin-only caller info (no class details)
         */
        private CallerInfo toPluginOnly() {
            return pluginName == null ? null : new CallerInfo(pluginName, null, null, null);
        }

        /**
         * Formats a compact tag.
         *
         * @return Plugin or Plugin:full.class.Name#method:line
         */
        private String toShortString() {
            StringBuilder sb = new StringBuilder();
            if (pluginName != null && !pluginName.isEmpty()) {
                sb.append(pluginName);
            }
            if (className != null) {
                sb.append(":").append(className);
            }
            if (methodName != null) {
                sb.append("#").append(methodName);
            }
            if (line != null) {
                sb.append(":").append(line);
            }
            return sb.toString();
        }
    }
}
