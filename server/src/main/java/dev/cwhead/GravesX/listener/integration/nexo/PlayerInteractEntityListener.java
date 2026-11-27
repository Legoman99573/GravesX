package dev.cwhead.GravesX.listener.integration.nexo;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.integration.Nexo;
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
    private final Nexo nexo;

    /**
     * Constructs a new PlayerInteractEntityListener with the specified Graves and Nexo instances.
     *
     * @param plugin The Graves instance to use.
     * @param nexo The Nexo instance to use.
     */
    public PlayerInteractEntityListener(Graves plugin, Nexo nexo) {
        this.plugin = plugin;
        this.nexo = nexo;
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
        Grave grave = nexo.getGrave(entity);

        if (grave != null) {
            event.setCancelled(true);

            plugin.getGraveManager().openGrave(event.getPlayer(), entity.getLocation(), grave);
        }
    }
}