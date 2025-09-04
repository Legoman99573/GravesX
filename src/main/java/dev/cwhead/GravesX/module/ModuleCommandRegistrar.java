package dev.cwhead.GravesX.module;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.ModuleManager.LoadedModule;
import dev.cwhead.GravesX.module.command.GravesXModuleCommand;
import dev.cwhead.GravesX.module.command.GravesXModuleTabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registers and unregisters module-defined permissions and commands from {@code module.yml}.
 */
final class ModuleCommandRegistrar {
    private final Graves plugin;
    private final Map<String, List<PluginCommand>> cmds = new LinkedHashMap<String, List<PluginCommand>>();
    private final Map<String, List<Permission>> perms = new LinkedHashMap<String, List<Permission>>();

    /**
     * Creates a registrar bound to the owning plugin.
     *
     * @param plugin Owning Graves plugin.
     */
    ModuleCommandRegistrar(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers permissions and commands declared by a loaded module.
     *
     * @param lm Loaded module container.
     */
    void registerFor(LoadedModule lm) {
        YamlConfiguration yml = loadModuleYaml(lm);
        if (yml == null) return;
        registerPermissions(lm, yml.getConfigurationSection("permissions"));
        registerCommands(lm, yml.getConfigurationSection("commands"));
    }

    /**
     * Unregisters all permissions and commands previously registered for a module.
     *
     * @param lm Loaded module container.
     */
    void unregisterFor(LoadedModule lm) {
        List<PluginCommand> list = cmds.remove(lm.info.name());
        if (list != null) {
            for (PluginCommand pc : list) {
                CommandExecutor ex = pc.getExecutor();
                if (ex instanceof GravesXModuleCommand) {}
                if (pc.getTabCompleter() instanceof GravesXModuleTabCompleter) {
                    ((GravesXModuleTabCompleter) pc.getTabCompleter()).onUnregister();
                }
                unregister(pc);
            }
        }
        List<Permission> ps = perms.remove(lm.info.name());
        if (ps != null) {
            for (Permission p : ps) {
                try { Bukkit.getPluginManager().removePermission(p); } catch (Throwable ignored) {}
            }
        }
    }

    /**
     * Loads the module's {@code module.yml} using the module class loader.
     *
     * @param lm Loaded module container.
     * @return Parsed YAML or {@code null} if not found or failed.
     */
    private YamlConfiguration loadModuleYaml(LoadedModule lm) {
        try (InputStream in = lm.cl.getResourceAsStream("module.yml")) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Registers permissions from the {@code permissions} section.
     *
     * @param lm Loaded module container.
     * @param sec Configuration section containing permission nodes.
     */
    private void registerPermissions(LoadedModule lm, ConfigurationSection sec) {
        if (sec == null) return;
        List<Permission> out = new ArrayList<Permission>();
        for (String node : sec.getKeys(false)) {
            ConfigurationSection psec = sec.getConfigurationSection(node);
            if (psec == null) continue;
            String desc = psec.getString("description", "");
            String defStr = psec.getString("default", "FALSE").toUpperCase(Locale.ROOT);
            PermissionDefault def;
            try { def = PermissionDefault.valueOf(defStr); } catch (Exception ex) { def = PermissionDefault.FALSE; }
            Map<String, Boolean> children = new LinkedHashMap<String, Boolean>();
            ConfigurationSection csec = psec.getConfigurationSection("children");
            if (csec != null) {
                for (String child : csec.getKeys(false)) children.put(child, csec.getBoolean(child, true));
            }
            Permission perm;
            if (children.isEmpty()) perm = new Permission(node, desc, def);
            else perm = new Permission(node, desc, def, children);
            if (Bukkit.getPluginManager().getPermission(perm.getName()) == null) {
                Bukkit.getPluginManager().addPermission(perm);
                try { Bukkit.getPluginManager().recalculatePermissionDefaults(perm); } catch (Throwable ignored) {}
                out.add(perm);
            }
        }
        if (!out.isEmpty()) perms.put(lm.info.name(), out);
    }

    /**
     * Registers commands from the {@code commands} section and wires executors/tab-completers.
     *
     * @param lm Loaded module container.
     * @param sec Configuration section containing commands.
     */
    private void registerCommands(LoadedModule lm, ConfigurationSection sec) {
        if (sec == null) return;
        SimpleCommandMap map = commandMap();
        if (map == null) return;
        List<PluginCommand> out = new ArrayList<PluginCommand>();
        for (String name : sec.getKeys(false)) {
            ConfigurationSection c = sec.getConfigurationSection(name);
            if (c == null) continue;

            PluginCommand pc = newPluginCommand(name, plugin);
            if (pc == null) continue;

            String yamlDesc = c.getString("description", "");
            String yamlUsage = c.getString("usage", "/" + name);
            String yamlPerm = c.getString("permission", "");
            List<String> yamlAliases = readAliases(c.get("aliases"));

            String execClass = c.getString("executor", null);
            String tabClass = c.getString("tab-completer", null);

            CommandExecutor exec = null;
            GravesXModuleTabCompleter tab = null;

            if (execClass != null && !execClass.isEmpty()) exec = newExecutor(execClass, lm, pc);
            if (tabClass != null && !tabClass.isEmpty()) tab = newTabCompleter(tabClass, lm, pc);
            if (tab == null && exec instanceof GravesXModuleTabCompleter) tab = (GravesXModuleTabCompleter) exec;

            if (exec instanceof GravesXModuleCommand) {
                GravesXModuleCommand g = (GravesXModuleCommand) exec;
                String d = g.getDescription();
                String u = g.getUsage();
                String p = g.getPermission();
                List<String> a = g.getAliases();
                if (yamlDesc.isEmpty() && d != null) yamlDesc = d;
                if (yamlUsage.isEmpty() && u != null) yamlUsage = u;
                if (yamlPerm.isEmpty() && p != null) yamlPerm = p;
                if ((yamlAliases == null || yamlAliases.isEmpty()) && a != null && !a.isEmpty()) yamlAliases = a;
                String providedName = g.getName();
                if (providedName != null && !providedName.isEmpty() && !providedName.equalsIgnoreCase(name)) {
                    if (yamlAliases == null) yamlAliases = new ArrayList<String>();
                    if (!yamlAliases.contains(providedName)) yamlAliases.add(providedName);
                }
            }

            pc.setDescription(yamlDesc);
            pc.setUsage(yamlUsage);
            pc.setPermission(yamlPerm);
            if (yamlAliases != null && !yamlAliases.isEmpty()) pc.setAliases(yamlAliases);

            if (exec != null) pc.setExecutor(exec);
            if (tab != null) {
                pc.setTabCompleter(tab);
                tab.onRegister(lm.context, pc);
            }

            boolean ok = map.register(plugin.getName().toLowerCase(Locale.ROOT), pc);
            if (ok) out.add(pc);
        }
        if (!out.isEmpty()) cmds.put(lm.info.name(), out);
    }

    /**
     * Normalizes aliases from list, comma-separated string, or single value.
     *
     * @param val Raw aliases value.
     * @return List of aliases, possibly empty.
     */
    private List<String> readAliases(Object val) {
        if (val == null) return java.util.Collections.emptyList();
        if (val instanceof List) {
            List<?> raw = (List<?>) val;
            List<String> out = new ArrayList<String>(raw.size());
            for (Object o : raw) if (o != null) out.add(String.valueOf(o));
            return out;
        }
        String s = String.valueOf(val);
        if (s.indexOf(',') >= 0) {
            String[] parts = s.split(",");
            List<String> out = new ArrayList<String>(parts.length);
            for (String p : parts) if (!p.trim().isEmpty()) out.add(p.trim());
            return out;
        }
        if (s.isEmpty()) return java.util.Collections.emptyList();
        return java.util.Collections.singletonList(s);
    }

    /**
     * Instantiates a command executor using the module class loader.
     * Prefers a {@code (ModuleContext)} constructor, then no-arg.
     *
     * @param fqcn Fully qualified class name.
     * @param lm Loaded module container.
     * @param cmd Command being registered.
     * @return Executor instance or {@code null} if incompatible or failed.
     */
    private CommandExecutor newExecutor(String fqcn, LoadedModule lm, Command cmd) {
        try {
            Class<?> c = Class.forName(fqcn, true, lm.cl);
            if (!CommandExecutor.class.isAssignableFrom(c)) return null;
            try {
                Constructor<?> k = c.getDeclaredConstructor(ModuleContext.class);
                k.setAccessible(true);
                return (CommandExecutor) k.newInstance(lm.context);
            } catch (NoSuchMethodException ignored) {}
            Constructor<?> k2 = c.getDeclaredConstructor();
            k2.setAccessible(true);
            return (CommandExecutor) k2.newInstance();
        } catch (Throwable t) {
            plugin.getLogger().severe("Executor load failed for " + fqcn + ": " + t.getMessage());
            return null;
        }
    }

    /**
     * Instantiates a tab completer using the module class loader.
     * Prefers a {@code (ModuleContext)} constructor, then no-arg.
     *
     * @param fqcn Fully qualified class name.
     * @param lm Loaded module container.
     * @param cmd Command being registered.
     * @return Tab completer instance or {@code null} if incompatible or failed.
     */
    private GravesXModuleTabCompleter newTabCompleter(String fqcn, LoadedModule lm, Command cmd) {
        try {
            Class<?> c = Class.forName(fqcn, true, lm.cl);
            if (!GravesXModuleTabCompleter.class.isAssignableFrom(c)) return null;
            try {
                Constructor<?> k = c.getDeclaredConstructor(ModuleContext.class);
                k.setAccessible(true);
                return (GravesXModuleTabCompleter) k.newInstance(lm.context);
            } catch (NoSuchMethodException ignored) {}
            Constructor<?> k2 = c.getDeclaredConstructor();
            k2.setAccessible(true);
            return (GravesXModuleTabCompleter) k2.newInstance();
        } catch (Throwable t) {
            plugin.getLogger().severe("TabCompleter load failed for " + fqcn + ": " + t.getMessage());
            return null;
        }
    }

    /**
     * Reflectively obtains the server command map.
     *
     * @return Command map or {@code null} if unavailable.
     */
    private SimpleCommandMap commandMap() {
        try {
            Object server = Bukkit.getServer();
            Field f = server.getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            return (SimpleCommandMap) f.get(server);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Creates a {@link PluginCommand} bound to the owner plugin.
     *
     * @param name Command name.
     * @param owner Owner plugin.
     * @return New command or {@code null} on failure.
     */
    private PluginCommand newPluginCommand(String name, Graves owner) {
        try {
            Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            c.setAccessible(true);
            return c.newInstance(name, owner);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Unregisters a command from the command map and removes it from known commands.
     *
     * @param pc Plugin command to unregister.
     */
    private void unregister(PluginCommand pc) {
        try {
            SimpleCommandMap map = commandMap();
            if (map == null) return;
            try { pc.unregister(map); } catch (Throwable ignored) {}
            Field f = SimpleCommandMap.class.getDeclaredField("knownCommands");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> known = (Map<String, Command>) f.get(map);
            known.values().removeIf(cmd -> cmd == pc);
        } catch (Throwable ignored) {}
    }
}