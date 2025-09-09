package dev.cwhead.GravesX.api.provider;

import com.ranull.graves.data.EntityData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;

/**
 * Custom grave provider discovered via Bukkit Services.
 * Modules implement this and register it with ModuleContext.registerService(...).
 */
public interface GraveProvider {
    /**
     * Stable, namespaced ID (e.g. "myaddon:furniture"). Used in logs/configs.
     *
     * @return the Grave Provider Namespace ID
     */
    String id();

    /**
     * Lower runs earlier. Use to order multiple providers (default 0).
     *
     * @return the order the grave spawns (lowest has higher priority)
     */
    default int order() {
        return 0;
    }

    /**
     * Create/place any custom objects for this grave at the given location.
     *
     * @param location the location of the grave
     * @param grave the grave to place over the default grave
     */
    void place(Location location, Grave grave) throws Exception;

    /**
     * Remove any custom objects that belong to this grave.
     *
     * @param grave the grave to remove custom objects
     */
    void remove(Grave grave) throws Exception;

    /**
     * Return true if this provider detects something placed for the grave.
     *
     * @param grave the grave to check
     * @return true if the grave is placed. False otherwise.
     */
    boolean isPlaced(Grave grave);

    /**
     * Return true if this provider recognizes the given CUSTOM entity data.
     * (E.g. check a metadata field or namespaced tag your module wrote.)
     *
     * @param data the Custom Entity Data
     * @return if it recognizes the given Custom entity data.
     */
    boolean supports(EntityData data);

    /**
     * Remove a specific CUSTOM entity data record, if supported.
     *
     * @param data the entity data record
     * @return true if handled/removed, false to let others try.
     */
    boolean removeEntityData(EntityData data) throws Exception;
}
