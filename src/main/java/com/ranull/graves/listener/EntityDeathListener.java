package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.compatibility.CompatibilityInventoryView;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.ExperienceUtil;
import com.ranull.graves.util.LocationUtil;
import dev.cwhead.GravesX.event.GraveBlockPlaceEvent;
import dev.cwhead.GravesX.event.GraveCreateEvent;
import dev.cwhead.GravesX.event.GraveObituaryAddEvent;
import dev.cwhead.GravesX.event.GravePlayerHeadDropEvent;
import dev.cwhead.GravesX.event.GraveProtectionCreateEvent;
import dev.cwhead.GravesX.util.SkinTextureUtil_post_1_21_9;
import me.jay.GravesX.util.SkinSignatureUtil;
import me.jay.GravesX.util.SkinTextureUtil;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

        final List<String> permissionList = isPlayer ? plugin.getPermissionList(livingEntity) : null;

        if (!isPlayer && livingEntity instanceof Zombie zombie) {
            if (isConfiguredZombieType(zombie) && hasGravesXMetadata(zombie)) {
                removePlayerSkullFromDrops(zombie, event);
            }
        }

        if (!isEnabledGrave(livingEntity, permissionList, entityName)) return;

        final List<String> worldList = plugin.getConfig("world", livingEntity, permissionList).getStringList("world");
        if (!isValidWorld(worldList, livingEntity, entityName)) return;

        if (!isValidDamageCause(livingEntity, permissionList, entityName)) return;

        if (!isPlayer && isInvalidCreatureSpawn(livingEntity, null, entityName)) return;

        if (isPlayer) {
            if (handlePlayerDeath(player, entityName)) return;

            if (pde != null && isKeepInventory(pde, entityName) &&
                    !plugin.hasGrantedPermission("graves.keepinventory.bypass", player)) {
                return;
            }

            if (pde != null && event.getDrops().isEmpty()) {
                plugin.debugMessage("Grave not created for " + entityName + " because they had an empty inventory", 2);
                return;
            }
        }

        if (!hasValidToken(livingEntity, permissionList, entityName, event.getDrops())) return;

        final List<ItemStack> graveItemStackList = getGraveItemStackList(event, livingEntity, permissionList);
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

        createGrave(event, livingEntity, entityName, permissionList, removedItemStackList, graveItemStackList, location, pde, player);
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
            if (plugin.hasGrantedPermission("grave.max.limit.unlimited", player)) return Integer.MAX_VALUE;
            if (plugin.hasGrantedPermission("grave.max.limit." + i, player)) maxGraves = i;
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
     * @param event the EntityDeathEvent containing the drops
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
            event.getDrops().clear();
            event.setDroppedExp(0);
            return true;
        }
        return false;
    }

    /**
     * Checks if the entity is an invalid grave zombie.
     *
     * @param event       The entity death event.
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
            if (!plugin.getConfig("zombie.drop", type, perms).getBoolean("zombie.drop")) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }
            return true;
        }
        return false;
    }

    /**
     * Handles player death and checks if a grave should be created.
     *
     * @param player      The player who died.
     * @param entityName  The name of the player.
     * @return True if a grave should not be created, false otherwise.
     */
    private boolean handlePlayerDeath(Player player, String entityName) {
        if (!plugin.hasGrantedPermission("graves.place", player)) {
            plugin.debugMessage("Grave not created for " + entityName + " because they don't have permission to place graves", 2);
            return true;
        } else if (plugin.hasGrantedPermission("essentials.keepinv", player)) {
            plugin.debugMessage(entityName + " has essentials.keepinv", 2);
        }
        return false;
    }

    /**
     * Checks if graves are enabled for the specified entity.
     *
     * @param livingEntity The entity to check.
     * @param permissionList The list of permissions.
     * @param entityName    The name of the entity.
     * @return True if graves are enabled, false otherwise.
     */
    private boolean isEnabledGrave(LivingEntity livingEntity, List<String> permissionList, String entityName) {
        if (!plugin.getConfig("grave.enabled", livingEntity, permissionList).getBoolean("grave.enabled")) {
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
            if (event.getKeepInventory() && !plugin.hasGrantedPermission("graves.keepinventory.bypass", event.getEntity())) {
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
            List<String> reasons = plugin.getConfig("spawn.reason", livingEntity, permissionList).getStringList("spawn.reason");
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
            List<String> allowed = plugin.getConfig("death.reason", livingEntity, permissionList).getStringList("death.reason");

            boolean ok =
                    allowed.contains("ALL")
                            || allowed.contains(cause.name())
                            || (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                            && ((livingEntity.getKiller() != null
                            && plugin.getConfig("death.player", livingEntity, permissionList).getBoolean("death.player"))
                            || (livingEntity.getKiller() == null
                            && plugin.getConfig("death.entity", livingEntity, permissionList).getBoolean("death.entity"))))
                            || (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                            && plugin.getConfig("death.environmental", livingEntity, permissionList).getBoolean("death.environmental"));

            if (!ok) {
                plugin.debugMessage("Grave not created for " + entityName + " because they died to an invalid damage cause", 2);
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the entity has a valid grave token.
     *
     * @param livingEntity  The entity to check.
     * @param permissionList The list of permissions.
     * @param entityName    The name of the entity.
     * @param drops         The list of item drops.
     * @return True if the entity has a valid grave token, false otherwise.
     */
    private boolean hasValidToken(LivingEntity livingEntity, List<String> permissionList, String entityName, List<ItemStack> drops) {
        if (plugin.getVersionManager().hasPersistentData()
                && plugin.getConfig("token.enabled", livingEntity, permissionList).getBoolean("token.enabled")) {
            String name = plugin.getConfig("token.name", livingEntity).getString("token.name", "basic");
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
     * @param event         The entity death event.
     * @param livingEntity  The entity that died.
     * @param permissionList The list of permissions.
     * @return The list of item stacks for the grave.
     */
    private List<ItemStack> getGraveItemStackList(EntityDeathEvent event, LivingEntity livingEntity, List<String> permissionList) {
        final List<ItemStack> graveList = new ArrayList<>();

        try {
            final ListIterator<ItemStack> it = event.getDrops().listIterator();
            while (it.hasNext()) {
                final ItemStack item = it.next();
                if (item == null || item.getType() == Material.AIR) continue;

                final UUID graveId = plugin.getEntityManager().getGraveUUIDFromItemStack(item);
                if (graveId != null) {
                    if (plugin.getConfig("compass.destroy", livingEntity, permissionList).getBoolean("compass.destroy")) {
                        it.remove();
                    } else if (!plugin.getConfig("compass.ignore", livingEntity, permissionList).getBoolean("compass.ignore")) {
                        graveList.add(item);
                        it.remove();
                    }
                    continue;
                }

                if (!plugin.getGraveManager().shouldIgnoreItemStack(item, livingEntity, permissionList)) {
                    graveList.add(item);
                    it.remove();
                }
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {}

        return graveList;
    }

    /**
     * Creates a grave for the specified entity.
     *
     * @param event              The entity death event.
     * @param livingEntity       The entity that died.
     * @param entityName         The name of the entity.
     * @param permissionList     The list of permissions.
     * @param removedItemStackList The list of removed item stacks.
     * @param graveItemStackList The list of item stacks for the grave.
     * @param location           The location of the grave.
     */
    private void createGrave(EntityDeathEvent event,
                             LivingEntity livingEntity,
                             String entityName,
                             List<String> permissionList,
                             List<ItemStack> removedItemStackList,
                             List<ItemStack> graveItemStackList,
                             Location location,
                             PlayerDeathEvent pde,
                             Player player) {

        if (player != null) {
            int serverMax = plugin.getConfig("grave.max", livingEntity, permissionList).getInt("grave.max");
            int permsMax = getMaxGravesPermission(player);
            int applicableMax = (permsMax > 0) ? permsMax : serverMax;

            if (plugin.getGraveManager().getGraveList(livingEntity).size() >= applicableMax) {
                if (plugin.hasGrantedPermission("graves.max.replace", player)
                        && plugin.getConfig("grave.replace-oldest", livingEntity, permissionList).getBoolean("grave.replace-oldest")) {
                    plugin.getGraveManager().removeOldestGrave(livingEntity);
                    plugin.getEntityManager().sendMessage("message.grave-oldest-replaced", livingEntity, livingEntity.getLocation(), permissionList);
                    plugin.debugMessage("Grave replaced oldest for " + entityName + " because they reached maximum graves", 2);
                } else if (plugin.hasGrantedPermission("graves.max.bypass", player)) {
                    plugin.debugMessage("Grave created for " + entityName + " even though they reached the maximum graves cap", 2);
                } else {
                    plugin.getEntityManager().sendMessage("message.max", livingEntity, livingEntity.getLocation(), permissionList);
                    plugin.debugMessage("Grave not created for " + entityName + " because they reached maximum graves", 2);
                    return;
                }
            }
        }

        Grave grave = new Grave(UUID.randomUUID());
        setupGrave(grave, livingEntity, entityName, permissionList);
        setGraveExperience(grave, event, livingEntity, pde);
        setupGraveKiller(grave, livingEntity);
        setupGraveProtection(livingEntity, grave);

        GraveCreateEvent modern = new GraveCreateEvent(livingEntity, grave);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveCreateEvent legacy = new com.ranull.graves.event.GraveCreateEvent(livingEntity, grave);
        plugin.getServer().getPluginManager().callEvent(legacy);

        boolean cancelled = modern.isCancelled() || legacy.isCancelled();
        boolean addon     = modern.isAddon()     || legacy.isAddon();

        if (!cancelled && !addon) {
            placeGrave(event, grave, graveItemStackList, removedItemStackList, location,
                    livingEntity, permissionList, player);
        } else if (cancelled && !addon) {
            if (player != null) player.getInventory().clear();
        }
    }

    /**
     * Sets up the basic properties of the grave.
     *
     * @param grave         The grave to set up.
     * @param livingEntity  The entity that died.
     * @param entityName    The name of the entity.
     * @param permissionList The list of permissions.
     */
    private void setupGrave(Grave grave, LivingEntity livingEntity, String entityName, List<String> permissionList) {
        grave.setOwnerType(livingEntity.getType());
        grave.setOwnerName(entityName);
        grave.setOwnerNameDisplay(livingEntity instanceof Player p ? p.getDisplayName() : entityName);
        grave.setOwnerUUID(livingEntity.getUniqueId());
        grave.setPermissionList(permissionList);
        grave.setYaw(livingEntity.getLocation().getYaw());
        grave.setPitch(livingEntity.getLocation().getPitch());
        grave.setTimeAlive(plugin.getConfig("grave.time", grave).getInt("grave.time") * 1000L);
        if (!plugin.getVersionManager().is_v1_7()) {
            if (plugin.getVersionManager().isPost1_21_9()) {
                grave.setOwnerTexture(SkinTextureUtil_post_1_21_9.getTexture(livingEntity));
            } else {
                grave.setOwnerTexture(SkinTextureUtil.getTexture(livingEntity));
            }
            grave.setOwnerTextureSignature(SkinSignatureUtil.getSignature(livingEntity));
        }
    }

    /**
     * Sets the experience for the grave.
     *
     * @param grave        The grave to set the experience for.
     * @param event        The entity death event.
     * @param livingEntity The entity that died.
     */
    private void setGraveExperience(Grave grave, EntityDeathEvent event, LivingEntity livingEntity, PlayerDeathEvent pde) {
        float pct = (float) plugin.getConfig("experience.store", grave).getDouble("experience.store");
        pct = Math.max(0f, Math.min(1f, pct));
        plugin.debugMessage("Experience Percentage for " + grave.getUUID() + ": " + pct, 2);

        if (pct >= 0) {
            if (livingEntity instanceof Player p) {
                if (plugin.hasGrantedPermission("graves.experience", p)) {
                    int adjusted = ExperienceUtil.getDropPercent(ExperienceUtil.getPlayerExperience(p), pct);
                    grave.setExperience(adjusted);
                    plugin.debugMessage("Set Experience for player grave " + grave.getUUID() + ": " + adjusted, 2);
                } else {
                    grave.setExperience(event.getDroppedExp());
                    plugin.debugMessage("Set Experience for player grave " + grave.getUUID() + ": " + event.getDroppedExp(), 2);
                }
                if (pde != null) {
                    pde.setKeepLevel(false);
                }
            } else {
                int adjusted = ExperienceUtil.getDropPercent(event.getDroppedExp(), pct);
                grave.setExperience(adjusted);
                plugin.debugMessage("Set Experience for non player grave " + grave.getUUID() + ": " + adjusted, 2);
            }
        } else {
            grave.setExperience(event.getDroppedExp());
            plugin.debugMessage("Set Experience for default grave " + grave.getUUID() + ": " + event.getDroppedExp(), 2);
        }
    }

    /**
     * Sets up the killer details for the grave.
     *
     * @param grave        The grave to set up.
     * @param livingEntity The entity that died.
     */
    private void setupGraveKiller(Grave grave, LivingEntity livingEntity) {
        if (livingEntity.getKiller() != null) {
            grave.setKillerType(EntityType.PLAYER);
            grave.setKillerName(livingEntity.getKiller().getName());
            grave.setKillerNameDisplay(livingEntity.getKiller().getDisplayName());
            grave.setKillerUUID(livingEntity.getKiller().getUniqueId());
        } else if (livingEntity.getLastDamageCause() != null) {
            EntityDamageEvent e = livingEntity.getLastDamageCause();
            if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && e instanceof EntityDamageByEntityEvent by) {
                grave.setKillerUUID(by.getDamager().getUniqueId());
                grave.setKillerType(by.getDamager().getType());
                grave.setKillerName(plugin.getEntityManager().getEntityName(by.getDamager()));
            } else {
                grave.setKillerUUID(null);
                grave.setKillerType(null);
                grave.setKillerName(plugin.getGraveManager().getDamageReason(e.getCause(), grave));
            }
            grave.setKillerNameDisplay(grave.getKillerName());
        }
    }

    /**
     * Sets up the protection details for the grave.
     *
     * @param grave The grave to set up.
     */
    private void setupGraveProtection(LivingEntity livingEntity, Grave grave) {
        if (plugin.getConfig("protection.enabled", grave).getBoolean("protection.enabled")) {
            GraveProtectionCreateEvent modern = new GraveProtectionCreateEvent(livingEntity, grave);
            plugin.getServer().getPluginManager().callEvent(modern);

            com.ranull.graves.event.GraveProtectionCreateEvent legacy = new com.ranull.graves.event.GraveProtectionCreateEvent(livingEntity, grave);
            plugin.getServer().getPluginManager().callEvent(legacy);

            if (!(modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon())) {
                grave.setProtection(true);
                grave.setTimeProtection(plugin.getConfig("protection.time", grave).getInt("protection.time") * 1000L);
            }
        }
    }

    /**
     * Places the grave at the specified location.
     *
     * @param event               The entity death event.
     * @param grave               The grave to place.
     * @param graveItemStackList  The list of item stacks for the grave.
     * @param removedItemStackList The list of removed item stacks.
     * @param location            The location to place the grave.
     * @param livingEntity        The entity that died.
     * @param permissionList      The list of permissions.
     */
    private void placeGrave(EntityDeathEvent event,
                            Grave grave,
                            List<ItemStack> graveItemStackList,
                            List<ItemStack> removedItemStackList,
                            Location location,
                            LivingEntity livingEntity,
                            List<String> permissionList,
                            Player player) {

        Map<Location, BlockData.BlockType> locationMap = new HashMap<>();
        Location safeLocation = plugin.getLocationManager().getSafeGraveLocation(livingEntity, location, grave);

        event.getDrops().clear();
        event.setDroppedExp(0);

        grave.setLocationDeath(safeLocation != null ? safeLocation : location);
        grave.getLocationDeath().setYaw(grave.getYaw());
        grave.getLocationDeath().setPitch(grave.getPitch());

        locationMap.put(grave.getLocationDeath(), BlockData.BlockType.DEATH);

        setupObituary(grave, graveItemStackList, livingEntity, location);
        setupSkull(grave, graveItemStackList, livingEntity, location);

        grave.setInventory(plugin.getGraveManager().getGraveInventory(grave, livingEntity, graveItemStackList, removedItemStackList, permissionList));
        grave.setEquipmentMap(!plugin.getVersionManager().is_v1_7() ? plugin.getEntityManager().getEquipmentMap(livingEntity, grave) : new HashMap<>());

        if (!locationMap.isEmpty()) {
            notifyGraveCreation(event, grave, locationMap, livingEntity, permissionList, player);
        } else {
            handleFailedGravePlacement(event, grave, location, livingEntity);
        }
    }

    /**
     * Sets up the obituary item for the grave.
     *
     * @param grave               The grave to set up.
     * @param graveItemStackList  The list of item stacks for the grave.
     */
    private void setupObituary(Grave grave, List<ItemStack> graveItemStackList, LivingEntity livingEntity, Location location) {
        if (plugin.getConfig("obituary.enabled", grave).getBoolean("obituary.enabled")) {
            double pct = plugin.getConfig("obituary.percent", grave).getDouble("obituary.percent");
            boolean drop = plugin.getConfig("obituary.drop", grave).getBoolean("obituary.drop");
            pct = Math.max(0d, Math.min(1d, pct));

            if (Math.random() <= pct) {
                GraveObituaryAddEvent modern = new GraveObituaryAddEvent(grave, location, livingEntity);
                plugin.getServer().getPluginManager().callEvent(modern);

                com.ranull.graves.event.GraveObituaryAddEvent legacy = new com.ranull.graves.event.GraveObituaryAddEvent(grave, location, livingEntity);
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
     * @param grave               The grave to set up.
     * @param graveItemStackList  The list of item stacks for the grave.
     */
    private void setupSkull(Grave grave, List<ItemStack> graveItemStackList, LivingEntity livingEntity, Location location) {
        if (plugin.getConfig("head.enabled", grave).getBoolean("head.enabled")
                && Math.random() < plugin.getConfig("head.percent", grave).getDouble("head.percent")
                && grave.getOwnerTexture() != null
                && grave.getOwnerTextureSignature() != null) {

            boolean drop = plugin.getConfig("head.drop", grave).getBoolean("head.drop");

            GravePlayerHeadDropEvent modern = new GravePlayerHeadDropEvent(grave, location, livingEntity);
            plugin.getServer().getPluginManager().callEvent(modern);

            com.ranull.graves.event.GravePlayerHeadDropEvent legacy = new com.ranull.graves.event.GravePlayerHeadDropEvent(grave, location, livingEntity);
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
     * @param event              The entity death event.
     * @param grave              The grave that was created.
     * @param locationMap        The map of locations for the grave.
     * @param livingEntity       The entity that died.
     * @param permissionList     The list of permissions.
     */
    private void notifyGraveCreation(EntityDeathEvent event, Grave grave, Map<Location, BlockData.BlockType> locationMap,
                                     LivingEntity livingEntity, List<String> permissionList, Player player) {
        plugin.getEntityManager().sendMessage("message.death", livingEntity, grave.getLocationDeath(), grave);
        plugin.getEntityManager().runCommands("event.command.create", livingEntity, grave.getLocationDeath(), grave);
        plugin.getDataManager().addGrave(grave);

        if (player != null && plugin.getConfig("grave.smite-death-location", grave).getBoolean("grave.smite-death-location", true)) {
            World world = player.getWorld();
            if (world != null) {
                switch (world.getEnvironment()) {
                    case NORMAL:
                        if (plugin.getConfig("grave.actually-smite-death-location", grave).getBoolean("grave.actually-smite-death-location", false)) {
                            world.strikeLightning(player.getLocation());
                        } else {
                            world.strikeLightningEffect(player.getLocation());
                        }
                        break;
                    case CUSTOM:
                        Boolean weatherCycle = world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE);
                        if (Boolean.TRUE.equals(weatherCycle)) {
                            if (plugin.getConfig("grave.actually-smite-death-location", grave).getBoolean("grave.actually-smite-death-location", false)) {
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
                && plugin.getConfig("noteblockapi.enabled", grave).getBoolean("noteblockapi.enabled")
                && plugin.getIntegrationManager().hasNoteBlockAPI()) {

            String cause = event.getEntity().getLastDamageCause() != null
                    ? event.getEntity().getLastDamageCause().getCause().name()
                    : "UNKNOWN";
            String nbsSound = null;
            List<String> map = plugin.getConfig("noteblockapi.death-causes", grave).getStringList("noteblockapi.death-causes");
            for (String s : map) {
                String[] parts = s.split(": ");
                if (parts.length == 2 && parts[0].equalsIgnoreCase(cause)) {
                    nbsSound = parts[1].trim();
                    break;
                }
            }
            if (nbsSound == null) {
                nbsSound = plugin.getConfig("noteblockapi.nbs-sound", grave).getString("noteblockapi.nbs-sound");
            }

            if (plugin.getConfig("noteblockapi.play-locally", grave).getBoolean("noteblockapi.play-locally")) {
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

        placeGraveBlocks(grave, locationMap, livingEntity);
    }

    /**
     * Places the grave blocks at the specified locations.
     *
     * @param grave              The grave to place.
     * @param locationMap        The map of locations for the grave.
     * @param livingEntity       The entity that died.
     */
    private void placeGraveBlocks(Grave grave, Map<Location, BlockData.BlockType> locationMap, LivingEntity livingEntity) {
        for (Map.Entry<Location, BlockData.BlockType> entry : locationMap.entrySet()) {
            Location base = entry.getKey().clone();

            int dx = 0, dy = 0, dz = 0;
            if (entry.getValue() == BlockData.BlockType.NORMAL) {
                dx = plugin.getConfig("placement.offset.x", grave).getInt("placement.offset.x");
                dy = plugin.getConfig("placement.offset.y", grave).getInt("placement.offset.y");
                dz = plugin.getConfig("placement.offset.z", grave).getInt("placement.offset.z");
            }
            Location loc = base.add(dx, dy, dz);

            GraveBlockPlaceEvent modern = new GraveBlockPlaceEvent(grave, loc, entry.getValue(), entry.getKey().getBlock(), livingEntity);
            plugin.getServer().getPluginManager().callEvent(modern);

            com.ranull.graves.event.GraveBlockPlaceEvent legacy = new com.ranull.graves.event.GraveBlockPlaceEvent(grave, loc, entry.getValue(), entry.getKey().getBlock(), livingEntity);
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
        }
    }

    /**
     * Handles failed grave placement.
     *
     * @param event         The entity death event.
     * @param grave         The grave that failed to be placed.
     * @param location      The location where the grave was to be placed.
     * @param livingEntity  The entity that died.
     */
    private void handleFailedGravePlacement(EntityDeathEvent event, Grave grave, Location location, LivingEntity livingEntity) {
        if (event instanceof PlayerDeathEvent pde
                && plugin.getConfig("placement.failure-keep-inventory", grave).getBoolean("placement.failure-keep-inventory")) {
            try {
                pde.setKeepLevel(true);
                pde.setKeepInventory(true);
                plugin.getEntityManager().sendMessage("message.failure-keep-inventory", livingEntity, location, grave);
            } catch (NoSuchMethodError ignored) {}
        } else {
            plugin.getEntityManager().sendMessage("message.failure", livingEntity, location, grave);
        }
    }
}