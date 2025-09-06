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
     */
    String id();

    /**
     * Lower runs earlier. Use to order multiple providers (default 0).
     */
    default int order() { return 0; }

    /**
     * Create/place any custom objects for this grave at the given location.
     */
    void place(Location location, Grave grave) throws Exception;

    /**
     * Remove any custom objects that belong to this grave.
     */
    void remove(Grave grave) throws Exception;

    /**
     * Return true if this provider detects something placed for the grave.
     */
    boolean isPlaced(Grave grave);

    /**
     * Return true if this provider recognizes the given CUSTOM entity data.
     * (E.g. check a metadata field or namespaced tag your module wrote.)
     */
    boolean supports(EntityData data);

    /**
     * Remove a specific CUSTOM entity data record, if supported.
     *
     * @return true if handled/removed, false to let others try.
     */
    boolean removeEntityData(EntityData data) throws Exception;
}
