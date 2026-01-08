package com.ranull.graves.command;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilityTeleport;
import dev.cwhead.GravesX.util.PluginDownloadUtil;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles command execution and tab completion for the Graves plugin.
 */
public class GravesCommand implements CommandExecutor, TabCompleter {
    private final Graves plugin;
    private final Set<UUID> pendingImports = new HashSet<>();
    private boolean consolePendingImport = false;

    /**
     * Constructor to initialize the GravesCommand with the Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public GravesCommand(Graves plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,
                             @NotNull String string, String[] args) {
        if (args.length < 1) {
            if (commandSender instanceof Player player) {
                if (plugin.getPermissionManager().hasGrantedPermission("graves.gui", player.getPlayer())) {
                    plugin.getGUIManager().openGraveList(player);
                } else {
                    plugin.getEntityManager().sendMessage("message.permission-denied", player.getPlayer());
                }
            } else {
                sendHelpMenu(commandSender);
            }
        } else {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "list", "gui" -> handleListGuiCommand(commandSender, args);
                case "teleport", "tp" -> handleTeleportCommand(commandSender, args);
                case "virtual", "open" -> handleVirtualCommand(commandSender, args);
                case "givetoken" -> handleGiveTokenCommand(commandSender, args);
                case "reload" -> handleReloadCommand(commandSender);
                case "dump" -> handleDumpCommand(commandSender);
                case "debug" -> handleDebugCommand(commandSender, args);
                case "cleanup" -> handleCleanupCommand(commandSender);
                case "purge" -> handlePurgeCommand(commandSender, args);
                case "import" -> handleImportCommand(commandSender, args);
                case "addons", "addon" -> handleAddonCommand(commandSender, args);
                case "help" -> sendHelpMenu(commandSender);
                default -> sendHelpMenu(commandSender);
            }
        }
        return true;
    }

    /**
     * Sends the help menu to the command sender.
     *
     * @param sender The command sender.
     */
    public void sendHelpMenu(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + plugin.getName() + " " + ChatColor.DARK_GRAY + "v" + plugin.getVersion());

