package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import de.oliver.fancynpcs.api.*;

import de.oliver.fancynpcs.api.utils.NpcEquipmentSlot;
import de.oliver.fancynpcs.api.utils.SkinFetcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

public final class FancyNPCs extends EntityDataManager {
    private final Graves plugin;
    public FancyNPCs(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
    }
    public void createCorpse(UUID uuid, Location location, Grave grave) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (plugin.getConfig("fancynpcs.corpse.enabled", grave).getBoolean("fancynpcs.corpse.enabled") && grave.getOwnerType() == EntityType.PLAYER) {
                location.getBlock().setType(Material.AIR);
                Location npcLocation = location.clone();

                String id = grave.getOwnerUUID().toString().replace("-", "");
                String ID = uuid.toString().replace("-","");
                NpcManager npcManager = FancyNpcsPlugin.get().getNpcManager();
                try {
                    double x = plugin.getConfig("fancynpcs.corpse.offset.x", grave)
                            .getDouble("fancynpcs.corpse.offset.x");
                    double y = plugin.getConfig("fancynpcs.corpse.offset.y", grave)
                            .getDouble("fancynpcs.corpse.offset.y");
                    double z = plugin.getConfig("fancynpcs.corpse.offset.z", grave)
                            .getDouble("fancynpcs.corpse.offset.z");
                    npcLocation.add(x, y, z);
                } catch (IllegalArgumentException handled) {
                    npcLocation.add(-0.5, 0, -0.5);
                }
                NpcData newNpcData = new NpcData(uuid.toString().replace("-",""), grave.getOwnerUUID(), npcLocation);
                SkinFetcher.SkinData skin = new SkinFetcher.SkinData(uuid.toString(), null,null);
                Npc newNpc = FancyNpcsPlugin.get().getNpcAdapter().apply(newNpcData);
                newNpc.getData().setSkin(skin);
                newNpc.create();
                npcManager.registerNpc(newNpc);
                newNpc.spawnForAll();
                newNpc.update(Bukkit.getPlayer(grave.getOwnerUUID()));
                newNpc.updateForAll();

                Npc npc = npcManager.getNpc(ID);
                NpcAttribute attribute = FancyNpcsPlugin.get().getAttributeManager().getAttributeByName(EntityType.PLAYER, "pose");
                npc.getData().getAttributes().put(attribute, "sleeping");
                npc.getData().setCollidable(false);
                npc.getData().setShowInTab(false);
                npc.getData().setDisplayName("<empty>");
                npc.updateForAll();
                npc.getData().setSkin(skin);
                if (plugin.getConfig("fancynpcs.corpse.armor", grave).getBoolean("fancynpcs.corpse.armor")) {
                    if (grave.getEquipmentMap().containsKey(EquipmentSlot.HEAD)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.HEAD, grave.getEquipmentMap().get(EquipmentSlot.HEAD));
                    }

                    if (grave.getEquipmentMap().containsKey(EquipmentSlot.CHEST)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.CHEST, grave.getEquipmentMap().get(EquipmentSlot.CHEST));
                    }

