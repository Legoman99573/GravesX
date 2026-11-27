package dev.cwhead.GravesX.util;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.Collections;

/**
 * Utility methods for safely applying custom model data across multiple
 * Minecraft / Bukkit API versions.
 */
public class CustomModelDataUtil {

    public CustomModelDataUtil() {}

    /**
     * Applies custom model data using the 1.20.5+ component API if available,
     * falling back to legacy {@link ItemMeta#setCustomModelData(Integer)}.
     *
     * @param meta            the item meta (book meta or item meta)
     * @param customModelData the model data id; ignored if &lt; 0
     */
    public static void applyCustomModelData(ItemMeta meta, int customModelData) {
        if (customModelData <= -1 || meta == null) return;

        try {
            CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
            cmdComponent.setFloats(Collections.singletonList((float) customModelData));
            meta.setCustomModelDataComponent(cmdComponent);
        } catch (Throwable ignored) {
            try {
                meta.setCustomModelData(customModelData);
            } catch (Throwable ignoreAgain) {
            }
        }
    }
}
