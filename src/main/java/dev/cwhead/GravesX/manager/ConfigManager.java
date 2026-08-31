package dev.cwhead.GravesX.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.ResourceUtil;
import com.ranull.graves.util.YAMLUtil;
import com.tchristofferson.configupdater.ConfigUpdater;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Self-managed configuration system for GravesX.
 *
 * <p>Does NOT rely on JavaPlugin's built-in config lifecycle (saveDefaultConfig/reloadConfig/saveConfig).
 * It owns extraction, loading, merging, default injection, versioning, backups, and updates.</p>
 *
 * <p>It still returns Bukkit's FileConfiguration (YamlConfiguration) so the rest of the codebase can keep using
 * getConfig().getX(...).</p>
 */
public final class ConfigManager {

    public static final int CURRENT_CONFIG_VERSION = 27;

    private final Graves plugin;
    private final Paths paths;
    private final IO io;
    private final Merger merger;
    private final Defaults defaults;
    private final Updater updater;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile FileConfiguration mergedConfig;

    public ConfigManager(@NotNull Graves plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.paths = new Paths(plugin);
        this.io = new IO(plugin);
        this.merger = new Merger(plugin, io);
        this.defaults = new Defaults(plugin, io);
        this.updater = new Updater(plugin, paths);
    }

    /**
     * Initializes the configuration system:
     * <ol>
     *   <li>Extract defaults if missing</li>
     *   <li>Optionally update configs if outdated / dev build</li>
     *   <li>Load merged config into memory</li>
     * </ol>
     */
    public void init(boolean isDevelopmentBuild) {
        ensureDefaultsExist();
        updateIfNeeded(isDevelopmentBuild);
        reload();
    }

    /**
     * Extracts jar defaults to disk if missing.
     * Uses ResourceUtil copier (not JavaPlugin#saveDefaultConfig).
     */
    public void ensureDefaultsExist() {
        ResourceUtil.copyResources("config", paths.configFolder.getPath(), false, plugin);
    }

