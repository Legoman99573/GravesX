package dev.cwhead.GravesX.module.command;

import org.bukkit.command.CommandExecutor;

import java.util.Collections;
import java.util.List;

/**
 * Command executor with optional metadata used by the module registrar
 * to auto-fill name, description, usage, permission, and aliases.
 */
public interface GravesXModuleCommand extends CommandExecutor {

    /**
     * Command name override. If different from the YAML key, it may be added as an alias.
     *
     * @return Command name or {@code null} to keep the YAML/default.
     */
    default String getName() { return null; }

    /**
     * Short help text shown in command lists.
     *
     * @return Description string, empty if none.
     */
    default String getDescription() { return ""; }

    /**
     * Usage string shown on errors or help.
     *
     * @return Usage text (e.g. {@code "/cmd <arg>"}), or {@code null} for default.
     */
    default String getUsage() { return null; }

    /**
     * Permission node required to run the command.
     *
     * @return Permission node or {@code null} for default.
     */
    default String getPermission() { return null; }

    /**
     * Additional names that run the same command.
     *
     * @return List of aliases, possibly empty.
     */
    default List<String> getAliases() { return Collections.emptyList(); }
}
