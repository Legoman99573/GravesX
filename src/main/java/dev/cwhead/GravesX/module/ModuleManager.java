package dev.cwhead.GravesX.module;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.util.LibraryImporter;
import dev.cwhead.GravesX.module.util.ModuleClassLoader;
import dev.cwhead.GravesX.module.util.ModuleInfo;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * Manages GravesX modules: discovers, loads, resolves order, and enables/disables them.
 */
public final class ModuleManager {
    private final Graves plugin;
    private final File modulesDir;

    private LibraryImporter importer;
    private final Map<String, LoadedModule> loaded = new LinkedHashMap<String, LoadedModule>();
    private final Set<String> pending = new LinkedHashSet<>();
    private List<String> topoOrder = List.of();
    private final ModuleCommandRegistrar commandRegistrar;

    /**
     * Holds a loaded module instance and its metadata.
     */
    public static final class LoadedModule {
        /** Module descriptor parsed from module.yml. */
        public final ModuleInfo info;
        /** Class loader used to load the module jar. */
        public final ModuleClassLoader cl;
        /** Module main instance. */
        public final GravesXModule instance;
        /** Runtime context provided to the module. */
        public final ModuleContext context;
        /** Whether the module is currently enabled. */
        public boolean enabled;

        /**
         * Creates a loaded module bundle.
         *
         * @param info Module metadata.
         * @param cl Class loader for the module.
         * @param instance Module main instance.
         * @param ctx Module runtime context.
         */
        LoadedModule(ModuleInfo info, ModuleClassLoader cl, GravesXModule instance, ModuleContext ctx) {
            this.info = info;
            this.cl = cl;
            this.instance = instance;
            this.context = ctx;
        }
    }

    /**
     * Creates a manager bound to the given plugin and ensures the modules directory exists.
     *
     * @param plugin Owning plugin.
     */
    public ModuleManager(Graves plugin) {
        this.plugin = plugin;
        this.modulesDir = new File(plugin.getDataFolder(), "modules");
        if (!modulesDir.exists()) modulesDir.mkdirs();
        this.commandRegistrar = new ModuleCommandRegistrar(plugin);
    }

    /**
     * Sets the library importer used by modules to resolve external libraries.
     *
     * @param importer Library importer to use.
     */
    public void setLibraryImporter(LibraryImporter importer) { this.importer = importer; }

    /**
     * Gets all loaded modules.
     *
     * @return Unmodifiable view of loaded modules.
     */
    public Collection<LoadedModule> modules() { return Collections.unmodifiableCollection(loaded.values()); }

    /**
     * Looks up a loaded module by name.
     *
     * @param name Module name.
     * @return Optional containing the module if present.
     */
    public Optional<LoadedModule> get(String name) { return Optional.ofNullable(loaded.get(name)); }

    /**
     * Returns the computed topological load order.
     *
     * @return List of module names in load order.
     */
    public List<String> order() { return topoOrder; }

    /**
     * Returns the set of modules waiting on missing requirements.
     *
     * @return Unmodifiable set of pending module names.
     */
    public Set<String> pending() { return Collections.unmodifiableSet(pending); }

