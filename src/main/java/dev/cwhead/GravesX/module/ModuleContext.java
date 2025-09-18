package dev.cwhead.GravesX.module;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.util.LibraryImporter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private final List<ServiceReg> services = new CopyOnWriteArrayList<ServiceReg>();
    private final List<AutoCloseable> closeables = new CopyOnWriteArrayList<AutoCloseable>();
    private final List<Runnable> shutdownHooks = new CopyOnWriteArrayList<Runnable>();
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
        final File baseDir = configFile.getParentFile();
        if (!baseDir.exists()) baseDir.mkdirs();

        java.util.jar.JarFile jarFile = null;
        try {
            try {
                java.net.URL marker = moduleClassLoader.getResource("module.yml");
                if (marker != null && "jar".equalsIgnoreCase(marker.getProtocol())) {
                    java.net.JarURLConnection juc = (java.net.JarURLConnection) marker.openConnection();
                    jarFile = juc.getJarFile();
                }
            } catch (Throwable ignored) {}

            if (jarFile == null && moduleClassLoader instanceof java.net.URLClassLoader) {
                for (java.net.URL u : ((java.net.URLClassLoader) moduleClassLoader).getURLs()) {
                    try {
                        java.io.File f = new java.io.File(u.toURI());
                        if (f.isFile() && f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                            jarFile = new JarFile(f);
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (jarFile != null) {
                java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry e = entries.nextElement();
                    if (e.isDirectory()) continue;

                    String name = e.getName();
                    String lower = name.toLowerCase(java.util.Locale.ROOT);
                    if (!lower.endsWith(".yml")) continue;

                    String last = name.substring(name.lastIndexOf('/') + 1);
                    if ("module.yml".equalsIgnoreCase(last)) continue;

                    java.io.File outFile = new java.io.File(baseDir, name);

                    String basePath = baseDir.getCanonicalPath();
                    String outPath  = outFile.getCanonicalFile().getParentFile().getCanonicalPath();
                    if (!outPath.startsWith(basePath)) {
                        logger.warning("[Modules] Skipping suspicious path in JAR: " + name);
                        continue;
                    }

                    if (outFile.exists()) continue;

                    java.io.File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();

                    try (java.io.InputStream in = jarFile.getInputStream(e);
                         java.io.OutputStream out = new java.io.FileOutputStream(outFile)) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                    } catch (Exception copyEx) {
                        logger.warning("[Modules] Failed to write default file " + name + " for " + moduleName + ": " + copyEx.getMessage());
                    }
                }
            } else {
                if (!configFile.exists()) {
                    try (java.io.InputStream in = moduleClassLoader.getResourceAsStream("config.yml")) {
                        if (in != null) {
                            try (java.io.OutputStream out = new java.io.FileOutputStream(configFile)) {
                                byte[] buffer = new byte[8192];
                                int r;
                                while ((r = in.read(buffer)) != -1) out.write(buffer, 0, r);
                            }
                        }
                    } catch (Exception ioe) {
                        logger.warning("[Modules] Failed to write default config for " + moduleName + ": " + ioe.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            logger.warning("[Modules] Failed extracting default YAMLs for " + moduleName + ": " + ex.getMessage());
        } finally {
            try { if (jarFile != null) jarFile.close(); } catch (Exception ignored) {}
        }

        // Ensure there is at least a config.yml (copy or stub)
        if (!configFile.exists()) {
            try {
                saveString(configFile, "# Auto-generated config for " + moduleName + System.lineSeparator());
            } catch (Exception ioe) {
                logger.warning("[Modules] Failed to write default config for " + moduleName + ": " + ioe.getMessage());
            }
        }

        reloadConfig();
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
        File outFile = new File(dataFolder, path);
        if (outFile.exists() && !replace) return;
        File parent = outFile.getParentFile();
        if (parent != null) parent.mkdirs();
        try (InputStream in = moduleClassLoader.getResourceAsStream(path)) {
            if (in == null) {
                logger.warning("[Modules] Resource not found in module JAR: " + path);
                return;
            }
            try (OutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int r;
                while ((r = in.read(buffer)) != -1) {
                    out.write(buffer, 0, r);
                }
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
        return new Runnable() {
            public void run() {
                if (!disabling) r.run();
            }
        };
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

    /** Internal: attaches a per-module controller (wired by the GravesXModuleController). */
    void _internalAttachController(GravesXModuleController controller) {
        this.controller = controller;
    }

    /** Exposes the per-module controller for enable/disable/isEnabled access. */
    public GravesXModuleController getGravesXModules() {
        return controller;
    }

    private static <T> List<T> snapshot(List<T> list) {
        return new ArrayList<T>(list);
    }

    private static void safeRun(Runnable r) {
        try {
            r.run();
        } catch (Throwable ignored) {

        }
    }

    private static void saveString(File file, String contents) throws Exception {
        try (Writer w = new FileWriter(file, StandardCharsets.UTF_8)) {
            w.write(contents);
        }
    }
}