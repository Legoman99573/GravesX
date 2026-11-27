package dev.cwhead.GravesXProxy.velocity.command;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.cwhead.GravesXProxy.ProxyCore;
import dev.cwhead.GravesXProxy.velocity.platform.VelocityPlatform;

import java.util.List;

/**
 *  Creates gravesproxy command
 */
public final class VelocityAdminCommand implements SimpleCommand {
    private static final String PERMISSION = "gravesx.proxy.admin";

    private final ProxyCore core;
    private final VelocityPlatform platform;

    private VelocityAdminCommand(ProxyCore core, VelocityPlatform platform) {
        this.core = core;
        this.platform = platform;
    }

    public static void register(Object plugin, ProxyServer proxy, ProxyCore core, VelocityPlatform platform) {
        CommandMeta meta = proxy.getCommandManager().metaBuilder("gravesproxy").aliases("gxproxy").plugin(plugin).build();

        proxy.getCommandManager().register(meta, new VelocityAdminCommand(core, platform));
    }

    @Override
    public void execute(Invocation invocation) {
        boolean admin = invocation.source().hasPermission(PERMISSION);
        for (String line : core.command(admin, invocation.arguments())) {
            invocation.source().sendMessage(platform.format(line));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION) ? core.tabComplete(invocation.arguments()) : List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }
}
