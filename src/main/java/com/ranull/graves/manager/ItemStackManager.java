package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the creation and manipulation of ItemStacks related to graves.
 */
public final class ItemStackManager extends EntityDataManager {
    /**
     * The main plugin instance associated with Graves.
     * <p>
     * This {@link Graves} instance represents the core plugin that this Graves is part of. It provides access
     * to the plugin's functionality, configuration, and other services.
     * </p>
     */
    private final Graves plugin;

    private Entity e = null;

    /**
     * Initializes a new instance of the ItemStackManager class.
     *
     * @param plugin The plugin instance.
     */
    public ItemStackManager(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
    }

    /**
     * Creates an ItemStack representing the obituary of a grave.
     *
     * @param grave The grave to create an obituary for.
     * @return The created ItemStack.
     */
    public ItemStack getGraveObituary(Grave grave) {
        ItemStack itemStack = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
        Enchantment durability = plugin.getVersionManager().getEnchantmentForVersion("DURABILITY");

        if (bookMeta != null) {
            List<String> lineList = new ArrayList<>();
            List<String> loreList = new ArrayList<>();

            for (String lore : plugin.getConfig("obituary.line", grave).getStringList("obituary.line")) {
                lineList.add(StringUtil.parseString(lore, grave.getLocationDeath(), grave, plugin));
            }

            for (String string : plugin.getConfig("obituary.lore", grave).getStringList("obituary.lore")) {
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, grave.getLocationDeath(), grave, plugin));
            }

            int customModelData = plugin.getConfig("obituary.model-data", grave).getInt("obituary.model-data", -1);

            if (customModelData > -1) {
                bookMeta.setCustomModelData(customModelData);
            }

            if (plugin.getConfig("obituary.glow", grave).getBoolean("obituary.glow")) {
                bookMeta.addEnchant(durability, 1, true);

                if (!plugin.getVersionManager().is_v1_7()) {
                    bookMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }

            if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()
                    && !plugin.getVersionManager().is_v1_9()) {
                bookMeta.setGeneration(null);
            }

