package com.ranull.graves.integration;

import com.jojodmo.itembridge.ItemBridgeListener;
import com.jojodmo.itembridge.ItemBridgeListenerPriority;
import com.ranull.graves.Graves;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Integration with the ItemBridge plugin for handling custom items related to graves.
 */
public final class ItemBridge implements ItemBridgeListener {
    private final Graves plugin;
    private com.jojodmo.itembridge.ItemBridge itemBridge;

    /**
     * Constructs an ItemBridge instance and registers it with the ItemBridge plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public ItemBridge(Graves plugin) {
        this.plugin = plugin;

        unregister();
        register();
    }

    /**
     * Unregisters the current ItemBridge listener if it exists.
     */
    private void unregister() {
        if (itemBridge != null) {
            itemBridge.removeListener(this);
        }
    }

    /**
     * Registers the ItemBridge listener with the ItemBridge plugin.
     */
    private void register() {
        itemBridge = new com.jojodmo.itembridge.ItemBridge(plugin, "graves", "grave");
        itemBridge.registerListener(this);
    }

    /**
     * Gets the priority of the ItemBridge listener.
     *
     * @return The priority of the listener.
     */
    @Override
    public ItemBridgeListenerPriority getPriority() {
        return ItemBridgeListenerPriority.MEDIUM;
    }

    /**
     * Fetches an ItemStack based on a string identifier.
     *
     * @param string The string identifier for the item.
     * @return The ItemStack corresponding to the identifier, or null if not found.
     */
    @Override
    public ItemStack fetchItemStack(@NotNull String string) {
        string = string.toLowerCase();

        if (plugin.getVersionManager().hasPersistentData() && string.startsWith("token_")) {
            string = string.replaceFirst("token_", "");

            return plugin.getConfig().isSet("settings.token." + string)
                    ? plugin.getRecipeManager().getToken(string) : null;
        }

        return null;
    }

    /**
     * Gets the name of the item based on its ItemStack.
     *
     * @param itemStack The ItemStack for which to retrieve the name.
     * @return The name of the item, or null if not recognized.
     */
    @Override
    public String getItemName(@NotNull ItemStack itemStack) {
        if (plugin.getVersionManager().hasPersistentData() && plugin.getRecipeManager().isToken(itemStack)) {
            return plugin.getRecipeManager().getTokenName(itemStack);
        }

        return null;
    }

    /**
     * Checks if the given ItemStack matches the specified string identifier.
     *
     * @param itemStack The ItemStack to check.
     * @param string    The string identifier to compare with.
     * @return True if the ItemStack matches the identifier, false otherwise.
     */
    @Override
    public boolean isItem(@NotNull ItemStack itemStack, @NotNull String string) {
        return string.equals(getItemName(itemStack));
    }
}