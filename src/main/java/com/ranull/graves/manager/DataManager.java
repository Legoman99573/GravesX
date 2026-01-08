package com.ranull.graves.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.ranull.graves.Graves;
import com.ranull.graves.data.*;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.*;
import dev.cwhead.GravesX.api.provider.GraveProvider;
import dev.cwhead.GravesX.api.provider.RegisterGraveProviders;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.nio.file.*;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Manages data storage and retrieval for the Graves plugin.
 */
public class DataManager {
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
            case SQLITE -> {
                plugin.getLogger().warning("Database Option SQLITE is set for removal in a future release. Use H2 Database option instead for better reliance.");
                loadType(Type.SQLITE);
                load();
                keepConnectionAlive(); // If we don't enable this, connection will close or time out :/
            }
            case H2, POSTGRESQL, MYSQL, MARIADB -> {
                loadType(this.type);
                if (testDatabaseConnection()) {
                    migrate();
                    load();
                    keepConnectionAlive(); // If we don't enable this, connection will close or time out :/
                } else {
                    plugin.getLogger().severe("Failed to connect to " + this.type + " database. Disabling plugin...");
                    plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                }
            }
            case MSSQL -> {
                loadType(Type.MSSQL);
                if (testDatabaseConnection()) {
                    migrate();
                    load();
                    keepConnectionAlive();
                } else {
                    plugin.getLogger().severe("Failed to connect to " + this.type + " database. Disabling plugin...");
                    plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                }
            }
            default -> {
                plugin.getLogger().severe("Database Type is invalid. Only valid options: SQLITE, H2, POSTGRESQL, MARIADB, and MYSQL. Disabling plugin...");
                plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            }
        }
    }

    public String getType() {
        return switch (type) {
            case H2 -> "H2";
            case MYSQL -> "MySQL";
            case SQLITE -> "SQLite";
            case MARIADB -> "MariaDB";
            case POSTGRESQL -> "PostgreSQL";
            case MSSQL -> "Microsoft SQL Server";
            case INVALID -> null;
            default -> null;
        };
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
        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
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

            Map<String, EntityData.Type> integrationMap = new HashMap<>();
            integrationMap.put("furniturelib", EntityData.Type.FURNITURELIB);
            integrationMap.put("furnitureengine", EntityData.Type.FURNITUREENGINE);
            integrationMap.put("itemsadder", EntityData.Type.ITEMSADDER);
            integrationMap.put("oraxen", EntityData.Type.ORAXEN);
            integrationMap.put("nexo", EntityData.Type.NEXO);
            integrationMap.put("playernpc", EntityData.Type.PLAYERNPC);
            integrationMap.put("mannequins", EntityData.Type.MANNEQUIN);
            for (Map.Entry<String, EntityData.Type> entry : integrationMap.entrySet()) {
                String integration = entry.getKey();
                EntityData.Type type = entry.getValue();

                if (isIntegrationEnabled(integration)) {
                    createEntityDataMapTable(integration);
                    loadEntityDataMap(integration, type);
                    if (integration.equals("playernpc")) {
                        plugin.getIntegrationManager().getPlayerNPC().createCorpses();
                    }
                }
            }

            List<GraveProvider> providers = RegisterGraveProviders.getAll();
            if (providers.isEmpty()) return;

            Set<String> seen = new LinkedHashSet<>();
            for (GraveProvider p : providers) {
                if (p == null) continue;

                String id = p.id();
                if (id == null || id.isBlank()) {
                    plugin.getLogger().warning("Skipping GraveProvider with empty id: " + p.getClass().getName());
                    continue;
                }

                String key = "custom_" + sanitizeKey(id);
                if (!seen.add(key)) continue;

                try {
                    createEntityDataMapTable(key);
                    loadEntityDataMap(key, EntityData.Type.CUSTOM);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed initializing custom provider for key: " + key);
                    plugin.logStackTrace(e);
                }
            }
        });
    }

    /**
     * Checks if the integration is enabled.
     * @param integration The name of the integration.
     * @return true if enabled, false otherwise.
     */
    private boolean isIntegrationEnabled(String integration) {
        String key = sanitizeKey(integration);
        return switch (key) {
            case "furniturelib" -> plugin.getIntegrationManager().hasFurnitureLib();
            case "furnitureengine" -> plugin.getIntegrationManager().hasFurnitureEngine();
            case "itemsadder" -> plugin.getIntegrationManager().hasItemsAdder();
            case "oraxen" -> plugin.getIntegrationManager().hasOraxen();
            case "nexo" -> plugin.getIntegrationManager().hasNexo();
            case "playernpc" -> plugin.getIntegrationManager().hasPlayerNPC();
            case "mannequins" -> plugin.getIntegrationManager().hasMannequins();
            default -> {
                if (key.startsWith("custom_")) {
                    String expected = key.substring("custom_".length());
                    List<GraveProvider> providers = RegisterGraveProviders.getAll();
                    if (providers.isEmpty()) {
                        yield false;
                    }
                    boolean found = false;
                    for (GraveProvider p : providers) {
                        if (p == null) continue;
                        String id = p.id();
                        if (id == null || id.isBlank()) continue;
                        if (sanitizeKey(id).equals(expected)) {
                            found = true;
                            break;
                        }
                    }
                    yield found;
                }
                yield false;
            }
        };
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
        setupEntityTables();
    }

    /**
     * Sets up entity tables.
     *
     * @throws SQLException if an SQL error occurs.
     */
    private void setupEntityTables() throws SQLException {
        Map<String, Boolean> integrationMap = new HashMap<>();
        integrationMap.put("armorstand", true);
        integrationMap.put("itemframe", true);
        integrationMap.put("furniturelib", plugin.getIntegrationManager().hasFurnitureLib());
        integrationMap.put("furnitureengine", plugin.getIntegrationManager().hasFurnitureEngine());
        integrationMap.put("itemsadder", plugin.getIntegrationManager().hasItemsAdder());
        integrationMap.put("oraxen", plugin.getIntegrationManager().hasOraxen());
        integrationMap.put("nexo", plugin.getIntegrationManager().hasNexo());
        integrationMap.put("playernpc", plugin.getIntegrationManager().hasPlayerNPC());
        integrationMap.put("mannequins", plugin.getIntegrationManager().hasMannequins());

        for (Map.Entry<String, Boolean> entry : integrationMap.entrySet()) {
            if (entry.getValue()) {
                setupEntityTable(entry.getKey());
            }
        }

        List<GraveProvider> providers = RegisterGraveProviders.getAll();
        if (providers.isEmpty()) return;

        Set<String> seen = new LinkedHashSet<>();
        for (GraveProvider p : providers) {
            if (p == null) continue;
            String id = p.id();
            if (id == null || id.isBlank()) continue;

            String key = "custom_" + sanitizeKey(id);
            if (seen.add(key)) {
                setupEntityTable(key);
            }
        }
    }

    /**
     * Sanitizes custom grave providers.
     */
    private static String sanitizeKey(String s) {
        return s == null ? "unknown" : s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
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
        switch (type) {
            case SQLITE -> {
                plugin.getLogger().warning("Database Option SQLITE is set for removal in a future release. Use H2 Database option instead for better reliance.");
                loadType(Type.SQLITE);
                load();
                keepConnectionAlive(); // If we don't enable this, connection will close or time out :/
            }
            case H2, POSTGRESQL, MYSQL, MARIADB -> {
                loadType(type);
                if (testDatabaseConnection()) {
                    migrate();
                    load();
                    keepConnectionAlive(); // If we don't enable this, connection will close or time out :/
                } else {
                    plugin.getLogger().severe("Failed to connect to " + type + " database. Disabling plugin...");
                    plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                }
            }
            case MSSQL -> {
                loadType(Type.MSSQL);
                if (testDatabaseConnection()) {
                    migrate();
                    load();
                    keepConnectionAlive();
                } else {
                    plugin.getLogger().severe("Failed to connect to " + type + " database. Disabling plugin...");
                    plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                }
            }
            default -> {
                plugin.getLogger().severe("Database Type is invalid. Only valid options: SQLITE, H2, POSTGRESQL, MARIADB, and MYSQL. Disabling plugin...");
                plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            }
        }
    }

    /**
     * Loads the database type and sets up the data source.
     *
     * @param type the type of database.
     */
    public void loadType(Type type) {
        this.type = type;
        HikariConfig config = new HikariConfig();

        switch (type) {
            case POSTGRESQL -> configurePostgreSQL(config);
            case SQLITE -> {
                migrateRootDataSubData();
                configureSQLite(config);
            }
            case H2 -> configureH2(config);
            case MSSQL -> configureMSSQL(config);
            case MARIADB, MYSQL -> configureMySQLOrMariaDB(config, type);
            default -> throw new IllegalArgumentException("Unsupported database type: " + type);
        }

        dataSource = new HikariDataSource(config);
        checkAndUnlockDatabase();

        if (type == Type.MYSQL) {
            checkMariaDBasMySQL();
        }
    }

    private void checkMariaDBasMySQL() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery("SELECT VERSION()");
            if (resultSet.next()) {
                String version = resultSet.getString(1);
                if (version.contains("MariaDB")) {
                    String versionNumber = version.split(" ")[0];

                    if (isVersionGreaterThan(versionNumber)) {
                        plugin.getLogger().warning("MySQL Warning: Your configuration is currently set to use MySQL, but the server is running MariaDB. As of MariaDB version 11, MySQL has been deprecated and will be removed in future versions. To avoid potential conflicts, we recommend updating your config.yml to use MARIADB.");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check version of MySQL");
            plugin.logStackTrace(e);
        }
    }

    private boolean isVersionGreaterThan(String version) {
        String[] versionParts = version.split("\\.");
        String[] compareToParts = "11".split("\\.");

        int maxLength = Math.max(versionParts.length, compareToParts.length);

        for (int i = 0; i < maxLength; i++) {
            int currentVersionPart = (i < versionParts.length) ? Integer.parseInt(versionParts[i]) : 0;
            int compareToVersionPart = (i < compareToParts.length) ? Integer.parseInt(compareToParts[i]) : 0;

            if (currentVersionPart > compareToVersionPart) {
                return true;
            } else if (currentVersionPart < compareToVersionPart) {
                return false;
            }
        }

        return false;
    }

    /**
     * Configures the SQLite data source.
     *
     * @param config the HikariConfig to configure.
     */
    private void configureSQLite(HikariConfig config) {
        String journalMode = plugin.getConfig().getString("settings.storage.sqlite.journal-mode", "WAL");
        String synchronous = plugin.getConfig().getString("settings.storage.sqlite.synchronous", "OFF");

        String jm = journalMode.toUpperCase(Locale.ROOT);
        if (!jm.equals("DELETE") && !jm.equals("TRUNCATE") && !jm.equals("PERSIST")
                && !jm.equals("MEMORY") && !jm.equals("WAL") && !jm.equals("OFF")) {
            jm = "WAL";
        }

        String syn = synchronous.toUpperCase(Locale.ROOT);
        if (!syn.equals("OFF") && !syn.equals("0")
                && !syn.equals("NORMAL") && !syn.equals("1")
                && !syn.equals("FULL") && !syn.equals("2")
                && !syn.equals("EXTRA") && !syn.equals("3")) {
            syn = "OFF";
        }

        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File dbFile = new File(dataDir, "data.db");
        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath() + "?busy_timeout=30000";

        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setPoolName("Graves SQLite");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.setConnectionInitSql("PRAGMA journal_mode=" + jm + "; PRAGMA synchronous=" + syn + ";");

        config.setConnectionTestQuery("SELECT 1");
    }

    /**
     * Configures the HikariConfig for PostgreSQL.
     *
     * @param config the HikariConfig to configure.
     */
    private void configurePostgreSQL(HikariConfig config) {
        String host = plugin.getConfig().getString("settings.storage.postgresql.host", "localhost");
        int port = plugin.getConfig().getInt("settings.storage.postgresql.port", 5432);
        String user = plugin.getConfig().getString("settings.storage.postgresql.username", "username");
        String password = plugin.getConfig().getString("settings.storage.postgresql.password", "password");
        String database = plugin.getConfig().getString("settings.storage.postgresql.database", "graves");
        long maxLifetime = plugin.getConfig().getLong("settings.storage.postgresql.maxLifetime", 1800000);
        int maxConnections = plugin.getConfig().getInt("settings.storage.postgresql.maxConnections", 20);
        long connectionTimeout = plugin.getConfig().getLong("settings.storage.postgresql.connectionTimeout", 30000);
        boolean ssl = plugin.getConfig().getBoolean("settings.storage.postgresql.ssl", true);

        String sslMode = plugin.getConfig().getString("settings.storage.postgresql.sslmode", ssl ? "require" : "disable");
        if (!"disable".equalsIgnoreCase(sslMode)
                && !"allow".equalsIgnoreCase(sslMode)
                && !"prefer".equalsIgnoreCase(sslMode)
                && !"require".equalsIgnoreCase(sslMode)
                && !"verify-ca".equalsIgnoreCase(sslMode)
                && !"verify-full".equalsIgnoreCase(sslMode)) {
            sslMode = ssl ? "require" : "disable";
        }

        String params = String.join("&",
                "sslmode=" + sslMode,
                "targetServerType=primary",
                "reWriteBatchedInserts=true",
                "ApplicationName=Graves"
        );
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?%s", host, port, database, params);

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.ranull.graves.libraries.postgresql.Driver");

        config.setMaximumPoolSize(maxConnections);
        config.setMinimumIdle(Math.min(2, maxConnections));
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(maxLifetime);

        try {
            config.setKeepaliveTime(300000);
            config.setValidationTimeout(5000);
        } catch (Throwable ignored) {
        }

        config.addDataSourceProperty("tcpKeepAlive", "true");

        String sslFactory = plugin.getConfig().getString("settings.storage.postgresql.sslfactory", "");
        if (!sslFactory.trim().isEmpty()) {
            config.addDataSourceProperty("sslfactory", sslFactory.trim());
        }

        String sslRootCert = plugin.getConfig().getString("settings.storage.postgresql.sslrootcert", "");
        String sslCert = plugin.getConfig().getString("settings.storage.postgresql.sslcert", "");
        String sslKey = plugin.getConfig().getString("settings.storage.postgresql.sslkey", "");

        if (!sslRootCert.trim().isEmpty()) {
            config.addDataSourceProperty("sslrootcert", sslRootCert.trim());
        }
        if (!sslCert.trim().isEmpty()) {
            config.addDataSourceProperty("sslcert", sslCert.trim());
        }
        if (!sslKey.trim().isEmpty()) {
            config.addDataSourceProperty("sslkey", sslKey.trim());
        }
    }

    /**
     * Configures the H2 data source.
     *
     * @param config the HikariConfig to configure.
     */
    private void configureH2(HikariConfig config) {
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File file = new File(dataDir, "graves.data");
        String filePath = file.getAbsolutePath();

        String username = plugin.getConfig().getString("settings.storage.h2.username", "sa");
        String password = plugin.getConfig().getString("settings.storage.h2.password", "");
        long maxLifetime = plugin.getConfig().getLong("settings.storage.h2.maxLifetime", 1800000);
        int maxConnections = plugin.getConfig().getInt("settings.storage.h2.maxConnections", 50); // Increased pool size
        long connectionTimeout = plugin.getConfig().getLong("settings.storage.h2.connectionTimeout", 30000);

        String jdbcUrl = "jdbc:h2:file:" + filePath + ";AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1;";

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.ranull.graves.libraries.h2.Driver");
        config.setPoolName("Graves H2");

        config.setMaximumPoolSize(maxConnections);
        config.setMinimumIdle(Math.min(2, maxConnections));
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(maxLifetime);

        try {
            config.setKeepaliveTime(300000);
            config.setValidationTimeout(5000);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Configures the HikariConfig for MySQL or MariaDB.
     *
     * @param config the HikariConfig to configure.
     * @param type   the type of database (MYSQL or MARIADB).
     */
    private void configureMySQLOrMariaDB(HikariConfig config, Type type) {
        String host = plugin.getConfig().getString("settings.storage.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("settings.storage.mysql.port", 3306);
        String user = plugin.getConfig().getString("settings.storage.mysql.username", "username");
        String password = plugin.getConfig().getString("settings.storage.mysql.password", "password");
        String database = plugin.getConfig().getString("settings.storage.mysql.database", "graves");
        long maxLifetime = plugin.getConfig().getLong("settings.storage.mysql.maxLifetime", 1800000);
        int maxConnections = plugin.getConfig().getInt("settings.storage.mysql.maxConnections", 20);
        long connectionTimeout = plugin.getConfig().getLong("settings.storage.mysql.connectionTimeout", 30000);
        boolean useSSL = plugin.getConfig().getBoolean("settings.storage.mysql.useSSL", true);
        boolean allowPublicKeyRetrieval = plugin.getConfig().getBoolean("settings.storage.mysql.allowPublicKeyRetrieval", false);
        boolean verifyServerCertificate = plugin.getConfig().getBoolean("settings.storage.mysql.verifyServerCertificate", false);

        String jdbcUrl = (type == Type.MARIADB)
                ? String.format("jdbc:mariadb://%s:%d/%s", host, port, database)
                : String.format("jdbc:mysql://%s:%d/%s", host, port, database);

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName(
                type == Type.MARIADB
                        ? "com.ranull.graves.libraries.mariadb.jdbc.Driver"
                        : "com.ranull.graves.libraries.mysql.cj.jdbc.Driver"
        );

        config.setMaximumPoolSize(maxConnections);
        config.setMinimumIdle(Math.min(2, maxConnections));
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(maxLifetime);

        try {
            config.setKeepaliveTime(300000);
            config.setValidationTimeout(5000);
        } catch (Throwable ignored) {
        }

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
    }

    /**
     * Configures the HikariConfig for Microsoft SQL Server (MSSQL).
     *
     * @param config the HikariConfig to configure.
     */
    private void configureMSSQL(HikariConfig config) {
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

        String jdbcUrl = String.format(
                "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%s;trustServerCertificate=%s;applicationName=Graves;sendStringParametersAsUnicode=false",
                host, port, database, String.valueOf(encrypt), String.valueOf(trustServerCertificate)
        );

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.ranull.graves.libraries.microsoft.sqlserver.jdbc.SQLServerDriver");
        config.setPoolName("Graves MSSQL");

        config.setMaximumPoolSize(maxConnections);
        config.setMinimumIdle(Math.min(2, maxConnections));
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(maxLifetime);

        try {
            config.setKeepaliveTime(300000);
            config.setValidationTimeout(5000);
        } catch (Throwable ignored) {
        }

        config.addDataSourceProperty("encrypt", String.valueOf(encrypt));
        config.addDataSourceProperty("trustServerCertificate", String.valueOf(trustServerCertificate));
        config.addDataSourceProperty("applicationName", "Graves");
        config.addDataSourceProperty("sendStringParametersAsUnicode", "false");
    }


    /**
     * Migrates root data to a sub-data directory.
     */
    private void migrateRootDataSubData() {
        Path root = plugin.getDataFolder().toPath();
        Path dataDir = root.resolve("data");

        try {
            Files.createDirectories(dataDir);

            try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(root, "data.db*")) {
                for (Path src : stream) {
                    if (Files.isRegularFile(src)) {
                        Path dest = dataDir.resolve(src.getFileName());
                        try {
                            Files.move(
                                    src, dest,
                                    StandardCopyOption.REPLACE_EXISTING,
                                    StandardCopyOption.ATOMIC_MOVE
                            );
                        } catch (AtomicMoveNotSupportedException e) {
                            Files.move(
                                    src, dest,
                                    StandardCopyOption.REPLACE_EXISTING
                            );
                        }
                    }
                }
            }
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Failed to migrate SQLite files to /data: " + e.getMessage());
            plugin.logStackTrace(e);
        }
    }

    /**
     * Checks if chunk data exists for a specified location.
     *
     * @param location the location to check.
     * @return true if chunk data exists, false otherwise.
     */
    public boolean hasChunkData(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        boolean isFolia = plugin.getVersionManager().isFolia();

        String key;
        if (isFolia) {
            int cx = location.getBlockX() >> 4;
            int cz = location.getBlockZ() >> 4;
            key = location.getWorld().getName() + ":" + cx + "," + cz;
        } else {
            key = LocationUtil.chunkToString(location);
        }

        return plugin.getCacheManager().getChunkMap().containsKey(key);
    }

    /**
     * Retrieves chunk data for a specified location.
     *
     * @param location the location to retrieve chunk data for.
     * @return the chunk data.
     */
    public ChunkData getChunkData(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        boolean isFolia = plugin.getVersionManager().isFolia();

        String chunkString;
        int cx = location.getBlockX() >> 4;
        int cz = location.getBlockZ() >> 4;

        if (isFolia) {
            chunkString = location.getWorld().getName() + ":" + cx + "," + cz;
        } else {
            chunkString = LocationUtil.chunkToString(location);
        }

        return plugin.getCacheManager().getChunkMap().computeIfAbsent(
                chunkString,
                k -> isFolia
                        ? new ChunkData(location.getWorld().getName(), cx, cz)
                        : new ChunkData(location)
        );
    }

    /**
     * Removes chunk data.
     *
     * @param chunkData the chunk data to remove.
     */
    public void removeChunkData(ChunkData chunkData) {
        if (chunkData == null) {
            return;
        }

        boolean isFolia = plugin.getVersionManager().isFolia();

        String key;
        if (isFolia) {
            String worldName = chunkData.getWorld() != null ? chunkData.getWorld().getName() : null;
            if (worldName == null) {
                return;
            }
            key = worldName + ":" + chunkData.getX() + "," + chunkData.getZ();
        } else {
            Location loc = chunkData.getLocation();
            if (loc == null && chunkData.getWorld() != null) {
                int bx = chunkData.getX() << 4;
                int bz = chunkData.getZ() << 4;
                loc = new Location(chunkData.getWorld(), bx, 0, bz);
            }
            if (loc == null) {
                return;
            }
            key = LocationUtil.chunkToString(loc);
        }

        plugin.getCacheManager().getChunkMap().remove(key);
    }

    /**
     * Retrieves a list of columns for a specified table.
     *
     * @param tableName the table name.
     * @return the list of columns.
     */
    public List<String> getColumnList(String tableName) {
        List<String> columnList = new ArrayList<>();

        String query = switch (type) {
            case MYSQL, MARIADB -> "DESCRIBE " + tableName + ";";
            case SQLITE -> "PRAGMA table_info(" + tableName + ");";
            case POSTGRESQL -> "SELECT column_name FROM information_schema.columns WHERE table_name = '" + tableName + "';";
            case MSSQL -> "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + tableName + "' AND TABLE_SCHEMA = 'dbo';";
            case H2 -> "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + tableName + "';";
            default -> {
                plugin.getLogger().severe("Unsupported database type: " + type);
                yield null;
            }
        };
        if (query == null) {
            return columnList;
        }

        try (Connection connection = getConnection()) {
            if (connection == null) {
                return columnList;
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {

                while (resultSet.next()) {
                    String columnName = switch (type) {
                        case MYSQL, MARIADB -> resultSet.getString("Field");
                        case SQLITE -> resultSet.getString("name");
                        default -> resultSet.getString("COLUMN_NAME");
                    };
                    columnList.add(columnName);
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Error occurred while getting Column List");
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
        String query = switch (type) {
            case MYSQL, MARIADB ->
                    "SHOW TABLES LIKE '" + tableName + "';";
            case SQLITE ->
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='" + getStoragePrefix() + tableName + "';";
            case POSTGRESQL ->
                    "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = '" + getStoragePrefix() + tableName + "');";
            case H2 ->
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + getStoragePrefix() + tableName + "';";
            case MSSQL ->
                    "IF OBJECT_ID('" + getStoragePrefix() + tableName + "', 'U') IS NOT NULL SELECT 1 AS TableExists ELSE SELECT 0 AS TableExists;";
            default -> {
                plugin.getLogger().severe("Unsupported database type: " + type);
                yield null;
            }
        };

        if (query == null) {
            return false;
        }

        try (Connection connection = getConnection()) {
            if (connection == null) {
                return false;
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {

                if (!resultSet.next()) {
                    return false;
                }

                return switch (type) {
                    case POSTGRESQL -> resultSet.getBoolean(1);
                    case H2 -> resultSet.getInt(1) > 0;
                    case MSSQL -> resultSet.getInt("TableExists") == 1;
                    case MYSQL, MARIADB, SQLITE -> true;
                    default -> false;
                };
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Error occurred while checking if table exists");
            plugin.getLogger().severe("Query: " + query);
            plugin.logStackTrace(exception);
            return false;
        }
    }

    /**
     * Adds a column to a table if it does not exist.
     *
     * @param tableName        the table name.
     * @param columnName       the column name.
     * @param columnDefinition the column definition.
     * @throws SQLException if an SQL error occurs.
     */
    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) throws SQLException {
        List<String> columnList = getColumnList(tableName);

        if (columnList.contains(columnName)) {
            return;
        }

        String query = switch (type) {
            case MYSQL, MARIADB, SQLITE ->
                    "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition + ";";
            case POSTGRESQL ->
                    "DO $$ BEGIN "
                            + "IF NOT EXISTS (SELECT 1 FROM information_schema.columns "
                            + "WHERE table_name = '" + getStoragePrefix() + tableName + "' "
                            + "AND column_name = '" + columnName + "') THEN "
                            + "ALTER TABLE " + getStoragePrefix() + tableName + " ADD COLUMN " + columnName + " " + columnDefinition + "; "
                            + "END IF; "
                            + "END $$;";
            case H2 ->
                    "ALTER TABLE " + getStoragePrefix() + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " " + columnDefinition + ";";
            case MSSQL ->
                    "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS "
                            + "WHERE TABLE_NAME = '" + getStoragePrefix() + tableName + "' "
                            + "AND COLUMN_NAME = '" + columnName + "') "
                            + "BEGIN "
                            + "ALTER TABLE " + getStoragePrefix() + tableName + " ADD " + columnName + " " + columnDefinition + ";"
                            + "END;";
            default -> {
                plugin.getLogger().severe("Unsupported database type: " + type);
                yield null;
            }
        };

        if (query == null) {
            return;
        }

        executeUpdate(query, new Object[0]);
    }

    /**
     * Alters an existing column’s definition if the column already exists in the given table.
     *
     * @param tableName        the name of the table to modify
     * @param columnName       the name of the column to alter
     * @param columnDefinition the SQL definition to apply (type, length, etc.)
     * @throws SQLException if an SQL error occurs during the ALTER TABLE operation
     */
    private void alterColumnIfExists(String tableName, String columnName, String columnDefinition) throws SQLException {
        List<String> columnList = getColumnList(tableName);
        if (!columnList.contains(columnName)) {
            return;
        }

        String query = switch (type) {
            case MYSQL, MARIADB ->
                    "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + columnDefinition + ";";
            case POSTGRESQL ->
                    "ALTER TABLE " + getStoragePrefix() + tableName + " ALTER COLUMN " + columnName + " TYPE " + columnDefinition + ";";
            case H2, MSSQL ->
                    "ALTER TABLE " + getStoragePrefix() + tableName + " ALTER COLUMN " + columnName + " " + columnDefinition + ";";
            case SQLITE -> {
                plugin.debugMessage(
                        "SQLite does not support altering columns. Skipping alteration of column '"
                                + columnName + "' in table '" + getStoragePrefix() + tableName + "'.", 2);
                yield null;
            }
            default -> {
                plugin.getLogger().severe("Unsupported database type: " + type);
                yield null;
            }
        };

        if (query == null) {
            return;
        }

        executeUpdate(query, new Object[0]);
    }

    /**
     * Sets up the grave table in the database.
     *
     * @throws SQLException if an SQL error occurs.
     */
    public void setupGraveTable() throws SQLException {
        String name = getStoragePrefix() + "grave";
        if (!tableExists(name)) {
            String create = switch (type) {
                case H2, POSTGRESQL ->
                        "CREATE TABLE IF NOT EXISTS " + name + " ("
                                + "uuid VARCHAR(255) UNIQUE,"
                                + "owner_type VARCHAR(255),"
                                + "owner_name VARCHAR(255),"
                                + "owner_name_display VARCHAR(255),"
                                + "owner_uuid VARCHAR(255),"
                                + "owner_texture TEXT,"
                                + "owner_texture_signature TEXT,"
                                + "killer_type VARCHAR(255),"
                                + "killer_name VARCHAR(255),"
                                + "killer_name_display VARCHAR(255),"
                                + "killer_uuid VARCHAR(255),"
                                + "location_death VARCHAR(255),"
                                + "yaw REAL,"
                                + "pitch REAL,"
                                + "inventory TEXT,"
                                + "equipment TEXT,"
                                + "experience INT,"
                                + "protection INT,"
                                + "is_abandoned INT,"
                                + "time_alive BIGINT,"
                                + "time_protection BIGINT,"
                                + "time_creation BIGINT,"
                                + "permissions TEXT"
                                + ");";
                case MSSQL ->
                        "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = '" + name + "')"
                                + " CREATE TABLE " + name + " ("
                                + "uuid NVARCHAR(255) UNIQUE,"
                                + "owner_type NVARCHAR(255),"
                                + "owner_name NVARCHAR(255),"
                                + "owner_name_display NVARCHAR(255),"
                                + "owner_uuid NVARCHAR(255),"
                                + "owner_texture NVARCHAR(MAX),"
                                + "owner_texture_signature NVARCHAR(MAX),"
                                + "killer_type NVARCHAR(255),"
                                + "killer_name NVARCHAR(255),"
                                + "killer_name_display NVARCHAR(255),"
                                + "killer_uuid NVARCHAR(255),"
                                + "location_death NVARCHAR(255),"
                                + "yaw FLOAT,"
                                + "pitch FLOAT,"
                                + "inventory NVARCHAR(MAX),"
                                + "equipment NVARCHAR(MAX),"
                                + "experience INT,"
                                + "protection BIT,"
                                + "is_abandoned BIT,"
                                + "time_alive BIGINT,"
                                + "time_protection BIGINT,"
                                + "time_creation BIGINT,"
                                + "permissions NVARCHAR(MAX)"
                                + ");";
                default ->
                        "CREATE TABLE IF NOT EXISTS " + name + " ("
                                + "uuid VARCHAR(255) UNIQUE,"
                                + "owner_type VARCHAR(255),"
                                + "owner_name VARCHAR(255),"
                                + "owner_name_display VARCHAR(255),"
                                + "owner_uuid VARCHAR(255),"
                                + "owner_texture LONGTEXT,"
                                + "owner_texture_signature LONGTEXT,"
                                + "killer_type VARCHAR(255),"
                                + "killer_name VARCHAR(255),"
                                + "killer_name_display VARCHAR(255),"
                                + "killer_uuid VARCHAR(255),"
                                + "location_death VARCHAR(255),"
                                + "yaw FLOAT(16),"
                                + "pitch FLOAT(16),"
                                + "inventory LONGTEXT,"
                                + "equipment LONGTEXT,"
                                + "experience INT(16),"
                                + "protection INT(1),"
                                + "is_abandoned INT(1),"
                                + "time_alive BIGINT,"
                                + "time_protection BIGINT,"
                                + "time_creation BIGINT,"
                                + "permissions LONGTEXT"
                                + ");";
            };
            executeUpdate(create, new Object[0]);
        }

        addColumnIfNotExists(name, "uuid", "VARCHAR(255) UNIQUE");
        alterColumnIfExists(name, "uuid", "VARCHAR(255) UNIQUE");

        addColumnIfNotExists(name, "owner_type", "VARCHAR(255)");
        alterColumnIfExists(name, "owner_type", "VARCHAR(255)");

        addColumnIfNotExists(name, "owner_name", "VARCHAR(255)");
        alterColumnIfExists(name, "owner_name", "VARCHAR(255)");

        addColumnIfNotExists(name, "owner_name_display", "VARCHAR(255)");
        alterColumnIfExists(name, "owner_name_display", "VARCHAR(255)");

        addColumnIfNotExists(name, "owner_uuid", "VARCHAR(255)");
        alterColumnIfExists(name, "owner_uuid", "VARCHAR(255)");

        switch (type) {
            case H2, POSTGRESQL -> {
                addColumnIfNotExists(name, "owner_texture", "TEXT");
                alterColumnIfExists(name, "owner_texture", "TEXT");
                addColumnIfNotExists(name, "owner_texture_signature", "TEXT");
                alterColumnIfExists(name, "owner_texture_signature", "TEXT");
            }
            case MSSQL -> {
                addColumnIfNotExists(name, "owner_texture", "NVARCHAR(MAX)");
                alterColumnIfExists(name, "owner_texture", "NVARCHAR(MAX)");
                addColumnIfNotExists(name, "owner_texture_signature", "NVARCHAR(MAX)");
                alterColumnIfExists(name, "owner_texture_signature", "NVARCHAR(MAX)");
            }
            default -> {
                addColumnIfNotExists(name, "owner_texture", "LONGTEXT");
                alterColumnIfExists(name, "owner_texture", "LONGTEXT");
                addColumnIfNotExists(name, "owner_texture_signature", "LONGTEXT");
                alterColumnIfExists(name, "owner_texture_signature", "LONGTEXT");
            }
        }

        addColumnIfNotExists(name, "killer_type", "VARCHAR(255)");
        alterColumnIfExists(name, "killer_type", "VARCHAR(255)");

        addColumnIfNotExists(name, "killer_name", "VARCHAR(255)");
        alterColumnIfExists(name, "killer_name", "VARCHAR(255)");

        addColumnIfNotExists(name, "killer_name_display", "VARCHAR(255)");
        alterColumnIfExists(name, "killer_name_display", "VARCHAR(255)");

        addColumnIfNotExists(name, "killer_uuid", "VARCHAR(255)");
        alterColumnIfExists(name, "killer_uuid", "VARCHAR(255)");

        addColumnIfNotExists(name, "location_death", "VARCHAR(255)");
        alterColumnIfExists(name, "location_death", "VARCHAR(255)");

        switch (type) {
            case H2, POSTGRESQL -> {
                addColumnIfNotExists(name, "yaw", "REAL");
                alterColumnIfExists(name, "yaw", "REAL");
                addColumnIfNotExists(name, "pitch", "REAL");
                alterColumnIfExists(name, "pitch", "REAL");
            }
            case MSSQL -> {
                addColumnIfNotExists(name, "yaw", "FLOAT");
                alterColumnIfExists(name, "yaw", "FLOAT");
                addColumnIfNotExists(name, "pitch", "FLOAT");
                alterColumnIfExists(name, "pitch", "FLOAT");
            }
            default -> {
                addColumnIfNotExists(name, "yaw", "FLOAT(16)");
                alterColumnIfExists(name, "yaw", "FLOAT(16)");
                addColumnIfNotExists(name, "pitch", "FLOAT(16)");
                alterColumnIfExists(name, "pitch", "FLOAT(16)");
            }
        }

        switch (type) {
            case H2, POSTGRESQL -> {
                addColumnIfNotExists(name, "inventory", "TEXT");
                alterColumnIfExists(name, "inventory", "TEXT");
                addColumnIfNotExists(name, "equipment", "TEXT");
                alterColumnIfExists(name, "equipment", "TEXT");
                addColumnIfNotExists(name, "experience", "INT");
                alterColumnIfExists(name, "experience", "INT");
                addColumnIfNotExists(name, "protection", "INT");
                alterColumnIfExists(name, "protection", "INT");
                addColumnIfNotExists(name, "is_abandoned", "INT");
                alterColumnIfExists(name, "is_abandoned", "INT");
            }
            case MSSQL -> {
                addColumnIfNotExists(name, "inventory", "NVARCHAR(MAX)");
                alterColumnIfExists(name, "inventory", "NVARCHAR(MAX)");
                addColumnIfNotExists(name, "equipment", "NVARCHAR(MAX)");
                alterColumnIfExists(name, "equipment", "NVARCHAR(MAX)");
                addColumnIfNotExists(name, "experience", "INT");
                alterColumnIfExists(name, "experience", "INT");
                addColumnIfNotExists(name, "protection", "BIT");
                alterColumnIfExists(name, "protection", "BIT");
                addColumnIfNotExists(name, "is_abandoned", "BIT");
                alterColumnIfExists(name, "is_abandoned", "BIT");
            }
            default -> {
                addColumnIfNotExists(name, "inventory", "LONGTEXT");
                alterColumnIfExists(name, "inventory", "LONGTEXT");
                addColumnIfNotExists(name, "equipment", "LONGTEXT");
                alterColumnIfExists(name, "equipment", "LONGTEXT");
                addColumnIfNotExists(name, "experience", "INT(16)");
                alterColumnIfExists(name, "experience", "INT(16)");
                addColumnIfNotExists(name, "protection", "INT(1)");
                alterColumnIfExists(name, "protection", "INT(1)");
                addColumnIfNotExists(name, "is_abandoned", "INT(1)");
                alterColumnIfExists(name, "is_abandoned", "INT(1)");
            }
        }

        addColumnIfNotExists(name, "time_alive", "BIGINT");
        alterColumnIfExists(name, "time_alive", "BIGINT");
        addColumnIfNotExists(name, "time_protection", "BIGINT");
        alterColumnIfExists(name, "time_protection", "BIGINT");
        addColumnIfNotExists(name, "time_creation", "BIGINT");
        alterColumnIfExists(name, "time_creation", "BIGINT");

        switch (type) {
            case H2, POSTGRESQL -> {
                addColumnIfNotExists(name, "permissions", "TEXT");
                alterColumnIfExists(name, "permissions", "TEXT");
            }
            case MSSQL -> {
                addColumnIfNotExists(name, "permissions", "NVARCHAR(MAX)");
                alterColumnIfExists(name, "permissions", "NVARCHAR(MAX)");
            }
            default -> {
                addColumnIfNotExists(name, "permissions", "LONGTEXT");
                alterColumnIfExists(name, "permissions", "LONGTEXT");
            }
        }
    }

    /**
     * Sets up the block table in the database.
     *
     * @throws SQLException if an SQL error occurs.
     */
    public void setupBlockTable() throws SQLException {
        String name = getStoragePrefix() + "block";

        if (!tableExists(name)) {
            String replaceDataType = switch (type) {
                case MSSQL -> "NVARCHAR(MAX)";
                default -> "TEXT";
            };

            String createTableQuery =
                    "CREATE TABLE IF NOT EXISTS " + name + " ("
                            + "location VARCHAR(255),"
                            + "uuid_grave VARCHAR(255),"
                            + "replace_material VARCHAR(255),"
                            + "replace_data " + replaceDataType
                            + ");";

            executeUpdate(createTableQuery, new Object[0]);
        }

        addColumnIfNotExists(name, "location", "VARCHAR(255)");
        alterColumnIfExists(name, "location", "VARCHAR(255)");

        addColumnIfNotExists(name, "uuid_grave", "VARCHAR(255)");
        alterColumnIfExists(name, "uuid_grave", "VARCHAR(255)");

        addColumnIfNotExists(name, "replace_material", "VARCHAR(255)");
        alterColumnIfExists(name, "replace_material", "VARCHAR(255)");

        String replaceDataTypeForAlter = switch (type) {
            case MSSQL -> "NVARCHAR(MAX)";
            default -> "TEXT";
        };
        addColumnIfNotExists(name, "replace_data", replaceDataTypeForAlter);
        alterColumnIfExists(name, "replace_data", replaceDataTypeForAlter);
    }


    /**
     * Sets up the hologram table in the database.
     *
     * @throws SQLException if an SQL error occurs.
     */
    public void setupHologramTable() throws SQLException {
        String name = getStoragePrefix() + "hologram";

        if (!tableExists(name)) {
            String createTableQuery = switch (type) {
                case MYSQL, MARIADB ->
                        "CREATE TABLE IF NOT EXISTS " + name + " ("
                                + "uuid_entity VARCHAR(255),"
                                + "uuid_grave VARCHAR(255),"
                                + "line INT(16),"
                                + "location VARCHAR(255)"
                                + ");";
                case SQLITE, POSTGRESQL, H2 ->
                        "CREATE TABLE IF NOT EXISTS " + name + " ("
                                + "uuid_entity VARCHAR(255),"
                                + "uuid_grave VARCHAR(255),"
                                + "line INTEGER,"
                                + "location VARCHAR(255)"
                                + ");";
                case MSSQL ->
                        "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = '" + name + "')"
                                + " CREATE TABLE " + name + " ("
                                + "uuid_entity NVARCHAR(255),"
                                + "uuid_grave NVARCHAR(255),"
                                + "line INT,"
                                + "location NVARCHAR(255)"
                                + ");";
                default -> {
                    plugin.getLogger().severe("Unsupported database type: " + type);
                    yield null;
                }
            };

            if (createTableQuery == null) {
                return;
            }
            executeUpdate(createTableQuery, new Object[0]);
        }

        String varcharDef = switch (type) {
            case MSSQL -> "NVARCHAR(255)";
            default -> "VARCHAR(255)";
        };

        addColumnIfNotExists(name, "uuid_entity", varcharDef);
        alterColumnIfExists(name, "uuid_entity", varcharDef);

        addColumnIfNotExists(name, "uuid_grave", varcharDef);
        alterColumnIfExists(name, "uuid_grave", varcharDef);

        String lineType = switch (type) {
            case MYSQL, MARIADB -> "INT(16)";
            case MSSQL -> "INT";
            default -> "INTEGER";
        };
        addColumnIfNotExists(name, "line", lineType);
        alterColumnIfExists(name, "line", lineType);

        addColumnIfNotExists(name, "location", varcharDef);
        alterColumnIfExists(name, "location", varcharDef);
    }

    /**
     * Sets up an entity table in the database.
     *
     * @param name the name of the table.
     * @throws SQLException if an SQL error occurs.
     */
    private void setupEntityTable(String name) throws SQLException {
        String table = getStoragePrefix() + name;

        if (!tableExists(table)) {
            String createTableQuery = switch (type) {
                case MYSQL, MARIADB, POSTGRESQL, H2 ->
                        "CREATE TABLE IF NOT EXISTS " + table + " ("
                                + "location VARCHAR(255), "
                                + "uuid_entity VARCHAR(255), "
                                + "uuid_grave VARCHAR(255)"
                                + ");";
                case SQLITE ->
                        "CREATE TABLE IF NOT EXISTS " + table + " ("
                                + "location TEXT, "
                                + "uuid_entity TEXT, "
                                + "uuid_grave TEXT"
                                + ");";
                case MSSQL ->
                        "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = '" + table + "')"
                                + " CREATE TABLE " + table + " ("
                                + "location NVARCHAR(255), "
                                + "uuid_entity NVARCHAR(255), "
                                + "uuid_grave NVARCHAR(255)"
                                + ");";
                default -> {
                    plugin.getLogger().severe("Unsupported database type: " + type);
                    yield null;
                }
            };

            if (createTableQuery == null) {
                return;
            }
            executeUpdate(createTableQuery, new Object[0]);
        }

        String varcharDef = switch (type) {
            case MSSQL -> "NVARCHAR(255)";
            case SQLITE -> "TEXT";
            default -> "VARCHAR(255)";
        };

        addColumnIfNotExists(table, "location", varcharDef);
        alterColumnIfExists(table, "location", varcharDef);

        addColumnIfNotExists(table, "uuid_entity", varcharDef);
        alterColumnIfExists(table, "uuid_entity", varcharDef);

        addColumnIfNotExists(table, "uuid_grave", varcharDef);
        alterColumnIfExists(table, "uuid_grave", varcharDef);
    }

    /**
     * Loads the grave map from the database.
     */
    public void loadGraveMap() {
        plugin.getCacheManager().getGraveMap().clear();
        plugin.getLogger().info("Loading grave maps...");
        String query = "SELECT * FROM " + getStoragePrefix() + "grave;";
        int graveCount = 0;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                Grave grave = resultSetToGrave(resultSet);
                if (grave != null) {
                    plugin.getCacheManager().getGraveMap().put(grave.getUUID(), grave);
                    graveCount++;
                }
            }

            if (graveCount == 0) {
                plugin.getLogger().info("Found 0 grave maps to load into cache.");
            } else {
                plugin.getLogger().info("Loaded " + graveCount + " grave maps into cache.");
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            String message = exception.getMessage() != null ? exception.getMessage().toLowerCase(java.util.Locale.ROOT) : "";

            if ("42701".equals(sqlState)
                    || "42P07".equals(sqlState)
                    || "42S01".equals(sqlState)
                    || "42S04".equals(sqlState)
                    || "X0Y32".equals(sqlState)
                    || "42000".equals(sqlState)
                    || ("SQLITE_ERROR".equals(sqlState) && message.contains("duplicate column name"))) {
                plugin.getLogger().info("Found 0 grave maps to load into cache.");
            } else {
                plugin.getLogger().severe("Error occurred while loading Grave Map");
                plugin.logStackTrace(exception);
            }
        } catch (NullPointerException exception) {
            plugin.getLogger().severe("A null pointer exception occurred while loading Grave Map");
            plugin.logStackTrace(exception);
        }
    }

    /**
     * Loads the block map from the database (Folia-safe).
     * - Reads DB asynchronously.
     * - Batches rows per chunk and applies them on the chunk's region thread
     *   using plugin.getSchedulerManager().execute(anchorLocation, ...).
     */
    public void loadBlockMap() {
        String query = "SELECT * FROM " + getStoragePrefix() + "block;";

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            plugin.getLogger().info("Loading Block Map cache...");

            class BlockWork {
                Location loc;
                BlockData data;
                BlockWork(Location loc, BlockData data) { this.loc = loc; this.data = data; }
            }
            Map<String, List<BlockWork>> byChunk = new HashMap<>();
            int scheduledCount = 0;

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    try {
                        Location location = LocationUtil.stringToLocation(resultSet.getString("location"));
                        UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));
                        String replaceMaterial = resultSet.getString("replace_material");
                        String replaceData = resultSet.getString("replace_data");

                        if (location.getWorld() == null) {
                            plugin.getLogger().warning("Skipping block entry with invalid/missing location for Grave " + uuidGrave);
                            continue;
                        }

                        BlockData bd;
                        if (replaceMaterial != null && replaceData != null) {
                            bd = new BlockData(location, uuidGrave, replaceMaterial, replaceData);
                        } else {
                            bd = new BlockData(location, uuidGrave, "AIR", "minecraft:air");
                        }

                        int cx = location.getBlockX() >> 4;
                        int cz = location.getBlockZ() >> 4;
                        String key = location.getWorld().getUID() + ":" + cx + ":" + cz;

                        byChunk.computeIfAbsent(key, k -> new java.util.ArrayList<>())
                                .add(new BlockWork(location, bd));

                        scheduledCount++;
                    } catch (Exception e) {
                        String uuidGraveStr = resultSet.getString("uuid_grave");
                        plugin.getLogger().warning("Failed to process a block entry for Grave " + uuidGraveStr);
                        plugin.logStackTrace(e);
                    }
                }

                for (List<BlockWork> group : byChunk.values()) {
                    if (group.isEmpty()) continue;

                    Location anchor = group.get(0).loc.clone();

                    plugin.getSchedulerManager().execute(anchor, () -> {
                        for (BlockWork w : group) {
                            try {
                                getChunkData(w.loc).addBlockData(w.data);
                            } catch (Throwable t) {
                                plugin.getLogger().warning("Failed to cache block at " + w.loc + ": " + t.getMessage());
                            }
                        }
                    });
                }

                plugin.getLogger().info("Queued " + scheduledCount + " Blocks into the Block Map Cache (batched by chunk).");

            } catch (SQLException exception) {
                String sqlState = exception.getSQLState();
                String message = exception.getMessage() != null
                        ? exception.getMessage().toLowerCase(java.util.Locale.ROOT)
                        : "";

                if ("42701".equals(sqlState)
                        || "42P07".equals(sqlState)
                        || "42S01".equals(sqlState)
                        || "42S04".equals(sqlState)
                        || "X0Y32".equals(sqlState)
                        || "42000".equals(sqlState)
                        || ("SQLITE_ERROR".equals(sqlState) && message.contains("duplicate column name"))) {
                    plugin.getLogger().info("Loaded 0 Blocks into Block Map Cache.");
                } else {
                    plugin.getLogger().severe("Error occurred while loading Block Map");
                    plugin.logStackTrace(exception);
                }
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
        String query = "SELECT * FROM " + getStoragePrefix() + table + ";";

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            plugin.getLogger().info("Loading Entity Map Cache for " + table + "...");
            int entityCount = 0;

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    String locationString = resultSet.getString("location");
                    if (locationString == null) {
                        plugin.getLogger().warning("EntityMap row skipped: location is null (table=" + table + ").");
                        continue;
                    }

                    Location location = LocationUtil.stringToLocation(locationString);
                    if (location.getWorld() == null) {
                        plugin.getLogger().warning("EntityMap row skipped: invalid location '" + locationString + "' (table=" + table + ").");
                        continue;
                    }

                    String uuidEntityString = resultSet.getString("uuid_entity");
                    String uuidGraveString = resultSet.getString("uuid_grave");
                    if (uuidEntityString == null || uuidGraveString == null) {
                        plugin.getLogger().warning("EntityMap row skipped: missing UUIDs (location=" + location + ", table=" + table + ").");
                        continue;
                    }

                    try {
                        UUID uuidEntity = UUID.fromString(uuidEntityString);
                        UUID uuidGrave = UUID.fromString(uuidGraveString);
                        getChunkData(location).addEntityData(new EntityData(location, uuidEntity, uuidGrave, type));
                        entityCount++;
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("EntityMap row skipped: malformed UUIDs (entity=" + uuidEntityString
                                + ", grave=" + uuidGraveString + ", table=" + table + ").");
                    }
                }

                if (entityCount == 0) {
                    plugin.getLogger().info("Loaded 0 entities into Entity Map Cache for " + getStoragePrefix() + table + ".");
                } else {
                    plugin.getLogger().info("Loaded " + entityCount + " entities into Entity Map Cache for " + getStoragePrefix() + table + ".");
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Error occurred while loading Entity Map for " + table);
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Loads the hologram map from the database.
     */
    public void loadHologramMap() {
        String query = "SELECT * FROM " + getStoragePrefix() + "hologram;";

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            plugin.getLogger().info("Loading Holograms into Hologram Map Cache...");
            int hologramCount = 0;

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    String locationString = resultSet.getString("location");
                    if (locationString == null) {
                        plugin.getLogger().warning("Hologram row skipped: location is null.");
                        continue;
                    }

                    Location location = LocationUtil.stringToLocation(locationString);
                    if (location.getWorld() == null) {
                        plugin.getLogger().warning("Hologram row skipped: invalid location '" + locationString + "'.");
                        continue;
                    }

                    String uuidEntityString = resultSet.getString("uuid_entity");
                    String uuidGraveString = resultSet.getString("uuid_grave");
                    if (uuidEntityString == null || uuidGraveString == null) {
                        plugin.getLogger().warning("Hologram row skipped: missing UUIDs (location=" + location + ").");
                        continue;
                    }

                    try {
                        UUID uuidEntity = UUID.fromString(uuidEntityString);
                        UUID uuidGrave = UUID.fromString(uuidGraveString);
                        int line = resultSet.getInt("line");

                        getChunkData(location).addEntityData(new HologramData(location, uuidEntity, uuidGrave, line));
                        hologramCount++;
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Hologram row skipped: malformed UUIDs (entity=" + uuidEntityString
                                + ", grave=" + uuidGraveString + ").");
                    }
                }

                if (hologramCount == 0) {
                    plugin.getLogger().info("Loaded 0 Holograms into Hologram Map Cache.");
                } else {
                    plugin.getLogger().info("Loaded " + hologramCount + " Holograms into Hologram Map Cache.");
                }

            } catch (SQLException exception) {
                plugin.getLogger().severe("Error occurred while loading Hologram Map");
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Creates the entity data map table if it does not exist.
     *
     * @param name the name of the table.
     */
    private void createEntityDataMapTable(String name) {
        String physicalTable = getStoragePrefix() + name;

        String createTableQuery = switch (type) {
            case MYSQL, MARIADB, POSTGRESQL, H2 ->
                    "CREATE TABLE IF NOT EXISTS " + physicalTable + " ("
                            + "location VARCHAR(255), "
                            + "uuid_entity VARCHAR(255), "
                            + "uuid_grave VARCHAR(255)"
                            + ");";
            case SQLITE ->
                    "CREATE TABLE IF NOT EXISTS " + physicalTable + " ("
                            + "location TEXT, "
                            + "uuid_entity TEXT, "
                            + "uuid_grave TEXT"
                            + ");";
            case MSSQL ->
                    "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='" + physicalTable + "' AND xtype='U') "
                            + "CREATE TABLE " + physicalTable + " ("
                            + "location NVARCHAR(255), "
                            + "uuid_entity NVARCHAR(255), "
                            + "uuid_grave NVARCHAR(255)"
                            + ");";
            default -> {
                plugin.getLogger().severe("Unsupported database type: " + type);
                yield null;
            }
        };

        if (createTableQuery == null) {
            return;
        }

        try {
            executeUpdate(createTableQuery, new Object[0]);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create entity data map table: " + physicalTable);
            plugin.logStackTrace(e);
        }

        String varcharDef = switch (type) {
            case MSSQL -> "NVARCHAR(255)";
            case SQLITE -> "TEXT";
            default -> "VARCHAR(255)";
        };

        try {
            addColumnIfNotExists(physicalTable, "location", varcharDef);
            addColumnIfNotExists(physicalTable, "uuid_entity", varcharDef);
            addColumnIfNotExists(physicalTable, "uuid_grave", varcharDef);
        } catch (Exception ignored) {
            // ignored
        }
    }

    /**
     * Loads entity data from the database.
     *
     * @param table the table name.
     * @param type  the type of entity data.
     */
    private void loadEntityDataMap(String table, EntityData.Type type) {
        String physicalTable = getStoragePrefix() + table;
        String query = "SELECT location, uuid_entity, uuid_grave FROM " + physicalTable + ";";

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            plugin.getLogger().info("Loading Entity Data Map Cache for " + physicalTable + "...");
            int entityCount = 0;

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    String locationString = resultSet.getString("location");
                    if (locationString == null) {
                        plugin.getLogger().warning("Entity Data row skipped: location is null (" + physicalTable + ").");
                        continue;
                    }

                    Location location = LocationUtil.stringToLocation(locationString);
                    if (location.getWorld() == null) {
                        plugin.getLogger().warning("Entity Data row skipped: invalid location '" + locationString + "' (" + physicalTable + ").");
                        continue;
                    }

                    String uuidEntityString = resultSet.getString("uuid_entity");
                    String uuidGraveString = resultSet.getString("uuid_grave");
                    if (uuidEntityString == null || uuidGraveString == null) {
                        plugin.getLogger().warning("Entity Data row skipped: missing UUIDs (location=" + location + ", table=" + physicalTable + ").");
                        continue;
                    }

                    try {
                        UUID uuidEntity = UUID.fromString(uuidEntityString);
                        UUID uuidGrave = UUID.fromString(uuidGraveString);
                        getChunkData(location).addEntityData(new EntityData(location, uuidEntity, uuidGrave, type));
                        entityCount++;
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Entity Data row skipped: malformed UUIDs (entity=" + uuidEntityString
                                + ", grave=" + uuidGraveString + ", table=" + physicalTable + ").");
                    }
                }

                if (entityCount == 0) {
                    plugin.getLogger().info("Loaded 0 entities into Entity Data Map Cache for " + physicalTable + ".");
                } else {
                    plugin.getLogger().info("Loaded " + entityCount + " entities into Entity Data Map Cache for " + physicalTable + ".");
                }
            } catch (SQLException | NullPointerException exception) {
                plugin.getLogger().severe("Error occurred while loading Entity Data Map for " + physicalTable);
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
        Location loc = blockData.getLocation();

        if (loc != null && loc.getWorld() != null
                && plugin.getVersionManager().isFolia()) {

            plugin.getSchedulerManager().execute(loc, () -> getChunkData(loc).addBlockData(blockData));
        } else {
            Objects.requireNonNull(getChunkData(loc)).addBlockData(blockData);
        }

        String query =
                "INSERT INTO " + getStoragePrefix()
                        + "block (location, uuid_grave, replace_material, replace_data) VALUES (?, ?, ?, ?)";

        Object[] parameters = new Object[] {
                LocationUtil.locationToString(blockData.getLocation()),
                blockData.getGraveUUID() != null ? blockData.getGraveUUID().toString() : null,
                blockData.getReplaceMaterial(),
                blockData.getReplaceData()
        };

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to add block data");
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Removes block data from the database.
     *
     * @param location the location of the block data to remove.
     */
    /**
     * Removes block data from the database.
     *
     * <p>Uses GravesXScheduler (UniversalScheduler) to run the in-memory cache update
     * at the block's location (Folia-safe). The database delete runs asynchronously.</p>
     *
     * @param location the location of the block data to remove.
     */
    public void removeBlockData(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        plugin.getSchedulerManager().execute(location, () -> {
            ChunkData chunkData = getChunkData(location);
            if (chunkData != null) {
                chunkData.removeBlockData(location);
            }
        });

        String query = "DELETE FROM " + getStoragePrefix() + "block WHERE location = ?";
        Object[] parameters = new Object[] { LocationUtil.locationToString(location) };

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to remove block data");
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Adds hologram data to the database.
     *
     * <p>Uses GravesXScheduler (UniversalScheduler) to execute the in-memory cache update
     * at the hologram's location (Folia-safe). The database write is performed asynchronously.</p>
     *
     * @param hologramData the hologram data to add.
     */
    public void addHologramData(HologramData hologramData) {
        Location loc = hologramData.getLocation();

        if (loc != null && loc.getWorld() != null) {
            plugin.getSchedulerManager().execute(loc, () -> getChunkData(loc).addEntityData(hologramData));
        } else {
            Objects.requireNonNull(getChunkData(loc)).addEntityData(hologramData);
        }

        String query =
                "INSERT INTO " + getStoragePrefix()
                        + "hologram (uuid_entity, uuid_grave, line, location) VALUES (?, ?, ?, ?)";

        Object[] parameters = new Object[] {
                hologramData.getUUIDEntity().toString(),
                hologramData.getUUIDGrave().toString(),
                hologramData.getLine(),
                LocationUtil.locationToString(hologramData.getLocation())
        };

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to add hologram data");
                plugin.logStackTrace(exception);
            }
        });
    }

    /**
     * Removes hologram entries from the database based on the grave UUID.
     * This is a fallback if entity data is not loaded in memory.
     *
     * @param grave the grave to remove hologram data.
     */
    public void removeHologramData(Grave grave) {
        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            String selectSql = "SELECT uuid_entity, location FROM " + getStoragePrefix() + "hologram WHERE uuid_grave = ?";
            String deleteSql = "DELETE FROM " + getStoragePrefix() + "hologram WHERE uuid_grave = ?";

            int scheduledRemovals = 0;

            try (Connection connection = plugin.getDataManager().getConnection();
                 PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {

                selectStmt.setString(1, grave.getUUID().toString());

                try (ResultSet rs = selectStmt.executeQuery()) {
                    while (rs.next()) {
                        String uuidEntityStr = rs.getString("uuid_entity");
                        String locString = rs.getString("location");

                        if (uuidEntityStr == null || locString == null) {
                            continue;
                        }

                        UUID uuidEntity;
                        try {
                            uuidEntity = UUID.fromString(uuidEntityStr);
                        } catch (IllegalArgumentException ex) {
                            continue;
                        }

                        Location location = LocationUtil.deserializeLocation(locString);
                        if (location == null || location.getWorld() == null) {
                            continue;
                        }

                        EntityData.Type type = EntityData.Type.HOLOGRAM;
                        EntityData entityData = new EntityData(location, uuidEntity, grave.getUUID(), type);

                        plugin.getSchedulerManager().execute(location, () -> {
                            ChunkData chunkData = getChunkData(location);
                            if (chunkData != null) {
                                chunkData.removeEntityData(entityData);
                            }
                        });
                        scheduledRemovals++;
                    }
                }

                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, grave.getUUID().toString());
                    deleteStmt.executeUpdate();
                }

                plugin.debugMessage("Deleted " + scheduledRemovals + " holograms from DB for grave UUID: " + grave.getUUID(), 2);

            } catch (SQLException e) {
                plugin.getLogger().severe("Error deleting holograms for grave " + grave.getUUID());
                plugin.logStackTrace(e);
            }
        });
    }

    /**
     * Adds entity data to the database.
     *
     * @param entityData the entity data to add.
     */
    public void addEntityData(EntityData entityData) {
        Location loc = entityData.getLocation();

        if (loc != null && loc.getWorld() != null) {
            plugin.getSchedulerManager().execute(loc, () -> getChunkData(loc).addEntityData(entityData));
        } else {
            Objects.requireNonNull(getChunkData(loc)).addEntityData(entityData);
        }

        String table = entityDataTypeTable(entityData.getType());
        String query = "INSERT INTO " + getStoragePrefix() + table + " (location, uuid_entity, uuid_grave) VALUES (?, ?, ?)";

        String locationString = LocationUtil.locationToString(entityData.getLocation());
        Object[] parameters = new Object[] {
                locationString,
                entityData.getUUIDEntity(),
                entityData.getUUIDGrave()
        };

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to add entity data");
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
        removeEntityData(List.of(entityData));
    }

    /**
     * Removes a list of entity data from the database.
     *
     * @param entityDataList the list of entity data to remove.
     */
    public void removeEntityData(List<EntityData> entityDataList) {
        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            try (Connection connection = getConnection()) {
                for (EntityData entityData : entityDataList) {
                    Location loc = entityData.getLocation();

                    if (loc != null && loc.getWorld() != null) {
                        plugin.getSchedulerManager().execute(loc, () -> {
                            ChunkData chunkData = getChunkData(loc);
                            if (chunkData != null) {
                                chunkData.removeEntityData(entityData);
                            }
                            plugin.getHologramManager().removeHologram(entityData);
                        });
                    } else {
                        ChunkData chunkData = getChunkData(loc);
                        if (chunkData != null) {
                            chunkData.removeEntityData(entityData);
                        }
                        plugin.getHologramManager().removeHologram(entityData);
                    }

                    String table = entityDataTypeTable(entityData.getType());
                    String query = "DELETE FROM " + getStoragePrefix() + table + " WHERE uuid_entity = ?";
                    Object[] parameters = new Object[] { entityData.getUUIDEntity() };
                    executeUpdate(query, parameters);

                    plugin.debugMessage("Removing " + getStoragePrefix() + table + " for grave " + entityData.getUUIDGrave(), 1);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to remove entity data");
                plugin.logStackTrace(e);
            }
        });
    }

    /**
     * Checks whether a grave exists at the given location.
     *
     * <p><b>Unused.</b> This method is deprecated and will be removed.</p>
     *
     * @param location the location to check
     * @return {@code true} if a grave exists at the location; {@code false} otherwise
     * @deprecated Unused API. Deprecated as of 4.9.9.1 and scheduled for removal in 4.9.11.1.
     */
    @Deprecated(since = "4.9.9.1", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.9.11.1")
    public boolean hasGraveAtLocation(Location location) {
        String query = "SELECT COUNT(*) FROM " + getStoragePrefix() + "grave WHERE location_death = ?";
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
        return switch (type) {
            case ARMOR_STAND    -> "armorstand";
            case ITEM_FRAME     -> "itemframe";
            case HOLOGRAM       -> "hologram";
            case FURNITURELIB   -> "furniturelib";
            case FURNITUREENGINE-> "furnitureengine";
            case ITEMSADDER     -> "itemsadder";
            case ORAXEN         -> "oraxen";
            case NEXO           -> "nexo";
            case PLAYERNPC      -> "playernpc";
            case MANNEQUIN      -> "mannequins";
            default             -> type.name().toLowerCase(java.util.Locale.ROOT).replace("_", "");
        };
    }

    /**
     * Adds a grave to the database.
     *
     * @param grave the grave to add.
     */
    public void addGrave(Grave grave) {
        Location deathLoc = grave.getLocationDeath();
        if (deathLoc != null && deathLoc.getWorld() != null) {
            plugin.getSchedulerManager().execute(deathLoc, () ->
                    plugin.getCacheManager().getGraveMap().put(grave.getUUID(), grave)
            );
        } else {
            plugin.getCacheManager().getGraveMap().put(grave.getUUID(), grave);
        }

        String query = "INSERT INTO " + getStoragePrefix() + "grave (uuid, owner_type, owner_name, owner_name_display, owner_uuid, owner_texture, owner_texture_signature, killer_type, killer_name, killer_name_display, killer_uuid, location_death, yaw, pitch, inventory, equipment, experience, protection, is_abandoned, time_alive, time_protection, time_creation, permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Object[] parameters = new Object[] {
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
                Base64Util.objectToBase64(
                        grave.getEquipmentMap().entrySet().stream()
                                .filter(entry -> entry.getValue() != null)
                                .collect(java.util.stream.Collectors.toMap(
                                        entry -> entry.getKey().name(),
                                        java.util.Map.Entry::getValue
                                ))
                ),
                grave.getExperience(),
                grave.getProtection() ? 1 : 0,
                grave.isAbandoned() ? 1 : 0,
                grave.getTimeAlive(),
                grave.getTimeProtection(),
                grave.getTimeCreation(),
                grave.getPermissionList() != null && !grave.getPermissionList().isEmpty()
                        ? StringUtils.join(grave.getPermissionList(), "|")
                        : null
        };

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to add grave");
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
        Grave grave = plugin.getCacheManager().getGraveMap().remove(uuid);

        if (grave != null) {
            Location deathLoc = grave.getLocationDeath();
            if (deathLoc != null && deathLoc.getWorld() != null) {
                plugin.getSchedulerManager().execute(deathLoc, () -> {
                    plugin.getHologramManager().removeHologram(grave);
                });
            } else {
                plugin.getHologramManager().removeHologram(grave);
            }

            removeHologramData(grave);
        }

        String deleteQuery = "DELETE FROM " + getStoragePrefix() + "grave WHERE uuid = ?";
        Object[] deleteParams = new Object[] { uuid };

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            try {
                plugin.debugMessage("Attempting to remove grave for UUID: " + uuid, 1);
                executeUpdate(deleteQuery, deleteParams);
                plugin.debugMessage("Grave successfully removed for UUID: " + uuid, 1);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to remove grave");
                plugin.logStackTrace(e);
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
        String query = "UPDATE " + getStoragePrefix() + "grave SET " + column + " = ? WHERE uuid = ?";
        Object[] parameters = new Object[] { integer, grave.getUUID() };

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update grave");
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
        String query = "UPDATE " + getStoragePrefix() + "grave SET " + column + " = ? WHERE uuid = ?";
        Object[] parameters = new Object[] { string, grave.getUUID() };

        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            try {
                executeUpdate(query, parameters);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update grave");
                plugin.logStackTrace(e);
            }
        });
    }

    /**
     * Updates a grave in the database using the main thread.
     *
     * @param grave  the grave to update.
     * @param column the column to update.
     * @param string the new value for the column.
     */
    @ApiStatus.Experimental
    public void updateGraveMainThread(Grave grave, String column, String string) {
        String query = "UPDATE " + getStoragePrefix() + "grave SET " + column + " = ? WHERE uuid = ?";
        Object[] parameters = new Object[] { string, grave.getUUID() };

        try {
            executeUpdateMainThread(query, parameters);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update grave over main thread");
            plugin.logStackTrace(e);
        }
    }

    /**
     * Converts a ResultSet to a Grave object.
     *
     * @param resultSet the ResultSet to convert.
     * @return the Grave object, or null if an error occurs.
     */
    public Grave resultSetToGrave(ResultSet resultSet) {
        try {
            String uuidString = resultSet.getString("uuid");
            if (uuidString == null) {
                plugin.getLogger().warning("Skipping grave at row " + resultSet.getRow() + " due to null UUID.");
                return null;
            }

            Grave grave = new Grave(UUID.fromString(uuidString));
            List<String> invalidationReason = new ArrayList<>();
            Location graveLocation = null;

            String ownerType = resultSet.getString("owner_type");
            if (ownerType == null) invalidationReason.add("owner_type");
            grave.setOwnerType(ownerType != null ? EntityType.valueOf(ownerType) : null);

            String ownerName = resultSet.getString("owner_name");
            if (ownerType != null && ownerName == null) {
                plugin.getLogger().warning("Skipping grave at row " + resultSet.getRow() + " due to null Owner Name.");
                return null;
            }
            grave.setOwnerName(ownerName != null ? ownerName.replace(" ", "_") : null);

            String ownerNameDisplay = resultSet.getString("owner_name_display");
            if (ownerNameDisplay == null) invalidationReason.add("owner_name_display");
            grave.setOwnerNameDisplay(ownerNameDisplay);

            String ownerUUID = resultSet.getString("owner_uuid");
            if (ownerUUID == null) invalidationReason.add("owner_uuid");
            grave.setOwnerUUID(ownerUUID != null ? UUID.fromString(ownerUUID) : null);

            String ownerTexture = resultSet.getString("owner_texture");
            if (ownerTexture == null) invalidationReason.add("owner_texture");
            grave.setOwnerTexture(ownerTexture);

            String ownerTextureSignature = resultSet.getString("owner_texture_signature");
            if (ownerTextureSignature == null) invalidationReason.add("owner_texture_signature");
            grave.setOwnerTextureSignature(ownerTextureSignature);

            String killerType = resultSet.getString("killer_type");
            if (killerType == null) invalidationReason.add("killer_type");
            grave.setKillerType(killerType != null ? EntityType.valueOf(killerType) : null);

            String killerName = resultSet.getString("killer_name");
            if (killerName == null) invalidationReason.add("killer_name");
            grave.setKillerName(killerName != null ? killerName.replace(" ", "_") : null);

            String killerNameDisplay = resultSet.getString("killer_name_display");
            if (killerNameDisplay == null) invalidationReason.add("killer_name_display");
            grave.setKillerNameDisplay(killerNameDisplay != null ? killerNameDisplay.replace(" ", "_") : null);

            String killerUUID = resultSet.getString("killer_uuid");
            if ("PLAYER".equalsIgnoreCase(killerType) && killerUUID == null) {
                invalidationReason.add("killer_uuid for killer_type PLAYER");
            }
            grave.setKillerUUID(killerUUID != null ? UUID.fromString(killerUUID) : null);

            String locationDeath = resultSet.getString("location_death");

            graveLocation = (locationDeath != null) ? LocationUtil.stringToLocation(locationDeath) : null;
            if (graveLocation == null) {
                plugin.getLogger().warning("Skipping grave " + uuidString + " at row " + resultSet.getRow()
                        + " because location is null.");
                return null;
            }

            World world = graveLocation.getWorld();
            if (world == null || Bukkit.getWorld(world.getName()) == null) {
                plugin.getLogger().warning("Skipping grave " + uuidString + " at row " + resultSet.getRow()
                        + " because the world '" + (world != null ? world.getName() : "null") + "' does not exist or is not loaded.");
                return null;
            }
            grave.setLocationDeath(graveLocation);

            if (resultSet.getString("equipment") == null) invalidationReason.add("equipment");
            if (resultSet.getString("equipment") != null) {
                @SuppressWarnings("unchecked")
                Map<EquipmentSlot, ItemStack> equipmentMap = (Map<EquipmentSlot, ItemStack>) Base64Util
                        .base64ToObject(resultSet.getString("equipment"));
                grave.setEquipmentMap(equipmentMap != null ? equipmentMap : new HashMap<>());
            }

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
                    StringUtil.parseString(plugin.getConfigManager().getConfigSection("gui.grave.title", grave.getOwnerType(),
                                    grave.getPermissionList())
                            .getString("gui.grave.title"), grave.getLocationDeath(), grave, plugin), plugin));

            if (!invalidationReason.isEmpty()) {
                plugin.logInvalidGraveSite(uuidString, graveLocation, invalidationReason);
            }

            return grave;
        } catch (SQLException exception) {
            plugin.getLogger().severe("Error occurred while converting a ResultSet to a Grave object");
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
     * @return the database connection, or {@code null} if unavailable.
     */
    private Connection getConnection() {
        if (dataSource == null || dataSource.isClosed()) {
            plugin.getLogger().severe("DataSource is not initialized or has been closed");
            return null;
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Error obtaining database connection");
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
        String version = getDatabaseVersion();

        return switch (type) {
            case MYSQL -> Map.of("MySQL", Map.of(version, 1));
            case MARIADB -> Map.of("MariaDB", Map.of(version, 1));
            case POSTGRESQL -> Map.of("PostgreSQL", Map.of(version, 1));
            case MSSQL -> Map.of("Microsoft SQL Server", Map.of(version, 1));
            default -> Map.of("Other", Map.of("Unknown", 1));
        };
    }

    /**
     * Retrieves the version of the database connection type.
     *
     * @return the database version as a string.
     */
    public String getDatabaseVersion() {
        String query = switch (type) {
            case POSTGRESQL, MARIADB, MYSQL -> "SELECT version()";
            default -> null;
        };
        if (query == null) {
            return "Unknown";
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            if (resultSet.next()) {
                String version = resultSet.getString(1);
                return (version != null && !version.isEmpty()) ? version.split(",")[0] : "Unknown";
            }
            return "Unknown";
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
     * Executes an update SQL statement with parameters.
     *
     * @param sql        the SQL statement.
     * @param parameters the parameters for the SQL statement.
     * @throws SQLException if a database access error occurs.
     */
    private void executeUpdate(String sql, Object[] parameters) throws SQLException {
        plugin.getSchedulerManager().runTaskAsynchronously(() -> {
            try (Connection connection = getConnection()) {
                if (connection == null) {
                    plugin.getLogger().severe("Error executing SQL update: connection is null");
                    plugin.getLogger().severe("Failed SQL statement: " + sql);
                    return;
                }

                try (PreparedStatement statement = connection.prepareStatement(sql)) {

                    if (parameters != null) {
                        for (int i = 0; i < parameters.length; i++) {
                            Object parameter = parameters[i];
                            int idx = i + 1;

                            if (parameter == null) {
                                statement.setNull(idx, Types.VARCHAR); // adjust per expected type as needed
                            } else if (parameter instanceof String s) {
                                statement.setString(idx, s);
                            } else if (parameter instanceof Integer n) {
                                statement.setInt(idx, n);
                            } else if (parameter instanceof Long n) {
                                statement.setLong(idx, n);
                            } else if (parameter instanceof Double n) {
                                statement.setDouble(idx, n);
                            } else if (parameter instanceof Float n) {
                                statement.setFloat(idx, n);
                            } else if (parameter instanceof Boolean b) {
                                statement.setBoolean(idx, b);
                            } else if (parameter instanceof UUID u) {
                                statement.setObject(idx, u.toString(), Types.VARCHAR);
                            } else if (parameter instanceof byte[] bytes) {
                                statement.setBytes(idx, bytes);
                            } else if (parameter instanceof Date d) {
                                statement.setDate(idx, d);
                            } else if (parameter instanceof Timestamp ts) {
                                statement.setTimestamp(idx, ts);
                            } else if (parameter instanceof LocalDate ld) {
                                statement.setObject(idx, ld, Types.DATE);
                            } else if (parameter instanceof LocalDateTime ldt) {
                                statement.setObject(idx, ldt, Types.TIMESTAMP);
                            } else if (parameter instanceof Clob c) {
                                statement.setClob(idx, c);
                            } else if (parameter instanceof Blob b) {
                                statement.setBlob(idx, b);
                            } else if (parameter instanceof EntityType et) {
                                statement.setString(idx, et.name());
                            } else {
                                statement.setObject(idx, parameter);
                            }
                        }
                    }

                    statement.executeUpdate();
                }
            } catch (SQLException exception) {
                String sqlState = exception.getSQLState();
                String message = exception.getMessage() != null
                        ? exception.getMessage().toLowerCase(java.util.Locale.ROOT)
                        : "";

                if ("42701".equals(sqlState)
                        || "42P07".equals(sqlState)
                        || "42S01".equals(sqlState)
                        || "42S02".equals(sqlState)
                        || "42S04".equals(sqlState)
                        || "X0Y32".equals(sqlState)
                        || "42000".equals(sqlState)
                        || ("SQLITE_ERROR".equals(sqlState) && message.contains("duplicate column name"))) {
                    // ignore
                } else {
                    plugin.getLogger().severe("Error executing SQL update");
                    plugin.getLogger().severe("Failed SQL statement: " + sql);
                    plugin.logStackTrace(exception);
                }
            }
        });
    }

    /**
     * Executes an update SQL statement with parameters using the main thread.
     *
     * @param sql        the SQL statement.
     * @param parameters the parameters for the SQL statement.
     * @throws SQLException if a database access error occurs.
     */
    @ApiStatus.Experimental
    private void executeUpdateMainThread(String sql, Object[] parameters) throws SQLException {
        try (Connection connection = getConnection()) {
            if (connection == null) {
                plugin.getLogger().severe("Error executing SQL update on main thread: connection is null");
                plugin.getLogger().severe("Failed SQL statement: " + sql);
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                if (parameters != null) {
                    for (int i = 0; i < parameters.length; i++) {
                        Object parameter = parameters[i];
                        int idx = i + 1;

                        if (parameter == null) {
                            statement.setNull(idx, Types.VARCHAR);
                        } else if (parameter instanceof String s) {
                            statement.setString(idx, s);
                        } else if (parameter instanceof Integer n) {
                            statement.setInt(idx, n);
                        } else if (parameter instanceof Long n) {
                            statement.setLong(idx, n);
                        } else if (parameter instanceof Double n) {
                            statement.setDouble(idx, n);
                        } else if (parameter instanceof Float n) {
                            statement.setFloat(idx, n);
                        } else if (parameter instanceof Boolean b) {
                            statement.setBoolean(idx, b);
                        } else if (parameter instanceof UUID u) {
                            statement.setObject(idx, u.toString(), Types.VARCHAR);
                        } else if (parameter instanceof byte[] bytes) {
                            statement.setBytes(idx, bytes);
                        } else if (parameter instanceof Date d) {
                            statement.setDate(idx, d);
                        } else if (parameter instanceof Timestamp ts) {
                            statement.setTimestamp(idx, ts);
                        } else if (parameter instanceof LocalDate ld) {
                            statement.setObject(idx, ld, Types.DATE);
                        } else if (parameter instanceof LocalDateTime ldt) {
                            statement.setObject(idx, ldt, Types.TIMESTAMP);
                        } else if (parameter instanceof Clob c) {
                            statement.setClob(idx, c);
                        } else if (parameter instanceof Blob b) {
                            statement.setBlob(idx, b);
                        } else if (parameter instanceof EntityType et) {
                            statement.setString(idx, et.name());
                        } else {
                            statement.setObject(idx, parameter);
                        }
                    }
                }

                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            String message = exception.getMessage() != null
                    ? exception.getMessage().toLowerCase(java.util.Locale.ROOT)
                    : "";
            if ("42701".equals(sqlState)
                    || "42P07".equals(sqlState)
                    || "42S01".equals(sqlState)
                    || "42S02".equals(sqlState)
                    || "42S04".equals(sqlState)
                    || "X0Y32".equals(sqlState)
                    || "42000".equals(sqlState)
                    || ("SQLITE_ERROR".equals(sqlState) && message.contains("duplicate column name"))) {
                // ignore
            } else {
                plugin.getLogger().severe("Error executing SQL update");
                plugin.getLogger().severe("Failed SQL statement: " + sql);
                plugin.logStackTrace(exception);
            }
        }
    }

    /**
     * Tests the database connection.
     *
     * @return true if the connection is successful, false otherwise.
     */
    private boolean testDatabaseConnection() {
        try (Connection testConnection = getConnection()) {
            if (testConnection == null) {
                plugin.getLogger().severe("Failed to obtain a database connection for type: " + this.type);
                return false;
            }
            try {
                return testConnection.isValid(5);
            } catch (SQLException ignored) {
                return !testConnection.isClosed();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to " + this.type + " database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Migrates data from the plugin’s SQLite schema into the configured target database.
     * <p>
     * - Reads each table from the SQLite file in {@code plugin/data/data.db}.
     * - Recreates it in the target database with mapped column types.
     * - Copies all rows.
     * - Renames the SQLite file to data.old.db on success.
     * </p>
     */
    private void migrate() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        File sqliteFile  = new File(dataFolder, "data.db");
        if (!dataFolder.exists() || !sqliteFile.exists()) {
            plugin.getLogger().warning("No SQLite database at " + sqliteFile.getPath() + "; skipping migration.");
            return;
        }

        HikariConfig sourceConfig = new HikariConfig();
        String journalMode = plugin.getConfig().getString("settings.storage.sqlite.journal-mode", "WAL");
        String synchronous = plugin.getConfig().getString("settings.storage.sqlite.synchronous", "OFF");
        sourceConfig.setJdbcUrl("jdbc:sqlite:" + sqliteFile.getPath());
        sourceConfig.setDriverClassName("org.sqlite.JDBC");
        sourceConfig.setConnectionInitSql(
                "PRAGMA busy_timeout = 30000; " +
                        "PRAGMA journal_mode = " + journalMode + "; " +
                        "PRAGMA synchronous = " + synchronous + ";"
        );
        sourceConfig.setConnectionTimeout(30000);
        sourceConfig.setIdleTimeout(600000);
        sourceConfig.setMaxLifetime(1800000);
        sourceConfig.setMaximumPoolSize(10);

        try (HikariDataSource sourceDs = new HikariDataSource(sourceConfig);
             Connection sqliteConn = sourceDs.getConnection();
             Connection targetConn = getConnection()) {

            if (targetConn == null) {
                plugin.getLogger().severe("Migration aborted: target database connection is null.");
                return;
            }

            DatabaseMetaData sqliteMeta = sqliteConn.getMetaData();
            try (ResultSet tables = sqliteMeta.getTables(null, null, getStoragePrefix() + "%", new String[] { "TABLE" })) {
                boolean allSuccess = true;

                while (tables.next()) {
                    String sqliteTable = tables.getString("TABLE_NAME");

                    StringBuilder createSql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                            .append(sqliteTable).append(" (");
                    StringJoiner defs = new StringJoiner(", ");

                    List<String> cols = new ArrayList<>();
                    try (Statement stmt = sqliteConn.createStatement();
                         ResultSet rsMeta = stmt.executeQuery("SELECT * FROM " + sqliteTable + " LIMIT 1")) {
                        ResultSetMetaData md = rsMeta.getMetaData();
                        for (int i = 1; i <= md.getColumnCount(); i++) {
                            String col        = md.getColumnName(i);
                            String sqliteType = md.getColumnTypeName(i);
                            String targetType = mapSQLiteTypeToTargetDB(sqliteType, col);
                            if (targetType == null) continue;
                            cols.add(col);
                            defs.add(col + " " + targetType);
                        }
                    }

                    if (cols.isEmpty()) {
                        plugin.getLogger().warning("No columns mapped for " + sqliteTable + "; skipping.");
                        continue;
                    }

                    createSql.append(defs).append(");");
                    executeUpdate(createSql.toString(), new Object[0]);

                    if (sqliteTable.equals(getStoragePrefix() + "grave")) {
                        adjustGraveTableForTargetDB();
                    }

                    String placeholders = String.join(", ", Collections.nCopies(cols.size(), "?"));
                    String insertSql = "INSERT INTO " + sqliteTable +
                            " (" + String.join(", ", cols) + ") VALUES (" + placeholders + ")";

                    try (PreparedStatement insert = targetConn.prepareStatement(insertSql);
                         Statement readStmt = sqliteConn.createStatement();
                         ResultSet rows = readStmt.executeQuery("SELECT * FROM " + sqliteTable)) {

                        while (rows.next()) {
                            for (int idx = 0; idx < cols.size(); idx++) {
                                String val = rows.getString(cols.get(idx));
                                insert.setString(idx + 1, val);
                            }
                            insert.executeUpdate();
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().severe("Error inserting into " + sqliteTable);
                        plugin.logStackTrace(e);
                        allSuccess = false;
                    }
                }

                if (allSuccess) {
                    Path from = sqliteFile.toPath();
                    Path to   = new File(dataFolder, "data.old.db").toPath();
                    try {
                        Files.move(from, to,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                        plugin.getLogger().info("Renamed SQLite file to data.old.db");
                    } catch (Exception moveEx) {
                        plugin.getLogger().severe("Failed to rename SQLite file");
                        plugin.logStackTrace(moveEx);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Migration failed");
            plugin.logStackTrace(e);
        }
    }

    /**
     * Maps a SQLite data type to the appropriate type for the configured
     * target database (MySQL/MariaDB, PostgreSQL, H2, or MSSQL).
     *
     * @param sqliteType  the SQLite type declaration (e.g. "VARCHAR(255)", "INT")
     * @param columnName  the column name, used for special-case mappings
     * @return the target database type declaration, or null if no mapping is available
     */
    private String mapSQLiteTypeToTargetDB(String sqliteType, String columnName) {
        return switch (type) {
            case MYSQL, MARIADB -> mapSQLiteTypeToMySQL(sqliteType, columnName);
            case POSTGRESQL     -> mapSQLiteTypeToPostgreSQL(sqliteType, columnName);
            case H2             -> mapSQLiteTypeToH2(sqliteType, columnName);
            case MSSQL          -> mapSQLiteTypeToMSSQL(sqliteType, columnName);
            default -> {
                plugin.getLogger().warning("Unhandled database type: " + type);
                yield null;
            }
        };
    }

    /**
     * Maps a SQLite column type (including optional length/precision) to the corresponding
     * MySQL/MariaDB type.
     *
     * @param sqliteType  the SQLite type declaration (e.g. "VARCHAR(100)", "INT", "NUMERIC(8,2)")
     * @param columnName  the column name, used for special‐casing certain fields
     * @return the MySQL/MariaDB type declaration, or null if no mapping is available
     */
    private String mapSQLiteTypeToMySQL(String sqliteType, String columnName) {
        String upperType = sqliteType.toUpperCase(java.util.Locale.ROOT).trim();

        if (upperType.startsWith("INT")) {
            if ("protection".equals(columnName)) {
                return "INT(1)";
            }
            if ("time_protection".equals(columnName)
                    || "time_creation".equals(columnName)
                    || "time_alive".equals(columnName)) {
                return "BIGINT";
            }
            return "INT(16)";
        }
        if (upperType.startsWith("BIGINT")) {
            return "BIGINT";
        }
        if (upperType.startsWith("VARCHAR")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String len = (start >= 0 && end > start) ? upperType.substring(start + 1, end) : "255";
            return "VARCHAR(" + len + ")";
        }
        if (upperType.startsWith("CHAR")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String len = (start >= 0 && end > start) ? upperType.substring(start + 1, end) : "";
            return len.isEmpty() ? "CHAR" : "CHAR(" + len + ")";
        }
        if (upperType.startsWith("TEXT")) {
            return "TEXT";
        }
        if (upperType.startsWith("BLOB")) {
            return "BLOB";
        }
        if (upperType.startsWith("NUMERIC")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String spec = (start >= 0 && end > start) ? upperType.substring(start, end + 1) : "(10,5)";
            return "DECIMAL" + spec;
        }
        if (upperType.startsWith("FLOAT")) {
            return "FLOAT(16)";
        }
        if (upperType.startsWith("REAL") || upperType.startsWith("DOUBLE")) {
            return "DOUBLE";
        }

        plugin.getLogger().warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
        return null;
    }

    /**
     * Maps a SQLite column type (including optional length/precision) to the corresponding PostgreSQL type.
     *
     * @param sqliteType  the SQLite type declaration (e.g. "VARCHAR(100)", "INT", "NUMERIC(8,2)")
     * @param columnName  the column name, used for special casing certain fields
     * @return the PostgreSQL type declaration, or null if no mapping is available
     */
    private String mapSQLiteTypeToPostgreSQL(String sqliteType, String columnName) {
        String upperType = sqliteType.toUpperCase(java.util.Locale.ROOT).trim();

        if (upperType.startsWith("INT")) {
            if ("protection".equals(columnName)) {
                return "BOOLEAN";
            }
            if ("time_protection".equals(columnName)
                    || "time_creation".equals(columnName)
                    || "time_alive".equals(columnName)) {
                return "BIGINT";
            }
            return "INTEGER";
        }
        if (upperType.startsWith("BIGINT")) {
            return "BIGINT";
        }
        if (upperType.startsWith("VARCHAR")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String len = (start >= 0 && end > start) ? upperType.substring(start + 1, end) : "255";
            return "VARCHAR(" + len + ")";
        }
        if (upperType.startsWith("CHAR")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String len = (start >= 0 && end > start) ? upperType.substring(start + 1, end) : "";
            return len.isEmpty() ? "CHAR" : "CHAR(" + len + ")";
        }
        if (upperType.startsWith("TEXT")) {
            return "TEXT";
        }
        if (upperType.startsWith("BLOB")) {
            return "BYTEA";
        }
        if (upperType.startsWith("NUMERIC")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String spec = (start >= 0 && end > start) ? upperType.substring(start, end + 1) : "(10,5)";
            return "NUMERIC" + spec;
        }
        if (upperType.startsWith("FLOAT")) {
            return "REAL";
        }
        if (upperType.startsWith("DOUBLE") || upperType.startsWith("REAL")) {
            return "DOUBLE PRECISION";
        }

        plugin.getLogger().warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
        return null;
    }

    /**
     * Maps a SQLite column type (including optional length/precision) to the corresponding H2 type.
     *
     * @param sqliteType  the SQLite type declaration (e.g. "VARCHAR(100)", "INT", "NUMERIC(8,2)")
     * @param columnName  the column name, used for special casing certain fields
     * @return the H2 type declaration, or null if no mapping is available
     */
    private String mapSQLiteTypeToH2(String sqliteType, String columnName) {
        String upperType = sqliteType.toUpperCase(java.util.Locale.ROOT).trim();

        if (upperType.startsWith("INT")) {
            if ("protection".equals(columnName)) {
                return "BOOLEAN";
            }
            if ("time_protection".equals(columnName)
                    || "time_creation".equals(columnName)
                    || "time_alive".equals(columnName)) {
                return "BIGINT";
            }
            return "INTEGER";
        }
        if (upperType.startsWith("BIGINT")) {
            return "BIGINT";
        }
        if (upperType.startsWith("VARCHAR")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String len = (start >= 0 && end > start) ? upperType.substring(start + 1, end) : "255";
            return "VARCHAR(" + len + ")";
        }
        if (upperType.startsWith("CHAR")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String len = (start >= 0 && end > start) ? upperType.substring(start + 1, end) : "";
            return len.isEmpty() ? "CHAR" : "CHAR(" + len + ")";
        }
        if (upperType.startsWith("TEXT")) {
            return "TEXT";
        }
        if (upperType.startsWith("BLOB")) {
            return "BLOB";
        }
        if (upperType.startsWith("NUMERIC")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String spec = (start >= 0 && end > start) ? upperType.substring(start, end + 1) : "(10,5)";
            return "NUMERIC" + spec;
        }
        if (upperType.startsWith("REAL") || upperType.startsWith("FLOAT") || upperType.startsWith("DOUBLE")) {
            return "DOUBLE";
        }

        plugin.getLogger().warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
        return null;
    }

    /**
     * Maps a SQLite column type (including length specifiers) to the corresponding MSSQL type.
     *
     * @param sqliteType  the SQLite type declaration (e.g. "VARCHAR(255)", "INT", "NUMERIC(10,2)")
     * @param columnName  the column name, used for special casing certain fields
     * @return the MSSQL type declaration, or null if no mapping is available
     */
    private String mapSQLiteTypeToMSSQL(String sqliteType, String columnName) {
        String upperType = sqliteType.toUpperCase(java.util.Locale.ROOT).trim();

        if (upperType.startsWith("INT")) {
            if ("protection".equals(columnName)) {
                return "BIT";
            }
            if ("time_protection".equals(columnName)
                    || "time_creation".equals(columnName)
                    || "time_alive".equals(columnName)) {
                return "BIGINT";
            }
            return "INT";
        }
        if (upperType.startsWith("BIGINT")) {
            return "BIGINT";
        }
        if (upperType.startsWith("VARCHAR")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String len = (start >= 0 && end > start) ? upperType.substring(start + 1, end) : "255";
            return "NVARCHAR(" + len + ")";
        }
        if (upperType.startsWith("CHAR")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String len = (start >= 0 && end > start) ? upperType.substring(start + 1, end) : "";
            return len.isEmpty() ? "NCHAR" : "NCHAR(" + len + ")";
        }
        if (upperType.startsWith("TEXT")) {
            return "NVARCHAR(MAX)";
        }
        if (upperType.startsWith("BLOB")) {
            return "VARBINARY(MAX)";
        }
        if (upperType.startsWith("NUMERIC")) {
            int start = upperType.indexOf('('), end = upperType.indexOf(')');
            String spec = (start >= 0 && end > start) ? upperType.substring(start, end + 1) : "(10,5)";
            return "DECIMAL" + spec;
        }
        if (upperType.startsWith("REAL") || upperType.startsWith("FLOAT") || upperType.startsWith("DOUBLE")) {
            return "FLOAT";
        }

        plugin.getLogger().warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
        return null;
    }

    /**
     * Adjusts the grave table for the target database if necessary for MSSQL.
     */
    private void adjustGraveTableForTargetDB() throws SQLException {
        String table = getStoragePrefix() + "grave";

        switch (type) {
            case MSSQL -> {
                plugin.getLogger().info("Altering table " + table + " to ensure column sizes are correct for MSSQL.");
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN owner_texture NVARCHAR(MAX)", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN owner_texture_signature NVARCHAR(MAX)", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN inventory NVARCHAR(MAX)", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN equipment NVARCHAR(MAX)", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN time_creation BIGINT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN time_protection BIGINT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN time_alive BIGINT", new Object[0]);
            }
            case MYSQL, MARIADB -> {
                plugin.getLogger().info("Altering table " + table + " to ensure column sizes are correct for MySQL/MariaDB.");
                executeUpdate("ALTER TABLE " + table + " MODIFY COLUMN owner_texture LONGTEXT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " MODIFY COLUMN owner_texture_signature LONGTEXT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " MODIFY COLUMN inventory LONGTEXT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " MODIFY COLUMN equipment LONGTEXT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " MODIFY COLUMN time_creation BIGINT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " MODIFY COLUMN time_protection BIGINT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " MODIFY COLUMN time_alive BIGINT", new Object[0]);
            }
            case POSTGRESQL, H2 -> {
                plugin.getLogger().info("Altering table " + table + " to ensure column sizes are correct for PostgreSQL/H2.");
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN owner_texture TYPE TEXT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN owner_texture_signature TYPE TEXT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN inventory TYPE TEXT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN equipment TYPE TEXT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN time_creation TYPE BIGINT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN time_protection TYPE BIGINT", new Object[0]);
                executeUpdate("ALTER TABLE " + table + " ALTER COLUMN time_alive TYPE BIGINT", new Object[0]);
            }
            default -> {
                // No adjustment needed for other types
            }
        }
    }

    /**
     * Keeps the database connection alive by periodically executing a query.
     */
    private void keepConnectionAlive() {
        plugin.getSchedulerManager().runTaskTimerAsynchronously(() -> {
            if (!isConnected()) {
                return;
            }

            checkAndUnlockDatabase();
            try (Connection connection = getConnection();
                 PreparedStatement statement = (connection != null) ? connection.prepareStatement("SELECT 1") : null) {

                if (statement != null) {
                    statement.execute();
                } else {
                    plugin.getLogger().severe("Keep-alive skipped: connection is null");
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to keep connection alive.");
                plugin.logStackTrace(exception);
            }
        }, 0L, 25 * 20L);
    }

    /**
     * Checks if the Database is locked and attempts to unlock the database.
     */
    private void checkAndUnlockDatabase() {
        String checkQuery = "SELECT 1";

        try (Connection connection = getConnection();
             Statement statement = (connection != null) ? connection.createStatement() : null) {

            if (statement == null) {
                plugin.getLogger().severe("Database check failed: connection is null.");
                return;
            }
            statement.execute(checkQuery);

        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase(java.util.Locale.ROOT).contains("database is locked")) {
                plugin.getLogger().severe("Database is locked. Attempting to unlock...");

                switch (type) {
                    case SQLITE -> handleUnlockSQLite();
                    case MYSQL, MARIADB -> handleUnlockMySQL();
                    case POSTGRESQL, H2 -> handleUnlockPostgreSQLandH2();
                    case MSSQL -> handleUnlockMSSQL();
                    case INVALID -> plugin.getLogger().warning(
                            "Server is using an invalid database type. No attempts to fix database locking will be attempted.");
                    default -> plugin.getLogger().warning(
                            "Unhandled database type. No attempts to fix database locking will be attempted.");
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
        try (Connection connection = getConnection()) {
            if (connection == null) {
                plugin.getLogger().severe("Failed to unlock MySQL/MariaDB database: connection is null");
                return;
            }

            plugin.getLogger().info("Attempting to unlock MySQL/MariaDB database.");
            connection.setAutoCommit(false);
            connection.commit();
            plugin.getLogger().info("MySQL/MariaDB database unlocked successfully using COMMIT.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unlock MySQL/MariaDB database using COMMIT");
            plugin.logStackTrace(e);
        }
    }

    /**
     * Handles unlocking for PostgreSQL/H2 databases using COMMIT or ROLLBACK.
     */
    private void handleUnlockPostgreSQLandH2() {
        try (Connection connection = getConnection()) {
            if (connection == null) {
                plugin.getLogger().severe("Failed to unlock PostgreSQL/H2 database: connection is null");
                return;
            }
            connection.setAutoCommit(false);
            connection.commit();
            plugin.getLogger().info("PostgreSQL/H2 database unlocked successfully using COMMIT.");
            return;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unlock PostgreSQL/H2 database using COMMIT");
            plugin.logStackTrace(e);
        }

        try (Connection connection = getConnection()) {
            if (connection == null) {
                plugin.getLogger().severe("Failed to unlock PostgreSQL/H2 database via ROLLBACK: connection is null");
                return;
            }
            connection.rollback();
            plugin.getLogger().info("PostgreSQL/H2 database unlocked successfully using ROLLBACK.");
        } catch (SQLException rollbackException) {
            plugin.getLogger().severe("Failed to unlock PostgreSQL/H2 database using ROLLBACK");
            plugin.logStackTrace(rollbackException);
        }
    }

    /**
     * Handles unlocking for Microsoft SQL Server databases using COMMIT.
     */
    private void handleUnlockMSSQL() {
        try (Connection connection = getConnection()) {
            if (connection == null) {
                plugin.getLogger().severe("Failed to unlock MSSQL database: connection is null");
                return;
            }
            plugin.getLogger().info("Attempting to unlock MSSQL database.");
            connection.setAutoCommit(false);
            connection.commit();
            plugin.getLogger().info("MSSQL database unlocked successfully using COMMIT.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unlock MSSQL database using COMMIT");
            plugin.logStackTrace(e);
        }
    }

    /**
     * Retrieves the configured storage prefix for GravesX from the plugin configuration.
     * <p>
     * If the prefix is not set or is empty/whitespace, this method returns an empty string.
     * If a valid prefix is provided, it appends an underscore to the end (e.g., {@code "gravesx_"}).
     * <p>
     * While this is intended to help namespace GravesX database tables, note that
     * shared databases (e.g., running multiple plugin instances in the same DB) are not supported.
     *
     * @return the formatted table prefix with an underscore, or an empty string if no prefix is set.
     */
    @ApiStatus.Experimental
    private String getStoragePrefix() {
        String prefix = plugin.getConfig().getString("settings.storage.prefix", "");
        if (prefix.isBlank()) {
            return "";
        }
        return prefix.trim();
    }
}