    /**
     * Reloads configuration from disk (legacy single-file supported; otherwise merges folder mode).
     */
    public void reload() {
        lock.writeLock().lock();
        try {
            FileConfiguration loaded;

            if (paths.legacySingleConfig.exists()) {
                loaded = io.loadYaml(paths.legacySingleConfig);
                if (loaded == null) loaded = new YamlConfiguration();

                defaults.applyResourceDefaults(loaded, paths.legacySingleConfig.getName());
            } else {
                loaded = merger.loadMergedFolderConfig(paths.configFolder);
            }

            mergedConfig = loaded;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the currently loaded merged config (lazy loads if needed).
     */
    @NotNull
    public FileConfiguration config() {
        FileConfiguration snapshot = mergedConfig;
        if (snapshot != null) {
            return snapshot;
        }

        lock.writeLock().lock();
        try {
            if (mergedConfig == null) {
                reload();
            }
            return mergedConfig;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Checks disk config-version and updates config files if needed, then reloads.
     * This is the former Graves#updateConfig() logic, now owned here.
     */
    public void updateIfNeeded(boolean isDevelopmentBuild) {
        updater.updateIfNeeded(isDevelopmentBuild);
    }

    @Nullable
    public ConfigurationSection getConfigSection(String configKey, @NotNull Grave grave) {
        return getConfigSection(configKey, grave.getOwnerType(), grave.getPermissionList());
    }

    @Nullable
    public ConfigurationSection getConfigSection(String configKey, @Nullable List<Grave> graveList) {
        if (graveList == null) return null;

        for (Grave grave : graveList) {
            ConfigurationSection section = getConfigSection(configKey, grave.getOwnerType(), grave.getPermissionList());
            if (section != null) return section;
        }
        return null;
    }

    @Nullable
    public ConfigurationSection getConfigSection(String configKey, @NotNull Entity entity) {
        return getConfigSection(configKey, entity.getType(), getPermissionList(entity));
    }

    @Nullable
    public ConfigurationSection getConfigSection(String configKey, @NotNull Entity entity, @Nullable List<String> permissionList) {
        return getConfigSection(configKey, entity.getType(), permissionList);
    }

    /**
     * Priority order:
     * <ol>
     *     <li>{@code settings.permission.<perm>}</li>
     *     <li>{@code settings.entity.<ENTITYTYPE>}</li>
     *     <li>{@code settings.default.default}</li>
     * </ol>
     */
    @Nullable
    public ConfigurationSection getConfigSection(String configKey, @Nullable EntityType entityType, @Nullable List<String> permissionList) {
        FileConfiguration cfg = config();

        if (permissionList != null && !permissionList.isEmpty()) {
            for (String permission : permissionList) {
                String sectionPath = "settings.permission." + permission;
                if (cfg.isConfigurationSection(sectionPath)) {
                    ConfigurationSection section = cfg.getConfigurationSection(sectionPath);
                    if (section != null && contains(section, configKey)) {
                        return section;
                    }
                }
            }
        }

        if (entityType != null) {
            String sectionPath = "settings.entity." + entityType.name();
            if (cfg.isConfigurationSection(sectionPath)) {
                ConfigurationSection section = cfg.getConfigurationSection(sectionPath);
                if (section != null && contains(section, configKey)) {
                    return section;
                }
            }
        }

        return cfg.getConfigurationSection("settings.default.default");
    }

    private boolean contains(@NotNull ConfigurationSection section, @NotNull String path) {
        if (plugin.getVersionManager() != null && plugin.getVersionManager().hasConfigContains()) {
            return section.contains(path, true);
        }
        return section.contains(path);
    }

    /**
     * Builds a sorted list of permission keys for a given player, matching config sections.
     */
    @NotNull
    public List<String> getPermissionList(@NotNull Entity entity) {
        if (!(entity instanceof Player player)) return Collections.emptyList();

        FileConfiguration cfg = config();
        List<String> permissionList = new ArrayList<>();
        List<String> permissionListSorted = new ArrayList<>();

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission();

            if (perm.startsWith("graves.permission.")) {
                String key = perm.replace("graves.permission.", "").toLowerCase(Locale.ROOT);
                if (cfg.isConfigurationSection("settings.permission." + key)) {
                    permissionList.add(key);
                }
            }
        }

        ConfigurationSection permissionRoot = cfg.getConfigurationSection("settings.permission");
        if (permissionRoot != null) {
            for (String key : permissionRoot.getKeys(false)) {
                if (permissionList.contains(key)) {
                    permissionListSorted.add(key);
                }
            }
        }

        return permissionListSorted;
    }

    @NotNull
    public File getConfigFolder() {
        return paths.configFolder;
    }

    @NotNull
    public File getPluginsFolder() {
        return paths.pluginsFolder;
    }

    /**
     * Centralized paths for config storage.
     */
    static final class Paths {
        final File dataFolder;
        final File pluginsFolder;

        final File configFolder;
        final File outdatedFolder;
        final File legacySingleConfig;

        final List<String> managedConfigFiles = List.of("config.yml", "entity.yml", "grave.yml");

        Paths(Graves plugin) {
            this.dataFolder = plugin.getDataFolder();
            this.pluginsFolder = dataFolder.getParentFile();

            this.configFolder = new File(dataFolder, "config");
            this.outdatedFolder = new File(dataFolder, "outdated");
            this.legacySingleConfig = new File(dataFolder, "config.yml");
        }
    }

    /**
     * YAML IO utilities (disk load only).
     */
    static final class IO {
        private final Graves plugin;

        IO(Graves plugin) {
            this.plugin = plugin;
        }

        @Nullable
        FileConfiguration loadYaml(@NotNull File file) {
            if (!YAMLUtil.isValidYAML(file)) return null;

            String jarResource = "config/" + file.getName();

            YAMLUtil.YamlParseError err = YAMLUtil.validateWithBukkit(plugin, file, jarResource);
            if (err != null) {
                YAMLUtil.logParseError(plugin, file.getName(), err);
                return null;
            }

            return YamlConfiguration.loadConfiguration(file);
        }

        @NotNull
        List<File> listFilesSorted(@NotNull File folder) {
            File[] files = folder.listFiles();
            if (files == null) return Collections.emptyList();

            Arrays.sort(files);
            return new ArrayList<>(Arrays.asList(files));
        }
    }

    /**
     * Applies defaults (from resources) and “bakes” defaults into the merged view.
     */
    static final class Defaults {
        private final Graves plugin;

        Defaults(Graves plugin, IO io) {
            this.plugin = plugin;
        }

        void applyResourceDefaults(@NotNull FileConfiguration cfg, @NotNull String resourcePath) {
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in == null) return;

                cfg.addDefaults(
                        YamlConfiguration.loadConfiguration(
                                new InputStreamReader(in, StandardCharsets.UTF_8)
                        )
                );
            } catch (IOException e) {
                plugin.logStackTrace(e);
            }
        }

        void bakeDefaults(@NotNull FileConfiguration cfg) {
            try {
                cfg.options().copyDefaults(true);
                cfg.loadFromString(cfg.saveToString());
            } catch (InvalidConfigurationException ignored) {
            }
        }

        /**
         * For folder-based resources you ship under /config/<filename>,
         * this applies defaults for that file name to the merged config.
         */
        void applyFolderResourceDefaults(@NotNull FileConfiguration merged, @NotNull String fileName) {
            String resource = "config" + File.separator + fileName;
            try (InputStream in = plugin.getResource(resource)) {
                if (in == null) return;

                merged.addDefaults(
                        YamlConfiguration.loadConfiguration(
                                new InputStreamReader(in, StandardCharsets.UTF_8)
                        )
                );
            } catch (IOException e) {
                plugin.logStackTrace(e);
            }
        }
    }

    /**
     * Loads folder-based config by recursively merging YAML files and applying shipped defaults.
     */
    static final class Merger {
        private final Graves plugin;
        private final IO io;
        private final Defaults defaults;

        Merger(Graves plugin, IO io) {
            this.plugin = plugin;
            this.io = io;
            this.defaults = new Defaults(plugin, io);
        }

        @NotNull
        FileConfiguration loadMergedFolderConfig(@NotNull File folder) {
            FileConfiguration result = new YamlConfiguration();

            List<File> files = io.listFilesSorted(folder);
            if (files.isEmpty()) return result;

            File mainConfig = new File(folder, "config.yml");
            if (files.contains(mainConfig)) {
                files.remove(mainConfig);
                files.add(0, mainConfig);
            }

            for (File file : files) {
                if (!YAMLUtil.isValidYAML(file)) continue;

                if (file.isDirectory()) {
                    result.addDefaults(loadMergedFolderConfig(file));
                    continue;
                }

                FileConfiguration loaded = io.loadYaml(file);
                if (loaded == null) {
                    plugin.getLogger().warning("Unable to load config " + file.getName() + ". Plugin may not function correctly. Do not report as a bug. Correct the following issues above.");
                    continue;
                }

                result.addDefaults(loaded);
                defaults.bakeDefaults(result);

                defaults.applyFolderResourceDefaults(result, file.getName());
            }

            return result;
        }
    }

    /**
     * Handles config-version checking, backups, and ConfigUpdater merges.
     */
    static final class Updater {
        private final Graves plugin;
        private final Paths paths;

        Updater(Graves plugin, Paths paths) {
            this.plugin = plugin;
            this.paths = paths;
        }

        void updateIfNeeded(boolean isDevelopmentBuild) {
            File configFolder = paths.configFolder;
            File mainConfigFile = new File(configFolder, "config.yml");

            if (!configFolder.exists()) {
                return;
            }

            FileConfiguration mainConfig = YamlConfiguration.loadConfiguration(mainConfigFile);
            int configVersion = mainConfig.getInt("config-version", 0);

            if (configVersion != CURRENT_CONFIG_VERSION || isDevelopmentBuild) {
                paths.outdatedFolder.mkdirs();
                backupOutdatedConfigs(configVersion);

                plugin.getLogger().warning("Outdated config detected (v" + configVersion + "), current version is (v"
                        + CURRENT_CONFIG_VERSION + "). Moving old configs to outdated folder and generating new config files.");

                updateConfigFile("config.yml", CURRENT_CONFIG_VERSION, true);
                updateConfigFile("entity.yml", CURRENT_CONFIG_VERSION, false);
                updateConfigFile("grave.yml", CURRENT_CONFIG_VERSION, false);
            }
        }

        private void backupOutdatedConfigs(double configVersion) {
            for (String name : paths.managedConfigFiles) {
                File existing = new File(paths.configFolder, name);
                if (!existing.exists()) continue;

                File backup = new File(paths.outdatedFolder, name + "-" + configVersion);
                try {
                    Files.copy(existing.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    plugin.logStackTrace(e);
                }
            }
        }

        private void updateConfigFile(@NotNull String fileName, int currentConfigVersion, boolean shouldUpdateConfigVersion) {
            File configFile = new File(paths.dataFolder, "config/" + fileName);
            if (!configFile.exists()) {
                plugin.getLogger().severe("File " + configFile.getAbsolutePath() + " does not exist.");
                return;
            }

            String resourceName = "config/" + fileName;

            try (InputStream resourceStream = plugin.getResource(resourceName)) {
                if (resourceStream == null) {
                    plugin.getLogger().warning("Resource " + resourceName + " not found in the JAR.");
                    return;
                }

                ConfigUpdater.update(plugin, resourceName, configFile, Collections.emptyList());

                if (shouldUpdateConfigVersion) {
                    FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
                    cfg.set("config-version", currentConfigVersion);
                    cfg.save(configFile);
                }

                plugin.getLogger().info("Config updated: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to update " + fileName + ": " + e.getMessage());
                plugin.logStackTrace(e);
            }
        }
    }
}
