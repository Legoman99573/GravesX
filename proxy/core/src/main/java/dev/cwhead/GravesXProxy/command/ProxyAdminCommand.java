package dev.cwhead.GravesXProxy.command;

import dev.cwhead.GravesXProxy.ProxyCore;
import dev.cwhead.GravesXProxy.platform.ProxyPlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The /gravesproxy admin command (status, reload, forget). Output lines use '&' colour codes. */
public final class ProxyAdminCommand {
    private final ProxyCore core;
    private final ProxyPlatform platform;

    public ProxyAdminCommand(ProxyCore core, ProxyPlatform platform) {
        this.core = core;
        this.platform = platform;
    }

    public List<String> run(boolean admin, String[] args) {
        List<String> out = new ArrayList<>();
        if (!admin) {
            out.add("&cYou do not have permission.");
            return out;
        }

        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                core.reload();
                out.add("&aGravesX proxy config reloaded.");
            }
            case "forget" -> {
                if (args.length < 2) {
                    out.add("&c/gravesproxy forget <server>");
                } else {
                    core.forgetServer(args[1]);
                    out.add("&aForgot GravesX detection and grave data for &f" + args[1] + "&a.");
                }
            }
            default -> status(out);
        }
        return out;
    }

    private void status(List<String> out) {
        out.add("&c☠ &8» &cGravesX Proxy &8- &7servers");
        List<String> names = new ArrayList<>(platform.serverNames());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        for (String s : names) {
            String status;
            if (core.isExcluded(s)) status = "&8excluded";
            else if (core.registry().isInstalled(s)) status = "&aGravesX detected";
            else status = core.config().ignoreWithoutGravesX() ? "&7ignored (no GravesX)" : "&7GravesX not detected";
            out.add("&f" + s + " &8- " + status);
        }
        out.add("&7Graves stored in: &f" + core.storeDescription());
        out.add("&7/gravesproxy reload | forget <server>");
    }

    public List<String> tabComplete(String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length <= 1) {
            result.addAll(List.of("status", "reload", "forget"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("forget")) {
            result.addAll(platform.serverNames());
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        result.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(prefix));
        return result;
    }
}
