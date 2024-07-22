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

import java.util.ArrayList;
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

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,
                             @NotNull String string, String[] args) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            // Disable for everyone except JaySmethers, not ready for production.
            if (!player.getName().contains("JaySmethers")) {
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + "Graveyards not ready for production.");
                return true;
            }

            if (args.length < 1) {
                player.sendMessage("/graveyards create");
                player.sendMessage("/graveyards modify");
            } else {
                switch (args[0].toLowerCase()) {
                    case "create":
                        handleCreateCommand(player, args);
                        break;
                    case "modify":
                        handleModifyCommand(player, args);
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

    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                      @NotNull String string, @NotNull String @NotNull [] args) {
        return new ArrayList<>();
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/graveyard create (type)");
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
                player.sendMessage("Unknown type " + args[1]);
                break;
        }
    }

    private void handleModifyCommand(Player player, String[] args) {
        if (plugin.getGraveyardManager().isModifyingGraveyard(player)) {
            plugin.getGraveyardManager().stopModifyingGraveyard(player);
        } else {
            if (args.length < 2) {
                player.sendMessage("/graveyard modify (type)");
            } else {
                switch (args[1].toLowerCase()) {
                    case "worldguard":
                        handleWorldGuardModify(player, args);
                        break;
                    default:
                        player.sendMessage("Unknown type " + args[1]);
                        break;
                }
            }
        }
    }

    private void handleWorldGuardCreate(Player player, String[] args) {
        if (plugin.getIntegrationManager().getWorldGuard() == null) {
            player.sendMessage("WorldGuard not detected");
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.graveyard.worldguard.enabled")) {
            player.sendMessage("WorldGuard support disabled");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("/graveyard create worldguard (region)");
            return;
        }

        String region = args[2];
        World world = plugin.getIntegrationManager().getWorldGuard().getRegionWorld(region);

        if (world == null) {
            player.sendMessage("Region not found " + region);
            return;
        }

        if (!plugin.getIntegrationManager().getWorldGuard().isMember(region, player) && !player.isOp()) {
            player.sendMessage("You are not a member of this region");
            return;
        }

        Graveyard graveyard = plugin.getGraveyardManager()
                .createGraveyard(player.getLocation(), region, world, Graveyard.Type.WORLDGUARD);

        player.sendMessage("Creating graveyard " + region);
        plugin.getGraveyardManager().startModifyingGraveyard(player, graveyard);
    }

    private void handleTownyCreate(Player player, String[] args) {
        if (!plugin.getIntegrationManager().hasTowny()) {
            player.sendMessage("Towny not detected");
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.graveyard.towny.enabled")) {
            player.sendMessage("Towny support disabled");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("/graveyard create towny (name)");
            return;
        }

        String name = args[2].replace("_", " ");

        if (!plugin.getIntegrationManager().getTowny().hasTownPlot(player, name)) {
            player.sendMessage("Plot not found " + name);
            return;
        }

        Graveyard graveyard = plugin.getGraveyardManager()
                .createGraveyard(player.getLocation(), name, player.getWorld(), Graveyard.Type.TOWNY);

        player.sendMessage("Creating graveyard " + name);
        plugin.getGraveyardManager().startModifyingGraveyard(player, graveyard);
    }

    private void handleWorldGuardModify(Player player, String[] args) {
        if (plugin.getIntegrationManager().getWorldGuard() == null) {
            player.sendMessage("WorldGuard not detected");
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.graveyard.worldguard.enabled")) {
            player.sendMessage("WorldGuard support disabled");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("/graveyard modify worldguard (region)");
            return;
        }

        String region = args[2];
        World world = plugin.getIntegrationManager().getWorldGuard().getRegionWorld(region);

        if (world == null) {
            player.sendMessage("Region not found " + region);
            return;
        }

        Graveyard graveyard = plugin.getGraveyardManager().getGraveyardByKey("worldguard|" + world.getName() + "|" + region);

        if (graveyard == null) {
            player.sendMessage("Graveyard " + region + " not found");
            return;
        }

        player.sendMessage("Graveyard found");
        plugin.getGraveyardManager().startModifyingGraveyard(player, graveyard);
    }
}