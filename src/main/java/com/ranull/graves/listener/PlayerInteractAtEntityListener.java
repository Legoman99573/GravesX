package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Listener for handling PlayerInteractAtEntityEvent to interact with graves represented by ArmorStands.
 */
public class PlayerInteractAtEntityListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a PlayerInteractAtEntityListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public PlayerInteractAtEntityListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerInteractAtEntityEvent when a player interacts with an ArmorStand entity.
     *
     * If the interacted entity is an ArmorStand and represents a grave, the interaction will
     * either open the grave or cancel the event based on the grave's state and the player's actions.
     *
     * The event is only processed if:
     * - The hand used for the interaction is the main hand (or the plugin version does not support a second hand).
     * - The player is not in Spectator mode (if the server version is 1.7).
     *
     * @param event The PlayerInteractAtEntityEvent to handle.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();

        if (isMainHandInteraction(event) && isArmorStand(event.getRightClicked()) && isNotSpectatorMode(player)) {
            Entity entity = event.getRightClicked();
            Grave grave = plugin.getEntityDataManager().getGrave(entity);

            if (grave != null) {
                event.setCancelled(plugin.getGraveManager().openGrave(player, entity.getLocation(), grave));
            }
        }
    }

    /**
     * Checks if the interaction is performed with the main hand.
     *
     * @param event The PlayerInteractAtEntityEvent.
     * @return True if the interaction is performed with the main hand, false otherwise.
     */
    private boolean isMainHandInteraction(PlayerInteractAtEntityEvent event) {
        return !plugin.getVersionManager().hasSecondHand() || event.getHand() == EquipmentSlot.HAND;
    }

    /**
     * Checks if the entity being interacted with is an ArmorStand.
     *
     * @param entity The entity being interacted with.
     * @return True if the entity is an ArmorStand, false otherwise.
     */
    private boolean isArmorStand(Entity entity) {
        return entity instanceof ArmorStand;
    }

    /**
     * Checks if the player is not in Spectator mode.
     *
     * @param player The player to check.
     * @return True if the player is not in Spectator mode, false otherwise.
     */
    private boolean isNotSpectatorMode(Player player) {
        return plugin.getVersionManager().is_v1_7() || player.getGameMode() != GameMode.SPECTATOR;
    }
}