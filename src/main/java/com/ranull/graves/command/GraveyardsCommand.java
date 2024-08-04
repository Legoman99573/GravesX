package com.ranull.graves.command;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Graveyard;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles command execution and tab completion for the Graveyards functionality.
 */
public final class GraveyardsCommand implements CommandExecutor, TabCompleter {
    private final Graves plugin;

    /**
     * Constructor to initialize the GraveyardsCommand with the Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public GraveyardsCommand(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the given command, returning its success.
     *
     * @param commandSender Source of the command.
     * @param command       Command which was executed.
     * @param string        Alias of the command which was used.
     * @param args          Passed command arguments.
     * @return true if a valid command, otherwise false.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,
                             @NotNull String string, String[] args) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            // Disable for everyone except JaySmethers, Legoman99573, and Ranull, not ready for production.
            if (!player.getName().contains("Ranull") && !player.getName().contains("JaySmethers") && !player.getName().contains("Legoman99573")) {
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + "Graveyards not ready for production.");
                return true;
            }

            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "/graveyards create");
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "/graveyards modify");
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "/graveyards delete");
            } else {
                switch (args[0].toLowerCase()) {
                    case "create":
                        try {
                            handleCreateCommand(player, args);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case "modify":
                        try {
                            handleModifyCommand(player, args);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case "delete":
                        handleDeleteCommand(player, args);
                        break;
                    default:
                        player.sendMessage("Unknown command " + args[0]);
                        break;
                }
            }
        } else {
            commandSender.sendMessage("Only players can run graveyard commands");
        }

        return true;
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param commandSender Source of the command.
     * @param command       Command which was executed.
     * @param string        Alias of the command which was used.
     * @param args          Passed command arguments.
     * @return A List of possible completions for the final argument, or null to default to the command executor.
     */
    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                      @NotNull String string, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "modify", "delete"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("modify")) {
                if (plugin.getIntegrationManager().getWorldGuard() != null && plugin.getIntegrationManager().getWorldEdit() != null) {
                    completions.add("worldguard");
                }
                if (plugin.getIntegrationManager().hasTowny()) {
                    completions.add("towny");
                }
            } else if (args[0].equalsIgnoreCase("delete")) {
                completions.addAll(getExistingGraveyardNames());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("create")) {
                if (args[1].equalsIgnoreCase("worldguard") && plugin.getIntegrationManager().getWorldGuard() != null) {
                    completions.addAll(plugin.getIntegrationManager().getWorldGuard().getRegions());
                } else if (args[1].equalsIgnoreCase("towny") && plugin.getIntegrationManager().hasTowny()) {
                    completions.addAll(plugin.getIntegrationManager().getTowny().getTownNames());
                }
            }
        }
        Collections.sort(completions);
        return completions;
    }

    /**
     * Retrieves a list of existing graveyard names for tab completion.
     *
     * @return A List of existing graveyard names.
     */
    private List<String> getExistingGraveyardNames() {
        List<String> names = new ArrayList<>();
        for (Graveyard graveyard : plugin.getGraveyardManager().getAllGraveyardArray()) {
            names.add(graveyard.getName());
        }
        return names;
    }

    /**
     * Handles the create command.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     * @throws InvocationTargetException If there is an error during command execution.
     */
    private void handleCreateCommand(Player player, String[] args) throws InvocationTargetException {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "/graveyard create (type)");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "worldguard":
                handleWorldGuardCreate(player, args);
                break;
            case "towny":
                handleTownyCreate(player, args);
                break;
            default:
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Unknown type " + args[1]);
                break;
        }
    }

    /**
     * Handles the modify command.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     * @throws InvocationTargetException If there is an error during command execution.
     */
    private void handleModifyCommand(Player player, String[] args) throws InvocationTargetException {
        if (plugin.getGraveyardManager().isModifyingGraveyard(player)) {
            plugin.getGraveyardManager().stopModifyingGraveyard(player);
        } else {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "/graveyard modify (type)");
            } else {
                switch (args[1].toLowerCase()) {
                    case "worldguard":
                        handleWorldGuardModify(player, args);
                        break;
                    case "towny":
                        handleTownyModify(player, args);
                    default:
                        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Unknown type " + args[1]);
                        break;
                }
            }
        }
    }

    /**
     * Handles the delete command.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     */
    private void handleDeleteCommand(Player player, String[] args) {
        // Check if enough arguments are provided
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "/graveyard delete (name)");
            return;
        }

        String graveyardName = args[1];
        Graveyard graveyard = plugin.getGraveyardManager().getGraveyardByName(graveyardName);

        if (graveyard != null) {
            plugin.getGraveyardManager().deleteGraveyard(player, graveyard);
        } else {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Graveyard not found: " + graveyardName);
        }
    }

    /**
     * Handles the WorldGuard create command.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     * @throws InvocationTargetException If there is an error during command execution.
     */
    private void handleWorldGuardCreate(Player player, String[] args) throws InvocationTargetException {
        if (plugin.getIntegrationManager().getWorldGuard() == null) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "WorldGuard not detected");
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.graveyard.worldguard.enabled")) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "WorldGuard support disabled");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "/graveyard create worldguard (region)");
            return;
        }

        String region = args[2];
        World world = plugin.getIntegrationManager().getWorldGuard().getRegionWorld(region);

        if (world == null) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Region not found " + region);
            return;
        }

        if (!plugin.getIntegrationManager().getWorldGuard().isMember(region, player) && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "You are not a member of this region");
            return;
        }

        Graveyard graveyard = plugin.getGraveyardManager()
                .createGraveyard(player.getLocation(), region, world, Graveyard.Type.WORLDGUARD);

        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Creating graveyard " + region);
        plugin.getGraveyardManager().startModifyingGraveyard(player, graveyard);
    }

    /**
     * Handles the Towny create command.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     * @throws InvocationTargetException If there is an error during command execution.
     */
    private void handleTownyCreate(Player player, String[] args) throws InvocationTargetException {
        if (!plugin.getIntegrationManager().hasTowny()) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Towny not detected");
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.graveyard.towny.enabled")) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Towny support disabled");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "/graveyard create towny (name)");
            return;
        }

        String name = args[2].replace("_", " ");

        if (!plugin.getIntegrationManager().getTowny().hasTownPlot(player, name)) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Plot not found " + name);
            return;
        }

        Graveyard graveyard = plugin.getGraveyardManager()
                .createGraveyard(player.getLocation(), name, player.getWorld(), Graveyard.Type.TOWNY);

        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Creating graveyard " + name);
        plugin.getGraveyardManager().startModifyingGraveyard(player, graveyard);
    }

    /**
     * Handles the WorldGuard modify command.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     * @throws InvocationTargetException If there is an error during command execution.
     */
    private void handleWorldGuardModify(Player player, String[] args) throws InvocationTargetException {
        if (plugin.getIntegrationManager().getWorldGuard() == null) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "WorldGuard not detected");
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.graveyard.worldguard.enabled")) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "WorldGuard support disabled");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "/graveyard modify worldguard (region)");
            return;
        }

        String region = args[2];
        World world = plugin.getIntegrationManager().getWorldGuard().getRegionWorld(region);

        if (world == null) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Region not found " + region);
            return;
        }

        Graveyard graveyard = plugin.getGraveyardManager().getGraveyardByKey("worldguard|" + world.getName() + "|" + region);

        if (graveyard == null) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Graveyard " + region + " not found");
            return;
        }

        player.sendMessage("Graveyard found");
        plugin.getGraveyardManager().startModifyingGraveyard(player, graveyard);
    }

    /**
     * Handles the Towny modify command.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     * @throws InvocationTargetException If there is an error during command execution.
     */
    private void handleTownyModify(Player player, String[] args) throws InvocationTargetException {
        if (!plugin.getIntegrationManager().hasTowny()) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Towny not detected");
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.graveyard.towny.enabled")) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Towny support disabled");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "/graveyard modify towny (name)");
            return;
        }

        String name = args[2].replace("_", " ");
        Graveyard graveyard = plugin.getGraveyardManager().getGraveyardByKey("towny|" + player.getWorld().getName() + "|" + name);

        if (graveyard == null) {
            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Graveyard not found: " + name);
            return;
        }

        player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Graveyard found");
        plugin.getGraveyardManager().startModifyingGraveyard(player, graveyard);
    }
}