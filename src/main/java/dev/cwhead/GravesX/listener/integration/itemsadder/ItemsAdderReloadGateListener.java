package dev.cwhead.GravesX.listener.integration.itemsadder;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.integration.ItemsAdder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

/**
 * Listens for ItemsAdder reload-related commands and temporarily gates the
 * GravesX ItemsAdder integration as "not ready".
 *
 * <p>This prevents integration logic from running while ItemsAdder is rebuilding
 * its data (e.g., during {@code /iareload} or {@code /iazip}). Readiness should be
 * re-enabled when {@code ItemsAdderLoadDataEvent} fires again.</p>
 */
public final class ItemsAdderReloadGateListener implements Listener {

    /** GravesX plugin instance. */
    private final Graves plugin;

    /** ItemsAdder integration wrapper. */
    private final ItemsAdder integration;

    /**
     * Creates the listener.
     *
     * @param plugin GravesX plugin instance
     * @param integration ItemsAdder integration wrapper
     */
    public ItemsAdderReloadGateListener(Graves plugin, ItemsAdder integration) {
        this.plugin = plugin;
        this.integration = integration;
    }

    /**
     * Detects reload commands sent by players and gates integration readiness.
     *
     * @param event player command event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (isReloadCommand(msg)) {
            gateNotReady("player", msg);
        }
    }

    /**
     * Detects reload commands sent by the console and gates integration readiness.
     *
     * @param event console command event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent event) {
        String cmd = event.getCommand();
        if (isReloadCommand(cmd)) {
            gateNotReady("console", cmd);
        }
    }

    /**
     * Checks whether the raw command string represents an ItemsAdder reload command.
     *
     * <p>Handles optional leading {@code /}, optional namespace prefix (e.g. {@code ia:iareload}),
     * and ignores trailing arguments.</p>
     *
     * @param raw raw command line
     * @return {@code true} if the command is {@code iareload} or {@code iazip}
     */
    private boolean isReloadCommand(String raw) {
        String s = raw.trim().toLowerCase();
        if (s.startsWith("/")) s = s.substring(1);

        int space = s.indexOf(' ');
        String base = (space >= 0) ? s.substring(0, space) : s;
        String noNamespace = base.contains(":") ? base.substring(base.indexOf(':') + 1) : base;

        return noNamespace.equals("iareload") || noNamespace.equals("iazip");
    }

    /**
     * Marks the integration as not ready on the plugin scheduler (thread-safe),
     * and logs a debug message once per reload trigger.
     *
     * @param source command source label (e.g. {@code "player"} or {@code "console"})
     * @param cmd raw command line
     */
    private void gateNotReady(String source, String cmd) {
        plugin.getGravesXScheduler().runTask(() -> {
            if (integration.isReady()) {
                integration.setReady(false);
                plugin.debugMessage(
                        "ItemsAdder reload detected (" + source + ": " + cmd + "). Integration gated until ItemsAdderLoadDataEvent.",
                        1
                );
            }
        });
    }
}
