package dev.cwhead.GravesX.module;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.ModuleManager.LoadedModule;
import dev.cwhead.GravesX.module.command.GravesXModuleCommand;
import dev.cwhead.GravesX.module.command.GravesXModuleTabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Registers and unregisters module-defined permissions and commands from {@code module.yml}.
 */
final class ModuleCommandRegistrar {
    private final Graves plugin;
    private final Map<String, List<PluginCommand>> cmds = new LinkedHashMap<>();
    private final Map<String, List<Permission>> perms = new LinkedHashMap<>();

    ModuleCommandRegistrar(Graves plugin) {
        this.plugin = plugin;
    }

    void registerFor(LoadedModule lm) {
        var yml = loadModuleYaml(lm);
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
        var cmdList = cmds.remove(lm.info.name());
        if (cmdList != null) {
            for (var pc : cmdList) {
                if (pc.getTabCompleter() instanceof GravesXModuleTabCompleter tab) {
                    tab.onUnregister();
                }
                unregister(pc);
            }
        }

        var permList = perms.remove(lm.info.name());
        if (permList != null) {
            for (var p : permList) {
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
        } catch (Exception ignored) {
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
        var out = new ArrayList<Permission>();

        for (var node : sec.getKeys(false)) {
            var psec = sec.getConfigurationSection(node);
            if (psec == null) continue;

            String desc = psec.getString("description", "");
            var def = PermissionDefault.FALSE;
            try { def = PermissionDefault.valueOf(psec.getString("default", "FALSE").toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}

            var children = new LinkedHashMap<String, Boolean>();
            var csec = psec.getConfigurationSection("children");
            if (csec != null) {
                for (var child : csec.getKeys(false)) children.put(child, csec.getBoolean(child, true));
            }

            var perm = children.isEmpty() ? new Permission(node, desc, def) : new Permission(node, desc, def, children);
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
        var map = commandMap();
        if (map == null) return;

        var out = new ArrayList<PluginCommand>();

        for (var name : sec.getKeys(false)) {
            var c = sec.getConfigurationSection(name);
            if (c == null) continue;

            var pc = newPluginCommand(name, plugin);
            if (pc == null) continue;

            var yamlDesc = c.getString("description", "");
            var yamlUsage = c.getString("usage", "/" + name);
            var yamlPerm = c.getString("permission", "");
            var yamlAliases = readAliases(c.get("aliases"));

            CommandExecutor exec = execInstance(c.getString("executor"), lm, pc);
            GravesXModuleTabCompleter tab = tabInstance(c.getString("tab-completer"), lm, pc);

            if (tab == null && exec instanceof GravesXModuleTabCompleter gmtc) tab = gmtc;

            if (exec instanceof GravesXModuleCommand g) {
                if (yamlDesc.isEmpty() && g.getDescription() != null) yamlDesc = g.getDescription();
                if (yamlUsage.isEmpty() && g.getUsage() != null) yamlUsage = g.getUsage();
                if (yamlPerm.isEmpty() && g.getPermission() != null) yamlPerm = g.getPermission();
                if ((yamlAliases == null || yamlAliases.isEmpty()) && g.getAliases() != null) yamlAliases = g.getAliases();

                String providedName = g.getName();
                if (providedName != null && !providedName.isEmpty() && !providedName.equalsIgnoreCase(name)) {
                    if (yamlAliases == null) yamlAliases = new ArrayList<>();
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

            if (map.register(plugin.getName().toLowerCase(Locale.ROOT), pc)) out.add(pc);
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
        if (val == null) return Collections.emptyList();
        if (val instanceof List<?> raw) {
            var out = new ArrayList<String>();
            for (var o : raw) if (o != null) out.add(String.valueOf(o));
            return out;
        }

        var s = String.valueOf(val);
        if (s.contains(",")) {
            var out = new ArrayList<String>();
            for (var part : s.split(",")) if (!part.trim().isEmpty()) out.add(part.trim());
            return out;
        }
        return s.isEmpty() ? Collections.emptyList() : Collections.singletonList(s);
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
    private CommandExecutor execInstance(String fqcn, LoadedModule lm, Command cmd) {
        if (fqcn == null || fqcn.isEmpty()) return null;
        try {
            var c = Class.forName(fqcn, true, lm.cl);
            if (!CommandExecutor.class.isAssignableFrom(c)) return null;
            return instantiate(c, lm.context, CommandExecutor.class);
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
    private GravesXModuleTabCompleter tabInstance(String fqcn, LoadedModule lm, Command cmd) {
        if (fqcn == null || fqcn.isEmpty()) return null;
        try {
            var c = Class.forName(fqcn, true, lm.cl);
            if (!GravesXModuleTabCompleter.class.isAssignableFrom(c)) return null;
            return instantiate(c, lm.context, GravesXModuleTabCompleter.class);
        } catch (Throwable t) {
            plugin.getLogger().severe("TabCompleter load failed for " + fqcn + ": " + t.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiate(Class<?> c, ModuleContext ctx, Class<T> type) throws Exception {
        try {
            var cons = c.getDeclaredConstructor(ModuleContext.class);
            cons.setAccessible(true);
            return (T) cons.newInstance(ctx);
        } catch (NoSuchMethodException ignored) {
            var cons = c.getDeclaredConstructor();
            cons.setAccessible(true);
            return (T) cons.newInstance();
        }
    }

    /**
     * Reflectively obtains the server command map.
     *
     * @return Command map or {@code null} if unavailable.
     */
    private SimpleCommandMap commandMap() {
        try {
            var f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            return (SimpleCommandMap) f.get(Bukkit.getServer());
        } catch (Throwable ignored) { return null; }
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
            var c = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            c.setAccessible(true);
            return c.newInstance(name, owner);
        } catch (Throwable ignored) { return null; }
    }

    /**
     * Unregisters a command from the command map and removes it from known commands.
     *
     * @param pc Plugin command to unregister.
     */
    private void unregister(PluginCommand pc) {
        try {
            var map = commandMap();
            if (map == null) return;
            try { pc.unregister(map); } catch (Throwable ignored) {}

            var f = SimpleCommandMap.class.getDeclaredField("knownCommands");
            f.setAccessible(true);
            var known = (Map<String, Command>) f.get(map);
            known.values().removeIf(cmd -> cmd == pc);
        } catch (Throwable ignored) {}
    }
}