package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listens for EntityDamageByEntityEvent to manage damage to specific entities.
 */
public class EntityDamageByEntityListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new EntityDamageByEntityListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public EntityDamageByEntityListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles EntityDamageByEntityEvent to determine if damage should be cancelled
     * based on the entity type and associated data.
     *
     * @param event The EntityDamageByEntityEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        // Cancel the event if the entity is an ItemFrame or an ArmorStand, and it is associated with a grave
        if (entity instanceof ItemFrame
                || (plugin.getVersionManager().is_v1_7() || entity instanceof ArmorStand)) {
            event.setCancelled(plugin.getEntityDataManager().getGrave(entity) != null);
        }
    }
}