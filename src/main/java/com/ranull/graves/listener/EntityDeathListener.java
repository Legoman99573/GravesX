package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.compatibility.CompatibilityInventoryView;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.ExperienceUtil;
import com.ranull.graves.util.LocationUtil;
import dev.cwhead.GravesX.event.*;
import dev.cwhead.GravesX.util.SkinTextureUtil_post_1_21_9;
import me.jay.GravesX.util.SkinSignatureUtil;
import me.jay.GravesX.util.SkinTextureUtil;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Listener for handling entity death events and creating graves.
 */
public class EntityDeathListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs an EntityDeathListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public EntityDeathListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the EntityDeathEvent to create a grave based on various conditions.
     *
     * @param event The EntityDeathEvent to handle.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        final LivingEntity livingEntity = event.getEntity();
        final boolean isPlayer = livingEntity instanceof Player;
        final Player player = isPlayer ? (Player) livingEntity : null;
        final PlayerDeathEvent pde = (event instanceof PlayerDeathEvent) ? (PlayerDeathEvent) event : null;

        if (isInvalidMohistDeath(event)) {
            return;
        }

        final String entityName = plugin.getEntityManager().getEntityName(livingEntity);
        if (isInvalidGraveZombie(event, livingEntity, entityName)) {
            return;
        }

        final Location location = LocationUtil.roundLocation(livingEntity.getLocation());
        if (location == null) {
            plugin.debugMessage("Grave not created for " + entityName + " because the location couldn't be determined.", 2);
            return;
        }

        final List<String> permissionList = isPlayer ? plugin.getConfigManager().getPermissionList(livingEntity) : null;

        if (!isPlayer && livingEntity instanceof Zombie zombie) {
            if (isConfiguredZombieType(zombie) && hasGravesXMetadata(zombie)) {
                removePlayerSkullFromDrops(zombie, event);
            }
        }

        if (!isEnabledGrave(livingEntity, permissionList, entityName)) return;

        final List<String> worldList =
                plugin.getConfigManager().getConfigSection("world", livingEntity, permissionList).getStringList("world");
        if (!isValidWorld(worldList, livingEntity, entityName)) return;

        if (!isValidDamageCause(livingEntity, permissionList, entityName)) return;

        if (!isPlayer && isInvalidCreatureSpawn(livingEntity, null, entityName)) return;

        if (isPlayer) {
            if (handlePlayerDeath(player, entityName)) return;

            if (pde != null && isKeepInventory(pde, entityName)) {
                boolean bypass = plugin.getPermissionManager().hasGrantedPermission("graves.keepinventory.bypass", player.getPlayer());

                if (!bypass) {
                    plugin.debugMessage("Grave not created for " + entityName + " because they had keep inventory. You can set a user to have the bypass permission.", 2);
                    return;
                }

                try {
                    pde.setKeepInventory(false);
                } catch (NoSuchMethodError ignored) {
                    // Ignore
                }

                plugin.debugMessage("Grave creation proceeding for " + entityName + "; forcing keep inventory off to avoid duplication.", 1);
            } else {
                if (pde != null) {
                    pde.setKeepInventory(false);
                }
            }

            if (pde != null && event.getDrops().isEmpty()) {
                plugin.debugMessage("Grave not created for " + entityName + " because they had an empty inventory", 2);
                return;
            }
        }

        if (!hasValidToken(livingEntity, permissionList, entityName, event.getDrops())) return;

        if (isPlayer) {
            int serverMax = plugin.getConfigManager().getConfigSection("grave.max", livingEntity, permissionList).getInt("grave.max");
            int permsMax = getMaxGravesPermission(player);
            int applicableMax = (permsMax > 0) ? permsMax : serverMax;

            if (plugin.getGraveManager().getGraveList(livingEntity).size() >= applicableMax) {
                if (plugin.getPermissionManager().hasGrantedPermission("graves.max.replace", player) && plugin.getConfigManager().getConfigSection("grave.replace-oldest", livingEntity, permissionList).getBoolean("grave.replace-oldest")) {
                    plugin.getGraveManager().removeOldestGrave(livingEntity);
                    plugin.getEntityManager().sendMessage("message.grave-oldest-replaced", livingEntity, livingEntity.getLocation(), permissionList);
                    plugin.debugMessage("Grave replaced oldest for " + entityName + " because they reached maximum graves", 2);
                } else if (plugin.getPermissionManager().hasGrantedPermission("graves.max.bypass", player)) {
                    plugin.debugMessage("Grave created for " + entityName + " even though they reached the maximum graves cap", 2);
                } else {
                    plugin.getEntityManager().sendMessage("message.max", livingEntity, livingEntity.getLocation(), permissionList);
                    plugin.debugMessage("Grave not created for " + entityName + " because they reached maximum graves.", 2);

                    boolean keepInvOnMax = plugin.getConfigManager().getConfigSection("placement.failure-keep-inventory", livingEntity, permissionList).getBoolean("placement.failure-keep-inventory");

                    if (keepInvOnMax && pde != null) {
                        try {
                            pde.setKeepLevel(true);
                            pde.setKeepInventory(true);

                            event.getDrops().clear();
                            plugin.getEntityManager().sendMessage("message.failure-keep-inventory", livingEntity, location, permissionList);
                        } catch (NoSuchMethodError ignored) {
                            // Older APIs may not support keepInventory/keepLevel
                        }
                    }

                    return;
                }
            }
        }

        final List<ItemStack> ignoredItemStackList = new ArrayList<>();
        final List<ItemStack> graveItemStackList =
                getGraveItemStackList(event, livingEntity, permissionList, ignoredItemStackList);

        if (graveItemStackList.isEmpty()) {
            plugin.debugMessage("Grave not created for " + entityName + " because they had no drops", 2);
            return;
        }

        final List<ItemStack> removedItemStackList = getRemovedItemStacks(livingEntity);

        if (isPlayer && pde != null && location.getWorld() != null) {
            InventoryView view = player.getOpenInventory();
            if (view != null) {
                Inventory top = CompatibilityInventoryView.getTopInventory(view);
                if (top != null && (top.getType() == InventoryType.WORKBENCH || top.getType() == InventoryType.CRAFTING)) {
                    ItemStack[] contents = top.getContents();
                    for (int i = 1; i < contents.length; i++) {
                        ItemStack item = contents[i];
                        if (item != null && item.getType() != Material.AIR) {
                            location.getWorld().dropItemNaturally(location, item);
                            top.setItem(i, null);
                        }
                    }
                }
            }
        }

        event.getDrops().clear();

        createGrave(event, livingEntity, entityName, permissionList,
                removedItemStackList, graveItemStackList, ignoredItemStackList,
                location, pde, player);
    }

    /**
     * Retrieves the maximum number of graves a player is allowed to have based on their permissions.
     * <p>
     * The method checks for permissions related to grave limits and returns the highest limit found. If the player
     * has the "grave.max.limit.unlimited" permission, the method will return {@code Integer.MAX_VALUE} indicating
     * that the player has no limit on the number of graves. If no specific permissions are found, the method returns
     * {@code 0} by default, which should be interpreted as no specific limit set by permissions.
     * </p>
     *
     * @param player The player whose grave limit is being checked.
     * @return The maximum number of graves the player is allowed to have. Returns {@code Integer.MAX_VALUE} for
     *         unlimited graves, or {@code 0} if no specific limit is set by permissions.
     */
    private int getMaxGravesPermission(Player player) {
        int maxGraves = 0;
        for (int i = 0; i <= 10; i++) {
            if (plugin.getPermissionManager().hasGrantedPermission("grave.max.limit.unlimited", player)) return Integer.MAX_VALUE;
            if (plugin.getPermissionManager().hasGrantedPermission("grave.max.limit." + i, player)) maxGraves = i;
        }
        return maxGraves;
    }

    /**
     * Checks if the zombie is of the type configured in config.yml.
     *
     * @param zombie the zombie entity to check
     * @return true if the zombie is of the configured type, false otherwise
     */
    private boolean isConfiguredZombieType(Zombie zombie) {
        final String configuredZombieType = plugin.getConfig().getString("zombie.type", "ZOMBIE").toUpperCase();
        return zombie.getType() == EntityType.valueOf(configuredZombieType);
    }

    /**
     * Removes player skull from the drops of the entity if it is wearing one.
     *
     * @param entity the entity whose drops are to be modified
     * @param event  the EntityDeathEvent containing the drops
     */
    private void removePlayerSkullFromDrops(LivingEntity entity, EntityDeathEvent event) {
        ItemStack helmet = entity.getEquipment() != null ? entity.getEquipment().getHelmet() : null;
        if (helmet != null && helmet.getType() == Material.PLAYER_HEAD) {
            event.getDrops().removeIf(i -> i != null && i.getType() == Material.PLAYER_HEAD);
        }
    }

    /**
     * Checks if the given entity has the GravesX metadata.
     *
     * @param zombie the zombie entity to check
     * @return true if the entity has the GravesX metadata, false otherwise
     */
    private boolean hasGravesXMetadata(Zombie zombie) {
        for (MetadataValue v : zombie.getMetadata("GravesX")) if (v.asBoolean()) return true;
        return false;
    }

    /**
     * Retrieves the list of removed item stacks for the specified entity.
     *
     * @param livingEntity The entity whose removed item stacks are to be retrieved.
     * @return The list of removed item stacks.
     */
    private List<ItemStack> getRemovedItemStacks(LivingEntity livingEntity) {
        List<ItemStack> list = new ArrayList<>();
        Map<UUID, List<ItemStack>> map = plugin.getCacheManager().getRemovedItemStackMap();
        if (map.containsKey(livingEntity.getUniqueId())) {
            list.addAll(map.get(livingEntity.getUniqueId()));
            map.remove(livingEntity.getUniqueId());
        }
        return list;
    }

    /**
     * Checks if the entity death event is an invalid Mohist death.
     *
     * @param event The entity death event to check.
     * @return True if the event is an invalid Mohist death, false otherwise.
     */
    private boolean isInvalidMohistDeath(EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.PLAYER && !(event instanceof PlayerDeathEvent)) {
            event.setDroppedExp(0);
            return true;
        }
        return false;
    }

    /**
     * Checks if the entity is an invalid grave zombie.
     *
     * @param event        The entity death event.
     * @param livingEntity The entity to check.
     * @param entityName   The name of the entity.
     * @return True if the entity is an invalid grave zombie, false otherwise.
     */
    private boolean isInvalidGraveZombie(EntityDeathEvent event, LivingEntity livingEntity, String entityName) {
        if (plugin.getEntityManager().hasDataByte(livingEntity, "graveZombie")) {
            EntityType type = plugin.getEntityManager().hasDataString(livingEntity, "graveEntityType")
                    ? EntityType.valueOf(plugin.getEntityManager().getDataString(livingEntity, "graveEntityType"))
                    : EntityType.PLAYER;
            List<String> perms = plugin.getEntityManager().hasDataString(livingEntity, "gravePermissionList")
                    ? Arrays.asList(plugin.getEntityManager().getDataString(livingEntity, "gravePermissionList").split("\\|"))
                    : null;
            if (!plugin.getConfigManager().getConfigSection("zombie.drop", type, perms).getBoolean("zombie.drop")) {
                event.setDroppedExp(0);
            }
            return true;
        }
        return false;
    }

    /**
     * Handles player death and checks if a grave should be created.
     *
     * @param player     The player who died.
     * @param entityName The name of the player.
     * @return True if a grave should not be created, false otherwise.
     */
    private boolean handlePlayerDeath(Player player, String entityName) {
        Plugin essentials = plugin.getServer().getPluginManager().getPlugin("Essentials");

        if (!plugin.getPermissionManager().hasGrantedPermission("graves.place", player)) {
            plugin.debugMessage("Grave not created for " + entityName + " because they don't have permission to place graves", 2);
            return true;
        } else if ((essentials != null && essentials.isEnabled()) && plugin.getPermissionManager().hasGrantedPermission("essentials.keepinv", player)) {
            plugin.debugMessage(entityName + " has essentials.keepinv", 2);
        }
        return false;
    }

    /**
     * Checks if graves are enabled for the specified entity.
     *
     * @param livingEntity  The entity to check.
     * @param permissionList The list of permissions.
     * @param entityName    The name of the entity.
     * @return True if graves are enabled, false otherwise.
     */
    private boolean isEnabledGrave(LivingEntity livingEntity, List<String> permissionList, String entityName) {
        if (!plugin.getConfigManager().getConfigSection("grave.enabled", livingEntity, permissionList).getBoolean("grave.enabled")) {
            if (livingEntity instanceof Player) {
                plugin.debugMessage("Grave not created for " + entityName + " because they have graves disabled", 2);
            }
            return false;
        }
        return true;
    }

    /**
     * Checks if the player has keep inventory enabled.
     *
     * @param event      The player death event.
     * @param entityName The name of the player.
     * @return True if the player has keep inventory enabled, false otherwise.
     */
    private boolean isKeepInventory(PlayerDeathEvent event, String entityName) {
        try {
            if (event.getKeepInventory() && !plugin.getPermissionManager().hasGrantedPermission("graves.keepinventory.bypass", event.getEntity().getPlayer())) {
                plugin.debugMessage("Grave not created for " + entityName + " because they had keep inventory", 2);
                return true;
            }
        } catch (NoSuchMethodError ignored) {}
        return false;
    }

    /**
     * Checks if the creature spawn reason is valid.
     *
     * @param livingEntity  The creature entity.
     * @param permissionList The list of permissions.
     * @param entityName    The name of the entity.
     * @return True if the spawn reason is invalid, false otherwise.
     */
    private boolean isInvalidCreatureSpawn(LivingEntity livingEntity, List<String> permissionList, String entityName) {
        if (livingEntity instanceof Creature) {
            List<String> reasons = plugin.getConfigManager().getConfigSection("spawn.reason", livingEntity, permissionList).getStringList("spawn.reason");
            if (plugin.getEntityManager().hasDataString(livingEntity, "spawnReason")
                    && (!reasons.contains("ALL")
                    && !reasons.contains(plugin.getEntityManager().getDataString(livingEntity, "spawnReason")))) {
                plugin.debugMessage("Grave not created for " + entityName + " because they had an invalid spawn reason", 2);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the entity is in a valid world.
     *
     * @param worldList    The list of valid worlds.
     * @param livingEntity The entity to check.
     * @param entityName   The name of the entity.
     * @return True if the entity is in a valid world, false otherwise.
     */
    private boolean isValidWorld(List<String> worldList, LivingEntity livingEntity, String entityName) {
        if (!worldList.contains("ALL") && !worldList.contains(livingEntity.getWorld().getName())) {
            plugin.debugMessage("Grave not created for " + entityName + " because they are not in a valid world", 2);
            return false;
        }
        return true;
    }

    /**
     * Checks if the damage cause is valid for creating a grave.
     *
     * @param livingEntity  The entity that was damaged.
     * @param permissionList The list of permissions.
     * @param entityName    The name of the entity.
     * @return True if the damage cause is valid, false otherwise.
     */
    private boolean isValidDamageCause(LivingEntity livingEntity, List<String> permissionList, String entityName) {
        if (livingEntity.getLastDamageCause() != null) {
            EntityDamageEvent.DamageCause cause = livingEntity.getLastDamageCause().getCause();
            List<String> allowed = plugin.getConfigManager().getConfigSection("death.reason", livingEntity, permissionList).getStringList("death.reason");

            boolean ok =
                    allowed.contains("ALL")
                            || allowed.contains(cause.name())
                            || (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                            && ((livingEntity.getKiller() != null
                            && plugin.getConfigManager().getConfigSection("death.player", livingEntity, permissionList).getBoolean("death.player"))
                            || (livingEntity.getKiller() == null
                            && plugin.getConfigManager().getConfigSection("death.entity", livingEntity, permissionList).getBoolean("death.entity"))))
                            || (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                            && plugin.getConfigManager().getConfigSection("death.environmental", livingEntity, permissionList).getBoolean("death.environmental"));

            if (!ok) {
                plugin.debugMessage("Grave not created for " + entityName + " because they died to an invalid damage cause", 2);
                return false;
            }
        }
        return true;
    }

    /**
     * @deprecated Use GravesXModule: Tokens instead
     *
     * Checks if the entity has a valid grave token.
     *
     * @param livingEntity  The entity to check.
     * @param permissionList The list of permissions.
     * @param entityName    The name of the entity.
     * @param drops         The list of item drops.
     * @return True if the entity has a valid grave token, false otherwise.
     */
    @Deprecated (forRemoval = true, since = "4.9.10.10")
    private boolean hasValidToken(LivingEntity livingEntity, List<String> permissionList, String entityName, List<ItemStack> drops) {
        if (plugin.getVersionManager().hasPersistentData()
                && plugin.getConfigManager().getConfigSection("token.enabled", livingEntity, permissionList).getBoolean("token.enabled")) {
            String name = plugin.getConfigManager().getConfigSection("token.name", livingEntity).getString("token.name", "basic");
            if (plugin.getConfig().isConfigurationSection("settings.token." + name)) {
                ItemStack token = plugin.getRecipeManager().getGraveTokenFromPlayer(name, drops);
                if (token != null) {
                    token.setAmount(token.getAmount() - 1);
                } else {
                    plugin.getEntityManager().sendMessage("message.no-token", livingEntity, livingEntity.getLocation(), permissionList);
                    plugin.debugMessage("Grave not created for " + entityName + " because they did not have a grave token", 2);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Retrieves the list of item stacks for the grave.
     *
     * <p>In EXACT mode we must preserve slot positions. We therefore start from the player's inventory
     * slot layout, but ONLY keep items that are actually present in {@code event.getDrops()} (multiset/amount aware).
     * Anything not present in drops is set to {@code null} to avoid duping.</p>
     *
     * @param event                 The entity death event.
     * @param livingEntity          The entity that died.
     * @param permissionList        The list of permissions.
     * @param ignoredItemStackList  A list to populate with items that should be ignored by the grave
     *                              (they will be dropped normally instead of stored).
     * @return The list of item stacks for the grave.
     */
    private List<ItemStack> getGraveItemStackList(EntityDeathEvent event,
                                                  LivingEntity livingEntity,
                                                  List<String> permissionList,
                                                  List<ItemStack> ignoredItemStackList) {

        try {
            final boolean exactMode = (event.getEntity() instanceof Player player)
                    && plugin.getGraveManager().getStorageMode(
                    plugin.getConfigManager().getConfigSection("storage.mode", player).getString("storage.mode")
            ) == Grave.StorageMode.EXACT;

            if (exactMode) {
                final Player player = (Player) event.getEntity();

                final List<ItemStack> remainingDrops = new ArrayList<>();
                for (ItemStack drop : event.getDrops()) {
                    if (drop == null || drop.getType() == Material.AIR) continue;
                    remainingDrops.add(drop.clone());
                }

                final List<ItemStack> slots = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
                final ListIterator<ItemStack> it = slots.listIterator();

                while (it.hasNext()) {
                    final ItemStack invItem = it.next();
                    if (invItem == null || invItem.getType() == Material.AIR) {
                        continue;
                    }

                    // ---- Curse of Binding: stays equipped, never stored in grave ----
                    if (plugin.getVersionManager().hasEnchantmentCurse()
                            && invItem.containsEnchantment(Enchantment.BINDING_CURSE)) {
                        it.set(null);
                        continue;
                    }

                    // ---- Curse of Vanishing: not stored in grave ----
                    if (plugin.getVersionManager().hasEnchantmentCurse()
                            && invItem.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                        it.set(null);
                        continue;
                    }

                    // Compass (grave item) handling
                    final UUID graveId = plugin.getEntityManager().getGraveUUIDFromItemStack(invItem);
                    if (graveId != null) {
                        if (plugin.getConfigManager().getConfigSection("compass.destroy", livingEntity, permissionList).getBoolean("compass.destroy")) {
                            it.set(null);
                        } else if (plugin.getConfigManager().getConfigSection("compass.ignore", livingEntity, permissionList).getBoolean("compass.ignore")) {
                            if (ignoredItemStackList != null) ignoredItemStackList.add(invItem);
                            it.set(null);
                        }
                        continue;
                    }

                    if (plugin.getGraveManager().shouldIgnoreItemStack(invItem, livingEntity, permissionList)) {
                        if (ignoredItemStackList != null) ignoredItemStackList.add(invItem);
                        it.set(null);
                        continue;
                    }

                    final int consumed = consumeFromDropsBySimilarity(remainingDrops, invItem);
                    if (consumed <= 0) {
                        it.set(null);
                        continue;
                    }

                    final ItemStack stored = invItem.clone();
                    stored.setAmount(consumed);
                    it.set(stored);
                }

                event.getDrops().clear();
                for (ItemStack left : remainingDrops) {
                    if (left != null && left.getType() != Material.AIR && left.getAmount() > 0) {
                        event.getDrops().add(left);
                    }
                }

                return slots;
            }

            final List<ItemStack> graveList = new ArrayList<>();
            final ListIterator<ItemStack> dropIt = event.getDrops().listIterator();

            while (dropIt.hasNext()) {
                final ItemStack item = dropIt.next();
                if (item == null || item.getType() == Material.AIR) continue;

                if (plugin.getVersionManager().hasEnchantmentCurse()
                        && (item.containsEnchantment(Enchantment.BINDING_CURSE)
                        || item.containsEnchantment(Enchantment.VANISHING_CURSE))) {
                    dropIt.remove();
                    continue;
                }

                final UUID graveId = plugin.getEntityManager().getGraveUUIDFromItemStack(item);
                if (graveId != null) {
                    if (plugin.getConfigManager().getConfigSection("compass.destroy", livingEntity, permissionList).getBoolean("compass.destroy")) {
                        dropIt.remove();
                    } else if (!plugin.getConfigManager().getConfigSection("compass.ignore", livingEntity, permissionList).getBoolean("compass.ignore")) {
                        graveList.add(item);
                        dropIt.remove();
                    } else {
                        if (ignoredItemStackList != null) ignoredItemStackList.add(item);
                        dropIt.remove();
                    }
                    continue;
                }

                if (!plugin.getGraveManager().shouldIgnoreItemStack(item, livingEntity, permissionList)) {
                    graveList.add(item);
                    dropIt.remove();
                } else {
                    if (ignoredItemStackList != null) ignoredItemStackList.add(item);
                    dropIt.remove();
                }
            }

            return graveList;

        } catch (ArrayIndexOutOfBoundsException ignored) {
            return new ArrayList<>();
        }
    }

    /**
     * Consumes up to {@code target.getAmount()} from {@code remainingDrops} using {@link ItemStack#isSimilar(ItemStack)}
     * (exact meta match, ignoring amount). Mutates {@code remainingDrops} by decrementing/removing stacks.
     *
     * @return amount actually consumed (0 if no matching drops)
     */
    private static int consumeFromDropsBySimilarity(List<ItemStack> remainingDrops, ItemStack target) {
        int need = target.getAmount();
        int consumed = 0;

        for (ListIterator<ItemStack> it = remainingDrops.listIterator(); it.hasNext() && need > 0; ) {
            final ItemStack drop = it.next();
            if (drop == null || drop.getType() == Material.AIR) {
                it.remove();
                continue;
            }

            if (!target.isSimilar(drop)) {
                continue;
            }

            final int take = Math.min(need, drop.getAmount());
            consumed += take;
            need -= take;

            final int left = drop.getAmount() - take;
            if (left <= 0) {
                it.remove();
            } else {
                drop.setAmount(left);
                it.set(drop);
            }
        }

        return consumed;
    }

    /**
     * Creates a grave for the specified entity.
     *
     * @param event                The entity death event.
     * @param livingEntity         The entity that died.
     * @param entityName           The name of the entity.
     * @param permissionList       The list of permissions.
     * @param removedItemStackList The list of removed item stacks.
     * @param graveItemStackList   The list of item stacks for the grave.
     * @param ignoredItemStackList The list of items that were ignored for the grave and should be dropped normally.
     * @param location             The location of the grave.
     */
    private void createGrave(EntityDeathEvent event,
                             LivingEntity livingEntity,
                             String entityName,
                             List<String> permissionList,
                             List<ItemStack> removedItemStackList,
                             List<ItemStack> graveItemStackList,
                             List<ItemStack> ignoredItemStackList,
                             Location location,
                             PlayerDeathEvent pde,
                             Player player) {

        List<Block> ignoredBlockList = new ArrayList<>();

        Grave grave = new Grave(UUID.randomUUID());

        GraveCreateEvent modern = new GraveCreateEvent(livingEntity, grave, graveItemStackList, ignoredItemStackList, ignoredBlockList);

        if (ignoredItemStackList != null && !ignoredItemStackList.isEmpty()) {
            modern.setIgnoredItems(ignoredItemStackList);
        }
        modern.setIgnoredBlocks(ignoredBlockList);

        setupGrave(modern, grave, livingEntity, entityName, permissionList);
        setGraveExperience(modern, grave, event, livingEntity, pde);
        setupGraveKiller(modern, grave, livingEntity);
        setupGraveProtection(modern, livingEntity, grave);

        if (plugin.getConfigManager().getConfigSection("placement.safe-location", grave).getBoolean("placement.safe-location", true)) {
            Location safeLocation = plugin.getLocationManager().getSafeGraveLocation(livingEntity, location, grave);
            Location target = safeLocation != null ? safeLocation : location;
            event.setDroppedExp(0);
            if (plugin.getLocationManager().hasCachedGraveAt(target)) {
                Location newLoc = plugin.getLocationManager().getNewLocationIfCachedGraveExists(livingEntity, target, grave);
                if (newLoc != null) {
                    target = newLoc;
                }
            }
            modern.setDeathLocation(LocationUtil.roundLocation(target));
        } else {
            event.setDroppedExp(0);
            Location target = location;
            if (plugin.getLocationManager().hasCachedGraveAt(target)) {
                Location newLoc = plugin.getLocationManager().getNewLocationIfCachedGraveExists(livingEntity, target, grave);
                if (newLoc != null) {
                    target = newLoc;
                }
            }
            modern.setDeathLocation(LocationUtil.roundLocation(target));
        }

        GravePreCreateEvent pre = new GravePreCreateEvent(livingEntity, grave, graveItemStackList, ignoredItemStackList, ignoredBlockList);

        pre.setGraveUUID(grave.getUUID());
        pre.setDeathLocation(modern.getLocationDeath());
        pre.setOwnerType(modern.getOwnerType());
        pre.setOwnerName(modern.getOwnerName());
        pre.setOwnerNameDisplay(modern.getOwnerNameDisplay());
        pre.setOwnerUUID(modern.getOwnerUUID());
        pre.setPermissionList(modern.getPermissionList());
        pre.setYaw(modern.getYaw());
        pre.setPitch(modern.getPitch());
        pre.setTimeAlive(modern.getTimeAlive());
        pre.setOwnerTexture(modern.getOwnerTexture());
        pre.setOwnerTextureSignature(modern.getOwnerTextureSignature());
        pre.setExperience(modern.getExperience());
        pre.setKillerType(modern.getKillerType());
        pre.setKillerName(modern.getKillerName());
        pre.setKillerNameDisplay(modern.getKillerNameDisplay());
        pre.setKillerUUID(modern.getKillerUUID());
        pre.setProtection(modern.getProtection());
        pre.setTimeProtection(modern.getTimeProtection());

        plugin.getServer().getPluginManager().callEvent(pre);

        if (pre.isAddon()) {
            return;
        }

        if (pre.isCancelled()) {
            List<ItemStack> toReturn = new ArrayList<>();
            Set<ItemStack> identitySet = Collections.newSetFromMap(new IdentityHashMap<>());

            if (removedItemStackList != null) {
                for (ItemStack item : removedItemStackList) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    if (identitySet.add(item)) toReturn.add(item);
                }
            }
            if (graveItemStackList != null) {
                for (ItemStack item : graveItemStackList) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    if (identitySet.add(item)) toReturn.add(item);
                }
            }
            if (ignoredItemStackList != null) {
                for (ItemStack item : ignoredItemStackList) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    if (identitySet.add(item)) toReturn.add(item);
                }
            }

            if (!toReturn.isEmpty()) {
                event.getDrops().addAll(toReturn);
            }
            return;
        }

        UUID preUUID = pre.getGraveUUID();
        if (preUUID != null && !preUUID.equals(grave.getUUID())) {
            grave = new Grave(preUUID);
            modern = new GraveCreateEvent(livingEntity, grave, graveItemStackList, ignoredItemStackList, ignoredBlockList);

            if (ignoredItemStackList != null && !ignoredItemStackList.isEmpty()) {
                modern.setIgnoredItems(ignoredItemStackList);
            }
            modern.setIgnoredBlocks(ignoredBlockList);
        }

        modern.setDeathLocation(pre.getLocationDeath());
        modern.setOwnerType(pre.getOwnerType());
        modern.setOwnerName(pre.getOwnerName());
        modern.setOwnerNameDisplay(pre.getOwnerNameDisplay());
        modern.setOwnerUUID(pre.getOwnerUUID());
        modern.setPermissionList(pre.getPermissionList().isEmpty() ? null : new ArrayList<>(pre.getPermissionList()));
        modern.setYaw(pre.getYaw());
        modern.setPitch(pre.getPitch());
        modern.setTimeAlive(pre.getTimeAlive());
        modern.setOwnerTexture(pre.getOwnerTexture());
        modern.setOwnerTextureSignature(pre.getOwnerTextureSignature());
        modern.setExperience(pre.getExperience());
        modern.setKillerType(pre.getKillerType());
        modern.setKillerName(pre.getKillerName());
        modern.setKillerNameDisplay(pre.getKillerNameDisplay());
        modern.setKillerUUID(pre.getKillerUUID());
        modern.setProtection(pre.getProtection());
        modern.setTimeProtection(pre.getTimeProtection());

        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveCreateEvent legacy =
                new com.ranull.graves.event.GraveCreateEvent(livingEntity, grave, ignoredItemStackList, ignoredBlockList);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (graveItemStackList != null) {
            graveItemStackList.clear();
            graveItemStackList.addAll(modern.getGraveItemStackList());
        } else if (!modern.getGraveItemStackList().isEmpty()) {
            graveItemStackList = new ArrayList<>(modern.getGraveItemStackList());
        }

        List<ItemStack> effectiveIgnoredItems = new ArrayList<>();
        Set<ItemStack> itemsIdentity = Collections.newSetFromMap(new IdentityHashMap<>());

        if (ignoredItemStackList != null) {
            for (ItemStack item : ignoredItemStackList) {
                if (item != null && itemsIdentity.add(item)) {
                    effectiveIgnoredItems.add(item);
                }
            }
        }
        for (ItemStack item : modern.getIgnoredItems()) {
            if (item != null && itemsIdentity.add(item)) {
                effectiveIgnoredItems.add(item);
            }
        }
        for (ItemStack item : legacy.getIgnoredItems()) {
            if (item != null && itemsIdentity.add(item)) {
                effectiveIgnoredItems.add(item);
            }
        }

        List<Block> effectiveIgnoredBlocks = new ArrayList<>();
        Set<Block> blocksIdentity = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Block block : ignoredBlockList) {
            if (block != null && blocksIdentity.add(block)) {
                effectiveIgnoredBlocks.add(block);
            }
        }

        for (Block block : modern.getIgnoredBlocks()) {
            if (block != null && blocksIdentity.add(block)) {
                effectiveIgnoredBlocks.add(block);
            }
        }
        for (Block block : legacy.getIgnoredBlocks()) {
            if (block != null && blocksIdentity.add(block)) {
                effectiveIgnoredBlocks.add(block);
            }
        }

        boolean cancelled = pre.isCancelled() || modern.isCancelled() || legacy.isCancelled();
        boolean addon     = pre.isAddon()     || modern.isAddon()     || legacy.isAddon();

        if (addon) {
            plugin.debugMessage("GraveCreateEvent is handled by addon. Developers should handle it here. If not, this will do absolutely nothing at all.", 1);
            return;
        }

        if (!cancelled) {
            grave.setOwnerType(modern.getOwnerType());
            grave.setOwnerName(modern.getOwnerName());
            grave.setOwnerNameDisplay(modern.getOwnerNameDisplay());
            grave.setOwnerUUID(modern.getOwnerUUID());
            grave.setPermissionList(modern.getPermissionList());
            grave.setYaw(modern.getYaw());
            grave.setPitch(modern.getPitch());
            grave.setTimeAlive(modern.getTimeAlive());
            grave.setOwnerTexture(modern.getOwnerTexture());
            grave.setOwnerTextureSignature(modern.getOwnerTextureSignature());
            grave.setExperience(modern.getExperience());
            grave.setKillerName(modern.getKillerName());
            grave.setKillerNameDisplay(modern.getKillerNameDisplay());
            grave.setKillerUUID(modern.getKillerUUID());
            grave.setKillerType(modern.getKillerType());
            grave.setProtection(modern.getProtection());
            grave.setTimeProtection(modern.getTimeProtection());
            grave.setLocationDeath(modern.getLocationDeath());

            Location deathLoc = Objects.requireNonNull(modern.getLocationDeath());
            World world = deathLoc.getWorld();

            boolean allowNetherRoof = plugin.getConfigManager().getConfigSection("placement.nether-roof", grave).getBoolean("placement.nether-roof");
            if ((world != null && deathLoc.getY() < world.getMinHeight())
                    || (world != null && deathLoc.getY() > world.getMaxHeight())
                    || !allowNetherRoof && (world != null && world.getEnvironment() == World.Environment.NETHER && plugin.getSafeLocationManager().isAboveNetherRoof(grave.getLocationDeath(), grave))) {
                handleFailedGravePlacement(event, grave, deathLoc, livingEntity, removedItemStackList, graveItemStackList);
                return;
            }

            List<String> effectivePerms = modern.getPermissionList().isEmpty() ? permissionList : new ArrayList<>(modern.getPermissionList());

            Location placedLocation = placeGrave(
                    event, grave, graveItemStackList, removedItemStackList, deathLoc,
                    livingEntity, effectivePerms, player
            );

            plugin.getServer().getPluginManager().callEvent(
                    new GravePostCreateEvent(livingEntity, grave, placedLocation)
            );

            if (!effectiveIgnoredItems.isEmpty() || !effectiveIgnoredBlocks.isEmpty()) {
                dropIgnored(livingEntity, deathLoc, event, effectiveIgnoredItems, effectiveIgnoredBlocks);
            }
            return;
        }

        List<ItemStack> toReturn = new ArrayList<>();
        Set<ItemStack> identitySet = Collections.newSetFromMap(new IdentityHashMap<>());

        if (graveItemStackList != null) {
            for (ItemStack item : graveItemStackList) {
                if (item == null || item.getType() == Material.AIR) continue;
                if (identitySet.add(item)) {
                    toReturn.add(item);
                }
            }
        }

        for (ItemStack item : effectiveIgnoredItems) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (identitySet.add(item)) {
                toReturn.add(item);
            }
        }

        if (!toReturn.isEmpty()) {
            event.getDrops().addAll(toReturn);
        }

        if (!effectiveIgnoredBlocks.isEmpty()) {
            Location dropLoc = (location != null) ? location : livingEntity.getLocation();
            World world = dropLoc.getWorld();

            if (world != null) {
                for (Block block : effectiveIgnoredBlocks) {
                    if (block == null) continue;

                    Location blockLoc = block.getLocation();
                    for (ItemStack drop : block.getDrops()) {
                        if (drop != null && drop.getType() != Material.AIR) {
                            block.getWorld().dropItemNaturally(blockLoc, drop);
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets up the basic properties of the grave.
     *
     * @param graveCreateEvent The Grave Create Event that is set up.
     * @param grave         The grave to set up.
     * @param livingEntity  The entity that died.
     * @param entityName    The name of the entity.
     * @param permissionList The list of permissions.
     */
    private void setupGrave(GraveCreateEvent graveCreateEvent, Grave grave, LivingEntity livingEntity, String entityName, List<String> permissionList) {
        graveCreateEvent.setOwnerType(livingEntity.getType());
        graveCreateEvent.setOwnerName(entityName);
        graveCreateEvent.setOwnerNameDisplay(livingEntity instanceof Player p ? p.getDisplayName() : entityName);
        graveCreateEvent.setOwnerUUID(livingEntity.getUniqueId());
        graveCreateEvent.setPermissionList(permissionList);
        graveCreateEvent.setYaw(livingEntity.getLocation().getYaw());
        graveCreateEvent.setPitch(livingEntity.getLocation().getPitch());
        graveCreateEvent.setTimeAlive(plugin.getConfigManager().getConfigSection("grave.time", grave).getInt("grave.time") * 1000L);
        if (!plugin.getVersionManager().is_v1_7()) {
            if (plugin.getVersionManager().isPost1_21_9()) {
                graveCreateEvent.setOwnerTexture(SkinTextureUtil_post_1_21_9.getTexture(livingEntity));
            } else {
                graveCreateEvent.setOwnerTexture(SkinTextureUtil.getTexture(livingEntity));
            }
            graveCreateEvent.setOwnerTextureSignature(SkinSignatureUtil.getSignature(livingEntity));
        }
    }

    /**
     * Sets the experience for the grave.
     *
     * @param graveCreateEvent The Grave Create Event that is set up.
     * @param grave        The grave to set the experience for.
     * @param event        The entity death event.
     * @param livingEntity The entity that died.
     */
    private void setGraveExperience(GraveCreateEvent graveCreateEvent, Grave grave, EntityDeathEvent event, LivingEntity livingEntity, PlayerDeathEvent pde) {
        float pct = (float) plugin.getConfigManager().getConfigSection("experience.store", grave).getDouble("experience.store");
        boolean storeExp = plugin.getConfigManager().getConfigSection("experience.store-in-grave", grave).getBoolean("experience.store-in-grave", true);
        plugin.debugMessage("Experience Percentage for " + grave.getUUID() + ": " + pct, 2);
        int vanillaDrop = event.getDroppedExp();
        event.setDroppedExp(0);

        if (livingEntity instanceof Player p) {
            Plugin essentials = plugin.getServer().getPluginManager().getPlugin("Essentials");

            if (!storeExp) {
                if ((essentials != null && essentials.isEnabled()) && plugin.getPermissionManager().hasGrantedPermission("essentials.keepxp", p.getPlayer())) {
                    graveCreateEvent.setExperience(0);
                    String playerDisplay;
                    try {
                        playerDisplay = p.getPlayer().getDisplayName();
                    } catch (NullPointerException e) {
                        try {
                            playerDisplay = p.getPlayer().getName();
                        } catch (NullPointerException e2) {
                            playerDisplay = "Unknown";
                        }
                    }
                    pde.setNewExp(event.getDroppedExp());
                    graveCreateEvent.setExperience(0);
                    pde.setKeepLevel(true);
                    plugin.debugMessage("Set Dropped Experience not applied to " + grave.getUUID() + " because " + playerDisplay  + " has essentials.keepxp.", 2);
                } else if (pde.getKeepLevel() && !plugin.getPermissionManager().hasGrantedPermission("graves.keepinventory.bypass", p.getPlayer())) {
                    graveCreateEvent.setExperience(0);
                    String playerDisplay;
                    try {
                        playerDisplay = p.getPlayer().getDisplayName();
                    } catch (NullPointerException e) {
                        try {
                            playerDisplay = p.getPlayer().getName();
                        } catch (NullPointerException e2) {
                            playerDisplay = "Unknown";
                        }
                    }
                    pde.setNewExp(event.getDroppedExp());
                    graveCreateEvent.setExperience(0);
                    pde.setKeepLevel(true);
                    plugin.debugMessage("Set Dropped Experience not applied to " + grave.getUUID() + " because " + playerDisplay  + " has keep experience.", 2);
                } else if (pct >= 0 && plugin.getPermissionManager().hasGrantedPermission("graves.experience", p.getPlayer())) {
                    int total = ExperienceUtil.getPlayerExperience(p);
                    int stored = ExperienceUtil.getDropPercent(total, pct);
                    graveCreateEvent.setExperience(0);
                    pde.setDroppedExp(stored);
                    plugin.debugMessage("Set Dropped Experience for player grave " + grave.getUUID() + ": " + stored, 1);
                    pde.setKeepLevel(false);
                } else {
                    graveCreateEvent.setExperience(0);
                    pde.setDroppedExp(vanillaDrop);
                    pde.setKeepLevel(false);
                    plugin.debugMessage("Set Dropped Experience for player grave " + grave.getUUID() + ": " + vanillaDrop, 1);
                }
            } else {
                if ((essentials != null && essentials.isEnabled()) && plugin.getPermissionManager().hasGrantedPermission("essentials.keepxp", p.getPlayer())) {
                    graveCreateEvent.setExperience(0);
                    String playerDisplay;
                    try {
                        playerDisplay = p.getPlayer().getDisplayName();
                    } catch (NullPointerException e) {
                        try {
                            playerDisplay = p.getPlayer().getName();
                        } catch (NullPointerException e2) {
                            playerDisplay = "Unknown";
                        }
                    }
                    pde.setNewExp(event.getDroppedExp());
                    graveCreateEvent.setExperience(0);
                    pde.setKeepLevel(true);
                    plugin.debugMessage("Set Grave Experience not applied to " + grave.getUUID() + " because " + playerDisplay  + " has essentials.keepxp.", 2);
                } else if (pde.getKeepLevel() && !plugin.getPermissionManager().hasGrantedPermission("graves.keepinventory.bypass", p.getPlayer())) {
                    graveCreateEvent.setExperience(0);
                    String playerDisplay;
                    try {
                        playerDisplay = p.getPlayer().getDisplayName();
                    } catch (NullPointerException e) {
                        try {
                            playerDisplay = p.getPlayer().getName();
                        } catch (NullPointerException e2) {
                            playerDisplay = "Unknown";
                        }
                    }
                    pde.setNewExp(event.getDroppedExp());
                    graveCreateEvent.setExperience(0);
                    pde.setKeepLevel(true);
                    plugin.debugMessage("Set Grave Experience not applied to " + grave.getUUID() + " because " + playerDisplay  + " has keep experience.", 2);
                } else if (pct >= 0 && plugin.getPermissionManager().hasGrantedPermission("graves.experience", p.getPlayer())) {
                    int total = ExperienceUtil.getPlayerExperience(p);
                    int stored = ExperienceUtil.getDropPercent(total, pct);
                    graveCreateEvent.setExperience(stored);
                    plugin.debugMessage("Set Grave Experience for player grave " + grave.getUUID() + ": " + stored, 1);
                    pde.setKeepLevel(false);
                } else {
                    graveCreateEvent.setExperience(vanillaDrop);
                    pde.setKeepLevel(false);
                    plugin.debugMessage("Set Grave Experience for player grave " + grave.getUUID() + ": " + vanillaDrop, 1);
                }
            }
        } else {
            if (!storeExp) {
                if (pct >= 0) {
                    int stored = ExperienceUtil.getDropPercent(vanillaDrop, pct);
                    graveCreateEvent.setExperience(0);
                    pde.setDroppedExp(stored);
                    plugin.debugMessage("Set Dropped Experience for non player grave " + grave.getUUID() + ": " + stored, 1);
                } else {
                    graveCreateEvent.setExperience(0);
                    pde.setDroppedExp(vanillaDrop);
                    plugin.debugMessage("Set Dropped Experience for default grave " + grave.getUUID() + ": " + vanillaDrop, 1);
                }
            } else {
                if (pct >= 0) {
                    int stored = ExperienceUtil.getDropPercent(vanillaDrop, pct);
                    graveCreateEvent.setExperience(stored);
                    plugin.debugMessage("Set Grave Experience for non player grave " + grave.getUUID() + ": " + stored, 1);
                } else {
                    graveCreateEvent.setExperience(vanillaDrop);
                    plugin.debugMessage("Set Grave Experience for default grave " + grave.getUUID() + ": " + vanillaDrop, 1);
                }
            }
        }
    }

    /**
     * Sets up the killer details for the grave.
     *
     * @param graveCreateEvent The Grave Create Event that is set up.
     * @param grave        The grave to set up.
     * @param livingEntity The entity that died.
     */
    private void setupGraveKiller(GraveCreateEvent graveCreateEvent, Grave grave, LivingEntity livingEntity) {
        if (livingEntity.getKiller() != null) {
            graveCreateEvent.setKillerType(EntityType.PLAYER);
            graveCreateEvent.setKillerName(livingEntity.getKiller().getName());
            graveCreateEvent.setKillerNameDisplay(livingEntity.getKiller().getDisplayName());
            graveCreateEvent.setKillerUUID(livingEntity.getKiller().getUniqueId());
        } else if (livingEntity.getLastDamageCause() != null) {
            EntityDamageEvent e = livingEntity.getLastDamageCause();
            if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && e instanceof EntityDamageByEntityEvent by) {
                graveCreateEvent.setKillerUUID(by.getDamager().getUniqueId());
                graveCreateEvent.setKillerType(by.getDamager().getType());
                graveCreateEvent.setKillerName(plugin.getEntityManager().getEntityName(by.getDamager()));
            } else {
                graveCreateEvent.setKillerUUID(null);
                graveCreateEvent.setKillerType(null);
                graveCreateEvent.setKillerName(plugin.getGraveManager().getDamageReason(e.getCause(), grave));
            }
            graveCreateEvent.setKillerNameDisplay(graveCreateEvent.getKillerName());
        }
    }

    /**
     * Sets up the protection details for the grave.
     *
     * @param graveCreateEvent The Grave Create Event that is set up.
     * @param livingEntity The entity that died.
     * @param grave The grave to set up.
     */
    private void setupGraveProtection(GraveCreateEvent graveCreateEvent, LivingEntity livingEntity, Grave grave) {
        if (plugin.getConfigManager().getConfigSection("protection.enabled", grave).getBoolean("protection.enabled")) {
            GraveProtectionCreateEvent modern = new GraveProtectionCreateEvent(livingEntity, grave);
            plugin.getServer().getPluginManager().callEvent(modern);

            com.ranull.graves.event.GraveProtectionCreateEvent legacy =
                    new com.ranull.graves.event.GraveProtectionCreateEvent(livingEntity, grave);
            plugin.getServer().getPluginManager().callEvent(legacy);

            if (!(modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon())) {
                graveCreateEvent.setProtection(true);
                graveCreateEvent.setTimeProtection(plugin.getConfigManager().getConfigSection("protection.time", grave).getInt("protection.time") * 1000L);
            }
        }
    }

    /**
     * Places the grave at the specified location.
     *
     * @param event                The entity death event.
     * @param grave                The grave to place.
     * @param graveItemStackList   The list of item stacks for the grave.
     * @param removedItemStackList The list of removed item stacks.
     * @param location             The location to place the grave.
     * @param livingEntity         The entity that died.
     * @param permissionList       The list of permissions.
     * @return if placement is listed or null
     */
    private Location placeGrave(EntityDeathEvent event,
                                Grave grave,
                                List<ItemStack> graveItemStackList,
                                List<ItemStack> removedItemStackList,
                                Location location,
                                LivingEntity livingEntity,
                                List<String> permissionList,
                                Player player) {

        Map<Location, BlockData.BlockType> locationMap = new HashMap<>();

        grave.getLocationDeath().setYaw(grave.getYaw());
        grave.getLocationDeath().setPitch(grave.getPitch());

        locationMap.put(grave.getLocationDeath(), BlockData.BlockType.DEATH);

        setupObituary(grave, graveItemStackList, livingEntity, location);
        setupSkull(grave, graveItemStackList, livingEntity, location);

        grave.setInventory(plugin.getGraveManager().getGraveInventory(grave, livingEntity, graveItemStackList, removedItemStackList, permissionList));
        grave.setEquipmentMap(!plugin.getVersionManager().is_v1_7()
                ? plugin.getEntityManager().getEquipmentMap(livingEntity, grave)
                : new HashMap<>());

        if (!locationMap.isEmpty()) {
            notifyGraveCreation(event, grave, locationMap, livingEntity, permissionList, player,
                    location, removedItemStackList, graveItemStackList);
            return location;
        } else {
            handleFailedGravePlacement(event, grave, location, livingEntity, removedItemStackList, graveItemStackList);
            return null;
        }
    }

    /**
     * Sets up the obituary item for the grave.
     *
     * @param grave              The grave to set up.
     * @param graveItemStackList The list of item stacks for the grave.
     */
    private void setupObituary(Grave grave, List<ItemStack> graveItemStackList, LivingEntity livingEntity, Location location) {
        if (plugin.getConfigManager().getConfigSection("obituary.enabled", grave).getBoolean("obituary.enabled")) {
            double pct = plugin.getConfigManager().getConfigSection("obituary.percent", grave).getDouble("obituary.percent");
            boolean drop = plugin.getConfigManager().getConfigSection("obituary.drop", grave).getBoolean("obituary.drop");

            if (Math.random() <= pct) {
                GraveObituaryAddEvent modern = new GraveObituaryAddEvent(grave, location, livingEntity);
                plugin.getServer().getPluginManager().callEvent(modern);

                com.ranull.graves.event.GraveObituaryAddEvent legacy =
                        new com.ranull.graves.event.GraveObituaryAddEvent(grave, location, livingEntity);
                plugin.getServer().getPluginManager().callEvent(legacy);

                if (!(modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon())) {
                    if (drop) {
                        if (location.getWorld() != null) {
                            location.getWorld().dropItemNaturally(location, plugin.getItemStackManager().getGraveObituary(grave));
                            plugin.debugMessage(
                                    "Obituary dropped at location x: " + location.getBlockX()
                                            + " y: " + location.getBlockY()
                                            + " z: " + location.getBlockZ() + ".", 2);
                        } else {
                            Block b = location.getBlock();
                            plugin.debugMessage(
                                    "World not found. Obituary added to " + grave.getOwnerName()
                                            + "'s Grave at location x: " + b.getX()
                                            + " y: " + b.getY()
                                            + " z: " + b.getZ() + ".", 2);
                            graveItemStackList.add(plugin.getItemStackManager().getGraveObituary(grave));
                        }
                    } else {
                        Block b = location.getBlock();
                        plugin.debugMessage(
                                "Obituary added to " + grave.getOwnerName()
                                        + "'s Grave at location x: " + b.getX()
                                        + " y: " + b.getY()
                                        + " z: " + b.getZ() + ".", 2);
                        graveItemStackList.add(plugin.getItemStackManager().getGraveObituary(grave));
                    }
                }
            }
        }
    }

    /**
     * Sets up the skull item for the grave.
     *
     * @param grave              The grave to set up.
     * @param graveItemStackList The list of item stacks for the grave.
     */
    private void setupSkull(Grave grave, List<ItemStack> graveItemStackList, LivingEntity livingEntity, Location location) {
        if (plugin.getConfigManager().getConfigSection("head.enabled", grave).getBoolean("head.enabled")
                && Math.random() < plugin.getConfigManager().getConfigSection("head.percent", grave).getDouble("head.percent")
                && grave.getOwnerTexture() != null
                && grave.getOwnerTextureSignature() != null) {

            boolean drop = plugin.getConfigManager().getConfigSection("head.drop", grave).getBoolean("head.drop");

            GravePlayerHeadDropEvent modern = new GravePlayerHeadDropEvent(grave, location, livingEntity);
            plugin.getServer().getPluginManager().callEvent(modern);

            com.ranull.graves.event.GravePlayerHeadDropEvent legacy =
                    new com.ranull.graves.event.GravePlayerHeadDropEvent(grave, location, livingEntity);
            plugin.getServer().getPluginManager().callEvent(legacy);

            if (!(modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon())) {
                if (drop) {
                    if (location.getWorld() != null) {
                        ItemStack headItem = plugin.getItemStackManager().getGraveHead(grave);
                        location.getWorld().dropItemNaturally(location, headItem);
                        plugin.debugMessage("Player Head dropped at location x: " + location.getBlockX()
                                + ", y: " + location.getBlockY()
                                + ", z: " + location.getBlockZ() + ".", 2);
                    } else {
                        final Location graveLoc = grave.getLocationDeath();
                        plugin.debugMessage("World not found. Player Head added to " + livingEntity.getName()
                                + "'s grave at location x: " + graveLoc.getBlockX()
                                + ", y: " + graveLoc.getBlockY()
                                + ", z: " + graveLoc.getBlockZ() + ".", 2);
                        graveItemStackList.add(plugin.getItemStackManager().getGraveHead(grave));
                    }
                } else {
                    Location graveLoc = grave.getLocationDeath();
                    plugin.debugMessage("Player Head added to " + livingEntity.getName()
                            + "'s grave at location x: " + graveLoc.getBlockX()
                            + ", y: " + graveLoc.getBlockY()
                            + ", z: " + graveLoc.getBlockZ() + ".", 2);
                    graveItemStackList.add(plugin.getItemStackManager().getGraveHead(grave));
                }
            }
        }
    }

    /**
     * Notifies the creation of the grave and places the grave blocks.
     *
     * @param event               The entity death event.
     * @param grave               The grave that was created.
     * @param locationMap         The map of locations for the grave.
     * @param livingEntity        The entity that died.
     * @param permissionList      The list of permissions.
     */
    private void notifyGraveCreation(EntityDeathEvent event, Grave grave, Map<Location, BlockData.BlockType> locationMap, LivingEntity livingEntity, List<String> permissionList, Player player, Location fallbackLocation, List<ItemStack> removedItemStackList, List<ItemStack> graveItemStackList) {
        plugin.getEntityManager().sendMessage("message.death", livingEntity, grave.getLocationDeath(), grave);
        plugin.getEntityManager().runCommands("event.command.create", livingEntity, grave.getLocationDeath(), grave);
        plugin.getDataManager().addGrave(grave);

        if (player != null && plugin.getConfigManager().getConfigSection("grave.smite-death-location", grave).getBoolean("grave.smite-death-location", true)) {
            World world = player.getWorld();
            if (world != null) {
                switch (world.getEnvironment()) {
                    case NORMAL:
                        if (plugin.getConfigManager().getConfigSection("grave.actually-smite-death-location", grave).getBoolean("grave.actually-smite-death-location", false)) {
                            world.strikeLightning(player.getLocation());
                        } else {
                            world.strikeLightningEffect(player.getLocation());
                        }
                        break;
                    case CUSTOM:
                        Boolean weatherCycle = (Boolean) world.getGameRuleValue(Objects.requireNonNull(GameRule.getByName("do_weather_cycle")));
                        if (Boolean.TRUE.equals(weatherCycle)) {
                            if (plugin.getConfigManager().getConfigSection("grave.actually-smite-death-location", grave).getBoolean("grave.actually-smite-death-location", false)) {
                                world.strikeLightning(player.getLocation());
                            } else {
                                world.strikeLightningEffect(player.getLocation());
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        if (player != null
                && plugin.getConfigManager().getConfigSection("noteblockapi.enabled", grave).getBoolean("noteblockapi.enabled")
                && plugin.getIntegrationManager().hasNoteBlockAPI()) {

            String cause = event.getEntity().getLastDamageCause() != null
                    ? event.getEntity().getLastDamageCause().getCause().name()
                    : "UNKNOWN";
            String nbsSound = null;
            List<String> map = plugin.getConfigManager().getConfigSection("noteblockapi.death-causes", grave).getStringList("noteblockapi.death-causes");
            for (String s : map) {
                String[] parts = s.split(": ");
                if (parts.length == 2 && parts[0].equalsIgnoreCase(cause)) {
                    nbsSound = parts[1].trim();
                    break;
                }
            }
            if (nbsSound == null) {
                nbsSound = plugin.getConfigManager().getConfigSection("noteblockapi.nbs-sound", grave).getString("noteblockapi.nbs-sound");
            }

            if (plugin.getConfigManager().getConfigSection("noteblockapi.play-locally", grave).getBoolean("noteblockapi.play-locally")) {
                plugin.getIntegrationManager().getNoteBlockAPI().playSongForPlayer(player, nbsSound);
            } else {
                plugin.getIntegrationManager().getNoteBlockAPI().playSongForAllPlayers(nbsSound);
            }
        } else if (player != null) {
            plugin.getEntityManager().playPlayerSound("sound.grave-create", player, grave);
        }

        if (plugin.getIntegrationManager().hasMultiPaper()) {
            plugin.getIntegrationManager().getMultiPaper().notifyGraveCreation(grave);
        }

        boolean placed = placeGraveBlocks(grave, locationMap, livingEntity);

        if (!placed) {
            handleFailedGravePlacement(event, grave, fallbackLocation, livingEntity, removedItemStackList, graveItemStackList);
        }
    }

    /**
     * Places the grave blocks at the specified locations.
     *
     * @param grave        The grave to place.
     * @param locationMap  The map of locations for the grave.
     * @param livingEntity The entity that died.
     */
    private boolean placeGraveBlocks(Grave grave, Map<Location, BlockData.BlockType> locationMap, LivingEntity livingEntity) {
        boolean placed = false;

        for (Map.Entry<Location, BlockData.BlockType> entry : locationMap.entrySet()) {
            Location base = entry.getKey().clone();

            int dx = 0, dy = 0, dz = 0;
            if (entry.getValue() == BlockData.BlockType.NORMAL) {
                dx = plugin.getConfigManager().getConfigSection("placement.offset.x", grave).getInt("placement.offset.x");
                dy = plugin.getConfigManager().getConfigSection("placement.offset.y", grave).getInt("placement.offset.y");
                dz = plugin.getConfigManager().getConfigSection("placement.offset.z", grave).getInt("placement.offset.z");
            }
            Location loc = base.add(dx, dy, dz);

            GraveBlockPlaceEvent modern = new GraveBlockPlaceEvent(grave, loc, entry.getValue(), entry.getKey().getBlock(), livingEntity);
            plugin.getServer().getPluginManager().callEvent(modern);

            com.ranull.graves.event.GraveBlockPlaceEvent legacy =
                    new com.ranull.graves.event.GraveBlockPlaceEvent(grave, loc, entry.getValue(), entry.getKey().getBlock(), livingEntity);
            plugin.getServer().getPluginManager().callEvent(legacy);

            if (modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon()) {
                continue;
            }

            Location effectiveLoc = modern.hasLocation() ? modern.getLocation()
                    : legacy.hasLocation() ? legacy.getLocation()
                    : loc;

            plugin.getGraveManager().placeGrave(effectiveLoc, grave);
            plugin.getEntityManager().sendMessage("message.block", livingEntity, effectiveLoc, grave);
            plugin.getEntityManager().runCommands("event.command.block", livingEntity, effectiveLoc, grave);

            placed = true;
        }

        return placed;
    }

    /**
     * Handles failed grave placement.
     *
     * @param event                The entity death event.
     * @param grave                The grave that failed to be placed.
     * @param location             The location where the grave was to be placed.
     * @param livingEntity         The entity that died.
     * @param removedItemStackList The removed item stack list
     */
    private void handleFailedGravePlacement(EntityDeathEvent event, Grave grave, Location location, LivingEntity livingEntity, List<ItemStack> removedItemStackList, List<ItemStack> graveItemStackList) {
        if (event instanceof PlayerDeathEvent pde
                && plugin.getConfigManager().getConfigSection("placement.failure-keep-inventory", grave).getBoolean("placement.failure-keep-inventory")) {
            try {
                pde.setKeepLevel(true);
                pde.setKeepInventory(true);
                plugin.getEntityManager().sendMessage("message.failure-keep-inventory", livingEntity, location, grave);
            } catch (NoSuchMethodError ignored) {}
        } else {
            plugin.getEntityManager().sendMessage("message.failure", livingEntity, location, grave);
            if (removedItemStackList != null) {
                event.getDrops().addAll(removedItemStackList);
            }
            if (graveItemStackList != null) {
                event.getDrops().addAll(graveItemStackList);
            }
        }
    }

    /**
     * Drops ignored items and blocks regardless of grave placement outcome.
     *
     * @param livingEntity     The entity that died.
     * @param fallbackLocation Fallback location to drop at if needed.
     * @param event            The death event (used when world is null).
     * @param ignoredItems     Items that were ignored for the grave.
     * @param ignoredBlocks    Blocks that were ignored for the grave.
     */
    private void dropIgnored(LivingEntity livingEntity,
                             Location fallbackLocation,
                             EntityDeathEvent event,
                             List<ItemStack> ignoredItems,
                             List<Block> ignoredBlocks) {

        if ((ignoredItems == null || ignoredItems.isEmpty())
                && (ignoredBlocks == null || ignoredBlocks.isEmpty())) {
            return;
        }

        Location dropLoc = (fallbackLocation != null) ? fallbackLocation : livingEntity.getLocation();
        World world = dropLoc.getWorld();

        if (world != null) {
            if (ignoredItems != null) {
                for (ItemStack item : ignoredItems) {
                    if (item != null && item.getType() != Material.AIR) {
                        world.dropItemNaturally(dropLoc, item);
                    }
                }
            }

            if (ignoredBlocks != null) {
                for (Block block : ignoredBlocks) {
                    if (block == null) continue;

                    Location blockLoc = block.getLocation();
                    for (ItemStack drop : block.getDrops()) {
                        if (drop != null && drop.getType() != Material.AIR) {
                            block.getWorld().dropItemNaturally(blockLoc, drop);
                        }
                    }
                }
            }
        } else {
            if (ignoredItems != null && !ignoredItems.isEmpty()) {
                event.getDrops().addAll(ignoredItems);
            }
        }
    }
}