    /**
     * Scans the modules directory, validates jars, loads metadata, constructs instances, and calls onModuleLoad.
     * Also computes the topological order after loading descriptors.
     */
    public void loadAll() {
        File[] jars = modulesDir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            plugin.getLogger().info("[Modules] No module jars found in " + modulesDir.getPath());
            return;
        }
        Arrays.sort(jars);
        for (File jar : jars) {
            try (JarFile jf = new JarFile(jar)) {
                JarEntry entry = jf.getJarEntry("module.yml");
                if (entry == null) { warn("Skipping " + jar.getName() + " (missing module.yml)"); continue; }

                ModuleInfo info;
                try (InputStream in = jf.getInputStream(entry)) { info = ModuleInfo.fromYaml(in); }
                if (info.name() == null || info.mainClass() == null) {
                    warn("Skipping " + jar.getName() + " (missing name/main)");
                    continue;
                }
                if (loaded.containsKey(info.name())) {
                    warn("Duplicate module name " + info.name() + "; skipping " + jar.getName());
                    continue;
                }
                info("Loading " + info.name() + " version " + info.version());

                URL url = jar.toURI().toURL();
                ModuleClassLoader cl = new ModuleClassLoader(url, plugin.getClass().getClassLoader());
                Class<?> main = Class.forName(info.mainClass(), true, cl);
                if (!GravesXModule.class.isAssignableFrom(main)) {
                    cl.close(); warn(info.name() + " main does not implement Module"); continue;
                }

                GravesXModule instance = (GravesXModule) main.getDeclaredConstructor().newInstance();
                ModuleContext ctx = new ModuleContext(plugin, info.name(), cl, importer);
                ctx.saveDefaultConfig();
                instance.onModuleLoad(ctx);

                loaded.put(info.name(), new LoadedModule(info, cl, instance, ctx));
            } catch (Throwable t) {
                severe("Failed loading " + jar.getName(), t);
            }
        }
        buildTopoOrder();
    }

    /**
     * Computes a topological order across module dependencies and soft constraints.
     * Falls back to a partial order if cycles are detected.
     */
    private void buildTopoOrder() {
        Map<String, Set<String>> adj = new LinkedHashMap<String, Set<String>>();
        Map<String, Integer> indeg = new LinkedHashMap<String, Integer>();
        for (String n : loaded.keySet()) { adj.put(n, new LinkedHashSet<String>()); indeg.put(n, 0); }

        final class EdgeAdder { void add(String a, String b) {
            if (!loaded.containsKey(a) || !loaded.containsKey(b)) return;
            if (adj.get(a).add(b)) indeg.put(b, indeg.get(b) + 1);
        }}
        EdgeAdder addEdge = new EdgeAdder();

        for (LoadedModule lm : loaded.values()) {
            String me = lm.info.name();
            for (String dep : lm.info.moduleDepends()) addEdge.add(dep, me);
            for (String soft : lm.info.moduleSoftDepends()) addEdge.add(soft, me);
            for (String before : lm.info.moduleLoadBefore()) addEdge.add(me, before);
        }

        PriorityQueue<String> q = new PriorityQueue<String>();
        for (Map.Entry<String,Integer> e : indeg.entrySet()) if (e.getValue() == 0) q.add(e.getKey());
        List<String> order = new ArrayList<String>(loaded.size());
        while (!q.isEmpty()) {
            String u = q.poll(); order.add(u);
            for (String v : adj.get(u)) {
                indeg.put(v, indeg.get(v) - 1);
                if (indeg.get(v) == 0) q.add(v);
            }
        }
        if (order.size() != loaded.size()) {
            warn("Detected a cycle in module dependencies; falling back to partial order.");
            for (String n : loaded.keySet()) if (!order.contains(n)) order.add(n);
        }
        topoOrder = order;
    }

    /**
     * Enables all modules in topological order.
     */
    public void enableAll() {
        for (String n : topoOrder) {
            Optional<LoadedModule> lm = get(n);
            if (lm.isPresent() && !lm.get().enabled) attemptEnable(lm.get());
        }
    }

    /**
     * Attempts to enable modules currently pending if their requirements are now met.
     */
    public void tryEnablePending() {
        if (pending.isEmpty()) return;
        for (String n : topoOrder) {
            if (!pending.contains(n)) continue;
            Optional<LoadedModule> lm = get(n);
            if (lm.isPresent() && !lm.get().enabled) attemptEnable(lm.get());
        }
    }

    /**
     * Enables a specific module by name.
     *
     * @param name Module name.
     * @return True if already enabled or enabled successfully.
     */
    public boolean enable(String name) {
        LoadedModule lm = loaded.get(name);
        return lm != null && (lm.enabled || attemptEnable(lm));
    }

    /**
     * Disables and unloads a specific module by name.
     *
     * @param name Module name.
     * @return True if disabled, false if not found.
     */
    public boolean disable(String name) {
        info("Disabling Module " + name);
        ModuleManager.LoadedModule lm = loaded.get(name);
        if (lm == null) return false;

        try { lm.context._internalPreDisable(); } catch (Throwable ignored) {}
        try { lm.instance.onModuleDisable(lm.context); } catch (Throwable t) { severe("Error in onModuleDisable for " + name, t); }
        try { commandRegistrar.unregisterFor(lm); } catch (Throwable ignored) {}
        try { lm.context._internalCleanup(); } catch (Throwable ignored) {}
        try { lm.cl.close(); } catch (Throwable ignored) {}

        lm.enabled = false;
        pending.remove(name);

        return true;
    }

    /**
     * Disables all modules in reverse topological order.
     */
    public void disableAll() {
        ListIterator<String> it = new ArrayList<>(topoOrder).listIterator(topoOrder.size());
        while (it.hasPrevious()) disable(it.previous());
        topoOrder = List.of();
    }

    /**
     * Attempts to enable the given loaded module, checking plugin and module dependencies.
     * On failure, it logs, cleans up, and unloads the module.
     *
     * @param lm Loaded module bundle.
     * @return True if enabled successfully.
     */
    private boolean attemptEnable(LoadedModule lm) {
        List<String> missingPlugins = missingRequiredPlugins(lm.info);
        if (!missingPlugins.isEmpty()) {
            warn(lm.info.name() + ": required plugin(s) not installed: " + String.join(", ", missingPlugins));
            disable(lm.info.name());
            return false;
        }

        List<String> inactivePlugins = inactiveRequiredPlugins(lm.info);
        if (!inactivePlugins.isEmpty()) {
            pending.add(lm.info.name());
            info("Pending " + lm.info.name() + " (waiting for required plugins to enable: "
                    + String.join(", ", inactivePlugins) + ")");
            return false;
        }

        List<String> missingMods = new ArrayList<>();
        for (String m : lm.info.moduleDepends()) {
            LoadedModule dep = loaded.get(m);
            if (dep == null || !dep.enabled) missingMods.add(m);
        }

        if (!missingMods.isEmpty()) {
            pending.add(lm.info.name());
            info("Pending " + lm.info.name() + " (waiting for modules: " + String.join(", ", missingMods) + ")");
            return false;
        }

        try {
            lm.instance.onModuleEnable(lm.context);
            lm.enabled = true;
            pending.remove(lm.info.name());
            try {
                commandRegistrar.registerFor(lm);
            } catch (Throwable t) {
                severe("Command registration failed for " + lm.info.name(), t);
                disable(lm.info.name());
            }
            info("Enabled Module " + lm.info.name());
            tryEnablePending();
            return true;
        } catch (Throwable t) {
            severe("Failed enabling " + lm.info.name() + ". Disabling Module.", t);
            disable(lm.info.name());
            return false;
        }
    }

    /**
     * Collects required Bukkit plugins that are not installed (null from PluginManager).
     *
     * @param info Module metadata.
     * @return List of missing plugin names.
     */
    private List<String> missingRequiredPlugins(ModuleInfo info) {
        List<String> missing = new ArrayList<>();
        for (String req : info.pluginDepends()) {
            Plugin p = Bukkit.getPluginManager().getPlugin(req);
            if (p == null) missing.add(req);
        }
        return missing;
    }

    /**
     * Collects required Bukkit plugins that exist but are currently disabled.
     *
     * @param info Module metadata.
     * @return List of inactive plugin names.
     */
    private List<String> inactiveRequiredPlugins(ModuleInfo info) {
        List<String> inactive = new ArrayList<String>();
        for (String req : info.pluginDepends()) {
            Plugin p = Bukkit.getPluginManager().getPlugin(req);
            if (p != null && !p.isEnabled()) inactive.add(req);
        }
        return inactive;
    }

    /**
     * Logs an info-level message with a modules prefix.
     *
     * @param m Message to log.
     */
    private void info(String m) { plugin.getLogger().info("[Modules] " + m); }

    /**
     * Logs a warning-level message with a modules prefix.
     *
     * @param m Message to log.
     */
    private void warn(String m) { plugin.getLogger().warning("[Modules] " + m); }

    /**
     * Logs a severe-level message with a modules prefix and a throwable.
     *
     * @param m Message to log.
     * @param t Throwable to include.
     */
    private void severe(String m, Throwable t) { plugin.getLogger().log(Level.SEVERE, "[Modules] " + m, t); }
}
