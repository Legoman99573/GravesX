package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.compatibility.CompatibilityInventoryView;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.integration.MiniMessage;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.InventoryUtil;
import com.ranull.graves.util.MaterialUtil;
import dev.cwhead.GravesX.api.provider.GraveProvider;
import dev.cwhead.GravesX.api.provider.RegisterGraveProviders;
import dev.cwhead.GravesX.event.*;
import dev.cwhead.GravesX.exception.GravesXGraveProviderException;
import me.jay.GravesX.util.pluginsWithoutMavenReposOrUsefulApiDocsThatCauseBugs.ReflectSupportAE;
import com.ranull.graves.util.StringUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * Manages the operations and lifecycle of graves within the Graves plugin.
 */
public final class GraveManager {

    /**
     * The main plugin instance associated with Graves.
     * <p>
     * This {@link Graves} instance represents the core plugin that this Graves is part of. It provides access
     * to the plugin's functionality, configuration, and other services.
     * </p>
     */
    private final Graves plugin;

    /**
     * Determine all graves that have a grave present. Ignore further if the grave exists. This helps with massive graves.
     */
    private final Set<UUID> knownGraves = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Initializes the GraveManager with the specified plugin instance.
     *
     * @param plugin the Graves plugin instance.
     */
    public GraveManager(@NotNull Graves plugin) {
        this.plugin = plugin;
        startGraveTimer();
    }

    /**
     * Starts the grave timer task that periodically checks and updates graves.
     */
    private void startGraveTimer() {
        plugin.getGravesXScheduler().runTaskTimer(this::checkAndUpdateGraves, 20L, 20L);
    }

    /**
     * Checks and updates graves, entities, and blocks, removing expired elements and triggering necessary events.
     */
    private void checkAndUpdateGraves() {
        final List<Grave> graveRemoveList = new ArrayList<>();
        final List<EntityData> entityDataRemoveList = new ArrayList<>();
        final List<BlockData> blockDataRemoveList = new ArrayList<>();

        processGraves(graveRemoveList);

        processChunks(entityDataRemoveList, blockDataRemoveList);

        removeExpiredElements(graveRemoveList, entityDataRemoveList, blockDataRemoveList);

        if (!graveRemoveList.isEmpty() && plugin.getConfig("grave.check-missing-graves", graveRemoveList.getFirst()).getBoolean("grave.check-missing-graves", false)) {
            restoreMissingGraves();
            plugin.getHologramManager().purgeLingeringHolograms();
        }
    }

    /**
     * Processes all graves to check their remaining time and protection status.
     *
     * @param graveRemoveList the list to which graves to be removed will be added.
     */
    private void processGraves(List<Grave> graveRemoveList) {
        Collection<Grave> graves = plugin.getCacheManager().getGraveMap().values();

        for (Grave grave : new ArrayList<>(graves)) {
            long remainingTime = grave.getTimeAliveRemaining();

            if (remainingTime == -1L) {
                continue;
            }

            plugin.debugMessage("Checking grave: " + grave.getUUID() + " with remaining time: " + remainingTime, 2);

            if (remainingTime == 0L) {
                handleGraveTimeout(grave, graveRemoveList);
            }

            if (grave.getProtection() && grave.getTimeProtectionRemaining() == 0L) {
                toggleGraveProtection(grave);
            }
        }
    }

    /**
     * Handles the timeout or abandonment of a grave by firing the
     * GraveTimeoutEvent and then either dropping its contents (timeout)
     * or moving it into abandoned logic.
     *
     * @param grave           the grave to check.
     * @param graveRemoveList graves to remove get added here.
     */
    private void handleGraveTimeout(Grave grave, List<Grave> graveRemoveList) {
        long remaining = grave.getTimeAliveRemaining();

        if (remaining == -1L) {
            return;
        }

        plugin.debugMessage("GraveTimeout check for " + grave.getUUID(), 1);

        if (remaining > 0L) {
            return;
        }

        boolean dropOnTimeout = plugin.getConfig("drop.timeout", grave).getBoolean("drop.timeout", true);
        boolean abandonEnabled = plugin.getConfig("drop.abandon", grave).getBoolean("drop.abandon", false);

        if (abandonEnabled && dropOnTimeout) {
            plugin.debugMessage("Config 'drop.abandon' ignored because 'drop.timeout' is enabled", 2);
            abandonEnabled = false;
        }

        GraveTimeoutEvent tevModern = new GraveTimeoutEvent(grave);
        plugin.getServer().getPluginManager().callEvent(tevModern);

        com.ranull.graves.event.GraveTimeoutEvent tevLegacy = new com.ranull.graves.event.GraveTimeoutEvent(grave);
        plugin.getServer().getPluginManager().callEvent(tevLegacy);

        if (tevModern.isCancelled() || tevModern.isAddon() || tevLegacy.isCancelled() || tevLegacy.isAddon()) {
            plugin.debugMessage("GraveTimeoutEvent cancelled → infinite life for " + grave.getUUID(), 2);
            grave.setTimeAliveRemaining(-1L);
            return;
        }

        Location loc = (tevModern.hasLocation() ? tevModern.getLocation() : tevLegacy.getLocation());
        if (loc.getWorld() == null) {
            plugin.debugMessage("Invalid timeout location for " + grave.getUUID(), 2);
            return;
        }

        loc.getChunk().load(true);

        boolean abandonEnabledSnapshot = abandonEnabled;
        plugin.getGravesXScheduler().runTask(() -> {
            org.bukkit.Chunk chunk = loc.getChunk();
            if (!chunk.isLoaded()) {
                plugin.debugMessage("Chunk still not loaded at (" + chunk.getX() + "," + chunk.getZ() + ")", 2);
                return;
            }
            chunk.setForceLoaded(true);

            if (dropOnTimeout && !abandonEnabledSnapshot) {
                plugin.debugMessage("Dropping on timeout: " + grave.getUUID(), 2);
                dropGraveItems(loc, grave);
                dropGraveExperience(loc, grave);
                sendPlayerMessage(grave, "message.timeout", loc);
                graveRemoveList.add(grave);
                removeGrave(grave);
                chunk.setForceLoaded(false);
                return;
            }

            if (!dropOnTimeout && abandonEnabledSnapshot) {
                plugin.debugMessage("Abandoning grave: " + grave.getUUID(), 2);

                GraveAbandonedEvent aevModern = new GraveAbandonedEvent(grave);
                plugin.getServer().getPluginManager().callEvent(aevModern);

                com.ranull.graves.event.GraveAbandonedEvent aevLegacy = new com.ranull.graves.event.GraveAbandonedEvent(grave);
                plugin.getServer().getPluginManager().callEvent(aevLegacy);

                if (aevModern.isCancelled() || aevModern.isAddon() || aevLegacy.isCancelled() || aevLegacy.isAddon()) {
                    plugin.debugMessage("Abandon event cancelled for " + grave.getUUID(), 2);
                    dropGraveItems(loc, grave);
                    dropGraveExperience(loc, grave);
                    sendPlayerMessage(grave, "message.timeout", loc);
                    graveRemoveList.add(grave);
                    removeGrave(grave);
                } else {
                    org.bukkit.Location abandonLoc = (aevModern.hasLocation() ? aevModern.getLocation() : aevLegacy.getLocation());
                    sendPlayerMessage(grave, "message.grave-abandoned", abandonLoc != null ? abandonLoc : loc);
                    abandonGrave(grave);
                }
                chunk.setForceLoaded(false);
                return;
            }

            GraveExpiredEvent graveExpiredEvent = new GraveExpiredEvent(grave);
            plugin.getServer().getPluginManager().callEvent(graveExpiredEvent);

            if (!graveExpiredEvent.isCancelled() || !graveExpiredEvent.isAddon()) {
                plugin.debugMessage("Fallback drop for " + grave.getUUID() + " as drop.timeout and drop.abandon are false", 2);
                sendPlayerMessage(grave, "message.timeout", loc);
                graveRemoveList.add(grave);
                removeGrave(grave);
                chunk.setForceLoaded(false);
                return;
            }

            plugin.debugMessage("Fallback drop for " + grave.getUUID() + " was cancelled. Grave will now last forever.", 2);
            chunk.setForceLoaded(false);
        });
    }


    /**
     * Utility to send a message to the grave owner if they're online.
     *
     * @param grave      the grave whose owner should be notified
     * @param messageKey the message key to resolve from messages configuration
     * @param location   the relevant location to include with the message
     */
    private void sendPlayerMessage(Grave grave, String messageKey, Location location) {
        if (grave.getOwnerType() == EntityType.PLAYER && grave.getOwnerUUID() != null) {
            Player player = plugin.getServer().getPlayer(grave.getOwnerUUID());
            if (player != null && player.isOnline()) {
                plugin.getEntityManager().sendMessage(messageKey, player, location, grave);
            }
        }
    }

    /**
     * Processes all chunks to handle entities and blocks within them.
     *
     * @param entityDataRemoveList the list to which entity data to be removed will be added.
     * @param blockDataRemoveList  the list to which block data to be removed will be added.
     */
    private void processChunks(List<EntityData> entityDataRemoveList, List<BlockData> blockDataRemoveList) {
        Collection<ChunkData> chunks = plugin.getCacheManager().getChunkMap().values();
        for (ChunkData chunkData : chunks) {
            Location anchor = new Location(chunkData.getWorld(), chunkData.getX() << 4, 0.0D, chunkData.getZ() << 4);
            if (plugin.getVersionManager().isFolia()) {
                plugin.getGravesXScheduler().execute(anchor, () -> {
                    if (!chunkData.isLoaded()) return;

                    processEntityData(chunkData, entityDataRemoveList, anchor);
                    processBlockData(chunkData, blockDataRemoveList);
                });
            } else {
                if (!chunkData.isLoaded()) return;

                processEntityData(chunkData, entityDataRemoveList, anchor);
                processBlockData(chunkData, blockDataRemoveList);
            }
        }
    }

