package dev.cwhead.GravesX.module;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.util.LibraryImporter;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Provides services and utilities to a single module: data folder, logging,
 * config handling, resource I/O, event/task/service registration, and cleanup.
 *
 * <p>When running on Folia, scheduling helpers delegate to the GravesX scheduler,
 * which is expected to provide a Folia-aware implementation (e.g. region threads).</p>
 */
public final class ModuleContext {
    private final Graves plugin;
    private final String moduleName;
    private final ClassLoader moduleClassLoader;
    private final File dataFolder;
    private final Logger logger;
    private final LibraryImporter importer;

    private final File configFile;
    private YamlConfiguration config;

    private volatile boolean disabling = false;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final List<ServiceReg> services = new CopyOnWriteArrayList<>();
    private final List<AutoCloseable> closeables = new CopyOnWriteArrayList<>();
    private final List<Runnable> shutdownHooks = new CopyOnWriteArrayList<>();
    private volatile GravesXModuleController controller;

    private static final class ServiceReg {
        final Class<?> type;
        final Object provider;

        ServiceReg(Class<?> type, Object provider) {
            this.type = type;
            this.provider = provider;
        }
    }

    /**
     * Creates a context for a module and prepares its storage and config.
     *
     * @param plugin            Owning Graves plugin.
     * @param moduleName        Module name used for paths and messages.
     * @param moduleClassLoader Class loader that serves module resources.
     * @param importer          Library importer used by {@link #importLibrary(String)}.
     */
    public ModuleContext(Graves plugin,
                         String moduleName,
                         ClassLoader moduleClassLoader,
                         LibraryImporter importer) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
        this.moduleClassLoader = Objects.requireNonNull(moduleClassLoader, "moduleClassLoader");
        this.importer = importer;
        this.logger = plugin.getLogger();
        this.dataFolder = new File(plugin.getDataFolder(), "modules" + File.separator + moduleName);
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("[Modules] Could not create data folder for " + moduleName + " at " + dataFolder.getPath());
        }
        this.configFile = new File(dataFolder, "config.yml");
    }

    /**
     * Gets the owning Graves plugin.
     *
     * @return Plugin instance.
     */
    public Graves getPlugin() {
        return plugin;
    }

    /**
     * Gets this module's name.
     *
     * @return Module name.
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Gets the module-specific data folder.
     *
     * @return Data folder path.
     */
    public File getDataFolder() {
        return dataFolder;
    }

    /**
     * Gets the logger to use for this module.
     *
     * @return Logger instance.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Gets the module's class loader.
     *
     * @return Class loader serving module resources.
     */
    public ClassLoader getClassLoader() {
        return moduleClassLoader;
    }

    /**
     * Opens an embedded resource from the module JAR using the module class loader.
     *
     * @param path resource path inside the module JAR
     * @return input stream for the resource, or {@code null} if not found.
     */
    public InputStream getResource(String path) {
        Objects.requireNonNull(path, "path");
        return moduleClassLoader.getResourceAsStream(path);
    }

    /**
     * Opens an embedded text resource as UTF-8.
     *
     * @param path resource path inside the module JAR
     * @return reader for the resource, or {@code null} if not found.
     */
    public Reader getTextResource(String path) {
        InputStream in = getResource(path);
        return (in == null ? null : new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * Resolves a file under this module's data folder.
     *
     * @param relativePath path relative to {@link #getDataFolder()}
     * @return resolved file
     */
    public File resolveFile(String relativePath) {
        Objects.requireNonNull(relativePath, "relativePath");
        return new File(dataFolder, relativePath);
    }

    /**
     * Ensures a subfolder exists under the module data folder and returns it.
     *
     * @param name subfolder name (e.g. "languages", "cache")
     * @return the subfolder
     */
    public File ensureSubfolder(String name) {
        Objects.requireNonNull(name, "name");
        File f = new File(dataFolder, name);
        if (!f.exists() && !f.mkdirs()) {
            logger.warning("[Modules] Could not create subfolder '" + name + "' for " + moduleName + " at " + f.getPath());
        }
        return f;
    }

    /**
     * Loads a YAML file from the module data folder.
     *
     * @param relativePath path relative to data folder (e.g. "languages/en_us.yml")
     */
    public YamlConfiguration loadYaml(String relativePath) {
        File file = resolveFile(relativePath);
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Saves a YAML configuration to the module data folder.
     *
     * @param relativePath path relative to data folder
     * @param yaml         configuration to save
     */
    public void saveYaml(String relativePath, YamlConfiguration yaml) {
        Objects.requireNonNull(yaml, "yaml");
        File file = resolveFile(relativePath);
        try {
            yaml.save(file);
        } catch (IOException e) {
            logger.warning("[Modules] Failed to save YAML '" + relativePath + "' for " + moduleName + ": " + e.getMessage());
        }
    }

    /**
     * Whether this module declares Folia support in {@code module.yml} via {@code supportsFolia: true}.
     *
     * <p>This is a convenience that forwards to the underlying {@link GravesXModuleDescriptor}
     * via the module controller. Returns {@code false} if the controller or descriptor is
     * not yet attached.</p>
     *
     * @return {@code true} if the module descriptor reports Folia support; {@code false} otherwise
     */
    public boolean supportsFolia() {
        GravesXModuleController c = controller;
        return c != null && c.supportsFolia();
    }

    /**
     * Copies all default YAML resources (except module.yml) from the module JAR into this module's
     * data folder, preserving subfolders. Existing files are not overwritten. Ensures a config.yml
     * exists (copy or stub), then reloads the config.
     */
    public void saveDefaultConfig() {
        try {
            Path baseDir = configFile.getParentFile().toPath();
            Files.createDirectories(baseDir);

            JarFile jarFile = findModuleJar();
            if (jarFile != null) {
                extractYamlEntries(jarFile, baseDir);
                try {
                    jarFile.close();
                } catch (IOException ignored) {}
            } else {
                // Fallback: copy a top-level config.yml resource if present
                if (!configFile.exists()) {
                    try (InputStream in = moduleClassLoader.getResourceAsStream("config.yml")) {
                        if (in != null) {
                            Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.warning("[Modules] Failed extracting default YAMLs for " + moduleName + ": " + ex.getMessage());
        }

        if (!configFile.exists()) {
            try {
                saveString(configFile.toPath(), "# Auto-generated config for " + moduleName + System.lineSeparator());
            } catch (Exception ioe) {
                logger.warning("[Modules] Failed to write default config for " + moduleName + ": " + ioe.getMessage());
            }
        }

        reloadConfig();
    }

    private JarFile findModuleJar() {
        try {
            URL marker = moduleClassLoader.getResource("module.yml");
            if (marker != null && "jar".equalsIgnoreCase(marker.getProtocol())) {
                JarURLConnection juc = (JarURLConnection) marker.openConnection();
                return juc.getJarFile();
            }
        } catch (Throwable ignored) {
        }

        if (moduleClassLoader instanceof URLClassLoader urlCl) {
            for (URL u : urlCl.getURLs()) {
                try {
                    Path p = Paths.get(u.toURI());
                    if (Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".jar")) {
                        return new JarFile(p.toFile());
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private void extractYamlEntries(JarFile jarFile, Path baseDir) {
        Path baseAbs = baseDir.toAbsolutePath().normalize();

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            if (e.isDirectory()) continue;

            String name = e.getName();
            String lower = name.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".yml")) continue;

            String last = name.substring(name.lastIndexOf('/') + 1);
            if ("module.yml".equalsIgnoreCase(last)) continue;

            try {
                Path target = baseAbs.resolve(name).normalize();

                if (!target.startsWith(baseAbs)) {
                    logger.warning("[Modules] Skipping suspicious path in JAR: " + name);
                    continue;
                }

                if (Files.exists(target)) continue;

                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);

                try (InputStream in = jarFile.getInputStream(e)) {
                    Files.copy(in, target);
                }

                if (Files.isSymbolicLink(target)) {
                    Files.delete(target);
                    logger.warning("[Modules] Skipped symlink file: " + name);
                }

            } catch (Exception ex) {
                logger.warning("[Modules] Failed to write default file " + name + " for " + moduleName + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Gets the YAML configuration, loading it if not already loaded.
     *
     * @return Configuration handle.
     */
    public FileConfiguration getConfig() {
        if (config == null) reloadConfig();
        return config;
    }

    /**
     * Saves the current configuration to disk.
     */
    public void saveConfig() {
        if (config == null) return;
        try {
            config.save(configFile);
        } catch (Exception e) {
            logger.warning("[Modules] Failed to save config for " + moduleName + ": " + e.getMessage());
        }
    }

    /**
     * Reloads the configuration from disk and applies default values from resources.
     */
    public void reloadConfig() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        try (InputStream def = moduleClassLoader.getResourceAsStream("config.yml")) {
            if (def != null) {
                YamlConfiguration defCfg = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(def, StandardCharsets.UTF_8));
                cfg.setDefaults(defCfg);
                cfg.options().copyDefaults(true);
            }
        } catch (Exception ignored) {}
        this.config = cfg;
    }

    /**
     * Saves an embedded resource from the module jar into the module data folder.
     *
     * @param path    Resource path inside the jar.
     * @param replace If true, overwrites an existing file.
     */
    public void saveResource(String path, boolean replace) {
        Path outPath = dataFolder.toPath().resolve(path);
        if (Files.exists(outPath) && !replace) return;
        try {
            Path parent = outPath.getParent();
            if (parent != null) Files.createDirectories(parent);

            try (InputStream in = moduleClassLoader.getResourceAsStream(path)) {
                if (in == null) {
                    logger.warning("[Modules] Resource not found in module JAR: " + path);
                    return;
                }
                Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            logger.warning("[Modules] Failed saving resource " + path + " for " + moduleName + ": " + e.getMessage());
        }
    }

    /**
     * Registers an event listener and tracks it for automatic cleanup.
     *
     * @param listener Listener to register.
     * @param <T>      Listener type.
     * @return The same listener for chaining.
     */
    public <T extends Listener> T registerListener(T listener) {
        Objects.requireNonNull(listener, "listener");
        org.bukkit.Bukkit.getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
        return listener;
    }

    /**
     * Unregisters a previously registered listener and removes it from tracking.
     *
     * @param listener Listener to unregister.
     */
    public void unregisterListener(Listener listener) {
        if (listener == null) return;
        HandlerList.unregisterAll(listener);
        listeners.remove(listener);
    }

    /**
     * Unregisters all listeners registered via this context.
     */
    private void unregisterAllListeners() {
        for (Listener listener : snapshot(listeners)) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
    }

    private Runnable guard(final Runnable r) {
        return () -> {
            if (!disabling) r.run();
        };
    }

    /**
     * Schedules a synchronous task using the GravesX scheduler.
     *
     * @param r Task to run.
     */
    public void runTask(Runnable r) {
        plugin.getSchedulerManager().runTask(guard(r));
    }

    /**
     * Schedules a delayed synchronous task using the GravesX scheduler.
     *
     * @param r     Task to run.
     * @param delay Delay in ticks before first run.
     */
    public void runTaskLater(Runnable r, long delay) {
        plugin.getSchedulerManager().runTaskLater(guard(r), delay);
    }

    /**
     * Schedules a repeating synchronous task using the GravesX scheduler.
     *
     * @param r      Task to run.
     * @param delay  Delay in ticks before first run.
     * @param period Period in ticks between runs.
     */
    public void runTaskTimer(Runnable r, long delay, long period) {
        plugin.getSchedulerManager().runTaskTimer(guard(r), delay, period);
    }

    /**
     * Schedules an asynchronous task using the GravesX scheduler.
     *
     * @param r Task to run.
     */
    public void runTaskAsync(Runnable r) {
        plugin.getSchedulerManager().runTaskAsynchronously(guard(r));
    }

    /**
     * Schedules a repeating asynchronous task using the GravesX scheduler.
     *
     * @param r      Task to run.
     * @param delay  Delay in ticks before first run.
     * @param period Period in ticks between runs.
     */
    public void runTaskTimerAsync(Runnable r, long delay, long period) {
        plugin.getSchedulerManager().runTaskTimerAsynchronously(guard(r), delay, period);
    }

    /**
     * Executes a task on the region thread associated with the given location
     * using the GravesX scheduler.
     *
     * <p>On Folia, this should run the task on the appropriate region thread.
     * On non-Folia servers, the scheduler may fall back to the main thread or
     * another compatible implementation.</p>
     *
     * @param location Location whose region thread should be used.
     * @param r        Task to run.
     */
    public void executeRegion(Location location, Runnable r) {
        if (location == null || r == null) return;
        plugin.getSchedulerManager().execute(location, guard(r));
    }

    /**
     * Registers a Bukkit service and tracks it for automatic unregister.
     *
     * @param service  Service interface class.
     * @param provider Service implementation instance.
     * @param prio     Registration priority.
     */
    public <T> void registerService(Class<T> service, T provider, ServicePriority prio) {
        org.bukkit.Bukkit.getServicesManager().register(service, provider, plugin, prio);
        services.add(new ServiceReg(service, provider));
    }

    /**
     * Registers a closeable resource to be closed during cleanup.
     *
     * @param closeable Resource to track.
     * @param <T>       Resource type.
     * @return The same resource for chaining.
     */
    public <T extends AutoCloseable> T registerCloseable(T closeable) {
        closeables.add(closeable);
        return closeable;
    }

    /**
     * Adds a hook to be invoked during cleanup.
     *
     * @param hook Runnable to execute on shutdown.
     */
    public void addShutdownHook(Runnable hook) {
        shutdownHooks.add(Objects.requireNonNull(hook, "hook"));
    }

    /**
     * Imports external libraries for this module using the configured importer.
     *
     * @param coordinates One or more coordinates (implementation-defined).
     */
    public void importLibrary(String coordinates) {
        if (importer != null) {
            importer.importLibrary(this, coordinates);
        } else {
            logger.info("[Modules] importLibrary() called for " + moduleName + " (no importer configured yet).");
        }
    }

    /**
     * Marks the context as disabling to guard scheduled tasks from running.
     */
    void _internalPreDisable() {
        disabling = true;
    }

    /**
     * Unregisters listeners/services, closes resources, and runs hooks.
     */
    void _internalCleanup() {
        for (Runnable r : snapshot(shutdownHooks)) {
            safeRun(r);
        }
        shutdownHooks.clear();

        unregisterAllListeners();

        for (ServiceReg reg : snapshot(services)) {
            try {
                Bukkit.getServicesManager().unregister(reg.type, reg.provider);
            } catch (Throwable ignored) {}
        }
        services.clear();

        for (AutoCloseable c : snapshot(closeables)) {
            try {
                c.close();
            } catch (Throwable ignored) {}
        }
        closeables.clear();
    }

    /**
     * Internal: attaches a per-module controller (wired by the module manager).
     */
    void _internalAttachController(GravesXModuleController controller) {
        this.controller = controller;
    }

    /**
     * Exposes the per-module controller for enable/disable/isEnabled/Folia access.
     *
     * @return controller instance, or {@code null} if not yet attached
     */
    public GravesXModuleController getGravesXModules() {
        return controller;
    }

    private static <T> List<T> snapshot(List<T> list) {
        return new ArrayList<>(list);
    }

    private static void safeRun(Runnable r) {
        try {
            r.run();
        } catch (Throwable ignored) {
        }
    }

    private static void saveString(Path file, String contents) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}