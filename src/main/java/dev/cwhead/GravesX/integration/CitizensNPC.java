package dev.cwhead.GravesX.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import dev.cwhead.GravesX.listener.integration.citizensnpcs.CitizensNPCInteractListener;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.*;
import java.util.*;

/**
 * Manages NPC interactions and corpse creation/removal related to player graves using Citizens2.
 * Utilizes reflection to remain compatible across multiple Citizens versions.
 */
public final class CitizensNPC extends EntityDataManager {
    private final Graves plugin;
    private final CitizensNPCInteractListener citizensNPCInteractListener;

    private final Class<?> npcClass;
    private final Class<?> skinTraitClass;
    private final Method getNPCRegistryMethod;
    private final Method createNPCMethod;
    private final Method spawnMethod;
    private final Method dataMethod;
    private final Method getOrAddTraitMethod;
    private final Method destroyMethod;
    private final Method deregisterMethod;
    private final Method nmsRemoveMethod;

    /**
     * Constructs a new CitizensNPC instance with the specified Graves plugin.
     *
     * @param plugin The main Graves plugin instance.
     */
    public CitizensNPC(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
        this.citizensNPCInteractListener = new CitizensNPCInteractListener(plugin, this);

        ClassLoader cl = CitizensNPC.class.getClassLoader();
        // Reflection metadata
        Class<?> citizensAPIClass = findClass(new String[]{
                "net.citizensnpcs.api.CitizensAPI",
                "net.citizensnpcs.CitizensAPI"
        }, cl);
        Class<?> npcRegistryClass = findClass(new String[]{
                "net.citizensnpcs.api.npc.NPCRegistry",
                "net.citizensnpcs.NPCRegistry"
        }, cl);
        npcClass = findClass(new String[]{
                "net.citizensnpcs.api.npc.NPC",
                "net.citizensnpcs.NPC"
        }, cl);
        skinTraitClass = findClass(new String[]{
                "net.citizensnpcs.api.trait.SkinTrait",
                "net.citizensnpcs.trait.SkinTrait"
        }, cl);
        Class<?> nmsClass = findClass(new String[]{
                "net.citizensnpcs.api.util.NMS",
                "net.citizensnpcs.util.NMS"
        }, cl);

        try {
            getNPCRegistryMethod = citizensAPIClass.getMethod("getNPCRegistry");
            createNPCMethod = npcRegistryClass.getMethod("createNPC", EntityType.class, String.class);
            spawnMethod = npcClass.getMethod("spawn", Location.class);
            dataMethod = npcClass.getMethod("data");
            getOrAddTraitMethod = npcClass.getMethod("getOrAddTrait", Class.class);
            destroyMethod = npcClass.getMethod("destroy");
            deregisterMethod = npcRegistryClass.getMethod("deregister", npcClass);
            nmsRemoveMethod = nmsClass.getMethod("remove", org.bukkit.entity.Entity.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Citizens API methods not found via reflection", e);
        }
        registerListeners();
    }

    private Class<?> findClass(String[] names, ClassLoader loader) {
        for (String name : names) {
            try {
                return Class.forName(name, true, loader);
            } catch (ClassNotFoundException ignored) {}
        }
        throw new RuntimeException("None of classes " + Arrays.toString(names) + " found");
    }

    /**
     * Registers the NPC interaction listeners.
     */
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(citizensNPCInteractListener, plugin);
    }

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
            for (EntityData data : chunkData.getEntityDataMap().values()) {
                if (data.getType() == EntityData.Type.CITIZENSNPC) {
                    Grave grave = plugin.getCacheManager().getGraveMap().get(data.getUUIDGrave());
                    if (grave != null) {
                        createCorpse(data.getUUIDEntity(), data.getLocation(), grave, false);
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
        plugin.getGravesXScheduler().runTask(() -> {
            if (!plugin.getConfig("citizens.corpse.enabled", grave).getBoolean("citizens.corpse.enabled")
                    || grave.getOwnerType() != EntityType.PLAYER) return;

            Player player = plugin.getServer().getPlayer(grave.getOwnerUUID());
            if (player == null || location.getWorld() == null) return;

            location.getBlock().setType(Material.AIR);
            String npcName = getNPCNameFromLocation(location);
            try {
                Object registry = getNPCRegistryMethod.invoke(null);
                Object npc = createNPCMethod.invoke(registry, EntityType.PLAYER, npcName);

                // Position offset
                try {
                    double x = plugin.getConfig("citizens.corpse.offset.x", grave).getDouble("citizens.corpse.offset.x");
                    double y = plugin.getConfig("citizens.corpse.offset.y", grave).getDouble("citizens.corpse.offset.y");
                    double z = plugin.getConfig("citizens.corpse.offset.z", grave).getDouble("citizens.corpse.offset.z");
                    location.add(x, y, z);
                } catch (IllegalArgumentException ex) {
                    location.add(0.5, 0.5, 0.5);
                }
                spawnMethod.invoke(npc, location);

                // Hide nameplate via NMS
                ScoreboardManager mgr = Bukkit.getScoreboardManager();
                if (mgr != null) {
                    Scoreboard board = mgr.getMainScoreboard();
                    Team team = board.getTeam("npcTeam");
                    if (team == null) team = board.registerNewTeam("npcTeam");
                    team.addEntry(npcName);
                    nmsRemoveMethod.getDeclaringClass()
                            .getMethod("setTeamNameTagVisible", Team.class, boolean.class)
                            .invoke(null, team, false);
                }

                // Skin
                Object skinTrait = getOrAddTraitMethod.invoke(npc, skinTraitClass);
                try {
                    skinTraitClass.getMethod("setSkinPersistent", String.class, String.class, String.class)
                            .invoke(skinTrait, grave.getOwnerName(), grave.getOwnerTextureSignature(), grave.getOwnerTexture());
                } catch (NoSuchMethodException ignore) {
                    skinTraitClass.getMethod("setSkinName", String.class)
                            .invoke(skinTrait, grave.getOwnerName());
                }

                // Store metadata to save state
                Object meta = dataMethod.invoke(npc);
                Method setPersistent = meta.getClass().getMethod("setPersistent", String.class, Object.class);
                setPersistent.invoke(meta, "grave_uuid", grave.getUUID().toString());

                if (createEntityData) {
                    createEntityData(location, uuid, grave.getUUID(), EntityData.Type.CITIZENSNPC);
                }
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().severe("Error spawning Citizens NPC");
                plugin.logStackTrace(e);
            }
        });
    }

    /**
     * Removes the NPC corpse for the given grave.
     */
    public void removeCorpse(Grave grave) {
        Location loc = grave.getLocationDeath();
        if (loc == null) return;
        String name = getNPCNameFromLocation(loc);
        Object npc = getNPCByName(name);
        if (npc != null) {
            try {
                destroyMethod.invoke(npc);
                Object registry = getNPCRegistryMethod.invoke(null);
                deregisterMethod.invoke(registry, npc);
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().warning("Failed to remove NPC " + name + ": " + e.getMessage());
            }
        }
        getLoadedEntityDataList(grave).stream()
                .filter(d -> d.getType() == EntityData.Type.CITIZENSNPC)
                .findFirst().ifPresent(this::removeCorpse);
    }

    /**
     * Removes a specific corpse entity and its data.
     */
    public void removeCorpse(EntityData entityData) {
        Location loc = entityData.getLocation();
        if (loc != null) {
            String name = getNPCNameFromLocation(loc);
            Object npc = getNPCByName(name);
            try {
                destroyMethod.invoke(npc);
                Object registry = getNPCRegistryMethod.invoke(null);
                deregisterMethod.invoke(registry, npc);
                nmsRemoveMethod.invoke(null, ((org.bukkit.entity.Entity) npcClass.getMethod("getEntity").invoke(npc)));
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().warning("Failed to remove NPC data entity: " + e.getMessage());
            }
        }
        Map<EntityData, Object> map = getEntityDataNPCMap(List.of(entityData));
        removeCorpse(map);
    }

    /**
     * Bulk removal using reflection.
     */
    public void removeCorpse(Map<EntityData, Object> entityDataMap) {
        List<EntityData> toRemove = new ArrayList<>();
        try {
            Object registry = getNPCRegistryMethod.invoke(null);
            for (Map.Entry<EntityData, Object> entry : entityDataMap.entrySet()) {
                Object npc = entry.getValue();
                destroyMethod.invoke(npc);
                deregisterMethod.invoke(registry, npc);
                toRemove.add(entry.getKey());
            }
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Failed bulk remove NPCs: " + e.getMessage());
        }
        plugin.getDataManager().removeEntityData(toRemove);
    }

    private String getNPCNameFromLocation(Location location) {
        if (location.getWorld() != null) {
            return (location.getWorld().getName() + "_" + location.getBlockX() + "_"
                    + location.getBlockY() + "_" + location.getBlockZ()).replace("|", "");
        }
        return "";
    }

    /**
     * Finds all matching EntityData and NPCs.
     */
    private Map<EntityData, Object> getEntityDataNPCMap(List<EntityData> list) {
        Map<EntityData, Object> map = new HashMap<>();
        for (EntityData d : list) {
            String name = getNPCNameFromLocation(d.getLocation());
            Object npc = getNPCByName(name);
            if (npc != null) map.put(d, npc);
        }
        return map;
    }

    /**
     * Locates an NPC by name via registry iteration.
     */
    @SuppressWarnings("unchecked")
    private Object getNPCByName(String name) {
        try {
            Object registry = getNPCRegistryMethod.invoke(null);
            for (Object npc : (Iterable<Object>) registry) {
                if (name.equals(npcClass.getMethod("getName").invoke(npc))) {
                    return npc;
                }
            }
        } catch (ReflectiveOperationException ignore) {}
        return null;
    }

    /**
     * Checks if a corpse exists for the grave.
     */
    public boolean hasNPCCorpse(Grave grave) {
        Location loc = grave.getLocationDeath();
        if (loc != null) {
            return getNPCByName(getNPCNameFromLocation(loc)) != null;
        }
        return false;
    }
}