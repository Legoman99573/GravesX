package com.ranull.graves.util;

import com.ranull.graves.Graves;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Utility class for inventory-related operations.
 */
public final class InventoryUtil {

    /**
     * Gets the appropriate inventory size based on the given size.
     *
     * @param size The size to be used for determining the inventory size.
     * @return The appropriate inventory size.
     */
    public static int getInventorySize(int size) {
        if (size <= 9) {
            return 9;
        } else if (size <= 18) {
            return 18;
        } else if (size <= 27) {
            return 27;
        } else if (size <= 36) {
            return 36;
        } else if (size <= 45) {
            return 45;
        } else {
            return 54;
        }
    }

    /**
     * Equips the player's armor from the given inventory.
     *
     * @param inventory The inventory containing the armor items.
     * @param player    The player to be equipped with armor.
     */
    public static void equipArmor(Inventory inventory, Player player) {
        List<ItemStack> itemList = Arrays.asList(inventory.getContents());
        Collections.reverse(itemList);

        for (ItemStack itemStack : itemList) {
            if (itemStack != null) {
                if (player.getInventory().getHelmet() == null && isHelmet(itemStack)) {
                    player.getInventory().setHelmet(itemStack);
                    playArmorEquipSound(player, itemStack);
                    inventory.removeItem(itemStack);
                }

                if (player.getInventory().getChestplate() == null && isChestplate(itemStack)) {
                    player.getInventory().setChestplate(itemStack);
                    playArmorEquipSound(player, itemStack);
                    inventory.removeItem(itemStack);
                }

                if (player.getInventory().getLeggings() == null && isLeggings(itemStack)) {
                    player.getInventory().setLeggings(itemStack);
                    playArmorEquipSound(player, itemStack);
                    inventory.removeItem(itemStack);
                }

                if (player.getInventory().getBoots() == null && isBoots(itemStack)) {
                    player.getInventory().setBoots(itemStack);
                    playArmorEquipSound(player, itemStack);
                    inventory.removeItem(itemStack);
                }
            }
        }
    }

    /**
     * Equips the player's inventory items from the given inventory.
     *
     * @param inventory The inventory containing the items.
     * @param player    The player to be equipped with items.
     */
    public static void equipItems(Inventory inventory, Player player) {
        List<ItemStack> itemStackList = new ArrayList<>();

        for (ItemStack itemStack : inventory.getContents().clone()) {
            if (itemStack != null && !MaterialUtil.isAir(itemStack.getType())) {
                itemStackList.add(itemStack);
            }
        }

        inventory.clear();

        for (ItemStack itemStack : itemStackList) {
            for (Map.Entry<Integer, ItemStack> itemStackEntry : player.getInventory().addItem(itemStack).entrySet()) {
                inventory.addItem(itemStackEntry.getValue())
                        .forEach((key, value) -> player.getWorld().dropItem(player.getLocation(), value));
            }
        }
    }