    /**
     * Removes expired graves, entities, and blocks from the system.
     *
     * @param graveRemoveList       the list of graves to be removed.
     * @param entityDataRemoveList  the list of entity data to be removed.
     * @param blockDataRemoveList   the list of block data to be removed.
     */
    private void removeExpiredElements(List<Grave> graveRemoveList, List<EntityData> entityDataRemoveList, List<BlockData> blockDataRemoveList) {
        List<Grave> gravesSnapshot = new ArrayList<>(graveRemoveList);
        List<EntityData> entitySnapshot = new ArrayList<>(entityDataRemoveList);
        List<BlockData> blockSnapshot = new ArrayList<>(blockDataRemoveList);

        graveRemoveList.clear();
        entityDataRemoveList.clear();
        blockDataRemoveList.clear();

        for (Grave grave : gravesSnapshot) {
            plugin.debugMessage("Removing grave: " + grave.getUUID(), 2);

            Location graveLoc = null;
            try {
                graveLoc = grave.getLocationDeath();
            } catch (Throwable ignored) {
                // ignored
            }

            if (graveLoc != null && graveLoc.getWorld() != null) {
                Location anchor = new Location(graveLoc.getWorld(), graveLoc.getX(), graveLoc.getY(), graveLoc.getZ());
                plugin.getGravesXScheduler().execute(anchor, () -> removeGrave(grave));
            } else {
                plugin.getGravesXScheduler().runTask(() -> removeGrave(grave));
            }
        }

        for (EntityData entityData : entitySnapshot) {
            if (entityData == null) {
                plugin.debugMessage("Attempted to remove null EntityData", 2);
                continue;
            }

            Location entityLoc = null;
            try {
                entityLoc = entityData.getLocation();
            } catch (Throwable ignored) {
            }

            if (entityLoc != null && entityLoc.getWorld() != null) {
                Location anchor = new Location(entityLoc.getWorld(), entityLoc.getX(), entityLoc.getY(), entityLoc.getZ());
                plugin.getGravesXScheduler().execute(anchor, () -> removeEntityData(entityData));
            } else {
                plugin.getGravesXScheduler().runTask(() -> removeEntityData(entityData));
            }
        }

        for (BlockData blockData : blockSnapshot) {
            Location blockLoc = null;
            try {
                blockLoc = blockData.getLocation();
            } catch (Throwable ignored) {
            }

            if (blockLoc != null && blockLoc.getWorld() != null) {
                Location anchor = new Location(blockLoc.getWorld(), blockLoc.getX(), blockLoc.getY(), blockLoc.getZ());
                plugin.getGravesXScheduler().execute(anchor, () -> plugin.getBlockManager().removeBlock(blockData));
            } else {
                plugin.getGravesXScheduler().runTask(() -> plugin.getBlockManager().removeBlock(blockData));
            }
        }

        plugin.getGravesXScheduler().runTask(() -> plugin.getGUIManager().refreshMenus());
    }

    /**
     * Processes the entity data within the given chunk.
     *
     * @param chunkData             the data of the chunk being processed.
     * @param entityDataRemoveList  the list to which entity data to be removed will be added.
     * @param location              the location representing the chunk coordinates.
     */
    private void processEntityData(ChunkData chunkData, List<EntityData> entityDataRemoveList, Location location) {
        try {
            Collection<EntityData> values = chunkData.getEntityDataMap().values();

            for (EntityData entityData : new ArrayList<>(values)) {
                if (entityData == null) {
                    plugin.debugMessage("Encountered null EntityData while processing chunk at coordinates: (" + chunkData.getX() + ", " + chunkData.getZ() + ").", 2);
                    continue;
                }

                boolean hasGrave = entityData.getUUIDGrave() != null && plugin.getCacheManager().getGraveMap().containsKey(entityData.getUUIDGrave());

                if (hasGrave) {
                    if (entityData instanceof HologramData hologramData) {
                        processHologramData(hologramData, location, entityDataRemoveList);
                    }
                } else {
                    entityDataRemoveList.add(entityData);
                }
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
            // ignored
        }
    }

    /**
     * Processes hologram data within the chunk.
     *
     * @param hologramData          the hologram data to be processed.
     * @param location              the location representing the chunk coordinates.
     * @param entityDataRemoveList  the list to which hologram data to be removed will be added.
     */
    private void processHologramData(HologramData hologramData, Location location, List<EntityData> entityDataRemoveList) {
        try {
            Grave grave = plugin.getCacheManager().getGraveMap().get(hologramData.getUUIDGrave());
            if (grave == null) return;

            List<String> lineList = plugin.getConfig("hologram.line", grave).getStringList("hologram.line");
            Collections.reverse(lineList);

            if (plugin.getVersionManager().isFolia()) {
                plugin.getGravesXScheduler().execute(location, () -> {
                    Chunk chunk = hologramData.getLocation().getChunk();
                    for (Entity entity : chunk.getEntities()) {
                        if (!entity.getUniqueId().equals(hologramData.getUUIDEntity())) continue;

                        int lineIndex = hologramData.getLine();
                        if (lineIndex < lineList.size()) {
                            String lineText = StringUtil.parseString(lineList.get(lineIndex), location, grave, plugin);

                            if (plugin.getIntegrationManager().hasMiniMessage()) {
                                entity.setCustomName(MiniMessage.parseString(lineText));
                            } else {
                                entity.setCustomName(lineText);
                            }
                        } else {
                            entityDataRemoveList.add(hologramData);
                        }
                    }
                });
            } else {
                Chunk chunk = hologramData.getLocation().getChunk();
                for (Entity entity : chunk.getEntities()) {
                    if (!entity.getUniqueId().equals(hologramData.getUUIDEntity())) continue;

                    int lineIndex = hologramData.getLine();
                    if (lineIndex < lineList.size()) {
                        String lineText = StringUtil.parseString(lineList.get(lineIndex), location, grave, plugin);

                        if (plugin.getIntegrationManager().hasMiniMessage()) {
                            entity.setCustomName(MiniMessage.parseString(lineText));
                        } else {
                            entity.setCustomName(lineText);
                        }
                    } else {
                        entityDataRemoveList.add(hologramData);
                    }
                }
            }

        } catch (ArrayIndexOutOfBoundsException | IllegalStateException ignored) {

        }
    }

    /**
     * Processes the block data within the given chunk.
     *
     * @param chunkData             the data of the chunk being processed.
     * @param blockDataRemoveList   the list to which block data to be removed will be added.
     */
    private void processBlockData(ChunkData chunkData, List<BlockData> blockDataRemoveList) {
        try {
            Collection<BlockData> values = chunkData.getBlockDataMap().values();

            for (BlockData blockData : new ArrayList<>(values)) {
                if (blockData == null) {
                    continue;
                }

                Location loc = blockData.getLocation();
                if (loc == null || loc.getWorld() == null) {
                    blockDataRemoveList.add(blockData);
                    continue;
                }

                UUID graveId = blockData.getGraveUUID();
                boolean hasGrave = graveId != null && plugin.getCacheManager().getGraveMap().containsKey(graveId);

                if (hasGrave) {
                    Grave grave = plugin.getCacheManager().getGraveMap().get(graveId);
                    graveParticle(loc, grave);
                } else {
                    blockDataRemoveList.add(blockData);
                }
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
            // ignored
        }
    }

    /**
     * Unloads all open grave inventories for online players.
     */
    @SuppressWarnings("ConstantConditions")
    public void unload() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            InventoryView view = player.getOpenInventory();
            if (view == null) {
                continue;
            }

            Inventory topInventory = CompatibilityInventoryView.getTopInventory(view);
            if (topInventory == null) {
                continue;
            }

            InventoryHolder inventoryHolder = topInventory.getHolder();
            if (inventoryHolder == null) {
                continue;
            }

            try {
                if (inventoryHolder instanceof Grave
                        || inventoryHolder instanceof GraveList
                        || inventoryHolder instanceof GraveMenu) {
                    player.closeInventory();
                }
            } catch (Exception ignored) {
                // ignore any platform-specific edge cases
            }
        }
    }

    /**
     * Toggles the protection state of a grave.
     *
     * @param grave the grave to toggle protection for.
     */
    public void toggleGraveProtection(Grave grave) {
        boolean currentProtection = grave.getProtection();
        long protectionRemaining = grave.getTimeProtectionRemaining();

        grave.setProtection(!currentProtection);
        plugin.getDataManager().updateGrave(grave, "protection", grave.getProtection() ? 1 : 0);

        if (protectionRemaining == -1L) {
            plugin.debugMessage("Grave " + grave.getUUID() + " has infinite protection, skipping protection remaining handling.", 2);
            return;
        }

        if (currentProtection) {
            GraveProtectionExpiredEvent modern = new GraveProtectionExpiredEvent(grave);
            plugin.getServer().getPluginManager().callEvent(modern);

            com.ranull.graves.event.GraveProtectionExpiredEvent legacy = new com.ranull.graves.event.GraveProtectionExpiredEvent(grave);
            plugin.getServer().getPluginManager().callEvent(legacy);

            boolean cancelled = modern.isCancelled() || legacy.isCancelled();
            boolean addon = modern.isAddon() || legacy.isAddon();

            if (cancelled && !addon) {
                grave.setProtection(true);
                plugin.debugMessage("GraveProtectionExpiredEvent called for grave: " + grave.getUUID(), 2);
                plugin.getDataManager().updateGrave(grave, "protection", 1);
                grave.setTimeProtection(-1L);
            } else if (!cancelled && !addon) {
                plugin.debugMessage("Grave protection expired for grave: " + grave.getUUID(), 1);
                plugin.getDataManager().updateGrave(grave, "protection", grave.getProtection() ? 1 : 0);
            }
        }
    }

