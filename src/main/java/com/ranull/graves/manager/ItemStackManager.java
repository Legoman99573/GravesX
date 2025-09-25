package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.StringUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Initializes a new instance of the ItemStackManager class.
     *
     * @param plugin The plugin instance.
     */
    public ItemStackManager(final Graves plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    /**
     * Creates an ItemStack representing the obituary of a grave.
     *
     * @param grave The grave to create an obituary for.
     * @return The created ItemStack.
     */
    public ItemStack getGraveObituary(final Grave grave) {
        final ItemStack itemStack = new ItemStack(Material.WRITTEN_BOOK, 1);
        final BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
        final Enchantment durability = plugin.getVersionManager().getEnchantmentForVersion("DURABILITY");

        if (bookMeta == null) {
            return itemStack;
        }

        if (plugin.getIntegrationManager().hasMiniMessage()) {
            final List<String> lineList = new ArrayList<>();
            final List<String> loreList = new ArrayList<>();

            for (final String lore : plugin.getConfig("obituary.line", grave).getStringList("obituary.line")) {
                lineList.add(MiniMessage.convertLegacyToMiniMessage(
                        StringUtil.parseString(lore, grave.getLocationDeath(), grave, plugin)));
            }

            for (final String string : plugin.getConfig("obituary.lore", grave).getStringList("obituary.lore")) {
                loreList.add(MiniMessage.convertLegacyToMiniMessage(
                        StringUtil.parseString(string, grave.getLocationDeath(), grave, plugin)));
            }

            final List<List<String>> pages = splitIntoPages(lineList, 13);

            final int customModelData = plugin.getConfig("obituary.model-data", grave)
                    .getInt("obituary.model-data", -1);
            applyCustomModelData(bookMeta, customModelData);

            if (plugin.getConfig("obituary.glow", grave).getBoolean("obituary.glow")) {
                bookMeta.addEnchant(durability, 1, true);
                if (!plugin.getVersionManager().is_v1_7()) {
                    bookMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }

            if (!plugin.getVersionManager().is_v1_7()
                    && !plugin.getVersionManager().is_v1_8()
                    && !plugin.getVersionManager().is_v1_9()) {
                bookMeta.setGeneration(null);
            }

            final String title = plugin.getConfig("obituary.title", grave).getString("obituary.title");
            final String author = plugin.getConfig("obituary.author", grave).getString("obituary.author");

            final String titleOriginal = StringUtil.parseString(title, grave, plugin);
            final String authorOriginal = StringUtil.parseString(author, grave, plugin);

            final Component titleConverted = MiniMessage.convertLegacyToComponent(titleOriginal);
            final Component authorConverted = MiniMessage.convertLegacyToComponent(authorOriginal);

            final List<Component> componentPages = pages.stream()
                    .map(page -> MiniMessage.convertLegacyToComponent(String.join("\n", page)))
                    .collect(Collectors.toList());

            final List<Component> componentLore = loreList.stream()
                    .map(MiniMessage::convertLegacyToComponent)
                    .collect(Collectors.toList());

            return MiniMessage.formatBookMeta(
                    plugin,
                    grave,
                    itemStack,
                    titleConverted,
                    authorConverted,
                    componentPages,
                    componentLore
            );
        } else {
            final List<String> lineList = new ArrayList<>();
            final List<String> loreList = new ArrayList<>();

            for (final String lore : plugin.getConfig("obituary.line", grave).getStringList("obituary.line")) {
                lineList.add(StringUtil.parseString(lore, grave.getLocationDeath(), grave, plugin));
            }

            for (final String string : plugin.getConfig("obituary.lore", grave).getStringList("obituary.lore")) {
                loreList.add(StringUtil.parseString(string, grave.getLocationDeath(), grave, plugin));
            }

            final List<List<String>> pages = splitIntoPages(lineList, 13);

            final int customModelData = plugin.getConfig("obituary.model-data", grave)
                    .getInt("obituary.model-data", -1);
            applyCustomModelData(bookMeta, customModelData);

            if (plugin.getConfig("obituary.glow", grave).getBoolean("obituary.glow")) {
                bookMeta.addEnchant(durability, 1, true);
                if (!plugin.getVersionManager().is_v1_7()) {
                    bookMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }

            if (!plugin.getVersionManager().is_v1_7()
                    && !plugin.getVersionManager().is_v1_8()
                    && !plugin.getVersionManager().is_v1_9()) {
                bookMeta.setGeneration(null);
            }

            final List<String> stringPages = pages.stream()
                    .map(page -> String.join("\n", page))
                    .collect(Collectors.toList());

            bookMeta.setPages(stringPages);
            bookMeta.setLore(loreList);
            bookMeta.setTitle(ChatColor.WHITE + StringUtil.parseString(
                    plugin.getConfig("obituary.title", grave).getString("obituary.title"), grave, plugin));
            bookMeta.setAuthor(StringUtil.parseString(
                    plugin.getConfig("obituary.author", grave).getString("obituary.author"), grave, plugin));

            itemStack.setItemMeta(bookMeta);
            return itemStack;
        }
    }

    /**
     * Splits a list of strings into sublists, each containing up to maxLinesPerPage lines.
     *
     * @param lines           The list of strings to split.
     * @param maxLinesPerPage The maximum number of lines per page.
     * @return A list of pages, where each page is a list of strings.
     */
    private List<List<String>> splitIntoPages(final List<String> lines, final int maxLinesPerPage) {
        final List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += maxLinesPerPage) {
            pages.add(new ArrayList<>(lines.subList(i, Math.min(i + maxLinesPerPage, lines.size()))));
        }
        return pages;
    }

    /**
     * Creates an ItemStack representing the head of a grave owner.
     *
     * @param grave The grave to create a head for.
     * @return The created ItemStack.
     */
    public ItemStack getGraveHead(final Grave grave) {
        ItemStack itemStack = plugin.getCompatibility().getSkullItemStack(grave, plugin);
        final ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null) return itemStack;

        final List<String> loreList = new ArrayList<>();

        for (final String string : plugin.getConfig("head.lore", grave).getStringList("head.lore")) {
            if (plugin.getIntegrationManager().hasMiniMessage()) {
                final String loreNew = StringUtil.parseString("&7" + string, grave.getLocationDeath(), grave, plugin);
                loreList.add(MiniMessage.parseString(loreNew));
            } else {
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, grave.getLocationDeath(), grave, plugin));
            }
        }

        final int customModelData = plugin.getConfig("head.model-data", grave)
                .getInt("head.model-data", -1);
        applyCustomModelData(itemMeta, customModelData);

        itemMeta.setLore(loreList);

        final String displayName;
        if (plugin.getIntegrationManager().hasMiniMessage()) {
            final String displayNameNew = StringUtil.parseString("&f" + plugin.getConfig("head.name", grave)
                    .getString("head.name"), grave, plugin);
            displayName = MiniMessage.parseString(displayNameNew);
        } else {
            displayName = ChatColor.WHITE + StringUtil.parseString(
                    plugin.getConfig("head.name", grave).getString("head.name"), grave, plugin);
        }

        itemMeta.setDisplayName(displayName);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    /**
     * Creates an ItemStack representing a grave in a list.
     *
     * @param number The number to display on the item.
     * @param grave  The grave to create the ItemStack for.
     * @return The created ItemStack.
     */
    public ItemStack createGraveListItemStack(final int number, final Grave grave) {
        final Enchantment durability = plugin.getVersionManager().getEnchantmentForVersion("DURABILITY");

        final Material material;
        if (plugin.getConfig("gui.menu.list.item.block", grave).getBoolean("gui.menu.list.item.block")) {
            String materialString = plugin.getConfig("block.material", grave)
                    .getString("block.material", "CHEST");

            if ("PLAYER_HEAD".equals(materialString) && !plugin.getVersionManager().hasBlockData()) {
                materialString = "SKULL_ITEM";
            }

            material = Material.matchMaterial(materialString);
        } else {
            material = Material.matchMaterial(plugin.getConfig("gui.menu.list.item.material", grave)
                    .getString("gui.menu.list.item.block", "CHEST"));
        }

        ItemStack itemStack = new ItemStack(material != null ? material : Material.CHEST);

        if ("PLAYER_HEAD".equals(itemStack.getType().name()) || "SKULL_ITEM".equals(itemStack.getType().name())) {
            itemStack = plugin.getCompatibility().getSkullItemStack(grave, plugin);
        }

        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return itemStack;

        final String name;
        if (plugin.getIntegrationManager().hasMiniMessage()) {
            final String newName = StringUtil.parseString("&f" + plugin.getConfig("gui.menu.list.name", grave)
                            .getString("gui.menu.list.name"),
                    grave, plugin).replace("%number%", String.valueOf(number));
            name = MiniMessage.parseString(newName);
        } else {
            name = ChatColor.WHITE + StringUtil.parseString(
                    plugin.getConfig("gui.menu.list.name", grave).getString("gui.menu.list.name"),
                    grave, plugin).replace("%number%", String.valueOf(number));
        }

        final List<String> loreList = new ArrayList<>();
        final int customModelData = plugin.getConfig("gui.menu.list.model-data", grave)
                .getInt("gui.menu.list.model-data", -1);

        for (final String string : plugin.getConfig("gui.menu.list.lore", grave).getStringList("gui.menu.list.lore")) {
            final Entity ownerEntity = Bukkit.getEntity(grave.getOwnerUUID());
            if (plugin.getIntegrationManager().hasMiniMessage()) {
                final String loreOriginal = StringUtil.parseString("&7" + string, ownerEntity,
                        grave.getLocationDeath(), grave, plugin);
                loreList.add(MiniMessage.parseString(loreOriginal));
            } else {
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, ownerEntity,
                        grave.getLocationDeath(), grave, plugin));
            }
        }

        if (plugin.getConfig().getBoolean("gui.menu.list.glow")) {
            itemMeta.addEnchant(durability, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        applyCustomModelData(itemMeta, customModelData);

        itemMeta.setDisplayName(name);
        itemMeta.setLore(loreList);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    /**
     * Creates an ItemStack for a grave menu slot.
     *
     * @param slot  The slot number.
     * @param grave The grave to create the ItemStack for.
     * @return The created ItemStack.
     */
    public ItemStack createGraveMenuItemStack(final int slot, final Grave grave) {
        final String materialString = plugin.getConfig("gui.menu.grave.slot." + slot + ".material", grave)
                .getString("gui.menu.grave.slot." + slot + ".material", "PAPER");
        Material material = Material.matchMaterial(materialString);
        final Enchantment durability = plugin.getVersionManager().getEnchantmentForVersion("DURABILITY");

        if (material == null) {
            material = Material.PAPER;
            plugin.debugMessage(materialString.toUpperCase() + " is not a Material ENUM", 1);
        }

        final ItemStack itemStack = new ItemStack(material);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return itemStack;

        final String name;
        if (plugin.getIntegrationManager().hasMiniMessage()) {
            final String newName = StringUtil.parseString("&f" +
                    plugin.getConfig("gui.menu.grave.slot." + slot + ".name", grave)
                            .getString("gui.menu.grave.slot." + slot + ".name"), grave, plugin);
            name = MiniMessage.parseString(newName);
        } else {
            name = ChatColor.WHITE + StringUtil.parseString(
                    plugin.getConfig("gui.menu.grave.slot." + slot + ".name", grave)
                            .getString("gui.menu.grave.slot." + slot + ".name"), grave, plugin);
        }

        final List<String> loreList = new ArrayList<>();
        final int customModelData = plugin.getConfig("gui.menu.grave.slot." + slot + ".model-data", grave)
                .getInt("gui.menu.grave.slot." + slot + ".model-data", -1);

        for (final String string : plugin.getConfig("gui.menu.grave.slot." + slot + ".lore", grave)
                .getStringList("gui.menu.grave.slot." + slot + ".lore")) {
            final Entity ownerEntity = Bukkit.getEntity(grave.getOwnerUUID());
            if (plugin.getIntegrationManager().hasMiniMessage()) {
                final String newLore = StringUtil.parseString("&7" + string, ownerEntity,
                        grave.getLocationDeath(), grave, plugin);
                loreList.add(MiniMessage.parseString(newLore));
            } else {
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, ownerEntity,
                        grave.getLocationDeath(), grave, plugin));
            }
        }

        if (plugin.getConfig().getBoolean("gui.menu.grave.slot." + slot + ".glow")) {
            itemMeta.addEnchant(durability, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        applyCustomModelData(itemMeta, customModelData);

        itemMeta.setDisplayName(name);
        itemMeta.setLore(loreList);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    /**
     * Applies custom model data using the 1.20.5+ component API if available,
     * falling back to legacy {@link ItemMeta#setCustomModelData(Integer)}.
     *
     * @param meta            the item meta (book meta or item meta)
     * @param customModelData the model data id; ignored if &lt; 0
     */
    private void applyCustomModelData(final ItemMeta meta, final int customModelData) {
        if (customModelData <= -1 || meta == null) return;

        try {
            final CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
            cmdComponent.setFloats(Collections.singletonList((float) customModelData));
            meta.setCustomModelDataComponent(cmdComponent);
        } catch (final Throwable ignored) {
            try {
                meta.setCustomModelData(customModelData);
            } catch (final Throwable ignoreAgain) {
            }
        }
    }
}