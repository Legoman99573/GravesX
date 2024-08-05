package com.ranull.graves.command;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles command execution and tab completion for the Graves plugin.
 */
public final class GravesCommand implements CommandExecutor, TabCompleter {
    private final Graves plugin;

    /**
     * Constructor to initialize the GravesCommand with the Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public GravesCommand(Graves plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,
                             @NotNull String string, String[] args) {
        if (args.length < 1) {
            if (commandSender instanceof Player) {
                Player player = (Player) commandSender;
                if (plugin.hasGrantedPermission("graves.gui", player)) {
                    plugin.getGUIManager().openGraveList(player);
                } else {
                    plugin.getEntityManager().sendMessage("message.permission-denied", player);
                }
            } else {
                sendHelpMenu(commandSender);
            }
        } else {
            switch (args[0].toLowerCase()) {
                case "list":
                case "gui":
                    handleListGuiCommand(commandSender, args);
                    break;
                case "givetoken":
                    handleGiveTokenCommand(commandSender, args);
                    break;
                case "reload":
                    handleReloadCommand(commandSender);
                    break;
                case "dump":
                    handleDumpCommand(commandSender);
                    break;
                case "debug":
                    handleDebugCommand(commandSender, args);
                    break;
                case "cleanup":
                    handleCleanupCommand(commandSender);
                    break;
                case "purge":
                    handlePurgeCommand(commandSender, args);
                    break;
                case "import":
                    handleImportCommand(commandSender);
                    break;
                case "help":
                default:
                    sendHelpMenu(commandSender);
                    break;
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
        sender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Graves "
                + ChatColor.DARK_GRAY + "v" + plugin.getVersion());

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (plugin.hasGrantedPermission("graves.gui", player)) {
                sender.sendMessage(ChatColor.RED + "/graves " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Graves GUI");
            }

            if (plugin.hasGrantedPermission("graves.gui", player)) {
                if (plugin.hasGrantedPermission("graves.gui.other", player)) {
                    sender.sendMessage(ChatColor.RED + "/graves list {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET +
                            " View player graves");
                } else {
                    sender.sendMessage(ChatColor.RED + "/graves list " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET +
                            " Player graves");
                }
            }

            if (plugin.hasGrantedPermission("graves.givetoken", player)) {
                if (plugin.getConfig().getBoolean("settings.token")) {
                    sender.sendMessage(ChatColor.RED + "/graves givetoken {player} {amount} " + ChatColor.DARK_GRAY + "-"
                            + ChatColor.RESET + " Give grave token");
                }
            }

            if (plugin.hasGrantedPermission("graves.reload", player)) {
                sender.sendMessage(ChatColor.RED + "/graves reload " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                        + " Reload plugin");
            }

            if (plugin.hasGrantedPermission("graves.dump", player)) {
                sender.sendMessage(ChatColor.RED + "/graves dump " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                        + " Dump server information");
            }

            if (plugin.hasGrantedPermission("graves.debug", player)) {
                sender.sendMessage(ChatColor.RED + "/graves debug {level} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                        + " Change debug level");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "/graves list {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET +
                    " View player graves");
            sender.sendMessage(ChatColor.RED + "/graves givetoken {player} {amount} " + ChatColor.DARK_GRAY + "-"
                    + ChatColor.RESET + " Give grave token");
            sender.sendMessage(ChatColor.RED + "/graves reload " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                    + " Reload plugin");
            sender.sendMessage(ChatColor.RED + "/graves dump " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                    + " Dump server information");
            sender.sendMessage(ChatColor.RED + "/graves debug {level} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                    + " Change debug level");
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "Author: " + ChatColor.RED + "Ranull");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                      @NotNull String string, @NotNull String @NotNull [] args) {
        List<String> stringList = new ArrayList<>();

        if (args.length == 1) {
            stringList.add("help");

            if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.gui", (Player) commandSender)) {
                stringList.add("list");
                stringList.add("gui");
            }

            if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.reload", (Player) commandSender)) {
                stringList.add("reload");
            }

            if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.dump", (Player) commandSender)) {
                stringList.add("dump");
            }

            if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.debug", (Player) commandSender)) {
                stringList.add("debug");
            }

            if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.cleanup", (Player) commandSender)) {
                stringList.add("cleanup");
            }

            if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.purge", (Player) commandSender)) {
                stringList.add("purge");
            }

            if (plugin.getRecipeManager() != null && (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.givetoken", (Player) commandSender))) {
                stringList.add("givetoken");
            }
        } else if (args.length > 1) {
            if ((args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("gui"))
                    && (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.gui.other", (Player) commandSender))) {
                plugin.getServer().getOnlinePlayers().forEach((player -> stringList.add(player.getName())));
            } else if (args[0].equals("debug") && (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.debug", (Player) commandSender))) {
                stringList.add("0");
                stringList.add("1");
                stringList.add("2");
            } else if (args[0].equals("purge") && (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.debug", (Player) commandSender))) {
                if (args.length == 2) {
                    stringList.add("graves");
                    stringList.add("holograms");
                }
            } else if (plugin.getRecipeManager() != null && args[0].equalsIgnoreCase("givetoken")
                    && (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.givetoken", (Player) commandSender))) {
                if (args.length == 2) {
                    plugin.getServer().getOnlinePlayers().forEach((player -> stringList.add(player.getName())));
                } else if (args.length == 3) {
                    stringList.addAll(plugin.getRecipeManager().getTokenList());
                }
            }
        }

        return stringList;
    }

    private void handleListGuiCommand(CommandSender commandSender, String[] args) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            if (args.length == 1) {
                if (plugin.hasGrantedPermission("graves.gui", player)) {
                    plugin.getGUIManager().openGraveList(player);
                } else {
                    plugin.getEntityManager().sendMessage("message.permission-denied", player);
                }
            } else if (args.length == 2) {
                if (plugin.hasGrantedPermission("graves.gui.other", player)) {
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

    private void handleGiveTokenCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.gui", (Player) commandSender)) {
            if (args.length == 1) {
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + ChatColor.RESET + "/graves givetoken {player} {token}");
            } else if (args.length == 2) {
                if (commandSender instanceof Player) {
                    Player player = (Player) commandSender;
                    ItemStack itemStack = plugin.getRecipeManager().getToken(args[1].toLowerCase());

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
                    ItemStack itemStack = plugin.getRecipeManager().getToken(args[2].toLowerCase());

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
                    ItemStack itemStack = plugin.getRecipeManager().getToken(args[2].toLowerCase());

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
        } else if (commandSender instanceof Player) {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handleReloadCommand(CommandSender commandSender) {
        if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.reload", (Player) commandSender)) {
            Plugin skriptPlugin = plugin.getServer().getPluginManager().getPlugin("Skript");
            if (skriptPlugin != null && skriptPlugin.isEnabled()) {
                plugin.getLogger().warning("Skript v." + skriptPlugin.getDescription().getVersion() + " detected. Skript Integration option will only take effect on restart.");
            }
            plugin.reload();
            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                    + "Reloaded config file.");
        } else if (commandSender instanceof Player) {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handleDumpCommand(CommandSender commandSender) {
        if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.dump", (Player) commandSender)) {
            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                    + "Running dump functions...");
            plugin.dumpServerInfo(commandSender);
        } else if (commandSender instanceof Player) {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handleDebugCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.debug", (Player) commandSender)) {
            if (args.length > 1) {
                try {
                    plugin.getConfig().set("settings.debug.level", Integer.parseInt(args[1]));

                    if (commandSender instanceof Player) {
                        Player player = (Player) commandSender;
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
        } else if (commandSender instanceof Player) {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handleCleanupCommand(CommandSender commandSender) {
        if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.cleanup", (Player) commandSender)) {
            List<Grave> graveList = new ArrayList<>(plugin.getCacheManager().getGraveMap().values());

            for (Grave grave : graveList) {
                plugin.getGraveManager().removeGrave(grave);
            }

            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                    + ChatColor.RESET + graveList.size() + " graves cleaned up.");
        } else if (commandSender instanceof Player) {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handlePurgeCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.purge", (Player) commandSender)) {
            if (args.length > 1 && !args[1].equalsIgnoreCase("graves") && !args[1].equalsIgnoreCase("grave")) {
                if (args[1].equalsIgnoreCase("holograms") || args[1].equalsIgnoreCase("hologram")) {
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
            } else {
                List<Grave> graveList = new ArrayList<>(plugin.getCacheManager().getGraveMap().values());

                for (Grave grave : graveList) {
                    plugin.getGraveManager().removeGrave(grave);
                }

                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + graveList.size() + " graves purged.");
            }
        } else if (commandSender instanceof Player) {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }

    private void handleImportCommand(CommandSender commandSender) {
        if (!(commandSender instanceof Player) || plugin.hasGrantedPermission("graves.import", (Player) commandSender)) {
            // Disable for everyone except Ranull, not ready for production.
            if (!commandSender.getName().equals("Ranull") && !commandSender.getName().equals("JaySmethers") && !commandSender.getName().equals("Legoman99573")) {
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + "Import functionality not ready for production.");
                return;
            }

            List<Grave> graveList = plugin.getImportManager().importExternalPluginGraves();

            for (Grave grave : graveList) {
                plugin.getDataManager().addGrave(grave);
                plugin.getGraveManager().placeGrave(grave.getLocationDeath(), grave);
            }

            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                    + " Imported " + graveList.size() + " graves.");
        } else if (commandSender instanceof Player) {
            plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
        }
    }
}