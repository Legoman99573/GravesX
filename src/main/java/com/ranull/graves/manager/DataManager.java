package com.ranull.graves.manager;

import com.ranull.graves.type.Graveyard;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.ranull.graves.Graves;
import com.ranull.graves.data.*;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages data storage and retrieval for the Graves plugin.
 */
public final class DataManager {
    private final Graves plugin;
    private Type type;
    private HikariDataSource dataSource;

    /**
     * Initializes the DataManager with the specified plugin instance and sets up the database connection.
     *
     * @param plugin the Graves plugin instance.
     */
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
            case H2:
            case POSTGRESQL:
            case MYSQL:
            case MARIADB:
                loadType(this.type);
                if (testDatabaseConnection()) {
                    migrate();
                    load();
                    keepConnectionAlive(); // If we don't enable this, connection will close or time out :/
                } else {
                    plugin.getLogger().severe("Failed to connect to " + this.type + " database. Disabling plugin...");
                    plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                }
                break;
            default:
                plugin.getLogger().severe("Database Type is invalid. Only valid options: SQLITE and MYSQL. Disabling plugin...");
                plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                return;
        }
    }

    /**
     * Enum representing the types of databases supported.
     */
    public enum Type {
        SQLITE,
        MYSQL,
        MARIADB,
        POSTGRESQL,
        H2,
        INVALID
    }

    /**
     * Loads data from the database asynchronously.
     */
    private void load() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                loadTables();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            loadGraveMap();
            loadBlockMap();
            loadGraveyardsMap();
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

            if (plugin.getIntegrationManager().hasCitizensNPC()) {
                loadEntityDataMap("citizensnpc", EntityData.Type.CITIZENSNPC);
                plugin.getIntegrationManager().getCitizensNPC().createCorpses();
            }
        });
    }

    /**
     * Loads database tables.
     *
     * @throws SQLException if an SQL error occurs.
     */
    private void loadTables() throws SQLException {
        setupGraveTable();
        setupBlockTable();
        setupHologramTable();
        setupGraveyardsTable();
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

        if (plugin.getIntegrationManager().hasCitizensNPC()) {
            setupEntityTable("citizensnpc");
        }
    }

    /**
     * Reloads the data manager with the current type.
     */
    public void reload() {
        reload(type);
    }

    /**
     * Reloads the data manager with the specified type.
     *
     * @param type the type of database.
     */
    public void reload(Type type) {
        loadType(type);
        if ((type == Type.MYSQL || type == Type.MARIADB) && !testDatabaseConnection()) {
            plugin.getLogger().severe("Failed to connect to MySQL database. Disabling plugin...");
            plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            return;
        }
        load();
    }

    /**
     * Loads the database type and sets up the data source.
     *
     * @param type the type of database.
     */
    public void loadType(Type type) {
        this.type = type;
        if (type == Type.POSTGRESQL) {
            String host = plugin.getConfig().getString("settings.storage.postgresql.host", "localhost");
            int port = plugin.getConfig().getInt("settings.storage.postgresql.port", 3306);
            String user = plugin.getConfig().getString("settings.storage.postgresql.username", "username");
            String password = plugin.getConfig().getString("settings.storage.postgresql.password", "password");
            String database = plugin.getConfig().getString("settings.storage.postgresql.database", "graves");
            long maxLifetime = plugin.getConfig().getLong("settings.storage.postgresql.maxLifetime", 1800000);
            int maxConnections = plugin.getConfig().getInt("settings.storage.postgresql.maxConnections", 20); // Increased pool size
            long connectionTimeout = plugin.getConfig().getLong("settings.storage.postgresql.connectionTimeout", 30000);
            boolean ssl = plugin.getConfig().getBoolean("settings.storage.postgresql.ssl", true);
            String allowPublicKeyRetrieval = plugin.getConfig().getString("settings.storage.postgresql.sslfactory", "com.ranull.graves.postgresql.ssl.NonValidatingFactory");
            String verifyServerCertificate = plugin.getConfig().getString("settings.storage.postgresql.sslmode", "disable");
            String sslrootcert = plugin.getConfig().getString("settings.storage.postgresql.sslrootcert", "/path/to/server.crt");
            String sslcert = plugin.getConfig().getString("settings.storage.postgresql.sslcert", "/path/to/client.crt");
            String sslkey = plugin.getConfig().getString("settings.storage.postgresql.sslkey", "/path/to/client.key");
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
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
            config.addDataSourceProperty("ssl", String.valueOf(ssl));
            if (ssl) {
                config.addDataSourceProperty("sslfactory", allowPublicKeyRetrieval);
                config.addDataSourceProperty("sslmode", verifyServerCertificate);
                config.addDataSourceProperty("sslrootcert", sslrootcert);
                config.addDataSourceProperty("sslcert", sslcert);
                config.addDataSourceProperty("sslkey", sslkey);
            }
            config.setDriverClassName("org.postgresql.ds.PGSimpleDataSource");
            config.setMaximumPoolSize(maxConnections);
            config.setMaxLifetime(maxLifetime);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(connectionTimeout);
            config.setPoolName("Graves PostgreSQL");
            config.setIdleTimeout(600000); // 10 minutes
            config.setConnectionTestQuery("SELECT 1");
            config.setLeakDetectionThreshold(15000); // Detect connection leaks
        } else if (type == Type.SQLITE) {
            migrateRootDataSubData();
            HikariConfig config = new HikariConfig();
            configureSQLite(config);
            dataSource = new HikariDataSource(config);
            checkAndUnlockDatabase(); // Check and unlock the database if needed
        } else if (type == Type.H2) {
            migrateRootDataSubData();
            HikariConfig config = new HikariConfig();
            configureH2(config);
            dataSource = new HikariDataSource(config);
            checkAndUnlockDatabase(); // Check and unlock the database if needed
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
            if (useSSL) {
                config.addDataSourceProperty("allowPublicKeyRetrieval", String.valueOf(allowPublicKeyRetrieval));
                config.addDataSourceProperty("verifyServerCertificate", String.valueOf(verifyServerCertificate));
            }
            config.setMaximumPoolSize(maxConnections);
            config.setMaxLifetime(maxLifetime);
            config.setMinimumIdle(2);
            if (type == Type.MARIADB) {
                config.setPoolName("Graves MariaDB");
            } else {
                config.setPoolName("Graves MySQL");
            }
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(600000); // 10 minutes
            config.setConnectionTestQuery("SELECT 1");
            config.setLeakDetectionThreshold(15000); // Detect connection leaks

            if (type == Type.MARIADB) {
                config.setDriverClassName("org.mariadb.jdbc.Driver");
            } else {
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }

            dataSource = new HikariDataSource(config);

            if (testDatabaseConnection()) {
                migrate();
            }
        }
    }

    /**
     * Configures the SQLite data source.
     *
     * @param config the HikariConfig to configure.
     */
    private void configureSQLite(HikariConfig config) {
        String journal_mode = plugin.getConfig().getString("settings.storage.sqlite.journal-mode", "WAL");
        String synchronous = plugin.getConfig().getString("settings.storage.sqlite.synchronous", "OFF");

        config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "data" + File.separator + "data.db");
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setMaximumPoolSize(50); // Might as well increase this.
        config.addDataSourceProperty("dataSource.journalMode", journal_mode); // DELETE | TRUNCATE | PERSIST | MEMORY | WAL | OFF
        config.addDataSourceProperty("dataSource.synchronous", synchronous); // 0 | OFF | 1 | NORMAL | 2 | FULL | 3 | EXTRA
        config.setConnectionInitSql("PRAGMA busy_timeout = 30000");
        config.setConnectionInitSql("PRAGMA journal_mode=" + journal_mode + "; PRAGMA synchronous=" + synchronous + ";");
        config.setPoolName("Graves SQLite");
        config.addDataSourceProperty("autoReconnect", "true");
        config.setDriverClassName("org.sqlite.JDBC");
    }

    /**
     * Configures the H2 data source.
     *
     * @param config the HikariConfig to configure.
     */
    private void configureH2(HikariConfig config) {
        String filePath = plugin.getDataFolder() + File.separator + "data" + File.separator + "graves.data";
        String username = plugin.getConfig().getString("settings.storage.h2.username", "sa");
        String password = plugin.getConfig().getString("settings.storage.h2.password", "");
        long maxLifetime = plugin.getConfig().getLong("settings.storage.h2.maxLifetime", 1800000);
        int maxConnections = plugin.getConfig().getInt("settings.storage.h2.maxConnections", 50); // Increased pool size
        long connectionTimeout = plugin.getConfig().getLong("settings.storage.h2.connectionTimeout", 30000);

        config.setJdbcUrl("jdbc:h2:file:./" + filePath + ";AUTO_SERVER=TRUE");
        config.setUsername(username);
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

        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(maxConnections);
        config.setMaxLifetime(maxLifetime);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(connectionTimeout);
        config.setPoolName("Graves H2");
        config.setIdleTimeout(600000); // 10 minutes
        config.setConnectionTestQuery("SELECT 1");
        config.setLeakDetectionThreshold(15000); // Detect connection leaks
    }

    /**
     * Migrates root data to a sub-data directory.
     */
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

    /**
     * Checks if chunk data exists for a specified location.
     *
     * @param location the location to check.
     * @return true if chunk data exists, false otherwise.
     */
    public boolean hasChunkData(Location location) {
        return plugin.getCacheManager().getChunkMap().containsKey(LocationUtil.chunkToString(location));
    }

    /**
     * Retrieves chunk data for a specified location.
     *
     * @param location the location to retrieve chunk data for.
     * @return the chunk data.
     */
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

    /**
     * Removes chunk data.
     *
     * @param chunkData the chunk data to remove.
     */
    public void removeChunkData(ChunkData chunkData) {
        plugin.getCacheManager().getChunkMap().remove(LocationUtil.chunkToString(chunkData.getLocation()));
    }

    /**
     * Retrieves a list of columns for a specified table.
     *
     * @param tableName the table name.
     * @return the list of columns.
     */
    public List<String> getColumnList(String tableName) {
        List<String> columnList = new ArrayList<>();
        String query = (type == Type.MYSQL || type == Type.MARIADB)
                ? "DESCRIBE " + tableName + ";"
                : "PRAGMA table_info(" + tableName + ");";

        try (Connection connection = getConnection(); // Ensure you get a connection
             Statement statement = connection != null ? connection.createStatement() : null;
             ResultSet resultSet = statement != null ? statement.executeQuery(query) : null) {

            if (resultSet != null) {
                while (resultSet.next()) {
                    String columnName = (type == Type.MYSQL || type == Type.MARIADB) ? resultSet.getString("Field") : resultSet.getString("name");
                    columnList.add(columnName);
                }
            }
        } catch (SQLException exception) {
            plugin.logStackTrace(exception);
        }

        return columnList;
    }

    /**
     * Checks if a table exists in the database.
     *
     * @param tableName the table name.
     * @return true if the table exists, false otherwise.
     */
    public boolean tableExists(String tableName) {
        ResultSet resultSet = null;
        try (Connection connection = getConnection();
             Statement statement = connection != null ? connection.createStatement() : null) {
            if (type == Type.MYSQL || type == Type.MARIADB) {
                if (statement != null) {
                    resultSet = statement.executeQuery("SHOW TABLES LIKE '" + tableName + "';");
                }
            } else {
                if (statement != null) {
                    resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "';");
                }
            }
            return resultSet != null && resultSet.next();
        } catch (NullPointerException | SQLException exception) {
            plugin.logStackTrace(exception);
            return false;
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException exception) {
                    plugin.logStackTrace(exception);
                }
            }
        }
    }

    /**
     * Adds a column to a table if it does not exist.
     *
     * @param tableName       the table name.
     * @param columnName      the column name.
     * @param columnDefinition the column definition.
     * @throws SQLException if an SQL error occurs.
     */
    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) throws SQLException {
        List<String> columnList = getColumnList(tableName);
        if (!columnList.contains(columnName)) {
            executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition + ";");
        }
    }

    /**
     * Sets up the grave table in the database.
     *
     * @throws SQLException if an SQL error occurs.
     */
    public void setupGraveTable() throws SQLException {
        String name = "grave";
        if (!tableExists(name)) {
            if (type == Type.H2) {
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
                        "yaw FLOAT,\n" +
                        "pitch FLOAT,\n" +
                        "inventory TEXT,\n" +
                        "equipment TEXT,\n" +
                        "experience INT,\n" +
                        "protection INT,\n" +
                        "time_alive BIGINT,\n" +
                        "time_protection BIGINT,\n" +
                        "time_creation BIGINT,\n" +
                        "permissions TEXT);");
            } else if (type == Type.POSTGRESQL) {
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
                        "yaw REAL,\n" +
                        "pitch REAL,\n" +
                        "inventory TEXT,\n" +
                        "equipment TEXT,\n" +
                        "experience INT,\n" +
                        "protection INT,\n" +
                        "time_alive BIGINT,\n" +
                        "time_protection BIGINT,\n" +
                        "time_creation BIGINT,\n" +
                        "permissions TEXT);");
            } else {
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

    /**
     * Sets up the block table in the database.
     *
     * @throws SQLException if an SQL error occurs.
     */
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

    /**
     * Sets up the graveyards table in the database.
     *
     * @throws SQLException if an SQL error occurs.
     */
    private void setupGraveyardsTable() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS graveyards (" +
                "name VARCHAR(255) NOT NULL," +
                "world VARCHAR(255) NOT NULL," +
                "type VARCHAR(255) NOT NULL," +
                "serializedLocations TEXT," +
                "PRIMARY KEY (name, world)" +
                ");";
        executeUpdate(createTableQuery);
    }

    /**
     * Sets up the hologram table in the database.
     *
     * @throws SQLException if an SQL error occurs.
     */
    public void setupHologramTable() throws SQLException {
        String name = "hologram";
        if (!tableExists(name)) {
            if (type == Type.H2) {
                executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                        "uuid_entity VARCHAR(255),\n" +
                        "uuid_grave VARCHAR(255),\n" +
                        "line INT,\n" +
                        "location VARCHAR(255));");
            } else {
                executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                        "uuid_entity VARCHAR(255),\n" +
                        "uuid_grave VARCHAR(255),\n" +
                        "line INT(16),\n" +
                        "location VARCHAR(255));");
            }
        }

        addColumnIfNotExists(name, "uuid_entity", "VARCHAR(255)");
        addColumnIfNotExists(name, "uuid_grave", "VARCHAR(255)");
        addColumnIfNotExists(name, "line", "INT(16)");
        addColumnIfNotExists(name, "location", "VARCHAR(255)");
    }

    /**
     * Sets up an entity table in the database.
     *
     * @param name the name of the table.
     * @throws SQLException if an SQL error occurs.
     */
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

    /**
     * Loads the grave map from the database.
     */
    private void loadGraveMap() {
        plugin.getCacheManager().getGraveMap().clear();

        String query = "SELECT * FROM grave;";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                Grave grave = resultSetToGrave(resultSet);
                if (grave != null) {
                    plugin.getCacheManager().getGraveMap().put(grave.getUUID(), grave);
                }
            }
        } catch (NullPointerException | SQLException exception) {
            plugin.logStackTrace(exception);
        }
    }

    /**
     * Loads graveyards from the database into the provided map.
     */
    public void loadGraveyardsMap() {
        String query = "SELECT * FROM graveyards";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String world = resultSet.getString("world");
                String type = resultSet.getString("type");
                String serializedLocations = resultSet.getString("serializedLocations");
                Graveyard graveyard = new Graveyard(name, plugin.getServer().getWorld(world), Graveyard.Type.valueOf(type.toUpperCase()));
                Map<Location, BlockFace> locations = Graveyard.deserializeLocations(serializedLocations);
                for (Map.Entry<Location, BlockFace> entry : locations.entrySet()) {
                    graveyard.addGraveLocation(entry.getKey(), entry.getValue());
                }
                plugin.getCacheManager().getGraveyardsMap().put(name, graveyard);
            }
        } catch (NullPointerException | SQLException e) {
            plugin.getLogger().severe("Failed to load graveyards: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates the graveyard location data in the database.
     *
     * @param graveyard The graveyard to update.
     */
    public void updateGraveyardLocationData(Graveyard graveyard) {
        String serializedLocation = Base64Util.objectToBase64(graveyard.getGraveLocationMap());
        String query = "UPDATE graveyards SET serializedLocations = ? WHERE name = ? AND world = ?";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection != null ? connection.prepareStatement(query) : null) {
                if (statement != null) {
                    statement.setString(1, serializedLocation);
                    statement.setString(2, graveyard.getName());
                    statement.setString(3, graveyard.getWorld().getName());
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update graveyard location data: " + e.getMessage());
            }
        });
    }

    /**
     * Loads the block map from the database.
     */
    private void loadBlockMap() {
        String query = "SELECT * FROM block;";

        try (Connection connection = getConnection();
             Statement statement = connection != null ? connection.createStatement() : null;
             ResultSet resultSet = statement != null ? statement.executeQuery(query) : null) {

            if (resultSet != null) {
                while (resultSet.next()) {
                    Location location = LocationUtil.stringToLocation(resultSet.getString("location"));
                    UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));
                    String replaceMaterial = resultSet.getString("replace_material");
                    String replaceData = resultSet.getString("replace_data");

                    getChunkData(location).addBlockData(new BlockData(location, uuidGrave, replaceMaterial, replaceData));
                }
            }
        } catch (SQLException exception) {
            plugin.logStackTrace(exception);
        }
    }

    /**
     * Loads an entity map from the database.
     *
     * @param table the table name.
     * @param type  the type of entity data.
     */
    private void loadEntityMap(String table, EntityData.Type type) {
        String query = "SELECT * FROM " + table + ";";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
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
        } catch (NullPointerException | SQLException exception) {
            plugin.logStackTrace(exception);
        }
    }

    /**
     * Loads the hologram map from the database.
     */
    private void loadHologramMap() {
        String query = "SELECT * FROM hologram;";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
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
        } catch (NullPointerException | SQLException exception) {
            plugin.logStackTrace(exception);
        }
    }

    /**
     * Loads entity data from the database.
     *
     * @param table the table name.
     * @param type  the type of entity data.
     */
    private void loadEntityDataMap(String table, EntityData.Type type) {
        String query = "SELECT * FROM " + table + ";";

        try (Connection connection = getConnection();
             Statement statement = connection != null ? connection.createStatement() : null;
             ResultSet resultSet = statement != null ? statement.executeQuery(query) : null) {

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
            plugin.logStackTrace(exception);
        }
    }

    /**
     * Adds block data to the database.
     *
     * @param blockData the block data to add.
     */
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

    /**
     * Removes block data from the database.
     *
     * @param location the location of the block data to remove.
     */
    public void removeBlockData(Location location) {
        getChunkData(location).removeBlockData(location);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                executeUpdate("DELETE FROM block WHERE location = '"
                        + LocationUtil.locationToString(location) + "';"));
    }

    /**
     * Adds hologram data to the database.
     *
     * @param hologramData the hologram data to add.
     */
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

    /**
     * Removes hologram data from the database.
     *
     * @param entityDataList the list of entity data to remove.
     */
    public void removeHologramData(List<EntityData> entityDataList) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 Statement statement = connection != null ? connection.createStatement() : null) {
                for (EntityData hologramData : entityDataList) {
                    getChunkData(hologramData.getLocation()).removeEntityData(hologramData);
                    if (statement != null) {
                        statement.addBatch("DELETE FROM hologram WHERE uuid_entity = '"
                                + hologramData.getUUIDEntity() + "';");
                    }
                }
                if (statement != null) {
                    executeBatch(statement);
                }
            } catch (SQLException exception) {
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Adds entity data to the database.
     *
     * @param entityData the entity data to add.
     */
    public void addEntityData(EntityData entityData) {
        getChunkData(entityData.getLocation()).addEntityData(entityData);

        String table = entityDataTypeTable(entityData.getType());

        String location = "'" + LocationUtil.locationToString(entityData.getLocation()) + "'";
        String uuidEntity = "'" + entityData.getUUIDEntity() + "'";
        String uuidGrave = "'" + entityData.getUUIDGrave() + "'";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                executeUpdate("INSERT INTO " + table + " (location, uuid_entity, uuid_grave) VALUES ("
                        + location + ", " + uuidEntity + ", " + uuidGrave + ");"));
    }

    /**
     * Removes entity data from the database.
     *
     * @param entityData the entity data to remove.
     */
    public void removeEntityData(EntityData entityData) {
        removeEntityData(Collections.singletonList(entityData));
    }

    /**
     * Removes a list of entity data from the database.
     *
     * @param entityDataList the list of entity data to remove.
     */
    public void removeEntityData(List<EntityData> entityDataList) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 Statement statement = connection != null ? connection.createStatement() : null) {
                for (EntityData entityData : entityDataList) {
                    getChunkData(entityData.getLocation()).removeEntityData(entityData);
                    String table = entityDataTypeTable(entityData.getType());
                    if (statement != null) {
                        statement.addBatch("DELETE FROM " + table + " WHERE uuid_entity = '"
                                + entityData.getUUIDEntity() + "';");
                        plugin.debugMessage("Removing " + table + " for grave "
                                + entityData.getUUIDGrave(), 1);
                    }
                }
                if (statement != null) {
                    executeBatch(statement);
                }
            } catch (SQLException exception) {
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Saves a graveyard to the database.
     *
     * @param graveyard The graveyard to save.
     */
    public void saveGraveyard(Graveyard graveyard, String serializedLocations) {
        plugin.getLogger().info("Saving serialized locations: " + serializedLocations);

        if (graveyardExists(graveyard.getName())) {
            updateGraveyard(graveyard, serializedLocations);
        } else {
            insertGraveyard(graveyard, serializedLocations);
        }
    }

    private boolean graveyardExists(String name) {
        String query = "SELECT COUNT(*) FROM graveyards WHERE name = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check existence of graveyard: " + e.getMessage());
            plugin.logStackTrace(e);
        }

        return false;
    }

    private void updateGraveyard(Graveyard graveyard, String serializedLocations) {
        String query = "UPDATE graveyards SET world = ?, type = ?, serializedLocations = ? WHERE name = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, graveyard.getWorld().getName());
            statement.setString(2, graveyard.getType().toString());
            statement.setString(3, serializedLocations);
            statement.setString(4, graveyard.getName());

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                plugin.getLogger().severe("No rows updated for graveyard: " + graveyard.getName());
            } else {
                plugin.getLogger().info("Successfully updated graveyard: " + graveyard.getName());
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update graveyard: " + e.getMessage());
            plugin.logStackTrace(e);
        }
    }

    /**
     * Inserts a graveyard to the database.
     *
     * @param graveyard The graveyard to save.
     */
    private void insertGraveyard(Graveyard graveyard, String serializedLocations) {
        String query = "INSERT INTO graveyards (name, world, type, serializedLocations) VALUES (?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, graveyard.getName());
            statement.setString(2, graveyard.getWorld().getName());
            statement.setString(3, graveyard.getType().toString());
            statement.setString(4, serializedLocations);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                plugin.getLogger().severe("Failed to insert graveyard: " + graveyard.getName());
            } else {
                plugin.getLogger().info("Successfully inserted graveyard: " + graveyard.getName());
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert graveyard: " + e.getMessage());
            plugin.logStackTrace(e);
        }
    }

    /**
     * Retrieves a graveyard by its name.
     *
     * @param graveyardName The name of the graveyard.
     * @return The Graveyard object if found, otherwise null.
     */
    public Graveyard getGraveyardByName(String graveyardName) {
        String query = "SELECT * FROM graveyards WHERE name = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection != null ? connection.prepareStatement(query) : null) {
            if (statement != null) {
                statement.setString(1, graveyardName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String name = resultSet.getString("name");
                        String world = resultSet.getString("world");
                        String type = resultSet.getString("type");
                        String serializedLocation = resultSet.getString("serializedLocation");
                        // Assume there's a method in your Graves plugin to get a World by its name
                        return new Graveyard(name, plugin.getServer().getWorld(world), Graveyard.Type.valueOf(type.toUpperCase()));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to retrieve graveyard: " + e.getMessage());
        }
        return null;
    }

    /**
     * Deletes a graveyard from the database.
     *
     * @param graveyard The graveyard to delete.
     */
    public void deleteGraveyard(Graveyard graveyard) {
        String query = "DELETE FROM graveyards WHERE name = ? AND world = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection != null ? connection.prepareStatement(query) : null) {
            if (statement != null) {
                statement.setString(1, graveyard.getName());
                statement.setString(2, graveyard.getWorld().getName());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete graveyard: " + e.getMessage());
        }
    }

    public boolean hasGraveAtLocation(Location location) {
        String query = "SELECT COUNT(*) FROM grave WHERE location_death = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, LocationUtil.locationToString(location));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check if grave exists at location: " + e.getMessage());
        }
        return false;
    }

    /**
     * Returns the table name for the specified entity data type.
     *
     * @param type the entity data type.
     * @return the table name.
     */
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
            case CITIZENSNPC:
                return "citizensnpc";
            default:
                return type.name().toLowerCase().replace("_", "");
        }
    }

    /**
     * Adds a grave to the database.
     *
     * @param grave the grave to add.
     */
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

        // Convert EquipmentSlot keys to String and filter out null values from the map before serialization
        Map<String, Object> equipmentMap = grave.getEquipmentMap().entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue));

        String equipment = "'" + Base64Util.objectToBase64(equipmentMap) + "'";
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

    /**
     * Removes a grave from the database.
     *
     * @param grave the grave to remove.
     */
    public void removeGrave(Grave grave) {
        removeGrave(grave.getUUID());
    }

    /**
     * Removes a grave from the database by UUID.
     *
     * @param uuid the UUID of the grave to remove.
     */
    public void removeGrave(UUID uuid) {
        plugin.getCacheManager().getGraveMap().remove(uuid);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("DELETE FROM grave WHERE uuid = '" + uuid + "';");
        });
    }

    /**
     * Updates a grave in the database.
     *
     * @param grave  the grave to update.
     * @param column the column to update.
     * @param string the new value for the column.
     */
    public void updateGrave(Grave grave, String column, String string) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("UPDATE grave SET " + column + " = '" + string + "' WHERE uuid = '"
                    + grave.getUUID() + "';");
        });
    }

    /**
     * Converts a ResultSet to a Grave object.
     *
     * @param resultSet the ResultSet to convert.
     * @return the Grave object.
     */
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
            plugin.logStackTrace(exception);
        }
        return null;
    }

    /**
     * Checks if the database connection is active.
     *
     * @return true if the connection is active, false otherwise.
     */
    private boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Retrieves a connection from the data source.
     *
     * @return the database connection.
     */
    private Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Error obtaining database connection: " + exception.getMessage());
            plugin.logStackTrace(exception);
            return null;
        }
    }

    /**
     * Closes the database connection.
     */
    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Executes a batch of SQL statements.
     *
     * @param statement the statement containing the batch.
     */
    private void executeBatch(Statement statement) {
        try {
            statement.executeBatch();
        } catch (SQLException exception) {
            plugin.logStackTrace(exception);
        }
    }

    /**
     * Executes an update SQL statement.
     *
     * @param sql the SQL statement.
     */
    private void executeUpdate(String sql) {
        Statement statement = null;
        Connection connection = null;
        try {
            connection = getConnection();
            if (connection != null) {
                statement = connection.createStatement();
                statement.executeUpdate(sql);
            }
        } catch (SQLException exception) {
            // Log the SQL statement and exception message
            plugin.getLogger().severe("Error executing SQL update: " + exception.getMessage());
            plugin.getLogger().severe("Failed SQL statement: " + sql);
            plugin.logStackTrace(exception);
        } finally {
            // Ensure statement and connection are closed properly
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException exception) {
                    plugin.logStackTrace(exception);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException exception) {
                    plugin.logStackTrace(exception);
                }
            }
        }
    }

    /**
     * Executes a query SQL statement.
     *
     * @param sql the SQL statement.
     * @return the ResultSet of the query.
     */
    private ResultSet executeQuery(String sql) throws SQLException {
        ResultSet resultSet = null;
        Statement statement = null;
        try (Connection connection = getConnection()) {
            if (connection != null) {
                statement = connection.createStatement();
                resultSet = statement.executeQuery(sql);
            }
        } catch (SQLException exception) {
            plugin.logStackTrace(exception);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException exception) {
                    plugin.logStackTrace(exception);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException exception) {
                    plugin.logStackTrace(exception);
                }
            }
        }
        return resultSet;
    }

    /**
     * Closes a database connection.
     *
     * @param connection the connection to close.
     */
    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.logStackTrace(e);
            }
        }
    }

    /**
     * Closes a statement.
     *
     * @param statement the statement to close.
     */
    private void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                plugin.logStackTrace(e);
            }
        }
    }

    /**
     * Closes a ResultSet.
     *
     * @param resultSet the ResultSet to close.
     */
    private void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException exception) {
                plugin.logStackTrace(exception);
            }
        }
    }

    /**
     * Tests the MySQL connection.
     *
     * @return true if the connection is successful, false otherwise.
     */
    private boolean testDatabaseConnection() {
        try (Connection testConnection = getConnection()) {
            return testConnection != null && !testConnection.isClosed();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Migrates data from SQLite to the target database (MySQL, MariaDB, PostgreSQL, or H2).
     */
    public void migrate() {
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
                        String targetType = mapSQLiteTypeToTargetDB(sqliteType, columnName);
                        plugin.getLogger().info("Mapping column: " + columnName + " of type: " + sqliteType + " to target DB type: " + targetType);

                        if (targetType != null) {
                            createTableQuery.append(columnName)
                                    .append(" ")
                                    .append(targetType)
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
                        adjustGraveTableForTargetDB();
                    }

                    while (tableData.next()) {
                        StringBuilder insertQuery = new StringBuilder("INSERT INTO " + tableName + " (");
                        insertQuery.append(String.join(", ", columns)).append(") VALUES (");

                        for (int i = 1; i <= tableMetaData.getColumnCount(); i++) {
                            if (columns.contains(tableMetaData.getColumnName(i))) {
                                String data = tableData.getString(i);
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
            plugin.getLogger().severe("Error migrating SQLite to target DB: " + e.getMessage());
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

    /**
     * Maps SQLite data types to target database data types (MySQL/MariaDB, PostgreSQL, or H2).
     *
     * @param sqliteType the SQLite data type.
     * @param columnName the column name.
     * @return the target database data type.
     */
    private String mapSQLiteTypeToTargetDB(String sqliteType, String columnName) {
        switch (this.type) {
            case MYSQL:
            case MARIADB:
                return mapSQLiteTypeToMySQL(sqliteType, columnName);
            case POSTGRESQL:
                return mapSQLiteTypeToPostgreSQL(sqliteType, columnName);
            case H2:
                return mapSQLiteTypeToH2(sqliteType, columnName);
            default:
                plugin.getLogger().warning("Unhandled database type: " + this.type);
                return null; // Ignore unhandled types
        }
    }

    /**
     * Maps SQLite data types to MySQL/MariaDB data types.
     *
     * @param sqliteType the SQLite data type.
     * @param columnName the column name.
     * @return the MySQL/MariaDB data type.
     */
    private String mapSQLiteTypeToMySQL(String sqliteType, String columnName) {
        switch (sqliteType.toUpperCase()) {
            case "INT":
            case "BIGINT":
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
                return "TEXT";
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

    /**
     * Maps SQLite data types to PostgreSQL data types.
     *
     * @param sqliteType the SQLite data type.
     * @param columnName the column name.
     * @return the PostgreSQL data type.
     */
    private String mapSQLiteTypeToPostgreSQL(String sqliteType, String columnName) {
        switch (sqliteType.toUpperCase()) {
            case "INT":
            case "BIGINT":
            case "INTEGER":
                if ("protection".equals(columnName))
                    return "BOOLEAN";
                if ("time_protection".equals(columnName) || "time_creation".equals(columnName) || "time_alive".equals(columnName))
                    return "BIGINT";
                return "INTEGER";
            case "VARCHAR":
                return "VARCHAR(255)";
            case "FLOAT":
                return "REAL";
            case "TEXT":
                return "TEXT";
            case "BLOB":
                return "BYTEA";
            case "REAL":
                return "DOUBLE PRECISION";
            case "NUMERIC":
                return "NUMERIC(10, 5)";
            default:
                plugin.getLogger().warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
                return null; // Ignore unhandled types
        }
    }

    /**
     * Maps SQLite data types to H2 data types.
     *
     * @param sqliteType the SQLite data type.
     * @param columnName the column name.
     * @return the H2 data type.
     */
    private String mapSQLiteTypeToH2(String sqliteType, String columnName) {
        switch (sqliteType.toUpperCase()) {
            case "INT":
            case "BIGINT":
            case "INTEGER":
                if ("protection".equals(columnName))
                    return "BOOLEAN";
                if ("time_protection".equals(columnName) || "time_creation".equals(columnName) || "time_alive".equals(columnName))
                    return "BIGINT";
                return "INTEGER";
            case "VARCHAR":
                return "VARCHAR(255)";
            case "FLOAT":
                return "FLOAT";
            case "TEXT":
                return "TEXT";
            case "BLOB":
                return "BLOB";
            case "REAL":
                return "DOUBLE";
            case "NUMERIC":
                return "NUMERIC(10, 5)";
            default:
                plugin.getLogger().warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
                return null; // Ignore unhandled types
        }
    }

    /**
     * Adjusts the grave table for the target database if necessary.
     */
    private void adjustGraveTableForTargetDB() throws SQLException {
        if (this.type == Type.MYSQL || this.type == Type.MARIADB) {
            plugin.getLogger().info("Altering table grave to ensure column sizes are correct.");
            executeUpdate("ALTER TABLE grave MODIFY owner_texture TEXT");
            executeUpdate("ALTER TABLE grave MODIFY owner_texture_signature TEXT");
            executeUpdate("ALTER TABLE grave MODIFY time_creation BIGINT");
            executeUpdate("ALTER TABLE grave MODIFY time_protection BIGINT");
            executeUpdate("ALTER TABLE grave MODIFY time_alive BIGINT");
        } else if (this.type == Type.POSTGRESQL) {
            plugin.getLogger().info("Altering table grave to ensure column sizes are correct.");
            executeUpdate("ALTER TABLE grave ALTER COLUMN owner_texture TYPE TEXT");
            executeUpdate("ALTER TABLE grave ALTER COLUMN owner_texture_signature TYPE TEXT");
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_creation TYPE BIGINT");
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_protection TYPE BIGINT");
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_alive TYPE BIGINT");
        } else if (this.type == Type.H2) {
            plugin.getLogger().info("Altering table grave to ensure column sizes are correct.");
            executeUpdate("ALTER TABLE grave ALTER COLUMN owner_texture TEXT");
            executeUpdate("ALTER TABLE grave ALTER COLUMN owner_texture_signature TEXT");
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_creation BIGINT");
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_protection BIGINT");
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_alive BIGINT");
        }
    }

    /**
     * Keeps the database connection alive by periodically executing a query.
     */
    private void keepConnectionAlive() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (isConnected()) {
                checkAndUnlockDatabase(); // Good to check
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection != null ? connection.prepareStatement("SELECT 1") : null) {
                    if (statement != null) {
                        statement.executeQuery();
                    }
                } catch (NullPointerException | SQLException exception) {
                    plugin.logStackTrace(exception);
                }
            }
        }, 0L, 25 * 20L); // 25 seconds interval
    }

    /**
     * Checks if the SQLite database is locked and attempts to unlock it by running COMMIT or ROLLBACK.
     */
    public void checkAndUnlockDatabase() {
        if (type != Type.SQLITE) return;
        String checkQuery = "SELECT 1";
        try (Connection connection = getConnection();
             Statement statement = connection != null ? connection.createStatement() : null) {
            if (statement != null) {
                statement.executeQuery(checkQuery);
            }
        } catch (NullPointerException | SQLException e) {
            if (e.getMessage().contains("database is locked")) {
                plugin.getLogger().severe("Database is locked. Attempting to unlock...");
                try (Connection connection = getConnection()) {
                    if (connection != null) {
                        connection.setAutoCommit(false);
                        connection.commit();
                        plugin.getLogger().info("Database unlocked successfully using COMMIT.");
                    }
                } catch (SQLException commitException) {
                    plugin.getLogger().severe("Failed to unlock database using COMMIT: " + commitException.getMessage());
                    try (Connection connection = getConnection()) {
                        if (connection != null) {
                            connection.rollback();
                            plugin.getLogger().info("Database unlocked successfully using ROLLBACK.");
                        }
                    } catch (SQLException rollbackException) {
                        plugin.getLogger().severe("Failed to unlock database using ROLLBACK: " + rollbackException.getMessage());
                    }
                }
            }
        }
    }
}