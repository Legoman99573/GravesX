package dev.cwhead.GravesX.module;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.ModuleManager.LoadedModule;
import dev.cwhead.GravesX.module.command.GravesXModuleCommand;
import dev.cwhead.GravesX.module.command.GravesXModuleTabCompleter;
import dev.cwhead.GravesX.module.util.ModuleInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Registers and unregisters module-defined permissions and commands based on {@link ModuleInfo}.
 *
 * <p>Module metadata (permissions/commands) is parsed once from {@code module.yml} into
 * {@link ModuleInfo} and then used here as the single source of truth when wiring Bukkit
 * permissions and commands.</p>
 */
final class ModuleCommandRegistrar {
    private final Graves plugin;
    private final Map<String, List<PluginCommand>> cmds = new LinkedHashMap<>();
    private final Map<String, List<Permission>> perms = new LinkedHashMap<>();

    ModuleCommandRegistrar(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers permissions and commands for a loaded module using its {@link ModuleInfo}.
     *
     * @param lm loaded module container
     */
    void registerFor(LoadedModule lm) {
        registerPermissions(lm);
        registerCommands(lm);
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
                try {
                    Bukkit.getPluginManager().removePermission(p);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * Registers permissions from {@link ModuleInfo#permissions()}.
     *
     * @param lm Loaded module container.
     */
    private void registerPermissions(LoadedModule lm) {
        Map<String, ModuleInfo.PermissionDef> defs = lm.info.permissions();
        if (defs == null || defs.isEmpty()) return;

        var out = new ArrayList<Permission>();

        for (ModuleInfo.PermissionDef def : defs.values()) {
            if (def == null || def.node() == null || def.node().isBlank()) continue;

            String node = def.node();
            String desc = Optional.ofNullable(def.description()).orElse("");
            String defStr = Optional.ofNullable(def.defaultValue()).orElse("FALSE");

            PermissionDefault permDefault = PermissionDefault.FALSE;
            try {
                permDefault = PermissionDefault.valueOf(defStr.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
                // fall back to FALSE if invalid
            }

            Permission perm = new Permission(node, desc, permDefault);

            if (Bukkit.getPluginManager().getPermission(perm.getName()) == null) {
                Bukkit.getPluginManager().addPermission(perm);
                try {
                    Bukkit.getPluginManager().recalculatePermissionDefaults(perm);
                } catch (Throwable ignored) {
                }
                out.add(perm);
            }
        }

        if (!out.isEmpty()) perms.put(lm.info.name(), out);
    }

    /**
     * Registers commands from {@link ModuleInfo#commands()} and wires executors/tab-completers.
     *
     * @param lm Loaded module container.
     */
    private void registerCommands(LoadedModule lm) {
        Map<String, ModuleInfo.CommandDef> defs = lm.info.commands();
        if (defs == null || defs.isEmpty()) return;

        SimpleCommandMap map = commandMap();
        if (map == null) {
            plugin.debugMessage("CommandMap is null; cannot register YAML commands for module " + lm.info.name(), 2);
            return;
        }

        var out = new ArrayList<PluginCommand>();

        for (ModuleInfo.CommandDef def : defs.values()) {
            if (def == null || def.name() == null || def.name().isBlank()) continue;
            String name = def.name();

            PluginCommand pc = newPluginCommand(name, plugin);
            if (pc == null) {
                plugin.debugMessage("Failed to create PluginCommand for '" + name + "' (module " + lm.info.name() + ")", 2);
                continue;
            }

            String yamlDesc = Optional.ofNullable(def.description()).orElse("");
            String yamlUsage = Optional.ofNullable(def.usage()).orElse("/" + name);
            String yamlPerm = Optional.ofNullable(def.permission()).orElse("");
            List<String> yamlAliases = new ArrayList<>(def.aliases());

            CommandExecutor exec = execInstance(def.executor(), lm, pc);
            GravesXModuleTabCompleter tab = tabInstance(def.tabCompleter(), lm, pc);

            // If executor implements GravesXModuleTabCompleter, and no explicit tab completer is provided, reuse it
            if (tab == null && exec instanceof GravesXModuleTabCompleter gmtc) {
                tab = gmtc;
            }

            // Allow GravesXModuleCommand to fill in metadata when not defined in module.yml
            if (exec instanceof GravesXModuleCommand g) {
                if (yamlDesc.isEmpty() && g.getDescription() != null) yamlDesc = g.getDescription();
                if (yamlUsage.isEmpty() && g.getUsage() != null) yamlUsage = g.getUsage();
                if (yamlPerm.isEmpty() && g.getPermission() != null) yamlPerm = g.getPermission();
                if (yamlAliases.isEmpty() && g.getAliases() != null) {
                    yamlAliases = new ArrayList<>(g.getAliases());
                }

                String providedName = g.getName();
                if (providedName != null && !providedName.isEmpty() && !providedName.equalsIgnoreCase(name)) {
                    if (!yamlAliases.contains(providedName)) yamlAliases.add(providedName);
                }
            }

            pc.setDescription(yamlDesc);
            pc.setUsage(yamlUsage);
            pc.setPermission(yamlPerm);
            if (!yamlAliases.isEmpty()) pc.setAliases(yamlAliases);

            if (exec != null) pc.setExecutor(exec);
            if (tab != null) {
                pc.setTabCompleter(tab);
                tab.onRegister(lm.context, pc);
            }

            if (map.register(plugin.getName().toLowerCase(Locale.ROOT), pc)) {
                cmds.computeIfAbsent(lm.info.name(), k -> new ArrayList<>()).add(pc);
                plugin.debugMessage("Registered YAML command '/" + name + "' for module " + lm.info.name(), 1);
            } else {
                plugin.debugMessage("Failed to register YAML command '/" + name + "' for module " + lm.info.name() + " (command map rejected registration)", 2);
            }
        }
    }

    /**
     * Instantiates a command executor using the module class loader.
     * Prefers a {@code (ModuleContext)} constructor, then no-arg.
     *
     * @param fqcn Fully qualified class name.
     * @param lm   Loaded module container.
     * @param cmd  Command being registered.
     * @return Executor instance or {@code null} if incompatible or failed.
     */
    private CommandExecutor execInstance(String fqcn, LoadedModule lm, Command cmd) {
        if (fqcn == null || fqcn.isEmpty()) return null;
        try {
            Class<?> c = Class.forName(fqcn, true, lm.cl);
            if (!CommandExecutor.class.isAssignableFrom(c)) return null;
            return instantiate(c, lm.context, CommandExecutor.class);
        } catch (Throwable t) {
            plugin.getLogger().severe("Executor load failed for " + fqcn + ": " + t.getMessage());
            plugin.logStackTrace(t);
            return null;
        }
    }

    /**
     * Instantiates a tab completer using the module class loader.
     * Prefers a {@code (ModuleContext)} constructor, then no-arg.
     *
     * @param fqcn Fully qualified class name.
     * @param lm   Loaded module container.
     * @param cmd  Command being registered.
     * @return Tab completer instance or {@code null} if incompatible or failed.
     */
    private GravesXModuleTabCompleter tabInstance(String fqcn, LoadedModule lm, Command cmd) {
        if (fqcn == null || fqcn.isEmpty()) return null;
        try {
            Class<?> c = Class.forName(fqcn, true, lm.cl);
            if (!GravesXModuleTabCompleter.class.isAssignableFrom(c)) return null;
            return instantiate(c, lm.context, GravesXModuleTabCompleter.class);
        } catch (Throwable t) {
            plugin.getLogger().severe("TabCompleter load failed for " + fqcn + ": " + t.getMessage());
            plugin.logStackTrace(t);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiate(Class<?> c, ModuleContext ctx, Class<T> type) throws Exception {
        try {
            Constructor<?> cons = c.getDeclaredConstructor(ModuleContext.class);
            cons.setAccessible(true);
            return (T) cons.newInstance(ctx);
        } catch (NoSuchMethodException ignored) {
            Constructor<?> cons = c.getDeclaredConstructor();
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
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            return (SimpleCommandMap) f.get(Bukkit.getServer());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Creates a {@link PluginCommand} bound to the owner plugin.
     *
     * @param name  Command name.
     * @param owner Owner plugin.
     * @return New command or {@code null} on failure.
     */
    private PluginCommand newPluginCommand(String name, Graves owner) {
        try {
            var c = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            c.setAccessible(true);
            return c.newInstance(name, owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Unregisters a command from the command map and removes it from known commands.
     *
     * @param pc Plugin command to unregister.
     */
    @SuppressWarnings("unchecked")
    private void unregister(PluginCommand pc) {
        try {
            SimpleCommandMap map = commandMap();
            if (map == null) return;
            try {
                pc.unregister(map);
            } catch (Throwable ignored) {
            }

            Field f = SimpleCommandMap.class.getDeclaredField("knownCommands");
            f.setAccessible(true);
            Map<String, Command> known = (Map<String, Command>) f.get(map);
            known.values().removeIf(cmd -> cmd == pc);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Dynamically registers a command for a module using a command instance.
     *
     * @param lm      Loaded module container.
     * @param name    Primary command label (without leading /).
     * @param command Command implementation.
     */
    void registerDynamicCommand(LoadedModule lm, String name, GravesXModuleCommand command) {
        if (lm == null || command == null || name == null || name.isBlank()) return;

        SimpleCommandMap map = commandMap();
        if (map == null) {
            plugin.debugMessage("CommandMap is null; cannot register dynamic command '/" + name + "' for module " + lm.info.name(), 2);
            return;
        }

        PluginCommand pc = newPluginCommand(name, plugin);
        if (pc == null) {
            plugin.debugMessage("Failed to create PluginCommand for dynamic '/" + name + "' (module " + lm.info.name() + ")", 2);
            return;
        }

        // Metadata from GravesXModuleCommand
        String desc = Optional.ofNullable(command.getDescription()).orElse("");
        String usage = Optional.ofNullable(command.getUsage()).orElse("/" + name);
        String perm = Optional.ofNullable(command.getPermission()).orElse("");
        List<String> aliases = new ArrayList<>();
        if (command.getAliases() != null) {
            aliases.addAll(command.getAliases());
        }

        String providedName = command.getName();
        if (providedName != null && !providedName.isEmpty()
                && !providedName.equalsIgnoreCase(name)
                && !aliases.contains(providedName)) {
            aliases.add(providedName);
        }

        pc.setDescription(desc);
        pc.setUsage(usage);
        pc.setPermission(perm);
        if (!aliases.isEmpty()) {
            pc.setAliases(aliases);
        }

        GravesXModuleTabCompleter tab = null;
        if (command instanceof GravesXModuleTabCompleter gmtc) {
            tab = gmtc;
        }

        pc.setExecutor(command);
        if (tab != null) {
            pc.setTabCompleter(tab);
            tab.onRegister(lm.context, pc);
        }

        if (map.register(plugin.getName().toLowerCase(Locale.ROOT), pc)) {
            cmds.computeIfAbsent(lm.info.name(), k -> new ArrayList<>()).add(pc);
            plugin.debugMessage("Registered dynamic command '/" + name + "' for module " + lm.info.name(), 1);
        } else {
            plugin.debugMessage("Failed to register dynamic command '/" + name + "' for module " + lm.info.name() + " (command map rejected registration)", 2);
        }
    }

    /**
     * Dynamically registers a command for a module using a command class.
     *
     * Any class that implements {@link GravesXModuleCommand} is accepted.
     *
     * @param lm           Loaded module container.
     * @param name         Primary command label (without leading /).
     * @param commandClass Implementation class of the command.
     */
    void registerDynamicCommand(LoadedModule lm, String name, Class<? extends GravesXModuleCommand> commandClass) {
        if (lm == null || commandClass == null || name == null || name.isBlank()) return;

        SimpleCommandMap map = commandMap();
        if (map == null) {
            plugin.debugMessage("CommandMap is null; cannot register dynamic class-based command '/" + name + "' for module " + lm.info.name(), 2);
            return;
        }

        PluginCommand pc = newPluginCommand(name, plugin);
        if (pc == null) {
            plugin.debugMessage("Failed to create PluginCommand for dynamic class-based '/" + name + "' (module " + lm.info.name() + ")", 2);
            return;
        }

        GravesXModuleCommand command;
        try {
            command = instantiate(commandClass, lm.context, GravesXModuleCommand.class);
        } catch (Exception t) {
            plugin.getLogger().severe("Dynamic command instantiation failed for " + commandClass.getName() + ": " + t.getMessage());
            plugin.logStackTrace(t);
            return;
        }

        // Metadata from GravesXModuleCommand
        String desc = Optional.ofNullable(command.getDescription()).orElse("");
        String usage = Optional.ofNullable(command.getUsage()).orElse("/" + name);
        String perm = Optional.ofNullable(command.getPermission()).orElse("");
        List<String> aliases = new ArrayList<>();
        if (command.getAliases() != null) {
            aliases.addAll(command.getAliases());
        }

        String providedName = command.getName();
        if (providedName != null && !providedName.isEmpty()
                && !providedName.equalsIgnoreCase(name)
                && !aliases.contains(providedName)) {
            aliases.add(providedName);
        }

        pc.setDescription(desc);
        pc.setUsage(usage);
        pc.setPermission(perm);
        if (!aliases.isEmpty()) {
            pc.setAliases(aliases);
        }

        GravesXModuleTabCompleter tab = null;
        if (command instanceof GravesXModuleTabCompleter gmtc) {
            tab = gmtc;
        }

        pc.setExecutor(command);
        if (tab != null) {
            pc.setTabCompleter(tab);
            tab.onRegister(lm.context, pc);
        }

        if (map.register(plugin.getName().toLowerCase(Locale.ROOT), pc)) {
            cmds.computeIfAbsent(lm.info.name(), k -> new ArrayList<>()).add(pc);
            plugin.debugMessage("Registered dynamic class-based command '/" + name + "' for module " + lm.info.name(), 1);
        } else {
            plugin.debugMessage("Failed to register dynamic class-based command '/" + name + "' for module " + lm.info.name() + " (command map rejected registration)", 2);
        }
    }
}