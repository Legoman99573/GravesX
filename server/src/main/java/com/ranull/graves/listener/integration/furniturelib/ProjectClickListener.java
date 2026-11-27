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
        if (hasUUID(event)) {
            handleProjectClick(event);
        }
    }

    /**
     * Checks if the ProjectClickEvent has a UUID associated with it.
     *
     * @param event The ProjectClickEvent.
     * @return True if the event has a UUID, false otherwise.
     */
    private boolean hasUUID(ProjectClickEvent event) {
        return event.getID().getUUID() != null;
    }

    /**
     * Handles the ProjectClickEvent. If the project is associated with a grave,
     * cancels the event and opens the grave for the player.
     *
     * @param event The ProjectClickEvent.
     */
    private void handleProjectClick(ProjectClickEvent event) {
        Grave grave = furnitureLib.getGrave(event.getLocation(), event.getID().getUUID());

        if (grave != null) {
            event.setCancelled(plugin.getGraveManager().openGrave(event.getPlayer(), event.getLocation(), grave));
        }
    }
}