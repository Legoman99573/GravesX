package com.ranull.graves.listener.integration.furniturelib;

import com.ranull.graves.integration.FurnitureLib;
import de.Ste3et_C0st.FurnitureLib.SchematicLoader.Events.ProjectBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listens for ProjectBreakEvent from FurnitureLib and handles the event based on the presence of associated graves.
 */
public class ProjectBreakListener implements Listener {
    private final FurnitureLib furnitureLib;

    /**
     * Constructs a new ProjectBreakListener with the specified FurnitureLib instance.
     *
     * @param furnitureLib The FurnitureLib instance to use.
     */
    public ProjectBreakListener(FurnitureLib furnitureLib) {
        this.furnitureLib = furnitureLib;
    }

    /**
     * Handles ProjectBreakEvent. If the project being broken is associated with a grave,
     * it cancels the event to prevent the break.
     *
     * @param event The ProjectBreakEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectBreak(ProjectBreakEvent event) {
        if (isProjectAssociatedWithGrave(event)) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if the project being broken is associated with a grave.
     *
     * @param event The ProjectBreakEvent.
     * @return True if the project is associated with a grave, false otherwise.
     */
    private boolean isProjectAssociatedWithGrave(ProjectBreakEvent event) {
        return event.getID().getUUID() != null
                && furnitureLib.getGrave(event.getLocation(), event.getID().getUUID()) != null;
    }
}