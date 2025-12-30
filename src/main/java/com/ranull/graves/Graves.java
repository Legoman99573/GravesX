package com.ranull.graves;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.ranull.graves.command.GravesCommand;
import com.ranull.graves.compatibility.*;
import com.ranull.graves.listener.*;
import com.ranull.graves.manager.*;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.*;
import dev.cwhead.GravesX.addon.GravesXAddon;
import dev.cwhead.GravesX.command.GxModulesCommand;
import dev.cwhead.GravesX.debug.KeepInventoryDetector;
import dev.cwhead.GravesX.debug.LateEnableHook;
import dev.cwhead.GravesX.listener.PlayerAfterRespawnListener;
import dev.cwhead.GravesX.manager.*;
import dev.cwhead.GravesX.module.listener.DependencyEnableListener;
import dev.cwhead.GravesX.module.util.LibbyImporter;
import dev.cwhead.GravesX.module.ModuleManager;
import dev.cwhead.GravesX.util.LibraryLoaderUtil;
import dev.cwhead.GravesX.util.MclogsUtil;
import dev.cwhead.GravesX.util.PastebinUtil;
import dev.cwhead.GravesX.util.ToptalUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Graves extends JavaPlugin {
    private VersionManager versionManager;
    private IntegrationManager integrationManager;
    private CacheManager cacheManager;
    private DataManager dataManager;
    private ImportManager importManager;
    private BlockManager blockManager;
    private ItemStackManager itemStackManager;
    private EntityDataManager entityDataManager;
    private HologramManager hologramManager;
    private GUIManager guiManager;
    private EntityManager entityManager;
    private RecipeManager recipeManager;
    private LocationManager locationManager;
    private GraveManager graveManager;
    private ParticleManager particleManager;
    private DebugManager debugManager;
    private PermissionManager permissionManager;
    private ConfigManager configManager;
    private Compatibility compatibility;
    private FileConfiguration fileConfiguration;
    private boolean isDevelopmentBuild = false;
    private boolean isOutdatedBuild = false;
    private boolean isUnknownBuild = false;
    private static TaskScheduler graveScheduler;
    private ModuleManager moduleManager;
    private boolean deferModuleLoad;
    private DependencyEnableListener depListener;

    @Override
    public void onLoad() {
        debugManager = new DebugManager(this);
        configManager = new ConfigManager(this);

        // Ensure plugin data folder exists (some servers won't create it until first save)
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Failed to create plugin data folder: " + getDataFolder().getAbsolutePath());
            // continue anyway; some file operations may fail and will log later
        }

        // Migrate legacy folder name: /plugins/Graves -> /plugins/GravesX
        File pluginsDir = getDataFolder().getParentFile();
        File gravesDirectory = new File(pluginsDir, "Graves");
        File newGravesDirectory = new File(pluginsDir, "GravesX");

        if (gravesDirectory.exists() && gravesDirectory.isDirectory()) {
            getLogger().warning("Your server has legacy version of Graves. Migrating the folder to GravesX for you.");
            if (gravesDirectory.renameTo(newGravesDirectory)) {
                getLogger().info("Successfully renamed legacy folder Graves to GravesX.");
            } else {
                getLogger().severe("Failed to rename legacy folder Graves to GravesX. Ensure the folder doesn't already exist.");
            }
        }

        // IMPORTANT: after any migration, re-ensure the data folder exists
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Failed to create plugin data folder after migration: " + getDataFolder().getAbsolutePath());
        }

        if (configManager == null) {
            configManager = new dev.cwhead.GravesX.manager.ConfigManager(this);
        }

        configManager.ensureDefaultsExist();

        GravesXAddon.ensureAddonRoot(this);

        integrationManager = new IntegrationManager(this);
        moduleManager = new ModuleManager(this);
        moduleManager.setLibraryImporter(new LibbyImporter(this));

        deferModuleLoad = moduleManager.shouldDeferLoadOnExternalPlugins();
    }

    @Override
    public void onEnable() {
        debugManager = new DebugManager(this);

        if (configManager == null) {
            configManager = new dev.cwhead.GravesX.manager.ConfigManager(this);
        }

        configManager.ensureDefaultsExist();
        configManager.updateIfNeeded(isPluginDevelopmentBuild());

        configManager.reload();
        loadLibraries();

        versionManager = new VersionManager();
        graveScheduler = UniversalScheduler.getScheduler(this);

        integrationManager.load();
        integrationManager.loadNoReload();

        cacheManager = new CacheManager();
        dataManager = new DataManager(this);
        importManager = new ImportManager(this);
        blockManager = new BlockManager(this);
        itemStackManager = new ItemStackManager(this);
        entityDataManager = new EntityDataManager(this);
        hologramManager = new HologramManager(this);
        guiManager = new GUIManager(this);
        entityManager = new EntityManager(this);
        locationManager = new LocationManager(this);
        graveManager = new GraveManager(this);
        particleManager = new ParticleManager(this);
        permissionManager = new PermissionManager(this);

        this.moduleManager = new ModuleManager(this);
        this.moduleManager.setLibraryImporter(new LibbyImporter(this));
        moduleManager.loadAll();
        moduleManager.enableAll();

        depListener = new DependencyEnableListener(moduleManager);
        getServer().getPluginManager().registerEvents(depListener, this);
        getGravesXScheduler().runTask(moduleManager::tryEnablePending);

        registerCommands();
        registerListeners();
        registerRecipes();
        saveTextFiles();

        getGravesXScheduler().runTaskLater(() -> {
            KeepInventoryDetector.install(this);
            KeepInventoryDetector.logWorldsWithGameruleKeepInventoryTrue(this);
        }, 1L);

        getServer().getPluginManager().registerEvents(new LateEnableHook(), this);

        if (getConfig().getBoolean("settings.metrics.enabled", true)) {
            getLogger().info("Loading bStats Metrics Manager...");

            MetricsManager metricsManager = new MetricsManager(this);
            metricsManager.registerMetrics();
            metricsManager.registerMetricsLegacy();
        } else {
            getLogger().warning("Metrics has been disabled. Metrics will not be sent to bStats.");
        }

        getGravesXScheduler().runTask(() -> {
            compatibilityChecker();
            updateChecker();
            getConfigManager().updateIfNeeded(isPluginDevelopmentBuild());
            RegisterSoftCrashHandler();
        });
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) moduleManager.disableAll();
        runShutdownTasks();
    }

    private void RegisterSoftCrashHandler() {
        getLogger().info("Registering Soft Crash Handler...");
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                getLogger().severe("GravesX detected server went into a crashed state...");
                runShutdownTasks();
                getServer().getPluginManager().disablePlugin(this);
            }));
            getLogger().info("Registered Soft Crash Handler. Server will handle crashes in a separate thread. This does not work on hard crashes.");
        } catch (Exception e) {
            getLogger().severe("Failed to Register Soft Crash Handler. Server will not listen to soft crashed states in a separate thread.");
        }
    }

    private void runShutdownTasks() {
        getLogger().info("Saving Grave inventories before shutting down...");
        for (Grave grave : getCacheManager().getGraveMap().values()) {
            try {
                getDataManager().updateGraveMainThread(grave, "inventory",
                        InventoryUtil.inventoryToString(grave.getInventory()));
                debugMessage("Saved inventory for grave UUID " + grave.getUUID() + " successfully", 2);
            } catch (Exception e) {
                getLogger().severe("Failed to save grave " + grave.getUUID() + " on shutdown: " + e.getMessage());
                logStackTrace(e);
            }
        }

        getLogger().info("Grave inventories saved.");

        getLogger().info("Shutting Down GravesX...");
        try {
            dataManager.closeConnection();
        } catch (Exception e) {
            getLogger().severe("Failed to close Database Connection.");
        }

        getLogger().info("Unloading GraveManager...");
        try {
            graveManager.unload();
            getLogger().info("Unloaded GraveManager Successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to unload GraveManager.");
        }

        getLogger().info("Unloading IntegrationManager...");
        try {
            integrationManager.unload();
            integrationManager.unloadNoReload();
            getLogger().info("Unloaded IntegrationManager Successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to unload IntegrationManager. Cause: " + e.getCause());
        }

        if (recipeManager != null) {
            getLogger().info("Unloading RecipeManager...");
            try {
                recipeManager.unload();
                getLogger().info("Unloaded RecipeManager Successfully.");
            } catch (Exception e) {
                getLogger().severe("Failed to unload RecipeManager.");
            }
        }

        if (debugManager != null) {
            getLogger().info("Unloading DebugManager...");
            try {
                debugManager.unload();
                getLogger().info("Unloaded DebugManager Successfully.");
            } catch (Exception e) {
                getLogger().severe("Failed to unload DebugManager.");
            }
        }
        getLogger().info("Shutdown Completed :)");
    }

    private void loadLibraries() {
        getLogger().info("Loading Libraries for GravesX");

        LibraryLoaderUtil libraryLoaderUtil = new LibraryLoaderUtil(this);

        getLogger().warning(getServer().getName() + " v." + getServer().getVersion() + " detected. Using BukkitLibraryManager to download and load libraries.");

        libraryLoaderUtil.loadLibrary("org{}bstats", "bstats-base", "3.1.0", "org{}bstats", "com{}ranull{}graves{}libraries{}bstats", false);
        libraryLoaderUtil.loadLibrary("org{}bstats", "bstats-bukkit", "3.1.0", "org{}bstats", "com{}ranull{}graves{}libraries{}bstats", false);

        libraryLoaderUtil.loadLibrary("com{}zaxxer", "HikariCP", "6.3.3", "com{}zaxxer{}hikari", "com{}ranull{}graves{}libraries{}hikari", false);
        libraryLoaderUtil.loadLibrary("org{}xerial", "sqlite-jdbc", "3.50.3.0", false);

        try {
            Class.forName("org.json.JSONObject");
        } catch (ClassNotFoundException e) {
            libraryLoaderUtil.loadLibrary("org{}json", "json", "20250517");
        }

        try {
            Class.forName("com.google.gson.Gson");
        } catch (ClassNotFoundException e) {
            libraryLoaderUtil.loadLibrary("com{}google{}code{}gson", "gson", "2.13.2", false);
        }

        try {
            Class.forName("com.google.common.collect.ImmutableList");
        } catch (ClassNotFoundException e) {
            libraryLoaderUtil.loadLibrary("com{}google{}guava", "guava", "33.5.0-jre", false);
        }

        //libraryLoaderUtil.loadLibrary("com{}github{}oshi", "oshi-core", "6.8.2", false);

        String storageType = Objects.requireNonNull(getConfig().getString("settings.storage.type")).toUpperCase();

        switch (storageType) {
            case "POSTGRESQL":
                libraryLoaderUtil.loadLibrary("org{}postgresql", "postgresql", "42.7.8", "org{}postgresql", "com{}ranull{}graves{}libraries{}postgresql", false);
                break;
            case "MARIADB":
                libraryLoaderUtil.loadLibrary("com{}mysql", "mysql-connector-j", "9.4.0", "com{}mysql", "com{}ranull{}graves{}libraries{}mysql", false);
                libraryLoaderUtil.loadLibrary("org{}mariadb{}jdbc", "mariadb-java-client", "3.5.6", "org{}mariadb", "com{}ranull{}graves{}libraries{}mariadb", false);
                break;
            case "MYSQL":
                libraryLoaderUtil.loadLibrary("com{}mysql", "mysql-connector-j", "9.4.0", "com{}mysql", "com{}ranull{}graves{}libraries{}mysql", false);
                break;
            case "H2":
                libraryLoaderUtil.loadLibrary("com{}h2database", "h2", "2.4.240", "org{}h2", "com{}ranull{}graves{}libraries{}h2", false, "https://repo1.maven.org/maven2/");
                break;
            case "MSSQL":
                libraryLoaderUtil.loadLibrary("com{}microsoft{}sqlserver", "mssql-jdbc", "13.2.0.jre11", "com{}microsoft", "com{}ranull{}graves{}libraries{}microsoft", false);
                break;
        }
        libraryLoaderUtil.loadLibrary("net{}kyori", "adventure-platform-bukkit", "4.4.1", "net{}kyori", "com{}ranull{}graves{}libraries{}kyori", false);
        libraryLoaderUtil.loadLibrary("net{}kyori", "adventure-api", "4.24.0", "net{}kyori", "com{}ranull{}graves{}libraries{}kyori", false);
        libraryLoaderUtil.loadLibrary("net{}kyori", "adventure-text-minimessage", "4.24.0", "net{}kyori", "com{}ranull{}graves{}libraries{}kyori", false);
        libraryLoaderUtil.loadLibrary("net{}kyori", "adventure-text-serializer-gson", "4.24.0", "net{}kyori", "com{}ranull{}graves{}libraries{}kyori", false);
        libraryLoaderUtil.loadLibrary("com{}github{}puregero", "multilib", "1.2.4", "com{}github{}puregero{}multilib", "com{}ranull{}graves{}libraries{}multilib", false, "https://repo.clojars.org/");
        libraryLoaderUtil.loadLibrary("org{}apache{}commons", "commons-text", "1.14.0", "org{}apache{}commons{}text", "com{}ranull{}graves{}libraries{}commonstext", false);

        getLogger().info("Finished Loading Libraries for GravesX.");
    }

    @Override
    public void saveDefaultConfig() {
        getConfigManager().ensureDefaultsExist();
    }

    @Override
    public void reloadConfig() {
        getConfigManager().reload();
    }

    @Override
    @NotNull
    public FileConfiguration getConfig() {
        return getConfigManager().config();
    }

    public void reload() {
        getConfigManager().ensureDefaultsExist();
        getConfigManager().updateIfNeeded(isPluginDevelopmentBuild());
        getConfigManager().reload();
        saveTextFiles();
        unregisterListeners();
        registerListeners();
        // dataManager.reload();
        integrationManager.reload();
        try {
            registerRecipes();
        } catch (Exception e) {
            if (recipeManager != null) {
                recipeManager.reload();
            }
        }

        infoMessage(getName() + " reloaded.");
    }

    public void saveTextFiles() {
        ResourceUtil.copyResources("data/text/readme.txt", getDataFolder().getPath()
                + "/readme.txt", this);
        ResourceUtil.copyResources("data/text/placeholders.txt", getDataFolder().getPath()
                + "/placeholders.txt", this);

        if (integrationManager != null) {
            if (integrationManager.hasPlaceholderAPI()) {
                ResourceUtil.copyResources("data/text/placeholderapi.txt", getDataFolder().getPath()
                        + "/placeholderapi.txt", this);
            }

            if (integrationManager.hasFurnitureLib()) {
                ResourceUtil.copyResources("data/text/furniturelib.txt", getDataFolder().getPath()
                        + "/furniturelib.txt", this);
            }
        }
    }

    public void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerBucketListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        if (getVersionManager().isFolia()) {
            getServer().getPluginManager().registerEvents(new PlayerAfterRespawnListener(this), this);
        } else {
            getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        }
        getServer().getPluginManager().registerEvents(new PlayerTeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDropItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBurnAndIgniteListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDamageByEntityListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockFromToListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPistonExtendListener(this), this);
        getServer().getPluginManager().registerEvents(new HangingBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryDragListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryOpenListener(this), this);
        getServer().getPluginManager().registerEvents(new CreatureSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosionPrimeListener(this), this);
        getServer().getPluginManager().registerEvents(new ProjectileHitListener(this), this);

        if (!versionManager.is_v1_7()) {
            getServer().getPluginManager().registerEvents(new PlayerInteractAtEntityListener(this), this);
        }

        if (!versionManager.is_v1_7() && !versionManager.is_v1_8()) {
            getServer().getPluginManager().registerEvents(new BlockExplodeListener(this), this);
        }

        //getServer().getPluginManager().registerEvents(new GraveTestListener(this), this); // Test Listener
    }

    public void unregisterListeners() {
        HandlerList.unregisterAll(this);
    }

    private void registerRecipes() {
        if (versionManager.hasPersistentData() && !versionManager.isMohist()) {
            recipeManager = new RecipeManager(this);
        }
    }

    private void registerCommands() {
        PluginCommand gravesPluginCommand = getCommand("graves");

        if (gravesPluginCommand != null) {
            GravesCommand gravesCommand = new GravesCommand(this);

            gravesPluginCommand.setExecutor(gravesCommand);
            gravesPluginCommand.setTabCompleter(gravesCommand);
        }

        PluginCommand cmd = getCommand("gravesmodule");
        if (cmd != null) {
            GxModulesCommand handler = new GxModulesCommand(moduleManager, this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
    }

    /**
     * Sends a debug message using {@link DebugManager}.
     *
     * @param string the message
     * @param level  0=Nothing, 1=info, 2=warnings
     */
    public void debugMessage(String string, int level) {
        getDebugManager().debug(string, level);
    }

    /**
     * Logs a warning message to the console with a "Warning" prefix.
     *
     * @param string the message to log
     */
    public void warningMessage(String string) {
        getLogger().warning("Warning: " + string);
    }

    /**
     * Logs a compatibility-related warning message to the console.
     *
     * @param string the message to log
     */
    public void compatibilityMessage(String string) {
        getLogger().warning("Compatibility: " + string + " This is not a bug.");
    }

    /**
     * Logs an informational message to the console.
     *
     * @param string the message to log
     */
    public void infoMessage(String string) {
        getLogger().info("Information: " + string);
    }

    /**
     * Logs a test message to the console. Used for internal/debug purposes.
     *
     * @param string the message to log
     */
    public void testMessage(String string) {
        getLogger().info("Test: " + string);
    }

    /**
     * Logs an update message to the console.
     *
     * @param string the message to log
     */
    public void updateMessage(String string) {
        getLogger().info("Update: " + string);
    }

    /**
     * Logs an integration message to the console as an info message by default.
     *
     * @param string the message to log
     */
    public void integrationMessage(String string) {
        integrationMessage(string, "info");
    }

    /**
     * Logs an integration message to the console with the specified message level.
     *
     * @param string      the message to log
     * @param messageType the type of message: "info", "warn", or "severe"
     */
    public void integrationMessage(String string, String messageType) {
        switch (messageType) {
            case "warning", "warn" -> getLogger().warning("Integration: " + string);
            case "severe", "error" -> getLogger().severe("Integration: " + string);
            case "info", "debug" -> getLogger().info("Integration: " + string);
            default -> getLogger().info("Integration: " + string);
        }
    }

    /**
     * Asynchronously checks for plugin updates based on the configured update check setting.
     * Logs messages indicating whether the plugin is outdated, up to date, or a development build.
     * Also handles malformed version formats gracefully.
     */
    private void updateChecker() {
        if (getConfig().getBoolean("settings.update.check")) {
            getGravesXScheduler().runTaskAsynchronously(() -> {
                String latestVersion = getLatestVersion();
                String installedVersion = getDescription().getVersion();

                if (latestVersion != null && !installedVersion.equalsIgnoreCase(latestVersion)) {
                    try {
                        int comparisonResult = compareVersions(installedVersion, latestVersion);

                        if (comparisonResult < 0) {
                            setPluginOutdatedBuild(true);
                            getLogger().warning("You are using an outdated version of " + getDescription().getName() + ".");
                            getLogger().warning("Installed Version: " + installedVersion);
                            getLogger().warning("Latest Version:  " + latestVersion);
                            getLogger().warning("Grab the latest release from https://www.spigotmc.org/resources/" + getSpigotID() + "/");
                        } else if (comparisonResult > 0) {
                            setPluginDevelopmentBuild(true);
                            getLogger().severe("You are running " + getDescription().getName() + " version " + installedVersion + ", which is a development build and is not production safe.");
                            getLogger().severe("THERE WILL NOT BE SUPPORT IF YOU LOSE GRAVE DATA FROM DEVELOPMENT OR COMPILED BUILDS. THIS BUILD IS FOR TESTING PURPOSES ONLY");
                            getLogger().severe("Keep note that you are using a development version when you report bugs.");
                            getLogger().severe("If the same issue occurs in "  + latestVersion + ", then let us know in https://discord.ranull.com/.");
                        } else {
                            getLogger().info("You are running the latest version of " + getDescription().getName() + ".");
                        }
                    } catch (NumberFormatException exception) {
                        setPluginUnknownBuild(true);
                        getLogger().severe("NumberFormatException: " + exception.getMessage());
                        if (!installedVersion.equalsIgnoreCase(latestVersion)) {
                            getLogger().severe("You are either running an outdated version of " + getDescription().getName() + " or a development version.");
                            getLogger().severe("Installed Version: " + installedVersion);
                            getLogger().severe("Latest Version:  " + latestVersion);
                        }
                    }
                }
            });
        }
    }

    /**
     * Compares two semantic version strings (e.g., "1.16.5" vs. "1.18").
     * <p>
     * Each version string is split by the period (.) character, and each corresponding
     * segment is compared numerically. If one version has more segments than the other,
     * missing or non-numeric segments are treated as 0.
     * </p>
     *
     * @param version1 the first version string to compare
     * @param version2 the second version string to compare
     * @return -1 if {@code version1} is lower than {@code version2},
     *          1 if {@code version1} is higher than {@code version2},
     *          0 if both versions are equal
     */
    private int compareVersions(String version1, String version2) {
        String[] levels1 = version1.split("\\.");
        String[] levels2 = version2.split("\\.");

        int length = Math.max(levels1.length, levels2.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < levels1.length ? parseVersionPart(levels1[i]) : 0;
            int v2 = i < levels2.length ? parseVersionPart(levels2[i]) : 0;

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
     * Attempts to parse a version segment to an integer.
     * Returns 0 if parsing fails.
     *
     * @param part the version segment as a string
     * @return the parsed integer or 0 if invalid
     */
    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Checks for server and version compatibility, and sets the appropriate
     * compatibility handler depending on whether the server supports {@code BlockData}.
     * <p>
     * Outputs informational messages about potential issues when running on legacy versions,
     * Bukkit, or Mohist servers.
     * </p>
     */
    private void compatibilityChecker() {
        compatibility = versionManager.hasBlockData() ? new CompatibilityBlockData() : new CompatibilityMaterialData();

        if (!versionManager.hasBlockData()) {
            infoMessage("Legacy version detected, Graves will run but may have problems with material names, " +
                    "the default config is setup for the latest version of the game, you can alter the config manually to fix " +
                    "any issues you encounter, you will need to find the names of materials and sounds for your version.");
        }

        if (versionManager.isBukkit()) {
            infoMessage("Bukkit detected, some functions won't work on Bukkit, like hex codes.");
        }

        if (versionManager.isMohist()) {
            infoMessage("Mohist detected, not injecting custom recipes. We also do not recommend Mohist nor will provide support if something goes wrong. Read here why: https://essentialsx.net/do-not-use-mohist.html");
        }
    }

    public void dumpServerInfo(CommandSender commandSender) {
        if (!isEnabled()) return;

        getGravesXScheduler().runTaskAsynchronously(() -> {
            final String serverDumpInfo = ServerUtil.getServerDumpInfo(this);
            String message = serverDumpInfo;

            final String method = getConfig().getString("settings.dump.method", "MCLOGS").trim().toUpperCase();

            String response = null;
            try {
                switch (method) {
                    case "HASTEBIN":
                    case "TOPTAL": {
                        org.bukkit.configuration.ConfigurationSection tp =
                                getConfig().getConfigurationSection("settings.dump.toptal");
                        String token = tp != null ? tp.getString("token", "") : "";
                        response = ToptalUtil.post(serverDumpInfo, token);
                        break;
                    }
                    case "MCLOGS":
                        response = MclogsUtil.postLogToMclogs(serverDumpInfo);
                        break;
                    case "PASTEBIN": {
                        org.bukkit.configuration.ConfigurationSection pb =
                                getConfig().getConfigurationSection("settings.dump.pastebin");
                        String devKey  = pb != null ? pb.getString("dev-key", "") : "";
                        String userKey = pb != null ? pb.getString("user-key", "") : "";
                        String privacy = pb != null ? pb.getString("privacy", "UNLISTED") : "UNLISTED";
                        String expire  = pb != null ? pb.getString("expire", "1W") : "1W";
                        String title   = "GravesX Dump " + System.currentTimeMillis();

                        response = PastebinUtil.post(
                                devKey, userKey, title, serverDumpInfo, privacy, expire
                        );
                        break;
                    }
                    default:
                        getLogger().warning("Unknown dump method '" + method + "', falling back to local file.");
                }
            } catch (Exception ex) {
                getLogger().severe("Failed to dump info. You will need to either switch provider or provide the plugin version when reporting dump info.");
                logStackTrace(ex);
            }

            if (response != null && !response.isBlank()) {
                message = response;
                getLogger().info("Log uploaded successfully. URL: " + response);
            } else {
                try {
                    File dumpDir = new File(getDataFolder(), "dump");
                    if (!dumpDir.exists() && !dumpDir.mkdirs()) {
                        throw new IOException("Could not create dump directory: " + dumpDir.getAbsolutePath());
                    }
                    String name = "gravesx-dump-" + System.currentTimeMillis() + ".txt";
                    File dumpFile = new File(dumpDir, name);
                    try (OutputStream os = new FileOutputStream(dumpFile);
                         Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                        w.write(serverDumpInfo);
                    }
                    message = dumpFile.getAbsolutePath();
                } catch (IOException ioe) {
                    logStackTrace(ioe);
                }
            }

            commandSender.sendMessage(
                    ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + "Dumped: " + message
            );
        });
    }

    /**
     * @return the {@link VersionManager} responsible for handling Minecraft version compatibility.
     */
    public VersionManager getVersionManager() {
        return versionManager;
    }

    /**
     * @return the {@link IntegrationManager} that manages third-party plugin integrations.
     */
    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }

    /**
     * @return the {@link GraveManager} that handles the creation and management of graves.
     */
    public GraveManager getGraveManager() {
        return graveManager;
    }

    /**
     * @return the {@link HologramManager} for displaying holographic text or elements above graves.
     */
    public HologramManager getHologramManager() {
        return hologramManager;
    }

    /**
     * @return the {@link BlockManager} that manages custom block-related functionality.
     */
    public BlockManager getBlockManager() {
        return blockManager;
    }

    /**
     * @return the {@link ItemStackManager} that handles item serialization and manipulation.
     */
    public ItemStackManager getItemStackManager() {
        return itemStackManager;
    }

    /**
     * @return the {@link EntityDataManager} used for storing and retrieving entity-specific data.
     */
    public EntityDataManager getEntityDataManager() {
        return entityDataManager;
    }

    /**
     * @return the {@link CacheManager} responsible for caching frequently accessed data.
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * @return the {@link DataManager} that manages persistent plugin data and file I/O.
     */
    public DataManager getDataManager() {
        return dataManager;
    }

    /**
     * @return the {@link ImportManager} used for importing data from other plugins or older formats.
     */
    public ImportManager getImportManager() {
        return importManager;
    }

    /**
     * @return the {@link GUIManager} that handles graphical user interfaces shown to players.
     */
    public GUIManager getGUIManager() {
        return guiManager;
    }

    /**
     * @return the {@link RecipeManager} responsible for managing custom recipes.
     */
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    /**
     * @return the {@link LocationManager} that handles location serialization and retrieval.
     */
    public LocationManager getLocationManager() {
        return locationManager;
    }

    /**
     * @return the {@link EntityManager} for managing in-game entities related to graves.
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * @return the {@link ParticleManager} that manages particle effects used by the plugin.
     */
    public ParticleManager getParticleManager() {
        return particleManager;
    }

    /**
     * @return the {@link DebugManager} that manages debugging used by the plugin.
     */
    public DebugManager getDebugManager() {
        return debugManager;
    }

    /**
     * @return the {@link PermissionManager} that manages permission handling used by this plugin.
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    /**
     * @return the {@link ConfigManager} that manages Configuration used by this plugin.
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * @return the {@link Compatibility} handler that ensures functionality across Minecraft versions and server platforms.
     */
    public Compatibility getCompatibility() {
        return compatibility;
    }

    /**
     * @return the {@link TaskScheduler} used for running asynchronous or scheduled plugin tasks.
     */
    public TaskScheduler getGravesXScheduler() {
        return graveScheduler;
    }

    /**
     * Returns the plugin's current release type.
     *
     * @return a string indicating whether the build is Development, Outdated, Unknown, or Production.
     */
    public String getPluginReleaseType() {
        if (isPluginDevelopmentBuild()) {
            return "Development Build";
        } else if (isPluginOutdatedBuild()) {
            return "Outdated Build";
        } else if (isPluginUnknownBuild()) {
            return "Unknown Build";
        } else {
            return "Production Build";
        }
    }

    /**
     * Gets a configuration section based on a specific grave.
     *
     * @param config the config key.
     * @param grave the grave instance.
     * @return the matching configuration section, or default if none match.
     * @deprecated Will be removed in {@code 4.9.12.1}. Use
     * {@link #getConfigManager()}{@link dev.cwhead.GravesX.manager.ConfigManager#getConfigSection(String, Grave)}
     * instead.
     */
    @Deprecated(since = "4.9.10.1")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.12.1")
    public ConfigurationSection getConfig(String config, Grave grave) {
        debugMessage("Graves#getConfig(String, Grave) is deprecated and will be removed in 4.9.12.1. Use Graves#getConfigManager().getConfigSection(String, Grave) instead", 2);
        return getConfigManager().getConfigSection(config, grave);
    }

    /**
     * Gets a configuration section for a list of graves.
     * Returns the first matching config found, or the default if none match.
     *
     * @param config the config key.
     * @param graveList the list of graves.
     * @return the matching configuration section, or default if none match.
     * @deprecated Will be removed in {@code 4.9.12.1}. Use
     * {@link #getConfigManager()}{@link ConfigManager#getConfigSection(String, List)}
     * instead.
     */
    @Deprecated(since = "4.9.10.1")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.12.1")
    public ConfigurationSection getConfig(String config, List<Grave> graveList) {
        debugMessage("Graves#getConfig(String, List<Grave>) is deprecated and will be removed in 4.9.12.1. Use Graves#getConfigManager().getConfigSection(String, List<Grave>) instead", 2);
        return getConfigManager().getConfigSection(config, graveList);
    }

    /**
     * Gets a configuration section based on a specific entity.
     *
     * @param config the config key.
     * @param entity the entity.
     * @return the matching configuration section, or default if none match.
     * @deprecated Will be removed in {@code 4.9.12.1}. Use
     * {@link #getConfigManager()}{@link ConfigManager#getConfigSection(String, Entity)}
     * instead.
     */
    @Deprecated(since = "4.9.10.1")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.12.1")
    public ConfigurationSection getConfig(String config, Entity entity) {
        debugMessage("Graves#getConfig(String, Entity) is deprecated and will be removed in 4.9.12.1. Use Graves#getConfigManager().getConfigSection(String, Entity) instead", 2);
        return getConfigManager().getConfigSection(config, entity);
    }

    /**
     * Gets a configuration section based on a specific entity and its permission list.
     *
     * @param config the config key.
     * @param entity the entity.
     * @param permissionList the permissions associated with the entity.
     * @return the matching configuration section, or default if none match.
     * @deprecated Will be removed in {@code 4.9.12.1}. Use
     * {@link #getConfigManager()}{@link ConfigManager#getConfigSection(String, Entity, List)}
     * instead.
     */
    @Deprecated(since = "4.9.10.1")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.12.1")
    public ConfigurationSection getConfig(String config, Entity entity, List<String> permissionList) {
        debugMessage("Graves#getConfig(String, Entity, List<String>) is deprecated and will be removed in 4.9.12.1. Use Graves#getConfigManager().getConfigSection(String, Entity, List<String>) instead", 2);
        return getConfigManager().getConfigSection(config, entity, permissionList);
    }

    /**
     * Resolves the most appropriate configuration section based on entity type and permissions.
     *
     * @param config the config key.
     * @param entityType the type of entity.
     * @param permissionList a list of permissions to prioritize.
     * @return the best matching configuration section.
     * @deprecated Will be removed in {@code 4.9.12.1}. Use
     * {@link #getConfigManager()}{@link ConfigManager#getConfigSection(String, EntityType, java.util.List)}
     * instead.
     */
    @Deprecated(since = "4.9.10.1")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.12.1")
    public ConfigurationSection getConfig(String config, EntityType entityType, List<String> permissionList) {
        debugMessage("Graves#getConfig(String, EntityType, List<String>) is deprecated and will be removed in 4.9.12.1. Use Graves#getConfigManager().getConfigSection(String, EntityType, List<String>) instead", 2);
        return getConfigManager().getConfigSection(config, entityType, permissionList);
    }

    /**
     * Builds a sorted list of permission keys for a given entity (player).
     *
     * @param entity the entity (usually a Player).
     * @return a sorted list of permission keys that match configuration sections.
     * @deprecated Will be removed in {@code 4.9.12.1}. Use
     * {@link #getConfigManager()}{@link ConfigManager#getPermissionList(Entity)}
     * instead.
     */
    @Deprecated(since = "4.9.10.1")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.12.1")
    public List<String> getPermissionList(Entity entity) {
        debugMessage("Graves#getPermissionList(Entity) is deprecated and will be removed in 4.9.12.1. Use Graves#getConfigManager().getPermissionList(Entity) instead", 2);
        return getConfigManager().getPermissionList(entity);
    }

    /**
     * @return the parent folder where all plugins are stored.
     */
    public final File getPluginsFolder() {
        return getConfigManager().getPluginsFolder();
    }

    /**
     * @return the current version of the Graves plugin from plugin.yml.
     */
    public String getVersion() {
        return getDescription().getVersion();
    }

    /**
     * @return the latest available version from Spigot update checking.
     */
    public String getLatestVersion() {
        return UpdateUtil.getLatestVersion(getSpigotID());
    }

    /**
     * @return the Spigot plugin resource ID used for update checking.
     */
    public final int getSpigotID() {
        return 118271;
    }

    /**
     * Logs the full stack trace of an exception to the plugin logger.
     *
     * @param e the exception to log.
     */
    public void logStackTrace(Exception e) {
        logStackTrace((Throwable) e);
    }

    /**
     * Logs the full stack trace of a throwable to the plugin logger.
     *
     * @param t the throwable to log.
     */
    public void logStackTrace(Throwable t) {
        java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        printThrowable(t, "", seen);
    }

    private void printThrowable(Throwable t, String prefix, java.util.Set<Throwable> seen) {
        if (t == null || seen.contains(t)) return;
        seen.add(t);
        getLogger().severe(prefix + t);
        for (StackTraceElement e : t.getStackTrace()) {
            getLogger().severe("  at " + e);
        }
        Throwable[] suppressed = t.getSuppressed();
        if (suppressed != null) {
            for (Throwable s : suppressed) {
                printThrowable(s, "Suppressed: ", seen);
            }
        }
        Throwable cause = t.getCause();
        if (cause != null) {
            printThrowable(cause, "Caused by: ", seen);
        }
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    /**
     * Logs information about a grave that has invalid or incomplete data.
     *
     * @param grave_uuid the UUID of the affected grave.
     * @param affectedGraveLocation the location of the grave.
     * @param invalidationReason reasons the grave is considered invalid.
     */
    public void logInvalidGraveSite(String grave_uuid, Location affectedGraveLocation, List<String> invalidationReason) {
        String graveWorld;
        try {
            graveWorld = Objects.requireNonNull(affectedGraveLocation.getWorld()).getName();
        } catch (Exception e) {
            graveWorld = "Unknown";
        }
        String graveX = String.valueOf(affectedGraveLocation.getBlockX());
        String graveY = String.valueOf(affectedGraveLocation.getBlockY());
        String graveZ = String.valueOf(affectedGraveLocation.getBlockZ());
        getLogger().warning("Grave "
                + grave_uuid
                + " at location World: "
                + graveWorld
                + ", x: "
                + graveX
                + ", y: "
                + graveY
                + ", z: "
                + graveZ
                + " has the following missing from grave data: "
                + String.join(", ", invalidationReason)
                + ". This shouldn't affect grave behavior. Do not report this as a bug.");
    }

    /**
     * Checks whether the given player has been granted the specified permission.
     *
     * @param permission the permission to check
     * @param player the player whose permissions are being checked
     * @return {@code true} if the player has the permission, otherwise {@code false}
     * @deprecated Will be removed in {@code 4.9.12.1}. Use
     * {@link #getPermissionManager()}{@link PermissionManager#hasGrantedPermission(String, Player)} instead.
     */
    @Deprecated(since = "4.9.10.1")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.12.1")
    public boolean hasGrantedPermission(String permission, Player player) {
        debugMessage("Graves#hasGrantedPermission(String, Player) is deprecated and will be removed in 4.9.12.1. Use Graves#getPermissionManager().hasGrantedPermission(String, Player) instead", 2);
        return getPermissionManager().hasGrantedPermission(permission, player);
    }

    /**
     * Checks whether the given offline player has been granted the specified permission.
     *
     * @param permission the permission to check
     * @param offlinePlayer the offline player whose permissions are being checked
     * @return {@code true} if the offline player has the permission, otherwise {@code false}
     * @deprecated Will be removed in {@code 4.9.12.1}. Use
     * {@link #getPermissionManager()}{@link PermissionManager#hasGrantedPermission(String, OfflinePlayer)} instead.
     */
    @Deprecated(since = "4.9.10.1")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.12.1")
    public boolean hasGrantedPermission(String permission, OfflinePlayer offlinePlayer) {
        debugMessage("Graves#hasGrantedPermission(String, OfflinePlayer) is deprecated and will be removed in 4.9.12.1. Use Graves#getPermissionManager().hasGrantedPermission(String, OfflinePlayer) instead", 2);
        return getPermissionManager().hasGrantedPermission(permission, offlinePlayer);
    }

    /**
     * @return true if this is a development build.
     */
    public boolean isPluginDevelopmentBuild() {
        return isDevelopmentBuild;
    }

    /**
     * @return true if this build is marked as outdated.
     */
    public boolean isPluginOutdatedBuild() {
        return isOutdatedBuild;
    }

    /**
     * @return true if this build's status is unknown.
     */
    public boolean isPluginUnknownBuild() {
        return isUnknownBuild;
    }

    /**
     * Sets whether this is a development build.
     *
     * @param value true for development build
     */
    public void setPluginDevelopmentBuild(boolean value) {
        isDevelopmentBuild = value;
    }

    /**
     * Sets whether this build is marked as outdated.
     *
     * @param value true for outdated build
     */
    public void setPluginOutdatedBuild(boolean value) {
        isOutdatedBuild = value;
    }

    /**
     * Sets whether this build's status is unknown.
     *
     * @param value true for unknown build
     */
    public void setPluginUnknownBuild(boolean value) {
        isUnknownBuild = value;
    }
}