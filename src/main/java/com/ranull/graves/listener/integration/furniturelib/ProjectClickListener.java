package com.ranull.graves.listener.integration.furniturelib;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.FurnitureLib;
import com.ranull.graves.type.Grave;
import de.Ste3et_C0st.FurnitureLib.SchematicLoader.Events.ProjectClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listens for ProjectClickEvent from FurnitureLib and handles the event based on the presence of associated graves.
 */
public class ProjectClickListener implements Listener {
    private final Graves plugin;
    private final FurnitureLib furnitureLib;

    /**
     * Constructs a new ProjectClickListener with the specified Graves and FurnitureLib instances.
     *
     * @param plugin The Graves instance to use.
     * @param furnitureLib The FurnitureLib instance to use.
     */
    public ProjectClickListener(Graves plugin, FurnitureLib furnitureLib) {
        this.plugin = plugin;
        this.furnitureLib = furnitureLib;
    }

    /**
     * Handles ProjectClickEvent. If the project being clicked is associated with a grave,
     * it cancels the event and opens the grave for the player.
     *
     * @param event The ProjectClickEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectClick(ProjectClickEvent event) {
        if (event.getID().getUUID() != null) {
            Grave grave = furnitureLib.getGrave(event.getLocation(), event.getID().getUUID());

            if (grave != null) {
                event.setCancelled(plugin.getGraveManager().openGrave(event.getPlayer(), event.getLocation(), grave));
            }
        }
    }
}