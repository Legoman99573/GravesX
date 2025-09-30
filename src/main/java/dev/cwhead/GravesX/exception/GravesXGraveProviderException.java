package dev.cwhead.GravesX.exception;

import dev.cwhead.GravesX.api.provider.GraveProvider;
import org.bukkit.Location;
import com.ranull.graves.type.Grave;

public class GravesXGraveProviderException extends Exception {
    public GravesXGraveProviderException(String message) { super(message); }
    public GravesXGraveProviderException(String message, Throwable cause) { super(message, cause); }

    public static GravesXGraveProviderException forProvider(
            GraveProvider p, Location loc, Grave grave, String detail, Throwable cause) {

        String world = (loc != null && loc.getWorld() != null) ? loc.getWorld().getName() : "null";
        String msg = "[CustomGraveProvider " + (p != null ? p.id() : "<unknown>")
                + " (order=" + (p != null ? p.order() : -1) + ")] "
                + detail + " @ " + world + "[" + (loc != null ? loc.getBlockX() : 0) + ","
                + (loc != null ? loc.getBlockY() : 0) + "," + (loc != null ? loc.getBlockZ() : 0) + "]"
                + " grave=" + (grave != null ? grave.getUUID() : "null");
        return (cause == null) ? new GravesXGraveProviderException(msg)
                : new GravesXGraveProviderException(msg, cause);
    }

    public static GravesXGraveProviderException forProvider(
            GraveProvider p, Location loc, Grave grave, Throwable cause) {
        String detail = "place() failed: " + (cause != null ? cause.getMessage() : "unknown");
        return forProvider(p, loc, grave, detail, cause);
    }
}