        if (sender instanceof Player player) {
            // GUI main command
            if (plugin.getPermissionManager().hasGrantedPermission("graves.gui", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Graves GUI");
            } else {
                sender.sendMessage(ChatColor.RED + "/graves " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Main Command");
            }

            // List / GUI (self)
            if (plugin.getPermissionManager().hasGrantedPermission("graves.gui", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves list " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Player graves");
            }

            // List / GUI (others)
            if (plugin.getPermissionManager().hasGrantedPermission("graves.gui.other", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves list {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " View player graves");
            }

            // Teleport (self)
            if (plugin.getPermissionManager().hasGrantedPermission("graves.teleport.command", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves teleport " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Teleport to your latest grave");
            }

            // Teleport (others)
            if (plugin.getPermissionManager().hasGrantedPermission("graves.teleport.command.others", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves teleport {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Teleport to latest player grave");
            }

            // Virtual / Open (self)
            if (plugin.getPermissionManager().hasGrantedPermission("graves.virtual.command", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves virtual " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Open your latest grave virtually");
            }

            // Virtual / Open (others)
            if (plugin.getPermissionManager().hasGrantedPermission("graves.virtual.command.others", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves virtual {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Open latest player grave virtually");
            }

            // Give token
            if (plugin.getPermissionManager().hasGrantedPermission("graves.givetoken", player.getPlayer())) {
                if (plugin.getConfig().getBoolean("settings.token")) {
                    sender.sendMessage(ChatColor.RED + "/graves givetoken {player} {amount} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Give grave token");
                }
            }

            // Reload
            if (plugin.getPermissionManager().hasGrantedPermission("graves.reload", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves reload " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Reload plugin");
            }

            // Dump
            if (plugin.getPermissionManager().hasGrantedPermission("graves.dump", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves dump " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Dump server information");
            }

            // Debug
            if (plugin.getPermissionManager().hasGrantedPermission("graves.debug", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves debug {level} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Change debug level");
            }

            // Import
            if (plugin.getPermissionManager().hasGrantedPermission("graves.import", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves import {plugin} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Imports grave data from another grave plugin");
            }

            // Purge
            if (plugin.getPermissionManager().hasGrantedPermission("graves.purge", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves purge {type} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Purges based on type");
            }

            // Addon download
            if (plugin.getPermissionManager().hasGrantedPermission("graves.download.addons", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves addon {addon} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Downloads addon (Restart Required)");
            }

            // Cleanup
            if (plugin.getPermissionManager().hasGrantedPermission("graves.cleanup", player.getPlayer())) {
                sender.sendMessage(ChatColor.RED + "/graves cleanup " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Purges all graves");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "/graves " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Main Command");
            sender.sendMessage(ChatColor.RED + "/graves list {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " View player graves");
            sender.sendMessage(ChatColor.RED + "/graves teleport {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Teleport to latest grave");
            sender.sendMessage(ChatColor.RED + "/graves virtual {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Open latest grave virtually");
            if (plugin.getConfig().getBoolean("settings.token")) {
                sender.sendMessage(ChatColor.RED + "/graves givetoken {player} {amount} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Give grave token");
            }
            sender.sendMessage(ChatColor.RED + "/graves reload " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Reload plugin");
            sender.sendMessage(ChatColor.RED + "/graves dump " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Dump server information");
            sender.sendMessage(ChatColor.RED + "/graves debug {level} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Change debug level");
            sender.sendMessage(ChatColor.RED + "/graves import {plugin} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Imports grave data from another grave plugin");
            sender.sendMessage(ChatColor.RED + "/graves purge {type} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Purges based on type");
            sender.sendMessage(ChatColor.RED + "/graves addon {addon} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Downloads addon (Restart Required)");
            sender.sendMessage(ChatColor.RED + "/graves cleanup " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Purges all graves");
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "Author: " + ChatColor.RED + "Ranull");
        sender.sendMessage(ChatColor.DARK_GRAY + "Maintainers: " + ChatColor.RED + "JaySmethers and Legoman99573");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                      @NotNull String string, @NotNull String[] args) {
        List<String> stringList = new ArrayList<>();

        if (args.length == 1) {
            stringList.add("help");

            if (commandSender instanceof Player player
                    && plugin.getPermissionManager().hasGrantedPermission("graves.teleport.command", player.getPlayer())) {
                stringList.add("teleport");
                stringList.add("tp");
                // offline teleport aliases share the same base permission
                stringList.add("tpoffline");
                stringList.add("teleportoffline");
            }

            if (commandSender instanceof Player player
                    && plugin.getPermissionManager().hasGrantedPermission("graves.import", player.getPlayer())) {
                stringList.add("import");
            }

            if (!(commandSender instanceof Player)
                    || plugin.getPermissionManager().hasGrantedPermission("graves.gui", ((Player) commandSender).getPlayer())) {
                stringList.add("list");
                stringList.add("gui");
            }

            // virtual / open (alias) – self virtual access
            if (!(commandSender instanceof Player)
                    || plugin.getPermissionManager().hasGrantedPermission("graves.virtual.command", ((Player) commandSender).getPlayer())) {
                stringList.add("virtual");
                stringList.add("open");
            }

            if (!(commandSender instanceof Player)
                    || plugin.getPermissionManager().hasGrantedPermission("graves.reload", ((Player) commandSender).getPlayer())) {
                stringList.add("reload");
            }

            if (!(commandSender instanceof Player)
                    || plugin.getPermissionManager().hasGrantedPermission("graves.dump", ((Player) commandSender).getPlayer())) {
                stringList.add("dump");
            }

            if (!(commandSender instanceof Player)
                    || plugin.getPermissionManager().hasGrantedPermission("graves.debug", ((Player) commandSender).getPlayer())) {
                stringList.add("debug");
            }

            if (!(commandSender instanceof Player)
                    || plugin.getPermissionManager().hasGrantedPermission("graves.cleanup", ((Player) commandSender).getPlayer())) {
                stringList.add("cleanup");
            }

            if (!(commandSender instanceof Player)
                    || plugin.getPermissionManager().hasGrantedPermission("graves.purge", ((Player) commandSender).getPlayer())) {
                stringList.add("purge");
            }

            if (!(commandSender instanceof Player)
                    || plugin.getPermissionManager().hasGrantedPermission("graves.download.addons", ((Player) commandSender).getPlayer())) {
                stringList.add("addon");
                stringList.add("addons");
            }

            if (plugin.getRecipeManager() != null
                    && (!(commandSender instanceof Player)
                    || plugin.getPermissionManager().hasGrantedPermission("graves.givetoken", ((Player) commandSender).getPlayer()))) {
                stringList.add("givetoken");
            }
        } else if (args.length > 1) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            switch (sub) {
                case "list":
                case "gui": {
                    if (!(commandSender instanceof Player)
                            || plugin.getPermissionManager().hasGrantedPermission("graves.gui.other", (Player) commandSender)) {

                        if (args.length == 2) {
                            String partialInput = args[1];
                            plugin.getSchedulerManager().runTaskAsynchronously(() -> {
                                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                    if (offlinePlayer.hasPlayedBefore()
                                            && offlinePlayer.getName() != null
                                            && !offlinePlayer.isOnline()) {
                                        String playerName = offlinePlayer.getName();
                                        if (playerName.startsWith(partialInput)) {
                                            synchronized (stringList) {
                                                stringList.add(offlinePlayer.getName());
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                    break;
                }

                case "teleport":
                case "tp": {
                    if (!(commandSender instanceof Player)
                            || plugin.getPermissionManager().hasGrantedPermission("graves.teleport.command.others", (Player) commandSender)) {
                        plugin.getServer().getOnlinePlayers().forEach(player -> stringList.add(player.getName()));
                    }
                    break;
                }

                case "tpoffline":
                case "teleportoffline": {
                    if (!(commandSender instanceof Player)
                            || plugin.getPermissionManager().hasGrantedPermission("graves.teleport.command.others", (Player) commandSender)) {

                        if (args.length == 2) {
                            String partialInput = args[1];
                            plugin.getSchedulerManager().runTaskAsynchronously(() -> {
                                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                    if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
                                        String playerName = offlinePlayer.getName();
                                        if (playerName.startsWith(partialInput)) {
                                            synchronized (stringList) {
                                                stringList.add(offlinePlayer.getName());
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                    break;
                }

                case "virtual":
                case "open": {
                    if (!(commandSender instanceof Player) || plugin.getPermissionManager().hasGrantedPermission("graves.virtual.command.others", (Player) commandSender)) {
                        if (args.length == 2) {
                            String partialInput = args[1];
                            plugin.getSchedulerManager().runTaskAsynchronously(() -> {
                                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                    if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
                                        String playerName = offlinePlayer.getName();
                                        if (playerName.startsWith(partialInput)) {
                                            synchronized (stringList) {
                                                stringList.add(offlinePlayer.getName());
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                    break;
                }

                case "debug": {
                    if (!(commandSender instanceof Player)
                            || plugin.getPermissionManager().hasGrantedPermission("graves.debug", ((Player) commandSender).getPlayer())) {
                        stringList.add("0");
                        stringList.add("1");
                        stringList.add("2");
                    }
                    break;
                }

                case "import": {
                    if (!(commandSender instanceof Player)
                            || plugin.getPermissionManager().hasGrantedPermission("graves.import", ((Player) commandSender).getPlayer())) {
                        stringList.add("AngelChest");
                        stringList.add("angelchest");
                    }
                    break;
                }

                case "addon":
                case "addons": {
                    if (!(commandSender instanceof Player)
                            || plugin.getPermissionManager().hasGrantedPermission("graves.download.addon", ((Player) commandSender).getPlayer())) {
                        stringList.add("LandProtection");
                    }
                    break;
                }

                case "purge": {
                    if (!(commandSender instanceof Player)
                            || plugin.getPermissionManager().hasGrantedPermission("graves.debug", ((Player) commandSender).getPlayer())) {

                        if (args.length == 2) {
                            stringList.add("graves");
                            stringList.add("grave");
                            stringList.add("player");
                            stringList.add("offline-player");
                            stringList.add("holograms");
                            stringList.add("hologram");
                            stringList.add("grave-specific");
                            stringList.add("grave-uuid");
                            stringList.add("abandon");
                            stringList.add("abandoned");
                        }

                        if ((args.length == 3 && args[1].equals("offline-player"))
                                || (args.length == 3 && args[1].equals("player"))) {
                            String partialInput = args[2];
                            plugin.getSchedulerManager().runTaskAsynchronously(() -> {
                                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                    if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
                                        String playerName = offlinePlayer.getName();
                                        if (playerName.startsWith(partialInput)) {
                                            synchronized (stringList) {
                                                stringList.add(offlinePlayer.getName());
                                            }
                                        }
                                    }
                                }
                            });
                        }

                        if ((args.length == 3 && args[1].equals("grave-specific"))
                                || (args.length == 3 && args[1].equals("grave-uuid"))) {
                            String partialInput = args[2];
                            List<Grave> graveList = new ArrayList<>(plugin.getCacheManager().getGraveMap().values());

                            for (Grave grave : graveList) {
                                String uuid = grave.getUUID().toString();
                                if (uuid.startsWith(partialInput)) {
                                    stringList.add(grave.getUUID().toString());
                                }
                            }
                        }
                    }
                    break;
                }

                case "givetoken": {
                    if (plugin.getRecipeManager() != null
                            && (!(commandSender instanceof Player)
                            || plugin.getPermissionManager().hasGrantedPermission("graves.givetoken", ((Player) commandSender).getPlayer()))) {

                        if (args.length == 2) {
                            plugin.getServer().getOnlinePlayers().forEach(player -> stringList.add(player.getName()));
                        } else if (args.length == 3) {
                            stringList.addAll(plugin.getRecipeManager().getTokenList());
                        }
                    }
                    break;
                }

                default:
                    break;
            }
        }

        return stringList;
    }

    private void handleListGuiCommand(CommandSender commandSender, String[] args) {
        if (commandSender instanceof Player player) {

            if (args.length == 1) {
                if (plugin.getPermissionManager().hasGrantedPermission("graves.gui", player.getPlayer())) {
                    plugin.getGUIManager().openGraveList(player);
                } else {
                    plugin.getEntityManager().sendMessage("message.permission-denied", player.getPlayer());
                }
            } else if (args.length == 2) {
                if (plugin.getPermissionManager().hasGrantedPermission("graves.gui.other", player.getPlayer())) {
                    OfflinePlayer otherPlayer = plugin.getServer().getOfflinePlayer(args[1]);

                    if (!plugin.getGraveManager().getGraveList(otherPlayer).isEmpty()) {
                        plugin.getGUIManager().openGraveList(player, otherPlayer.getUniqueId());
                    } else {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                                + ChatColor.RESET + ChatColor.RED + args[1] + ChatColor.RESET + " has no graves.");
                    }
                } else {
                    plugin.getEntityManager().sendMessage("message.permission-denied", player);
                }
            }
        } else {
            sendHelpMenu(commandSender);
        }
    }

    private void handleAddonCommand(CommandSender commandSender, String[] args) {
        if (args.length == 2) {
            if (commandSender instanceof Player player && plugin.getPermissionManager().hasGrantedPermission("graves.download.addons", player.getPlayer())) {
                switch (args[1].toUpperCase(Locale.ROOT)) {
                    case "LANDPROTECTION":
                        try {
                            PluginDownloadUtil.downloadAndReplacePlugin(120633, "GravesXAddon-LandProtection", "plugins", commandSender);
                        } catch (IOException e) {
                            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + "An error occurred while running command. Check console.");
                            plugin.getLogger().warning("An issue occurred while running this command.");
                            plugin.logStackTrace(e);
                        }
                        break;
                    default:
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + "Must enter a valid addon.");
                }
            } else {
                if (commandSender instanceof Player player) {
                    plugin.getEntityManager().sendMessage("message.permission-denied", player.getPlayer());
                }
            }
        } else {
            sendHelpMenu(commandSender);
        }
    }

    private void handleVirtualCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player player)) {
            sendHelpMenu(commandSender);
            return;
        }

        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase(player.getName()))) {
            if (!plugin.getPermissionManager().hasGrantedPermission("graves.virtual.command", player)) {
                plugin.getEntityManager().sendMessage("message.permission-denied", player);
                return;
            }

            var graves = plugin.getGraveManager().getGraveList(player);

            Grave grave = graves.get(0);

            if (grave == null) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                        + ChatColor.RESET + "You have no graves.");
                return;
            }

            Location deathLoc = grave.getLocationDeath();
            if (deathLoc == null || deathLoc.getWorld() == null) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                        + ChatColor.RESET + "Grave location is unavailable.");
                return;
            }

            plugin.getGraveManager().openGrave(player, grave.getLocationDeath(), grave);
            return;
        }

        if (args.length == 2 && !args[1].equalsIgnoreCase(player.getName())) {
            if (!plugin.getPermissionManager().hasGrantedPermission("graves.virtual.command.others", player)) {
                plugin.getEntityManager().sendMessage("message.permission-denied", player);
                return;
            }

            OfflinePlayer targetPl = plugin.getServer().getOfflinePlayer(args[1]);
            var graves = plugin.getGraveManager().getGraveList(targetPl);

            Grave grave = graves.get(0);

            if (grave == null) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + ChatColor.RED + args[1] + ChatColor.RESET + " has no graves.");

                return;
            }

            Location base = grave.getLocationDeath();
            if (base == null || base.getWorld() == null) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                        + ChatColor.RESET + "Grave location is unavailable.");
                return;
            }

            plugin.getGraveManager().openGrave(player, grave.getLocationDeath(), grave);
            return;
        }

        sendHelpMenu(commandSender);
    }

    private void handleTeleportCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player player)) {
            sendHelpMenu(commandSender);
            return;
        }

        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase(player.getName()))) {
            if (!plugin.getPermissionManager().hasGrantedPermission("graves.teleport.command", player)) {
                plugin.getEntityManager().sendMessage("message.permission-denied", player);
                return;
            }

            var graves = plugin.getGraveManager().getGraveList(player);
            if (graves.isEmpty()) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + "You have no graves.");
                return;
            }

            Grave grave = graves.get(0);
            Location deathLoc = grave.getLocationDeath();
            if (deathLoc == null || deathLoc.getWorld() == null) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + "Grave location is unavailable.");
                return;
            }

            if (plugin.getPermissionManager().hasGrantedPermission("graves.teleport.command.free", player)) {
                CompatibilityTeleport.teleportSafely(player, deathLoc, plugin).thenAccept(ok -> {
                    if (!ok) {
                        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + "Teleport failed.");
                    }
                });
            } else {
                Location target = plugin.getGraveManager()
                        .getGraveLocationList(player.getLocation(), grave)
                        .get(0);
                plugin.getEntityManager().teleportEntity(player, target, grave);
            }
            return;
        }

        if (args.length == 2 && !args[1].equalsIgnoreCase(player.getName())) {
            if (!plugin.getPermissionManager().hasGrantedPermission("graves.teleport.command.others", player)) {
                plugin.getEntityManager().sendMessage("message.permission-denied", player);
                return;
            }

            OfflinePlayer targetPl = plugin.getServer().getOfflinePlayer(args[1]);
            var graves = plugin.getGraveManager().getGraveList(targetPl);
            if (graves.isEmpty()) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + ChatColor.RED + args[1] + ChatColor.RESET + " has no graves.");
                return;
            }

            Grave grave = graves.get(0);
            Location base = grave.getLocationDeath();
            if (base == null || base.getWorld() == null) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + "Grave location is unavailable.");
                return;
            }

            if (plugin.getPermissionManager().hasGrantedPermission("graves.teleport.command.others.free", player)) {
                // Resolve a safe spot near the grave (+1,0,+1) using your helper
                Location target = plugin.getGraveManager().getGraveLocation(base.clone().add(1, 0, 1), grave);
                CompatibilityTeleport.teleportSafely(player, target, plugin).thenAccept(ok -> {
                    if (!ok) {
                        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + "Teleport failed.");
                    }
                });
            } else {
                Location target = plugin.getGraveManager()
                        .getGraveLocationList(player.getLocation(), grave)
                        .get(0);
                plugin.getEntityManager().teleportEntity(player, target, grave);
            }
            return;
        }

        // Otherwise show help
        sendHelpMenu(commandSender);
    }

