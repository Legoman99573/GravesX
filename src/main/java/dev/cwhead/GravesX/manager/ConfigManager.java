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
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
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
    private volatile Map<String, ConfigurationSection> playerOverrides = Collections.emptyMap();

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
            playerOverrides = loadPlayerOverrides();
            registerOverridePermissions(playerOverrides.keySet());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads independent, optional per-player override files; these are not merged globally.
     */
    private Map<String, ConfigurationSection> loadPlayerOverrides() {
        File folder = new File(paths.configFolder, "override");

        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create the GravesX override folder");
        }

        File example = new File(folder, "overrides.yml.example");

        String exampleText = "# Add settings under settings.default.<name>.\n"
                + "# Grant permission graves.override.<name> or graves.permission<name> to the player.\n"
                + "# Available settings are documented in config/grave.yml and will override the defaults\n"
                + "settings:\n"
                + "  default:\n"
                + "    premium:\n"
                + "      grave:\n"
                + "        priority: 10\n"
                + "        time: -1\n"
                + "      experience:\n"
                + "        store: 1.0\n";

        try {
            Files.writeString(example.toPath(), exampleText, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not refresh override example: " + e.getMessage());
        }

        File[] files = folder.listFiles(f -> f.isFile() && f.getName().endsWith(".yml"));

        if (files == null)
            return Collections.emptyMap();

        Arrays.sort(files, Comparator.comparing(File::getName));

        Map<String, ConfigurationSection> overrides = new LinkedHashMap<>();
        for (File file : files) {
            try {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.load(file);

                ConfigurationSection root = yaml.getConfigurationSection("settings.default");
                if (root == null) {
                    plugin.getLogger().warning("Override " + file.getName() + " must have a settings.default section");
                    continue;
                }

                for (String key : root.getKeys(false)) {
                    String name = key.toLowerCase(Locale.ROOT);
                    if (!name.matches("[a-z0-9_-]+") || !key.equals(name)) {
                        plugin.getLogger().warning("Invalid override name '" + key + "' in " + file.getName() + "; use lowercase letters, digits, _ or -");
                        continue;
                    }

                    ConfigurationSection section = root.getConfigurationSection(key);
                    if (section == null) {
                        plugin.getLogger().warning("Override '" + name + "' must be a YAML section in " + file.getName());
                        continue;
                    }

                    if (overrides.containsKey(name)) {
                        plugin.getLogger().warning("Duplicate override name '" + name
                                + "' in " + file.getName() + "; keeping the first definition");
                        continue;
                    }

                    overrides.put(name, section);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid override file " + file.getName() + ": " + e.getMessage());
            }
        }
        return Collections.unmodifiableMap(overrides);
    }

    /**
     * Explicit opt-in permission nodes for every configured override.
     */
    private void registerOverridePermissions(Set<String> names) {
        for (String name : names) {
            registerOptInPermission("graves.override." + name);
            registerOptInPermission("graves.permission." + name);
        }
    }

    private void registerOptInPermission(String node) {
        Permission permission = Bukkit.getPluginManager().getPermission(node);
        if (permission == null) {
            Bukkit.getPluginManager().addPermission(new Permission(node, PermissionDefault.FALSE));
        } else if (permission.getDefault() != PermissionDefault.FALSE) {
            permission.setDefault(PermissionDefault.FALSE);
            permission.recalculatePermissibles();
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
     *     <li>Named overrides selected by {@code graves.override.<name>} or legacy {@code graves.permission.<name>}</li>
     *     <li>Existing {@code settings.permission.<perm>} legacy sections, if present</li>
     *     <li>{@code settings.entity.<ENTITYTYPE>}</li>
     *     <li>{@code settings.default.default}</li>
     * </ol>
     */
    @Nullable
    public ConfigurationSection getConfigSection(String configKey, @Nullable EntityType entityType, @Nullable List<String> permissionList) {
        FileConfiguration cfg = config();
        ConfigurationSection base = null;

        if (permissionList != null) {
            for (String permission : permissionList) {
                if (permission.startsWith("override:")) continue;
                ConfigurationSection candidate = cfg.getConfigurationSection("settings.permission." + permission);
                if (candidate != null && contains(candidate, configKey)) {
                    base = candidate;
                    break;
                }
            }
        }
        if (base == null && entityType != null) {
            ConfigurationSection candidate = cfg.getConfigurationSection("settings.entity." + entityType.name());
            if (candidate != null && contains(candidate, configKey))
                base = candidate;
        }
        if (base == null)
            base = cfg.getConfigurationSection("settings.default.default");

        if (permissionList != null) {
            for (String permission : permissionList) {
                if (!permission.startsWith("override:")) continue;
                ConfigurationSection override = playerOverrides.get(permission.substring("override:".length()));
                if (override == null) continue;
                if (!contains(override, configKey)) break;
                YamlConfiguration resolved = new YamlConfiguration();
                if (base != null) {
                    for (String key : base.getKeys(true)) {
                        if (!base.isConfigurationSection(key))
                            resolved.set(key, base.get(key));
                    }
                }
                for (String key : override.getKeys(true)) {
                    if (!override.isConfigurationSection(key))
                        resolved.set(key, override.get(key));
                }
                return resolved;
            }
        }
        return base;
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

            if (!info.getValue())
                continue;

            if (perm.startsWith("graves.permission.")) {
                String key = perm.substring("graves.permission.".length()).toLowerCase(Locale.ROOT);
                if (playerOverrides.containsKey(key)) {
                    permissionListSorted.add("override:" + key);
                }
                if (cfg.isConfigurationSection("settings.permission." + key)) {
                    permissionList.add(key);
                }
            }
        }

        String selectedOverride = null;
        int highestPriority = Math.max(0, cfg.getInt("settings.default.default.grave.priority", 0));
        for (Map.Entry<String, ConfigurationSection> entry : playerOverrides.entrySet()) {
            String name = entry.getKey();
            if (!player.hasPermission("graves.override." + name) && !player.hasPermission("graves.permission." + name))
                continue;

            int priority = entry.getValue().getInt("grave.priority", 0);
            if (priority < 0) {
                plugin.getLogger().warning("Override '" + name + "' has negative grave.priority; ignoring it");
                continue;
            }

            if (priority > highestPriority || (priority == highestPriority && selectedOverride != null
                    && name.compareTo(selectedOverride) < 0)) {
                highestPriority = priority;
                selectedOverride = name;
            }
        }

        permissionListSorted.removeIf(p -> p.startsWith("override:"));
        if (selectedOverride != null)
            permissionListSorted.add("override:" + selectedOverride);

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
                    if (file.getName().equals("override"))
                        continue;

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
