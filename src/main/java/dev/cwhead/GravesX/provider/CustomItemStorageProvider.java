package dev.cwhead.GravesX.provider;

import com.ranull.graves.type.Grave;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides custom item serialization and restoration support for
 * third-party item systems such as CraftEngine, Oraxen,
 * ItemsAdder, MMOItems, MythicCrucible, etc.
 */
public interface CustomItemStorageProvider {

    /**
     * Unique identifier used as the serialization prefix.
     *
     * Example:
     * <pre>
     * CRAFTENGINE
     * ORAXEN
     * ITEMSADDER
     * MMOITEMS
     * </pre>
     *
     * Serialized format:
     *
     * <pre>
     * PROVIDER_ID:encoded-item-id:base64-item
     * </pre>
     *
     * @return provider identifier
     */
    @NotNull
    String getProviderId();

    /**
     * Returns the custom item identifier for an item.
     *
     * Returning {@code null} indicates the item is not handled
     * by this provider.
     *
     * @param itemStack item to inspect
     * @return custom item identifier or null
     */
    @Nullable
    String getCustomItemId(@Nullable ItemStack itemStack);

    /**
     * Rebuilds an item from a previously stored custom item id.
     *
     * @param itemId custom item identifier
     * @param amount desired amount
     * @return rebuilt item or null if restoration failed
     */
    @Nullable
    ItemStack rebuildCustomItem(@NotNull String itemId, int amount);
}