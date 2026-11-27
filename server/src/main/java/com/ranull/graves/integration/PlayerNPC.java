package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.listener.integration.playernpc.NPCInteractListener;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import dev.sergiferry.playernpc.api.NPC;
import dev.sergiferry.playernpc.api.NPCLib;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages NPC interactions and corpse creation related to player graves using NPCLib.
 * Extends EntityDataManager to handle entity data.
 *
 * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
 */
@Deprecated(since = "4.9.9.1")
public class PlayerNPC extends EntityDataManager {
    private final Graves plugin;
    private final NPCLib npcLib;
    private final NPCInteractListener npcInteractListener;

    /**
     * Constructs a new PlayerNPC instance with the specified Graves plugin.
     *
     * @param plugin The main Graves plugin instance.
     * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
     */
    @Deprecated(since = "4.9.9.1")
    public PlayerNPC(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
        this.npcLib = NPCLib.getInstance();
        this.npcInteractListener = new NPCInteractListener(plugin, this);

        if (!this.npcLib.isRegistered(plugin)) {
            this.npcLib.registerPlugin(plugin);
        }

        registerListeners();
    }

    /**
     * Registers the NPC interaction listeners.
     *
     * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
     */
    @Deprecated(since = "4.9.9.1")
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(npcInteractListener, plugin);
    }

    /**
     * Unregisters the NPC interaction listeners.
     *
     * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
     */
    @Deprecated(since = "4.9.9.1")
    public void unregisterListeners() {
        if (npcInteractListener != null) {
            HandlerList.unregisterAll(npcInteractListener);
        }
    }

    /**
     * Creates NPC corpses based on the cached entity data.
     *
     * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
     */
    @Deprecated(since = "4.9.9.1")
    public void createCorpses() {
        for (ChunkData chunkData : plugin.getCacheManager().getChunkMap().values()) {
            for (EntityData entityData : chunkData.getEntityDataMap().values()) {
                if (entityData.getType() != EntityData.Type.PLAYERNPC) {
                    continue;
                }
                Grave grave = plugin.getCacheManager().getGraveMap().get(entityData.getUUIDGrave());
                if (grave != null) {
                    createCorpse(entityData.getUUIDEntity(), entityData.getLocation(), grave, false);
                }
            }
        }
    }

    /**
     * Creates a new NPC corpse at the specified location with the given grave data.
     *
     * @param location The location to spawn the NPC.
     * @param grave    The grave data for the NPC.
     * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
     */
    @Deprecated(since = "4.9.9.1")
    public void createCorpse(Location location, Grave grave) {
        createCorpse(UUID.randomUUID(), location, grave, true);
    }

    /**
     * Creates a new NPC corpse with a specific UUID at the given location using the provided grave data.
     *
     * @param uuid             The UUID for the NPC.
     * @param location         The location to spawn the NPC.
     * @param grave            The grave data for the NPC.
     * @param createEntityData Whether to create entity data for the NPC.
     * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
     */
    @Deprecated(since = "4.9.9.1")
    public void createCorpse(UUID uuid, Location location, Grave grave, boolean createEntityData) {
        plugin.getSchedulerManager().runTask(() -> {
            if (!plugin.getConfigManager().getConfigSection("playernpc.corpse.enabled", grave).getBoolean("playernpc.corpse.enabled")
                    || grave.getOwnerType() != EntityType.PLAYER) {
                return;
            }

            Player player = plugin.getServer().getPlayer(grave.getOwnerUUID());
            Location npcLocation = location.clone();

            if (player == null || npcLocation.getWorld() == null
                    || npcLib.getGlobalNPC(plugin, grave.getUUID().toString()) != null) {
                return;
            }

            location.getBlock().setType(Material.AIR);

            NPC.Pose pose = NPC.Pose.SWIMMING;
            try {
                String poseName = plugin.getConfigManager().getConfigSection("playernpc.corpse.pose", grave)
                        .getString("playernpc.corpse.pose");
                if (poseName != null) {
                    pose = NPC.Pose.valueOf(poseName);
                }
            } catch (IllegalArgumentException ignored) {
                // keep default
            }

            try {
                double x = plugin.getConfigManager().getConfigSection("playernpc.corpse.offset.x", grave)
                        .getDouble("playernpc.corpse.offset.x");
                double y = plugin.getConfigManager().getConfigSection("playernpc.corpse.offset.y", grave)
                        .getDouble("playernpc.corpse.offset.y");
                double z = plugin.getConfigManager().getConfigSection("playernpc.corpse.offset.z", grave)
                        .getDouble("playernpc.corpse.offset.z");
                npcLocation.add(x, y, z);
            } catch (IllegalArgumentException handled) {
                npcLocation.add(0.5, -0.2, 0.5);
            }

            NPC.Global npc = npcLib.generateGlobalNPC(plugin, grave.getUUID().toString(), npcLocation);

            try {
                Optional<NPC.Skin.Custom> loaded = NPC.Skin.Custom.getLoadedSkin(plugin, grave.getOwnerUUID().toString());
                if (loaded.isPresent()) {
                    npc.setSkin(loaded.get());
                } else {
                    applySkinFromGraveOrFallback(npc, grave);
                }
            } catch (Exception e) {
                applySkinFromGraveOrFallback(npc, grave);
            }

            npc.setPose(pose);
            npc.setAutoCreate(true);
            npc.setAutoShow(true);
            npc.setCustomData(plugin, "grave_uuid", grave.getUUID().toString());

            boolean collide = plugin.getConfigManager().getConfigSection("playernpc.corpse.collide", grave)
                    .getBoolean("playernpc.corpse.collide");
            npc.setCollidable(collide);

            if (plugin.getConfigManager().getConfigSection("playernpc.corpse.armor", grave).getBoolean("playernpc.corpse.armor")) {
                if (grave.getEquipmentMap().containsKey(EquipmentSlot.HEAD)) {
                    npc.setHelmet(grave.getEquipmentMap().get(EquipmentSlot.HEAD));
                }
                if (grave.getEquipmentMap().containsKey(EquipmentSlot.CHEST)) {
                    npc.setChestplate(grave.getEquipmentMap().get(EquipmentSlot.CHEST));
                }
                if (grave.getEquipmentMap().containsKey(EquipmentSlot.LEGS)) {
                    npc.setLeggings(grave.getEquipmentMap().get(EquipmentSlot.LEGS));
                }
                if (grave.getEquipmentMap().containsKey(EquipmentSlot.FEET)) {
                    npc.setBoots(grave.getEquipmentMap().get(EquipmentSlot.FEET));
                }
            }

            if (plugin.getConfigManager().getConfigSection("playernpc.corpse.hand", grave).getBoolean("playernpc.corpse.hand")) {
                if (grave.getEquipmentMap().containsKey(EquipmentSlot.HAND)) {
                    npc.setItemInMainHand(grave.getEquipmentMap().get(EquipmentSlot.HAND));
                }
                if (plugin.getVersionManager().hasSecondHand()
                        && grave.getEquipmentMap().containsKey(EquipmentSlot.OFF_HAND)) {
                    npc.setItemInOffHand(grave.getEquipmentMap().get(EquipmentSlot.OFF_HAND));
                }
            }

            if (plugin.getConfigManager().getConfigSection("playernpc.corpse.glow.enabled", grave)
                    .getBoolean("playernpc.corpse.glow.enabled")) {
                try {
                    String colorName = plugin.getConfigManager().getConfigSection("playernpc.corpse.glow.color", grave)
                            .getString("playernpc.corpse.glow.color");
                    if (colorName != null) {
                        npc.setGlowing(true, ChatColor.valueOf(colorName));
                    } else {
                        npc.setGlowing(true);
                    }
                } catch (IllegalArgumentException ignored) {
                    npc.setGlowing(true);
                }
            }

            npc.forceUpdate();
            if (npcLocation.getWorld() != null) {
                plugin.debugMessage(
                        "Spawning PlayerNPC NPC for " + grave.getUUID() + " at "
                                + npcLocation.getWorld().getName() + ", "
                                + (npcLocation.getBlockX() + 0.5) + "x, "
                                + (npcLocation.getBlockY() + 0.5) + "y, "
                                + (npcLocation.getBlockZ() + 0.5) + "z", 1);
            }

            if (createEntityData) {
                createEntityData(location, uuid, grave.getUUID(), EntityData.Type.PLAYERNPC);
            }
        });
    }

    /**
     * Removes the NPC corpse associated with the given grave.
     *
     * @param grave The grave whose associated NPC corpse should be removed.
     * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
     */
    @Deprecated(since = "4.9.9.1")
    public void removeCorpse(Grave grave) {
        Optional<NPC.Global> grab = npcLib.grabGlobalNPC(plugin, grave.getUUID().toString());
        if (grab.isPresent()) {
            npcLib.removeGlobalNPC(grab.get());
        }
        removeCorpse(getEntityDataNPCMap(getLoadedEntityDataList(grave)));
    }

    /**
     * Removes the NPC corpse associated with the given entity data.
     *
     * @param entityData The entity data whose associated NPC corpse should be removed.
     * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
     */
    @Deprecated(since = "4.9.9.1")
    public void removeCorpse(EntityData entityData) {
        removeCorpse(getEntityDataNPCMap(Collections.singletonList(entityData)));
    }

    /**
     * Removes multiple NPC corpses based on the provided entity data map.
     *
     * @param entityDataMap A map of entity data to NPC.Global instances to be removed.
     * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
     */
    @Deprecated(since = "4.9.9.1")
    public void removeCorpse(Map<EntityData, NPC.Global> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();
        for (Map.Entry<EntityData, NPC.Global> entry : entityDataMap.entrySet()) {
            npcLib.removeGlobalNPC(entry.getValue());
            entityDataList.add(entry.getKey());
        }
        plugin.getDataManager().removeEntityData(entityDataList);
    }

    /**
     * Retrieves a map of entity data to NPC.Global instances based on the provided entity data list.
     *
     * @param entityDataList The list of entity data to match with NPC.Global instances.
     * @return A map of entity data to NPC.Global instances.
     * @deprecated since 4.9.9.1 — Unmaintained. Use {@link me.jay.GravesX.integration.FancyNPCs} instead.
     */
    @Deprecated(since = "4.9.9.1")
    private Map<EntityData, NPC.Global> getEntityDataNPCMap(List<EntityData> entityDataList) {
        Map<EntityData, NPC.Global> entityDataMap = new LinkedHashMap<>();
        for (EntityData entityData : entityDataList) {
            for (NPC.Global npc : npcLib.getAllGlobalNPCs()) {
                if (npc.hasCustomData(plugin, "grave_uuid")
                        && npc.getCustomDataKeys().contains(entityData.getUUIDGrave().toString())) {
                    entityDataMap.put(entityData, npc);
                }
            }
        }
        return entityDataMap;
    }

    /**
     * Attempts to apply skin from grave owner texture/signature; falls back to a baked skin if needed.
     */
    private void applySkinFromGraveOrFallback(NPC.Global npc, Grave grave) {
        try {
            if (grave.getOwnerTexture() != null
                    && grave.getOwnerTextureSignature() != null
                    && grave.getOwnerName() != null) {
                NPC.Skin skin = NPC.Skin.Custom.createCustomSkin(
                        plugin,
                        grave.getOwnerUUID().toString(),
                        grave.getOwnerTexture(),
                        grave.getOwnerTextureSignature()
                );
                npc.setSkin(skin);
                return;
            }
        } catch (Exception ignored) {
            // fall through to baked fallback
        }

        try {
            // baked fallback texture/signature pair
            NPC.Skin fallback = NPC.Skin.Custom.createCustomSkin(
                    plugin,
                    "194ffca812294de7ab5386bb5c2686d3",
                    "ewogICJ0aW1lc3RhbXAiIDogMTcwMDA3NTcyMjAzOSwKICAicHJvZmlsZUlkIiA6ICIxOTRmZmNhODEyMjk0ZGU3YWI1Mzg2YmI1YzI2ODZkMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJDb3Jwc2UiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWJmZjJlYzQ0ZWM1MWIzMmVmMzc4YTY1NzIwYzA2MGYzYWRmNzQ5NDVkNDgwNmQ1YjQyZTI1Y2UxNzM2NGViZiIKICAgIH0KICB9Cn0=",
                    "v2WrGsMU53dyK1xvx6xS5r41XM4mvR6tB/86Tf5CjtQtv5ozjhEaHARHqFChnTl4/oG238alBMoFw6punEdLLJ8vVYSAa0K8CSpm8RT/gGvxpd6JHGsvcOEWEOV2wv0cntBs9BgrvoKvdFz7WyzT7w1PyP/74waU/Z83lBMU9he71DOFgAVnWXIp2PIWttK89hpbSmkrrdMLQ18/bUURQnp082ZinlDa7G2OjRbdpxGluOCKU725rufdnMhMBj5FCuuW8FaApa+6vuDDg6puIJgOXwtRX5/ZTp22UwEaMSegM+aP7oENx3wmm6XHHs3fgsulquRmxDuhAZ+sMi8wnW6lZU+2FWpsIOh4Xehn426iDu5wl4/kFe4RzTXr7G6N4uncgDRVaQQwsM3L/A7TmRbs8rQVrphqhOMvZ5R9fVu668EbMtAJbobofNxsVTRsRA9o7jnusIhmrWwroqVVxpq4k517ZEzDbPHkH/2X/amc7IGoeSLLfngIRYD+n7EUzO5ErQWFS778DiCxtQHKNOrBc/D+Fg9HsoH/Z2rD5dUBcxQ5DhprgMGGbaLDoQXjFul0mkE4Rg5yubonK+Ccvwmtv2s37sj1FwEJwllSFxvhmjxifTjSCaVoXJnsGJEZf3Zok9g2qk9gBzbgM1V2Ub8iOMupRs4JET9WR8+XIEk="
            );
            npc.setSkin(fallback);
        } catch (Exception ignored) {
            // no-op; leave default skin
        }
    }
}