    private void handleGiveTokenCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player) || plugin.getPermissionManager().hasGrantedPermission("graves.givetoken", ((Player) commandSender).getPlayer())) {
            if (args.length == 1) {
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + ChatColor.RESET + "/graves givetoken {player} {token}");
            } else if (args.length == 2) {
                if (commandSender instanceof Player player) {
                    ItemStack itemStack = plugin.getRecipeManager().getToken(args[1].toLowerCase(Locale.ROOT));

                    if (itemStack != null) {
                        plugin.getEntityManager().sendMessage("message.give-token", player);
                        player.getInventory().addItem(itemStack);
                    } else {
                        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Token " + args[1] + " not found.");
                    }
                } else {
                    commandSender.sendMessage("Only players can run this command, " +
                            "try /graves givetoken {name} {token}");
                }
            } else if (args.length == 3) {
                Player player = plugin.getServer().getPlayer(args[1]);

                if (player != null) {
                    ItemStack itemStack = plugin.getRecipeManager().getToken(args[2].toLowerCase(Locale.ROOT));

                    if (itemStack != null) {
                        plugin.getEntityManager().sendMessage("message.give-token", player);
                        player.getInventory().addItem(itemStack);
                    } else {
                        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Token " + args[2] + " not found.");
                    }
                } else {
                    commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                            + "Player " + args[1] + " not found.");
                }
            } else if (args.length == 4) {
                Player player = plugin.getServer().getPlayer(args[1]);

                if (player != null) {
                    ItemStack itemStack = plugin.getRecipeManager().getToken(args[2].toLowerCase(Locale.ROOT));

                    if (itemStack != null) {
                        try {
                            int amount = Integer.parseInt(args[3]);
                            int count = 0;

                            while (count < amount) {
                                player.getInventory().addItem(itemStack);
                                count++;
                            }
                        } catch (NumberFormatException ignored) {
                            player.getInventory().addItem(itemStack);
                        }

                        plugin.getEntityManager().sendMessage("message.give-token", player);
                    } else {
                        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Token " + args[2] + " not found.");
                    }
                } else {
                    commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                            + "Player " + args[1] + " not found.");
                }
            }
        } else {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handleReloadCommand(CommandSender commandSender) {
        if (!(commandSender instanceof Player) || plugin.getPermissionManager().hasGrantedPermission("graves.reload", ((Player) commandSender).getPlayer())) {
            Plugin skriptPlugin = plugin.getServer().getPluginManager().getPlugin("Skript");
            if (skriptPlugin != null && skriptPlugin.isEnabled()) {
                plugin.getLogger().warning("Skript v." + skriptPlugin.getDescription().getVersion() + " detected. Skript Integration option will only take effect on restart.");
            }
            plugin.reload();
            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                    + "Reloaded config file.");
        } else {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handleDumpCommand(CommandSender commandSender) {
        if (!(commandSender instanceof Player) || plugin.getPermissionManager().hasGrantedPermission("graves.dump", ((Player) commandSender).getPlayer())) {
            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                    + "Running dump functions...");
            plugin.dumpServerInfo(commandSender);
        } else {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handleDebugCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player) || plugin.getPermissionManager().hasGrantedPermission("graves.debug", ((Player) commandSender).getPlayer())) {
            if (args.length > 1) {
                try {
                    plugin.getConfig().set("settings.debug.level", Integer.parseInt(args[1]));

                    if (commandSender instanceof Player player) {
                        List<String> stringList = plugin.getConfig().getStringList("settings.debug.user");

                        stringList.add(player.getUniqueId().toString());
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + ChatColor.RESET + "Debug level changed to: (" + args[1]
                                + "), User (" + player.getName() + ") added, This won't persist "
                                + "across restarts or reloads.");
                    } else {
                        plugin.getLogger().info("Debug level changed to: (" + args[1]
                                + "), This won't persist across restarts or reloads.");
                    }
                } catch (NumberFormatException ignored) {
                    commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                            + ChatColor.RESET + args[1] + " is not a valid int.");
                }
            } else {
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + ChatColor.RESET + "/graves debug {level}");
            }
        } else {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handleCleanupCommand(CommandSender commandSender) {
        if (!(commandSender instanceof Player) || plugin.getPermissionManager().hasGrantedPermission("graves.cleanup", ((Player) commandSender).getPlayer())) {
            List<Grave> graveList = new ArrayList<>(plugin.getCacheManager().getGraveMap().values());

            for (Grave grave : graveList) {
                plugin.getGraveManager().removeGrave(grave);
            }

            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                    + ChatColor.RESET + graveList.size() + " graves cleaned up.");
        } else {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handlePurgeCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player) || plugin.getPermissionManager().hasGrantedPermission("graves.purge", ((Player) commandSender).getPlayer())) {
            if (args.length < 2) {
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + "Please specify a subcommand.");
                return;
            }

            String subcommand = args[1].toLowerCase(Locale.ROOT);
            switch (subcommand) {
                case "holograms", "hologram" -> {
                    int count = 0;
                    for (World world : plugin.getServer().getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (entity.getScoreboardTags().contains("graveHologram")) {
                                entity.remove();
                                count++;
                            }
                        }
                    }
                    commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                            + ChatColor.RESET + count + " holograms purged.");
                }
                case "player", "offline-player" -> {
                    if (args.length < 3) {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Please specify an offline player's name.");
                        return;
                    }

                    String targetOfflinePlayerName = args[2];
                    OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetOfflinePlayerName);

                    if (!targetOfflinePlayer.hasPlayedBefore()) {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Player not found or has never played on this server.");
                        return;
                    }

                    UUID offlinePlayerUUID = targetOfflinePlayer.getUniqueId();
                    boolean offlineGraveFound = false;

                    for (Grave grave : plugin.getCacheManager().getGraveMap().values()) {
                        if (grave.getOwnerUUID().equals(offlinePlayerUUID)) {
                            plugin.getGraveManager().removeGrave(grave);
                            offlineGraveFound = true;
                        }
                    }

                    if (offlineGraveFound) {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Graves of offline player " + targetOfflinePlayerName + " purged.");
                    } else {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "No graves found for offline player " + targetOfflinePlayerName + ".");
                    }
                }
                case "grave-specific", "grave-uuid" -> {
                    if (args.length < 3) {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Please specify a grave UUID.");
                        return;
                    }

                    UUID graveUUIDTarget;
                    try {
                        graveUUIDTarget = UUID.fromString(args[2]);
                    } catch (IllegalArgumentException e) {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Invalid UUID format.");
                        return;
                    }

                    AtomicBoolean graveFound = new AtomicBoolean(false);
                    for (Grave grave : plugin.getCacheManager().getGraveMap().values()) {
                        if (grave.getUUID().equals(graveUUIDTarget)) {
                            int experience = grave.getExperience();
                            if (experience > 0) {
                                grave.getLocationDeath().getWorld().spawn(grave.getLocationDeath(), ExperienceOrb.class,
                                        orb -> orb.setExperience(experience));
                            }

                            Map<EquipmentSlot, ItemStack> items = grave.getEquipmentMap();
                            ItemStack[] graveInventory = grave.getInventory().getContents();

                            // Create a CountDownLatch to wait for both async tasks to finish
                            CountDownLatch latch = new CountDownLatch(2);

                            if (items != null && !items.isEmpty()) {
                                plugin.getSchedulerManager().runTaskAsynchronously(() -> {
                                    List<ItemStack> validItems = new ArrayList<>();
                                    for (ItemStack item : items.values()) {
                                        if (item != null && item.getType() != Material.AIR) {
                                            validItems.add(item);
                                        }
                                    }

                                    plugin.getSchedulerManager().runTask(() -> {
                                        for (ItemStack item : validItems) {
                                            if (validItems.isEmpty()) break;
                                            grave.getLocationDeath().getWorld().dropItem(grave.getLocationDeath(), item);
                                        }
                                        // Signal that the task is done
                                        latch.countDown();
                                    });
                                });
                            } else {
                                latch.countDown(); // If no items, count down immediately
                            }

                            if (graveInventory != null && graveInventory.length > 0) {
                                plugin.getSchedulerManager().runTaskAsynchronously(() -> {
                                    for (ItemStack item : graveInventory) {
                                        if (item != null && item.getAmount() > 0) {
                                            grave.getLocationDeath().getWorld().dropItem(grave.getLocationDeath(), item);
                                        }
                                    }
                                    // Signal that the task is done
                                    latch.countDown();
                                });
                            } else {
                                latch.countDown(); // If no grave inventory, count down immediately
                            }

                            // Wait for both async tasks to complete
                            plugin.getSchedulerManager().runTask(() -> {
                                try {
                                    latch.await(); // Wait until both tasks complete
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }

                                // After both async tasks are finished, remove the grave
                                plugin.getGraveManager().removeGrave(grave);
                                graveFound.set(true);
                            });

                            break;
                        }
                    }

                    if (graveFound.get()) {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Grave UUID " + graveUUIDTarget + " purged.");
                    } else {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "No graves found for UUID " + args[2] + ".");
                    }
                }
                case "abandon", "abandoned" -> {
                    int purged = 0;
                    List<Grave> gravesToPurge = new ArrayList<>();

                    for (Grave grave : plugin.getCacheManager().getGraveMap().values()) {
                        if (plugin.getGraveManager().isGraveAbandoned(grave)) {
                            gravesToPurge.add(grave);
                        }
                    }

                    for (Grave grave : gravesToPurge) {
                        plugin.getGraveManager().removeGrave(grave);
                        purged++;
                    }

                    commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " +
                            ChatColor.RESET + purged + " abandoned grave(s) purged.");
                }
                default -> {
                    List<Grave> allGraves = new ArrayList<>(plugin.getCacheManager().getGraveMap().values());
                    for (Grave grave : allGraves) {
                        plugin.getGraveManager().removeGrave(grave);
                    }
                    commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                            + allGraves.size() + " graves purged.");
                }
            }
        } else {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handleImportCommand(CommandSender commandSender, String[] args) {
        if (args == null || args.length == 0) {
            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                    + "Usage: /graves import {plugin}");
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        boolean isConsole = !(commandSender instanceof Player);
        Player player = isConsole ? null : (Player) commandSender;

        if (!isConsole) {
            if (!plugin.getPermissionManager().hasGrantedPermission("graves.import", player)) {
                plugin.getEntityManager().sendMessage("message.permission-denied", player);
                return;
            }
        }

        if (sub.equals("angelchest")) {
            if (isConsole) {
                consolePendingImport = true;
            } else {
                pendingImports.add(player.getUniqueId());
            }

            plugin.debugMessage(plugin.getImportManager().countAngelChestStatusText(), 1);
            plugin.debugMessage(plugin.getImportManager().listAngelChestMissingWorldText(), 2);

            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET +
                    "This will import " + ChatColor.RED + plugin.getImportManager().countAngelChestImportableOnly() + ChatColor.RESET + " graves from AngelChest. This may create many graves and cannot be undone.\n" +
                    "You bear in mind that hex color codes may not convert over.\n" +
                    ChatColor.YELLOW + "Type " + ChatColor.RED + "/graves import confirm" + ChatColor.YELLOW + " to proceed.");
            return;
        }

        if (sub.equals("confirm")) {
            boolean allowed = isConsole ? consolePendingImport : pendingImports.remove(player.getUniqueId());

            if (!allowed) {
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET +
                        "No pending import request. Run /graves import {plugin} first.");
                return;
            }

            if (isConsole) {
                consolePendingImport = false;
            }

            List<Grave> graveList = plugin.getImportManager().importExternalPluginAngelChest();

            int placed = 0;
            for (Grave grave : graveList) {
                plugin.getDataManager().addGrave(grave);
                if (grave.getLocationDeath() != null) {
                    plugin.getGraveManager().placeGrave(grave.getLocationDeath(), grave);
                    placed++;
                }
            }

            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET +
                    "Imported " + graveList.size() + " graves from AngelChest"
                    + (placed != graveList.size()
                    ? (" (" + placed + " placed, " + (graveList.size() - placed) + " skipped due to missing location)")
                    : "")
                    + ".");
            return;
        }

        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                + "Usage: /graves import {plugin}");
    }
}