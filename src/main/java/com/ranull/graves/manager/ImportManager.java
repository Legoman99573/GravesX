package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.StringUtil;
import com.ranull.graves.util.UUIDUtil;
import com.ranull.graves.util.YAMLUtil;
import me.jay.GravesX.util.SkinSignatureUtil;
import me.jay.GravesX.util.SkinTextureUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * The ImportManager class handles the import of graves from external plugins.
 */
public final class ImportManager {
    /**
     * The main plugin instance associated with Graves.
     * <p>
     * This {@link Graves} instance represents the core plugin that this Graves is part of. It provides access
     * to the plugin's functionality, configuration, and other services.
     * </p>
     */
    private final Graves plugin;

    /**
     * Initializes a new instance of the ImportManager class.
     *
     * @param plugin The plugin instance.
     */
    public ImportManager(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Imports graves from AngelChest.
     *
     * @return A list of imported graves.
     */
    public List<Grave> importExternalPluginAngelChest() {
        return new ArrayList<>(importAngelChest());
    }

    /**
     * Imports graves from the AngelChest plugin.
     *
     * @return A list of graves imported from AngelChest.
     */
    private List<Grave> importAngelChest() {
        List<Grave> graveList = new ArrayList<>();
        File angelChest = new File(plugin.getPluginsFolder(), "AngelChest");
        if (!angelChest.exists()) return graveList;

        File angelChests = new File(angelChest, "angelchests");
        if (!angelChests.exists()) return graveList;

        File[] files = angelChests.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return (lower.endsWith(".yml") || lower.endsWith(".yaml")) && !name.startsWith(".");
        });
        if (files == null) return graveList;

        for (File file : files) {
            Grave grave = convertAngelChestToGrave(file);
            if (grave != null) graveList.add(grave);
        }
        return graveList;
    }

    /**
     * Converts an AngelChest file to a Grave object.
     *
     * @param file The AngelChest file.
     * @return The converted Grave object.
     */
    public Grave convertAngelChestToGrave(File file) {
        FileConfiguration angelChest = loadFile(file);
        if (angelChest == null) return null;

        Grave grave = new Grave(UUID.randomUUID());

        UUID worldUUID = UUIDUtil.getUUID(angelChest.getString("worldid", null));
        String logfile = angelChest.getString("logfile", "");
        String[] logfileSplit = logfile.split("_");

        World world = null;
        if (worldUUID != null) {
            world = plugin.getServer().getWorld(worldUUID);
        }
        if (world == null && logfileSplit.length > 1) {
            world = plugin.getServer().getWorld(logfileSplit[1]);
        }

        if (world != null) {
            int x = angelChest.getInt("x", 0);
            int y = angelChest.getInt("y", 0);
            int z = angelChest.getInt("z", 0);
            grave.setLocationDeath(new Location(world, x, y, z));
        }

        grave.setOwnerType(EntityType.PLAYER);
        UUID ownerUUID = UUIDUtil.getUUID(angelChest.getString("owner", null));
        grave.setOwnerUUID(ownerUUID);

        if (logfileSplit.length > 0 && logfileSplit[0] != null && !logfileSplit[0].isEmpty()) {
            grave.setOwnerName(logfileSplit[0]);
        }

        if (ownerUUID != null) {
            Player player = plugin.getServer().getPlayer(ownerUUID);
            if (player != null) {
                try {
                    grave.setOwnerTexture(SkinTextureUtil.getTexture(player));
                } catch (Throwable ignored) {
                }
                try {
                    grave.setOwnerTextureSignature(SkinSignatureUtil.getSignature(player));
                } catch (Throwable ignored) {
                }
            }
        }

        grave.setTimeCreation(System.currentTimeMillis());
        long timeAliveMs = plugin.getConfig("grave.time", grave).getInt("grave.time", 0) * 1000L;
        grave.setTimeAlive(timeAliveMs);

        grave.setProtection(angelChest.getBoolean("isProtected", false));
        grave.setExperience(angelChest.getInt("experience", 0));

        if (angelChest.isConfigurationSection("deathCause")) {
            String damageCause = angelChest.getString("deathCause.damageCause", "VOID");
            String killer = angelChest.getString("deathCause.killer", "null");
            grave.setKillerName(!"null".equalsIgnoreCase(killer)
                    ? killer
                    : StringUtil.format(damageCause));
        }

        List<ItemStack> itemStackList = new ArrayList<>();
        itemStackList.addAll(readItemList(angelChest, "armorInv", true));
        itemStackList.addAll(readItemList(angelChest, "storageInv", false));
        itemStackList.addAll(readItemList(angelChest, "extraInv", false));

        if (!itemStackList.isEmpty() && grave.getLocationDeath() != null) {
            String title = StringUtil.parseString(
                    plugin.getConfig("gui.grave.title", grave).getString("gui.grave.title", "Grave"),
                    grave.getLocationDeath(), grave, plugin
            );

            Grave.StorageMode storageMode = plugin.getGraveManager()
                    .getStorageMode(plugin.getConfig("storage.mode", grave).getString("storage.mode", "INVENTORY"));

            Inventory inventory = plugin.getGraveManager().createGraveInventory(
                    grave, grave.getLocationDeath(), itemStackList, title, storageMode
            );
            grave.setInventory(inventory);
        }

        return grave;
    }

    /**
     * Loads a YAML file and returns its configuration.
     *
     * @param file The file to load.
     * @return The file configuration.
     */
    private FileConfiguration loadFile(File file) {
        if (!file.exists()) return null;
        if (!YAMLUtil.isValidYAML(file)) return null;
        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Reads an ItemList and returns output.
     *
     * @param cfg  The config to read.
     * @param path The path to read.
     * @param reverseIfList whether to reverse
     * @return The item output.
     */
    private List<ItemStack> readItemList(FileConfiguration cfg, String path, boolean reverseIfList) {
        if (!cfg.contains(path)) return Collections.emptyList();

        Object raw = cfg.get(path);
        List<ItemStack> out = new ArrayList<>();

        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            for (Object o : list) {
                if (o instanceof ItemStack) {
                    out.add((ItemStack) o);
                }
            }
            if (!out.isEmpty()) {
                if (reverseIfList) Collections.reverse(out);
                return out;
            }

            for (Object o : list) {
                if (o instanceof Map) {
                    try {
                        Map<?, ?> map = (Map<?, ?>) o;
                        Map<String, Object> m = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> e : map.entrySet()) {
                            if (e.getKey() != null) {
                                m.put(String.valueOf(e.getKey()), e.getValue());
                            }
                        }
                        ItemStack is = ItemStack.deserialize(m);
                        out.add(is);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return out;
    }
}
