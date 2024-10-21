package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.listener.integration.citizensnpcs.CitizensNPCInteractListener;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.trait.SkinTrait;
import net.citizensnpcs.util.NMS;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

/**
 * Manages NPC interactions and corpse creation related to player graves using Citizens2.
 * Extends EntityDataManager to handle entity data.
 */
public final class CitizensNPC extends EntityDataManager {
    private final Graves plugin;
    private final CitizensNPCInteractListener citizensNPCInteractListener;

    /**
     * Constructs a new CitizensNPC instance with the specified Graves plugin.
     *
     * @param plugin The main Graves plugin instance.
     */
    public CitizensNPC(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
        this.citizensNPCInteractListener = new CitizensNPCInteractListener(plugin, this);

        registerListeners();
    }

    /**
     * Registers the NPC interaction listeners.
     */
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(citizensNPCInteractListener, plugin);
    }

    /**
     * Unregisters the NPC interaction listeners.
     */
    public void unregisterListeners() {
        if (citizensNPCInteractListener != null) {
            HandlerList.unregisterAll(citizensNPCInteractListener);
        }
    }

    /**
     * Creates NPC corpses based on the cached entity data.
     */
    public void createCorpses() {
        for (ChunkData chunkData : plugin.getCacheManager().getChunkMap().values()) {
            for (EntityData entityData : chunkData.getEntityDataMap().values()) {
                if (entityData.getType() == EntityData.Type.CITIZENSNPC) {
                    if (plugin.getCacheManager().getGraveMap().containsKey(entityData.getUUIDGrave())) {
                        Grave grave = plugin.getCacheManager().getGraveMap().get(entityData.getUUIDGrave());

                        if (grave != null) {
                            createCorpse(entityData.getUUIDEntity(), entityData.getLocation(), grave, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a new NPC corpse at the specified location with the given grave data.
     *
     * @param location The location to spawn the NPC.
     * @param grave    The grave data for the NPC.
     */
    public void createCorpse(Location location, Grave grave) {
        createCorpse(UUID.randomUUID(), location, grave, true);
    }

    /**
     * Creates a new NPC corpse with a specific UUID at the given location using the provided grave data.
     *
     * @param uuid              The UUID for the NPC.
     * @param location          The location to spawn the NPC.
     * @param grave             The grave data for the NPC.
     * @param createEntityData  Whether to create entity data for the NPC.
     */
    public void createCorpse(UUID uuid, Location location, Grave grave, boolean createEntityData) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (plugin.getConfig("citizens.corpse.enabled", grave).getBoolean("citizens.corpse.enabled")
                    && grave.getOwnerType() == EntityType.PLAYER) {
                Player player = plugin.getServer().getPlayer(grave.getOwnerUUID());
                Location npcLocation = location.clone();

                if (player != null && npcLocation.getWorld() != null) {
                    location.getBlock().setType(Material.AIR);

                    // Create NPC name from location
                    String npcName = getNPCNameFromLocation(npcLocation);
                    NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, npcName);
                    try {
                        double x = plugin.getConfig("citizens.corpse.offset.x", grave)
                                .getDouble("citizens.corpse.offset.x");
                        double y = plugin.getConfig("citizens.corpse.offset.y", grave)
                                .getDouble("citizens.corpse.offset.y");
                        double z = plugin.getConfig("citizens.corpse.offset.z", grave)
                                .getDouble("citizens.corpse.offset.z");
                        npcLocation.add( x, y, z);
                    } catch (IllegalArgumentException handled) {
                        npcLocation.add(0.5, 0.5, 0.5);
                    }
                    npc.spawn(npcLocation);
                    npc.data().setPersistent(NPC.Metadata.DEFAULT_PROTECTED, true);
                    npc.data().setPersistent(NPC.Metadata.FLYABLE, true);
                    npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
                    npc.data().setPersistent(NPC.Metadata.KNOCKBACK, false);
                    npc.data().setPersistent(NPC.Metadata.TARGETABLE, false);
                    npc.data().setPersistent(NPC.Metadata.FLUID_PUSHABLE, false);
                    npc.data().setPersistent(NPC.Metadata.SWIM, false);
                    npc.data().setPersistent(NPC.Metadata.REMOVE_FROM_TABLIST, true);
                    npc.data().setPersistent(NPC.Metadata.REMOVE_FROM_PLAYERLIST, true);
                    npc.data().setPersistent(NPC.Metadata.SHOULD_SAVE, false);

                    npc.getOrAddTrait(SkinTrait.class).setSkinPersistent(
                            grave.getOwnerName(),
                            grave.getOwnerTextureSignature(),
                            grave.getOwnerTexture()
                    );

                    // Create a scoreboard team for the NPC
                    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                    Team team = scoreboard.getTeam("npcTeam");
                    if (team == null) {
                        team = scoreboard.registerNewTeam("npcTeam");
                    }
                    team.addEntry(npc.getName());
                    NMS.setTeamNameTagVisible(team, false); // doesnt work

                    npc.data().setPersistent(NPC.Metadata.COLLIDABLE, plugin.getConfig("citizens.corpse.collide", grave).getBoolean("citizens.corpse.collide"));

                    // Set NPC equipment
                    setNPCEquipment(npc, grave, Equipment.EquipmentSlot.HELMET, "citizens.corpse.armor");
                    setNPCEquipment(npc, grave, Equipment.EquipmentSlot.CHESTPLATE, "citizens.corpse.armor");
                    setNPCEquipment(npc, grave, Equipment.EquipmentSlot.LEGGINGS, "citizens.corpse.armor");
                    setNPCEquipment(npc, grave, Equipment.EquipmentSlot.BOOTS, "citizens.corpse.armor");
                    setNPCEquipment(npc, grave, Equipment.EquipmentSlot.HAND, "citizens.corpse.hand");

                    if (plugin.getVersionManager().hasSecondHand()) {
                        setNPCEquipment(npc, grave, Equipment.EquipmentSlot.OFF_HAND, "citizens.corpse.hand");
                    }

                    // Make the NPC perform the configured animation (default is sleeping)
                    if (npc.getEntity() instanceof Player) {
                        Player npcPlayer = (Player) npc.getEntity();
                        String animation = plugin.getConfig().getString("citizens.corpse.pose", "SLEEP").toUpperCase();

                        try {
                            // Attempt to load the correct PlayerAnimation class
                            Class<? extends Enum<?>> playerAnimationClass;
                            try {
                                playerAnimationClass = (Class<? extends Enum<?>>) Class.forName("net.citizensnpcs.util.PlayerAnimation");
                            } catch (ClassNotFoundException e) {
                                playerAnimationClass = (Class<? extends Enum<?>>) Class.forName("net.citizensnpcs.api.util.PlayerAnimation");
                            }

                            // Use the safer approach to get the enum constant
                            Enum<?> playerAnimation;
                            try {
                                playerAnimation = Enum.valueOf(playerAnimationClass.asSubclass(Enum.class), animation);
                                // Invoke the play method using reflection
                                try {
                                    playerAnimation.getClass().getMethod("play", Player.class).invoke(playerAnimation, npcPlayer);
                                } catch (NoSuchMethodException nsme) {
                                    plugin.getLogger().warning("Animation " + animation + " is not supported in this version.");
                                    // Print all valid enum constants
                                    Object[] enums = playerAnimationClass.getEnumConstants();
                                    if (enums != null) {
                                        plugin.getLogger().warning("Valid animations for " + playerAnimationClass.getSimpleName() + ":");
                                        for (Object enumConstant : enums) {
                                            plugin.getLogger().warning("- " + enumConstant.toString());
                                        }
                                    }
                                }
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid animation: " + animation + ". Please check the available animations.");
                                Object[] enums = playerAnimationClass.getEnumConstants();
                                if (enums != null) {
                                    plugin.getLogger().warning("Valid animations for " + playerAnimationClass.getSimpleName() + ":");
                                    for (Object enumConstant : enums) {
                                        plugin.getLogger().warning("- " + enumConstant.toString());
                                    }
                                }
                            }

                        } catch (Exception e) {
                            plugin.getLogger().severe("An error occurred while performing the animation: " + e.getMessage());
                            plugin.logStackTrace(e);
                        }
                    }

                    if (plugin.getConfig("citizens.corpse.glow.enabled", grave)
                            .getBoolean("citizens.corpse.glow.enabled")) {
                        try {
                            npc.data().setPersistent(NPC.Metadata.GLOWING, true);
                            npc.data().setPersistent(NPC.Metadata.valueOf("GLOWING_COLOR"), ChatColor.valueOf(plugin
                                    .getConfig("citizens.corpse.glow.color", grave)
                                    .getString("citizens.corpse.glow.color")).toString());
                        } catch (IllegalArgumentException ignored) {
                            npc.data().setPersistent(NPC.Metadata.GLOWING, true);
                        }
                    }

                    npc.data().setPersistent("grave_uuid", grave.getUUID().toString());

                    plugin.debugMessage("Spawning Citizens NPC for " + grave.getUUID() + " at "
                            + npcLocation.getWorld().getName() + ", " + (npcLocation.getBlockX() + 0.5) + "x, "
                            + (npcLocation.getBlockY() + 0.5) + "y, " + (npcLocation.getBlockZ() + 0.5) + "z", 1);

                    if (createEntityData) {
                        createEntityData(location, uuid, grave.getUUID(), EntityData.Type.CITIZENSNPC);
                    }
                }
            }
        });
    }

    private void setNPCEquipment(NPC npc, Grave grave, Equipment.EquipmentSlot slot, String configPath) {
        if (plugin.getConfig(configPath, grave).getBoolean(configPath) && grave.getEquipmentMap().containsKey(slot)) {
            ItemStack item = grave.getEquipmentMap().get(slot);
            npc.getOrAddTrait(Equipment.class).set(slot, item);
        }
    }

    private String getNPCNameFromLocation(Location location) {
        String npcName = location.getWorld().getName() + "_" + location.getBlockX() + "_"
                + location.getBlockY() + "_" + location.getBlockZ();
        return npcName.replace("|", "");
    }

    /**
     * Removes the NPC corpse associated with the given grave.
     *
     * @param grave The grave whose associated NPC corpse should be removed.
     */
    public void removeCorpse(Grave grave) {
        Location location = grave.getLocationDeath();
        if (location != null) {
            String npcName = getNPCNameFromLocation(location);
            NPC npc = getNPCByName(npcName);
            if (npc != null) {
                npc.destroy();
            }
        }
        removeCorpse(getEntityDataNPCMap(getLoadedEntityDataList(grave)));
    }

    /**
     * Removes the NPC corpse associated with the given entity data.
     *
     * @param entityData The entity data whose associated NPC corpse should be removed.
     */
    public void removeCorpse(EntityData entityData) {
        Location location = entityData.getLocation();
        if (location != null) {
            String npcName = getNPCNameFromLocation(location);
            NPC npc = getNPCByName(npcName);
            CitizensAPI.getNPCRegistry().deregister(npc);
            if (npc != null) {
                npc.destroy();
            }
        }
        removeCorpse(getEntityDataNPCMap(Collections.singletonList(entityData)));
    }

    /**
     * Removes multiple NPC corpses based on the provided entity data map.
     *
     * @param entityDataMap A map of entity data to NPC instances to be removed.
     */
    public void removeCorpse(Map<EntityData, NPC> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, NPC> entry : entityDataMap.entrySet()) {
            NMS.remove(entry.getValue().getEntity());
            CitizensAPI.getNPCRegistry().deregister(entry.getValue());
            entityDataList.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }

    /**
     * Retrieves a map of entity data to NPC instances based on the provided entity data list.
     *
     * @param entityDataList The list of entity data to match with NPC instances.
     * @return A map of entity data to NPC instances.
     */
    private Map<EntityData, NPC> getEntityDataNPCMap(List<EntityData> entityDataList) {
        Map<EntityData, NPC> entityDataMap = new HashMap<>();

        for (EntityData entityData : entityDataList) {
            Location location = entityData.getLocation();
            if (location != null) {
                String npcName = getNPCNameFromLocation(location);
                NPC npc = getNPCByName(npcName);
                if (npc != null) {
                    entityDataMap.put(entityData, npc);
                }
            }
        }

        return entityDataMap;
    }

    private NPC getNPCByName(String name) {
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (name.equals(npc.getName())) {
                return npc;
            }
        }
        return null;
    }
}