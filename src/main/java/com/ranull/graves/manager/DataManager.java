package com.ranull.graves.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.ranull.graves.Graves;
import com.ranull.graves.data.*;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.*;

public final class DataManager {
    private final Graves plugin;
    private Type type;
    private HikariDataSource dataSource;

    public DataManager(Graves plugin) {
        this.plugin = plugin;

        String typeStr = plugin.getConfig().getString("settings.storage.type", "SQLITE");
        try {
            this.type = Type.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.type = Type.INVALID;
        }

        switch (this.type) {
            case SQLITE:
                loadType(Type.SQLITE);
                load();
                keepConnectionAlive(); // If we don't enable this, connection will close or time out :/
                break;
            case MYSQL:
            case MARIADB:
                loadType(this.type);
                if (testMySQLConnection()) {
                    migrateToMySQL();
                    load();
                    keepConnectionAlive(); // If we don't enable this, connection will close or time out :/
                } else {
                    plugin.getLogger().severe("Failed to connect to MySQL database. Disabling plugin...");
                    plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                }
                break;
            default:
                plugin.getLogger().severe("Database Type is invalid. Only valid options: SQLITE and MYSQL. Disabling plugin...");
                plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                return;
        }
    }

    public enum Type {
        SQLITE,
        MYSQL,
        MARIADB,
        INVALID
    }

    private void load() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                loadTables();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            loadGraveMap();
            loadBlockMap();
            loadEntityMap("armorstand", EntityData.Type.ARMOR_STAND);
            loadEntityMap("itemframe", EntityData.Type.ITEM_FRAME);
            loadHologramMap();

            if (plugin.getIntegrationManager().hasFurnitureLib()) {
                loadEntityDataMap("furniturelib", EntityData.Type.FURNITURELIB);
            }

            if (plugin.getIntegrationManager().hasFurnitureEngine()) {
                loadEntityDataMap("furnitureengine", EntityData.Type.FURNITUREENGINE);
            }

            if (plugin.getIntegrationManager().hasItemsAdder()) {
                loadEntityDataMap("itemsadder", EntityData.Type.ITEMSADDER);
            }

            if (plugin.getIntegrationManager().hasOraxen()) {
                loadEntityDataMap("oraxen", EntityData.Type.ORAXEN);
            }

