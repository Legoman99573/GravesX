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
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Manages data storage and retrieval for the Graves plugin.
 */
public final class DataManager {
    /**
     * The main plugin instance associated with Graves.
     * <p>
     * This {@link Graves} instance represents the core plugin that this Graves is part of. It provides access
     * to the plugin's functionality, configuration, and other services.
     * </p>
     */
    private final Graves plugin;

    /**
     * The type of the component or event.
     * <p>
     * This {@link Type} represents the specific type or category of this component or event.
     * </p>
     */
    private Type type;

    /**
     * The data source used for database connections.
     * <p>
     * This {@link HikariDataSource} provides the connection pool for interacting with the database.
     * </p>
     */
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
                plugin.getLogger().warning("Database Option SQLITE is set for removal in a future release. Use H2 Database option instead for better reliance.");
                loadType(Type.SQLITE);
                load();
                keepConnectionAlive(); // If we don't enable this, connection will close or time out :/
                plugin.getServer().getScheduler().runTaskTimer(plugin, this::deleteOldBackups, 0L, 1200L);
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
                    plugin.getServer().getScheduler().runTaskTimer(plugin, this::deleteOldBackups, 0L, 1200L);
                } else {
                    plugin.getLogger().severe("Failed to connect to " + this.type + " database. Disabling plugin...");
                    plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                }
                break;
            case MSSQL:
                loadType(Type.MSSQL);
                migrate();
                load();
                keepConnectionAlive();
                plugin.getServer().getScheduler().runTaskTimer(plugin, this::deleteOldBackups, 0L, 1200L);
            default:
                plugin.getLogger().severe("Database Type is invalid. Only valid options: SQLITE, H2, POSTGRESQL, MARIADB, and MYSQL. Disabling plugin...");
                plugin.getServer().getPluginManager().disablePlugin(this.plugin);
        }
    }

    public String getType() {
        switch (type) {
            case H2:
                return "H2";
            case MYSQL:
                return "MySQL";
            case SQLITE:
                return "SQLite";
            case MARIADB:
                return "MariaDB";
            case POSTGRESQL:
                return "PostgreSQL";
            case MSSQL:
                return "Microsoft SQL Server";
            case INVALID:
            default:
                return null;
        }
    }

    /**
     * Enum representing the types of databases supported.
     */
    public enum Type {

        /**
         * SQLite database system.
         * <p>
         * This type represents an SQLite database, a self-contained, serverless SQL database engine.
         * </p>
         */
        SQLITE,

        /**
         * MySQL database system.
         * <p>
         * This type represents a MySQL database, a widely-used open-source relational database management system.
         * </p>
         */
        MYSQL,

        /**
         * MariaDB database system.
         * <p>
         * This type represents a MariaDB database, a community-developed fork of MySQL.
         * </p>
         */
        MARIADB,

        /**
         * PostgreSQL database system.
         * <p>
         * This type represents a PostgreSQL database, an open-source relational database known for its advanced features and extensibility.
         * </p>
         */
        POSTGRESQL,

        /**
         * H2 database system.
         * <p>
         * This type represents an H2 database, a Java SQL database that is fast and lightweight, often used for development and testing.
         * </p>
         */
        H2,

        /**
         * Microsoft SQL Server (MS SQL) database system.
         * <p>
         * This type represents a Microsoft SQL Server database, a robust, scalable, and enterprise-grade
         * relational database system commonly used in production environments. MS SQL offers
         * comprehensive features for transaction management, high availability, security, and
         * performance optimization, making it suitable for large-scale applications.
         * </p>
         */
        MSSQL,

        /**
         * Invalid or unsupported database type.
         * <p>
         * This type represents an invalid or unsupported database system, used to indicate errors or unsupported configurations.
         * </p>
         */
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
        setupGraveBackupTable();
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
            config.addDataSourceProperty("dataSourceName", "Graves");
            config.addDataSourceProperty("ssl", String.valueOf(ssl));
            if (ssl) {
                config.addDataSourceProperty("sslfactory", allowPublicKeyRetrieval);
                config.addDataSourceProperty("sslmode", verifyServerCertificate);
                config.addDataSourceProperty("sslrootcert", sslrootcert);
                config.addDataSourceProperty("sslcert", sslcert);
                config.addDataSourceProperty("sslkey", sslkey);
            }
            config.setDriverClassName("com.ranull.graves.libraries.postgresql.Driver");
            config.setMaximumPoolSize(maxConnections);
            config.setMaxLifetime(maxLifetime);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(connectionTimeout);
            config.setPoolName("Graves PostgreSQL");
            config.setIdleTimeout(600000); // 10 minutes
            config.setConnectionTestQuery("SELECT 1");
            config.setLeakDetectionThreshold(15000); // Detect connection leaks
            dataSource = new HikariDataSource(config);
            checkAndUnlockDatabase(); // Check and unlock the database if needed
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
        } else if (this.type == Type.MSSQL) {
            // MS SQL configuration
            String host = plugin.getConfig().getString("settings.storage.mssql.host", "localhost");
            int port = plugin.getConfig().getInt("settings.storage.mssql.port", 1433);
            String user = plugin.getConfig().getString("settings.storage.mssql.username", "username");
            String password = plugin.getConfig().getString("settings.storage.mssql.password", "password");
            String database = plugin.getConfig().getString("settings.storage.mssql.database", "graves");
            long maxLifetime = plugin.getConfig().getLong("settings.storage.mssql.maxLifetime", 1800000);
            int maxConnections = plugin.getConfig().getInt("settings.storage.mssql.maxConnections", 20);
            long connectionTimeout = plugin.getConfig().getLong("settings.storage.mssql.connectionTimeout", 30000);
            boolean encrypt = plugin.getConfig().getBoolean("settings.storage.mssql.encrypt", true);
            boolean trustServerCertificate = plugin.getConfig().getBoolean("settings.storage.mssql.trustServerCertificate", false);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + database);
            config.setUsername(user);
            config.setPassword(password);
            config.addDataSourceProperty("encrypt", String.valueOf(encrypt));
            config.addDataSourceProperty("trustServerCertificate", String.valueOf(trustServerCertificate));
            config.setMaximumPoolSize(maxConnections);
            config.setMaxLifetime(maxLifetime);
            config.setMinimumIdle(2);
            config.setPoolName("Graves MSSQL");
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(600000);
            config.setConnectionTestQuery("SELECT 1");
            config.setLeakDetectionThreshold(15000);

            // Set MSSQL driver class
            config.setDriverClassName("com.ranull.graves.libraries.microsoft.sqlserver.jdbc.SQLServerDriver");

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
                config.setDriverClassName("com.ranull.graves.libraries.mariadb.jdbc.Driver");
            } else {
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }

            dataSource = new HikariDataSource(config);
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

        config.setDriverClassName("com.ranull.graves.libraries.h2.Driver");
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
        String query = null;

        // Determine the query based on the database type
        switch (type) {
            case MYSQL:
            case MARIADB:
                query = "DESCRIBE " + tableName + ";";
                break;
            case SQLITE:
                query = "PRAGMA table_info(" + tableName + ");";
                break;
            case POSTGRESQL:
                query = "SELECT column_name FROM information_schema.columns WHERE table_name = '" + tableName + "';";
                break;
            case MSSQL:
                query = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + tableName + "' AND TABLE_SCHEMA = 'dbo';";
                break;
            case H2:
                query = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + tableName + "';";
                break;
            default:
                plugin.getLogger().severe("Unsupported database type: " + type);
                return columnList;
        }

        try (Connection connection = getConnection();
             Statement statement = connection != null ? connection.createStatement() : null;
             ResultSet resultSet = statement != null ? statement.executeQuery(query) : null) {

            if (resultSet != null) {
                while (resultSet.next()) {
                    String columnName;
                    if (type == Type.MYSQL || type == Type.MARIADB) {
                        columnName = resultSet.getString("Field");
                    } else if (type == Type.SQLITE) {
                        columnName = resultSet.getString("name");
                    } else {
                        columnName = resultSet.getString("COLUMN_NAME");
                    }
                    columnList.add(columnName);
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Error occurred while getting Column List: " + exception.getMessage());
            plugin.getLogger().severe("Query: " + query);
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
        String query = null;

        // Determine the query based on the database type
        switch (type) {
            case MYSQL:
            case MARIADB:
                query = "SHOW TABLES LIKE '" + tableName + "';";
                break;
            case SQLITE:
                query = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "';";
                break;
            case POSTGRESQL:
                query = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = '" + tableName + "');";
                break;
            case H2:
                query = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + tableName + "';";
                break;
            case MSSQL:
                query = "IF OBJECT_ID('" + tableName + "', 'U') IS NOT NULL SELECT 1 AS TableExists ELSE SELECT 0 AS TableExists;";
                break;
            default:
                plugin.getLogger().severe("Unsupported database type: " + type);
                return false;
        }

        try (Connection connection = getConnection();
             Statement statement = connection != null ? connection.createStatement() : null) {
            if (statement != null) {
                resultSet = statement.executeQuery(query);
            }
            if (resultSet != null && resultSet.next()) {
                // For PostgreSQL and H2, check if the result indicates the table exists
                if (type == Type.POSTGRESQL) {
                    return resultSet.getBoolean(1);
                } else if (type == Type.H2) {
                    return resultSet.getInt(1) > 0;
                } else if (type == Type.MSSQL) {
                    return resultSet.getInt("TableExists") == 1; // Check the result for MS SQL
                } else {
                    return true; // For MySQL, MariaDB, and SQLite, table exists if the result is returned
                }
            }
        } catch (NullPointerException | SQLException exception) {
            plugin.getLogger().severe("Error occurred while checking if table exists: " + exception.getMessage());
            plugin.getLogger().severe("Query: " + query);
            plugin.logStackTrace(exception);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException exception) {
                    plugin.getLogger().severe("Error occurred while closing resultSet: " + exception.getMessage());
                    plugin.getLogger().severe("Result Set: " + resultSet);
                    plugin.logStackTrace(exception);
                }
            }
        }

        return false;
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
            String query;

            switch (type) {
                case MYSQL:
                case MARIADB:
                case SQLITE:
                    query = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition + ";";
                    break;
                case POSTGRESQL:
                    query = "DO $$ BEGIN " +
                            "IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = '" + tableName + "' AND column_name = '" + columnName + "') THEN " +
                            "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition + "; " +
                            "END IF; " +
                            "END $$;";
                    break;
                case H2:
                    query = "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " " + columnDefinition + ";";
                    break;
                case MSSQL:
                    query = "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + tableName + "' AND COLUMN_NAME = '" + columnName + "') " +
                            "BEGIN " +
                            "ALTER TABLE " + tableName + " ADD " + columnName + " " + columnDefinition + ";" +
                            "END;";
                    break;
                default:
                    plugin.getLogger().severe("Unsupported database type: " + type);
                    return;
            }

            executeUpdate(query, new Object[0]);
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
            if (type == Type.H2 || type == Type.POSTGRESQL) {
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
                        "is_abandoned INT,\n" +
                        "time_alive BIGINT,\n" +
                        "time_protection BIGINT,\n" +
                        "time_creation BIGINT,\n" +
                        "permissions TEXT);", new Object[0]);
            } else if (type == Type.MSSQL) {
                // MS SQL table creation logic
                executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                        "uuid NVARCHAR(255) UNIQUE,\n" +
                        "owner_type NVARCHAR(255),\n" +
                        "owner_name NVARCHAR(255),\n" +
                        "owner_name_display NVARCHAR(255),\n" +
                        "owner_uuid NVARCHAR(255),\n" +
                        "owner_texture NVARCHAR(MAX),\n" +
                        "owner_texture_signature NVARCHAR(MAX),\n" +
                        "killer_type NVARCHAR(255),\n" +
                        "killer_name NVARCHAR(255),\n" +
                        "killer_name_display NVARCHAR(255),\n" +
                        "killer_uuid NVARCHAR(255),\n" +
                        "location_death NVARCHAR(255),\n" +
                        "yaw FLOAT,\n" +
                        "pitch FLOAT,\n" +
                        "inventory NVARCHAR(MAX),\n" +
                        "equipment NVARCHAR(MAX),\n" +
                        "experience INT,\n" +
                        "protection BIT,\n" +
                        "is_abandoned BIT,\n" +
                        "time_alive BIGINT,\n" +
                        "time_protection BIGINT,\n" +
                        "time_creation BIGINT,\n" +
                        "permissions NVARCHAR(MAX));", new Object[0]);
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
                        "is_abandoned INT(1),\n" +
                        "time_alive BIGINT,\n" +
                        "time_protection BIGINT,\n" +
                        "time_creation BIGINT,\n" +
                        "permissions TEXT);", new Object[0]);
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
        if (type == Type.POSTGRESQL || type == Type.H2) {
            addColumnIfNotExists(name, "yaw", "REAL");
            addColumnIfNotExists(name, "pitch", "REAL");
        } else if (type == Type.MSSQL) {
            addColumnIfNotExists(name, "yaw", "FLOAT");
            addColumnIfNotExists(name, "pitch", "FLOAT");
        } else {
            addColumnIfNotExists(name, "yaw", "FLOAT(16)");
            addColumnIfNotExists(name, "pitch", "FLOAT(16)");
        }
        addColumnIfNotExists(name, "inventory", "TEXT");
        addColumnIfNotExists(name, "equipment", "TEXT");
        if (type == Type.POSTGRESQL || type == Type.H2) {
            addColumnIfNotExists(name, "experience", "INT");
            addColumnIfNotExists(name, "protection", "INT");
            addColumnIfNotExists(name, "is_abandoned", "INT");
        } else {
            addColumnIfNotExists(name, "experience", "INT(16)");
            addColumnIfNotExists(name, "protection", "INT(1)");
            addColumnIfNotExists(name, "is_abandoned", "INT(1)");
        }
        addColumnIfNotExists(name, "time_alive", "BIGINT");
        addColumnIfNotExists(name, "time_protection", "BIGINT");
        addColumnIfNotExists(name, "time_creation", "BIGINT");
        addColumnIfNotExists(name, "permissions", "TEXT");
    }

    /**
     * Sets up the grave table in the database.
     *
     * @throws SQLException if an SQL error occurs.
     */
    public void setupGraveBackupTable() throws SQLException {
        String graveBackupTableName = "grave_backup";

        // Create grave table if it doesn't exist
        if (!tableExists(graveBackupTableName)) {
            if (type == Type.H2 || type == Type.POSTGRESQL) {
                executeUpdate("CREATE TABLE IF NOT EXISTS " + graveBackupTableName + " (" +
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
                        "is_abandoned INT,\n" +
                        "time_alive BIGINT,\n" +
                        "time_protection BIGINT,\n" +
                        "time_creation BIGINT,\n" +
                        "permissions TEXT);", new Object[0]);
            } else if (type == Type.MSSQL) {
                executeUpdate("CREATE TABLE " + graveBackupTableName + " (" +
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
                        "yaw FLOAT,\n" + // Using FLOAT for MS SQL
                        "pitch FLOAT,\n" + // Using FLOAT for MS SQL
                        "inventory TEXT,\n" +
                        "equipment TEXT,\n" +
                        "experience INT,\n" +
                        "protection INT,\n" +
                        "is_abandoned INT,\n" +
                        "time_alive BIGINT,\n" +
                        "time_protection BIGINT,\n" +
                        "time_creation BIGINT,\n" +
                        "permissions TEXT);", new Object[0]);
            } else {
                executeUpdate("CREATE TABLE IF NOT EXISTS " + graveBackupTableName + " (" +
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
                        "is_abandoned INT(1),\n" +
                        "time_alive BIGINT,\n" +
                        "time_protection BIGINT,\n" +
                        "time_creation BIGINT,\n" +
                        "permissions TEXT);", new Object[0]);
            }
        }

        addColumnIfNotExists(graveBackupTableName, "uuid", "VARCHAR(255) UNIQUE");
        addColumnIfNotExists(graveBackupTableName, "owner_type", "VARCHAR(255)");
        addColumnIfNotExists(graveBackupTableName, "owner_name", "VARCHAR(255)");
        addColumnIfNotExists(graveBackupTableName, "owner_name_display", "VARCHAR(255)");
        addColumnIfNotExists(graveBackupTableName, "owner_uuid", "VARCHAR(255)");
        addColumnIfNotExists(graveBackupTableName, "owner_texture", "TEXT");
        addColumnIfNotExists(graveBackupTableName, "owner_texture_signature", "TEXT");
        addColumnIfNotExists(graveBackupTableName, "killer_type", "VARCHAR(255)");
        addColumnIfNotExists(graveBackupTableName, "killer_name", "VARCHAR(255)");
        addColumnIfNotExists(graveBackupTableName, "killer_name_display", "VARCHAR(255)");
        addColumnIfNotExists(graveBackupTableName, "killer_uuid", "VARCHAR(255)");
        addColumnIfNotExists(graveBackupTableName, "location_death", "VARCHAR(255)");
        if (type == Type.POSTGRESQL || type == Type.H2) {
            addColumnIfNotExists(graveBackupTableName, "yaw", "REAL");
            addColumnIfNotExists(graveBackupTableName, "pitch", "REAL");
        } else if (type == Type.MSSQL) {
            addColumnIfNotExists(graveBackupTableName, "yaw", "FLOAT");
            addColumnIfNotExists(graveBackupTableName, "pitch", "FLOAT");
        } else {
            addColumnIfNotExists(graveBackupTableName, "yaw", "FLOAT(16)");
            addColumnIfNotExists(graveBackupTableName, "pitch", "FLOAT(16)");
        }
        addColumnIfNotExists(graveBackupTableName, "inventory", "TEXT");
        addColumnIfNotExists(graveBackupTableName, "equipment", "TEXT");
        if (type == Type.POSTGRESQL || type == Type.H2) {
            addColumnIfNotExists(graveBackupTableName, "experience", "INT");
            addColumnIfNotExists(graveBackupTableName, "protection", "INT");
            addColumnIfNotExists(graveBackupTableName, "is_abandoned", "INT");
        } else {
            addColumnIfNotExists(graveBackupTableName, "experience", "INT(16)");
            addColumnIfNotExists(graveBackupTableName, "protection", "INT(1)");
            addColumnIfNotExists(graveBackupTableName, "is_abandoned", "INT(1)");
        }
        addColumnIfNotExists(graveBackupTableName, "time_alive", "BIGINT");
        addColumnIfNotExists(graveBackupTableName, "time_protection", "BIGINT");
        addColumnIfNotExists(graveBackupTableName, "time_creation", "BIGINT");
        addColumnIfNotExists(graveBackupTableName, "permissions", "TEXT");
    }

    /**
     * Sets up the block table in the database.
     *
     * @throws SQLException if an SQL error occurs.
     */
    public void setupBlockTable() throws SQLException {
        String name = "block";

        // Check if the table exists and create it if it does not
        if (!tableExists(name)) {
            String createTableQuery = "CREATE TABLE " + name + " (" +
                    "location VARCHAR(255),\n" +
                    "uuid_grave VARCHAR(255),\n" +
                    "replace_material VARCHAR(255),\n" +
                    "replace_data TEXT);";
            executeUpdate(createTableQuery, new Object[0]);
        }

        // Ensure all columns exist
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
        executeUpdate(createTableQuery, new Object[0]);
    }

    /**
     * Sets up the hologram table in the database.
     *
     * @throws SQLException if an SQL error occurs.
     */
    public void setupHologramTable() throws SQLException {
        String name = "hologram";

        // Check if the table exists and create it if it does not
        if (!tableExists(name)) {
            String createTableQuery;

            switch (type) {
                case MYSQL:
                case MARIADB:
                    createTableQuery = "CREATE TABLE IF NOT EXISTS " + name + " (" +
                            "uuid_entity VARCHAR(255),\n" +
                            "uuid_grave VARCHAR(255),\n" +
                            "line INT(16),\n" +
                            "location VARCHAR(255));";
                    break;
                case SQLITE:
                    createTableQuery = "CREATE TABLE IF NOT EXISTS " + name + " (" +
                            "uuid_entity VARCHAR(255),\n" +
                            "uuid_grave VARCHAR(255),\n" +
                            "line INTEGER,\n" +  // SQLite uses INTEGER
                            "location VARCHAR(255));";
                    break;
                case POSTGRESQL:
                case H2:
                    createTableQuery = "CREATE TABLE IF NOT EXISTS " + name + " (" +
                            "uuid_entity VARCHAR(255),\n" +
                            "uuid_grave VARCHAR(255),\n" +
                            "line INTEGER,\n" +  // PostgreSQL and H2 use INTEGER
                            "location VARCHAR(255));";
                    break;
                default:
                    plugin.getLogger().severe("Unsupported database type: " + type);
                    return;
            }

            // Execute the create table query
            try {
                executeUpdate(createTableQuery);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to create hologram table: " + e.getMessage());
                throw e; // rethrowing the exception
            }
        }

        // Ensure all columns exist with appropriate types
        addColumnIfNotExists(name, "uuid_entity", "VARCHAR(255)");
        addColumnIfNotExists(name, "uuid_grave", "VARCHAR(255)");
        addColumnIfNotExists(name, "line", "INTEGER"); // Use INTEGER for consistency
        addColumnIfNotExists(name, "location", "VARCHAR(255)");
    }

    /**
     * Sets up an entity table in the database.
     *
     * @param name the name of the table.
     * @throws SQLException if an SQL error occurs.
     */
    private void setupEntityTable(String name) throws SQLException {
        // Create table if it does not exist
        String createTableQuery;

        switch (type) {
            case MYSQL:
            case MARIADB:
                createTableQuery = "CREATE TABLE IF NOT EXISTS " + name + " (" +
                        "location VARCHAR(255), " +
                        "uuid_entity VARCHAR(255), " +
                        "uuid_grave VARCHAR(255));";
                break;
            case SQLITE:
                createTableQuery = "CREATE TABLE IF NOT EXISTS " + name + " (" +
                        "location TEXT, " +  // Use TEXT for SQLite
                        "uuid_entity TEXT, " +
                        "uuid_grave TEXT);";
                break;
            case POSTGRESQL:
            case H2:
                createTableQuery = "CREATE TABLE IF NOT EXISTS " + name + " (" +
                        "location VARCHAR(255), " +
                        "uuid_entity VARCHAR(255), " +
                        "uuid_grave VARCHAR(255));";
                break;
            default:
                plugin.getLogger().severe("Unsupported database type: " + type);
                return;
        }

        // Execute the create table query
        try {
            executeUpdate(createTableQuery, new Object[0]);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create entity table: " + e.getMessage());
            throw e; // rethrowing the exception
        }

        // Ensure all columns exist with appropriate types
        addColumnIfNotExists(name, "location", "VARCHAR(255)");
        addColumnIfNotExists(name, "uuid_entity", "VARCHAR(255)");
        addColumnIfNotExists(name, "uuid_grave", "VARCHAR(255)");
    }

    /**
     * Loads the grave map from the database.
     */
    public void loadGraveMap() {
        plugin.getCacheManager().getGraveMap().clear();
        plugin.getLogger().info("Loading grave maps...");
        String query = "SELECT * FROM grave;";
        int graveCount = 0;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                Grave grave = resultSetToGrave(resultSet);
                if (grave != null) {
                    plugin.getCacheManager().getGraveMap().put(grave.getUUID(), grave);
                    graveCount++;
                } else {
                    plugin.getLogger().severe("Failed to load grave from result set at row " + resultSet.getRow());
                }
            }
            if (graveCount == 0) {
                plugin.getLogger().info("Found 0 grave maps to load into cache.");
            } else {
                plugin.getLogger().info("Loaded " + graveCount + " grave maps into cache.");
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Error occurred while loading Grave Map: " + exception.getMessage());
            plugin.logStackTrace(exception);
        } catch (NullPointerException exception) {
            plugin.getLogger().severe("A null pointer exception occurred while loading Grave Map: " + exception.getMessage());
            plugin.logStackTrace(exception);
        }
    }

    /**
     * Loads graveyards from the database into the provided map.
     */
    public void loadGraveyardsMap() {
        String query = "SELECT * FROM graveyards";  // Select only required columns
        plugin.getLogger().info("Loading graveyards from the database...");

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String world = resultSet.getString("world");
                String type = resultSet.getString("type");
                String serializedLocations = resultSet.getString("serializedLocations");

                // Ensure the world is not null before attempting to use it
                World serverWorld = plugin.getServer().getWorld(world);
                if (serverWorld == null) {
                    plugin.getLogger().warning("World not found for graveyard '" + name + "': " + world);
                    continue;
                }

                plugin.getLogger().info("Loading graveyard: " + name);

                Graveyard.Type graveyardType;
                try {
                    graveyardType = Graveyard.Type.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown graveyard type for '" + name + "': " + type);
                    continue;
                }

                // Create the graveyard object
                Graveyard graveyard = new Graveyard(name, serverWorld, graveyardType);

                // Deserialize grave locations
                Map<Location, BlockFace> locations = Graveyard.deserializeLocations(serializedLocations);

                Location spawnLocation = null;

                // Loop through the grave locations
                for (Map.Entry<Location, BlockFace> entry : locations.entrySet()) {
                    Location graveLocation = entry.getKey();
                    BlockFace graveFacing = entry.getValue();

                    // Set the first grave location as the spawn location (or apply custom logic)
                    if (spawnLocation == null) {
                        spawnLocation = graveLocation;
                    }

                    // Add the grave location to the graveyard
                    graveyard.addGraveLocation(graveLocation, graveFacing);
                }

                // Set the spawn location if available
                if (spawnLocation != null) {
                    graveyard.setSpawnLocation(spawnLocation);
                } else {
                    plugin.getLogger().warning("No valid spawn location found for graveyard '" + name + "'.");
                }

                // Add to the cache
                plugin.getCacheManager().getGraveyardsMap().put(name, graveyard);
                plugin.getLogger().info("Graveyard '" + name + "' loaded with spawn location.");
            }

            plugin.getLogger().info("All graveyards loaded.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load graveyards: " + e.getMessage());
            plugin.logStackTrace(e);
        }
    }

    /**
     * Updates the graveyard location data in the database.
     *
     * @param graveyard The graveyard to update.
     */
    public void updateGraveyardLocationData(Graveyard graveyard) {
        // Serialize graveyard locations as JSON
        String serializedLocations = Graveyard.serializeLocations(graveyard.getGraveLocationMap());
        String query = "UPDATE graveyards SET serializedLocations = ? WHERE name = ? AND world = ?";

        // Prepare parameters for the query
        Object[] parameters = {
                serializedLocations,
                graveyard.getName(),
                graveyard.getWorld().getName()
        };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update graveyard location data: " + e.getMessage());
                plugin.logStackTrace(e);
            }
        });
    }

    /**
     * Loads the block map from the database.
     */
    public void loadBlockMap() {
        String query = "SELECT * FROM block;";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getLogger().info("Loading Block Map cache...");
            int blockCount = 0;

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection != null ? connection.prepareStatement(query) : null;
                 ResultSet resultSet = statement != null ? statement.executeQuery() : null) {

                if (statement == null || resultSet == null) {
                    plugin.getLogger().severe("Failed to create statement or result set.");
                    return;
                }

                while (resultSet.next()) {
                    // Retrieve and convert data
                    Location location = LocationUtil.stringToLocation(resultSet.getString("location"));
                    UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));
                    String replaceMaterial = resultSet.getString("replace_material");
                    String replaceData = resultSet.getString("replace_data");

                    // Ensure required fields are valid
                    if (replaceMaterial != null && replaceData != null) {
                        getChunkData(location).addBlockData(new BlockData(location, uuidGrave, replaceMaterial, replaceData));
                        blockCount++;
                    } else {
                        plugin.getLogger().warning("Data is missing or invalid in result set for location: " + location);
                    }
                }

                if (blockCount == 0) {
                    plugin.getLogger().info("Loaded 0 Blocks into Block Map Cache.");
                } else {
                    plugin.getLogger().info("Loaded " + blockCount + " Blocks into the Block Map Cache.");
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Error occurred while loading Block Map: " + exception.getMessage());
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Loads an entity map from the database.
     *
     * @param table the table name.
     * @param type  the type of entity data.
     */
    private void loadEntityMap(String table, EntityData.Type type) {
        String query = "SELECT * FROM " + table + ";";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getLogger().info("Loading Entity Map Cache for " + table + "...");
            int entityCount = 0;

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    Location location = null;

                    // Handle possible null values for location and chunk
                    String locationString = resultSet.getString("location");

                    if (locationString != null) {
                        location = LocationUtil.stringToLocation(locationString);
                    }

                    if (location != null) {
                        // Ensure UUIDs are not null and valid
                        String uuidEntityString = resultSet.getString("uuid_entity");
                        String uuidGraveString = resultSet.getString("uuid_grave");

                        if (uuidEntityString != null && uuidGraveString != null) {
                            UUID uuidEntity = UUID.fromString(uuidEntityString);
                            UUID uuidGrave = UUID.fromString(uuidGraveString);

                            getChunkData(location).addEntityData(new EntityData(location, uuidEntity, uuidGrave, type));
                            entityCount++;
                        } else {
                            plugin.getLogger().warning("Missing UUIDs in result set for location: " + location);
                        }
                    } else {
                        plugin.getLogger().warning("Invalid location data in result set.");
                    }
                }

                if (entityCount == 0) {
                    plugin.getLogger().info("Loaded 0 entities into Entity Map Cache for " + table + ".");
                } else {
                    plugin.getLogger().info("Loaded " + entityCount + " entities into Entity Map Cache for " + table + ".");
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Error occurred while loading Entity Map: " + exception.getMessage());
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Loads the hologram map from the database.
     */
    public void loadHologramMap() {
        String query = "SELECT * FROM hologram;";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getLogger().info("Loading Holograms into Hologram Map Cache...");
            int hologramCount = 0;

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    Location location = null;

                    // Handle possible null values for location
                    String locationString = resultSet.getString("location");

                    if (locationString != null) {
                        location = LocationUtil.stringToLocation(locationString);
                    }

                    if (location != null) {
                        // Ensure UUIDs are not null and valid
                        String uuidEntityString = resultSet.getString("uuid_entity");
                        String uuidGraveString = resultSet.getString("uuid_grave");

                        if (uuidEntityString != null && uuidGraveString != null) {
                            UUID uuidEntity = UUID.fromString(uuidEntityString);
                            UUID uuidGrave = UUID.fromString(uuidGraveString);
                            int line = resultSet.getInt("line");

                            getChunkData(location).addEntityData(new HologramData(location, uuidEntity, uuidGrave, line));
                            hologramCount++;  // Increment hologram count
                        } else {
                            plugin.getLogger().warning("Missing UUIDs in result set for location: " + location);
                        }
                    } else {
                        plugin.getLogger().warning("Invalid location data in result set.");
                    }
                }
                if (hologramCount == 0) {
                    plugin.getLogger().info("Loaded 0 Holograms into Hologram Map Cache.");
                } else {
                    plugin.getLogger().info("Loaded " + hologramCount + " Holograms into Hologram Map Cache.");
                }

            } catch (SQLException exception) {
                plugin.getLogger().severe("Error occurred while loading Hologram Map: " + exception.getMessage());
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Loads entity data from the database.
     *
     * @param table the table name.
     * @param type  the type of entity data.
     */
    private void loadEntityDataMap(String table, EntityData.Type type) {
        String query = "SELECT * FROM " + table + ";";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getLogger().info("Loading Entity Data Map Cache for " + table + "...");
            int entityCount = 0;

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    Location location = null;

                    // Retrieve and convert location or chunk data
                    String locationString = resultSet.getString("location");

                    if (locationString != null) {
                        location = LocationUtil.stringToLocation(locationString);
                    } else {
                        plugin.getLogger().warning("Invalid location for result set entry");
                        continue; // Continue processing remaining entries
                    }

                    // Retrieve and validate UUIDs
                    String uuidEntityString = resultSet.getString("uuid_entity");
                    String uuidGraveString = resultSet.getString("uuid_grave");

                    if (uuidEntityString != null && uuidGraveString != null) {
                        UUID uuidEntity = UUID.fromString(uuidEntityString);
                        UUID uuidGrave = UUID.fromString(uuidGraveString);

                        // Add entity data to the chunk data map
                        getChunkData(location).addEntityData(new EntityData(location, uuidEntity, uuidGrave, type));
                        entityCount++;  // Increment entity count
                    } else {
                        plugin.getLogger().warning("Missing UUIDs for location: " + location);
                    }
                }

                if (entityCount == 0) {
                    plugin.getLogger().info("Loaded 0 entities into Entity Data Map Cache for " + table + ".");
                } else {
                    plugin.getLogger().info("Loaded " + entityCount + " entities into Entity Data Map Cache for " + table + ".");
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Error occurred while loading Entity Data Map: " + exception.getMessage());
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Adds block data to the database.
     *
     * @param blockData the block data to add.
     */
    public void addBlockData(BlockData blockData) {
        getChunkData(blockData.getLocation()).addBlockData(blockData);

        String query = "INSERT INTO block (location, uuid_grave, replace_material, replace_data) VALUES (?, ?, ?, ?)";
        Object[] parameters = new Object[4];

        // Set location
        parameters[0] = LocationUtil.locationToString(blockData.getLocation());

        // Set uuid_grave
        parameters[1] = blockData.getGraveUUID() != null ? blockData.getGraveUUID().toString() : null;

        // Set replace_material
        parameters[2] = blockData.getReplaceMaterial();

        // Set replace_data
        parameters[3] = blockData.getReplaceData();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to add block data: " + exception.getMessage());
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Removes block data from the database.
     *
     * @param location the location of the block data to remove.
     */
    public void removeBlockData(Location location) {
        getChunkData(location).removeBlockData(location);

        String query = "DELETE FROM block WHERE location = ?";
        Object[] parameters = { LocationUtil.locationToString(location) };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to remove block data: " + exception.getMessage());
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Adds hologram data to the database.
     *
     * @param hologramData the hologram data to add.
     */
    public void addHologramData(HologramData hologramData) {
        getChunkData(hologramData.getLocation()).addEntityData(hologramData);

        String query = "INSERT INTO hologram (uuid_entity, uuid_grave, line, location) VALUES (?, ?, ?, ?)";
        Object[] parameters = {
                hologramData.getUUIDEntity().toString(),
                hologramData.getUUIDGrave().toString(),
                hologramData.getLine(),
                LocationUtil.locationToString(hologramData.getLocation())
        };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to add hologram data: " + exception.getMessage());
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Removes hologram data from the database.
     *
     * @param entityDataList the list of entity data to remove.
     */
    public void removeHologramData(List<EntityData> entityDataList) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM hologram WHERE uuid_entity = ?";
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection != null ? connection.prepareStatement(sql) : null) {
                if (statement != null) {
                    for (EntityData hologramData : entityDataList) {
                        getChunkData(hologramData.getLocation()).removeEntityData(hologramData);
                        statement.setString(1, String.valueOf(hologramData.getUUIDEntity()));
                        statement.addBatch();
                    }
                    executeBatch(statement);
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Error occurred while removing hologram data: " + exception.getMessage());
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
        String query = "INSERT INTO " + table + " (location, uuid_entity, uuid_grave) VALUES (?, ?, ?)";

        String location = LocationUtil.locationToString(entityData.getLocation());
        Object[] parameters = {
                location,
                entityData.getUUIDEntity(),
                entityData.getUUIDGrave()
        };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to add entity data: " + e.getMessage());
                plugin.logStackTrace(e);
            }
        });
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
                if (statement != null) {
                    for (EntityData entityData : entityDataList) {
                        getChunkData(entityData.getLocation()).removeEntityData(entityData);
                        String table = entityDataTypeTable(entityData.getType());
                        String query = "DELETE FROM " + table + " WHERE uuid_entity = ?";
                        Object[] parameters = { entityData.getUUIDEntity() };
                        executeUpdate(query, parameters);
                        plugin.debugMessage("Removing " + table + " for grave " + entityData.getUUIDGrave(), 1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to remove entity data: " + e.getMessage());
                plugin.logStackTrace(e);
            }
        });
    }

    /**
     * Saves a graveyard to the database.
     *
     * @param graveyard The graveyard to save.
     */
    public void saveGraveyard(Graveyard graveyard) {
        String serializedLocations = Graveyard.serializeLocations(graveyard.getGraveLocationMap());
        plugin.getLogger().info("Saving serialized locations: " + serializedLocations);

        if (graveyardExists(graveyard.getName())) {
            updateGraveyard(graveyard, serializedLocations);
        } else {
            insertGraveyard(graveyard, serializedLocations);
        }
    }

    /**
     * Checks if a graveyard with the specified name exists in the database.
     *
     * @param name the name of the graveyard.
     * @return true if the graveyard exists, false otherwise.
     */
    public boolean graveyardExists(String name) {
        String query = "SELECT COUNT(*) FROM graveyards WHERE name = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection != null ? connection.prepareStatement(query) : null) {

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

    /**
     * Updates a graveyard in the database.
     *
     * @param graveyard           the graveyard to update.
     * @param serializedLocations the new serialized locations for the graveyard.
     */
    private void updateGraveyard(Graveyard graveyard, String serializedLocations) {
        String query = "UPDATE graveyards SET world = ?, type = ?, serializedLocations = ? WHERE name = ?";

        Object[] parameters = {
                graveyard.getWorld().getName(),
                graveyard.getType().toString(),
                serializedLocations,
                graveyard.getName()
        };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update graveyard: " + e.getMessage());
                plugin.logStackTrace(e);
            }
        });
    }

    /**
     * Inserts a graveyard into the database.
     *
     * @param graveyard           the graveyard to insert.
     * @param serializedLocations the serialized locations for the graveyard.
     */
    private void insertGraveyard(Graveyard graveyard, String serializedLocations) {
        String query = "INSERT INTO graveyards (name, world, type, serializedLocations) VALUES (?, ?, ?, ?)";

        Object[] parameters = {
                graveyard.getName(),
                graveyard.getWorld().getName(),
                graveyard.getType().toString(),
                serializedLocations
        };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to insert graveyard: " + e.getMessage());
                plugin.logStackTrace(e);
            }
        });
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
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, graveyardName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String world = resultSet.getString("world");
                    String type = resultSet.getString("type");
                    String serializedLocations = resultSet.getString("serializedLocations");

                    World serverWorld = plugin.getServer().getWorld(world);
                    if (serverWorld == null) {
                        plugin.getLogger().warning("World not found for graveyard '" + name + "': " + world);
                        return null;
                    }

                    Graveyard.Type graveyardType = Graveyard.Type.valueOf(type.toUpperCase());
                    Graveyard graveyard = new Graveyard(name, serverWorld, graveyardType);

                    // Deserialize locations
                    Map<Location, BlockFace> locations = Graveyard.deserializeLocations(serializedLocations);
                    for (Map.Entry<Location, BlockFace> entry : locations.entrySet()) {
                        graveyard.addGraveLocation(entry.getKey(), entry.getValue());
                    }

                    return graveyard;
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

        Object[] parameters = {
                graveyard.getName(),
                graveyard.getWorld().getName()
        };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete graveyard: " + e.getMessage());
                plugin.logStackTrace(e);
            }
        });
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

        String query = "INSERT INTO grave (uuid, owner_type, owner_name, owner_name_display, owner_uuid, owner_texture, owner_texture_signature, killer_type, killer_name, killer_name_display, killer_uuid, location_death, yaw, pitch, inventory, equipment, experience, protection, is_abandoned, time_alive, time_protection, time_creation, permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Prepare parameters
        Object[] parameters = {
                grave.getUUID(),
                grave.getOwnerType(),
                grave.getOwnerName() != null ? grave.getOwnerName().replace("'", "''") : null,
                grave.getOwnerNameDisplay() != null ? grave.getOwnerNameDisplay().replace("'", "''") : null,
                grave.getOwnerUUID(),
                grave.getOwnerTexture() != null ? grave.getOwnerTexture().replace("'", "''") : null,
                grave.getOwnerTextureSignature() != null ? grave.getOwnerTextureSignature().replace("'", "''") : null,
                grave.getKillerType(),
                grave.getKillerName() != null ? grave.getKillerName().replace("'", "''") : null,
                grave.getKillerNameDisplay() != null ? grave.getKillerNameDisplay().replace("'", "''") : null,
                grave.getKillerUUID(),
                grave.getLocationDeath() != null ? LocationUtil.locationToString(grave.getLocationDeath()) : null,
                grave.getYaw(),
                grave.getPitch(),
                InventoryUtil.inventoryToString(grave.getInventory()),
                Base64Util.objectToBase64(grave.getEquipmentMap().entrySet().stream()
                        .filter(entry -> entry.getValue() != null)
                        .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue))),
                grave.getExperience(),
                grave.getProtection() ? 1 : 0,
                grave.isAbandoned() ? 1 : 0,
                grave.getTimeAlive(),
                grave.getTimeProtection(),
                grave.getTimeCreation(),
                grave.getPermissionList() != null && !grave.getPermissionList().isEmpty() ? StringUtils.join(grave.getPermissionList(), "|") : null
        };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to add grave: " + e.getMessage());
                plugin.logStackTrace(e);
            }
        });
    }

    /**
     * Removes a grave from the database.
     *
     * @param grave the grave to remove.
     */
    public void removeGrave(Grave grave) {
        removeGrave(grave.getUUID());
    }

    public void removeGrave(UUID uuid) {
        plugin.getCacheManager().getGraveMap().remove(uuid);

        String selectQuery = "SELECT * FROM grave WHERE uuid = ?";
        String deleteQuery = "DELETE FROM grave WHERE uuid = ?";
        Object[] deleteParams = { uuid };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.debugMessage("Attempting to remove grave for UUID: " + uuid, 1);

                // Select the grave data
                try (ResultSet rs = executeQuery(selectQuery, new Object[]{uuid})) {
                    if (rs != null && rs.next()) {
                        backupGrave(rs);
                    } else {
                        plugin.debugMessage("No grave found for UUID: " + uuid, 1);
                    }

                    // Remove the grave from the main table
                    executeUpdate(deleteQuery, deleteParams);
                    plugin.debugMessage("Grave successfully removed for UUID: " + uuid, 1);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to remove and backup grave: " + e.getMessage());
                plugin.logStackTrace(e);
            }
        });
    }

    /**
     * Backs up the grave data to the grave_backup table.
     *
     * @param rs the ResultSet containing the grave data to back up.
     * @throws SQLException if a database access error occurs.
     */
    private void backupGrave(ResultSet rs) throws SQLException {
        String insertBackupQuery = "INSERT INTO grave_backup (" +
                "uuid, owner_type, owner_name, owner_name_display, owner_uuid, owner_texture, " +
                "owner_texture_signature, killer_type, killer_name, killer_name_display, killer_uuid, " +
                "location_death, yaw, pitch, inventory, equipment, experience, protection, " +
                "is_abandoned, time_alive, time_protection, time_creation, permissions) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Object[] backupParams = {
                rs.getString("uuid"),
                rs.getString("owner_type"),
                rs.getString("owner_name"),
                rs.getString("owner_name_display"),
                rs.getString("owner_uuid"),
                rs.getString("owner_texture"),
                rs.getString("owner_texture_signature"),
                rs.getString("killer_type"),
                rs.getString("killer_name"),
                rs.getString("killer_name_display"),
                rs.getString("killer_uuid"),
                rs.getString("location_death"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch"),
                rs.getString("inventory"),
                rs.getString("equipment"),
                rs.getInt("experience"),
                rs.getInt("protection"),
                rs.getInt("is_abandoned"),
                rs.getLong("time_alive"),
                rs.getLong("time_protection"),
                System.currentTimeMillis(),  // Use current time for backup creation
                rs.getString("permissions"),
        };

        // Create a new database connection for the backup
        try {
            plugin.debugMessage("Backing up grave data for UUID: " + rs.getString("uuid"), 1);
            executeUpdate(insertBackupQuery, backupParams);
            plugin.debugMessage("Grave successfully backed up for UUID: " + rs.getString("uuid"), 1);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to back up grave: " + e.getMessage());
            throw e;  // Rethrow the exception to be handled in the calling method
        }
    }

    /**
     * Deletes all grave backups older than 30 days.
     */
    public void deleteOldBackups() {
        String deleteOldBackupsQuery = "DELETE FROM grave_backup WHERE time_creation < ?";

        // Calculate the timestamp of 30 days ago
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        Object[] params = { thirtyDaysAgo };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Execute the deletion without returning the number of rows deleted
                executeUpdate(deleteOldBackupsQuery, params);

                // Log a success message (optional)
                plugin.debugMessage("Deleted backups older than 30 days.", 5);
            } catch (SQLException e) {
                plugin.debugMessage("Failed to delete old grave backups: " + e.getMessage(), 5);
            }
        });
    }

    /**
     * Updates a grave in the database.
     *
     * @param grave  the grave to update.
     * @param column the column to update.
     * @param integer the new integer value for the column.
     */
    public void updateGrave(Grave grave, String column, int integer) {
        String query = "UPDATE grave SET " + column + " = ? WHERE uuid = ?";
        Object[] parameters = { integer, grave.getUUID() };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update grave: " + e.getMessage());
                plugin.logStackTrace(e);
            }
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
        String query = "UPDATE grave SET " + column + " = ? WHERE uuid = ?";
        Object[] parameters = { string, grave.getUUID() };

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update grave: " + e.getMessage());
                plugin.logStackTrace(e);
            }
        });
    }

    /**
     * Converts a ResultSet to a Grave object.
     *
     * @param resultSet the ResultSet to convert.
     * @return the Grave object, or null if an error occurs.
     */
    public Grave resultSetToGrave(ResultSet resultSet) {
        try {
            Grave grave = new Grave(UUID.fromString(resultSet.getString("uuid")));

            grave.setOwnerType(resultSet.getString("owner_type") != null
                    ? EntityType.valueOf(resultSet.getString("owner_type")) : null);
            grave.setOwnerName(resultSet.getString("owner_name").replace(" ", "_"));
            grave.setOwnerNameDisplay(resultSet.getString("owner_name_display"));
            grave.setOwnerUUID(resultSet.getString("owner_uuid") != null
                    ? UUID.fromString(resultSet.getString("owner_uuid")) : null);
            grave.setOwnerTexture(resultSet.getString("owner_texture"));
            grave.setOwnerTextureSignature(resultSet.getString("owner_texture_signature"));
            grave.setKillerType(resultSet.getString("killer_type") != null
                    ? EntityType.valueOf(resultSet.getString("killer_type")) : null);
            grave.setKillerName(resultSet.getString("killer_name").replace(" ", "_"));
            grave.setKillerNameDisplay(resultSet.getString("killer_name_display").replace(" ", "_"));
            grave.setKillerUUID(resultSet.getString("killer_uuid") != null
                    ? UUID.fromString(resultSet.getString("killer_uuid")) : null);
            grave.setLocationDeath(resultSet.getString("location_death") != null
                    ? LocationUtil.stringToLocation(resultSet.getString("location_death")) : null);
            grave.setYaw(resultSet.getFloat("yaw"));
            grave.setPitch(resultSet.getFloat("pitch"));
            grave.setExperience(resultSet.getInt("experience"));
            grave.setProtection(resultSet.getInt("protection") == 1);
            grave.setAbandoned(resultSet.getInt("is_abandoned") == 1);
            grave.setTimeAlive(resultSet.getLong("time_alive"));
            grave.setTimeProtection(resultSet.getLong("time_protection"));
            grave.setTimeCreation(resultSet.getLong("time_creation"));
            grave.setPermissionList(resultSet.getString("permissions") != null
                    ? new ArrayList<>(Arrays.asList(resultSet.getString("permissions").split("\\|"))) : new ArrayList<>());
            grave.setInventory(InventoryUtil.stringToInventory(grave, resultSet.getString("inventory"),
                    StringUtil.parseString(plugin.getConfig("gui.grave.title", grave.getOwnerType(),
                                    grave.getPermissionList())
                            .getString("gui.grave.title"), grave.getLocationDeath(), grave, plugin), plugin));

            if (resultSet.getString("equipment") != null) {
                @SuppressWarnings("unchecked")
                Map<EquipmentSlot, ItemStack> equipmentMap = (Map<EquipmentSlot, ItemStack>) Base64Util
                        .base64ToObject(resultSet.getString("equipment"));
                grave.setEquipmentMap(equipmentMap != null ? equipmentMap : new HashMap<>());
            }

            return grave;
        } catch (SQLException exception) {
            plugin.getLogger().severe("Error occurred while converting a ResultSet to a Grave object: " + exception.getMessage());
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
     * Retrieves the versions of supported databases.
     *
     * @return a map of database types and their versions as integers.
     * @throws SQLException if a database access error occurs.
     */
    public Map<String, Map<String, Integer>> getDatabaseVersions() throws SQLException {
        Map<String, Map<String, Integer>> versionData = new HashMap<>();
        String version = getDatabaseVersion(); // Fetch version as a string

        // Use a dummy integer value (e.g., 1) for each version entry
        Map<String, Integer> versionInfo = new HashMap<>();
        if (type == Type.MYSQL) {
            versionInfo.put(version, 1);
            versionData.put("MySQL", versionInfo);
        } else if (type == Type.MARIADB) {
            versionInfo.put(version, 1);
            versionData.put("MariaDB", versionInfo);
        } else if (type == Type.POSTGRESQL) {
            versionInfo.put(version, 1);
            versionData.put("PostgreSQL", versionInfo);
        } else if (type == Type.MSSQL) {
            versionInfo.put(version, 1);
            versionData.put("Microsoft SQL Server", versionInfo);
        } else {
            versionData.put("Other", Collections.singletonMap("Unknown", 1));
        }
        return versionData;
    }

    /**
     * Retrieves the version of the database connection type.
     *
     * @return the database version as a string.
     * @throws SQLException if a database access error occurs.
     */
    public String getDatabaseVersion() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String query;
            if (type == Type.POSTGRESQL || type == Type.MARIADB || type == Type.MYSQL) {
                query = "SELECT version()";
            } else {
                return "Unknown";
            }

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                if (resultSet.next()) {
                    String version = resultSet.getString(1);
                    if (version != null && !version.isEmpty()) {
                        // Split the version string by the comma and return the first part
                        return version.split(",")[0];
                    } else {
                        return "Unknown";
                    }
                } else {
                    return "Unknown";
                }
            }
        } catch (Exception e) {
            return "Unknown";
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
        if (statement == null) {
            plugin.debugMessage("Attempted to execute batch with a null statement.", 1);
            return;
        }

        try {
            statement.executeBatch();
        } catch (SQLException exception) {
            // Log the error message and the state of the batch statement
            plugin.getLogger().severe("Error occurred while executing batch: " + exception.getMessage());
            plugin.getLogger().severe("Failed batch statement: " + statement);
            plugin.logStackTrace(exception);
        }
    }

    /**
     * Executes an update SQL statement (deprecated method).
     *
     * @param sql the SQL statement.
     * @deprecated Use {@link #executeUpdate(String, Object[])} instead for parameterized queries.
     */
    @Deprecated
    private void executeUpdate(String sql) throws SQLException {
        // Convert the SQL statement to an array of parameters for the new method
        // This is a simple conversion assuming no parameters were used. For a real case, you'd need a more complex implementation.
        executeUpdate(sql, new Object[0]);
    }

    /**
     * Executes an update SQL statement with parameters.
     *
     * @param sql        the SQL statement.
     * @param parameters the parameters for the SQL statement.
     * @throws SQLException if a database access error occurs.
     */
    private void executeUpdate(String sql, Object[] parameters) throws SQLException {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                if (parameters != null) {
                    for (int i = 0; i < parameters.length; i++) {
                        Object parameter = parameters[i];
                        if (parameter == null) {
                            // Use specific SQL types for null values
                            statement.setNull(i + 1, Types.VARCHAR); // Adjust based on expected parameter type
                        } else if (parameter instanceof String) {
                            statement.setString(i + 1, (String) parameter);
                        } else if (parameter instanceof Integer) {
                            statement.setInt(i + 1, (Integer) parameter);
                        } else if (parameter instanceof Long) {
                            statement.setLong(i + 1, (Long) parameter);
                        } else if (parameter instanceof Double) {
                            statement.setDouble(i + 1, (Double) parameter);
                        } else if (parameter instanceof Float) {
                            statement.setFloat(i + 1, (Float) parameter);
                        } else if (parameter instanceof Boolean) {
                            statement.setBoolean(i + 1, (Boolean) parameter); // Use setBoolean for MSSQL
                        } else if (parameter instanceof UUID) {
                            statement.setObject(i + 1, parameter.toString(), Types.VARCHAR);
                        } else if (parameter instanceof byte[]) {
                            statement.setBytes(i + 1, (byte[]) parameter);
                        } else if (parameter instanceof Date) {
                            statement.setDate(i + 1, (Date) parameter);
                        } else if (parameter instanceof Timestamp) {
                            statement.setTimestamp(i + 1, (Timestamp) parameter);
                        } else if (parameter instanceof LocalDate) {
                            statement.setObject(i + 1, parameter, Types.DATE);
                        } else if (parameter instanceof LocalDateTime) {
                            statement.setObject(i + 1, parameter, Types.TIMESTAMP);
                        } else if (parameter instanceof Clob) {
                            statement.setClob(i + 1, (Clob) parameter);
                        } else if (parameter instanceof Blob) {
                            statement.setBlob(i + 1, (Blob) parameter);
                        } else if (parameter instanceof EntityType) {
                            statement.setString(i + 1, ((EntityType) parameter).name());
                        } else {
                            statement.setObject(i + 1, parameter);
                        }
                    }
                }

                statement.executeUpdate();
            } catch (SQLException exception) {
                String sqlState = exception.getSQLState();
                String message = exception.getMessage().toLowerCase();
                // Ignore errors related to existing tables or columns
                if ("42701".equals(sqlState)
                        || "42P07".equals(sqlState)
                        || "42S01".equals(sqlState)
                        || "X0Y32".equals(sqlState)
                        || "42000".equals(sqlState)
                        || (message.contains("duplicate column name") && "SQLITE_ERROR".equals(sqlState))) {
                    // ignore
                } else {
                    plugin.getLogger().severe("Error executing SQL update: " + exception.getMessage());
                    plugin.getLogger().severe("Failed SQL statement: " + sql);
                    plugin.logStackTrace(exception);
                }
            }
        });
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
     * Executes a query SQL statement with parameters.
     *
     * @param sql the SQL statement with placeholders (e.g., ? for parameters).
     * @param params the parameters to be set in the prepared statement.
     * @return the ResultSet of the query.
     * @throws SQLException if an SQL error occurs.
     */
    private ResultSet executeQuery(String sql, Object[] params) throws SQLException {
        AtomicReference<ResultSet> resultSet = new AtomicReference<>();
        AtomicReference<PreparedStatement> preparedStatement = new AtomicReference<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection()) {
                if (connection != null) {
                    preparedStatement.set(connection.prepareStatement(sql));

                    // Set the parameters in the PreparedStatement
                    for (int i = 0; i < params.length; i++) {
                        preparedStatement.get().setObject(i + 1, params[i]);
                    }

                    resultSet.set(preparedStatement.get().executeQuery());
                }
            } catch (SQLException exception) {
                plugin.logStackTrace(exception);
            } finally {
                if (resultSet.get() != null) {
                    try {
                        resultSet.get().close();
                    } catch (SQLException exception) {
                        plugin.logStackTrace(exception);
                    }
                }
                if (preparedStatement.get() != null) {
                    try {
                        preparedStatement.get().close();
                    } catch (SQLException exception) {
                        plugin.logStackTrace(exception);
                    }
                }
            }
        });
        return resultSet.get();
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
     * Migrates data from SQLite to the target database (MySQL, MariaDB, PostgreSQL, H2, or MSSQL).
     */
    private void migrate() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File dataFolder = new File(plugin.getDataFolder(), "data");
            File sqliteFile = new File(dataFolder, "data.db");

            if (!sqliteFile.exists() || !dataFolder.exists()) {
                plugin.getLogger().warning("SQLite database file or folder does not exist in \"" + dataFolder.getPath() + "\". Skipping database migration.");
                return;
            }

            HikariConfig config = new HikariConfig();
            String journalMode = plugin.getConfig().getString("settings.storage.sqlite.journal-mode", "WAL");
            String synchronous = plugin.getConfig().getString("settings.storage.sqlite.synchronous", "OFF");

            config.setJdbcUrl("jdbc:sqlite:" + sqliteFile.getPath());
            config.setConnectionTimeout(30000); // 30 seconds
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setMaximumPoolSize(50);
            config.addDataSourceProperty("dataSource.journalMode", journalMode);
            config.addDataSourceProperty("dataSource.synchronous", synchronous);
            config.setConnectionInitSql("PRAGMA busy_timeout = 30000; PRAGMA journal_mode=" + journalMode + "; PRAGMA synchronous=" + synchronous + ";");
            config.setPoolName("Graves SQLite to " + type + " Migration");
            config.addDataSourceProperty("autoReconnect", "true");
            config.setDriverClassName("org.sqlite.JDBC");

            // Use a try-with-resources to ensure that HikariDataSource is closed after use
            try (HikariDataSource dataSourceMigrate = new HikariDataSource(config);
                 Connection sqliteConnection = dataSourceMigrate.getConnection()) {

                DatabaseMetaData metaData = sqliteConnection.getMetaData();
                try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                    boolean migrationSuccess = true;

                    while (tables.next()) {
                        String tableName = tables.getString("TABLE_NAME");
                        StringBuilder createTableQuery = new StringBuilder();
                        List<String> columns = new ArrayList<>();

                        // Using try-with-resources for the statement
                        try (Statement sqliteStatement = sqliteConnection.createStatement();
                             ResultSet tableData = sqliteStatement.executeQuery("SELECT * FROM " + tableName)) {

                            ResultSetMetaData tableMetaData = tableData.getMetaData();
                            createTableQuery.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
                            for (int i = 1; i <= tableMetaData.getColumnCount(); i++) {
                                String columnName = tableMetaData.getColumnName(i);
                                String sqliteType = tableMetaData.getColumnTypeName(i);
                                String targetType = mapSQLiteTypeToTargetDB(sqliteType, columnName);

                                if (targetType != null) {
                                    createTableQuery.append(columnName).append(" ").append(targetType);
                                    if (i < tableMetaData.getColumnCount()) {
                                        createTableQuery.append(", ");
                                    } else {
                                        createTableQuery.append(")");
                                    }
                                    columns.add(columnName);
                                }
                            }

                            if (columns.isEmpty()) {
                                plugin.getLogger().warning("No valid columns found for table " + tableName + ". Skipping table creation.");
                                continue;
                            }

                            plugin.getLogger().info("Creating table with query: " + createTableQuery.toString());
                            executeUpdate(createTableQuery.toString(), new Object[0]);

                            if ("grave".equals(tableName)) {
                                adjustGraveTableForTargetDB();
                            }

                            String insertQueryTemplate = "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", Collections.nCopies(columns.size(), "?")) + ")";
                            try (PreparedStatement insertStatement = getConnection().prepareStatement(insertQueryTemplate)) {
                                while (tableData.next()) {
                                    for (int i = 1; i <= tableMetaData.getColumnCount(); i++) {
                                        String columnName = tableMetaData.getColumnName(i);
                                        if (columns.contains(columnName)) {
                                            String data = tableData.getString(i);
                                            if (data != null) {
                                                data = data.replace("'", "''");
                                            }
                                            insertStatement.setString(columns.indexOf(columnName) + 1, data != null ? data : null);
                                        }
                                    }
                                    plugin.getLogger().info("Inserting data with query: " + insertQueryTemplate);
                                    insertStatement.executeUpdate();
                                }
                            } catch (SQLException e) {
                                plugin.getLogger().severe("Error inserting data into table " + tableName + ": " + e.getMessage());
                                plugin.getLogger().severe("Failed query template: " + insertQueryTemplate);
                                migrationSuccess = false;
                            }
                        } catch (SQLException e) {
                            plugin.getLogger().severe("Error migrating table " + tableName + ": " + e.getMessage());
                            plugin.getLogger().severe("Failed query: " + createTableQuery);
                            migrationSuccess = false;
                        }
                    }

                    if (migrationSuccess) {
                        File renamedFile = new File(dataFolder, "data.old.db");
                        if (sqliteFile.renameTo(renamedFile)) {
                            plugin.getLogger().info("SQLite database successfully renamed to data.old.db");
                        } else {
                            plugin.getLogger().severe("Failed to rename SQLite database to data.old.db");
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Error retrieving tables from SQLite: " + e.getMessage());
                }

                try {
                    sqliteConnection.close();
                } catch (SQLException e) {
                    plugin.getLogger().severe("Error closing SQLite connection: " + e.getMessage());
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error migrating SQLite to target DB: " + e.getMessage());
            }
        });
    }

    /**
     * Maps SQLite data types to target database data types (MySQL/MariaDB, PostgreSQL, H2, or MSSQL).
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
            case MSSQL:
                return mapSQLiteTypeToMSSQL(sqliteType, columnName);
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
     * Maps SQLite data types to Microsoft SQL Server data types.
     *
     * @param sqliteType the SQLite data type.
     * @param columnName the column name.
     * @return the MSSQL data type.
     */
    private String mapSQLiteTypeToMSSQL(String sqliteType, String columnName) {
        switch (sqliteType.toUpperCase()) {
            case "INT":
            case "BIGINT":
            case "INTEGER":
                if ("protection".equals(columnName))
                    return "BIT"; // BOOLEAN in MSSQL
                if ("time_protection".equals(columnName) || "time_creation".equals(columnName) || "time_alive".equals(columnName))
                    return "BIGINT";
                return "INT";
            case "VARCHAR":
                return "NVARCHAR(255)";
            case "FLOAT":
                return "FLOAT";
            case "TEXT":
                return "NVARCHAR(MAX)";
            case "BLOB":
                return "VARBINARY(MAX)";
            case "REAL":
                return "FLOAT";
            case "NUMERIC":
                return "DECIMAL(10, 5)";
            default:
                plugin.getLogger().warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
                return null; // Ignore unhandled types
        }
    }

    /**
     * Adjusts the grave table for the target database if necessary for MSSQL.
     */
    private void adjustGraveTableForTargetDB() throws SQLException {
        // Ensure column sizes and types are correct for MSSQL
        if (this.type == Type.MSSQL) {
            plugin.getLogger().info("Altering table grave to ensure column sizes are correct for MSSQL.");

            // Example SQL commands to alter column types for MSSQL
            executeUpdate("ALTER TABLE grave ALTER COLUMN owner_texture NVARCHAR(MAX)", new Object[0]);
            executeUpdate("ALTER TABLE grave ALTER COLUMN owner_texture_signature NVARCHAR(MAX)", new Object[0]);
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_creation BIGINT", new Object[0]);
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_protection BIGINT", new Object[0]);
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_alive BIGINT", new Object[0]);
        } else if (this.type == Type.MYSQL || this.type == Type.MARIADB) {
            plugin.getLogger().info("Altering table grave to ensure column sizes are correct for MySQL/MariaDB.");
            executeUpdate("ALTER TABLE grave MODIFY owner_texture NVARCHAR(MAX)", new Object[0]);
            executeUpdate("ALTER TABLE grave MODIFY owner_texture_signature NVARCHAR(MAX)", new Object[0]);
            executeUpdate("ALTER TABLE grave MODIFY time_creation BIGINT", new Object[0]);
            executeUpdate("ALTER TABLE grave MODIFY time_protection BIGINT", new Object[0]);
            executeUpdate("ALTER TABLE grave MODIFY time_alive BIGINT", new Object[0]);
        } else if (this.type == Type.POSTGRESQL || this.type == Type.H2) {
            plugin.getLogger().info("Altering table grave to ensure column sizes are correct for PostgreSQL/H2.");
            executeUpdate("ALTER TABLE grave ALTER COLUMN owner_texture TYPE TEXT", new Object[0]);
            executeUpdate("ALTER TABLE grave ALTER COLUMN owner_texture_signature TYPE TEXT", new Object[0]);
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_creation TYPE BIGINT", new Object[0]);
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_protection TYPE BIGINT", new Object[0]);
            executeUpdate("ALTER TABLE grave ALTER COLUMN time_alive TYPE BIGINT", new Object[0]);
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
     * Checks if the Database is locked and attempts to unlock the database.
     */
    private void checkAndUnlockDatabase() {
        String checkQuery = "SELECT 1";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection != null ? connection.createStatement() : null) {

            if (statement != null) {
                statement.executeQuery(checkQuery);
            } else {
                throw new NullPointerException();
            }
        } catch (NullPointerException | SQLException e) {
            if (e.getMessage().contains("database is locked")) {
                plugin.getLogger().severe("Database is locked. Attempting to unlock...");

                switch (type) {
                    case SQLITE:
                        handleUnlockSQLite();
                        break;
                    case MYSQL:
                    case MARIADB:
                        handleUnlockMySQL();
                        break;
                    case POSTGRESQL:
                    case H2:
                        handleUnlockPostgreSQLandH2();
                        break;
                    case MSSQL:
                        handleUnlockMSSQL();
                        break;
                    case INVALID:
                    default:
                        plugin.getLogger().warning("Server is using an invalid database type. No attempts to fix database locking will be attempted.");
                }
            }
        }
    }

    /**
     * Handles unlocking for SQLite databases using COMMIT or Rollback.
     */
    private void handleUnlockSQLite() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null) {
                connection.setAutoCommit(false);
                connection.commit();
                plugin.getLogger().info("SQLite database unlocked successfully using COMMIT.");
            } else {
                throw new NullPointerException();
            }
        } catch (NullPointerException | SQLException e) {
            plugin.getLogger().severe("Failed to unlock SQLite database using COMMIT");
            plugin.logStackTrace(e);
            try (Connection connection = dataSource.getConnection()) {
                if (connection != null) {
                    connection.rollback();
                    plugin.getLogger().info("SQLite database unlocked successfully using ROLLBACK.");
                } else {
                    throw new NullPointerException();
                }
            } catch (NullPointerException | SQLException rollbackException) {
                plugin.getLogger().severe("Failed to unlock SQLite database using ROLLBACK");
                plugin.logStackTrace(rollbackException);
            }
        }
    }

    /**
     * Handles unlocking for MySQL and MariaDB databases.
     */
    private void handleUnlockMySQL() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null) {
                plugin.getLogger().info("Attempting to unlock MySQL/MariaDB database.");
                connection.setAutoCommit(false);
                connection.commit();
                plugin.getLogger().info("MySQL/MariaDB database unlocked successfully using COMMIT.");
            } else {
                throw new NullPointerException();
            }
        } catch (NullPointerException | SQLException e) {
            plugin.getLogger().severe("Failed to unlock MySQL/MariaDB database using COMMIT");
            plugin.logStackTrace(e);
        }
    }

    /**
     * Handles unlocking for PostgreSQL/H2 databases using COMMIT or Rollback.
     */
    private void handleUnlockPostgreSQLandH2() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null) {
                connection.setAutoCommit(false);
                connection.commit();
                plugin.getLogger().info("PostgreSQL/H2 database unlocked successfully using COMMIT.");
            } else {
                throw new NullPointerException();
            }
        } catch (NullPointerException | SQLException e) {
            plugin.getLogger().severe("Failed to unlock PostgreSQL/H2 database using COMMIT");
            plugin.logStackTrace(e);
            try (Connection connection = dataSource.getConnection()) {
                if (connection != null) {
                    connection.rollback();
                    plugin.getLogger().info("PostgreSQL/H2 database unlocked successfully using ROLLBACK.");
                } else {
                    throw new NullPointerException();
                }
            } catch (NullPointerException | SQLException rollbackException) {
                plugin.getLogger().severe("Failed to unlock PostgreSQL/H2 database using ROLLBACK");
                plugin.logStackTrace(rollbackException);
            }
        }
    }

    /**
     * Handles unlocking for Microsoft SQL Server databases using COMMIT.
     */
    private void handleUnlockMSSQL() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null) {
                plugin.getLogger().info("Attempting to unlock MSSQL database.");
                connection.setAutoCommit(false);
                connection.commit();
                plugin.getLogger().info("MSSQL database unlocked successfully using COMMIT.");
            } else {
                throw new NullPointerException();
            }
        } catch (NullPointerException | SQLException e) {
            plugin.getLogger().severe("Failed to unlock MSSQL database using COMMIT");
            plugin.logStackTrace(e);
        }
    }
}