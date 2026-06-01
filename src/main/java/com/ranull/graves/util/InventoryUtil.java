package com.ranull.graves.util;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.provider.CustomItemStorageProvider;
import com.ranull.graves.type.Grave;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class InventoryUtil {
    private InventoryUtil() {}

    public static int getInventorySize(int size) {
        if (size <= 9) return 9;
        if (size <= 18) return 18;
        if (size <= 27) return 27;
        if (size <= 36) return 36;
        if (size <= 45) return 45;
        return 54;
    }

    public static void equipArmor(Inventory inventory, Player player) {
        List<ItemStack> itemList = Arrays.asList(inventory.getContents());
        Collections.reverse(itemList);

        for (ItemStack itemStack : itemList) {
            if (itemStack == null) continue;

            if (player.getInventory().getHelmet() == null && isHelmet(itemStack)) {
                player.getInventory().setHelmet(itemStack);
                playArmorEquipSound(player, itemStack);
                inventory.removeItem(itemStack);
                continue;
            }

            if (player.getInventory().getChestplate() == null && isChestplate(itemStack)) {
                player.getInventory().setChestplate(itemStack);
                playArmorEquipSound(player, itemStack);
                inventory.removeItem(itemStack);
                continue;
            }

            if (player.getInventory().getLeggings() == null && isLeggings(itemStack)) {
                player.getInventory().setLeggings(itemStack);
                playArmorEquipSound(player, itemStack);
                inventory.removeItem(itemStack);
                continue;
            }

            if (player.getInventory().getBoots() == null && isBoots(itemStack)) {
                player.getInventory().setBoots(itemStack);
                playArmorEquipSound(player, itemStack);
                inventory.removeItem(itemStack);
            }
        }
    }

    public static void equipItems(Inventory inventory, Player player) {
        List<ItemStack> itemStackList = new ArrayList<>();

        for (ItemStack itemStack : inventory.getContents().clone()) {
            if (itemStack != null && !MaterialUtil.isAir(itemStack.getType())) {
                itemStackList.add(itemStack);
            }
        }

        inventory.clear();

        for (ItemStack itemStack : itemStackList) {
            player.getInventory().addItem(itemStack)
                    .forEach((key, value) -> inventory.addItem(value)
                            .forEach((dropKey, dropValue) ->
                                    player.getWorld().dropItem(player.getLocation(), dropValue)));
        }
    }

    public static void playArmorEquipSound(Player player, ItemStack itemStack) {
        try {
            String type = itemStack.getType().name();

            if (type.startsWith("NETHERITE")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1, 1);
            } else if (type.startsWith("DIAMOND")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1, 1);
            } else if (type.startsWith("GOLD")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1, 1);
            } else if (type.startsWith("COPPER")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_COPPER, 1, 1);
            } else if (type.startsWith("CHAIN")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1, 1);
            } else if (type.startsWith("IRON")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1, 1);
            } else if (type.startsWith("LEATHER")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            } else if (type.startsWith("ELYTRA")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1, 1);
            } else if (type.startsWith("TURTLE")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_TURTLE, 1, 1);
            } else {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1, 1);
            }
        } catch (NoSuchFieldError ignored) {
        }
    }

    public static boolean isArmor(ItemStack itemStack) {
        return isHelmet(itemStack) || isChestplate(itemStack) || isLeggings(itemStack) || isBoots(itemStack);
    }

    public static boolean isHelmet(ItemStack itemStack) {
        if (itemStack == null) return false;

        String type = itemStack.getType().name().toUpperCase(Locale.ROOT);

        return type.contains("HELMET")
                || type.equals("CARVED_PUMPKIN")
                || type.equals("PUMPKIN")
                || type.equals("PLAYER_HEAD")
                || type.equals("SKELETON_SKULL")
                || type.equals("WITHER_SKELETON_SKULL")
                || type.equals("ZOMBIE_HEAD")
                || type.equals("CREEPER_HEAD")
                || type.equals("DRAGON_HEAD")
                || type.equals("PIGLIN_HEAD");
    }

    public static boolean isChestplate(ItemStack itemStack) {
        if (itemStack == null) return false;

        String type = itemStack.getType().name().toUpperCase(Locale.ROOT);

        return type.contains("CHESTPLATE") || type.equals("ELYTRA");
    }

    public static boolean isLeggings(ItemStack itemStack) {
        if (itemStack == null) return false;

        return itemStack.getType().name().toUpperCase(Locale.ROOT).contains("LEGGINGS");
    }

    public static boolean isBoots(ItemStack itemStack) {
        if (itemStack == null) return false;

        return itemStack.getType().name().toUpperCase(Locale.ROOT).contains("BOOTS");
    }

    public static String inventoryToString(Inventory inventory) {
        return inventoryToString(inventory, null);
    }

    public static String inventoryToString(Inventory inventory, Graves plugin) {
        List<String> stringList = new ArrayList<>();

        if (inventory == null) {
            return "";
        }

        for (ItemStack itemStack : inventory.getContents()) {
            stringList.add(itemStackToString(itemStack, plugin, inventory.getHolder()));
        }

        return String.join("|", stringList);
    }

    public static String equipmentMapToString(Map<EquipmentSlot, ItemStack> equipmentMap, Graves plugin, Grave grave) {
        Map<String, String> serializedEquipmentMap = new HashMap<>();

        if (equipmentMap != null) {
            for (Map.Entry<EquipmentSlot, ItemStack> entry : equipmentMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue().getType() != Material.AIR) {
                    serializedEquipmentMap.put(entry.getKey().name(), itemStackToString(entry.getValue(), plugin, grave));
                }
            }
        }

        return Base64Util.objectToBase64(serializedEquipmentMap);
    }

    public static Map<EquipmentSlot, ItemStack> stringToEquipmentMap(String string, Graves plugin, Grave grave) {
        Map<EquipmentSlot, ItemStack> equipmentMap = new EnumMap<>(EquipmentSlot.class);

        if (string == null || string.isBlank()) {
            return equipmentMap;
        }

        try {
            Object object = Base64Util.base64ToObject(string);

            if (!(object instanceof Map<?, ?> rawMap)) {
                return equipmentMap;
            }

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                EquipmentSlot equipmentSlot = parseEquipmentSlot(entry.getKey());

                if (equipmentSlot == null || entry.getValue() == null) {
                    continue;
                }

                ItemStack itemStack = null;

                if (entry.getValue() instanceof ItemStack legacyItemStack) {
                    itemStack = legacyItemStack;
                } else if (entry.getValue() instanceof String itemString) {
                    itemStack = stringToItemStack(itemString, plugin, grave, -1, true);
                }

                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    equipmentMap.put(equipmentSlot, itemStack);
                }
            }
        } catch (Exception exception) {
            Bukkit.getLogger().warning("Exception during equipment Base64 conversion: " + exception.getMessage());
            Bukkit.getLogger().warning("Stack Trace:");
            exception.printStackTrace();
        }

        return equipmentMap;
    }

    public static Inventory stringToInventory(InventoryHolder inventoryHolder, String string, String title, Graves plugin) {
        String[] strings = string != null ? string.split("\\|") : new String[0];

        if (strings.length > 0 && !strings[0].equals("")) {
            Inventory inventory = plugin.getServer().createInventory(inventoryHolder,
                    InventoryUtil.getInventorySize(strings.length), title);

            int counter = 0;

            for (String itemString : strings) {
                inventory.setItem(counter, stringToItemStack(itemString, plugin, inventoryHolder, counter, false));
                counter++;
            }

            return inventory;
        }

        return plugin.getServer().createInventory(inventoryHolder, strings.length, title);
    }

    private static String itemStackToString(ItemStack itemStack, Graves plugin, InventoryHolder inventoryHolder) {
        ItemStack serializableItemStack = normalizeItemStackForStorage(itemStack);

        try {
            String base64 = Base64Util.objectToBase64(serializableItemStack);
            CustomSerializedItem customSerializedItem = getCustomSerializedItem(itemStack, plugin, inventoryHolder);

            if (customSerializedItem != null) {
                return customSerializedItem.providerId()
                        + ":"
                        + encodeString(customSerializedItem.itemId())
                        + ":"
                        + base64;
            }

            return base64;
        } catch (Exception exception) {
            Bukkit.getLogger().warning("Exception during Base64 conversion for: " + itemStack + " - " + exception.getMessage());
            Bukkit.getLogger().severe("NBT Data: " + itemStack);
            Bukkit.getLogger().warning("Removed problematic item " + itemStack + " from grave. While the grave will still generate, this is likely a Spigot/Paper bug.");
            Bukkit.getLogger().warning("Stack Trace:");
            exception.printStackTrace();
            return Base64Util.objectToBase64(new ItemStack(Material.AIR));
        }
    }

    private static ItemStack normalizeItemStackForStorage(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }

        Plugin nbtAPI = Bukkit.getPluginManager().getPlugin("NBTAPI");

        if (nbtAPI != null && nbtAPI.isEnabled()) {
            NBTItem nbtItem = new NBTItem(itemStack);
            return nbtItem.getItem();
        }

        return itemStack;
    }

    private static ItemStack stringToItemStack(String itemString, Graves plugin, InventoryHolder inventoryHolder,
                                               int slot, boolean equipment) {
        SerializedItem serializedItem = parseSerializedItem(itemString, plugin);

        try {
            Object object = Base64Util.base64ToObject(serializedItem.itemString());
            ItemStack itemStack = object instanceof ItemStack ? (ItemStack) object : new ItemStack(Material.AIR);

            if (itemStack != null && itemStack.getType() != Material.AIR) {
                Plugin nbtAPI = Bukkit.getPluginManager().getPlugin("NBTAPI");

                if (nbtAPI != null && nbtAPI.isEnabled()) {
                    NBTItem nbtItem = new NBTItem(itemStack);
                    itemStack = nbtItem.getItem();
                }
            }

            return restoreCustomItem(serializedItem.providerId(), serializedItem.customItemId(), itemStack, plugin, inventoryHolder);
        } catch (Exception exception) {
            if (exception.getMessage() == null || !exception.getMessage().contains("ItemStack can't be null/air/amount of 0!")) {
                String location = equipment ? "equipment item" : "item at slot " + slot;
                Bukkit.getLogger().warning("Exception during Base64 conversion for " + location + ": "
                        + itemString + " - " + exception.getMessage());
                Bukkit.getLogger().severe("NBT Data: " + itemString);
                Bukkit.getLogger().warning("Removed problematic " + location + ". While the grave will still generate, this is likely a Spigot/Paper bug.");
                Bukkit.getLogger().warning("Stack Trace:");
                exception.printStackTrace();
            }

            return new ItemStack(Material.AIR);
        }
    }

    private static CustomSerializedItem getCustomSerializedItem(ItemStack itemStack, Graves plugin, InventoryHolder inventoryHolder) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        for (CustomItemStorageProvider provider : getCustomItemStorageProviders(plugin)) {
            String itemId = provider.getCustomItemId(itemStack);

            if (itemId != null && !itemId.isBlank()) {
                return new CustomSerializedItem(provider.getProviderId(), itemId);
            }
        }

        return null;
    }

    private static ItemStack restoreCustomItem(String providerId, String customItemId, ItemStack fallbackItemStack,
                                               Graves plugin, InventoryHolder inventoryHolder) {
        if (providerId == null || customItemId == null) {
            return fallbackItemStack;
        }

        CustomItemStorageProvider provider = getCustomItemStorageProvider(plugin, providerId);

        if (provider == null) {
            return fallbackItemStack;
        }

        ItemStack fallback = fallbackItemStack != null ? fallbackItemStack : new ItemStack(Material.AIR);
        String currentId = provider.getCustomItemId(fallback);

        if (customItemId.equalsIgnoreCase(currentId)) {
            return fallback;
        }

        ItemStack rebuilt = provider.rebuildCustomItem(customItemId, Math.max(1, fallback.getAmount()));
        return rebuilt != null ? rebuilt : fallback;
    }

    private static List<CustomItemStorageProvider> getCustomItemStorageProviders(Graves plugin) {
        if (plugin == null || plugin.getIntegrationManager() == null) {
            return Collections.emptyList();
        }

        List<CustomItemStorageProvider> providers = plugin.getIntegrationManager().getCustomItemStorageProviders();
        return providers != null ? providers : Collections.emptyList();
    }

    private static CustomItemStorageProvider getCustomItemStorageProvider(Graves plugin, String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return null;
        }

        for (CustomItemStorageProvider provider : getCustomItemStorageProviders(plugin)) {
            if (provider.getProviderId().equalsIgnoreCase(providerId)) {
                return provider;
            }
        }

        return null;
    }

    private static SerializedItem parseSerializedItem(String itemString, Graves plugin) {
        if (itemString == null) {
            return new SerializedItem(null, null, null);
        }

        for (CustomItemStorageProvider provider : getCustomItemStorageProviders(plugin)) {
            String providerId = provider.getProviderId();

            if (providerId == null || providerId.isBlank()) {
                continue;
            }

            String prefix = providerId + ":";

            if (!itemString.startsWith(prefix)) {
                continue;
            }

            int separatorIndex = itemString.indexOf(':', prefix.length());

            if (separatorIndex <= prefix.length()) {
                return new SerializedItem(null, null, itemString);
            }

            String encodedItemId = itemString.substring(prefix.length(), separatorIndex);
            String base64Item = itemString.substring(separatorIndex + 1);

            try {
                return new SerializedItem(providerId, decodeString(encodedItemId), base64Item);
            } catch (IllegalArgumentException ignored) {
                return new SerializedItem(null, null, itemString);
            }
        }

        return new SerializedItem(null, null, itemString);
    }

    private static String encodeString(String string) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeString(String string) {
        return new String(Base64.getUrlDecoder().decode(string), StandardCharsets.UTF_8);
    }

    private static EquipmentSlot parseEquipmentSlot(Object object) {
        if (object instanceof EquipmentSlot equipmentSlot) {
            return equipmentSlot;
        }

        if (object instanceof String string) {
            try {
                return EquipmentSlot.valueOf(string);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }

    private record SerializedItem(String providerId, String customItemId, String itemString) {
    }

    private record CustomSerializedItem(String providerId, String itemId) {
    }
}