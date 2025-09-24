package me.jay.GravesX.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcAttribute;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.NpcManager;
import de.oliver.fancynpcs.api.skins.SkinData;
import de.oliver.fancynpcs.api.utils.NpcEquipmentSlot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

public final class FancyNPCs extends EntityDataManager {
    private static final String POSE_ATTRIBUTE_NAME = "pose";

    private final Graves plugin;

    public FancyNPCs(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void createCorpse(UUID uuid, Location location, Grave grave) {
        plugin.getGravesXScheduler().runTask(() -> {
            if (plugin.getConfig("fancynpcs.corpse.enabled", grave).getBoolean("fancynpcs.corpse.enabled")
                    && grave.getOwnerType() == EntityType.PLAYER) {

                location.getBlock().setType(Material.AIR);
                Location npcLocation = location.clone();

                String ownerIdStr = grave.getOwnerUUID().toString().replace("-", "");
                String npcIdStr = uuid.toString().replace("-", "");

                NpcManager npcManager = FancyNpcsPlugin.get().getNpcManager();
                try {
                    double x = plugin.getConfig("fancynpcs.corpse.offset.x", grave).getDouble("fancynpcs.corpse.offset.x");
                    double y = plugin.getConfig("fancynpcs.corpse.offset.y", grave).getDouble("fancynpcs.corpse.offset.y");
                    double z = plugin.getConfig("fancynpcs.corpse.offset.z", grave).getDouble("fancynpcs.corpse.offset.z");
                    npcLocation.add(x, y, z);
                } catch (IllegalArgumentException handled) {
                    npcLocation.add(-0.5, 0, -0.5);
                }

                NpcData newNpcData = new NpcData(npcIdStr, grave.getOwnerUUID(), npcLocation);
                SkinData skin = new SkinData(ownerIdStr, SkinData.SkinVariant.AUTO, grave.getOwnerTexture(), grave.getOwnerTextureSignature());

                Npc newNpc = FancyNpcsPlugin.get().getNpcAdapter().apply(newNpcData);
                newNpc.getData().setSkin(String.valueOf(skin));
                newNpc.create();
                npcManager.registerNpc(newNpc);
                newNpc.spawnForAll();
                newNpc.update(Bukkit.getPlayer(grave.getOwnerUUID()));
                newNpc.updateForAll();

                Npc npc = npcManager.getNpc(npcIdStr);
                NpcAttribute attribute = FancyNpcsPlugin.get().getAttributeManager().getAttributeByName(EntityType.PLAYER, POSE_ATTRIBUTE_NAME);
                npc.getData().getAttributes().put(attribute, "sleeping");
                npc.getData().setCollidable(false);
                npc.getData().setShowInTab(false);
                npc.getData().setDisplayName("<empty>");
                npc.updateForAll();
                npc.getData().setSkin(String.valueOf(skin));

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
                    if (plugin.getVersionManager().hasSecondHand() && grave.getEquipmentMap().containsKey(EquipmentSlot.OFF_HAND)) {
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
        plugin.getGravesXScheduler().runTask(() -> {
            if (plugin.getConfig("fancynpcs.corpse.enabled", grave).getBoolean("fancynpcs.corpse.enabled")
                    && grave.getOwnerType() == EntityType.PLAYER) {

                location.getBlock().setType(Material.AIR);
                Location npcLocation = location.clone();

                String npcIdStr = uuid.toString().replace("-", "");

                NpcManager npcManager = FancyNpcsPlugin.get().getNpcManager();
                try {
                    double x = plugin.getConfig("fancynpcs.corpse.offset.x", grave).getDouble("fancynpcs.corpse.offset.x");
                    double y = plugin.getConfig("fancynpcs.corpse.offset.y", grave).getDouble("fancynpcs.corpse.offset.y");
                    double z = plugin.getConfig("fancynpcs.corpse.offset.z", grave).getDouble("fancynpcs.corpse.offset.z");
                    npcLocation.add(x, y, z);
                } catch (IllegalArgumentException handled) {
                    npcLocation.add(-0.5, 0, -0.5);
                }

                NpcData newNpcData = new NpcData(grave.getUUID().toString().replace("-", ""), grave.getOwnerUUID(), npcLocation);
                SkinData skin = new SkinData(
                        "194ffca812294de7ab5386bb5c2686d3",
                        SkinData.SkinVariant.AUTO,
                        "ewogICJ0aW1lc3RhbXAiIDogMTcwMDA3NTcyMjAzOSwKICAicHJvZmlsZUlkIiA6ICIxOTRmZmNhODEyMjk0ZGU3YWI1Mzg2YmI1YzI2ODZkMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJDb3Jwc2UiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWJmZjJlYzQ0ZWM1MWIzMmVmMzc4YTY1NzIwYzA2MGYzYWRmNzQ5NDVkNDgwNmQ1YjQyZTI1Y2UxNzM2NGViZiIKICAgIH0KICB9Cn0=",
                        "v2WrGsMU53dyK1xvx6xS5r41XM4mvR6tB/86Tf5CjtQtv5ozjhEaHARHqFChnTl4/oG238alBMoFw6punEdLLJ8vVYSAa0K8CSpm8RT/gGvxpd6JHGsvcOEWEOV2wv0cntBs9BgrvoKvdFz7WyzT7w1PyP/74waU/Z83lBMU9he71DOFgAVnWXIp2PIWttK89hpbSmkrrdMLQ18/bUURQnp082ZinlDa7G2OjRbdpxGluOCKU725rufdnMhMBj5FCuuW8FaApa+6vuDDg6puIJgOXwtRX5/ZTp22UwEaMSegM+aP7oENx3wmm6XHHs3fgsulquRmxDuhAZ+sMi8wnW6lZU+2FWpsIOh4Xehn426iDu5wl4/kFe4RzTXr7G6N4uncgDRVaQQwsM3L/A7TmRbs8rQVrphqhOMvZ5R9fVu668EbMtAJbobofNxsVTRsRA9o7jnusIhmrWwroqVVxpq4k517ZEzDbPHkH/2X/amc7IGoeSLLfngIRYD+n7EUzO5ErQWFS778DiCxtQHKNOrBc/D+Fg9HsoH/Z2rD5dUBcxQ5DhprgMGGbaLDoQXjFul0mkE4Rg5yubonK+Ccvwmtv2s37sj1FwEJwllSFxvhmjxifTjSCaVoXJnsGJEZf3Zok9g2qk9gBzbgM1V2Ub8iOMupRs4JET9WR8+XIEk="
                );

                Npc newNpc = FancyNpcsPlugin.get().getNpcAdapter().apply(newNpcData);
                newNpc.getData().setSkin(String.valueOf(skin));
                newNpc.create();
                npcManager.registerNpc(newNpc);
                newNpc.spawnForAll();
                newNpc.update(Bukkit.getPlayer(grave.getOwnerUUID()));
                newNpc.updateForAll();

                Npc npc = npcManager.getNpc(npcIdStr);
                NpcAttribute attribute = FancyNpcsPlugin.get().getAttributeManager().getAttributeByName(EntityType.PLAYER, POSE_ATTRIBUTE_NAME);
                npc.getData().getAttributes().put(attribute, "sleeping");
                npc.getData().setCollidable(false);
                npc.getData().setShowInTab(false);
                npc.getData().setDisplayName("<empty>");
                npc.updateForAll();
                npc.getData().setSkin(String.valueOf(skin));

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
                    if (plugin.getVersionManager().hasSecondHand() && grave.getEquipmentMap().containsKey(EquipmentSlot.OFF_HAND)) {
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
        String id = grave.getUUID().toString().replace("-", "");
        NpcManager npcManager = FancyNpcsPlugin.get().getNpcManager();
        Npc npc = npcManager.getNpc(id);
        if (npc != null) {
            npc.removeForAll();
            npcManager.removeNpc(npc);
        }
    }

    public boolean hasCorpse(Grave grave) {
        String id = grave.getUUID().toString().replace("-", "");
        NpcManager npcManager = FancyNpcsPlugin.get().getNpcManager();
        Npc npc = npcManager.getNpc(id);
        return npc != null;
    }
}