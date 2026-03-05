package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.compatibility.CompatibilityInventoryView;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.InventoryUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The GUIManager class is responsible for managing the graphical user interfaces related to graves.
 */
public class GUIManager {
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
                // Mohist might return null even when Bukkit shouldn't.
                if (openInventory != null) {
                    Inventory topInventory = CompatibilityInventoryView.getTopInventory(openInventory);

                    if (topInventory.getHolder() instanceof GraveList graveList) {
                        setGraveListItems(topInventory, graveList.getUUID());
                    } else if (topInventory.getHolder() instanceof GraveMenu graveMenu) {
                        setGraveMenuItems(topInventory, graveMenu.getGrave());
                    }
                }
            }
        }
    }

    /**
     * This method schedules the per-player refresh on the player's region thread using an
     * anchor at the player's current location.
     */
    public void refreshMenusSafely() {
        List<Player> players = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (players.isEmpty()) return;

        for (Player player : players) {
            if (player == null || !player.isOnline()) continue;

            Location loc;
            try {
                loc = player.getLocation();
            } catch (Throwable t) {
                continue;
            }
            if (loc.getWorld() == null) continue;

            Location anchor = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());

            plugin.getSchedulerManager().execute(anchor, () -> {
                if (!player.isOnline()) return;

                InventoryView openInventory;
                try {
                    openInventory = player.getOpenInventory();
                } catch (Throwable t) {
                    return;
                }
                Inventory topInventory = CompatibilityInventoryView.getTopInventory(openInventory);

                if (topInventory == null) return;

                try {
                    InventoryHolder holder = topInventory.getHolder();

                    if (holder instanceof GraveList graveList) {
                        setGraveListItems(topInventory, graveList.getUUID());
                    } else if (holder instanceof GraveMenu graveMenu) {
                        setGraveMenuItems(topInventory, graveMenu.getGrave());
                    }
                } catch (IllegalStateException threadCheck) {
                    // Folia thread-check ("Cannot read world asynchronously") - skip this refresh.
                    plugin.debugMessage("Skipping menu refresh for " + player.getName() + ": " + threadCheck.getMessage(), 2);
                } catch (Throwable ignored) {
                    // Keep menu refresh resilient; we don't want one bad inventory to break the tick.
                }
            });
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
        if (entity instanceof Player player) {
            List<String> permissionList = plugin.getPermissionList(player);
            List<Grave> playerGraveList = plugin.getGraveManager().getGraveList(uuid);

            if (!playerGraveList.isEmpty()) {
                GraveList graveList = new GraveList(uuid, playerGraveList);
                Inventory inventory;

                if (plugin.getIntegrationManager().hasMiniMessage()) {
                    String guiTitle = StringUtil.parseString(
                            plugin.getConfigManager().getConfigSection("gui.menu.list.title", player, permissionList)
                                    .getString("gui.menu.list.title", "Graves Main Menu"),
                            player, plugin
                    );
                    String guiNew = MiniMessage.convertLegacyToMiniMessage(guiTitle);
                    inventory = plugin.getServer().createInventory(
                            graveList,
                            InventoryUtil.getInventorySize(playerGraveList.size()),
                            MiniMessage.parseString(guiNew)
                    );
                } else {
                    inventory = plugin.getServer().createInventory(
                            graveList,
                            InventoryUtil.getInventorySize(playerGraveList.size()),
                            StringUtil.parseString(
                                    plugin.getConfigManager().getConfigSection("gui.menu.list.title", player, permissionList)
                                            .getString("gui.menu.list.title", "Graves Main Menu"),
                                    player, plugin
                            )
                    );
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
        if (entity instanceof Player player) {
            GraveMenu graveMenu = new GraveMenu(grave);
            String title;

            if (plugin.getIntegrationManager().hasMiniMessage()) {
                String newTitle = StringUtil.parseString(
                        plugin.getConfigManager().getConfigSection("gui.menu.grave.title", player, grave.getPermissionList())
                                .getString("gui.menu.grave.title", "Grave"),
                        player, plugin
                );
                title = MiniMessage.parseString(newTitle);
            } else {
                title = StringUtil.parseString(
                        plugin.getConfigManager().getConfigSection("gui.menu.grave.title", player, grave.getPermissionList())
                                .getString("gui.menu.grave.title", "Grave"),
                        player, plugin
                );
            }

            int size = computeGraveMenuSize(grave);

            Inventory inventory = plugin.getServer().createInventory(graveMenu, size, title);

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

        ConfigurationSection configurationSection = plugin.getConfigManager()
                .getConfigSection("gui.menu.grave.slot", grave)
                .getConfigurationSection("gui.menu.grave.slot");

        if (configurationSection != null) {
            for (String string : configurationSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(string);

                    if (slot < 0 || slot >= inventory.getSize()) {
                        plugin.getLogger().warning("[GUI] Grave menu slot " + slot + " out of bounds (size=" + inventory.getSize() + "). Check gui.menu.grave.slot.*");
                        continue;
                    }

                    if (plugin.getConfigManager().getConfigSection("gui.menu.grave.slot." + slot + ".enabled", grave).getBoolean("gui.menu.grave.slot." + slot + ".enabled")) {
                        inventory.setItem(slot, plugin.getItemStackManager().createGraveMenuItemStack(slot, grave));
                    }
                } catch (NumberFormatException exception) {
                    plugin.debugMessage(string + " is not an int", 1);
                }
            }
        }
    }

    /**
     * Computes the inventory size needed to fit the configured grave menu slots.
     * Rounds up to a multiple of 9 and clamps to 54.
     */
    private int computeGraveMenuSize(Grave grave) {
        ConfigurationSection slots = plugin.getConfigManager().getConfigSection("gui.menu.grave.slot", grave).getConfigurationSection("gui.menu.grave.slot");

        int maxSlot = -1;

        if (slots != null) {
            for (String key : slots.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    if (slot < 0) continue;

                    boolean enabled = plugin.getConfigManager().getConfigSection("gui.menu.grave.slot." + slot + ".enabled", grave).getBoolean("gui.menu.grave.slot." + slot + ".enabled");

                    if (enabled) {
                        maxSlot = Math.max(maxSlot, slot);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        int required = Math.max(9, (maxSlot >= 0 ? (maxSlot + 1) : 9));

        int size = ((required + 8) / 9) * 9;

        if (size > 54) {
            plugin.getLogger().warning("[GUI] Grave menu requires " + size + " slots (maxSlot=" + maxSlot + "). Clamping to 54.");
            size = 54;
        }

        return size;
    }
}