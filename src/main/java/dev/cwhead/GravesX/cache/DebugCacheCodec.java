package dev.cwhead.GravesX.cache;

import com.ranull.graves.Graves;
import com.ranull.graves.data.*;
import com.ranull.graves.type.Grave;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.*;

import java.util.*;

public final class DebugCacheCodec {
    public static final String HEADER = "# gravesx-readable-v1\n";

    private DebugCacheCodec() {}

    public static boolean enabled(Graves plugin) {
        return plugin != null
                && CacheType.fromString(plugin.getConfig().getString("settings.cache.type", "NORMAL")) != CacheType.NORMAL
                && plugin.getConfig().getBoolean("cache.debug", plugin.getConfig().getBoolean("settings.cache.debug", false));
    }

    public static boolean isText(byte[] data) {
        if (data == null || data.length < HEADER.length())
            return false;

        for (int i = 0; i < HEADER.length(); i++)
            if (data[i] != (byte) HEADER.charAt(i))
                return false;

        return true;
    }

    public static boolean isText(String data) {
        return data != null && data.startsWith(HEADER);
    }

    public static String write(Object value) {
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("data", pack(value));

        return HEADER + yaml.saveToString();
    }

    public static Object read(String text) {
        try {
            YamlConfiguration yaml = new YamlConfiguration();

            yaml.loadFromString(text);

            return unpack(yaml.get("data"));
        } catch (Exception ex) {
            throw new CacheCodec.CacheException("Invalid readable cache data", ex);
        }
    }

    private static Map<String, Object> node(String kind, Object... fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kind", kind);

        for (int i = 0; i < fields.length; i += 2)
            result.put((String) fields[i], pack(fields[i + 1]));

        return result;
    }

    private static Object pack(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean)
            return value;

        if (value instanceof UUID id)
            return node("uuid", "value", id.toString());

        if (value instanceof EquipmentSlot slot)
            return node("equipment-slot", "value", slot.name());

        if (value instanceof ItemStack item) {
            Map<String, Object> itemNode = new LinkedHashMap<>();

            itemNode.put("kind", "item");
            itemNode.put("value", item);

            return itemNode;
        }

        if (value instanceof LocationData loc)
            return node("location-data",
                "world-uuid", loc.getWorldUUID(), "world-key", loc.getWorldKey(), "world-name", loc.getWorldName(),
                "x", loc.getX(), "y", loc.getY(), "z", loc.getZ(), "yaw", loc.getYaw(), "pitch", loc.getPitch());

        if (value instanceof Location loc)
            return node("location", "data", new LocationData(loc));

        if (value instanceof Grave grave)
            return graveNode(grave);

        if (value instanceof ChunkData chunk)
            return node("chunk", "world", chunk.getWorldName(), "x", chunk.getX(), "z", chunk.getZ(),
                "blocks", new ArrayList<>(chunk.getBlockDataMap().values()), "entities", new ArrayList<>(chunk.getEntityDataMap().values()));

        if (value instanceof BlockData block)
            return node("block", "location", block.getLocation(), "grave", block.getGraveUUID(),
                "replace-material", block.getReplaceMaterial(), "replace-data", block.getReplaceData());

        if (value instanceof HologramData holo)
            return node("hologram", "location", holo.getLocation(), "entity", holo.getUUIDEntity(),
                "grave", holo.getUUIDGrave(), "line", holo.getLine(), "backend", holo.getBackend().name());

