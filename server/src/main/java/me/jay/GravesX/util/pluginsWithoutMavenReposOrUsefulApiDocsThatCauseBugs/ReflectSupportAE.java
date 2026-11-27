package me.jay.GravesX.util.pluginsWithoutMavenReposOrUsefulApiDocsThatCauseBugs;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Reflected class for AEAPI because you have to pay to use the API. What dumbass thinks that is a good idea.
 * We only include this because people want it.
 *
 * <p>Java 17–compatible, no functional changes: just caches reflective lookups and fails closed.</p>
 */
public final class ReflectSupportAE {

    private ReflectSupportAE() {}

    private static volatile boolean lookedUp = false;
    private static volatile boolean available = false;

    private static Class<?> AEAPI;
    private static Method M_HAS_CUSTOM;
    private static Method M_GET_ENCHANTS;
    private static Method M_HAS_WHITESCROLL;

    /**
     * Checks if the given ItemStack has the "Soulbound" enchantment,
     * without ever statically linking against AEAPI.
     */
    public static boolean isSoulbound(ItemStack itemStack) {
        ensureLookup();
        if (!available || itemStack == null) return false;

        try {
            if (M_HAS_CUSTOM != null) {
                Object res = M_HAS_CUSTOM.invoke(null, "Soulbound", itemStack);
                if (Boolean.TRUE.equals(res)) return true;
            }

            if (M_GET_ENCHANTS != null) {
                @SuppressWarnings("unchecked")
                Map<String, ?> map = (Map<String, ?>) M_GET_ENCHANTS.invoke(null, itemStack);
                return map != null && map.containsKey("Soulbound");
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    /**
     * Checks if the given ItemStack has the "WhitScroll" enchantment,
     * without ever statically linking against AEAPI.
     */
    public static boolean hasWhitScroll(ItemStack itemStack) {
        ensureLookup();
        if (!available || itemStack == null) return false;

        try {
            if (M_HAS_WHITESCROLL != null) {
                Object res = M_HAS_WHITESCROLL.invoke(null, itemStack);
                return Boolean.TRUE.equals(res);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private static synchronized void ensureLookup() {
        if (lookedUp) return;
        lookedUp = true;
        try {
            AEAPI = Class.forName("AEAPI");

            try {
                M_HAS_CUSTOM = AEAPI.getMethod("hasCustomEnchant", String.class, ItemStack.class);
            } catch (NoSuchMethodException ignored) {
                M_HAS_CUSTOM = null;
            }

            try {
                M_GET_ENCHANTS = AEAPI.getMethod("getEnchantmentsOnItem", ItemStack.class);
            } catch (NoSuchMethodException ignored) {
                M_GET_ENCHANTS = null;
            }

            try {
                M_HAS_WHITESCROLL = AEAPI.getMethod("hasWhitescroll", ItemStack.class);
            } catch (NoSuchMethodException ignored) {
                M_HAS_WHITESCROLL = null;
            }

            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
    }
}