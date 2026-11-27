package dev.cwhead.GravesX.command;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.GravesXModuleController;
import dev.cwhead.GravesX.module.ModuleManager;
import dev.cwhead.GravesX.module.util.ModuleInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public final class GxModulesCommand implements CommandExecutor, TabCompleter {
    private final ModuleManager manager;
    private final Graves plugin;

    public GxModulesCommand(ModuleManager manager, Graves plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (sender instanceof Player player && !plugin.getPermissionManager().hasGrantedPermission("graves.command.modules", player.getPlayer())) {
            sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.RESET + "You don't have permission.");
            return true;
        }

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.GOLD + "GravesX Modules:");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " list" + ChatColor.GRAY + " - show modules + state");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <name>" + ChatColor.GRAY + " - module details");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <name> commands" + ChatColor.GRAY + " - list module commands");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <name> permissions" + ChatColor.GRAY + " - list module permission nodes");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - reload all modules (DANGEROUS; requires confirm)");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        List<String> order = manager.order();
        Set<String> pend = manager.pending();

        switch (sub) {
            case "list" -> {
                sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.GOLD + "Modules (" + order.size() + "):");
                for (String name : order) {
                    ModuleManager.LoadedModule lm = manager.get(name).orElse(null);
                    if (lm == null) continue;

                    String state = lm.enabled
                            ? ChatColor.GREEN + "ENABLED"
                            : (pend.contains(name) ? ChatColor.YELLOW + "PENDING" : ChatColor.RED + "DISABLED");

                    // Folia support indicator from module.yml -> supportsFolia
                    String foliaFlag = lm.info.supportsFolia()
                            ? ChatColor.GREEN + "YES"
                            : ChatColor.RED + "NO";

                    // Declared load phase from module.yml -> load
                    GravesXModuleController.LoadPhase phase = lm.info.loadPhase();
                    String phaseColor = switch (phase) {
                        case STARTUP -> ChatColor.YELLOW.toString();
                        case POSTWORLD -> ChatColor.GOLD.toString();
                        case COMPLETED -> ChatColor.AQUA.toString();
                    };

                    // Description strictly from module.yml "description"
                    String desc = (lm.info.description() == null) ? "" : lm.info.description().trim();
                    String descPreview = desc.isEmpty()
                            ? ""
                            : ChatColor.DARK_GRAY + " — " + ChatColor.GRAY + ellipsize(desc, 80);

                    sender.sendMessage(
                            ChatColor.AQUA + "- " + name + ChatColor.GRAY + " v" + lm.info.version()
                                    + ChatColor.DARK_GRAY + " [" + state + ChatColor.DARK_GRAY + "]"
                                    + ChatColor.DARK_GRAY + " [Folia: " + foliaFlag + ChatColor.DARK_GRAY + "]"
                                    + ChatColor.DARK_GRAY + " [Load: " + phaseColor + phase + ChatColor.DARK_GRAY + "]"
                                    + descPreview
                    );
                }
                return true;
            }

            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.RED + "Usage: /" + label + " info <name> [commands|permissions]");
                    return true;
                }

                String query = args[1];
                ModuleManager.LoadedModule lm = manager.get(query).orElse(null);
                if (lm == null) {
                    sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.RED + "Unknown module: " + query);
                    return true;
                }

                // Optional third argument: commands / permissions
                String mode = (args.length >= 3) ? args[2].toLowerCase(Locale.ROOT) : "";

                if ("commands".equals(mode)) {
                    return handleModuleCommands(sender, lm);
                } else if ("permissions".equals(mode)) {
                    return handleModulePermissions(sender, lm);
                }

                // ---- General module info ----
                String state = lm.enabled
                        ? ChatColor.GREEN + "ENABLED"
                        : (pend.contains(lm.info.name()) ? ChatColor.YELLOW + "PENDING" : ChatColor.RED + "DISABLED");

                // Folia support + current server type
                boolean supportsFolia = lm.info.supportsFolia();
                boolean runningFolia = plugin.getVersionManager().isFolia();
                String foliaSupportLine =
                        (supportsFolia ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO")
                                + ChatColor.DARK_GRAY + " (Server: "
                                + (runningFolia ? ChatColor.GREEN + "Folia" : ChatColor.YELLOW + "Non-Folia")
                                + ChatColor.DARK_GRAY + ")";

                // Declared load phase
                GravesXModuleController.LoadPhase phase = lm.info.loadPhase();
                String loadLine = switch (phase) {
                    case STARTUP -> ChatColor.YELLOW + "STARTUP" + ChatColor.DARK_GRAY + " (early enable)";
                    case POSTWORLD -> ChatColor.GOLD + "POSTWORLD" + ChatColor.DARK_GRAY + " (after worlds load)";
                    case COMPLETED -> ChatColor.AQUA + "COMPLETED" + ChatColor.DARK_GRAY + " (after startup complete)";
                };

                Function<String, String> fmtPluginReq = (dep) -> {
                    Plugin p = Bukkit.getPluginManager().getPlugin(dep);
                    if (p == null) {
                        return dep + ChatColor.DARK_GRAY + " [" + ChatColor.RED + "MISSING" + ChatColor.DARK_GRAY + "]";
                    } else if (!p.isEnabled()) {
                        return dep + ChatColor.DARK_GRAY + " [" + ChatColor.YELLOW + "PRESENT (DISABLED)" + ChatColor.DARK_GRAY + "]";
                    } else {
                        return dep + ChatColor.DARK_GRAY + " [" + ChatColor.GREEN + "PRESENT (ENABLED)" + ChatColor.DARK_GRAY + "]";
                    }
                };

                Function<String, String> fmtModuleReq = (dep) -> {
                    ModuleManager.LoadedModule d = manager.get(dep).orElse(null);
                    if (d == null) {
                        return dep + ChatColor.DARK_GRAY + " [" + ChatColor.RED + "MISSING" + ChatColor.DARK_GRAY + "]";
                    } else if (d.enabled) {
                        return dep + ChatColor.DARK_GRAY + " [" + ChatColor.GREEN + "ENABLED" + ChatColor.DARK_GRAY + "]";
                    } else if (pend.contains(dep)) {
                        return dep + ChatColor.DARK_GRAY + " [" + ChatColor.YELLOW + "PENDING" + ChatColor.DARK_GRAY + "]";
                    } else {
                        return dep + ChatColor.DARK_GRAY + " [" + ChatColor.RED + "DISABLED" + ChatColor.DARK_GRAY + "]";
                    }
                };

                List<String> pluginDependsLines = new ArrayList<>();
                for (String dep : lm.info.pluginDepends()) pluginDependsLines.add(fmtPluginReq.apply(dep));

                List<String> pluginSoftDependsLines = new ArrayList<>();
                for (String dep : lm.info.pluginSoftDepends()) pluginSoftDependsLines.add(fmtPluginReq.apply(dep));

                List<String> moduleDependsLines = new ArrayList<>();
                for (String dep : lm.info.moduleDepends()) moduleDependsLines.add(fmtModuleReq.apply(dep));

                List<String> moduleSoftDependsLines = new ArrayList<>();
                for (String dep : lm.info.moduleSoftDepends()) moduleSoftDependsLines.add(fmtModuleReq.apply(dep));

                List<String> authorsPretty = new ArrayList<>();
                for (String a : lm.info.authors()) {
                    if (a != null && !a.isBlank()) authorsPretty.add(ChatColor.AQUA + a + ChatColor.GRAY);
                }
                String authorsLine = authorsPretty.isEmpty() ? "-" : String.join(", ", authorsPretty);

                String site = lm.info.website();
                String websiteLine = (site == null || site.isBlank()) ? "-" : ChatColor.BLUE + site;

                // Description strictly from module.yml description
                String descFull = lm.info.description();
                String descLine = (descFull == null || descFull.isBlank()) ? "-" : ChatColor.GRAY + descFull;

                sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.GOLD + "Module: " + ChatColor.AQUA + lm.info.name());
                sender.sendMessage(ChatColor.GRAY + "Version: " + lm.info.version());
                sender.sendMessage(ChatColor.GRAY + "Description: " + descLine);
                sender.sendMessage(ChatColor.GRAY + "State: " + state);
                sender.sendMessage(ChatColor.GRAY + "Load phase: " + loadLine);
                sender.sendMessage(ChatColor.GRAY + "Data folder: " + lm.context.getDataFolder().getPath());
                sender.sendMessage(ChatColor.GRAY + "Authors: " + authorsLine);
                sender.sendMessage(ChatColor.GRAY + "Website: " + websiteLine);
                sender.sendMessage(ChatColor.GRAY + "Folia support: " + foliaSupportLine);

                sender.sendMessage(ChatColor.GRAY + "pluginDepends: " +
                        (pluginDependsLines.isEmpty() ? "-" : String.join(ChatColor.GRAY + ", ", pluginDependsLines)));

                sender.sendMessage(ChatColor.GRAY + "pluginSoftDepends: " +
                        (pluginSoftDependsLines.isEmpty() ? "-" : String.join(ChatColor.GRAY + ", ", pluginSoftDependsLines)));

                sender.sendMessage(ChatColor.GRAY + "pluginLoadBefore: " +
                        (lm.info.pluginLoadBefore().isEmpty() ? "-" : String.join(ChatColor.GRAY + ", ", lm.info.pluginLoadBefore())));

                sender.sendMessage(ChatColor.GRAY + "moduleDepends: " +
                        (moduleDependsLines.isEmpty() ? "-" : String.join(ChatColor.GRAY + ", ", moduleDependsLines)));

                sender.sendMessage(ChatColor.GRAY + "moduleSoftDepends: " +
                        (moduleSoftDependsLines.isEmpty() ? "-" : String.join(ChatColor.GRAY + ", ", moduleSoftDependsLines)));

                sender.sendMessage(ChatColor.GRAY + "moduleLoadBefore: " +
                        (lm.info.moduleLoadBefore().isEmpty() ? "-" : String.join(ChatColor.GRAY + ", ", lm.info.moduleLoadBefore())));

                sender.sendMessage(ChatColor.DARK_GRAY + "Tip: "
                        + ChatColor.GRAY + "Use "
                        + ChatColor.YELLOW + "/" + label + " info " + lm.info.name() + " commands"
                        + ChatColor.GRAY + " or "
                        + ChatColor.YELLOW + "/" + label + " info " + lm.info.name() + " permissions"
                        + ChatColor.GRAY + " to see module commands/permissions.");
                return true;
            }

            case "reload" -> {
                if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])) {
                    sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "WARNING:"
                            + ChatColor.RESET + " " + ChatColor.RED + "Reloading modules is NOT supported and may break things, leak memory, or corrupt state.");
                    sender.sendMessage(ChatColor.GRAY + "If you still want to proceed, run: "
                            + ChatColor.YELLOW + "/" + label + " reload confirm");
                    return true;
                }

                sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.GOLD + "Reloading modules…");
                try {
                    manager.disableAll();
                    manager.loadAll();

                    // Enable in phases to respect module.yml:load
                    manager.enableAll(GravesXModuleController.LoadPhase.STARTUP);
                    manager.enableAll(GravesXModuleController.LoadPhase.POSTWORLD);
                    manager.enableAll(GravesXModuleController.LoadPhase.COMPLETED);

                    sender.sendMessage(ChatColor.GREEN + "✔ Modules reloaded.");
                } catch (Throwable t) {
                    sender.sendMessage(ChatColor.RED + "✖ Reload failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
                }
                return true;
            }

            default -> {
                return false;
            }
        }
    }

    /**
     * Handles /gxm info <module> permissions
     */
    private boolean handleModulePermissions(CommandSender sender, ModuleManager.LoadedModule lm) {
        Map<String, ModuleInfo.PermissionDef> perms = lm.info.permissions();
        sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.GOLD + "Permissions for module " + ChatColor.AQUA + lm.info.name() + ChatColor.GOLD + ":");
        if (perms == null || perms.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "- (No permissions defined in module.yml)");
            return true;
        }

        for (ModuleInfo.PermissionDef def : perms.values()) {
            String node = def.node();
            String desc = def.description();
            String defVal = def.defaultValue();

            if (defVal == null || defVal.isBlank()) defVal = "NONE";

            sender.sendMessage(ChatColor.YELLOW + node
                    + ChatColor.GRAY + " - "
                    + ((desc == null || desc.isBlank())
                    ? ChatColor.DARK_GRAY + "(no description)"
                    : ChatColor.GRAY + desc));
            sender.sendMessage(ChatColor.DARK_GRAY + "  Default: "
                    + ChatColor.AQUA + defVal);
        }
        return true;
    }

    /**
     * Handles /gxm info <module> commands
     */
    private boolean handleModuleCommands(CommandSender sender, ModuleManager.LoadedModule lm) {
        Map<String, ModuleInfo.CommandDef> commands = lm.info.commands();
        sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.GOLD + "Commands for module " + ChatColor.AQUA + lm.info.name() + ChatColor.GOLD + ":");
        if (commands == null || commands.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "- (No commands defined in module.yml)");
            return true;
        }

        for (ModuleInfo.CommandDef def : commands.values()) {
            String name = def.name();
            String desc = def.description();
            String usage = def.usage();
            String perm = def.permission();
            List<String> aliases = def.aliases().isEmpty() ? List.of() : def.aliases();

            sender.sendMessage(ChatColor.AQUA + "/" + name
                    + ChatColor.GRAY + " - "
                    + ((desc == null || desc.isBlank())
                    ? ChatColor.DARK_GRAY + "(no description)"
                    : ChatColor.GRAY + desc));

            if (usage != null && !usage.isBlank()) {
                sender.sendMessage(ChatColor.DARK_GRAY + "  Usage: "
                        + ChatColor.GRAY + usage);
            }

            if (perm != null && !perm.isBlank()) {
                sender.sendMessage(ChatColor.DARK_GRAY + "  Permission: "
                        + ChatColor.YELLOW + perm);
            }

            if (!aliases.isEmpty()) {
                sender.sendMessage(ChatColor.DARK_GRAY + "  Aliases: "
                        + ChatColor.GRAY + String.join(", ", aliases));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("gravesx.modules")) return List.of();

        if (args.length == 1) {
            return prefix(List.of("help", "list", "info", "reload"), args[0]);
        }

        if (args.length == 2 && "info".equalsIgnoreCase(args[0])) {
            List<String> names = manager.modules().stream().map(lm -> lm.info.name()).toList();
            return prefix(names, args[1]);
        }

        if (args.length == 3 && "info".equalsIgnoreCase(args[0])) {
            return prefix(List.of("commands", "permissions"), args[2]);
        }

        return List.of();
    }

    private static List<String> prefix(List<String> items, String token) {
        String t = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : items) {
            if (s.toLowerCase(Locale.ROOT).startsWith(t)) out.add(s);
        }
        return out;
    }

    private static String ellipsize(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }
}