        if (value instanceof EntityData entity)
            return node("entity", "location", entity.getLocation(), "entity", entity.getUUIDEntity(),
                "grave", entity.getUUIDGrave(), "entity-type", entity.getType().name());

        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());

            for (Object entry : list)
                result.add(pack(entry));

            return result;
        }

        if (value instanceof Map<?, ?> map) {
            List<Object> entries = new ArrayList<>();

            for (var entry : map.entrySet())
                entries.add(node("entry", "key", entry.getKey(), "value", entry.getValue()));

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("kind", "map");
            result.put("entries", entries);

            return result;
        }

        throw new IllegalArgumentException("Unsupported readable cache value: " + value.getClass().getName());
    }

    private static Object unpack(Object raw) {
        if (raw instanceof List<?> list) {
            List<Object> values = new ArrayList<>(list.size());

            for (Object item : list)
                values.add(unpack(item));

            return values;
        }

        if (!(raw instanceof Map<?, ?>) && !(raw instanceof ConfigurationSection))
            return raw;

        Map<?, ?> map = raw instanceof ConfigurationSection section ? section.getValues(false) : (Map<?, ?>) raw;
        String kind = (String) map.get("kind");

        if (kind == null)
            throw new IllegalArgumentException("Missing readable cache type");

        return switch (kind) {
            case "uuid" ->
                    UUID.fromString((String)
                    map.get("value"));

            case "equipment-slot" ->
                    EquipmentSlot.valueOf((String)
                    map.get("value"));

            case "item" -> {
                Object item = map.get("value");
                if (!(item instanceof ItemStack))
                    throw new IllegalArgumentException("Invalid Bukkit item in readable data");

                yield item;
            }

            case "location-data" ->
                    new LocationData((UUID) field(map, "world-uuid"), (String) field(map, "world-key"),
                    (String) field(map, "world-name"), number(map, "x").doubleValue(), number(map, "y").doubleValue(),
                    number(map, "z").doubleValue(), number(map, "yaw").floatValue(), number(map, "pitch").floatValue());

            case "location" -> ((LocationData) field(map, "data")).getLocation();

            case "grave" -> grave(map);

            case "chunk" -> {
                ChunkData chunk = new ChunkData((String) field(map, "world"), number(map, "x").intValue(), number(map, "z").intValue());

                for (Object block : (List<?>)
                        field(map, "blocks")) chunk.addBlockData((BlockData) block);

                for (Object entity : (List<?>)
                        field(map, "entities")) chunk.addEntityData((EntityData) entity);

                yield chunk;
            }

            case "block" -> new BlockData((Location) field(map, "location"), (UUID) field(map, "grave"),
                    (String) field(map, "replace-material"), (String) field(map, "replace-data"));

            case "hologram" -> new HologramData((Location) field(map, "location"), (UUID) field(map, "entity"), (UUID) field(map, "grave"),
                    number(map, "line").intValue(), HologramData.Backend.valueOf((String) field(map, "backend")));

            case "entity" -> new EntityData((Location) field(map, "location"), (UUID) field(map, "entity"), (UUID) field(map, "grave"),
                    EntityData.Type.valueOf((String) field(map, "entity-type")));

            case "entry" -> new AbstractMap.SimpleImmutableEntry<>(field(map, "key"), field(map, "value"));

            case "map" -> {
                Map<Object, Object> values = new LinkedHashMap<>();
                for (Object entry : (List<?>) field(map, "entries")) {
                    Map.Entry<?, ?> pair = (Map.Entry<?, ?>) entry;
                    values.put(pair.getKey(), pair.getValue());
                }
                yield values;
            }
            default -> throw new IllegalArgumentException("Unknown readable cache type: " + kind);
        };
    }

    private static Object field(Map<?, ?> map, String key) {
        return unpack(map.get(key));
    }

    private static Number number(Map<?, ?> map, String key) {
        return (Number) field(map, key);
    }

    private static Map<String, Object> graveNode(Grave grave) {
        return node("grave", "uuid", grave.getUUID(),
                "location", grave.getLocationDeathData(), "yaw", grave.getYaw(), "pitch", grave.getPitch(),
                "owner-type", grave.getOwnerType() == null ? null : grave.getOwnerType().name(),
                "owner-name", grave.getOwnerName(), "owner-display", grave.getOwnerNameDisplay(), "owner-uuid", grave.getOwnerUUID(),
                "owner-texture", grave.getOwnerTexture(), "owner-signature", grave.getOwnerTextureSignature(),
                "killer-type", grave.getKillerType() == null ? null : grave.getKillerType().name(),
                "killer-name", grave.getKillerName(), "killer-display", grave.getKillerNameDisplay(), "killer-uuid", grave.getKillerUUID(),
                "experience", grave.getExperience(), "death-cause", grave.getDeathCause(), "protection", grave.getProtection(),
                "abandoned", grave.isAbandoned(), "time-alive", grave.getTimeAlive(), "time-created", grave.getTimeCreation(),
                "time-protection", grave.getTimeProtection(), "preview", grave.getGravePreview(), "provider", grave.getProviderId(),
                "permissions", grave.getPermissionList(), "equipment", grave.getEquipmentMap(),
                "inventory", grave.getInventory() == null ? null : Arrays.asList(grave.getInventory().getContents()));
    }

    @SuppressWarnings("unchecked")
    private static Grave grave(Map<?, ?> map) {
        Grave grave = new Grave((UUID) field(map, "uuid"));
        grave.setLocationDeathData((LocationData) field(map, "location"));
        grave.setYaw(number(map, "yaw").floatValue()); grave.setPitch(number(map, "pitch").floatValue());
        String ownerType = (String) field(map, "owner-type"), killerType = (String) field(map, "killer-type");
        grave.setOwnerType(ownerType == null ? null : EntityType.valueOf(ownerType));
        grave.setOwnerName((String) field(map, "owner-name")); grave.setOwnerNameDisplay((String) field(map, "owner-display"));
        grave.setOwnerUUID((UUID) field(map, "owner-uuid")); grave.setOwnerTexture((String) field(map, "owner-texture"));
        grave.setOwnerTextureSignature((String) field(map, "owner-signature"));
        grave.setKillerType(killerType == null ? null : EntityType.valueOf(killerType));
        grave.setKillerName((String) field(map, "killer-name")); grave.setKillerNameDisplay((String) field(map, "killer-display"));
        grave.setKillerUUID((UUID) field(map, "killer-uuid")); grave.setExperience(number(map, "experience").intValue());
        grave.setDeathCause((String) field(map, "death-cause")); grave.setProtection((Boolean) field(map, "protection"));
        grave.setAbandoned((Boolean) field(map, "abandoned")); grave.setTimeAlive(number(map, "time-alive").longValue());
        grave.setTimeCreation(number(map, "time-created").longValue()); grave.setTimeProtection(number(map, "time-protection").longValue());
        grave.setGravePreview((Boolean) field(map, "preview")); grave.setProviderId((String) field(map, "provider"));
        grave.setPermissionList((List<String>) field(map, "permissions"));
        Map<?, ?> equipment = (Map<?, ?>) field(map, "equipment");
        Map<EquipmentSlot, ItemStack> slots = new EnumMap<>(EquipmentSlot.class);

        if (equipment != null)
            equipment.forEach((slot, item) -> slots.put((EquipmentSlot) slot, (ItemStack) item));
        grave.setEquipmentMap(slots);
        List<?> contents = (List<?>) field(map, "inventory");

        if (contents != null) {
            Inventory inventory = Bukkit.createInventory(grave, Math.max(9, ((contents.size() + 8) / 9) * 9));
            inventory.setContents(contents.toArray(new ItemStack[0]));
            grave.setInventory(inventory);
        }

        return grave;
    }
}