    /**
     * Plays the appropriate armor equip sound based on the item type.
     *
     * @param player    The player equipping the armor.
     * @param itemStack The armor item being equipped.
     */
    public static void playArmorEquipSound(Player player, ItemStack itemStack) {
        try {
            if (itemStack.getType().name().startsWith("NETHERITE")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1, 1);
            } else if (itemStack.getType().name().startsWith("DIAMOND")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1, 1);
            } else if (itemStack.getType().name().startsWith("GOLD")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1, 1);
            } else if (itemStack.getType().name().startsWith("IRON")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1, 1);
            } else if (itemStack.getType().name().startsWith("LEATHER")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            } else if (itemStack.getType().name().startsWith("ELYTRA")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1, 1);
            } else if (itemStack.getType().name().startsWith("TURTLE")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_TURTLE, 1, 1);
            } else {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1, 1);
            }
        } catch (NoSuchFieldError ignored) {
        }
    }

    /**
     * Checks if the given item stack is armor.
     *
     * @param itemStack The item stack to be checked.
     * @return True if the item stack is armor, false otherwise.
     */
    public static boolean isArmor(ItemStack itemStack) {
        return isHelmet(itemStack) || isChestplate(itemStack) || isLeggings(itemStack) || isBoots(itemStack);
    }

    /**
     * Checks if the given item stack is a helmet.
     *
     * @param itemStack The item stack to be checked.
     * @return True if the item stack is a helmet, false otherwise.
     */
    public static boolean isHelmet(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name()
                .matches("(?i)NETHERITE_HELMET|DIAMOND_HELMET|GOLDEN_HELMET|GOLD_HELMET|IRON_HELMET|LEATHER_HELMET|" +
                        "CHAINMAIL_HELMET|TURTLE_HELMET|CARVED_PUMPKIN|PUMPKIN");
    }

    /**
     * Checks if the given item stack is a chestplate.
     *
     * @param itemStack The item stack to be checked.
     * @return True if the item stack is a chestplate, false otherwise.
     */
    public static boolean isChestplate(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name()
                .matches("(?i)NETHERITE_CHESTPLATE|DIAMOND_CHESTPLATE|GOLDEN_CHESTPLATE|GOLD_CHESTPLATE|" +
                        "IRON_CHESTPLATE|LEATHER_CHESTPLATE|CHAINMAIL_CHESTPLATE|ELYTRA");
    }

    /**
     * Checks if the given item stack is leggings.
     *
     * @param itemStack The item stack to be checked.
     * @return True if the item stack is leggings, false otherwise.
     */
    public static boolean isLeggings(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name()
                .matches("(?i)NETHERITE_LEGGINGS|DIAMOND_LEGGINGS|GOLDEN_LEGGINGS|GOLD_LEGGINGS|IRON_LEGGINGS|" +
                        "LEATHER_LEGGINGS|CHAINMAIL_LEGGINGS");
    }

    /**
     * Checks if the given item stack is boots.
     *
     * @param itemStack The item stack to be checked.
     * @return True if the item stack is boots, false otherwise.
     */
    public static boolean isBoots(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name()
                .matches("(?i)NETHERITE_BOOTS|DIAMOND_BOOTS|GOLDEN_BOOTS|GOLD_BOOTS|IRON_BOOTS|LEATHER_BOOTS|" +
                        "CHAINMAIL_BOOTS");
    }

    /**
     * Converts the given inventory to a string representation.
     *
     * @param inventory The inventory to be converted.
     * @return The string representation of the inventory.
     */
    public static String inventoryToString(Inventory inventory) {
        List<String> stringList = new ArrayList<>();

        for (ItemStack itemStack : inventory.getContents()) {
            try {
                String base64 = Base64Util.objectToBase64(itemStack != null ? itemStack : new ItemStack(Material.AIR));

                stringList.add(base64);
            } catch (Exception e) {
                stringList.add(Base64Util.objectToBase64(new ItemStack(Material.AIR)));
                Bukkit.getLogger().warning("Exception during Base64 conversion for : " + itemStack + " - " + e.getMessage());
                Bukkit.getLogger().severe("NBT Data: " + itemStack);
                Bukkit.getLogger().warning("Removed problematic item " + itemStack + " from grave. While the grave will still generate. This is likely a Spigot/Paper bug.");
                Bukkit.getLogger().warning("Stack Trace:");
                e.printStackTrace();
            }
        }

        return String.join("|", stringList);
    }


    /**
     * Converts a string representation of an inventory to an Inventory object.
     *
     * @param inventoryHolder The inventory holder.
     * @param string          The string representation of the inventory.
     * @param title           The title of the inventory.
     * @param plugin          The Graves plugin instance.
     * @return The Inventory object.
     */
    public static Inventory stringToInventory(InventoryHolder inventoryHolder, String string, String title, Graves plugin) {
        String[] strings = string.split("\\|");

        if (strings.length > 0 && !strings[0].equals("")) {
            Inventory inventory = plugin.getServer().createInventory(inventoryHolder,
                    InventoryUtil.getInventorySize(strings.length), title);

            int counter = 0;
            for (String itemString : strings) {
                try {
                    Object object = Base64Util.base64ToObject(itemString);

                    if (object instanceof ItemStack) {
                        inventory.setItem(counter, (ItemStack) object);
                        counter++;
                    } else {
                        inventory.setItem(counter, (ItemStack) Base64Util.base64ToObject(String.valueOf(Material.AIR)));
                        counter++;
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Exception during Base64 conversion for item at slot " + counter + ": " + itemString + " - " + e.getMessage());
                    Bukkit.getLogger().severe("NBT Data: " + itemString);
                    Bukkit.getLogger().warning("Removed problematic item " + itemString + " from slot " + counter + ". While the grave will still generate. This is likely a Spigot/Paper bug.");
                    Bukkit.getLogger().warning("Stack Trace:");
                    e.printStackTrace();
                    inventory.setItem(counter, (ItemStack) Base64Util.base64ToObject(String.valueOf(Material.AIR)));
                    counter++;
                }
            }

            return inventory;
        }

        return plugin.getServer().createInventory(inventoryHolder, strings.length, title);
    }
}