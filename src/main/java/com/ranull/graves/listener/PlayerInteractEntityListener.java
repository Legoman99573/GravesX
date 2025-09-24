package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Listener for handling PlayerInteractEntityEvent to interact with ItemFrame entities representing graves.
 */
public class PlayerInteractEntityListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a PlayerInteractEntityListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerInteractEntityListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerInteractEntityEvent when a player interacts with an ItemFrame entity.
     *
     * If the interacted entity is an ItemFrame and represents a grave, the interaction will
     * either open the grave or cancel the event based on the grave's state and the player's actions.
     *
     * The event is only processed if:
     * - The hand used for the interaction is the main hand (or the server version does not support offhand).
     * - The player is not in Spectator mode (except on very old versions that lack Spectator).
     *
     * @param event The PlayerInteractEntityEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();

        if (!isMainHandInteraction(event)) return;
        if (!isItemFrame(clicked)) return;
        if (!isNotSpectatorMode(player)) return;

        Grave grave = plugin.getEntityDataManager().getGrave(clicked);
        if (grave != null) {
            event.setCancelled(plugin.getGraveManager().openGrave(player, clicked.getLocation(), grave));
        }
    }

    /**
     * Checks if the interaction is performed with the main hand.
     */
    private boolean isMainHandInteraction(PlayerInteractEntityEvent event) {
        return !plugin.getVersionManager().hasSecondHand() || event.getHand() == EquipmentSlot.HAND;
    }

    /**
     * Checks if the entity being interacted with is an ItemFrame.
     */
    private boolean isItemFrame(Entity entity) {
        return entity instanceof ItemFrame;
    }

    /**
     * Checks if the player is not in Spectator mode.
     */
    private boolean isNotSpectatorMode(Player player) {
        return plugin.getVersionManager().is_v1_7() || player.getGameMode() != GameMode.SPECTATOR;
    }
}