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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports graves from external plugins (currently AngelChest) and converts them to GravesX {@link Grave} objects.
 */
public final class ImportManager {

    /** Main plugin instance. */
    private final Graves plugin;

    /** Filename pattern: &lt;player&gt;_&lt;world&gt;_&lt;x&gt;_&lt;y&gt;_&lt;z&gt;.yml (supports negative coords). */
    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "^(.+?)_(.+?)_(-?\\d+)_(-?\\d+)_(-?\\d+)\\.ya?ml$", Pattern.CASE_INSENSITIVE);

    /**
     * Creates a new importer bound to the given plugin instance.
     *
     * @param plugin the GravesX plugin instance
     */
    public ImportManager(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Imports all AngelChest graves found on disk.
     *
     * @return a list of converted {@link Grave} objects
     */
    public List<Grave> importExternalPluginAngelChest() {
        return new ArrayList<>(importAngelChest());
    }

    /**
     * Scans the AngelChest data directory and converts each file into a {@link Grave}.
     *
     * @return a list of converted {@link Grave} objects
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
        if (files == null || files.length == 0) return graveList;

        for (File file : files) {
            Grave grave = convertAngelChestToGrave(file);
            if (grave != null) graveList.add(grave);
        }
        return graveList;
    }

    /**
     * Converts a single AngelChest YAML file into a {@link Grave}, applying fallbacks for world, coords, and metadata.
     *
     * @param file the AngelChest YAML file to convert
     * @return the converted {@link Grave}, or {@code null} if the file is invalid
     */
    public Grave convertAngelChestToGrave(File file) {
        FileConfiguration ac = loadFile(file);
        if (ac == null) return null;

        Grave grave = new Grave(UUID.randomUUID());

        grave.setOwnerType(EntityType.PLAYER);
        UUID ownerUUID = UUIDUtil.getUUID(ac.getString("owner", null));
        grave.setOwnerUUID(ownerUUID);

        String ownerNameFromFile = parseOwnerFromFilename(file.getName());
        if (ownerNameFromFile != null && !ownerNameFromFile.isEmpty()) {
            grave.setOwnerName(ownerNameFromFile);
        } else {
            String logfile = ac.getString("logfile", "");
            String[] split = logfile != null ? logfile.split("_") : new String[0];
            if (split.length > 0 && split[0] != null && !split[0].isEmpty()) {
                grave.setOwnerName(split[0]);
            }
        }

        if (ownerUUID != null) {
            Player player = plugin.getServer().getPlayer(ownerUUID);
            if (player != null) {
                try { grave.setOwnerTexture(SkinTextureUtil.getTexture(player)); } catch (Throwable ignored) {}
                try { grave.setOwnerTextureSignature(SkinSignatureUtil.getSignature(player)); } catch (Throwable ignored) {}
            }
        }

        World world = null;
        UUID worldUUID = UUIDUtil.getUUID(ac.getString("worldid", null));
        if (worldUUID != null) world = plugin.getServer().getWorld(worldUUID);

        if (world == null) {
            UUID worldUUID2 = UUIDUtil.getUUID(ac.getString("customblock.location.worldid", null));
            if (worldUUID2 != null) world = plugin.getServer().getWorld(worldUUID2);
        }
        if (world == null) {
            String worldName = parseWorldFromFilename(file.getName());
            if (worldName != null) world = plugin.getServer().getWorld(worldName);
        }
        if (world == null) {
            String logfile = ac.getString("logfile", "");
            String[] split = logfile != null ? logfile.split("_") : new String[0];
            if (split.length > 1) world = plugin.getServer().getWorld(split[1]);
        }

        Integer x = ac.isInt("x") ? ac.getInt("x") : null;
        Integer y = ac.isInt("y") ? ac.getInt("y") : null;
        Integer z = ac.isInt("z") ? ac.getInt("z") : null;

        if (x == null && ac.isInt("customblock.location.x")) x = ac.getInt("customblock.location.x");
        if (y == null && ac.isInt("customblock.location.y")) y = ac.getInt("customblock.location.y");
        if (z == null && ac.isInt("customblock.location.z")) z = ac.getInt("customblock.location.z");

        if (x == null || y == null || z == null) {
            int[] coords = parseCoordsFromFilename(file.getName());
            if (coords != null) {
                if (x == null) x = coords[0];
                if (y == null) y = coords[1];
                if (z == null) z = coords[2];
            }
        }

        if (world != null && x != null && y != null && z != null) {
            grave.setLocationDeath(new Location(world, x, y, z));
        }

        grave.setTimeCreation(System.currentTimeMillis());
        if (ac.getBoolean("infinite", false)) {
            grave.setTimeAlive(-1L);
        } else {
            long timeAliveMs = plugin.getConfig("grave.time", grave).getInt("grave.time", 0) * 1000L;
            grave.setTimeAlive(timeAliveMs);
        }

        grave.setProtection(ac.getBoolean("isProtected", false));
        grave.setExperience(ac.getInt("experience", 0));

        if (ac.isConfigurationSection("deathCause")) {
            String damageCause = ac.getString("deathCause.damageCause", "VOID");
            String killer = ac.getString("deathCause.killer", "null");
            grave.setKillerName(!"null".equalsIgnoreCase(killer) ? killer : StringUtil.format(damageCause));
        }

        List<ItemStack> armor = readItemList(ac, "armorInv", true);
        List<ItemStack> storage = readItemList(ac, "storageInv", false);
        List<ItemStack> extra = readItemList(ac, "extraInv", false);
        List<ItemStack> overflow = readItemList(ac, "overflowInv", false);

        EnumMap<EquipmentSlot, ItemStack> equip = new EnumMap<>(EquipmentSlot.class);
        if (armor != null) {
            if (armor.size() > 0 && armor.get(0) != null) equip.put(EquipmentSlot.HEAD, armor.get(0));
            if (armor.size() > 1 && armor.get(1) != null) equip.put(EquipmentSlot.CHEST, armor.get(1));
            if (armor.size() > 2 && armor.get(2) != null) equip.put(EquipmentSlot.LEGS, armor.get(2));
            if (armor.size() > 3 && armor.get(3) != null) equip.put(EquipmentSlot.FEET, armor.get(3));
        }
        if (extra != null && !extra.isEmpty() && extra.get(0) != null) {
            equip.put(EquipmentSlot.OFF_HAND, extra.get(0));
        }
        grave.setEquipmentMap(equip);

        List<ItemStack> itemStackList = new ArrayList<>();
        if (armor != null && !armor.isEmpty()) itemStackList.addAll(armor);
        if (storage != null && !storage.isEmpty()) itemStackList.addAll(storage);
        if (extra != null && !extra.isEmpty()) itemStackList.addAll(extra);
        if (overflow != null && !overflow.isEmpty()) itemStackList.addAll(overflow);

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
     * Loads a YAML file if it exists and is valid.
     *
     * @param file the file to load
     * @return the {@link FileConfiguration}, or {@code null} if invalid
     */
    private FileConfiguration loadFile(File file) {
        if (file == null || !file.exists()) return null;
        if (!YAMLUtil.isValidYAML(file)) return null;
        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Reads an item list at the given path, skipping nulls and deserializing map entries.
     *
     * @param cfg the configuration to read from
     * @param path the YAML path to read
     * @param reverseIfList whether to reverse order when the list already contains {@link ItemStack}s
     * @return a list of {@link ItemStack}s (possibly empty)
     */
    private List<ItemStack> readItemList(FileConfiguration cfg, String path, boolean reverseIfList) {
        if (cfg == null || path == null || !cfg.contains(path)) return Collections.emptyList();

        Object raw = cfg.get(path);
        List<ItemStack> out = new ArrayList<>();

        if (raw instanceof List) {
            List<?> list = (List<?>) raw;

            boolean anyStacks = false;
            for (Object o : list) {
                if (o instanceof ItemStack) {
                    out.add((ItemStack) o);
                    anyStacks = true;
                }
            }
            if (anyStacks) {
                if (reverseIfList && !out.isEmpty()) Collections.reverse(out);
                return out;
            }

            for (Object o : list) {
                if (o instanceof Map) {
                    try {
                        Map<?, ?> map = (Map<?, ?>) o;
                        Map<String, Object> m = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> e : map.entrySet()) {
                            if (e.getKey() != null) m.put(String.valueOf(e.getKey()), e.getValue());
                        }
                        ItemStack is = ItemStack.deserialize(m);
                        if (is != null) out.add(is);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return out;
    }

    /**
     * Extracts the player name from an AngelChest filename.
     *
     * @param name the filename (e.g., {@code Player_world_x_y_z.yml})
     * @return the player name, or {@code null} if not matched
     */
    private String parseOwnerFromFilename(String name) {
        Matcher m = FILENAME_PATTERN.matcher(name);
        if (m.matches()) return m.group(1);
        return null;
    }

    /**
     * Extracts the world name from an AngelChest filename.
     *
     * @param name the filename (e.g., {@code Player_world_x_y_z.yml})
     * @return the world name, or {@code null} if not matched
     */
    private String parseWorldFromFilename(String name) {
        Matcher m = FILENAME_PATTERN.matcher(name);
        if (m.matches()) return m.group(2);
        return null;
    }

    /**
     * Extracts integer coordinates from an AngelChest filename.
     *
     * @param name the filename (e.g., {@code Player_world_x_y_z.yml})
     * @return an array {@code [x, y, z]}, or {@code null} if not matched
     */
    private int[] parseCoordsFromFilename(String name) {
        Matcher m = FILENAME_PATTERN.matcher(name);
        if (!m.matches()) return null;
        try {
            int x = Integer.parseInt(m.group(3));
            int y = Integer.parseInt(m.group(4));
            int z = Integer.parseInt(m.group(5));
            return new int[]{x, y, z};
        } catch (Exception ignored) {
            return null;
        }
    }
}
