package dev.cwhead.GravesX.listener;

import com.ranull.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

public class PlayerLocaleChangeListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a PlayerLocaleChangeListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerLocaleChangeListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerLocaleChangeEvent to notify players if the language isn't in their native language.
     *
     * @param event The PlayerJoinEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        plugin.getLanguageManager().notifyFallback(event.getPlayer());
    }
}