    /**
     * Abandons a grave.
     *
     * @param grave the grave to abandon.
     */
    public void abandonGrave(Grave grave) {
        grave.setAbandoned(true);

        if (plugin.getConfig("drop.abandon-lose-experience", grave).getBoolean("drop.abandon-lose-experience", true)) {
            grave.setExperience(0);
        }

        grave.setTimeProtection(0);
        grave.setTimeCreation(System.currentTimeMillis());
        grave.setTimeAlive(-1L);
        grave.setTimeAliveRemaining(-1L);
        grave.setOwnerName("Abandoned");
        grave.setOwnerDisplayName("Abandoned");
        grave.setOwnerTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTYxZjFmY2Q0MmY0OGNhNTFmOWRhN2M1NWI3MmYzNWE4MjZlNzViNmEwMjA0OGExZGVhNWQ3MTE5YmM5Y2Q2OSJ9fX0=");

        plugin.getDataManager().updateGrave(grave, "owner_name", grave.getOwnerName());
        plugin.getDataManager().updateGrave(grave, "experience", grave.getExperience());
        plugin.getDataManager().updateGrave(grave, "owner_name_display", grave.getOwnerDisplayName());
        plugin.getDataManager().updateGrave(grave, "is_abandoned", grave.isAbandoned() ? 1 : 0);

        // plugin.getDataManager().loadGraveMap();
    }
    /**
     * Spawns particle effects around a grave.
     * <p>
     * Folia-compatible: the actual particle spawning is scheduled on the correct
     * region thread via the GravesX scheduler using the provided location.
     * </p>
     *
     * @param location the location of the grave.
     * @param grave    the grave to spawn particles for.
     */
    public void graveParticle(Location location, Grave grave) {
        if (!plugin.getVersionManager().hasParticle()
                || location == null
                || location.getWorld() == null
                || !plugin.getConfig("particle.enabled", grave).getBoolean("particle.enabled")) {
            return;
        }

        Particle particle = plugin.getVersionManager().getParticleForVersion("REDSTONE");

        String configuredType = plugin.getConfig("particle.type", grave).getString("particle.type");
        if (configuredType != null && !configuredType.isEmpty()) {
            try {
                particle = plugin.getVersionManager().getParticleForVersion(configuredType);
            } catch (IllegalArgumentException ignored) {
                plugin.debugMessage(configuredType + " is not a Particle ENUM", 1);
                plugin.getLogger().severe("The Particle ENUM/INSTANCE " + configuredType + " is not valid. Update \"particle.type\" in grave.yml");
                return;
            }
        }

        int count = plugin.getConfig("particle.count", grave).getInt("particle.count");
        double offX = plugin.getConfig("particle.offset.x", grave).getDouble("particle.offset.x");
        double offY = plugin.getConfig("particle.offset.y", grave).getDouble("particle.offset.y");
        double offZ = plugin.getConfig("particle.offset.z", grave).getDouble("particle.offset.z");

        Location anchor = location.clone().add(offX + 0.5D, offY + 0.5D, offZ + 0.5D);
        if (anchor.getWorld() == null) {
            return;
        }

        Particle finalParticle = particle;
        plugin.getGravesXScheduler().execute(anchor, () -> {
            try {
                org.bukkit.World world = anchor.getWorld();
                if (world == null) {
                    return;
                }

                switch (finalParticle.name()) {
                    case "BLOCK":
                    case "BLOCK_CRUMBLE":
                    case "BLOCK_MARKER":
                    case "DUST_PILLAR":
                    case "FALLING_DUST": {
                        BlockData gravesBD = plugin.getParticleManager().getGravesBlockData(grave, anchor);
                        try {
                            world.spawnParticle(finalParticle, anchor, count, gravesBD);
                            break;
                        } catch (IllegalArgumentException badType) {
                            org.bukkit.block.data.BlockData bukkitBD = plugin.getParticleManager().toBukkitBlockData(gravesBD);
                            if (bukkitBD != null) {
                                world.spawnParticle(finalParticle, anchor, count, bukkitBD);
                            } else {
                                world.spawnParticle(finalParticle, anchor, count);
                            }
                        }
                        break;
                    }

                    case "DUST":
                    case "REDSTONE": {
                        int sizeInt = plugin.getConfig("particle.dust-size", grave).getInt("particle.dust-size");
                        float size = (float) sizeInt;
                        Color color = plugin.getParticleManager().safeColor(
                                plugin.getConfig("particle.dust-color", grave).getString("particle.dust-color", "RED"),
                                Color.RED
                        );
                        try {
                            world.spawnParticle(finalParticle, anchor, count, new Particle.DustOptions(color, size));
                        } catch (IllegalArgumentException e) {
                            world.spawnParticle(finalParticle, anchor, count, 1);
                        }
                        break;
                    }

                    case "DUST_COLOR_TRANSITION": {
                        float size = (float) plugin.getConfig("particle.dust-size", grave)
                                .getDouble("particle.dust-size", 1.0D);
                        Color from = plugin.getParticleManager().safeColor(
                                plugin.getConfig("particle.dust.from-color", grave).getString("particle.dust.from-color", "WHITE"),
                                Color.WHITE
                        );
                        Color to = plugin.getParticleManager().safeColor(
                                plugin.getConfig("particle.dust.to-color", grave).getString("particle.dust.to-color", "RED"),
                                Color.RED
                        );
                        Particle.DustTransition transition = new Particle.DustTransition(from, to, size);
                        world.spawnParticle(finalParticle, anchor, count, transition);
                        break;
                    }

                    case "ENTITY_EFFECT":
                    case "TINTED_LEAVES": {
                        Color tint = plugin.getParticleManager().safeColor(
                                plugin.getConfig("particle.color", grave).getString("particle.color", "WHITE"),
                                Color.WHITE
                        );
                        try {
                            world.spawnParticle(finalParticle, anchor, count, tint);
                        } catch (java.lang.IllegalArgumentException ex) {
                            world.spawnParticle(finalParticle, anchor, count);
                        }
                        break;
                    }

                    case "ITEM": {
                        ItemStack stack = plugin.getParticleManager().parseItemStack(grave);
                        if (stack != null) {
                            world.spawnParticle(finalParticle, anchor, count, stack);
                        } else {
                            world.spawnParticle(finalParticle, anchor, count);
                        }
                        break;
                    }

                    case "SCULK_CHARGE": {
                        float charge = (float) plugin.getConfig("particle.sculk-charge", grave).getDouble("particle.sculk-charge", 1.0D);
                        try {
                            world.spawnParticle(finalParticle, anchor, count, charge);
                        } catch (java.lang.IllegalArgumentException ex) {
                            world.spawnParticle(finalParticle, anchor, count);
                        }
                        break;
                    }

                    case "SHRIEK": {
                        int delay = plugin.getConfig("particle.shriek-delay", grave).getInt("particle.shriek-delay", 1);
                        try {
                            world.spawnParticle(finalParticle, anchor, count, delay);
                        } catch (IllegalArgumentException ex) {
                            world.spawnParticle(finalParticle, anchor, count, 1);
                        }
                        break;
                    }

                    case "TRAIL": {
                        Object trail = plugin.getParticleManager().parseTrailData(plugin, grave);
                        if (trail != null) {
                            try {
                                world.spawnParticle(finalParticle, anchor, count, trail);
                                break;
                            } catch (IllegalArgumentException ex) {
                                plugin.debugMessage("TRAIL data not supported: " + ex.getMessage(), 2);
                            }
                        }
                        world.spawnParticle(finalParticle, anchor, count);
                        break;
                    }

                    case "VIBRATION": {
                        String mode = plugin.getConfig("particle.vibration.mode", grave).getString("particle.vibration.mode", "single").toLowerCase(java.util.Locale.ROOT);

                        if ("bounce".equals(mode)) {
                            plugin.getParticleManager().spawnVibrationBounce(plugin, anchor, grave, finalParticle, count);
                        } else {
                            Vibration vib = plugin.getParticleManager().buildVibrationSingle(plugin, anchor, grave);
                            if (vib != null) {
                                world.spawnParticle(finalParticle, anchor, count, vib);
                            } else {
                                world.spawnParticle(finalParticle, anchor, count);
                            }
                        }
                        break;
                    }

                    default: {
                        try {
                            world.spawnParticle(finalParticle, anchor, count);
                        } catch (java.lang.IllegalArgumentException e) {
                            world.spawnParticle(finalParticle, anchor, count, 0, 0, 0, 0);
                        }
                        break;
                    }
                }
            } catch (java.lang.Throwable t) {
                plugin.debugMessage("Particle spawn failed for " + finalParticle + ": " + t.getMessage(), 2);
            }
        });
    }

    /**
     * Removes the oldest grave.
     *
     * @param livingEntity the entity whose oldest grave should be removed
     */
    public void removeOldestGrave(LivingEntity livingEntity) {
        if (livingEntity == null) {
            return;
        }

        Grave toDel = plugin.getCacheManager().getOldestGrave(livingEntity.getUniqueId());

        if (toDel != null) {
            removeGrave(toDel);
        }
    }

    /**
     * Removes a grave and its associated data.
     *
     * @param grave the grave to remove.
     */
    public void removeGrave(Grave grave) {
        if (grave == null) return;

        plugin.debugMessage("Starting removal of grave: " + grave.getUUID(), 1);

        if (plugin.getConfig("grave.check-missing-graves", grave).getBoolean("grave.check-missing-graves", false)) {
            knownGraves.remove(grave.getUUID());
        }
        Location anchor = null;
        try {
            anchor = grave.getLocationDeath();
        } catch (Throwable ignored) {}

        Location finalAnchor = anchor;
        Runnable work = () -> {
            try {
                closeGrave(grave);

                plugin.getBlockManager().removeBlock(grave);
                plugin.getHologramManager().removeHologram(grave);
                plugin.getEntityManager().removeEntity(grave);

                plugin.getDataManager().removeGrave(grave);

                List<GraveProvider> providers = RegisterGraveProviders.getAll();
                if (!providers.isEmpty()) {
                    for (GraveProvider p : providers) {
                        try {
                            p.remove(grave);
                            if (!p.isPlaced(grave)) {
                                plugin.debugMessage("[CustomGraveProvider " + p.id()
                                        + " (order=" + p.order() + ")] removed successfully.", 1);

                                plugin.getDataManager().removeGrave(grave);
                                plugin.getCacheManager().getGraveMap().remove(grave.getUUID());
                                plugin.debugMessage("Grave " + grave.getUUID() + " removed from cache", 1);
                            } else {
                                GravesXGraveProviderException stillThere =
                                        GravesXGraveProviderException.forProvider(p, finalAnchor, grave, "isPlaced=true after remove()", null);
                                plugin.getLogger().log(Level.WARNING, stillThere.getMessage(), stillThere);
                            }
                        } catch (Throwable t) {
                            GravesXGraveProviderException wrapped =
                                    GravesXGraveProviderException.forProvider(p, finalAnchor, grave, t);
                            plugin.getLogger().log(Level.WARNING, wrapped.getMessage(), wrapped);
                        }
                    }
                }

                if (plugin.getIntegrationManager().hasMultiPaper()) {
                    plugin.getIntegrationManager().getMultiPaper().notifyGraveRemoval(grave);
                }
                if (plugin.getIntegrationManager().hasFurnitureLib()) {
                    plugin.getIntegrationManager().getFurnitureLib().removeFurniture(grave);
                }
                if (plugin.getIntegrationManager().hasFurnitureEngine()) {
                    plugin.getLogger().warning("You have FurnitureEngine enabled. ");
                    plugin.getIntegrationManager().getFurnitureEngine().removeFurniture(grave);
                }
                if (plugin.getIntegrationManager().hasItemsAdder()) {
                    plugin.getIntegrationManager().getItemsAdder().removeFurniture(grave);
                }
                if (plugin.getIntegrationManager().hasOraxen()) {
                    plugin.getIntegrationManager().getOraxen().removeFurniture(grave);
                }
                if (plugin.getIntegrationManager().hasNexo()) {
                    plugin.getIntegrationManager().getNexo().removeFurniture(grave);
                }
                if (plugin.getIntegrationManager().hasPlayerNPC()) {
                    plugin.getIntegrationManager().getPlayerNPC().removeCorpse(grave);
                }
                if (plugin.getIntegrationManager().hasFancyNpcs()) {
                    plugin.getIntegrationManager().getFancyNpcs().removeCorpse(grave);
                }
                if (plugin.getIntegrationManager().hasCitizensNPC()) {
                    plugin.getIntegrationManager().getCitizensNPC().removeCorpse(grave);
                }

                plugin.getCacheManager().getGraveMap().remove(grave.getUUID());
                plugin.debugMessage("Grave " + grave.getUUID() + " removed from cache", 1);
            } catch (Throwable t) {
                plugin.getLogger().warning("Error while removing grave " + grave.getUUID() + ": " + t.getMessage());
            }
        };

        if (anchor != null && anchor.getWorld() != null) {
            plugin.getGravesXScheduler().execute(anchor, work);
        } else {
            plugin.getGravesXScheduler().runTask(work);
        }
    }

