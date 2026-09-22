package dev.cwhead.GravesX.cache;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.manager.DataManager;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import org.bukkit.Location;

import java.sql.*;
import java.util.*;

/**
 * Stores cache data using the currently configured database.
 */
public final class DatabaseCacheBackend {
    private final Graves plugin;
    private final DataManager data;

    public DatabaseCacheBackend(Graves plugin) {
        this.plugin = plugin;
        this.data = plugin.getDataManager();
    }

    public Map<UUID, Grave> graveMap() {
        return new DatabaseReadMap<>(this::getGrave, () -> uuidKeys("grave", "uuid"),
                id -> exists("grave", "uuid", id), () -> count("grave"));
    }

    public Map<UUID, EntityData> entityMap() {
        return new DatabaseReadMap<>(this::getEntity, this::entityKeys,
                id -> getEntity(id) != null, () -> entityKeys().size());
    }

    public Map<String, ChunkData> chunkMap() {
        return new DatabaseReadMap<>(this::getChunk, this::chunkKeys,
                key -> getChunk(key) != null, () -> chunkKeys().size());
    }

    public Grave getGrave(UUID id) {
        return readGrave("uuid = ?", id.toString());
    }

    public Grave getGrave(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return readGrave("location_death = ?", LocationUtil.locationToString(location));
    }

    public Grave oldestGrave(UUID owner) {
        return owner == null ? null : readGrave("owner_uuid = ? ORDER BY time_creation ASC", owner.toString());
    }

