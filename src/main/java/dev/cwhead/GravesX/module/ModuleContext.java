package dev.cwhead.GravesX.module;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.util.LibraryImporter;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Provides services and utilities to a single module: data folder, logging,
 * config handling, resource I/O, event/task/service registration, and cleanup.
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
     * @param plugin Owning Graves plugin.
     * @param moduleName Module name used for paths and messages.
     * @param moduleClassLoader Class loader that serves module resources.
     * @param importer Library importer used by {@link #importLibrary(String)}.
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
                try { jarFile.close(); } catch (IOException ignored) {}
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
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            if (e.isDirectory()) continue;

            String name = e.getName();
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (!lower.endsWith(".yml")) continue;

            String last = name.substring(name.lastIndexOf('/') + 1);
            if ("module.yml".equalsIgnoreCase(last)) continue;

            Path outPath = baseDir.resolve(name);
            try {
                Path parent = outPath.getParent();
                if (parent != null) Files.createDirectories(parent);

                // Security: ensure resolved path is inside baseDir
                if (!outPath.toRealPath().startsWith(baseDir.toRealPath())) {
                    logger.warning("[Modules] Skipping suspicious path in JAR: " + name);
                    continue;
                }

                if (Files.exists(outPath)) continue;

                try (InputStream in = jarFile.getInputStream(e)) {
                    Files.copy(in, outPath);
                }
            } catch (Exception copyEx) {
                logger.warning("[Modules] Failed to write default file " + name + " for " + moduleName + ": " + copyEx.getMessage());
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
        try { config.save(configFile); }
        catch (Exception e) { logger.warning("[Modules] Failed to save config for " + moduleName + ": " + e.getMessage()); }
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
     * @param path Resource path inside the jar.
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
     * @param <T> Listener type.
     * @return The same listener for chaining.
     */
    public <T extends Listener> T registerListener(T listener) {
        org.bukkit.Bukkit.getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
        return listener;
    }

    private Runnable guard(final Runnable r) {
        return () -> { if (!disabling) r.run(); };
    }

    /**
     * Schedules a synchronous task using the GravesX scheduler.
     *
     * @param r Task to run.
     */
    public void runTask(Runnable r) {
        plugin.getGravesXScheduler().runTask(guard(r));
    }

    /**
     * Schedules a delayed synchronous task using the GravesX scheduler.
     *
     * @param r Task to run.
     * @param delay Delay in ticks before first run.
     */
    public void runTaskLater(Runnable r, long delay) {
        plugin.getGravesXScheduler().runTaskLater(guard(r), delay);
    }

    /**
     * Schedules a repeating synchronous task using the GravesX scheduler.
     *
     * @param r Task to run.
     * @param delay Delay in ticks before first run.
     * @param period Period in ticks between runs.
     */
    public void runTaskTimer(Runnable r, long delay, long period) {
        plugin.getGravesXScheduler().runTaskTimer(guard(r), delay, period);
    }

    /**
     * Schedules an asynchronous task using the GravesX scheduler.
     *
     * @param r Task to run.
     */
    public void runTaskAsync(Runnable r) {
        plugin.getGravesXScheduler().runTaskAsynchronously(guard(r));
    }

    /**
     * Schedules a repeating asynchronous task using the GravesX scheduler.
     *
     * @param r Task to run.
     * @param delay Delay in ticks before first run.
     * @param period Period in ticks between runs.
     */
    public void runTaskTimerAsync(Runnable r, long delay, long period) {
        plugin.getGravesXScheduler().runTaskTimerAsynchronously(guard(r), delay, period);
    }

    /**
     * Registers a Bukkit service and tracks it for automatic unregister.
     *
     * @param service Service interface class.
     * @param provider Service implementation instance.
     * @param prio Registration priority.
     */
    public <T> void registerService(Class<T> service, T provider, ServicePriority prio) {
        org.bukkit.Bukkit.getServicesManager().register(service, provider, plugin, prio);
        services.add(new ServiceReg(service, provider));
    }

    /**
     * Registers a closeable resource to be closed during cleanup.
     *
     * @param closeable Resource to track.
     * @param <T> Resource type.
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
        for (Runnable r : snapshot(shutdownHooks)) { safeRun(r); }
        shutdownHooks.clear();

        for (Listener l : snapshot(listeners)) { HandlerList.unregisterAll(l); }
        listeners.clear();

        for (ServiceReg reg : snapshot(services)) {
            try { org.bukkit.Bukkit.getServicesManager().unregister(reg.type, reg.provider); } catch (Throwable ignored) {}
        }
        services.clear();

        for (AutoCloseable c : snapshot(closeables)) {
            try { c.close(); } catch (Throwable ignored) {}
        }
        closeables.clear();
    }

    /**
     * Internal: attaches a per-module controller (wired by the GravesXModuleController).
     */
    void _internalAttachController(GravesXModuleController controller) {
        this.controller = controller;
    }

    /**
     * Exposes the per-module controller for enable/disable/isEnabled access.
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
        Files.writeString(file, contents, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}