package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.StringUtil;
import com.ranull.graves.util.UUIDUtil;
import com.ranull.graves.util.YAMLUtil;
import dev.cwhead.GravesX.util.SkinTextureUtil_post_1_21_9;
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
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports graves from external plugins (currently AngelChest) and converts them to GravesX {@link Grave} objects.
 */
public final class ImportManager {

    /**
     * Main plugin instance.
     */
    private final Graves plugin;

    /**
     * Filename pattern: player_world_x_y_z.yml (supports negative coords).
     */
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
     * Counts AngelChest graves that will import successfully
     *
     * @return the number of graves that can be imported from AngelChest
     */
    public long countAngelChestImportableOnly() {
        File[] files = listAngelChestFiles();
        if (files == null || files.length == 0) return 0;

        long ok = 0;
        for (File f : files) {
            FileConfiguration ac = loadFile(f);
            if (ac == null) continue;

            World world = resolveWorldForScan(ac, f.getName());
            if (world == null) continue;

            Integer x = ac.isInt("x") ? ac.getInt("x") : null;
            Integer y = ac.isInt("y") ? ac.getInt("y") : null;
            Integer z = ac.isInt("z") ? ac.getInt("z") : null;

            if (x == null && ac.isInt("customblock.location.x")) x = ac.getInt("customblock.location.x");
            if (y == null && ac.isInt("customblock.location.y")) y = ac.getInt("customblock.location.y");
            if (z == null && ac.isInt("customblock.location.z")) z = ac.getInt("customblock.location.z");

            if (x == null || y == null || z == null) {
                int[] coords = parseCoordsFromFilename(f.getName());
                if (coords != null) {
                    if (x == null) x = coords[0];
                    if (y == null) y = coords[1];
                    if (z == null) z = coords[2];
                }
            }

            if (x == null || y == null || z == null) continue;

            ok++;
        }
        return ok;
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
     * Dry-run scan (text): counts total/importable/missing-world/invalid-YAML AngelChest files.
     * Does NOT create graves or inventories.
     *
     * @return multiline human-readable summary
     */
    public String countAngelChestStatusText() {
        File[] files = listAngelChestFiles();
        if (files == null || files.length == 0) {
            return "No files found in plugins/AngelChest/angelchests. Returning as none.";
        }

        int total = files.length;
        int valid = 0;
        int importable = 0;
        int missingWorld = 0;
        int invalid = 0;

        for (File f : files) {
            FileConfiguration ac = loadFile(f);
            if (ac == null) {
                invalid++;
                continue;
            }
            valid++;

            World world = resolveWorldForScan(ac, f.getName());
            if (world != null) importable++;
            else missingWorld++;
        }

        StringBuilder sb = new StringBuilder(256);
        sb.append("AngelChest Import Scan\n");
        sb.append("  Total files: ").append(total).append('\n');
        sb.append("  Valid YAML: ").append(valid).append('\n');
        sb.append("  Importable: ").append(importable).append('\n');
        sb.append("  Missing world: ").append(missingWorld).append('\n');
        sb.append("  Invalid YAML: ").append(invalid);
        return sb.toString();
    }

    /**
     * Dry-run scan (text): list files whose world cannot be resolved on this server,
     * including helpful hints (UUIDs/names/coords) to aid manual fixes or world restores.
     *
     * @return multiline human-readable list of missing-world entries
     */
    public String listAngelChestMissingWorldText() {
        File[] files = listAngelChestFiles();
        if (files == null || files.length == 0) {
            return "No files found in plugins/AngelChest/angelchests. Returning as none.";
        }

        StringBuilder sb = new StringBuilder(512);
        int rows = 0;

        for (File f : files) {
            FileConfiguration ac = loadFile(f);
            if (ac == null) continue;

            World resolved = resolveWorldForScan(ac, f.getName());
            if (resolved != null) continue; // only list unresolved worlds

            String ownerName = parseOwnerFromFilename(f.getName());
            if (ownerName == null || ownerName.isEmpty()) {
                String logfile = ac.getString("logfile", "");
                String[] split = logfile != null ? logfile.split("_") : new String[0];
                if (split.length > 0 && split[0] != null && !split[0].isEmpty()) ownerName = split[0];
            }
            UUID ownerUUID = UUIDUtil.getUUID(ac.getString("owner", null));

            UUID worldUUID1 = UUIDUtil.getUUID(ac.getString("worldid", null));
            UUID worldUUID2 = UUIDUtil.getUUID(ac.getString("customblock.location.worldid", null));
            String worldFromFile = parseWorldFromFilename(f.getName());
            String worldFromLogfile = null;
            String logfile = ac.getString("logfile", "");
            String[] split = logfile != null ? logfile.split("_") : new String[0];
            if (split.length > 1) worldFromLogfile = split[1];

            Integer x = ac.isInt("x") ? ac.getInt("x") : null;
            Integer y = ac.isInt("y") ? ac.getInt("y") : null;
            Integer z = ac.isInt("z") ? ac.getInt("z") : null;
            if (x == null && ac.isInt("customblock.location.x")) x = ac.getInt("customblock.location.x");
            if (y == null && ac.isInt("customblock.location.y")) y = ac.getInt("customblock.location.y");
            if (z == null && ac.isInt("customblock.location.z")) z = ac.getInt("customblock.location.z");
            if (x == null || y == null || z == null) {
                int[] coords = parseCoordsFromFilename(f.getName());
                if (coords != null) {
                    if (x == null) x = coords[0];
                    if (y == null) y = coords[1];
                    if (z == null) z = coords[2];
                }
            }

            if (rows == 0) {
                sb.append("AngelChest Missing-World Report\n");
                sb.append("  (These graves cannot import until the referenced world exists)\n");
            }
            rows++;

            sb.append("• File: ").append(f.getName()).append('\n');
            sb.append("    Owner: ").append(nullOr(ownerName))
                    .append("  UUID: ").append(nullOr(ownerUUID)).append('\n');
            sb.append("    World UUIDs: primary=").append(nullOr(worldUUID1))
                    .append(" secondary=").append(nullOr(worldUUID2)).append('\n');
            sb.append("    World names: file=").append(nullOr(worldFromFile))
                    .append(" logfile=").append(nullOr(worldFromLogfile)).append('\n');
            sb.append("    Coords: ").append(nullOr(x)).append(',').append(nullOr(y)).append(',').append(nullOr(z)).append('\n');
        }

        if (rows == 0) {
            return "AngelChest Missing-World Report\n  None — all referenced worlds are present.";
        }
        return sb.toString();
    }

    private static String nullOr(Object o) {
        return o == null ? "-" : String.valueOf(o);
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
        if (files == null) return graveList;

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
            String[] split = logfile.split("_");
            if (split.length > 0 && split[0] != null && !split[0].isEmpty()) {
                grave.setOwnerName(split[0]);
            }
        }

        if (ownerUUID != null) {
            Player player = null;
            try {
                player = plugin.getGravesXScheduler().callSyncMethod(() -> plugin.getServer().getPlayer(ownerUUID)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (player != null) {
                try {
                    if (plugin.getVersionManager().isPost1_21_9()) {
                        grave.setOwnerTexture(SkinTextureUtil_post_1_21_9.getTexture(player));
                    } else {
                        grave.setOwnerTexture(SkinTextureUtil.getTexture(player));
                    }

                } catch (Throwable ignored) {
                }
                try {
                    grave.setOwnerTextureSignature(SkinSignatureUtil.getSignature(player));
                } catch (Throwable ignored) {
                }
            }
        }

        World world = resolveWorldForScan(ac, file.getName()); // reuse logic (already Folia-safe)
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

        grave.setTimeCreation(ac.getLong("created", System.currentTimeMillis()));
        if (ac.getBoolean("infinite", false)) {
            grave.setTimeAlive(-1);
        } else {
            long alive = resolveTimeAliveMillis(ac, grave);
            grave.setTimeAlive(alive);
            grave.setTimeAliveRemaining(alive);
        }

        grave.setProtection(ac.getBoolean("isProtected", false));
        long protectionTimeRemaining = ac.getLong("unlockIn", 0);
        if (protectionTimeRemaining == -1) {
            grave.setTimeProtection(-1);
        } else {
            grave.setTimeProtection(resolveProtectionMillis(ac, grave));
        }
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
        if (!armor.isEmpty()) itemStackList.addAll(armor);
        if (!storage.isEmpty()) itemStackList.addAll(storage);
        if (!extra.isEmpty()) itemStackList.addAll(extra);
        if (!overflow.isEmpty()) itemStackList.addAll(overflow);

        if (!itemStackList.isEmpty() && grave.getLocationDeath() != null) {
            String title = StringUtil.parseString(
                    plugin.getConfig("gui.grave.title", grave).getString("gui.grave.title", "Grave"),
                    grave.getLocationDeath(), grave, plugin
            );

            Grave.StorageMode storageMode = plugin.getGraveManager()
                    .getStorageMode(plugin.getConfig("storage.mode", grave).getString("storage.mode", "INVENTORY"));

            Inventory inventory = null;
            try {
                inventory = plugin.getGravesXScheduler().callSyncMethod(() ->
                        plugin.getGraveManager().createGraveInventory(
                                grave, grave.getLocationDeath(), itemStackList, title, storageMode
                        )
                ).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

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
     */
    private List<ItemStack> readItemList(FileConfiguration cfg, String path, boolean reverseIfList) {
        if (cfg == null || path == null || !cfg.contains(path)) return Collections.emptyList();

        Object raw = cfg.get(path);
        List<ItemStack> out = new ArrayList<>();

        if (raw instanceof List<?> list) {
            boolean anyStacks = false;
            for (Object o : list) {
                if (o instanceof ItemStack is) {
                    out.add(is);
                    anyStacks = true;
                }
            }
            if (anyStacks) {
                if (reverseIfList && !out.isEmpty()) Collections.reverse(out);
                return out;
            }

            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    try {
                        Map<String, Object> m = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> e : map.entrySet()) {
                            if (e.getKey() != null) m.put(String.valueOf(e.getKey()), e.getValue());
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

    /**
     * Resolve world for scanning/import decisions (tries UUIDs, filename world, logfile world).
     * Returns null if the world is not present on this server.
     */
    private World resolveWorldForScan(FileConfiguration ac, String fileName) {
        UUID worldUUID = UUIDUtil.getUUID(ac.getString("worldid", null));
        World world = null;
        try {
            world = worldUUID != null
                    ? plugin.getGravesXScheduler().callSyncMethod(() -> plugin.getServer().getWorld(worldUUID)).get()
                    : null;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (world == null) {
            UUID worldUUID2 = UUIDUtil.getUUID(ac.getString("customblock.location.worldid", null));
            try {
                world = worldUUID2 != null
                        ? plugin.getGravesXScheduler().callSyncMethod(() -> plugin.getServer().getWorld(worldUUID2)).get()
                        : null;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        if (world == null) {
            String worldNameFromFile = parseWorldFromFilename(fileName);
            if (worldNameFromFile != null) {
                String name = worldNameFromFile;
                try {
                    world = plugin.getGravesXScheduler().callSyncMethod(() -> plugin.getServer().getWorld(name)).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (world == null) {
            String logfile = ac.getString("logfile", "");
            String[] split = logfile.split("_");
            if (split.length > 1) {
                String name = split[1];
                try {
                    world = plugin.getGravesXScheduler().callSyncMethod(() -> plugin.getServer().getWorld(name)).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return world;
    }

    /**
     * Extracts the player name from an AngelChest filename.
     */
    private String parseOwnerFromFilename(String name) {
        Matcher m = FILENAME_PATTERN.matcher(name);
        if (m.matches()) return m.group(1);
        return null;
    }

    /**
     * Extracts the world name from an AngelChest filename.
     */
    private String parseWorldFromFilename(String name) {
        Matcher m = FILENAME_PATTERN.matcher(name);
        if (m.matches()) return m.group(2);
        return null;
    }

    /**
     * Extracts integer coordinates from an AngelChest filename.
     *
     * @return [x,y,z] or null if not matched
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

    private File[] listAngelChestFiles() {
        File base = new File(plugin.getPluginsFolder(), "AngelChest");
        if (!base.exists()) return null;
        File dir = new File(base, "angelchests");
        if (!dir.exists()) return null;
        return dir.listFiles((d, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return (lower.endsWith(".yml") || lower.endsWith(".yaml")) && !name.startsWith(".");
        });
    }

    private long resolveTimeAliveMillis(FileConfiguration ac, Grave grave) {
        if (ac.getBoolean("infinite", false)) {
            return -1L;
        }

        long secondsLeft = ac.getLong("secondsLeft", -1L);
        if (secondsLeft >= 0L) {
            return secondsLeft * 1000L;
        }

        return plugin.getConfig("grave.time", grave).getInt("grave.time", 0) * 1000L;
    }

    private long resolveProtectionMillis(FileConfiguration ac, Grave grave) {
        long secondsLeft = ac.getLong("unlockIn", -1L);
        if (secondsLeft >= 0L) {
            return secondsLeft * 1000L;
        }

        return plugin.getConfig("grave.time", grave).getInt("grave.time", 0) * 1000L;
    }
}