    private Grave readGrave(String condition, String value) {
        if (!data.isStorageReady())
            return null;

        try (Connection connection = data.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM " + table("grave") + " WHERE " + condition)) {
            statement.setString(1, value);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    UUID id = UUID.fromString(rows.getString("uuid"));
                    Grave active = plugin.getCacheManager().getViewedGrave(id);

                    if (active != null)
                        return active;

                    Grave grave = data.resultSetToGrave(rows);

                    if (grave != null)
                        return grave;
                }
                return null;
            }
        } catch (SQLException ex) {
            throw failure("reading grave", ex);
        }
    }

    private Set<UUID> uuidKeys(String name, String column) {
        Set<UUID> keys = new LinkedHashSet<>();
        if (!data.isStorageReady())
            return keys;

        try (Connection connection = data.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT " + column + " FROM " + table(name));
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String value = rows.getString(1);
                try { if (value != null) keys.add(UUID.fromString(value)); }
                catch (IllegalArgumentException ignored) { /* skip malformed identifiers */ }
            }
            return keys;
        } catch (SQLException ex) {
            throw failure("listing " + name, ex);
        }
    }

    private boolean exists(String name, String column, UUID id) {
        if (!data.isStorageReady()) return false;
        try (Connection connection = data.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM " + table(name) + " WHERE " + column + " = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException ex) {
            throw failure("checking " + name, ex);
        }
    }

    private int count(String name) {
        if (!data.isStorageReady()) return 0;
        try (Connection connection = data.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table(name));
             ResultSet rows = statement.executeQuery()) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (SQLException ex) {
            throw failure("counting " + name, ex);
        }
    }

    private Set<UUID> entityKeys() {
        Set<UUID> keys = new LinkedHashSet<>();

        for (String name : data.getPersistentEntityTables().keySet())
            keys.addAll(uuidKeys(name, "uuid_entity"));

        return keys;
    }

    private EntityData getEntity(UUID id) {
        if (!data.isStorageReady())
            return null;

        for (Map.Entry<String, EntityData.Type> table : data.getPersistentEntityTables().entrySet()) {
            try (Connection connection = data.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM " + table(table.getKey()) + " WHERE uuid_entity = ?")) {
                statement.setString(1, id.toString());
                try (ResultSet rows = statement.executeQuery()) {
                    if (rows.next())
                        return entity(rows, table.getValue());
                }
            } catch (SQLException ex) {
                throw failure("reading entity from " + table.getKey(), ex);
            }
        }
        return null;
    }

    private EntityData entity(ResultSet row, EntityData.Type type) throws SQLException {
        Location location = location(row.getString("location"));
        if (location == null) return null;
        try {
            UUID id = UUID.fromString(row.getString("uuid_entity"));
            UUID grave = UUID.fromString(row.getString("uuid_grave"));

            if (type == EntityData.Type.HOLOGRAM) {
                HologramData.Backend backend = HologramData.Backend.ARMOR_STAND;
                String saved = row.getString("backend");

                if (saved != null) {
                    try {
                        backend = HologramData.Backend.valueOf(saved.trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        /* legacy/default backend */
                    }
                }
                return new HologramData(location, id, grave, row.getInt("line"), backend);
            }
            return new EntityData(location, id, grave, type);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

    private Set<String> chunkKeys() {
        Set<String> keys = new LinkedHashSet<>();
        if (!data.isStorageReady()) return keys;
        Set<String> tables = new LinkedHashSet<>(data.getPersistentEntityTables().keySet());
        tables.add("block");
        for (String name : tables) {
            try (Connection connection = data.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT location FROM " + table(name));
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Location location = location(rows.getString(1));
                    if (location != null) keys.add(chunkKey(location));
                }
            } catch (SQLException ex) { throw failure("listing chunks in " + name, ex); }
        }
        return keys;
    }

    private ChunkData getChunk(String key) {
        if (!data.isStorageReady()) return null;
        Location anchor = chunkLocation(key);
        if (anchor == null) return null;
        ChunkData chunk = new ChunkData(anchor.getWorld().getName(), anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4);
        Map<String, EntityData.Type> tables = new LinkedHashMap<>(data.getPersistentEntityTables());
        tables.put("block", null);
        // Locations in the existing schema are world|x|y|z strings. Filter by
        // world in SQL, then by chunk while streaming rows for portability.
        String worldPattern = anchor.getWorld().getName().replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "|%";
        for (Map.Entry<String, EntityData.Type> entry : tables.entrySet()) {
            try (Connection connection = data.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM " + table(entry.getKey()) + " WHERE location LIKE ? ESCAPE '!'")) {
                statement.setString(1, worldPattern);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        Location location = location(rows.getString("location"));
                        if (location == null || !location.getWorld().equals(anchor.getWorld())
                                || (location.getBlockX() >> 4) != chunk.getX()
                                || (location.getBlockZ() >> 4) != chunk.getZ()) continue;
                        if (entry.getValue() == null) {
                            try {
                                String material = rows.getString("replace_material");
                                String blockData = rows.getString("replace_data");
                                chunk.addBlockData(new BlockData(location, UUID.fromString(rows.getString("uuid_grave")),
                                        material != null && blockData != null ? material : "AIR",
                                        material != null && blockData != null ? blockData : "minecraft:air"));
                            } catch (IllegalArgumentException | NullPointerException ignored) { /* malformed row */ }
                        } else {
                            EntityData entity = entity(rows, entry.getValue());
                            if (entity != null) chunk.addEntityData(entity);
                        }
                    }
                }
            } catch (SQLException ex) { throw failure("reading chunk from " + entry.getKey(), ex); }
        }
        return chunk.hasData() ? chunk : null;
    }

    private String chunkKey(Location location) {
        if (plugin.getVersionManager().isFolia()) {
            return location.getWorld().getName() + ":" + (location.getBlockX() >> 4) + "," + (location.getBlockZ() >> 4);
        }
        return LocationUtil.chunkToString(location);
    }

    private Location chunkLocation(String key) {
        try {
            if (!plugin.getVersionManager().isFolia()) return loaded(LocationUtil.chunkStringToLocation(key));
            int split = key.lastIndexOf(':');
            String[] coordinates = key.substring(split + 1).split(",");
            return loaded(new Location(plugin.getServer().getWorld(key.substring(0, split)),
                    Integer.parseInt(coordinates[0]) << 4, 0, Integer.parseInt(coordinates[1]) << 4));
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) { return null; }
    }

    private Location location(String value) {
        if (value == null) return null;
        try { return loaded(LocationUtil.stringToLocation(value)); }
        catch (IllegalArgumentException | IndexOutOfBoundsException ex) { return null; }
    }

    private Location loaded(Location location) {
        return location != null && location.getWorld() != null ? location : null;
    }

    private String table(String suffix) { return data.getStoragePrefix() + suffix; }
    private CacheCodec.CacheException failure(String operation, SQLException ex) {
        return new CacheCodec.CacheException("Failed " + operation + " from persistent database (cache disabled)", ex);
    }
}