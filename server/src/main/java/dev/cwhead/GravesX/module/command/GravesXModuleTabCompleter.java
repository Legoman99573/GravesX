package dev.cwhead.GravesX.module.command;

import dev.cwhead.GravesX.module.ModuleContext;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

/**
 * Tab completer for module commands with simple lifecycle hooks.
 */
public interface GravesXModuleTabCompleter extends TabCompleter {

    /**
     * Called after this completer is registered on a command.
     *
     * @param ctx Module context for accessing config, logger, and scheduler.
     * @param command The {@link PluginCommand} this completer is attached to.
     */
    default void onRegister(ModuleContext ctx, PluginCommand command) {}

    /**
     * Called before this completer is unregistered. Use to release resources.
     */
    default void onUnregister() {}
}