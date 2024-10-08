package com.ranull.graves.listener.integration.oraxen;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.Oraxen;
import com.ranull.graves.type.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Listens for PlayerInteractEntityEvent and cancels the event if the player interacts with an ItemFrame associated with a grave.
 */
public class PlayerInteractEntityListener implements Listener {
    private final Graves plugin;
    private final Oraxen oraxen;

    /**
     * Constructs a new PlayerInteractEntityListener with the specified Graves and Oraxen instances.
     *
     * @param plugin The Graves instance to use.
     * @param oraxen The Oraxen instance to use.
     */
    public PlayerInteractEntityListener(Graves plugin, Oraxen oraxen) {
        this.plugin = plugin;
        this.oraxen = oraxen;
    }

    /**
     * Handles PlayerInteractEntityEvent. If the player interacts with an ItemFrame associated with a grave,
     * it cancels the event and opens the grave for the player.
     *
     * @param event The PlayerInteractEntityEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFurnitureInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        if (isItemFrame(entity)) {
            handleFurnitureInteraction(event, entity);
        }
    }

    /**
     * Checks if the entity is an ItemFrame.
     *
     * @param entity The entity to check.
     * @return True if the entity is an ItemFrame, false otherwise.
     */
    private boolean isItemFrame(Entity entity) {
        return entity instanceof ItemFrame;
    }

    /**
     * Handles the interaction with the furniture. If the furniture is associated with a grave,
     * the event is cancelled and the grave is opened for the player.
     *
     * @param event  The PlayerInteractEntityEvent.
     * @param entity The entity being interacted with.
     */
    private void handleFurnitureInteraction(PlayerInteractEntityEvent event, Entity entity) {
        Grave grave = oraxen.getGrave(entity);

        if (grave != null) {
            event.setCancelled(plugin.getGraveManager().openGrave(event.getPlayer(), entity.getLocation(), grave));
        }
    }
}