                    if (grave.getEquipmentMap().containsKey(EquipmentSlot.LEGS)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.LEGS, grave.getEquipmentMap().get(EquipmentSlot.LEGS));
                    }

                    if (grave.getEquipmentMap().containsKey(EquipmentSlot.FEET)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.FEET, grave.getEquipmentMap().get(EquipmentSlot.FEET));
                    }
                }

                if (plugin.getConfig("fancynpcs.corpse.hand", grave).getBoolean("fancynpcs.corpse.hand")) {
                    if (grave.getEquipmentMap().containsKey(EquipmentSlot.HAND)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.MAINHAND, grave.getEquipmentMap().get(EquipmentSlot.HAND));
                    }

                    if (plugin.getVersionManager().hasSecondHand()
                            && grave.getEquipmentMap().containsKey(EquipmentSlot.OFF_HAND)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.OFFHAND, grave.getEquipmentMap().get(EquipmentSlot.OFF_HAND));
                    }
                }
                npc.update(Bukkit.getPlayer(grave.getOwnerUUID()));
                npc.updateForAll();
                npc.removeForAll();
                npc.create();
                npc.spawnForAll();
            }
        });
    }
    public void createBedrockcompatCorpse(UUID uuid, Location location, Grave grave) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (plugin.getConfig("fancynpcs.corpse.enabled", grave).getBoolean("fancynpcs.corpse.enabled") && grave.getOwnerType() == EntityType.PLAYER) {
                location.getBlock().setType(Material.AIR);
                Location npcLocation = location.clone();

                String ID = uuid.toString().replace("-","");
                NpcManager npcManager = FancyNpcsPlugin.get().getNpcManager();
                try {
                    double x = plugin.getConfig("fancynpcs.corpse.offset.x", grave)
                            .getDouble("fancynpcs.corpse.offset.x");
                    double y = plugin.getConfig("fancynpcs.corpse.offset.y", grave)
                            .getDouble("fancynpcs.corpse.offset.y");
                    double z = plugin.getConfig("fancynpcs.corpse.offset.z", grave)
                            .getDouble("fancynpcs.corpse.offset.z");
                    npcLocation.add(x, y, z);
                } catch (IllegalArgumentException handled) {
                    npcLocation.add(-0.5, 0, -0.5);
                }
                NpcData newNpcData = new NpcData(grave.getUUID().toString().replace("-",""), grave.getOwnerUUID(), npcLocation);
                SkinFetcher.SkinData skin = new SkinFetcher.SkinData("corpse", null, null);
                Npc newNpc = FancyNpcsPlugin.get().getNpcAdapter().apply(newNpcData);
                newNpc.getData().setSkin(skin);
                newNpc.create();
                npcManager.registerNpc(newNpc);
                newNpc.spawnForAll();
                newNpc.update(Bukkit.getPlayer(grave.getOwnerUUID()));
                newNpc.updateForAll();

                Npc npc = npcManager.getNpc(ID);
                NpcAttribute attribute = FancyNpcsPlugin.get().getAttributeManager().getAttributeByName(EntityType.PLAYER, "pose");
                npc.getData().getAttributes().put(attribute, "sleeping");
                npc.getData().setCollidable(false);
                npc.getData().setShowInTab(false);
                npc.getData().setDisplayName("<empty>");
                npc.updateForAll();
                npc.getData().setSkin(skin);
                if (plugin.getConfig("fancynpcs.corpse.armor", grave).getBoolean("fancynpcs.corpse.armor")) {
                    if (grave.getEquipmentMap().containsKey(EquipmentSlot.HEAD)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.HEAD, grave.getEquipmentMap().get(EquipmentSlot.HEAD));
                    }

                    if (grave.getEquipmentMap().containsKey(EquipmentSlot.CHEST)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.CHEST, grave.getEquipmentMap().get(EquipmentSlot.CHEST));
                    }

                    if (grave.getEquipmentMap().containsKey(EquipmentSlot.LEGS)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.LEGS, grave.getEquipmentMap().get(EquipmentSlot.LEGS));
                    }

                    if (grave.getEquipmentMap().containsKey(EquipmentSlot.FEET)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.FEET, grave.getEquipmentMap().get(EquipmentSlot.FEET));
                    }
                }

                if (plugin.getConfig("fancynpcs.corpse.hand", grave).getBoolean("fancynpcs.corpse.hand")) {
                    if (grave.getEquipmentMap().containsKey(EquipmentSlot.HAND)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.MAINHAND, grave.getEquipmentMap().get(EquipmentSlot.HAND));
                    }

                    if (plugin.getVersionManager().hasSecondHand()
                            && grave.getEquipmentMap().containsKey(EquipmentSlot.OFF_HAND)) {
                        npc.getData().addEquipment(NpcEquipmentSlot.OFFHAND, grave.getEquipmentMap().get(EquipmentSlot.OFF_HAND));
                    }
                }
                npc.update(Bukkit.getPlayer(grave.getOwnerUUID()));
                npc.updateForAll();
                npc.removeForAll();
                npc.create();
                npc.spawnForAll();
            }
        });
    }
    public void removeCorpse(Grave grave) {
        String uuid = grave.getUUID().toString().replace("-", "");
        NpcManager npcManager = FancyNpcsPlugin.get().getNpcManager();
        Npc npc = npcManager.getNpc(uuid);
        if (npc != null) {
            npc.removeForAll();
            npcManager.removeNpc(npc);
        }
    }
}