            if (plugin.getIntegrationManager().hasPlayerNPC()) {
                loadEntityDataMap("playernpc", EntityData.Type.PLAYERNPC);
                plugin.getIntegrationManager().getPlayerNPC().createCorpses();
            }
        });
    }

    private void loadTables() throws SQLException {
        setupGraveTable();
        setupBlockTable();
        setupHologramTable();
        setupEntityTable("armorstand");
        setupEntityTable("itemframe");

        if (plugin.getIntegrationManager().hasFurnitureLib()) {
            setupEntityTable("furniturelib");
        }

        if (plugin.getIntegrationManager().hasFurnitureEngine()) {
            setupEntityTable("furnitureengine");
        }

        if (plugin.getIntegrationManager().hasItemsAdder()) {
            setupEntityTable("itemsadder");
        }

        if (plugin.getIntegrationManager().hasOraxen()) {
            setupEntityTable("oraxen");
        }

        if (plugin.getIntegrationManager().hasPlayerNPC()) {
            setupEntityTable("playernpc");
        }
    }

    public void reload() {
        reload(type);
    }

    public void reload(Type type) {
        loadType(type);
        if ((type == Type.MYSQL || type == Type.MARIADB) && !testMySQLConnection()) {
            plugin.getLogger().severe("Failed to connect to MySQL database. Disabling plugin...");
            plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            return;
        }
        load();
    }

    public void loadType(Type type) {
        this.type = type;
        if (type == Type.SQLITE) {
            migrateRootDataSubData();
            HikariConfig config = new HikariConfig();
            configureSQLite(config);
            dataSource = new HikariDataSource(config);

        } else {
            // MySQL or MariaDB configuration
            String host = plugin.getConfig().getString("settings.storage.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("settings.storage.mysql.port", 3306);
            String user = plugin.getConfig().getString("settings.storage.mysql.username", "username");
            String password = plugin.getConfig().getString("settings.storage.mysql.password", "password");
            String database = plugin.getConfig().getString("settings.storage.mysql.database", "graves");
            long maxLifetime = plugin.getConfig().getLong("settings.storage.mysql.maxLifetime", 1800000);
            int maxConnections = plugin.getConfig().getInt("settings.storage.mysql.maxConnections", 20); // Increased pool size
            long connectionTimeout = plugin.getConfig().getLong("settings.storage.mysql.connectionTimeout", 30000);
            boolean useSSL = plugin.getConfig().getBoolean("settings.storage.mysql.useSSL", true);
            boolean allowPublicKeyRetrieval = plugin.getConfig().getBoolean("settings.storage.mysql.allowPublicKeyRetrieval", false);
            boolean verifyServerCertificate = plugin.getConfig().getBoolean("settings.storage.mysql.verifyServerCertificate", false);

            HikariConfig config = new HikariConfig();
            if (type == Type.MARIADB) {
                config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
            } else {
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            }
            config.setUsername(user);
            config.setPassword(password);
            config.addDataSourceProperty("autoReconnect", "true");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            config.addDataSourceProperty("alwaysSendSetIsolation", "false");
            config.addDataSourceProperty("cacheCallableStmts", "true");
            config.addDataSourceProperty("useSSL", String.valueOf(useSSL));
            config.addDataSourceProperty("allowPublicKeyRetrieval", String.valueOf(allowPublicKeyRetrieval));
            config.addDataSourceProperty("verifyServerCertificate", String.valueOf(verifyServerCertificate));
            config.setMaximumPoolSize(maxConnections);
            config.setMaxLifetime(maxLifetime);
            config.setMinimumIdle(2);
            config.setPoolName("Graves");
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(600000); // 10 minutes
            config.setConnectionTestQuery("SELECT 1");
            config.setLeakDetectionThreshold(15000); // Detect connection leaks

            if (type == Type.MARIADB) {
                config.setDriverClassName("com.ranull.graves.mariadb.jdbc.Driver");
            } else {
                config.setDriverClassName("com.ranull.graves.mysql.cj.jdbc.Driver");
            }

            dataSource = new HikariDataSource(config);

            if (testMySQLConnection()) {
                migrateToMySQL();
            }
        }
    }

    private void configureSQLite(HikariConfig config) {
        config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "data" + File.separator + "data.db");
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setMaximumPoolSize(50); // Might as well increase this.
        config.addDataSourceProperty("autoReconnect", "true");
        config.setDriverClassName("org.sqlite.JDBC");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void migrateRootDataSubData() {
        new File(plugin.getDataFolder(), "data").mkdirs();
        File[] files = plugin.getDataFolder().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("data.db")) {
                    FileUtil.moveFile(file, "data" + File.separator + file.getName());
                }
            }
        }
    }

    public boolean hasChunkData(Location location) {
        return plugin.getCacheManager().getChunkMap().containsKey(LocationUtil.chunkToString(location));
    }

    public ChunkData getChunkData(Location location) {
        String chunkString = LocationUtil.chunkToString(location);
        ChunkData chunkData;

        if (plugin.getCacheManager().getChunkMap().containsKey(chunkString)) {
            chunkData = plugin.getCacheManager().getChunkMap().get(chunkString);
        } else {
            chunkData = new ChunkData(location);
            plugin.getCacheManager().getChunkMap().put(chunkString, chunkData);
        }
        return chunkData;
    }

    public void removeChunkData(ChunkData chunkData) {
        plugin.getCacheManager().getChunkMap().remove(LocationUtil.chunkToString(chunkData.getLocation()));
    }

    public List<String> getColumnList(String tableName) {
        List<String> columnList = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            if (type == Type.MYSQL || type == Type.MARIADB) {
                resultSet = executeQuery("DESCRIBE " + tableName + ";");
                while (resultSet != null && resultSet.next()) {
                    columnList.add(resultSet.getString("Field"));
                }
            } else if (type == Type.SQLITE) {
                resultSet = executeQuery("PRAGMA table_info(" + tableName + ");");
                while (resultSet != null && resultSet.next()) {
                    columnList.add(resultSet.getString("name"));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        } finally {
            closeResultSet(resultSet);
        }
        return columnList;
    }

    public boolean tableExists(String tableName) {
        ResultSet resultSet = null;
        try {
            if (type == Type.MYSQL || type == Type.MARIADB) {
                resultSet = executeQuery("SHOW TABLES LIKE '" + tableName + "';");
            } else {
                resultSet = executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "';");
            }
            return resultSet != null && resultSet.next();
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        } finally {
            closeResultSet(resultSet);
        }
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) throws SQLException {
        List<String> columnList = getColumnList(tableName);
        if (!columnList.contains(columnName)) {
            executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition + ";");
        }
    }

    public void setupGraveTable() throws SQLException {
        String name = "grave";
        if (!tableExists(name)) {
            executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                    "uuid VARCHAR(255) UNIQUE,\n" +
                    "owner_type VARCHAR(255),\n" +
                    "owner_name VARCHAR(255),\n" +
                    "owner_name_display VARCHAR(255),\n" +
                    "owner_uuid VARCHAR(255),\n" +
                    "owner_texture TEXT,\n" +
                    "owner_texture_signature TEXT,\n" +
                    "killer_type VARCHAR(255),\n" +
                    "killer_name VARCHAR(255),\n" +
                    "killer_name_display VARCHAR(255),\n" +
                    "killer_uuid VARCHAR(255),\n" +
                    "location_death VARCHAR(255),\n" +
                    "yaw FLOAT(16),\n" +
                    "pitch FLOAT(16),\n" +
                    "inventory TEXT,\n" +
                    "equipment TEXT,\n" +
                    "experience INT(16),\n" +
                    "protection INT(1),\n" +
                    "time_alive BIGINT,\n" +
                    "time_protection BIGINT,\n" +
                    "time_creation BIGINT,\n" +
                    "permissions TEXT);");
        }

        addColumnIfNotExists(name, "uuid", "VARCHAR(255) UNIQUE");
        addColumnIfNotExists(name, "owner_type", "VARCHAR(255)");
        addColumnIfNotExists(name, "owner_name", "VARCHAR(255)");
        addColumnIfNotExists(name, "owner_name_display", "VARCHAR(255)");
        addColumnIfNotExists(name, "owner_uuid", "VARCHAR(255)");
        addColumnIfNotExists(name, "owner_texture", "TEXT");
        addColumnIfNotExists(name, "owner_texture_signature", "TEXT");
        addColumnIfNotExists(name, "killer_type", "VARCHAR(255)");
        addColumnIfNotExists(name, "killer_name", "VARCHAR(255)");
        addColumnIfNotExists(name, "killer_name_display", "VARCHAR(255)");
        addColumnIfNotExists(name, "killer_uuid", "VARCHAR(255)");
        addColumnIfNotExists(name, "location_death", "VARCHAR(255)");
        addColumnIfNotExists(name, "yaw", "FLOAT(16)");
        addColumnIfNotExists(name, "pitch", "FLOAT(16)");
        addColumnIfNotExists(name, "inventory", "TEXT");
        addColumnIfNotExists(name, "equipment", "TEXT");
        addColumnIfNotExists(name, "experience", "INT(16)");
        addColumnIfNotExists(name, "protection", "INT(1)");
        addColumnIfNotExists(name, "time_alive", "BIGINT");
        addColumnIfNotExists(name, "time_protection", "BIGINT");
        addColumnIfNotExists(name, "time_creation", "BIGINT");
        addColumnIfNotExists(name, "permissions", "TEXT");
    }

    public void setupBlockTable() throws SQLException {
        String name = "block";
        if (!tableExists(name)) {
            executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                    "location VARCHAR(255),\n" +
                    "uuid_grave VARCHAR(255),\n" +
                    "replace_material VARCHAR(255),\n" +
                    "replace_data TEXT);");
        }

        addColumnIfNotExists(name, "location", "VARCHAR(255)");
        addColumnIfNotExists(name, "uuid_grave", "VARCHAR(255)");
        addColumnIfNotExists(name, "replace_material", "VARCHAR(255)");
        addColumnIfNotExists(name, "replace_data", "TEXT");
    }

    public void setupHologramTable() throws SQLException {
        String name = "hologram";
        if (!tableExists(name)) {
            executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                    "uuid_entity VARCHAR(255),\n" +
                    "uuid_grave VARCHAR(255),\n" +
                    "line INT(16),\n" +
                    "location VARCHAR(255));");
        }

        addColumnIfNotExists(name, "uuid_entity", "VARCHAR(255)");
        addColumnIfNotExists(name, "uuid_grave", "VARCHAR(255)");
        addColumnIfNotExists(name, "line", "INT(16)");
        addColumnIfNotExists(name, "location", "VARCHAR(255)");
    }

    private void setupEntityTable(String name) throws SQLException {
        if (!tableExists(name)) {
            executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                    "location VARCHAR(255),\n" +
                    "uuid_entity VARCHAR(255),\n" +
                    "uuid_grave VARCHAR(255));");
        }

        addColumnIfNotExists(name, "location", "VARCHAR(255)");
        addColumnIfNotExists(name, "uuid_entity", "VARCHAR(255)");
        addColumnIfNotExists(name, "uuid_grave", "VARCHAR(255)");
    }

    private void loadGraveMap() {
        plugin.getCacheManager().getGraveMap().clear();

        try (ResultSet resultSet = executeQuery("SELECT * FROM grave;")) {
            while (resultSet != null && resultSet.next()) {
                Grave grave = resultSetToGrave(resultSet);
                if (grave != null) {
                    plugin.getCacheManager().getGraveMap().put(grave.getUUID(), grave);
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void loadBlockMap() {
        try (ResultSet resultSet = executeQuery("SELECT * FROM block;")) {
            while (resultSet != null && resultSet.next()) {
                Location location = LocationUtil.stringToLocation(resultSet.getString("location"));
                UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));
                String replaceMaterial = resultSet.getString("replace_material");
                String replaceData = resultSet.getString("replace_data");

                getChunkData(location).addBlockData(new BlockData(location, uuidGrave, replaceMaterial, replaceData));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void loadEntityMap(String table, EntityData.Type type) {
        try (ResultSet resultSet = executeQuery("SELECT * FROM " + table + ";")) {
            while (resultSet != null && resultSet.next()) {
                Location location = null;

                if (resultSet.getString("location") != null) {
                    location = LocationUtil.stringToLocation(resultSet.getString("location"));
                } else if (resultSet.getString("chunk") != null) {
                    location = LocationUtil.chunkStringToLocation(resultSet.getString("chunk"));
                }

                if (location != null) {
                    UUID uuidEntity = UUID.fromString(resultSet.getString("uuid_entity"));
                    UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));

                    getChunkData(location).addEntityData(new EntityData(location, uuidEntity, uuidGrave, type));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void loadHologramMap() {
        try (ResultSet resultSet = executeQuery("SELECT * FROM hologram;")) {
            while (resultSet != null && resultSet.next()) {
                Location location = null;

                if (resultSet.getString("location") != null) {
                    location = LocationUtil.stringToLocation(resultSet.getString("location"));
                } else if (resultSet.getString("chunk") != null) {
                    location = LocationUtil.chunkStringToLocation(resultSet.getString("chunk"));
                }

                if (location != null) {
                    UUID uuidEntity = UUID.fromString(resultSet.getString("uuid_entity"));
                    UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));
                    int line = resultSet.getInt("line");

                    getChunkData(location).addEntityData(new HologramData(location, uuidEntity, uuidGrave, line));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void loadEntityDataMap(String table, EntityData.Type type) {
        try (ResultSet resultSet = executeQuery("SELECT * FROM " + table + ";")) {
            while (resultSet != null && resultSet.next()) {
                Location location = null;

                if (resultSet.getString("location") != null) {
                    location = LocationUtil.stringToLocation(resultSet.getString("location"));
                } else if (resultSet.getString("chunk") != null) {
                    location = LocationUtil.chunkStringToLocation(resultSet.getString("chunk"));
                }
                if (location != null) {
                    UUID uuidEntity = UUID.fromString(resultSet.getString("uuid_entity"));
                    UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));

                    getChunkData(location).addEntityData(new EntityData(location, uuidEntity, uuidGrave, type));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void addBlockData(BlockData blockData) {
        getChunkData(blockData.getLocation()).addBlockData(blockData);

        String uuidGrave = blockData.getGraveUUID() != null ? "'" + blockData.getGraveUUID() + "'" : "NULL";
        String location = "'" + LocationUtil.locationToString(blockData.getLocation()) + "'";
        String replaceMaterial = blockData.getReplaceMaterial() != null ? "'" + blockData.getReplaceMaterial() + "'" : "NULL";
        String replaceData = blockData.getReplaceData() != null ? "'" + blockData.getReplaceData() + "'" : "NULL";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                executeUpdate("INSERT INTO block (location, uuid_grave, replace_material, replace_data) " +
                        "VALUES (" + location + ", " + uuidGrave + ", " + replaceMaterial + ", " + replaceData + ");"));
    }

    public void removeBlockData(Location location) {
        getChunkData(location).removeBlockData(location);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                executeUpdate("DELETE FROM block WHERE location = '"
                        + LocationUtil.locationToString(location) + "';"));
    }

    public void addHologramData(HologramData hologramData) {
        getChunkData(hologramData.getLocation()).addEntityData(hologramData);

        String location = "'" + LocationUtil.locationToString(hologramData.getLocation()) + "'";
        String uuidEntity = "'" + hologramData.getUUIDEntity() + "'";
        String uuidGrave = "'" + hologramData.getUUIDGrave() + "'";
        int line = hologramData.getLine();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                executeUpdate("INSERT INTO hologram (uuid_entity, uuid_grave, line, location) VALUES ("
                        + uuidEntity + ", " + uuidGrave + ", " + line + ", " + location + ");"));
    }

    public void removeHologramData(List<EntityData> entityDataList) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                for (EntityData hologramData : entityDataList) {
                    getChunkData(hologramData.getLocation()).removeEntityData(hologramData);
                    statement.addBatch("DELETE FROM hologram WHERE uuid_entity = '"
                            + hologramData.getUUIDEntity() + "';");
                }
                executeBatch(statement);
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void addEntityData(EntityData entityData) {
        getChunkData(entityData.getLocation()).addEntityData(entityData);

        String table = entityDataTypeTable(entityData.getType());

        if (table != null) {
            String location = "'" + LocationUtil.locationToString(entityData.getLocation()) + "'";
            String uuidEntity = "'" + entityData.getUUIDEntity() + "'";
            String uuidGrave = "'" + entityData.getUUIDGrave() + "'";

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                    executeUpdate("INSERT INTO " + table + " (location, uuid_entity, uuid_grave) VALUES ("
                            + location + ", " + uuidEntity + ", " + uuidGrave + ");"));
        }
    }

    public void removeEntityData(EntityData entityData) {
        removeEntityData(Collections.singletonList(entityData));
    }

    public void removeEntityData(List<EntityData> entityDataList) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                for (EntityData entityData : entityDataList) {
                    getChunkData(entityData.getLocation()).removeEntityData(entityData);
                    String table = entityDataTypeTable(entityData.getType());
                    if (table != null) {
                        statement.addBatch("DELETE FROM " + table + " WHERE uuid_entity = '"
                                + entityData.getUUIDEntity() + "';");
                        plugin.debugMessage("Removing " + table + " for grave "
                                + entityData.getUUIDGrave(), 1);
                    }
                }
                executeBatch(statement);
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public String entityDataTypeTable(EntityData.Type type) {
        switch (type) {
            case ARMOR_STAND:
                return "armorstand";
            case ITEM_FRAME:
                return "itemframe";
            case HOLOGRAM:
                return "hologram";
            case FURNITURELIB:
                return "furniturelib";
            case FURNITUREENGINE:
                return "furnitureengine";
            case ITEMSADDER:
                return "itemsadder";
            case ORAXEN:
                return "oraxen";
            case PLAYERNPC:
                return "playernpc";
            default:
                return type.name().toLowerCase().replace("_", "");
        }
    }

    public void addGrave(Grave grave) {
        plugin.getCacheManager().getGraveMap().put(grave.getUUID(), grave);

        String uuid = grave.getUUID() != null ? "'" + grave.getUUID() + "'" : "NULL";
        String ownerType = grave.getOwnerType() != null ? "'" + grave.getOwnerType() + "'" : "NULL";
        String ownerName = grave.getOwnerName() != null ? "'" + grave.getOwnerName()
                .replace("'", "''") + "'" : "NULL";
        String ownerNameDisplay = grave.getOwnerNameDisplay() != null ? "'" + grave.getOwnerNameDisplay()
                .replace("'", "''") + "'" : "NULL";
        String ownerUUID = grave.getOwnerUUID() != null ? "'" + grave.getOwnerUUID() + "'" : "NULL";
        String ownerTexture = grave.getOwnerTexture() != null ? "'" + grave.getOwnerTexture()
                .replace("'", "''") + "'" : "NULL";
        String ownerTextureSignature = grave.getOwnerTextureSignature() != null ? "'" + grave.getOwnerTextureSignature()
                .replace("'", "''") + "'" : "NULL";
        String killerType = grave.getKillerType() != null ? "'" + grave.getKillerType() + "'" : "NULL";
        String killerName = grave.getKillerName() != null ? "'" + grave.getKillerName()
                .replace("'", "''") + "'" : "NULL";
        String killerNameDisplay = grave.getKillerNameDisplay() != null ? "'" + grave.getKillerNameDisplay()
                .replace("'", "''") + "'" : "NULL";
        String killerUUID = grave.getKillerUUID() != null ? "'" + grave.getKillerUUID() + "'" : "NULL";
        String locationDeath = grave.getLocationDeath() != null ? "'"
                + LocationUtil.locationToString(grave.getLocationDeath()) + "'" : "NULL";
        float yaw = grave.getYaw();
        float pitch = grave.getPitch();
        String inventory = "'" + InventoryUtil.inventoryToString(grave.getInventory()) + "'";
        String equipment = "'" + Base64Util.objectToBase64(grave.getEquipmentMap()) + "'";
        String permissions = grave.getPermissionList() != null && !grave.getPermissionList().isEmpty()
                ? "'" + StringUtils.join(grave.getPermissionList(), "|") + "'" : "NULL";
        int protection = grave.getProtection() ? 1 : 0;
        int experience = grave.getExperience();
        long timeAlive = grave.getTimeAlive();
        long timeProtection = grave.getTimeProtection();
        long timeCreation = grave.getTimeCreation();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                executeUpdate("INSERT INTO grave (uuid, owner_type, owner_name, owner_name_display, owner_uuid,"
                        + " owner_texture, owner_texture_signature, killer_type, killer_name, killer_name_display,"
                        + " killer_uuid, location_death, yaw, pitch, inventory, equipment, experience, protection, time_alive,"
                        + "time_protection, time_creation, permissions) VALUES (" + uuid + ", " + ownerType + ", "
                        + ownerName + ", " + ownerNameDisplay + ", " + ownerUUID + ", " + ownerTexture + ", "
                        + ownerTextureSignature + ", " + killerType + ", " + killerName + ", " + killerNameDisplay + ", "
                        + killerUUID + ", " + locationDeath + ", " + yaw + ", " + pitch + ", " + inventory + ", "
                        + equipment + ", " + experience + ", " + protection + ", " + timeAlive + ", "
                        + timeProtection + ", " + timeCreation + ", " + permissions + ");"));
    }

    public void removeGrave(Grave grave) {
        removeGrave(grave.getUUID());
    }

    public void removeGrave(UUID uuid) {
        plugin.getCacheManager().getGraveMap().remove(uuid);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("DELETE FROM grave WHERE uuid = '" + uuid + "';");
        });
    }

    public void updateGrave(Grave grave, String column, String string) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("UPDATE grave SET " + column + " = '" + string + "' WHERE uuid = '"
                    + grave.getUUID() + "';");
        });
    }

    public Grave resultSetToGrave(ResultSet resultSet) {
        try {
            Grave grave = new Grave(UUID.fromString(resultSet.getString("uuid")));

            grave.setOwnerType(resultSet.getString("owner_type") != null
                    ? EntityType.valueOf(resultSet.getString("owner_type")) : null);
            grave.setOwnerName(resultSet.getString("owner_name") != null
                    ? resultSet.getString("owner_name") : null);
            grave.setOwnerNameDisplay(resultSet.getString("owner_name_display") != null
                    ? resultSet.getString("owner_name_display") : null);
            grave.setOwnerUUID(resultSet.getString("owner_uuid") != null
                    ? UUID.fromString(resultSet.getString("owner_uuid")) : null);
            grave.setOwnerTexture(resultSet.getString("owner_texture") != null
                    ? resultSet.getString("owner_texture") : null);
            grave.setOwnerTextureSignature(resultSet.getString("owner_texture_signature") != null
                    ? resultSet.getString("owner_texture_signature") : null);
            grave.setKillerType(resultSet.getString("killer_type") != null
                    ? EntityType.valueOf(resultSet.getString("killer_type")) : null);
            grave.setKillerName(resultSet.getString("killer_name") != null
                    ? resultSet.getString("killer_name") : null);
            grave.setKillerNameDisplay(resultSet.getString("killer_name_display") != null
                    ? resultSet.getString("killer_name_display") : null);
            grave.setKillerUUID(resultSet.getString("killer_uuid") != null
                    ? UUID.fromString(resultSet.getString("killer_uuid")) : null);
            grave.setLocationDeath(resultSet.getString("location_death") != null
                    ? LocationUtil.stringToLocation(resultSet.getString("location_death")) : null);
            grave.setYaw(resultSet.getFloat("yaw"));
            grave.setPitch(resultSet.getFloat("pitch"));
            grave.setExperience(resultSet.getInt("experience"));
            grave.setProtection(resultSet.getInt("protection") == 1);
            grave.setTimeAlive(resultSet.getLong("time_alive"));
            grave.setTimeProtection(resultSet.getLong("time_protection"));
            grave.setTimeCreation(resultSet.getLong("time_creation"));
            grave.setPermissionList(resultSet.getString("permissions") != null
                    ? new ArrayList<>(Arrays.asList(resultSet.getString("permissions").split("\\|"))) : null);
            grave.setInventory(InventoryUtil.stringToInventory(grave, resultSet.getString("inventory"),
                    StringUtil.parseString(plugin.getConfig("gui.grave.title", grave.getOwnerType(),
                                    grave.getPermissionList())
                            .getString("gui.grave.title"), grave.getLocationDeath(), grave, plugin), plugin));

            if (resultSet.getString("equipment") != null) {
                Map<EquipmentSlot, ItemStack> equipmentMap = (Map<EquipmentSlot, ItemStack>) Base64Util
                        .base64ToObject(resultSet.getString("equipment"));
                grave.setEquipmentMap(equipmentMap != null ? equipmentMap : new HashMap<>());
            }

            return grave;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    private Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException exception) {
            exception.printStackTrace();
            plugin.getLogger().severe("Error obtaining database connection: " + exception.getMessage());
            return null;
        }
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void executeBatch(Statement statement) {
        try {
            statement.executeBatch();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void executeUpdate(String sql) {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            if (connection != null) {
                statement.executeUpdate(sql);
            }
        } catch (SQLException exception) {
            if (type == Type.SQLITE) return; // Will always fail. It doesn't cause any issues, so just don't log it.
            exception.printStackTrace();
        }
    }

    private ResultSet executeQuery(String sql) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            if (connection != null) {
                statement = connection.createStatement();
                return statement.executeQuery(sql);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        } finally {
            // Note: We do not close ResultSet here because it is used outside this method.
            closeStatement(statement);
            closeConnection(connection);
        }
        return null;
    }

    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    private boolean testMySQLConnection() {
        try (Connection testConnection = getConnection()) {
            return testConnection != null && !testConnection.isClosed();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            return false;
        }
    }

    private void migrateToMySQL() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        File sqliteFile = new File(dataFolder, "data.db");

        if (!sqliteFile.exists() || !dataFolder.exists()) {
            plugin.getLogger().warning("SQLite database file or folder does not exist in \"" + dataFolder.getPath() + "\". Skipping database migration.");
            return;
        }

        boolean migrationSuccess = true;

        try (Connection sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getPath())) {
            DatabaseMetaData metaData = sqliteConnection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                StringBuilder createTableQuery = new StringBuilder();
                List<String> columns = new ArrayList<>();

                try (Statement sqliteStatement = sqliteConnection.createStatement();
                     ResultSet tableData = sqliteStatement.executeQuery("SELECT * FROM " + tableName)) {

                    ResultSetMetaData tableMetaData = tableData.getMetaData();
                    createTableQuery.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
                    for (int i = 1; i <= tableMetaData.getColumnCount(); i++) {
                        String columnName = tableMetaData.getColumnName(i);
                        String sqliteType = tableMetaData.getColumnTypeName(i);
                        String mysqlType = mapSQLiteTypeToMySQL(sqliteType, columnName);
                        plugin.getLogger().info("Mapping column: " + columnName + " of type: " + sqliteType + " to MySQL type: " + mysqlType);

                        if (mysqlType != null) {
                            createTableQuery.append(columnName)
                                    .append(" ")
                                    .append(mysqlType)
                                    .append(i == tableMetaData.getColumnCount() ? ")" : ", ");
                            columns.add(columnName);
                        }
                    }

                    if (columns.isEmpty()) {
                        plugin.getLogger().warning("No valid columns found for table " + tableName + ". Skipping table creation.");
                        continue;
                    }

                    plugin.getLogger().info("Creating table with query: " + createTableQuery.toString());
                    executeUpdate(createTableQuery.toString());

                    // Modify columns if necessary
                    if ("grave".equals(tableName)) {
                        plugin.getLogger().info("Altering table " + tableName + " to ensure column sizes are correct.");
                        executeUpdate("ALTER TABLE grave MODIFY owner_texture TEXT");
                        executeUpdate("ALTER TABLE grave MODIFY owner_texture_signature TEXT");
                        executeUpdate("ALTER TABLE grave MODIFY time_creation BIGINT");
                        executeUpdate("ALTER TABLE grave MODIFY time_protection BIGINT");
                        executeUpdate("ALTER TABLE grave MODIFY time_alive BIGINT");
                    }

                    while (tableData.next()) {
                        StringBuilder insertQuery = new StringBuilder("INSERT INTO " + tableName + " (");
                        insertQuery.append(String.join(", ", columns)).append(") VALUES (");

                        for (int i = 1; i <= tableMetaData.getColumnCount(); i++) {
                            if (columns.contains(tableMetaData.getColumnName(i))) {
                                String data = tableData.getString(i);
                                String columnName = tableMetaData.getColumnName(i);
                                if (data != null) {
                                    data = data.replace("'", "''");
                                }
                                insertQuery.append(data != null ? "'" + data + "'" : "NULL").append(", ");
                            }
                        }
                        // Remove trailing comma and space, and close the parenthesis
                        insertQuery.setLength(insertQuery.length() - 2);
                        insertQuery.append(")");

                        plugin.getLogger().info("Inserting data with query: " + insertQuery.toString());
                        executeUpdate(insertQuery.toString());
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Error migrating table " + tableName + ": " + e.getMessage());
                    plugin.getLogger().severe("Failed query: " + createTableQuery);
                    migrationSuccess = false;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error migrating SQLite to MySQL: " + e.getMessage());
            migrationSuccess = false;
        }

        if (migrationSuccess) {
            File renamedFile = new File(dataFolder, "data.old.db");
            if (sqliteFile.renameTo(renamedFile)) {
                plugin.getLogger().info("SQLite database successfully renamed to data.old.db");
            } else {
                plugin.getLogger().severe("Failed to rename SQLite database to data.old.db");
            }
        }
    }

    private String mapSQLiteTypeToMySQL(String sqliteType, String columnName) {
        switch (sqliteType.toUpperCase()) {
            case "INT":
            case "INTEGER":
                if ("protection".equals(columnName))
                    return "INT(1)";
                if ("time_protection".equals(columnName) || "time_creation".equals(columnName) || "time_alive".equals(columnName))
                    return "BIGINT";
                return "INT(16)";
            case "VARCHAR":
                return "VARCHAR(255)";
            case "FLOAT":
                return "FLOAT(16)";
            case "TEXT":
                if ("owner_texture".equals(columnName) || "owner_texture_signature".equals(columnName) || "inventory".equals(columnName) || "equipment".equals(columnName) || "permissions".equals(columnName)) {
                    return "TEXT";
                }
                return "VARCHAR(255)";
            case "BLOB":
                return "BLOB";
            case "REAL":
                return "DOUBLE";
            case "NUMERIC":
                return "DECIMAL(10, 5)";
            default:
                plugin.getLogger().warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
                return null; // Ignore unhandled types
        }
    }

    private void keepConnectionAlive() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (isConnected()) {
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                    statement.executeQuery();
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            }
        }, 0L, 25 * 20L); // 25 seconds interval
    }
}