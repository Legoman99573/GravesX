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
import me.jay.GravesX.listener.integration.fancynpcs.NpcInteractListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class FancyNPCs extends EntityDataManager {

    private static final String POSE_ATTRIBUTE_NAME = "pose";
    private final Graves plugin;
    private final NpcInteractListener npcInteractListener;

    public FancyNPCs(Graves plugin) {
        super(plugin);
        this.plugin = plugin;
        this.npcInteractListener = new NpcInteractListener(plugin);

        plugin.getServer().getPluginManager().registerEvents(npcInteractListener, plugin);
    }

    public void unregisterListeners() {
        if (npcInteractListener != null) {
            HandlerList.unregisterAll(npcInteractListener);
        }
    }

    public void createCorpse(UUID uuid, Location location, Grave grave) {
        plugin.getGravesXScheduler().runTask(() -> {
            if (!plugin.getConfigManager().getConfigSection("fancynpcs.corpse.enabled", grave).getBoolean("fancynpcs.corpse.enabled")
                    || grave.getOwnerType() != EntityType.PLAYER) return;

            location.getBlock().setType(Material.AIR);
            Location npcLocation = location.clone();

            try {
                double x = plugin.getConfigManager().getConfigSection("fancynpcs.corpse.offset.x", grave).getDouble("fancynpcs.corpse.offset.x");
                double y = plugin.getConfigManager().getConfigSection("fancynpcs.corpse.offset.y", grave).getDouble("fancynpcs.corpse.offset.y");
                double z = plugin.getConfigManager().getConfigSection("fancynpcs.corpse.offset.z", grave).getDouble("fancynpcs.corpse.offset.z");
                npcLocation.add(x, y, z);
            } catch (IllegalArgumentException ignored) {
                npcLocation.add(-0.5, 0, -0.5);
            }

            String ownerIdStr = grave.getOwnerUUID().toString();
            String npcIdStr = uuid.toString().replace("-", "");

            NpcManager npcManager = FancyNpcsPlugin.get().getNpcManager();
            NpcData npcData = new NpcData(npcIdStr, grave.getOwnerUUID(), npcLocation);
            Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(npcData);

            try {
                SkinData skin = new SkinData(ownerIdStr, SkinData.SkinVariant.AUTO, toValidBase64Texture(grave.getOwnerTexture()), grave.getOwnerTextureSignature());
                npc.getData().setSkin(skin.getIdentifier());
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load NPC skin for grave " + grave.getUUID() + ": " + ex.getMessage());
                plugin.logStackTrace(ex);
            }

            npcManager.registerNpc(npc);
            npc.create();
            npc.spawnForAll();

            NpcAttribute poseAttribute = FancyNpcsPlugin.get().getAttributeManager().getAttributeByName(EntityType.PLAYER, POSE_ATTRIBUTE_NAME);
            npc.getData().getAttributes().put(poseAttribute, "swimming");
            npc.getData().setCollidable(false);
            npc.getData().setShowInTab(false);
            npc.getData().setDisplayName("<empty>");

            // Equip armor
            if (plugin.getConfigManager().getConfigSection("fancynpcs.corpse.armor", grave).getBoolean("fancynpcs.corpse.armor")) {
                equipArmor(npc, grave);
            }

            // Equip hands
            if (plugin.getConfigManager().getConfigSection("fancynpcs.corpse.hand", grave).getBoolean("fancynpcs.corpse.hand")) {
                equipHands(npc, grave);
            }

            npc.updateForAll();
        });
    }

    public void createBedrockcompatCorpse(UUID uuid, Location location, Grave grave) {
        plugin.getGravesXScheduler().runTask(() -> {
            if (!plugin.getConfigManager().getConfigSection("fancynpcs.corpse.enabled", grave).getBoolean("fancynpcs.corpse.enabled")
                    || grave.getOwnerType() != EntityType.PLAYER) return;

            location.getBlock().setType(Material.AIR);
            Location npcLocation = location.clone();

            try {
                double x = plugin.getConfigManager().getConfigSection("fancynpcs.corpse.offset.x", grave).getDouble("fancynpcs.corpse.offset.x");
                double y = plugin.getConfigManager().getConfigSection("fancynpcs.corpse.offset.y", grave).getDouble("fancynpcs.corpse.offset.y");
                double z = plugin.getConfigManager().getConfigSection("fancynpcs.corpse.offset.z", grave).getDouble("fancynpcs.corpse.offset.z");
                npcLocation.add(x, y, z);
            } catch (IllegalArgumentException ignored) {
                npcLocation.add(-0.5, 0, -0.5);
            }

            String npcIdStr = uuid.toString().replace("-", "");
            String ownerIdStr = grave.getOwnerUUID().toString();

            NpcManager npcManager = FancyNpcsPlugin.get().getNpcManager();
            NpcData npcData = new NpcData(npcIdStr, grave.getOwnerUUID(), npcLocation);
            Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(npcData);

            try {
                npc.getData().setSkin(ownerIdStr);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to apply Bedrock skin for grave " + grave.getUUID() + ": " + ex.getMessage());
                plugin.logStackTrace(ex);
            }

            npcManager.registerNpc(npc);
            npc.create();
            npc.spawnForAll();

            NpcAttribute poseAttribute = FancyNpcsPlugin.get().getAttributeManager()
                    .getAttributeByName(EntityType.PLAYER, POSE_ATTRIBUTE_NAME);
            npc.getData().getAttributes().put(poseAttribute, "swimming");
            npc.getData().setCollidable(false);
            npc.getData().setShowInTab(false);
            npc.getData().setDisplayName("<empty>");

            if (plugin.getConfigManager().getConfigSection("fancynpcs.corpse.armor", grave).getBoolean("fancynpcs.corpse.armor")) {
                equipArmor(npc, grave);
            }

            if (plugin.getConfigManager().getConfigSection("fancynpcs.corpse.hand", grave).getBoolean("fancynpcs.corpse.hand")) {
                equipHands(npc, grave);
            }

            npc.updateForAll();
        });
    }


    private void equipArmor(Npc npc, Grave grave) {
        if (grave.getEquipmentMap().containsKey(EquipmentSlot.HEAD))
            npc.getData().addEquipment(NpcEquipmentSlot.HEAD, grave.getEquipmentMap().get(EquipmentSlot.HEAD));
        if (grave.getEquipmentMap().containsKey(EquipmentSlot.CHEST))
            npc.getData().addEquipment(NpcEquipmentSlot.CHEST, grave.getEquipmentMap().get(EquipmentSlot.CHEST));
        if (grave.getEquipmentMap().containsKey(EquipmentSlot.LEGS))
            npc.getData().addEquipment(NpcEquipmentSlot.LEGS, grave.getEquipmentMap().get(EquipmentSlot.LEGS));
        if (grave.getEquipmentMap().containsKey(EquipmentSlot.FEET))
            npc.getData().addEquipment(NpcEquipmentSlot.FEET, grave.getEquipmentMap().get(EquipmentSlot.FEET));
    }

    private void equipHands(Npc npc, Grave grave) {
        if (grave.getEquipmentMap().containsKey(EquipmentSlot.HAND))
            npc.getData().addEquipment(NpcEquipmentSlot.MAINHAND, grave.getEquipmentMap().get(EquipmentSlot.HAND));
        if (plugin.getVersionManager().hasSecondHand() && grave.getEquipmentMap().containsKey(EquipmentSlot.OFF_HAND))
            npc.getData().addEquipment(NpcEquipmentSlot.OFFHAND, grave.getEquipmentMap().get(EquipmentSlot.OFF_HAND));
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
        return npcManager.getNpc(id) != null;
    }

    /**
     * Ensures a valid Base64-encoded Minecraft skin texture value for FancyNPCs 2.8.0+.
     * - If input is already base64 JSON, returns it unchanged.
     * - If input looks like a URL, wraps it in proper JSON and base64-encodes it.
     */
    private static String toValidBase64Texture(String graveTexture) {
        if (graveTexture == null || graveTexture.isEmpty()) {
            return "";
        }

        boolean isAlreadyBase64 = graveTexture.startsWith("eyJ");

        if (isAlreadyBase64) {
            return graveTexture;
        }

        String json = String.format(
                "{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}",
                graveTexture
        );
        return Base64.getEncoder()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}