    /**
     * Removes entity data associated with a grave.
     *
     * @param entityData the entity data to remove.
     */
    public void removeEntityData(EntityData entityData) {
        if (entityData == null || entityData.getType() == null) {
            plugin.debugMessage("Attempted to remove null entity data. This is not a bug", 3);
            return;
        }

        org.bukkit.Location anchor = null;
        try {
            anchor = entityData.getLocation();
        } catch (java.lang.Throwable ignored) {
        }

        java.lang.Runnable work = () -> {
            switch (entityData.getType()) {
                case HOLOGRAM: {
                    plugin.getHologramManager().removeHologram(entityData);
                    break;
                }
                case FURNITURELIB: {
                    plugin.getIntegrationManager().getFurnitureLib().removeEntityData(entityData);
                    break;
                }
                case FURNITUREENGINE: {
                    plugin.getIntegrationManager().getFurnitureEngine().removeEntityData(entityData);
                    break;
                }
                case ITEMSADDER: {
                    plugin.getIntegrationManager().getItemsAdder().removeEntityData(entityData);
                    break;
                }
                case ORAXEN: {
                    plugin.getIntegrationManager().getOraxen().removeEntityData(entityData);
                    break;
                }
                case NEXO: {
                    plugin.getIntegrationManager().getNexo().removeEntityData(entityData);
                    break;
                }
                case PLAYERNPC: {
                    plugin.getIntegrationManager().getPlayerNPC().removeEntityData(entityData);
                    break;
                }
                case CITIZENSNPC: {
                    plugin.getIntegrationManager().getCitizensNPC().removeEntityData(entityData);
                    break;
                }
                case CUSTOM: {
                    List<GraveProvider> providers = RegisterGraveProviders.getAll();
                    if (providers.isEmpty()) {
                        break;
                    }

                    for (GraveProvider p : providers) {
                        if (p == null) {
                            continue;
                        }
                        try {
                            if (p.supports(entityData) && p.removeEntityData(entityData)) {
                                plugin.getHologramManager().removeHologram(entityData);
                                return;
                            }
                        } catch (Throwable t) {
                            String pid;
                            try {
                                pid = String.valueOf(p.id());
                            } catch (java.lang.Throwable ignored) {
                                pid = p.getClass().getName();
                            }

                            plugin.getLogger().warning("[CustomGraveProvider " + pid + "] removeEntityData() failed: " + t.getMessage());
                            plugin.logStackTrace(t);
                        }
                    }
                    break;
                }
                default: {
                    // no-op
                    break;
                }
            }
        };

        if (anchor != null && anchor.getWorld() != null) {
            plugin.getGravesXScheduler().execute(anchor, work);
        } else {
            plugin.getGravesXScheduler().runTask(work);
        }
    }

    /**
     * Closes any open inventories associated with a grave.
     *
     * @param grave the grave to close inventories for.
     */
    @SuppressWarnings("ConstantConditions")
    public void closeGrave(Grave grave) {
        Inventory inv = grave.getInventory();
        if (inv != null) {
            List<HumanEntity> inventoryViewers = inv.getViewers();

            for (HumanEntity humanEntity : new ArrayList<>(inventoryViewers)) {
                try {
                    inv.getViewers().remove(humanEntity);
                } catch (Throwable ignored) {
                    // Defensive: some platforms may throw here
                }
                try {
                    humanEntity.closeInventory();
                } catch (Throwable ignored) {
                    // Defensive: ensure we don't break the loop
                }
                plugin.debugMessage("Closing grave " + grave.getUUID() + " for " + humanEntity.getName(), 1);
            }
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            InventoryView view = player.getOpenInventory();
            if (view == null) {
                continue;
            }

            Inventory topInventory = CompatibilityInventoryView.getTopInventory(view);
            if (topInventory == null) {
                continue;
            }

            InventoryHolder inventoryHolder = topInventory.getHolder();
            if (inventoryHolder instanceof GraveMenu graveMenu) {
                if (graveMenu.getGrave() != null
                        && graveMenu.getGrave().getUUID().equals(grave.getUUID())) {
                    player.closeInventory();
                }
            }
        }
    }

    /**
     * Creates a new grave for the specified entity and list of item stacks.
     *
     * @param entity        the entity to create the grave for.
     * @param itemStackList the list of item stacks to be included in the grave.
     * @return the created grave.
     */
    public Grave createGrave(Entity entity, List<ItemStack> itemStackList) {
        return createGrave(entity, itemStackList, plugin.getPermissionList(entity));
    }

    /**
     * Creates a new grave for the specified entity, list of item stacks, and permissions.
     *
     * @param entity          the entity to create the grave for.
     * @param itemStackList   the list of item stacks to be included in the grave.
     * @param permissionList  the list of permissions associated with the grave.
     * @return the created grave.
     */
    public Grave createGrave(Entity entity, List<ItemStack> itemStackList, List<String> permissionList) {
        Grave grave = new Grave(java.util.UUID.randomUUID());

        String entityName = plugin.getEntityManager().getEntityName(entity);
        UUID ownerUUID = entity.getUniqueId();
        String ownerName = entityName;
        String displayName = entity.getCustomName();

        if (entity instanceof Player player) {
            displayName = player.getDisplayName();
            if (plugin.getIntegrationManager().hasFloodgate()) {
                ownerUUID = plugin.getIntegrationManager().getFloodgate().getCorrectUniqueId(player);
                ownerName = plugin.getIntegrationManager().getFloodgate().getCorrectUsername(player);
            } else {
                ownerName = player.getName();
            }
        }

        grave.setOwnerType(entity.getType());
        grave.setOwnerName(ownerName);
        grave.setOwnerNameDisplay(displayName);
        grave.setOwnerUUID(ownerUUID);

        Location baseLocation = entity.getLocation().clone();
        String rawTitle = plugin.getConfig("gui.grave.title", entity, permissionList).getString("gui.grave.title");
        String parsedTitle = StringUtil.parseString(rawTitle, entity, baseLocation, grave, plugin);
        String storageModeStr = plugin.getConfig("storage.mode", entity, permissionList).getString("storage.mode");

        plugin.getGravesXScheduler().execute(baseLocation, () -> {
            try {
                grave.setInventory(createGraveInventory(
                        grave,
                        baseLocation,
                        itemStackList,
                        parsedTitle,
                        getStorageMode(storageModeStr)
                ));
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to create grave inventory for " + grave.getUUID() + ": " + t.getMessage());
                plugin.logStackTrace(t);
            }
        });

        plugin.debugMessage("Creating grave " + grave.getUUID() + " for entity " + entityName, 1);
        return grave;
    }

    /**
     * Restores graves that are in cache but missing from the world.
     */
    private void restoreMissingGraves() {
        Map<UUID, Grave> graveMap = plugin.getCacheManager().getGraveMap();
        if (graveMap == null || graveMap.isEmpty()) return;

        for (Grave grave : graveMap.values()) {
            if (grave == null) continue;

            UUID id = grave.getUUID();
            if (knownGraves.contains(id)) continue;

            Location loc;
            try {
                loc = grave.getLocationDeath();
            } catch (Throwable t) {
                plugin.debugMessage("Failed to get location for grave " + id + ": " + t.getMessage(), 2);
                continue;
            }

            if (loc == null || loc.getWorld() == null) {
                plugin.debugMessage("Cannot restore grave " + id + ": invalid location.", 2);
                continue;
            }

            boolean placed;
            try {
                placed = isGravePlaced(grave);
            } catch (Throwable t) {
                plugin.debugMessage("isGravePlaced threw for grave " + id + ": " + t.getMessage(), 2);
                placed = false;
            }

            if (!placed) {
                plugin.debugMessage("Grave " + id + " missing from world. Scheduling placement.", 1);

                final Location scheduleAnchor = loc;
                final Grave scheduleGrave = grave;

                plugin.getGravesXScheduler().execute(scheduleAnchor, () -> {
                    try {
                        if (isGravePlaced(scheduleGrave)) return; // double-check inside scheduler
                        plugin.getGraveManager().placeGrave(scheduleAnchor, scheduleGrave);
                        knownGraves.add(id); // mark as successfully placed
                    } catch (Throwable t) {
                        plugin.getLogger().warning("Failed to place grave " + id + ": " + t.getMessage());
                        plugin.logStackTrace(t);
                    }
                });
            } else {
                knownGraves.add(id);
            }
        }
    }

