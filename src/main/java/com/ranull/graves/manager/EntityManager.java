package com.ranull.graves.manager;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.ranull.graves.Graves;
import dev.cwhead.GravesX.compatibility.CompatibilityParticleEnum;
import dev.cwhead.GravesX.compatibility.CompatibilitySoundEnum;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.*;
import dev.cwhead.GravesX.compatibility.CompatibilityTeleport;
import dev.cwhead.GravesX.event.*;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Manages the operations and lifecycle of entities within the Graves plugin.
 */
public final class EntityManager extends EntityDataManager {
    /**
     * The main plugin instance associated with Graves.
     * <p>
     * This {@link Graves} instance represents the core plugin that this Graves is part of. It provides access
     * to the plugin's functionality, configuration, and other services.
     * </p>
     */
    private final Graves plugin;

    /**
     * Initializes the EntityManager with the specified plugin instance.
     *
     * @param plugin the Graves plugin instance.
     */
    public EntityManager(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    /**
     * Swings the main hand of the specified player.
     *
     * @param player the player whose main hand to swing.
     */
    public void swingMainHand(Player player) {
        if (plugin.getVersionManager().hasSwingHand()) {
            player.swingMainHand();
        } else {
            ReflectionUtil.swingMainHand(player);
        }
    }

    /**
     * Creates a grave compass for the specified player, location, and grave.
     *
     * @param player   the player for whom the compass is created.
     * @param location the location to set on the compass.
     * @param grave    the grave associated with the compass.
     * @return the created compass item stack.
     */
    public ItemStack createGraveCompass(Player player, Location location, Grave grave) {
        GraveCompassAddEvent modern = new GraveCompassAddEvent(player, grave);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveCompassAddEvent legacy =
                new com.ranull.graves.event.GraveCompassAddEvent(player, grave);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {
            if (!plugin.getVersionManager().hasPersistentData()) {
                return null;
            }

            Material material = Material.COMPASS;
            if (plugin.getConfig("compass.recovery", grave).getBoolean("compass.recovery")) {
                try {
                    material = Material.valueOf("RECOVERY_COMPASS");
                } catch (IllegalArgumentException ignored) {
                }
            }

            ItemStack itemStack = new ItemStack(material);
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                if (plugin.getVersionManager().hasCompassMeta() && itemMeta instanceof CompassMeta compassMeta) {
                    compassMeta.setLodestoneTracked(false);
                    compassMeta.setLodestone(location);
                } else if (itemStack.getType().name().equals("RECOVERY_COMPASS")) {
                    try {
                        player.setLastDeathLocation(location);
                    } catch (NoSuchMethodError ignored) {
                    }
                }

                List<String> loreList = new ArrayList<>();
                int customModelData = plugin.getConfig("compass.model-data", grave).getInt("compass.model-data", -1);

                if (customModelData > -1) {
                    try {
                        CustomModelDataComponent cmdComponent = itemMeta.getCustomModelDataComponent();
                        cmdComponent.setFloats(Collections.singletonList((float) customModelData));
                        itemMeta.setCustomModelDataComponent(cmdComponent);
                    } catch (Exception e) {
                        itemMeta.setCustomModelData(customModelData);
                    }
                }

                if (plugin.getConfig("compass.glow", grave).getBoolean("compass.glow")) {
                    Enchantment enchantment = plugin.getVersionManager().getEnchantmentForVersion("DURABILITY");
                    itemMeta.addEnchant(enchantment, 1, true);
                    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                if (plugin.getIntegrationManager().hasMiniMessage()) {
                    String compass_name = StringUtil.parseString("&f" +
                            plugin.getConfig("compass.name", grave).getString("compass.name"), grave, plugin);
                    itemMeta.setDisplayName(MiniMessage.parseString(compass_name));
                    itemMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "graveUUID"),
                            PersistentDataType.STRING, grave.getUUID().toString());

                    for (String string : plugin.getConfig("compass.lore", grave).getStringList("compass.lore")) {
                        String compass_lore = StringUtil.parseString("&7" + string, location, grave, plugin);
                        loreList.add(MiniMessage.parseString(compass_lore));
                    }
                } else {
                    itemMeta.setDisplayName(ChatColor.WHITE +
                            StringUtil.parseString(plugin.getConfig("compass.name", grave).getString("compass.name"), grave, plugin));
                    itemMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "graveUUID"),
                            PersistentDataType.STRING, grave.getUUID().toString());

                    for (String string : plugin.getConfig("compass.lore", grave).getStringList("compass.lore")) {
                        loreList.add(ChatColor.GRAY + StringUtil.parseString(string, location, grave, plugin));
                    }
                }

