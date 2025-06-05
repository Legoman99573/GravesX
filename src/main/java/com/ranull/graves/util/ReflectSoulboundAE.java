package com.ranull.graves.util;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class ReflectSoulboundAE {
    /**
     * Checks if the given ItemStack has the "Soulbound" enchantment,
     * without ever statically linking against AEAPI.
     */
    public static boolean isSoulbound(ItemStack itemStack) {
        try {
            // 1) Load the AEAPI class by name
            Class<?> aeapiClass = Class.forName("AEAPI");

            // 2) Find and invoke hasCustomEnchant(String, ItemStack)
            Method hasCustom = aeapiClass.getMethod("hasCustomEnchant", String.class, ItemStack.class);
            Boolean hasCustomResult = (Boolean) hasCustom.invoke(null, "Soulbound", itemStack);
            if (Boolean.TRUE.equals(hasCustomResult)) {
                return true;
            }

            // 3) Find and invoke getEnchantmentsOnItem(ItemStack) → Map
            Method getEnchants = aeapiClass.getMethod("getEnchantmentsOnItem", ItemStack.class);
            @SuppressWarnings("unchecked")
            Map<String, ?> enchantMap = (Map<String, ?>) getEnchants.invoke(null, itemStack);

            // 4) Check if the Map contains "Soulbound"
            return (enchantMap != null && enchantMap.containsKey("Soulbound"));

        } catch (ClassNotFoundException e) {
            // AEAPI isn't on the classpath at runtime
            return false;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // AEAPI’s method signatures have changed, or invocation failed
            return false;
        }
    }
}