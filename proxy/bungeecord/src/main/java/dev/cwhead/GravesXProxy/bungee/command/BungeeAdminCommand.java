package dev.cwhead.GravesXProxy.bungee.command;

import dev.cwhead.GravesXProxy.ProxyCore;
import dev.cwhead.GravesXProxy.bungee.platform.BungeePlatform;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.List;

/** /gravesproxy (alias /gxproxy). */
public final class BungeeAdminCommand extends Command implements TabExecutor {
    private static final String PERMISSION = "gravesx.proxy.admin";

    private final ProxyCore core;
    private final BungeePlatform platform;

    public BungeeAdminCommand(ProxyCore core, BungeePlatform platform) {
        super("gravesproxy", PERMISSION, "gxproxy");
        this.core = core;
        this.platform = platform;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean admin = sender.hasPermission(PERMISSION);
        for (String line : core.command(admin, args)) {
            sender.sendMessage(platform.format(line));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return sender.hasPermission(PERMISSION) ? core.tabComplete(args) : List.of();
    }
}