                itemMeta.setLore(loreList);
                itemStack.setItemMeta(itemMeta);
            }

            return itemStack;
        }
        return null;
    }

    /**
     * Retrieves a map of compasses and their associated UUIDs from a player's inventory.
     *
     * @param player the player whose inventory to check.
     * @return a map of compasses and their associated UUIDs.
     */
    public Map<ItemStack, UUID> getCompassesFromInventory(HumanEntity player) {
        Map<ItemStack, UUID> itemStackUUIDMap = new HashMap<>();

        if (plugin.getVersionManager().hasPersistentData()) {
            for (ItemStack itemStack : player.getInventory().getContents()) {
                if (itemStack == null) continue;
                UUID uuid = getGraveUUIDFromItemStack(itemStack);
                if (uuid != null) {
                    itemStackUUIDMap.put(itemStack, uuid);
                }
            }
        }

        return itemStackUUIDMap;
    }

    /**
     * Retrieves the grave UUID from an item stack.
     *
     * @param itemStack the item stack to check.
     * @return the grave UUID, or null if not found.
     */
    public UUID getGraveUUIDFromItemStack(ItemStack itemStack) {
        if (plugin.getVersionManager().hasPersistentData()
                && itemStack != null
                && itemStack.getItemMeta() != null
                && itemStack.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING)) {
            return UUIDUtil.getUUID(itemStack.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING));
        }
        return null;
    }

    /**
     * Teleports an entity to a grave-related target location with safety checks, optional delay,
     * movement-to-cancel behavior, and event hooks.
     *
     * @param entity   the entity to teleport (player or other)
     * @param location the base location to use for resolving a safe teleport position
     * @param grave    the grave context (for yaw, messages, permissions, etc.)
     */
    public void teleportEntity(Entity entity, Location location, Grave grave) {
        GravePreTeleportEvent modernPre = new GravePreTeleportEvent(grave, entity);
        plugin.getServer().getPluginManager().callEvent(modernPre);

        com.ranull.graves.event.GravePreTeleportEvent legacyPre =
                new com.ranull.graves.event.GravePreTeleportEvent(grave, entity);
        plugin.getServer().getPluginManager().callEvent(legacyPre);

        if (modernPre.isCancelled() || modernPre.isAddon() || legacyPre.isCancelled() || legacyPre.isAddon()) {
            return;
        }

        Location base = LocationUtil.roundLocation(location).clone();
        BlockFace face = BlockFaceUtil.getYawBlockFace(grave.getYaw());

        Location target = base.clone()
                .getBlock().getRelative(face).getRelative(face)
                .getLocation().add(0.5, 0.0, 0.5);

        if (plugin.getLocationManager().isLocationSafePlayer(target)) {
            target.setYaw(BlockFaceUtil.getBlockFaceYaw(face.getOppositeFace()));
            target.setPitch(20.0f);
        } else {
            Location safe = plugin.getLocationManager()
                    .getSafeTeleportLocation(entity, base.clone().add(0.0, 1.0, 0.0), grave, plugin);
            if (safe != null) {
                target = safe.add(0.5, 0.0, 0.5);
                target.setYaw(BlockFaceUtil.getBlockFaceYaw(face));
                target.setPitch(90.0f);
            } else {
                target = null;
            }
        }

        final long delaySeconds = plugin.getConfig("teleport.delay", grave).getLong("teleport.delay");

        if (target == null || target.getWorld() == null) {
            plugin.getEntityManager().sendMessage("message.teleport-failure", entity, base, grave);
            return;
        }

        if (entity instanceof Player player) {
            final Location initialLocation = player.getLocation().clone();

            GraveTeleportEvent modern = new GraveTeleportEvent(grave, player);
            plugin.getServer().getPluginManager().callEvent(modern);

            com.ranull.graves.event.GraveTeleportEvent legacy =
                    new com.ranull.graves.event.GraveTeleportEvent(grave, player);
            plugin.getServer().getPluginManager().callEvent(legacy);

            if (!modern.isCancelled() && !modern.isAddon() && !legacy.isCancelled() && !legacy.isAddon()) {
                final boolean bypass = plugin.hasGrantedPermission("graves.teleport.delay-bypass", player);

                if (!bypass && delaySeconds > 0L) {
                    final Location finalTarget = target.clone();

                    final BossBar bossBar;
                    if (plugin.getIntegrationManager().hasMiniMessage()) {
                        String mmText = StringUtil.parseString(
                                plugin.getConfig("message.teleport-waiting", grave).getString("message.teleport-waiting"),
                                base, grave, plugin
                        );
                        bossBar = plugin.getServer().createBossBar(
                                MiniMessage.parseString(mmText),
                                BarColor.RED, BarStyle.SOLID
                        );
                    } else {
                        bossBar = plugin.getServer().createBossBar(
                                StringUtil.parseString(
                                        plugin.getConfig("message.teleport-waiting", grave).getString("message.teleport-waiting"),
                                        base, grave, plugin
                                ),
                                BarColor.RED, BarStyle.SOLID
                        );
                    }
                    bossBar.addPlayer(player);

                    final int[] secondsLeft = { (int) Math.min(Integer.MAX_VALUE, Math.max(0L, delaySeconds)) };
                    final Object[] cancelRef = new Object[1];

                    Runnable tick = () -> {
                        if (!player.isOnline()) {
                            bossBar.removeAll();
                            plugin.getEntityManager().sendMessage("message.teleport-cancelled", player, player.getLocation(), grave);
                            if (cancelRef[0] != null) ((Runnable) cancelRef[0]).run();
                            return;
                        }

                        Location now = player.getLocation();
                        boolean moved = plugin.getConfig().getBoolean("teleport.strict")
                                ? !now.equals(initialLocation)
                                : !now.getBlock().equals(initialLocation.getBlock());

                        if (moved) {
                            bossBar.removeAll();
                            plugin.getEntityManager().sendMessage("message.teleport-cancelled", player, now, grave);
                            if (cancelRef[0] != null) ((Runnable) cancelRef[0]).run();
                            return;
                        }

                        if (secondsLeft[0] > 0) {
                            double progress = (double) secondsLeft[0] / (double) Math.max(1, delaySeconds);
                            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                            secondsLeft[0]--;
                        } else {
                            // Perform the teleport (compat layer handles the details)
                            CompatibilityTeleport.teleportSafely(player, finalTarget, plugin).thenAccept(ok -> {
                                if (ok) {
                                    plugin.getEntityManager().sendMessage("message.teleport", player, finalTarget, grave);
                                    plugin.getEntityManager().playPlayerSound("sound.teleport", player, finalTarget, grave);
                                } else {
                                    plugin.getEntityManager().sendMessage("message.teleport-cancelled", player, player.getLocation(), grave);
                                }
                                bossBar.removeAll();
                                if (cancelRef[0] != null) ((Runnable) cancelRef[0]).run();
                            });
                        }
                    };

                    final MyScheduledTask task = plugin.getGravesXScheduler().runTaskTimer(
                            () -> executeRegion(player, tick), 0L, 20L);
                    cancelRef[0] = (Runnable) task::cancel;

                } else {
                    final Location finalTarget = target.clone();
                    boolean strict = plugin.getConfig().getBoolean("teleport.strict");
                    boolean samePos = strict ? player.getLocation().equals(initialLocation)
                            : player.getLocation().getBlock().equals(initialLocation.getBlock());

                    if (samePos) {
                        CompatibilityTeleport.teleportSafely(player, finalTarget, plugin).thenAccept(ok -> {
                            if (ok) {
                                plugin.getEntityManager().sendMessage("message.teleport", player, finalTarget, grave);
                                plugin.getEntityManager().playPlayerSound("sound.teleport", player, finalTarget, grave);
                            } else {
                                plugin.getEntityManager().sendMessage("message.teleport-cancelled", player, player.getLocation(), grave);
                            }
                        });
                    } else {
                        plugin.getEntityManager().sendMessage("message.teleport-cancelled", player, player.getLocation(), grave);
                    }
                }
            }
        } else {
            // --- Non-player entity path ---
            GraveTeleportEvent modern = new GraveTeleportEvent(grave, entity);
            plugin.getServer().getPluginManager().callEvent(modern);

            com.ranull.graves.event.GraveTeleportEvent legacy =
                    new com.ranull.graves.event.GraveTeleportEvent(grave, entity);
            plugin.getServer().getPluginManager().callEvent(legacy);

            if (!modern.isCancelled() && !modern.isAddon() && !legacy.isCancelled() && !legacy.isAddon()) {
                final Location finalTarget = target.clone();

                Runnable doTeleport = () -> executeRegion(entity, () -> {
                    if (entity.isValid()) {
                        entity.teleport(finalTarget);
                        plugin.getEntityManager().sendMessage("message.teleport", entity, entity.getLocation(), grave);
                    }
                });

                if (delaySeconds > 0L) {
                    plugin.getGravesXScheduler().runTaskLater(doTeleport, delaySeconds * 20L);
                } else {
                    doTeleport.run();
                }
            }
        }
    }

    /**
     * Plays a world sound at the player's location.
     *
     * @param string the sound identifier.
     * @param player the player whose location to play the sound at.
     */
    public void playWorldSound(String string, Player player) {
        playWorldSound(string, player.getLocation(), null);
    }

    /**
     * Plays a world sound at the player's location, associated with a grave.
     *
     * @param string the sound identifier.
     * @param player the player whose location to play the sound at.
     * @param grave  the grave associated with the sound.
     */
    public void playWorldSound(String string, Player player, Grave grave) {
        playWorldSound(string, player.getLocation(), grave);
    }

    /**
     * Plays a world sound at a specified location, associated with a grave.
     *
     * @param string   the sound identifier.
     * @param location the location to play the sound at.
     * @param grave    the grave associated with the sound.
     */
    public void playWorldSound(String string, Location location, Grave grave) {
        playWorldSound(string, location, grave != null ? grave.getOwnerType() : null, grave != null
                ? grave.getPermissionList() : null, 1, 1);
    }

    /**
     * Plays a world sound at a specified location with additional parameters.
     *
     * @param string         the sound identifier.
     * @param location       the location to play the sound at.
     * @param entityType     the type of entity associated with the sound.
     * @param permissionList the list of permissions associated with the sound.
     * @param volume         the volume of the sound.
     * @param pitch          the pitch of the sound.
     */
    public void playWorldSound(String string, Location location, EntityType entityType, List<String> permissionList,
                               float volume, float pitch) {
        if (location.getWorld() != null) {
            string = plugin.getConfig(string, entityType, permissionList).getString(string);

            if (string != null && !string.isEmpty()) {
                try {
                    final Location locCopy = location.clone();
                    String finalString = string;
                    executeRegion(locCopy, () -> {
                        World w = locCopy.getWorld();
                        if (w != null) w.playSound(locCopy, CompatibilitySoundEnum.valueOf(finalString.toUpperCase()), volume, pitch);
                    });
                } catch (IllegalArgumentException exception) {
                    plugin.debugMessage(string.toUpperCase() + " is not a Sound ENUM", 1);
                }
            }
        }
    }

    /**
     * Plays a player-specific sound at the entity's location.
     *
     * @param string the sound identifier.
     * @param entity the entity to play the sound for.
     * @param grave  the grave associated with the sound.
     */
    public void playPlayerSound(String string, Entity entity, Grave grave) {
        playPlayerSound(string, entity, entity.getLocation(), grave.getPermissionList(), 1, 1);
    }

    /**
     * Plays a player-specific sound at a specified location for an entity.
     *
     * @param string   the sound identifier.
     * @param entity   the entity to play the sound for.
     * @param location the location to play the sound at.
     * @param grave    the grave associated with the sound.
     */
    public void playPlayerSound(String string, Entity entity, Location location, Grave grave) {
        playPlayerSound(string, entity, location, grave.getPermissionList(), 1, 1);
    }

    /**
     * Plays a player-specific sound at the entity's location with a permission list.
     *
     * @param string         the sound identifier.
     * @param entity         the entity to play the sound for.
     * @param permissionList the list of permissions associated with the sound.
     */
    public void playPlayerSound(String string, Entity entity, List<String> permissionList) {
        playPlayerSound(string, entity, entity.getLocation(), permissionList, 1, 1);
    }

    /**
     * Plays a player-specific sound at a specified location with a permission list.
     *
     * @param string         the sound identifier.
     * @param entity         the entity to play the sound for.
     * @param location       the location to play the sound at.
     * @param permissionList the list of permissions associated with the sound.
     */
    public void playPlayerSound(String string, Entity entity, Location location, List<String> permissionList) {
        playPlayerSound(string, entity, location, permissionList, 1, 1);
    }

    /**
     * Plays a player-specific sound at a specified location with additional parameters.
     *
     * @param string         the sound identifier.
     * @param entity         the entity to play the sound for.
     * @param location       the location to play the sound at.
     * @param permissionList the list of permissions associated with the sound.
     * @param volume         the volume of the sound.
     * @param pitch          the pitch of the sound.
     */
    public void playPlayerSound(String string, Entity entity, Location location, List<String> permissionList,
                                float volume, float pitch) {
        if (entity instanceof Player player) {
            string = plugin.getConfig(string, entity, permissionList).getString(string);

            if (string != null && !string.isEmpty()) {
                try {
                    final Location locCopy = location.clone();
                    String finalString = string;
                    executeRegion(player, () -> player.playSound(locCopy, CompatibilitySoundEnum.valueOf(finalString.toUpperCase()), volume, pitch));
                } catch (IllegalArgumentException exception) {
                    plugin.debugMessage(string.toUpperCase() + " is not a Sound ENUM", 1);
                }
            }
        }
    }

    /**
     * Sends a message to a command sender.
     *
     * @param string        the message identifier.
     * @param commandSender the command sender to send the message to.
     */
    public void sendMessage(String string, CommandSender commandSender) {
        if (commandSender instanceof Player player) {
            sendMessage(string, player, player.getLocation(), null, plugin.getPermissionList(player));
        }
    }

    /**
     * Sends a message to an entity.
     *
     * @param string the message identifier.
     * @param entity the entity to send the message to.
     */
    public void sendMessage(String string, Entity entity) {
        sendMessage(string, entity, entity.getLocation(), null, plugin.getPermissionList(entity));
    }

    /**
     * Sends a message to an entity with a permission list.
     *
     * @param string         the message identifier.
     * @param entity         the entity to send the message to.
     * @param permissionList the list of permissions associated with the message.
     */
    public void sendMessage(String string, Entity entity, List<String> permissionList) {
        sendMessage(string, entity, entity.getLocation(), null, permissionList);
    }

    /**
     * Sends a message to an entity at a specified location with a permission list.
     *
     * @param string         the message identifier.
     * @param entity         the entity to send the message to.
     * @param location       the location associated with the message.
     * @param permissionList the list of permissions associated with the message.
     */
    public void sendMessage(String string, Entity entity, Location location, List<String> permissionList) {
        sendMessage(string, entity, location, null, permissionList);
    }

    /**
     * Sends a message to an entity at a specified location associated with a grave.
     *
     * @param string   the message identifier.
     * @param entity   the entity to send the message to.
     * @param location the location associated with the message.
     * @param grave    the grave associated with the message.
     */
    public void sendMessage(String string, Entity entity, Location location, Grave grave) {
        sendMessage(string, entity, location, grave, null);
    }

    /**
     * Sends a message to an entity with a custom name and a permission list.
     *
     * @param string         the message identifier.
     * @param entity         the entity to send the message to.
     * @param name           the custom name associated with the message.
     * @param location       the location associated with the message.
     * @param permissionList the list of permissions associated with the message.
     */
    public void sendMessage(String string, Entity entity, String name, Location location, List<String> permissionList) {
        sendMessage(string, entity, name, location, null, permissionList);
    }

    private void sendMessage(String string, Entity entity, Location location, Grave grave, List<String> permissionList) {
        sendMessage(string, entity, getEntityName(entity), location, grave, permissionList);
    }

    private void sendMessage(String string, Entity entity, String name, Location location, Grave grave, List<String> permissionList) {
        if (entity instanceof Player player) {
            String originalConfigString = string;
            if (grave != null) {
                string = plugin.getConfig(string, grave).getString(string);
            } else {
                string = plugin.getConfig(string, entity.getType(), permissionList).getString(string);
            }

            String prefix = plugin.getConfig("message.prefix", entity.getType(), permissionList)
                    .getString("message.prefix");

            if (string != null && !string.isEmpty()) {
                if (prefix != null && !prefix.isEmpty()) {
                    if (plugin.getIntegrationManager().hasMiniMessage()) {
                        string = prefix + "<white>" + string;
                    } else {
                        string = prefix + string;
                    }
                } else {
                    string = plugin.getIntegrationManager().hasMiniMessage() ? "<white>" + string : "&r" + string;
                }

                String message = StringUtil.parseString(string, entity, name, location, grave, plugin);
                plugin.debugMessage("Message found for " + string + " in grave.yml. Sending message to " + entity.getName() + ".", 2);
                if (plugin.getIntegrationManager().hasMiniMessage()) {
                    MiniMessage.sendMessage(player, message);
                } else {
                    player.sendMessage(message);
                }
            } else {
                plugin.debugMessage("Original string " + originalConfigString + " is empty, no message sent.", 2);
            }
        }
    }

    /**
     * Runs commands associated with an entity, location, and grave.
     *
     * @param string   the command identifier.
     * @param entity   the entity associated with the command.
     * @param location the location associated with the command.
     * @param grave    the grave associated with the command.
     */
    public void runCommands(String string, Entity entity, Location location, Grave grave) {
        runCommands(string, entity, null, location, grave);
    }

    /**
     * Runs commands associated with a name, location, and grave.
     *
     * @param string   the command identifier.
     * @param name     the name associated with the command.
     * @param location the location associated with the command.
     * @param grave    the grave associated with the command.
     */
    public void runCommands(String string, String name, Location location, Grave grave) {
        runCommands(string, null, name, location, grave);
    }

    private void runCommands(String string, Entity entity, String name, Location location, Grave grave) {
        for (String command : plugin.getConfig(string, grave).getStringList(string)) {
            if (command != null && !command.isEmpty()) {
                runConsoleCommand(StringUtil.parseString(command, entity, name, location, grave, plugin));
            }
        }
    }

    private void runConsoleCommand(String string) {
        if (string != null && !string.isEmpty()) {
            ServerCommandEvent serverCommandEvent = new ServerCommandEvent(plugin.getServer().getConsoleSender(), string);

            plugin.getServer().getPluginManager().callEvent(serverCommandEvent);

            if ((plugin.getVersionManager().is_v1_7() || plugin.getVersionManager().is_v1_8())
                    || !serverCommandEvent.isCancelled()) {
                plugin.getGravesXScheduler().callSyncMethod(() -> plugin.getServer()
                        .dispatchCommand(serverCommandEvent.getSender(), serverCommandEvent.getCommand()));
                plugin.debugMessage("Running console command " + string, 1);
            }
        }
    }

    /**
     * Runs a function associated with an entity and a specified function name.
     *
     * @param entity   the entity to run the function for.
     * @param function the name of the function to run.
     * @return true if the function was run successfully, false otherwise.
     */
    public boolean runFunction(Entity entity, String function) {
        return runFunction(entity, function, null);
    }

    /**
     * Runs a function associated with an entity, a specified function name, and a grave.
     *
     * @param entity   the entity to run the function for.
     * @param function the name of the function to run.
     * @param grave    the grave associated with the function.
     * @return true if the function was run successfully, false otherwise.
     */
    public boolean runFunction(Entity entity, String function, Grave grave) {
        switch (function.toLowerCase()) {
            case "list" -> {
                UUID targetUUID = grave.getOwnerUUID();
                if (targetUUID == null) {
                    targetUUID = entity.getUniqueId();
                }
                plugin.getGUIManager().openGraveList(entity, targetUUID);
                return true;
            }
            case "menu" -> {
                plugin.getGUIManager().openGraveMenu(entity, grave);
                return true;
            }
            case "teleport", "teleportation" -> {
                if (plugin.getConfig("teleport.enabled", grave).getBoolean("teleport.enabled")
                        && (plugin.hasGrantedPermission("graves.teleport", ((Player) entity).getPlayer())
                        || plugin.getConfig("teleport.enabled", grave).getBoolean("teleport.enabled")
                        && plugin.hasGrantedPermission("graves.teleport.world." + grave.getLocationDeath().getWorld().getName(), ((Player) entity).getPlayer())
                        || plugin.hasGrantedPermission("graves.bypass", ((Player) entity).getPlayer()))
                        || plugin.hasGrantedPermission("graves.teleport.bypass", ((Player) entity).getPlayer())) {

                    boolean isBypassOther =
                            (plugin.hasGrantedPermission("graves.bypass", ((Player) entity).getPlayer())
                                    && !grave.getOwnerUUID().equals(entity.getUniqueId()))
                                    || (plugin.hasGrantedPermission("graves.teleport.bypass", ((Player) entity).getPlayer())
                                    && !grave.getOwnerUUID().equals(entity.getUniqueId()));

                    GraveTeleportEvent modern =
                            new GraveTeleportEvent(grave, entity);
                    plugin.getServer().getPluginManager().callEvent(modern);

                    com.ranull.graves.event.GraveTeleportEvent legacy =
                            new com.ranull.graves.event.GraveTeleportEvent(grave, entity);
                    plugin.getServer().getPluginManager().callEvent(legacy);

                    if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {
                        if (isBypassOther) {
                            executeRegion(entity, () ->
                                    entity.teleport(plugin.getGraveManager()
                                            .getGraveLocation(grave.getLocationDeath().add(1, 0, 1), grave)));
                        } else {
                            plugin.getEntityManager().teleportEntity(
                                    entity,
                                    plugin.getGraveManager().getGraveLocationList(entity.getLocation(), grave).get(0),
                                    grave
                            );
                        }
                    }
                } else {
                    plugin.getEntityManager().sendMessage("message.teleport-disabled", entity,
                            entity.getLocation(), grave);
                }
                return true;
            }
            case "protect", "protection" -> {
                GraveProtectionCreateEvent modern = new GraveProtectionCreateEvent(entity, grave);
                plugin.getServer().getPluginManager().callEvent(modern);

                com.ranull.graves.event.GraveProtectionCreateEvent legacy = new com.ranull.graves.event.GraveProtectionCreateEvent(entity, grave);
                plugin.getServer().getPluginManager().callEvent(legacy);

                if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {
                    if (grave.getTimeProtectionRemaining() > 0 || grave.getTimeProtectionRemaining() < 0) {
                        plugin.getGraveManager().toggleGraveProtection(grave);
                        playPlayerSound("sound.protection-change", entity, grave);
                        plugin.getGUIManager().openGraveMenu(entity, grave, false);
                    }
                }
                return true;
            }
            case "abandoned" -> {
                GraveAbandonedEvent modern = new GraveAbandonedEvent(grave);
                plugin.getServer().getPluginManager().callEvent(modern);

                com.ranull.graves.event.GraveAbandonedEvent legacy = new com.ranull.graves.event.GraveAbandonedEvent(grave);
                plugin.getServer().getPluginManager().callEvent(legacy);

                if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {
                    Location location = plugin.getGraveManager().getGraveLocation(entity.getLocation(), grave);
                    if (location != null) {
                        playPlayerSound("sound.abandoned", entity, grave);
                        plugin.getEntityManager().sendMessage("message.abandoned", entity, location, grave);
                        plugin.getGraveManager().abandonGrave(grave);
                    }
                }
                return true;
            }
            case "distance" -> {
                Player player = (entity instanceof Player p) ? p : null;
                if (player == null) return false;

                GraveCompassUseEvent modern = new GraveCompassUseEvent(player, grave);
                plugin.getServer().getPluginManager().callEvent(modern);

                com.ranull.graves.event.GraveCompassUseEvent legacy =
                        new com.ranull.graves.event.GraveCompassUseEvent(player, grave);
                plugin.getServer().getPluginManager().callEvent(legacy);

                if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {
                    Location location = plugin.getGraveManager().getGraveLocation(entity.getLocation(), grave);
                    if (location != null) {
                        if (entity.getWorld().equals(location.getWorld())) {
                            plugin.getEntityManager().sendMessage("message.distance", entity, location, grave);
                        } else {
                            plugin.getEntityManager().sendMessage("message.distance-world", entity, location, grave);
                        }
                    }
                }
                return true;
            }
            case "open", "loot", "virtual" -> {
                if (entity.getLocation().getWorld() == grave.getLocationDeath().getWorld()) {
                    double distance = plugin.getConfig("virtual.distance", grave).getDouble("virtual.distance");
                    if (distance < 0) {
                        plugin.getGraveManager().openGrave(entity, entity.getLocation(), grave);
                    } else {
                        Location location = plugin.getGraveManager().getGraveLocation(entity.getLocation(), grave);
                        if (location != null && entity.getLocation().getWorld() == grave.getLocationDeath().getWorld()) {
                            if (entity.getLocation().distance(location) <= distance) {
                                plugin.getGraveManager().openGrave(entity, entity.getLocation(), grave);
                            } else {
                                plugin.getEntityManager().sendMessage("message.distance-virtual", entity, location, grave);
                            }
                        }
                    }
                } else {
                    plugin.getEntityManager().sendMessage("message.distance-virtual", entity, entity.getLocation(), grave);
                }
                return true;
            }
            case "autoloot" -> {
                Location loc = entity.getLocation();

                GraveAutoLootEvent modern =
                        new GraveAutoLootEvent(entity, loc, grave);
                plugin.getServer().getPluginManager().callEvent(modern);

                com.ranull.graves.event.GraveAutoLootEvent legacy =
                        new com.ranull.graves.event.GraveAutoLootEvent(entity, loc, grave);
                plugin.getServer().getPluginManager().callEvent(legacy);

                if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {
                    plugin.getGraveManager().autoLootGrave(entity, loc, grave);

                    if (plugin.getIntegrationManager().hasNoteBlockAPI()) {
                        Player player = (Player) entity;
                        if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForPlayer(player)) {
                            plugin.getIntegrationManager().getNoteBlockAPI().stopSongForPlayer(player);
                        }
                        if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForAllPlayers()) {
                            plugin.getIntegrationManager().getNoteBlockAPI().stopSongForAllPlayers();
                        }
                    }
                }
                return true;
            }
            case "particle", "particles" -> {
                if (plugin.getConfig("compass.particles.enabled", grave).getBoolean("compass.particles.enabled")) {
                    Player player = (entity instanceof Player p) ? p : null;
                    if (player == null) return false;

                    GraveParticleEvent modern = new GraveParticleEvent(player, grave);
                    plugin.getServer().getPluginManager().callEvent(modern);

                    com.ranull.graves.event.GraveParticleEvent legacy =
                            new com.ranull.graves.event.GraveParticleEvent(player, grave);
                    plugin.getServer().getPluginManager().callEvent(legacy);

                    if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {
                        Location location = plugin.getGraveManager().getGraveLocation(entity.getLocation(), grave);

                        if (location != null && entity.getLocation().getWorld() == grave.getLocationDeath().getWorld()) {
                            plugin.getParticleManager().startCompassParticleTrail(
                                    entity.getLocation(),
                                    grave.getLocationDeath(),
                                    CompatibilityParticleEnum.valueOf(Objects.requireNonNull(
                                                    plugin.getConfig("compass.particles.particle", grave)
                                                            .getString("compass.particles.particle"))
                                            .toUpperCase()),
                                    plugin.getConfig("compass.particles.count", grave)
                                            .getInt("compass.particles.count", 5),
                                    plugin.getConfig("compass.particles.speed", grave)
                                            .getDouble("compass.particles.speed", 0.3),
                                    plugin.getConfig("compass.particles.duration", grave)
                                            .getInt("compass.particles.duration"),
                                    entity.getUniqueId()
                            );
                        }
                    }
                }
                return true;
            }
            case "preview", "sneekpeak" -> {
                Location location = plugin.getGraveManager().getGraveLocation(entity.getLocation(), grave);
                if (location != null && entity.getLocation().getWorld() == grave.getLocationDeath().getWorld()) {
                    if (plugin.getConfig("grave.preview", grave).getBoolean("grave.preview")) {
                        plugin.getGraveManager().openGrave(entity, entity.getLocation(), grave, true);
                    }
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Checks if a player can open a specified grave.
     * Supports both Java and Bedrock players (via Geyser/Floodgate).
     *
     * @param player the player attempting to open the grave
     * @param grave  the grave to check
     * @return true if the player can open the grave, false otherwise
     */
    public boolean canOpenGrave(Player player, Grave grave) {
        UUID playerId = plugin.getIntegrationManager().hasFloodgate()
                ? plugin.getIntegrationManager().getFloodgate().getNormalizedUUID(player)
                : player.getUniqueId();

        UUID ownerId = grave.getOwnerUUID();
        UUID killerId = grave.getKillerUUID();

        if (plugin.getIntegrationManager().hasFloodgate()) {
            if (ownerId != null && plugin.getIntegrationManager().getFloodgate().isFloodgateId(ownerId)) {
                ownerId = plugin.getIntegrationManager().getFloodgate().getCorrectUniqueId(ownerId);
            }

            if (killerId != null && plugin.getIntegrationManager().getFloodgate().isFloodgateId(killerId)) {
                killerId = plugin.getIntegrationManager().getFloodgate().getCorrectUniqueId(killerId);
            }
        }

        if (grave.getTimeProtectionRemaining() == 0 || plugin.hasGrantedPermission("graves.bypass", player)) {
            return true;
        }

        if (!grave.getProtection() || ownerId == null) {
            return true;
        }

        if (ownerId.equals(playerId)
                && plugin.getConfig("protection.open.owner", grave).getBoolean("protection.open.owner")) {
            return true;
        }

        if (killerId != null) {
            if (killerId.equals(playerId)
                    && plugin.getConfig("protection.open.killer", grave).getBoolean("protection.open.killer")) {
                return true;
            } else {
                return !ownerId.equals(playerId)
                        && !killerId.equals(playerId)
                        && plugin.getConfig("protection.open.other", grave).getBoolean("protection.open.other");
            }
        } else {
            return (ownerId.equals(playerId)
                    && plugin.getConfig("protection.open.missing.owner", grave).getBoolean("protection.open.missing.owner"))
                    || (!ownerId.equals(playerId)
                    && plugin.getConfig("protection.open.missing.other", grave).getBoolean("protection.open.missing.other"));
        }
    }

    /**
     * Spawns a zombie at a specified location, targeting a specified entity, and associated with a grave.
     *
     * @param location     the location to spawn the zombie.
     * @param entity       the entity associated with the zombie spawn.
     * @param targetEntity the entity to be targeted by the zombie.
     * @param grave        the grave associated with the zombie spawn.
     */
    public void spawnZombie(Location location, Entity entity, LivingEntity targetEntity, Grave grave) {
        if ((plugin.getConfig("zombie.spawn-owner", grave).getBoolean("zombie.spawn-owner")
                && grave.getOwnerUUID().equals(entity.getUniqueId())
                || plugin.getConfig("zombie.spawn-other", grave).getBoolean("zombie.spawn-other")
                && !grave.getOwnerUUID().equals(entity.getUniqueId()))) {
            spawnZombie(location, targetEntity, grave);
        }
    }

    /**
     * Spawns a zombie at a specified location, associated with a grave.
     *
     * @param location the location to spawn the zombie.
     * @param grave    the grave associated with the zombie spawn.
     */
    public void spawnZombie(Location location, Grave grave) {
        spawnZombie(location, null, grave);
    }

    @SuppressWarnings("deprecation")
    private void spawnZombie(Location location, LivingEntity targetEntity, Grave grave) {
        if (location == null || location.getWorld() == null || grave.getOwnerType() != EntityType.PLAYER) return;

        GraveZombieSpawnEvent modern = new GraveZombieSpawnEvent(location, targetEntity, grave);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveZombieSpawnEvent legacy =
                new com.ranull.graves.event.GraveZombieSpawnEvent(location, targetEntity, grave);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {

            final Location locCopy = location.clone();
            executeRegion(locCopy, () -> {
                String zombieType = plugin.getConfig("zombie.type", grave)
                        .getString("zombie.type", "ZOMBIE").toUpperCase();
                EntityType entityType = EntityType.ZOMBIE;
                try {
                    entityType = EntityType.valueOf(zombieType);
                } catch (IllegalArgumentException ex) {
                    plugin.debugMessage(zombieType + " is not a EntityType ENUM", 1);
                }

                if ("ZOMBIE".equals(entityType.name()) && MaterialUtil.isWater(locCopy.getBlock().getType())) {
                    try {
                        entityType = EntityType.valueOf("DROWNED");
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                Entity entity = locCopy.getWorld().spawnEntity(locCopy, entityType);

                if (entity instanceof LivingEntity livingEntity) {

                    if (livingEntity.getEquipment() != null) {
                        if (plugin.getConfig("zombie.owner-head", grave).getBoolean("zombie.owner-head")) {
                            livingEntity.getEquipment().setHelmet(plugin.getCompatibility().getSkullItemStack(grave, plugin));
                        }
                        livingEntity.getEquipment().setChestplate(null);
                        livingEntity.getEquipment().setLeggings(null);
                        livingEntity.getEquipment().setBoots(null);
                    }

                    livingEntity.setMetadata("GravesX", new FixedMetadataValue(plugin, true)); // don’t break other plugins

                    double zombieHealth = plugin.getConfig("zombie.health", grave).getDouble("zombie.health");
                    if (zombieHealth >= 0.5) {
                        livingEntity.setMaxHealth(zombieHealth);
                        livingEntity.setHealth(zombieHealth);
                    }

                    if (!plugin.getConfig("zombie.pickup", grave).getBoolean("zombie.pickup")) {
                        livingEntity.setCanPickupItems(false);
                    }

                    String zombieName = StringUtil.parseString(
                            plugin.getConfig("zombie.name", grave).getString("zombie.name"),
                            locCopy, grave, plugin
                    );

                    if (!zombieName.isEmpty()) {
                        if (plugin.getIntegrationManager().hasMiniMessage()) {
                            livingEntity.setCustomName(MiniMessage.parseString(zombieName));
                        } else {
                            livingEntity.setCustomName(zombieName);
                        }
                    }

                    setDataByte(livingEntity, "graveZombie");
                    setDataString(livingEntity, "graveUUID", grave.getUUID().toString());
                    setDataString(livingEntity, "graveEntityType", grave.getOwnerType().name());
                    runCommands("event.command.zombiespawn", targetEntity, locCopy, grave);

                    if (grave.getPermissionList() != null && !grave.getPermissionList().isEmpty()) {
                        setDataString(livingEntity, "gravePermissionList", String.join("|", grave.getPermissionList()));
                    }

                    if (livingEntity instanceof Mob mob) {
                        if (targetEntity != null && !targetEntity.isInvulnerable()
                                && (!(targetEntity instanceof Player p) || p.getGameMode() != GameMode.CREATIVE)) {
                            mob.setTarget(targetEntity);
                        }
                    }

                    if (livingEntity instanceof Zombie zombie) {
                        if (zombie.isBaby()) zombie.setBaby(false);
                    }
                }

                plugin.debugMessage("Zombie type " + getEntityName(entity) + " spawned for grave " + grave.getUUID(), 1);
            });
        }
    }

    /**
     * Creates an armor stand at a specified location associated with a grave.
     *
     * @param location the location to create the armor stand.
     * @param grave    the grave associated with the armor stand.
     */
    public void createArmorStand(Location location, Grave grave) {
        if (!plugin.getVersionManager().is_v1_7()
                && plugin.getConfig("armor-stand.enabled", grave).getBoolean("armor-stand.enabled")) {
            double offsetX = plugin.getConfig("armor-stand.offset.x", grave).getDouble("armor-stand.offset.x");
            double offsetY = plugin.getConfig("armor-stand.offset.y", grave).getDouble("armor-stand.offset.y");
            double offsetZ = plugin.getConfig("armor-stand.offset.z", grave).getDouble("armor-stand.offset.z");
            Location loc = LocationUtil.roundLocation(location)
                    .add(offsetX + 0.5, offsetY, offsetZ + 0.5);

            loc.setYaw(grave.getYaw());
            loc.setPitch(grave.getPitch());

            if (loc.getWorld() != null) {
                executeRegion(loc, () -> {
                    Material material = Material.matchMaterial(plugin.getConfig("armor-stand.material", grave)
                            .getString("armor-stand.material", "AIR"));

                    if (material != null && !MaterialUtil.isAir(material)) {
                        ItemStack itemStack = new ItemStack(material, 1);
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        int customModelData = plugin.getConfig("armor-stand.model-data", grave)
                                .getInt("armor-stand.model-data", -1);

                        if (itemMeta != null) {
                            if (customModelData > -1) {
                                try {
                                    CustomModelDataComponent cmdComponent = itemMeta.getCustomModelDataComponent();

                                    cmdComponent.setFloats(Collections.singletonList((float) customModelData));

                                    itemMeta.setCustomModelDataComponent(cmdComponent);
                                } catch (Exception e) {
                                    itemMeta.setCustomModelData(customModelData);
                                }
                            }

                            itemStack.setItemMeta(itemMeta);
                            loc.getBlock().setType(Material.AIR);

                            ArmorStand armorStand = loc.getWorld().spawn(loc, ArmorStand.class);

                            createEntityData(loc, armorStand.getUniqueId(), grave.getUUID(),
                                    EntityData.Type.ARMOR_STAND);

                            boolean marker = plugin.getConfig("armor-stand.marker", grave).getBoolean("armor-stand.marker");
                            if (!plugin.getVersionManager().is_v1_7()) {
                                try {
                                    armorStand.setMarker(marker);
                                } catch (NoSuchMethodError ignored) {
                                }
                            }

                            if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()) {
                                armorStand.setInvulnerable(true);
                            }

                            if (plugin.getVersionManager().hasScoreboardTags()) {
                                armorStand.getScoreboardTags().add("graveArmorStand");
                                armorStand.getScoreboardTags().add("graveArmorStandUUID:" + grave.getUUID());
                            }

                            armorStand.setVisible(false);
                            armorStand.setGravity(false);
                            armorStand.setCustomNameVisible(false);
                            armorStand.setSmall(plugin.getConfig("armor-stand.small", grave)
                                    .getBoolean("armor-stand.small"));

                            if (armorStand.getEquipment() != null) {
                                EquipmentSlot equipmentSlot = EquipmentSlot.HEAD;

                                try {
                                    equipmentSlot = EquipmentSlot.valueOf(plugin.getConfig("armor-stand.slot", grave)
                                            .getString("armor-stand.slot", "HEAD"));
                                } catch (IllegalArgumentException ignored) {
                                }

                                armorStand.getEquipment().setItem(equipmentSlot, itemStack);
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * Creates an item frame at a specified location associated with a grave.
     *
     * @param location the location to create the item frame.
     * @param grave    the grave associated with the item frame.
     */
    public void createItemFrame(Location location, Grave grave) {
        if (plugin.getConfig("item-frame.enabled", grave).getBoolean("item-frame.enabled")) {
            double offsetX = plugin.getConfig("item-frame.offset.x", grave).getDouble("item-frame.offset.x");
            double offsetY = plugin.getConfig("item-frame.offset.y", grave).getDouble("item-frame.offset.y");
            double offsetZ = plugin.getConfig("item-frame.offset.z", grave).getDouble("item-frame.offset.z");
            Location loc = LocationUtil.roundLocation(location).add(offsetX + 0.5, offsetY, offsetZ + 0.5);

            loc.setYaw(grave.getYaw());
            loc.setPitch(grave.getPitch());

            if (loc.getWorld() != null) {
                executeRegion(loc, () -> {
                    Material material = Material.matchMaterial(plugin.getConfig("item-frame.material", grave)
                            .getString("item-frame.material", "AIR"));

                    if (material != null && !MaterialUtil.isAir(material)) {
                        ItemStack itemStack = new ItemStack(material, 1);
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        int customModelData = plugin.getConfig("item-frame.model-data", grave)
                                .getInt("item-frame.model-data", -1);

                        if (itemMeta != null) {
                            if (customModelData > -1) {
                                try {
                                    CustomModelDataComponent cmdComponent = itemMeta.getCustomModelDataComponent();

                                    cmdComponent.setFloats(Collections.singletonList((float) customModelData));

                                    itemMeta.setCustomModelDataComponent(cmdComponent);
                                } catch (Exception e) {
                                    itemMeta.setCustomModelData(customModelData);
                                }
                            }

                            itemStack.setItemMeta(itemMeta);
                            loc.getBlock().setType(Material.AIR);

                            ItemFrame itemFrame = loc.getWorld().spawn(loc, ItemFrame.class);

                            itemFrame.setFacingDirection(BlockFace.UP);
                            itemFrame.setRotation(BlockFaceUtil.getBlockFaceRotation(BlockFaceUtil
                                    .getYawBlockFace(loc.getYaw())));
                            itemFrame.setVisible(false);
                            itemFrame.setGravity(false);
                            itemFrame.setCustomNameVisible(false);
                            itemFrame.setItem(itemStack);

                            if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()) {
                                itemFrame.setInvulnerable(true);
                            }

                            if (plugin.getVersionManager().hasScoreboardTags()) {
                                itemFrame.getScoreboardTags().add("graveItemFrame");
                                itemFrame.getScoreboardTags().add("graveItemFrameUUID:" + grave.getUUID());
                            }

                            createEntityData(loc, itemFrame.getUniqueId(), grave.getUUID(),
                                    EntityData.Type.ITEM_FRAME);
                        }
                    }
                });
            }
        }
    }

    /**
     * Removes all entities associated with a grave.
     *
     * @param grave the grave whose entities to remove.
     */
    public void removeEntity(Grave grave) {
        removeEntity(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    /**
     * Removes a map of entity data and their associated entities.
     *
     * @param entityDataMap the map of entity data and entities to remove.
     */
    public void removeEntity(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            if (entry.getKey().getType() == EntityData.Type.ARMOR_STAND
                    || entry.getKey().getType() == EntityData.Type.ITEM_FRAME
                    || entry.getKey().getType() == EntityData.Type.HOLOGRAM) {
                final Entity e = entry.getValue();
                executeRegion(e, e::remove);
                entityDataList.add(entry.getKey());
            }
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }

    /**
     * Retrieves a map of equipment slots and their corresponding item stacks for a living entity and grave.
     *
     * @param livingEntity the living entity to retrieve the equipment for.
     * @param grave        the grave associated with the equipment.
     * @return a map of equipment slots and their corresponding item stacks.
     */
    public Map<EquipmentSlot, ItemStack> getEquipmentMap(LivingEntity livingEntity, Grave grave) {
        Map<EquipmentSlot, ItemStack> equipmentSlotItemStackMap = new HashMap<>();

        if (livingEntity.getEquipment() != null) {
            EntityEquipment entityEquipment = livingEntity.getEquipment();

            if (entityEquipment.getHelmet() != null
                    && grave.getInventory().contains(entityEquipment.getHelmet())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.HEAD, entityEquipment.getHelmet());
            }

            if (entityEquipment.getChestplate() != null
                    && grave.getInventory().contains(entityEquipment.getChestplate())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.CHEST, entityEquipment.getChestplate());
            }

            if (entityEquipment.getLeggings() != null
                    && grave.getInventory().contains(entityEquipment.getLeggings())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.LEGS, entityEquipment.getLeggings());
            }

            if (entityEquipment.getBoots() != null
                    && grave.getInventory().contains(entityEquipment.getBoots())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.FEET, entityEquipment.getBoots());
            }

            if (plugin.getVersionManager().hasSecondHand()) {
                if (entityEquipment.getItemInMainHand().getType() != Material.AIR
                        && grave.getInventory().contains(entityEquipment.getItemInMainHand())) {
                    equipmentSlotItemStackMap.put(EquipmentSlot.HAND, entityEquipment.getItemInMainHand());
                }

                if (entityEquipment.getItemInOffHand().getType() != Material.AIR
                        && grave.getInventory().contains(entityEquipment.getItemInOffHand())) {
                    equipmentSlotItemStackMap.put(EquipmentSlot.OFF_HAND, entityEquipment.getItemInOffHand());
                }
            } else {
                if (entityEquipment.getItemInHand().getType() != Material.AIR
                        && grave.getInventory().contains(entityEquipment.getItemInHand())) {
                    equipmentSlotItemStackMap.put(EquipmentSlot.HAND, entityEquipment.getItemInHand());
                }
            }
        }

        return equipmentSlotItemStackMap;
    }

    /**
     * Returns the name of the specified entity.
     * <p>
     * This method handles different types of entities, including players and other entities, with legacy support for older versions of Minecraft.
     * </p>
     *
     * @param entity the {@link Entity} whose name is to be retrieved
     * @return the name of the entity, or "null" if the entity is null
     */
    @SuppressWarnings("redundant")
    public String getEntityName(Entity entity) {
        if (entity != null) {
            if (entity instanceof Player player) {
                return player.getName(); // Need redundancy for legacy support
            } else if (!plugin.getVersionManager().is_v1_7()) {
                return entity.getName();
            }
            return StringUtil.format(entity.getType().toString());
        }
        return "null";
    }

    /**
     * Checks if the specified entity has a persistent data string with the given key.
     * <p>
     * The method checks for persistent data if supported; otherwise, it checks for metadata.
     * </p>
     *
     * @param entity the {@link Entity} to check
     * @param string the key of the persistent data or metadata
     * @return {@code true} if the entity has the specified data string; {@code false} otherwise
     */
    public boolean hasDataString(Entity entity, String string) {
        return plugin.getVersionManager().hasPersistentData()
                ? entity.getPersistentDataContainer().has(new NamespacedKey(plugin, string), PersistentDataType.STRING)
                : entity.hasMetadata(string);
    }

    /**
     * Checks if the specified entity has a persistent data byte with the given key.
     * <p>
     * The method checks for persistent data if supported; otherwise, it checks for metadata.
     * </p>
     *
     * @param entity the {@link Entity} to check
     * @param string the key of the persistent data or metadata
     * @return {@code true} if the entity has the specified data byte; {@code false} otherwise
     */
    public boolean hasDataByte(Entity entity, String string) {
        return plugin.getVersionManager().hasPersistentData()
                ? entity.getPersistentDataContainer().has(new NamespacedKey(plugin, string), PersistentDataType.BYTE)
                : entity.hasMetadata(string);
    }

    /**
     * Retrieves the persistent data string associated with the given key from the specified entity.
     * <p>
     * If persistent data is supported, it retrieves the string from the persistent data container; otherwise, it retrieves it from metadata.
     * </p>
     *
     * @param entity the {@link Entity} to retrieve data from
     * @param key the key of the persistent data or metadata
     * @return the data string associated with the key, or {@code null} if not found
     */
    public String getDataString(Entity entity, String key) {
        if (plugin.getVersionManager().hasPersistentData()
                && entity.getPersistentDataContainer().has(new NamespacedKey(plugin, key), PersistentDataType.STRING)) {
            return entity.getPersistentDataContainer().get(new NamespacedKey(plugin, key), PersistentDataType.STRING);
        } else {
            return entity.getMetadata(key).toString();
        }
    }

    /**
     * Sets a persistent data string for the specified entity with the given key.
     * <p>
     * If persistent data is supported, it sets the string in the persistent data container; otherwise, it sets it in metadata.
     * </p>
     *
     * @param entity the {@link Entity} to set data for
     * @param key the key of the persistent data or metadata
     * @param string the data string to set
     */
    public void setDataString(Entity entity, String key, String string) {
        if (plugin.getVersionManager().hasPersistentData()) {
            entity.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, string);
        } else {
            entity.setMetadata(key, new FixedMetadataValue(plugin, string));
        }
    }

    /**
     * Sets a persistent data byte for the specified entity with the given key.
     * <p>
     * If persistent data is supported, it sets the byte in the persistent data container; otherwise, it sets it in metadata.
     * </p>
     *
     * @param entity the {@link Entity} to set data for
     * @param key the key of the persistent data or metadata
     */
    public void setDataByte(Entity entity, String key) {
        if (plugin.getVersionManager().hasPersistentData()) {
            entity.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.BYTE, (byte) 1);
        } else {
            entity.setMetadata(key, new FixedMetadataValue(plugin, (byte) 1));
        }
    }

    /**
     * Retrieves a {@link Grave} object from the persistent data or metadata of the specified entity.
     * <p>
     * The method checks if persistent data is supported and looks for a "graveUUID" key. If not found, it checks for metadata.
     * </p>
     *
     * @param entity the {@link Entity} from which to retrieve the grave
     * @return the {@link Grave} associated with the entity, or {@code null} if not found
     */
    public Grave getGraveFromEntityData(Entity entity) {
        if (plugin.getVersionManager().hasPersistentData()
                && entity.getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING)) {
            return plugin.getCacheManager().getGraveMap().get(UUIDUtil.getUUID(
                    entity.getPersistentDataContainer().get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING)));
        } else if (entity.hasMetadata("graveUUID")) {
            List<MetadataValue> metadataValue = entity.getMetadata("graveUUID");
            if (!metadataValue.isEmpty()) {
                return plugin.getCacheManager().getGraveMap().get(UUIDUtil.getUUID(metadataValue.get(0).asString()));
            }
        }
        return null;
    }

    private void executeRegion(Location loc, Runnable task) {
        var sched = plugin.getGravesXScheduler();
        if (sched != null) {
            sched.execute(loc, task);
        } else {
            plugin.getGravesXScheduler().runTask(task);
        }
    }

    private void executeRegion(Entity entity, Runnable task) {
        var sched = plugin.getGravesXScheduler();
        if (sched != null) {
            sched.execute(entity, task);
        } else {
            plugin.getGravesXScheduler().runTask(task);
        }
    }
}