    /**
     * Determines if the grave is placed in the world by checking for any physical
     * block or entity presence at the grave's location. This includes checking for
     * plugin-provided furniture/blocks and NPC corpses.
     *
     * @param grave the grave to check.
     * @return true if a block or entity is present at the grave's location (including integrations).
     */
    public boolean isGravePlaced(Grave grave) {
        if (grave == null) return false;
        UUID id = grave.getUUID();
        if (knownGraves.contains(id)) return true;

        Location location = grave.getLocationDeath();
        if (location == null || location.getWorld() == null) return false;

        try {
            List<GraveProvider> providers = RegisterGraveProviders.getAll();
            if (providers != null) {
                for (GraveProvider p : providers) {
                    if (p != null) {
                        try {
                            if (p.isPlaced(grave)) {
                                knownGraves.add(id);
                                return true;
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            IntegrationManager im = plugin.getIntegrationManager();
            if (im.hasFurnitureLib() && im.getFurnitureLib().hasFurniture(grave)) {
                knownGraves.add(id); return true;
            }
            if (im.hasFurnitureEngine() && im.getFurnitureEngine().hasFurniture(grave)) {
                knownGraves.add(id); return true;
            }
            if (im.hasFancyNpcs() && im.getFancyNpcs().hasCorpse(grave)) {
                knownGraves.add(id); return true;
            }
            if (im.hasCitizensNPC() && im.getCitizensNPC().hasNPCCorpse(grave)) {
                knownGraves.add(id); return true;
            }
            if (im.hasItemsAdder() && (im.getItemsAdder().hasBlock(grave) || im.getItemsAdder().hasFurniture(grave))) {
                knownGraves.add(id); return true;
            }
            if (im.hasOraxen() && (im.getOraxen().hasBlock(grave) || im.getOraxen().hasFurniture(grave))) {
                knownGraves.add(id); return true;
            }
            if (im.hasNexo() && (im.getNexo().hasBlock(grave) || im.getNexo().hasFurniture(grave))) {
                knownGraves.add(id); return true;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Integration check failed in isGravePlaced: " + t.getMessage());
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        plugin.getGravesXScheduler().execute(location, () -> {
            try {
                Collection<Entity> nearby = location.getWorld().getNearbyEntities(location, 0.49, 0.49, 0.49);
                if (!nearby.isEmpty()) {
                    result.complete(true);
                    return;
                }

                Block block = location.getBlock();
                if (isHeadBlock(block)) {
                    try {
                        BlockState state = block.getState();
                        if (state instanceof Skull skull) {
                            String owner = grave.getOwnerName();
                            if (owner != null) {
                                try {
                                    Object owningPlayer = Skull.class.getMethod("getOwningPlayer").invoke(skull);
                                    if (owningPlayer != null) {
                                        String n = (String) owningPlayer.getClass().getMethod("getName").invoke(owningPlayer);
                                        if (owner.equalsIgnoreCase(n)) {
                                            result.complete(true);
                                            return;
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    } catch (Throwable ignored) {}
                }

                result.complete(false);
            } catch (Throwable t) {
                plugin.debugMessage("isGravePlaced region work failed for " + id + " → treating as placed. Reason: " + t, 2);
                result.complete(true);
            }
        });

        try {
            boolean placed = result.get(100, TimeUnit.MILLISECONDS);
            if (placed) knownGraves.add(id);
            return placed;
        } catch (Exception e) {
            plugin.debugMessage("isGravePlaced timed out or failed for " + id + " → assuming placed.", 2);
            return true;
        }
    }

    private boolean isHeadBlock(Block block) {
        final String n = block.getType().name();
        return "PLAYER_HEAD".equals(n)
                || "PLAYER_WALL_HEAD".equals(n)
                || "SKULL".equals(n)
                || "LEGACY_SKULL".equals(n);
    }

    /**
     * Retrieves the storage mode for a given string representation.
     *
     * @param string the string representation of the storage mode.
     * @return the corresponding storage mode.
     */
    public Grave.StorageMode getStorageMode(String string) {
        if (string == null || string.isEmpty()) {
            return Grave.StorageMode.EXACT;
        }

        try {
            Grave.StorageMode storageMode = Grave.StorageMode.valueOf(string.toUpperCase(java.util.Locale.ROOT));

            if (storageMode == Grave.StorageMode.CHESTSORT
                    && !plugin.getIntegrationManager().hasChestSort()) {
                return Grave.StorageMode.COMPACT;
            }

            return storageMode;
        } catch (IllegalArgumentException ignored) {
            return Grave.StorageMode.EXACT;
        }
    }

    /**
     * Places a grave at a specified location.
     *
     * @param location the location to place the grave.
     * @param grave    the grave to be placed.
     */
    public void placeGrave(Location location, Grave grave) {
        if (location == null || location.getWorld() == null || grave == null) return;

        Location anchor = location.clone();

        plugin.getGravesXScheduler().execute(anchor, () -> {
            try {
                plugin.getBlockManager().createBlock(anchor, grave);
                plugin.getHologramManager().createHologram(anchor, grave);
                plugin.getEntityManager().createArmorStand(anchor, grave);
                plugin.getEntityManager().createItemFrame(anchor, grave);

                List<GraveProvider> providers = RegisterGraveProviders.getAll();
                if (!providers.isEmpty()) {
                    for (GraveProvider p : providers) {
                        try {
                            p.place(anchor, grave);
                            if (p.isPlaced(grave)) {
                                plugin.debugMessage("[CustomGraveProvider " + p.id() + " (order=" + p.order() + ")] placed successfully.", 1);
                                return;
                            } else {
                                GravesXGraveProviderException noOp = GravesXGraveProviderException.forProvider(p, anchor, grave, "isPlaced=false", null);
                                plugin.debugMessage(noOp.getMessage(), 2);
                            }
                        } catch (Throwable t) {
                            GravesXGraveProviderException wrapped = GravesXGraveProviderException.forProvider(p, anchor, grave, t);
                            plugin.getLogger().log(Level.WARNING, wrapped.getMessage(), wrapped);
                        }
                    }
                }

                if (plugin.getIntegrationManager().hasFurnitureLib()) {
                    plugin.getIntegrationManager().getFurnitureLib().createFurniture(anchor, grave);
                }
                if (plugin.getIntegrationManager().hasFurnitureEngine()) {
                    plugin.getIntegrationManager().getFurnitureEngine().createFurniture(anchor, grave);
                }
                if (plugin.getIntegrationManager().hasItemsAdder()) {
                    plugin.getIntegrationManager().getItemsAdder().createFurniture(anchor, grave);
                }
                if (plugin.getIntegrationManager().hasOraxen()) {
                    plugin.getIntegrationManager().getOraxen().createFurniture(anchor, grave);
                }
                if (plugin.getIntegrationManager().hasNexo()) {
                    plugin.getIntegrationManager().getNexo().createFurniture(anchor, grave);
                }
                if (plugin.getIntegrationManager().hasPlayerNPC()) {
                    plugin.getIntegrationManager().getPlayerNPC().createCorpse(anchor, grave);
                }
                if (plugin.getIntegrationManager().hasCitizensNPC()) {
                    plugin.getIntegrationManager().getCitizensNPC().createCorpse(anchor, grave);
                }
                if (plugin.getIntegrationManager().hasFancyNpcs() && !plugin.getIntegrationManager().hasFloodgate()) {
                    plugin.getIntegrationManager().getFancyNpcs().createCorpse(grave.getUUID(), grave.getLocationDeath(), grave);
                }
                if (plugin.getIntegrationManager().hasFancyNpcs() && plugin.getIntegrationManager().hasFloodgate()) {
                    try {
                        if (plugin.getIntegrationManager().getFloodgate().isFloodgateId(grave.getOwnerUUID())) {
                            plugin.getIntegrationManager().getFancyNpcs().createBedrockcompatCorpse(
                                    grave.getUUID(), grave.getLocationDeath(), grave);
                        } else {
                            plugin.getIntegrationManager().getFancyNpcs().createCorpse(
                                    grave.getUUID(), grave.getLocationDeath(), grave);
                        }
                    } catch (Throwable ignored) {
                        plugin.getIntegrationManager().getFancyNpcs().createCorpse(
                                grave.getUUID(), grave.getLocationDeath(), grave);
                    }
                }

            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to place grave " + grave.getUUID() + ": " + t.getMessage());
                plugin.logStackTrace(t);
            }
        });
    }

    /**
     * Retrieves the grave inventory for a specified grave and living entity.
     *
     * @param grave                 the grave.
     * @param livingEntity          the living entity.
     * @param graveItemStackList    the list of item stacks to be included in the grave.
     * @param removedItemStackList  the list of item stacks to be removed.
     * @param permissionList        the list of permissions associated with the grave.
     * @return the created grave inventory.
     */
    public Inventory getGraveInventory(Grave grave,
            LivingEntity livingEntity,
            List<org.bukkit.inventory.ItemStack> graveItemStackList,
            List<org.bukkit.inventory.ItemStack> removedItemStackList,
            List<String> permissionList) {
        List<ItemStack> filterGraveItemStackList = filterGraveItemStackList(graveItemStackList, removedItemStackList, livingEntity, permissionList);

        String title;
        if (plugin.getIntegrationManager().hasMiniMessage()) {
            String newTitle = StringUtil.parseString(plugin.getConfig("gui.grave.title", grave).getString("gui.grave.title"), livingEntity, grave.getLocationDeath(), grave, plugin);
            title = MiniMessage.parseString(newTitle);
        } else {
            title = StringUtil.parseString(plugin.getConfig("gui.grave.title", grave).getString("gui.grave.title"), livingEntity, grave.getLocationDeath(), grave, plugin);
        }

        Grave.StorageMode storageMode = getStorageMode(plugin.getConfig("storage.mode", grave).getString("storage.mode"));

        return plugin.getGraveManager().createGraveInventory(grave, grave.getLocationDeath(), filterGraveItemStackList, title, storageMode);
    }

    /**
     * Creates a grave inventory with the specified parameters.
     *
     * @param inventoryHolder the holder of the inventory.
     * @param location        the location of the grave.
     * @param itemStackList   the list of item stacks to be included in the inventory.
     * @param title           the title of the inventory.
     * @param storageMode     the storage mode for the inventory.
     * @return the created inventory.
     */
    public Inventory createGraveInventory(InventoryHolder inventoryHolder, Location location, List<ItemStack> itemStackList, String title, Grave.StorageMode storageMode) {
        if (storageMode == Grave.StorageMode.COMPACT || storageMode == Grave.StorageMode.CHESTSORT) {

            Inventory tempInventory = plugin.getServer().createInventory(null, 54);
            int counter = 0;

            for (ItemStack itemStack : itemStackList) {
                if (getItemStacksSize(tempInventory.getContents()) < tempInventory.getSize()) {
                    if (itemStack != null && !MaterialUtil.isAir(itemStack.getType())) {
                        tempInventory.addItem(itemStack);
                        counter++;
                    }
                } else if (itemStack != null && location != null && location.getWorld() != null) {
                    location.getWorld().dropItem(location, itemStack);
                }
            }

            counter = 0;
            for (ItemStack itemStack : tempInventory.getContents()) {
                if (itemStack != null) {
                    counter++;
                }
            }

            Inventory inventory = plugin.getServer().createInventory(inventoryHolder, InventoryUtil.getInventorySize(counter), title);

            for (ItemStack itemStack : tempInventory.getContents()) {
                if (itemStack != null && location != null && location.getWorld() != null) {
                    inventory.addItem(itemStack).forEach((key, value) -> location.getWorld().dropItem(location, value));
                }
            }

            if (storageMode == Grave.StorageMode.CHESTSORT
                    && plugin.getIntegrationManager().hasChestSort()) {
                plugin.getIntegrationManager().getChestSort().sortInventory(inventory);
            }

            return inventory;

        } else if (storageMode == Grave.StorageMode.EXACT) {
            if (plugin.getVersionManager().hasEnchantmentCurse()) {
                itemStackList.removeIf(itemStack -> itemStack != null && itemStack.containsEnchantment(Enchantment.VANISHING_CURSE));
            }

            ItemStack itemStackAir = new ItemStack(org.bukkit.Material.AIR);
            Inventory inventory = plugin.getServer().createInventory(inventoryHolder, InventoryUtil.getInventorySize(itemStackList.size()), title);

            int counter = 0;
            for (ItemStack itemStack : itemStackList) {
                if (counter < inventory.getSize()) {
                    inventory.setItem(counter, itemStack != null ? itemStack : itemStackAir);
                } else if (itemStack != null && location != null && location.getWorld() != null) {
                    location.getWorld().dropItem(location, itemStack);
                }
                counter++;
            }

            return inventory;
        }

        return null;
    }

    /**
     * Gets the size of the item stacks array.
     *
     * @param itemStacks the array of item stacks.
     * @return the number of non-null item stacks in the array.
     */
    public int getItemStacksSize(ItemStack[] itemStacks) {
        if (itemStacks == null) {
            return 0;
        }

        int counter = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Filters the grave item stack list based on the living entity, removed item stacks, and permission list.
     *
     * @param itemStackList        the original list of item stacks.
     * @param removedItemStackList the list of item stacks to be removed.
     * @param livingEntity         the living entity.
     * @param permissionList       the list of permissions associated with the grave.
     * @return the filtered list of item stacks.
     */
    public List<ItemStack> filterGraveItemStackList(List<ItemStack> itemStackList, List<ItemStack> removedItemStackList, LivingEntity livingEntity, List<String> permissionList) {
        List<ItemStack> source = (itemStackList != null) ? new ArrayList<>(itemStackList) : new ArrayList<>();
        List<ItemStack> removed = (removedItemStackList != null) ? new ArrayList<>(removedItemStackList) : new ArrayList<>();

        if (livingEntity instanceof Player player && getStorageMode(plugin.getConfig("storage.mode", livingEntity, permissionList).getString("storage.mode")) == Grave.StorageMode.EXACT) {

            List<ItemStack> playerInventoryContentList = Arrays.asList(player.getInventory().getContents());

            List<ItemStack> itemStackListNew = new ArrayList<>(playerInventoryContentList);
            List<ItemStack> differenceList = new ArrayList<>(removed);

            differenceList.removeIf(source::contains);

            itemStackListNew.removeAll(differenceList);

            source.removeAll(playerInventoryContentList);

            if (!source.isEmpty()) {
                int counter = 0;

                for (ItemStack itemStack : new ArrayList<>(itemStackListNew)) {
                    if (!source.isEmpty()) {
                        if (itemStack == null) {
                            itemStackListNew.set(counter, source.get(0));
                        } else {
                            itemStackListNew.add(source.get(0));
                        }
                        source.remove(0);
                    }
                    counter++;
                }
            }

            return itemStackListNew;
        }

        return source;
    }

    /**
     * Breaks a grave at its death location.
     *
     * @param grave the grave to be broken.
     */
    public void breakGrave(Grave grave) {
        breakGrave(grave.getLocationDeath(), grave);
    }

    /**
     * Breaks a grave at the specified location.
     *
     * @param location the location to break the grave.
     * @param grave    the grave to be broken.
     */
    public void breakGrave(Location location, Grave grave) {
        if (grave == null) {
            return;
        }
        if (location == null || location.getWorld() == null) {
            removeGrave(grave);
            plugin.debugMessage("Grave " + grave.getUUID() + " broken (no valid location/world for drops)", 1);
            return;
        }

        Location anchor = location.clone();
        plugin.getGravesXScheduler().execute(anchor, () -> {
            try {
                dropGraveItems(anchor, grave);
                dropGraveExperience(anchor, grave);
            } catch (Throwable t) {
                plugin.debugMessage("Error while dropping items/XP for grave " + grave.getUUID() + ": " + t.getMessage(), 2);
                plugin.logStackTrace(t);
            } finally {
                removeGrave(grave);
                plugin.debugMessage("Grave " + grave.getUUID() + " broken", 1);
            }
        });
    }

    /**
     * Drops the items from a grave at the specified location.
     * <p>
     * Folia-compatible: item drops are executed on the correct region thread
     * using {@code location} as the anchor.
     * </p>
     *
     * @param location the location to drop the items.
     * @param grave    the grave containing the items.
     */
    public void dropGraveItems(Location location, Grave grave) {
        if (grave == null || location == null || location.getWorld() == null) {
            return;
        }

        Inventory inv = grave.getInventory();

        Location anchor = location.clone();
        plugin.getGravesXScheduler().execute(anchor, () -> {
            try {
                World world = anchor.getWorld();
                if (world == null) {
                    return;
                }

                for (ItemStack itemStack : inv.getContents()) {
                    if (itemStack != null) {
                        world.dropItemNaturally(anchor, itemStack);
                    }
                }

                inv.clear();
            } catch (Throwable t) {
                plugin.debugMessage("dropGraveItems failed for " + grave.getUUID() + ": " + t.getMessage(), 2);
                plugin.logStackTrace(t);
            }
        });
    }

    /**
     * Gives the experience from a grave to a player.
     *
     * @param player the player to receive the experience.
     * @param grave  the grave containing the experience.
     */
    public void giveGraveExperience(Player player, Grave grave) {
        if (player == null || grave == null) {
            return;
        }

        int xp = grave.getExperience();
        if (xp <= 0) {
            return;
        }

        Location anchor = player.getLocation();
        if (anchor.getWorld() == null) {
            plugin.getGravesXScheduler().runTask(() -> {
                if (!player.isOnline()) {
                    return;
                }
                player.giveExp(xp);
                grave.setExperience(0);
                plugin.getEntityManager().playWorldSound("ENTITY_EXPERIENCE_ORB_PICKUP", player);
            });
            return;
        }

        plugin.getGravesXScheduler().execute(anchor, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.giveExp(xp);
            grave.setExperience(0);
            plugin.getEntityManager().playWorldSound("ENTITY_EXPERIENCE_ORB_PICKUP", player);
        });
    }

    /**
     * Drops the experience from a grave at the specified location.
     *
     * @param location the location to drop the experience.
     * @param grave    the grave containing the experience.
     */
    public void dropGraveExperience(Location location, Grave grave) {
        if (grave == null || location == null || location.getWorld() == null) {
            return;
        }

        int xp = grave.getExperience();
        if (xp <= 0) {
            return;
        }

        Location anchor = location.clone();
        plugin.getGravesXScheduler().execute(anchor, () -> {
            try {
                World world = anchor.getWorld();
                if (world == null) {
                    return;
                }

                ExperienceOrb experienceOrb = (ExperienceOrb) world.spawnEntity(anchor, EntityType.EXPERIENCE_ORB);

                experienceOrb.setExperience(xp);
                grave.setExperience(0);
            } catch (Throwable t) {
                plugin.debugMessage("dropGraveExperience failed for " + grave.getUUID() + ": " + t.getMessage(), 2);
                plugin.logStackTrace(t);
            }
        });
    }

    /**
     * Retrieves a list of graves associated with a player.
     *
     * @param player the player to retrieve the graves for.
     * @return the list of graves.
     */
    public List<Grave> getGraveList(Player player) {
        return getGraveList(player.getUniqueId());
    }

    /**
     * Retrieves a list of graves associated with an offline player.
     *
     * @param player the offline player to retrieve the graves for.
     * @return the list of graves.
     */
    public List<Grave> getGraveList(OfflinePlayer player) {
        return getGraveList(player.getUniqueId());
    }

    /**
     * Retrieves a list of graves associated with an entity.
     *
     * @param entity the entity to retrieve the graves for.
     * @return the list of graves.
     */
    public List<Grave> getGraveList(Entity entity) {
        return getGraveList(entity.getUniqueId());
    }

    /**
     * Retrieves a list of graves associated with a UUID.
     *
     * @param uuid the UUID to retrieve the graves for.
     * @return the list of graves.
     */
    public List<Grave> getGraveList(UUID uuid) {
        List<Grave> graveList = new ArrayList<>();

        if (uuid == null) {
            return graveList;
        }

        plugin.getCacheManager().getGraveMap().forEach((key, value) -> {
            if (value != null
                    && !value.isAbandoned()
                    && value.getOwnerUUID() != null
                    && value.getOwnerUUID().equals(uuid)) {
                graveList.add(value);
            }
        });

        return graveList;
    }

    /**
     * Retrieves the number of graves associated with an entity.
     *
     * @param entity the entity to retrieve the grave count for.
     * @return the number of graves.
     */
    public int getGraveCount(Entity entity) {
        return getGraveList(entity).size();
    }

    /**
     * Opens a grave for a player.
     *
     * @param entity   the entity attempting to open the grave.
     * @param location the location of the grave.
     * @param grave    the grave to be opened.
     * @return true if the grave was opened successfully, false otherwise.
     */
    public boolean openGrave(Entity entity, Location location, Grave grave) {
        openGrave(entity, location, grave, false);
        return false;
    }

    /**
     * Opens a grave for a player.
     *
     * <p>
     * Folia-compatible: world/player interactions (auto-loot, opening inventories,
     * running commands, playing sounds) are executed on the correct region thread
     * using the provided {@code location} as the anchor. The method returns {@code true}
     * once the operation has been scheduled (if permitted), not necessarily after it completes.
     * </p>
     *
     * @param entity   the entity attempting to open the grave.
     * @param location the location of the grave.
     * @param grave    the grave to be opened.
     * @param preview  whether to open the grave in preview mode (when allowed by config).
     * @return true if the open/auto-loot operation was scheduled, false otherwise.
     */
    public boolean openGrave(Entity entity, Location location, Grave grave, boolean preview) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        Location anchor = (location != null ? location : player.getLocation());
        if (anchor.getWorld() == null) {
            return false;
        }

        plugin.getGravesXScheduler().execute(anchor, () -> plugin.getEntityManager().swingMainHand(player));

        if (plugin.getEntityManager().canOpenGrave(player, grave)) {
            plugin.getGravesXScheduler().execute(anchor, () -> {
                cleanupCompasses(player, grave);

                if (player.isSneaking() && plugin.hasGrantedPermission("graves.autoloot", player.getPlayer())) {
                    GraveAutoLootEvent modern = new GraveAutoLootEvent(player, location, grave);
                    plugin.getServer().getPluginManager().callEvent(modern);

                    com.ranull.graves.event.GraveAutoLootEvent legacy = new com.ranull.graves.event.GraveAutoLootEvent(player, location, grave);
                    plugin.getServer().getPluginManager().callEvent(legacy);

                    if (!modern.isCancelled() || !modern.isAddon() || !legacy.isCancelled() || !legacy.isAddon()) {
                        autoLootGrave(player, location, grave);

                        if (plugin.getIntegrationManager().hasNoteBlockAPI()) {
                            if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForPlayer(player)) {
                                plugin.getIntegrationManager().getNoteBlockAPI().stopSongForPlayer(player);
                            }
                            if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForAllPlayers()) {
                                plugin.getIntegrationManager().getNoteBlockAPI().stopSongForAllPlayers();
                            }
                        }
                    }
                } else if (plugin.hasGrantedPermission("graves.open", player.getPlayer())) {
                    if (plugin.getConfig("grave.preview", grave).getBoolean("grave.preview", false)) {
                        grave.setGravePreview(preview);
                    } else {
                        grave.setGravePreview(false);
                    }
                    player.openInventory(grave.getInventory());
                    plugin.getEntityManager().runCommands("event.command.open", player, location, grave);
                    plugin.getEntityManager().playWorldSound("sound.open", location, grave);
                }
            });

            return true;
        } else {
            plugin.getGravesXScheduler().execute(anchor, () -> {
                boolean protPreview = plugin.getConfig("protection.preview", grave).getBoolean("protection.preview", false);
                boolean gravePreview = plugin.getConfig("grave.preview", grave).getBoolean("grave.preview", false);

                if (protPreview && gravePreview) {
                    grave.setGravePreview(preview);
                    player.openInventory(grave.getInventory());
                    plugin.getEntityManager().runCommands("event.command.open", player, location, grave);
                    plugin.getEntityManager().playWorldSound("sound.open", location, grave);
                } else if (protPreview) {
                    grave.setGravePreview(false);
                    player.openInventory(grave.getInventory());
                    plugin.getEntityManager().runCommands("event.command.open", player, location, grave);
                    plugin.getEntityManager().playWorldSound("sound.open", location, grave);
                } else {
                    plugin.getEntityManager().sendMessage("message.protection", player, location, grave);
                    plugin.getEntityManager().playWorldSound("sound.protection", location, grave);
                }
            });
        }

        return false;
    }

    /**
     * Cleans up compasses from a player's inventory that are associated with a grave.
     *
     * @param player the player to clean up the compasses for.
     * @param grave  the grave associated with the compasses.
     */
    public void cleanupCompasses(Player player, Grave grave) {
        if (player == null || grave == null) {
            return;
        }

        Map<ItemStack, UUID> compasses = plugin.getEntityManager().getCompassesFromInventory(player);

        if (compasses.isEmpty()) {
            return;
        }

        for (Map.Entry<ItemStack, UUID> entry : compasses.entrySet()) {
            UUID target = entry.getValue();
            if (target != null && target.equals(grave.getUUID())) {
                try {
                    player.getInventory().remove(entry.getKey());
                } catch (java.lang.Throwable ignored) {
                    // Be defensive against platform quirks
                }
            }
        }
    }

    /**
     * Retrieves a list of locations associated with a grave.
     *
     * @param baseLocation the base location.
     * @param grave        the grave to retrieve the locations for.
     * @return the list of locations ordered by proximity in the same world, followed by other worlds.
     */
    public List<Location> getGraveLocationList(Location baseLocation, Grave grave) {
        if (baseLocation == null || grave == null) {
            return new ArrayList<>();
        }

        List<Location> locationList = new ArrayList<>(plugin.getBlockManager().getBlockList(grave));
        Map<Double, Location> locationMap = new HashMap<>();
        List<Location> otherWorldLocationList = new ArrayList<>();

        if (baseLocation.getWorld() != null) {
            if (!locationList.contains(grave.getLocationDeath())) {
                locationList.add(grave.getLocationDeath());
            }

            for (Location location : locationList) {
                if (location != null) {
                    if (location.getWorld() != null && baseLocation.getWorld().equals(location.getWorld())) {
                        locationMap.put(location.distanceSquared(baseLocation), location);
                    } else {
                        otherWorldLocationList.add(location);
                    }
                }
            }

            locationList = new ArrayList<>(new TreeMap<>(locationMap).values());
            locationList.addAll(otherWorldLocationList);
        }

        return locationList;
    }

    /**
     * Retrieves the nearest grave location to a specified location.
     *
     * @param location the base location.
     * @param grave    the grave to retrieve the location for.
     * @return the nearest grave location, or {@code null} if none found.
     */
    public Location getGraveLocation(Location location, Grave grave) {
        if (location == null || grave == null) {
            return null;
        }

        List<Location> locationList = plugin.getGraveManager().getGraveLocationList(location, grave);

        return (!locationList.isEmpty() ? locationList.get(0) : null);
    }

    /**
     * Automatically loots a grave for a player.
     *
     * @param entity   the entity looting the grave.
     * @param location the location of the grave.
     * @param grave    the grave to be looted.
     */
    public void autoLootGrave(Entity entity, Location location, Grave grave) {
        if (!(entity instanceof Player player)) {
            return;
        }

        Location anchor = (location != null ? location : player.getLocation());
        if (anchor.getWorld() == null) {
            return;
        }

        plugin.getGravesXScheduler().execute(anchor, () -> {
            Grave.StorageMode storageMode = getStorageMode(
                    plugin.getConfig("storage.mode", grave).getString("storage.mode")
            );

            if (storageMode == Grave.StorageMode.EXACT) {
                java.util.List<ItemStack> itemStackListLeftOver = new ArrayList<>();
                int counter = 0;
                int inventorySize = player.getInventory().getSize();

                for (ItemStack itemStack : grave.getInventory().getContents()) {
                    if (itemStack != null) {
                        if (player.getInventory().getItem(counter) == null) {
                            if (counter < inventorySize) {
                                player.getInventory().setItem(counter, itemStack);
                                grave.getInventory().remove(itemStack);

                                if (counter == 17 && plugin.getVersionManager().hasSecondHand()) {
                                    player.getInventory().setItem(17, itemStack);
                                    grave.getInventory().remove(itemStack);
                                }
                                if ((counter == 39 && com.ranull.graves.util.InventoryUtil.isHelmet(itemStack))
                                        || (counter == 38 && com.ranull.graves.util.InventoryUtil.isChestplate(itemStack))
                                        || (counter == 37 && com.ranull.graves.util.InventoryUtil.isLeggings(itemStack))
                                        || (counter == 36 && com.ranull.graves.util.InventoryUtil.isBoots(itemStack))) {
                                    com.ranull.graves.util.InventoryUtil.playArmorEquipSound(player, itemStack);
                                }
                            } else {
                                itemStackListLeftOver.add(itemStack);
                            }
                        } else {
                            itemStackListLeftOver.add(itemStack);
                        }
                    }
                    counter++;
                }

                grave.getInventory().clear();

                for (ItemStack itemStack : itemStackListLeftOver) {
                    for (Map.Entry<Integer, ItemStack> itemStackEntry : player.getInventory().addItem(itemStack).entrySet()) {
                        grave.getInventory().addItem(itemStackEntry.getValue()).forEach((key, value) -> player.getWorld().dropItem(player.getLocation(), value));
                    }
                }
            } else {
                InventoryUtil.equipArmor(grave.getInventory(), player);InventoryUtil.equipItems(grave.getInventory(), player);
            }

            player.updateInventory();

            plugin.getDataManager().updateGrave(grave, "inventory", InventoryUtil.inventoryToString(grave.getInventory()));

            plugin.getEntityManager().runCommands("event.command.open", player, location, grave);

            if (grave.getItemAmount() <= 0) {
                plugin.getEntityManager().runCommands("event.command.loot", player, location, grave);
                plugin.getEntityManager().sendMessage("message.looted", player, location, grave);
                plugin.getEntityManager().playWorldSound("sound.close", location, grave);
                plugin.getEntityManager().spawnZombie(location, player, player, grave);

                giveGraveExperience(player, grave);

                playEffect("effect.loot", location, grave);

                removeGrave(grave);
                closeGrave(grave);

                plugin.debugMessage("Grave " + grave.getUUID() + " autolooted by " + player.getName(), 1);
            } else {
                plugin.getEntityManager().playWorldSound("sound.open", location, grave);
            }
        });
    }

    /**
     * Checks if the given location is within configured blocks of any grave.
     *
     * @param location The location to check.
     * @return True if the location is within configured blocks of any grave, false otherwise.
     */
    public boolean isNearGrave(Location location) {
        return isNearGrave(location, null, null);
    }

    /**
     * Overload for player-specific checks.
     */
    public boolean isNearGrave(Location location, Player player) {
        return isNearGrave(location, player, null);
    }

    /**
     * Overload for block-specific checks.
     */
    public boolean isNearGrave(Location location, Block block) {
        return isNearGrave(location, null, block);
    }


    /**
     * Checks if the given location is within configured blocks of any grave.
     *
     * @param location The location to check.
     * @param player   Optional player to consider for additional logic.
     * @param block    Optional block to consider for additional logic.
     * @return true if the location is within the configured protection radius of any grave; false otherwise.
     */
    public boolean isNearGrave(Location location, Player player, Block block) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        try {
            Collection<Grave> graves = plugin.getCacheManager().getGraveMap().values();

            for (Grave grave : new ArrayList<>(graves)) {
                if (grave == null) {
                    continue;
                }

                if (!plugin.getConfig("grave.should-protect-radius", grave).getBoolean("grave.should-protect-radius", false)) {
                    continue;
                }

                Location base =
                        (player != null) ? player.getLocation()
                                : (block != null) ? block.getLocation()
                                : location;

                Location graveLocation = plugin.getGraveManager().getGraveLocation(base, grave);

                if (graveLocation == null
                        || graveLocation.getWorld() == null
                        || !graveLocation.getWorld().equals(location.getWorld())) {
                    continue;
                }

                int protectionRadius = plugin.getConfig("grave.protection-radius", grave).getInt("grave.protection-radius", 0);
                if (protectionRadius <= 0) {
                    continue;
                }

                if (Math.abs(graveLocation.getBlockX() - location.getBlockX()) <= protectionRadius
                        && Math.abs(graveLocation.getBlockY() - location.getBlockY()) <= protectionRadius
                        && Math.abs(graveLocation.getBlockZ() - location.getBlockZ()) <= protectionRadius) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Graceful fallback in case of any error
        }

        return false;
    }

    /**
     * Retrieves the damage reason for a specified damage cause and grave.
     *
     * @param damageCause the cause of the damage.
     * @param grave       the grave associated with the damage.
     * @return the damage reason.
     */
    public String getDamageReason(EntityDamageEvent.DamageCause damageCause, Grave grave) {
        List<String> reasons = plugin.getConfig("message.death-reason", grave).getStringList("message.death-reason");
        String causeName = damageCause.name();

        for (String reason : reasons) {
            if (reason.startsWith(causeName + ":")) {
                return reason.split(":", 2)[1].trim();
            }
        }

        return StringUtil.format(causeName);
    }

    /**
     * Plays an effect at a specified location.
     *
     * @param string   the effect string.
     * @param location the location to play the effect.
     */
    public void playEffect(String string, Location location) {
        playEffect(string, location, null);
    }

    /**
     * Plays an effect at a specified location for a grave.
     *
     * @param string   the effect string.
     * @param location the location to play the effect.
     * @param grave    the grave associated with the effect.
     */
    public void playEffect(String string, Location location, Grave grave) {
        playEffect(string, location, 0, grave);
    }

    /**
     * Plays an effect at a specified location with additional data for a grave.
     * <p>
     * Folia-compatible: effect playback is scheduled on the correct region thread
     * using {@code location} as the anchor.
     * </p>
     *
     * @param string   the effect string (config key or enum name).
     * @param location the location to play the effect.
     * @param data     additional data for the effect.
     * @param grave    the grave associated with the effect.
     */
    public void playEffect(String string, Location location, int data, Grave grave) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        String effectName = string;
        if (grave != null) {
            effectName = plugin.getConfig(string, grave).getString(string);
        }

        if (effectName == null || effectName.isEmpty()) {
            return;
        }

        final String resolved = effectName;
        plugin.getGravesXScheduler().execute(location, () -> {
            World world = location.getWorld();
            if (world == null) {
                return;
            }
            try {
                Effect effect = Effect.valueOf(resolved.toUpperCase(Locale.ROOT));
                world.playEffect(location, effect, data);
            } catch (IllegalArgumentException ex) {
                plugin.debugMessage(resolved.toUpperCase(Locale.ROOT) + " is not an Effect ENUM", 1);
            }
        });
    }

    /**
     * Checks if an item stack should be ignored based on the entity and permissions.
     *
     * @param itemStack      the item stack to check.
     * @param entity         the entity.
     * @param permissionList the list of permissions.
     * @return true if the item stack should be ignored, false otherwise.
     */
    public boolean shouldIgnoreItemStack(ItemStack itemStack, Entity entity, List<String> permissionList) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return true;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("AdvancedEnchantments")) {
            if (ReflectSupportAE.isSoulbound(itemStack)) {
                entity.getWorld().dropItem(entity.getLocation(), itemStack);
                return true;
            }
            if (ReflectSupportAE.hasWhitScroll(itemStack)) {
                return true;
            }
        }

        if (plugin.getConfig("ignore.item.material", entity, permissionList).getStringList("ignore.item.material").contains(itemStack.getType().name())) {
            return true;
        }

        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                if (itemMeta.hasDisplayName()) {
                    for (String s : plugin.getConfig("ignore.item.name", entity, permissionList)
                            .getStringList("ignore.item.name")) {
                        if (!s.isEmpty()
                                && itemMeta.getDisplayName().equals(StringUtil.parseString(s, plugin))) {
                            return true;
                        }
                    }

                    for (String s : plugin.getConfig("ignore.item.name-contains", entity, permissionList)
                            .getStringList("ignore.item.name-contains")) {
                        if (!s.isEmpty()
                                && itemMeta.getDisplayName().contains(StringUtil.parseString(s, plugin))) {
                            return true;
                        }
                    }
                }

                if (itemMeta.hasLore() && itemMeta.getLore() != null) {
                    List<String> loreLines = itemMeta.getLore();

                    for (String s : plugin.getConfig("ignore.item.lore", entity, permissionList)
                            .getStringList("ignore.item.lore")) {
                        if (!s.isEmpty()) {
                            String target = StringUtil.parseString(s, plugin);
                            for (String lore : loreLines) {
                                if (lore.equals(target)) {
                                    return true;
                                }
                            }
                        }
                    }

                    for (String s : plugin.getConfig("ignore.item.lore-contains", entity, permissionList)
                            .getStringList("ignore.item.lore-contains")) {
                        if (s.isEmpty()) continue;

                        String parsed = StringUtil.parseString(s, plugin);
                        String[] parts = parsed.split("\\n", -1);

                        if (parts.length == 1) {
                            String needle = parts[0];
                            for (String lore : loreLines) {
                                if (lore != null && !needle.isEmpty() && lore.contains(needle)) {
                                    return true;
                                }
                            }
                        } else {
                            int window = parts.length;
                            for (int start = 0; start <= loreLines.size() - window; start++) {
                                boolean allMatch = true;
                                for (int i = 0; i < window; i++) {
                                    String part = parts[i];
                                    String lore = loreLines.get(start + i);
                                    if (lore == null || (part != null && !part.isEmpty() && !lore.contains(part))) {
                                        allMatch = false;
                                        break;
                                    }
                                }
                                if (allMatch) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        List<String> enchantList = plugin
                .getConfig("ignore.item.enchantment", entity, permissionList)
                .getStringList("ignore.item.enchantment");

        List<String> enchantContainsList = plugin
                .getConfig("ignore.item.enchantment-contains", entity, permissionList)
                .getStringList("ignore.item.enchantment-contains");

        for (Map.Entry<Enchantment, Integer> entry : itemStack.getEnchantments().entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();

            String fullKey = enchantment.getKey().toString().toLowerCase(Locale.ROOT);
            String shortKey = enchantment.getKey().getKey().toLowerCase(Locale.ROOT);

            for (String configKeyRaw : enchantList) {
                String configKey = StringUtil.parseString(configKeyRaw, plugin).toLowerCase(Locale.ROOT);

                if (configKey.contains("@")) {
                    String[] parts = configKey.split("@");
                    if (parts.length == 2) {
                        String key = parts[0];
                        String lvl = parts[1];
                        if ((key.equals(fullKey) || key.equals(shortKey)) && String.valueOf(level).equals(lvl)) {
                            return true;
                        }
                    }
                }
            }

            for (String containsKeyRaw : enchantContainsList) {
                String containsKey = StringUtil.parseString(containsKeyRaw, plugin).toLowerCase(Locale.ROOT);
                if (!containsKey.isEmpty() && (fullKey.contains(containsKey) || shortKey.contains(containsKey))) {
                    return true;
                }
            }
        }

        List<String> nsKeys = plugin
                .getConfig("ignore.item.namespacedkeys", entity, permissionList)
                .getStringList("ignore.item.namespacedkeys");
        if (namespacedKeys(itemStack, nsKeys)) {
            return true;
        }

        List<String> nsKeysContains = plugin
                .getConfig("ignore.item.namespacedkeys-contains", entity, permissionList)
                .getStringList("ignore.item.namespacedkeys-contains");
        if (namespacedKeysContains(itemStack, nsKeysContains)) {
            return true;
        }

        return false;
    }
    /**
     * Gets a list of all graves.
     *
     * @return a snapshot list of all graves currently known to the cache (never {@code null}).
     */
    public List<Grave> getAllGraves() {
        Map<UUID, Grave> graveMap = plugin.getCacheManager().getGraveMap();
        if (graveMap == null || graveMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Grave> snapshot = new ArrayList<>(graveMap.values());

        snapshot.removeIf(Objects::isNull);
        return snapshot;
    }

    /**
     * Checks if a block should be ignored based on the entity and permissions.
     *
     * @param grave           checks to see if the following grave is abandoned.
     * @return true if grave is abandoned, false otherwise.
     */
    public boolean isGraveAbandoned(Grave grave) {
        return grave.isAbandoned();
    }

    /**
     * Exact match against any namespaced key on the item.
     * Accepts "namespace:key" or just "key".
     */
    private boolean namespacedKeys(ItemStack itemStack, List<String> keysToMatch) {
        if (itemStack == null || keysToMatch == null || keysToMatch.isEmpty()) {
            return false;
        }

        Set<String> haystack = extractNamespacedKeys(itemStack);
        if (haystack.isEmpty()) {
            return false;
        }

        for (String raw : keysToMatch) {
            String k = StringUtil.parseString(raw, plugin);
            if (k == null) {
                continue;
            }
            k = k.trim();
            if (k.isEmpty()) {
                continue;
            }
            k = k.toLowerCase(Locale.ROOT);
            if (haystack.contains(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Substring match against any namespaced key on the item.
     * Accepts partials like "silk" or "minecraft:".
     */
    private boolean namespacedKeysContains(ItemStack itemStack, List<String> patterns) {
        if (itemStack == null || patterns == null || patterns.isEmpty()) {
            return false;
        }

        Set<String> haystack = extractNamespacedKeys(itemStack);
        if (haystack.isEmpty()) {
            return false;
        }

        for (String raw : patterns) {
            String p = StringUtil.parseString(raw, plugin);
            if (p == null) {
                continue;
            }
            p = p.trim();
            if (p.isEmpty()) {
                continue;
            }
            p = p.toLowerCase(Locale.ROOT);

            for (String key : haystack) {
                if (key.contains(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Collects namespaced keys present on this item.
     * Includes enchantment keys (applied and stored) and PDC keys.
     * Keys are added in both "namespace:key" and "key" forms, lowercased.
     */
    private Set<String> extractNamespacedKeys(ItemStack itemStack) {
        Set<String> out = new HashSet<>();
        if (itemStack == null) {
            return out;
        }

        // Helper to add both full and short forms
        BiConsumer<NamespacedKey, Set<String>> addKey = (k, set) -> {
            if (k != null) {
                set.add(k.toString().toLowerCase(Locale.ROOT));
                set.add(k.getKey().toLowerCase(Locale.ROOT));
            }
        };

        for (Enchantment ench : itemStack.getEnchantments().keySet()) {
            NamespacedKey k;
            k = ench.getKey();
            if (k.getKey().isEmpty()) continue;
            addKey.accept(k, out);
        }

        ItemMeta meta = itemStack.getItemMeta();

        if (meta instanceof EnchantmentStorageMeta esm) {
            for (Enchantment ench : esm.getStoredEnchants().keySet()) {
                NamespacedKey k;
                k = ench.getKey();
                if (k.getKey().isEmpty()) continue;
                addKey.accept(k, out);
            }
        }

        if (meta != null) {
            try {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                for (NamespacedKey k : pdc.getKeys()) {
                    addKey.accept(k, out);
                }
            } catch (NoClassDefFoundError ignored) {
                // PDC not available on this platform/version
            }
        }

        return out;
    }
}