package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.compatibility.CompatibilityInventoryView;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.InventoryUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.util.List;
import java.util.UUID;

/**
 * The GUIManager class is responsible for managing the graphical user interfaces related to graves.
 */
public final class GUIManager {
    private final Graves plugin;

    /**
     * Initializes a new instance of the GUIManager class.
     *
     * @param plugin The plugin instance.
     */
    public GUIManager(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the grave list for an entity.
     *
     * @param entity The entity to open the grave list for.
     */
    public void openGraveList(Entity entity) {
        openGraveList(entity, entity.getUniqueId(), true);
    }

    /**
     * Opens the grave list for an entity with a sound option.
     *
     * @param entity The entity to open the grave list for.
     * @param sound  Whether to play a sound.
     */
    public void openGraveList(Entity entity, boolean sound) {
        openGraveList(entity, entity.getUniqueId(), sound);
    }

    /**
     * Opens the grave list for an entity based on another entity's UUID.
     *
     * @param entity  The entity to open the grave list for.
     * @param entity2 The entity whose UUID will be used.
     */
    public void openGraveList(Entity entity, Entity entity2) {
        openGraveList(entity, entity2.getUniqueId(), true);
    }

    /**
     * Opens the grave list for an entity based on a UUID.
     *
     * @param entity The entity to open the grave list for.
     * @param uuid   The UUID to use.
     */
    public void openGraveList(Entity entity, UUID uuid) {
        openGraveList(entity, uuid, true);
    }

    /**
     * Refreshes the menus for all online players.
     */
    @SuppressWarnings("ConstantConditions")
    public void refreshMenus() {
        if (plugin.isEnabled()) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                InventoryView openInventory = player.getOpenInventory();
                if (openInventory != null) { // Mohist might return null even when Bukkit shouldn't.
                    Inventory topInventory = CompatibilityInventoryView.getTopInventory(openInventory);

                    if (topInventory.getHolder() instanceof GraveList) {
                        setGraveListItems(topInventory, ((GraveList) topInventory.getHolder()).getUUID());
                    } else if (topInventory.getHolder() instanceof GraveMenu) {
                        setGraveMenuItems(topInventory, ((GraveMenu) topInventory.getHolder()).getGrave());
                    }
                }
            }
        }
    }

    /**
     * Opens the grave list for an entity based on a UUID with a sound option.
     *
     * @param entity The entity to open the grave list for.
     * @param uuid   The UUID to use.
     * @param sound  Whether to play a sound.
     */
    public void openGraveList(Entity entity, UUID uuid, boolean sound) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            List<String> permissionList = plugin.getPermissionList(player);
            List<Grave> playerGraveList = plugin.getGraveManager().getGraveList(uuid);

            if (!playerGraveList.isEmpty()) {
                GraveList graveList = new GraveList(uuid, playerGraveList);
                Inventory inventory;
                if (plugin.getIntegrationManager().hasMiniMessage()) {
                    String guiTitle = StringUtil.parseString(plugin.getConfig("gui.menu.list.title", player, permissionList)
                            .getString("gui.menu.list.title", "Graves Main Menu"), player, plugin);
                    String guiNew = MiniMessage.convertLegacyToMiniMessage(guiTitle);
                    inventory = plugin.getServer().createInventory(graveList,
                            InventoryUtil.getInventorySize(playerGraveList.size()),
                            MiniMessage.parseString(guiNew));
                } else {
                    inventory = plugin.getServer().createInventory(graveList,
                            InventoryUtil.getInventorySize(playerGraveList.size()),
                            StringUtil.parseString(plugin.getConfig("gui.menu.list.title", player, permissionList)
                                    .getString("gui.menu.list.title", "Graves Main Menu"), player, plugin));
                }

                setGraveListItems(inventory, playerGraveList);
                graveList.setInventory(inventory);
                player.openInventory(graveList.getInventory());

                if (sound) {
                    plugin.getEntityManager().playPlayerSound("sound.menu-open", player, permissionList);
                }
            } else {
                plugin.getEntityManager().sendMessage("message.empty", player, permissionList);
            }
        }
    }

    /**
     * Sets the grave list items in the inventory based on a UUID.
     *
     * @param inventory The inventory to set the items in.
     * @param uuid      The UUID to use.
     */
    public void setGraveListItems(Inventory inventory, UUID uuid) {
        setGraveListItems(inventory, plugin.getGraveManager().getGraveList(uuid));
    }

    /**
     * Sets the grave list items in the inventory based on a list of graves.
     *
     * @param inventory The inventory to set the items in.
     * @param graveList The list of graves to use.
     */
    public void setGraveListItems(Inventory inventory, List<Grave> graveList) {
        inventory.clear();

        int count = 1;

        for (Grave grave : graveList) {
            inventory.addItem(plugin.getItemStackManager().createGraveListItemStack(count, grave));
            count++;
        }
    }

    /**
     * Opens the grave menu for an entity and grave.
     *
     * @param entity The entity to open the grave menu for.
     * @param grave  The grave to open the menu for.
     */
    public void openGraveMenu(Entity entity, Grave grave) {
        openGraveMenu(entity, grave, true);
    }

    /**
     * Opens the grave menu for an entity and grave with a sound option.
     *
     * @param entity The entity to open the grave menu for.
     * @param grave  The grave to open the menu for.
     * @param sound  Whether to play a sound.
     */
    public void openGraveMenu(Entity entity, Grave grave, boolean sound) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            GraveMenu graveMenu = new GraveMenu(grave);
            String title;
            if (plugin.getIntegrationManager().hasMiniMessage()) {
                String newTitle = StringUtil.parseString(plugin.getConfig("gui.menu.grave.title", player, grave.getPermissionList())
                        .getString("gui.menu.grave.title", "Grave"), player, plugin);
                title = MiniMessage.parseString(newTitle);
            } else {
                title = StringUtil.parseString(plugin.getConfig("gui.menu.grave.title", player, grave.getPermissionList())
                        .getString("gui.menu.grave.title", "Grave"), player, plugin);
            }
            Inventory inventory = plugin.getServer().createInventory(graveMenu, InventoryUtil.getInventorySize(5), title);

            setGraveMenuItems(inventory, grave);
            graveMenu.setInventory(inventory);
            player.openInventory(graveMenu.getInventory());

            if (sound) {
                plugin.getEntityManager().playPlayerSound("sound.menu-open", player, grave);
            }
        }
    }

    /**
     * Sets the grave menu items in the inventory based on a grave.
     *
     * @param inventory The inventory to set the items in.
     * @param grave     The grave to use.
     */
    public void setGraveMenuItems(Inventory inventory, Grave grave) {
        inventory.clear();

        ConfigurationSection configurationSection = plugin.getConfig("gui.menu.grave.slot", grave)
                .getConfigurationSection("gui.menu.grave.slot");

        if (configurationSection != null) {
            for (String string : configurationSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(string);
                    if (plugin.getConfig("gui.menu.grave.slot." + slot + ".enabled", grave)
                            .getBoolean("gui.menu.grave.slot." + slot + ".enabled")) {
                        inventory.setItem(slot, plugin.getItemStackManager().createGraveMenuItemStack(slot, grave));
                    }
                } catch (NumberFormatException exception) {
                    plugin.debugMessage(string + " is not an int", 1);
                }
            }
        }
    }
}