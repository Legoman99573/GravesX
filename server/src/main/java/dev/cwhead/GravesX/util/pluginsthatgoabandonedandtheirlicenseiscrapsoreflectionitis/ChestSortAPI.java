package dev.cwhead.GravesX.util.pluginsthatgoabandonedandtheirlicenseiscrapsoreflectionitis;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @deprecated Unmaintained upstream. see see <a href="https://www.spigotmc.org/profile-posts/239137/">here</a>
 * <p>
 * Minimal reflective bridge to {@code de.jeff_media.chestsort.api.ChestSortAPI}. This was done due to the actual jar wanting
 * to use jeff-media repo that now resolves to nothing. This is a messy way to call it, but fuk it
 * </p>
 */
@Deprecated
public final class ChestSortAPI {

    private static final String PLUGIN_NAME = "ChestSort";
    private static final String API_CLASS = "de.jeff_media.chestsort.api.ChestSortAPI";

    private static volatile boolean lookedUp = false;
    private static volatile boolean classAvailable = false;

    private static MethodHandle MH_sortInventory_Inv;

    private ChestSortAPI() {}

    /**
     * @deprecated Unmaintained upstream. see <a href="https://www.spigotmc.org/profile-posts/239137/">here</a>.
     * Sorts an {@link Inventory}.
     *
     * @param inventory inventory to sort (non-null)
     */
    @Deprecated
    public static void sortInventory(Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        if (!isEnabled()) return;
        ensureLookup();
        invokeStaticVoid(MH_sortInventory_Inv, inventory);
    }

    /** One-time reflective lookup of ChestSort API methods. */
    private static synchronized void ensureLookup() {
        if (lookedUp) return;
        lookedUp = true;

        try {
            final Plugin chestSort = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (chestSort == null) {
                classAvailable = false;
                return;
            }

            final ClassLoader cl = chestSort.getClass().getClassLoader();
            final Class<?> apiClazz = Class.forName(API_CLASS, false, cl);
            final MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            MH_sortInventory_Inv = staticHandle(lookup, apiClazz, Inventory.class);

            classAvailable = (MH_sortInventory_Inv != null);
        } catch (Throwable ignored) {
            classAvailable = false;
        }
    }

    /**
     * Gets a {@link MethodHandle} for a public static method or {@code null}.
     */
    private static MethodHandle staticHandle(MethodHandles.Lookup lookup,
                                             Class<?> owner,
                                             Class<?>... params) {
        try {
            final Method m = owner.getMethod("sortInventory", params);
            if (m.getReturnType() != void.class) return null;
            return lookup.unreflect(m);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Invokes a static void handle.
     */
    private static void invokeStaticVoid(MethodHandle mh, Object... args) {
        if (!classAvailable || mh == null) return;
        try {
            mh.invokeWithArguments(args);
        } catch (Throwable ignored) {
            // swallow – best-effort bridge
        }
    }

    /**
     * @return true if ChestSort plugin is present and enabled.
     */
    private static boolean isEnabled() {
        final Plugin p = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        return p != null && p.isEnabled();
    }
}