            bookMeta.setPages(String.join("\n", lineList));
            bookMeta.setLore(loreList);
            bookMeta.setTitle(ChatColor.WHITE + StringUtil.parseString(plugin.getConfig("obituary.title", grave)
                    .getString("obituary.title"), grave, plugin));
            bookMeta.setAuthor(StringUtil.parseString(plugin.getConfig("obituary.author", grave)
                    .getString("obituary.author"), grave, plugin));
            itemStack.setItemMeta(bookMeta);
        }

        return itemStack;
    }

    /**
     * Creates an ItemStack representing the head of a grave owner.
     *
     * @param grave The grave to create a head for.
     * @return The created ItemStack.
     */
    public ItemStack getGraveHead(Grave grave) {
        ItemStack itemStack = plugin.getCompatibility().getSkullItemStack(grave, plugin);
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            List<String> loreList = new ArrayList<>();

            for (String string : plugin.getConfig("head.lore", grave).getStringList("head.lore")) {
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, grave.getLocationDeath(), grave, plugin));
            }

            int customModelData = plugin.getConfig("head.model-data", grave).getInt("head.model-data", -1);

            if (customModelData > -1) {
                itemMeta.setCustomModelData(customModelData);
            }

            itemMeta.setLore(loreList);
            itemMeta.setDisplayName(ChatColor.WHITE + StringUtil.parseString(plugin.getConfig("head.name", grave)
                    .getString("head.name"), grave, plugin));
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    /**
     * Creates an ItemStack representing a grave in a list.
     *
     * @param number The number to display on the item.
     * @param grave  The grave to create the ItemStack for.
     * @return The created ItemStack.
     */
    public ItemStack createGraveListItemStack(int number, Grave grave) {
        Material material;
        Enchantment durability = plugin.getVersionManager().getEnchantmentForVersion("DURABILITY");

        if (plugin.getConfig("gui.menu.list.item.block", grave).getBoolean("gui.menu.list.item.block")) {
            String materialString = plugin.getConfig("block.material", grave)
                    .getString("block.material", "CHEST");

            if (materialString.equals("PLAYER_HEAD") && !plugin.getVersionManager().hasBlockData()) {
                materialString = "SKULL_ITEM";
            }

            material = Material.matchMaterial(materialString);
        } else {
            material = Material.matchMaterial(plugin.getConfig("gui.menu.list.item.material", grave)
                    .getString("gui.menu.list.item.block", "CHEST"));
        }

        if (material == null) {
            material = Material.CHEST;
        }

        ItemStack itemStack = new ItemStack(material);

        if (itemStack.getType().name().equals("PLAYER_HEAD") || itemStack.getType().name().equals("SKULL_ITEM")) {
            itemStack = plugin.getCompatibility().getSkullItemStack(grave, plugin);
        }

        if (itemStack.getItemMeta() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            String name = ChatColor.WHITE + StringUtil.parseString(plugin.getConfig("gui.menu.list.name", grave)
                    .getString("gui.menu.list.name"), grave, plugin).replace("%number%",
                    String.valueOf(number));
            List<String> loreList = new ArrayList<>();
            int customModelData = plugin.getConfig("gui.menu.list.model-data", grave)
                    .getInt("gui.menu.list.model-data", -1);

            for (String string : plugin.getConfig("gui.menu.list.lore", grave).getStringList("gui.menu.list.lore")) {
                e = Bukkit.getEntity(grave.getOwnerUUID());
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, e, grave.getLocationDeath(), grave, plugin));
            }

            if (plugin.getConfig().getBoolean("gui.menu.list.glow")) {
                itemMeta.addEnchant(durability, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (customModelData > -1) {
                itemMeta.setCustomModelData(customModelData);
            }

            itemMeta.setDisplayName(name);
            itemMeta.setLore(loreList);
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    /**
     * Creates an ItemStack for a grave menu slot.
     *
     * @param slot  The slot number.
     * @param grave The grave to create the ItemStack for.
     * @return The created ItemStack.
     */
    public ItemStack createGraveMenuItemStack(int slot, Grave grave) {
        String materialString = plugin.getConfig("gui.menu.grave.slot." + slot + ".material", grave)
                .getString("gui.menu.grave.slot." + slot + ".material", "PAPER");
        Material material = Material.matchMaterial(materialString);
        Enchantment durability = plugin.getVersionManager().getEnchantmentForVersion("DURABILITY");

        if (material == null) {
            material = Material.PAPER;

            plugin.debugMessage(materialString.toUpperCase() + " is not a Material ENUM", 1);
        }

        ItemStack itemStack = new ItemStack(material);

        if (itemStack.getItemMeta() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            String name = ChatColor.WHITE + StringUtil.parseString(plugin.getConfig("gui.menu.grave.slot." + slot + ".name", grave)
                    .getString("gui.menu.grave.slot." + slot + ".name"), grave, plugin);
            List<String> loreList = new ArrayList<>();
            int customModelData = plugin.getConfig("gui.menu.grave.slot." + slot + ".model-data", grave)
                    .getInt("gui.menu.grave.slot." + slot + ".model-data", -1);

            for (String string : plugin.getConfig("gui.menu.grave.slot." + slot + ".lore", grave)
                    .getStringList("gui.menu.grave.slot." + slot + ".lore")) {
                e = Bukkit.getEntity(grave.getOwnerUUID());
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, e, grave.getLocationDeath(), grave, plugin));
            }

            if (plugin.getConfig().getBoolean("gui.menu.grave.slot." + slot + ".glow")) {
                itemMeta.addEnchant(durability, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (customModelData > -1) {
                itemMeta.setCustomModelData(customModelData);
            }

            itemMeta.setDisplayName(name);
            itemMeta.setLore(loreList);
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    /**
     * Creates an ItemStack for a graveyard slot.
     *
     * @param slot  The slot number.
     * @param grave The grave to create the ItemStack for.
     * @return The created ItemStack.
     */
    public ItemStack createGraveyardItemStack(int slot, Grave grave) {
        String materialString = plugin.getConfig("gui.menu.grave.slot." + slot + ".material", grave)
                .getString("gui.menu.grave.slot." + slot + ".material", "PAPER");
        Material material = Material.matchMaterial(materialString);

        if (material == null) {
            material = Material.PAPER;

            plugin.debugMessage(materialString.toUpperCase() + " is not a Material ENUM", 1);
        }

        ItemStack itemStack = new ItemStack(material);

        if (itemStack.getItemMeta() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            String name = ChatColor.WHITE + StringUtil.parseString(plugin.getConfig("gui.menu.grave.slot." + slot + ".name", grave)
                    .getString("gui.menu.grave.slot." + slot + ".name"), grave, plugin);
            List<String> loreList = new ArrayList<>();
            int customModelData = plugin.getConfig("gui.menu.grave.slot." + slot + ".model-data", grave)
                    .getInt("gui.menu.grave.slot." + slot + ".model-data", -1);

            for (String string : plugin.getConfig("gui.menu.grave.slot." + slot + ".lore", grave)
                    .getStringList("gui.menu.grave.slot." + slot + ".lore")) {
                e = Bukkit.getEntity(grave.getOwnerUUID());
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, e, grave.getLocationDeath(), grave, plugin));
            }

            if (plugin.getConfig().getBoolean("gui.menu.grave.slot." + slot + ".glow")) {
                itemMeta.addEnchant(plugin.getVersionManager().getEnchantmentForVersion("DURABILITY"), 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (customModelData > -1) {
                itemMeta.setCustomModelData(customModelData);
            }

            itemMeta.setDisplayName(name);
            itemMeta.setLore(loreList);
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }
}