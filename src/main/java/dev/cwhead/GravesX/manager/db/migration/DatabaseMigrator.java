package dev.cwhead.GravesX.manager.db.migration;

import com.ranull.graves.Graves;
import com.ranull.graves.manager.DataManager;
import com.ranull.graves.manager.DataManager.Type;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Migrates data from a legacy database into the configured target database. All legacy-database handling
 * lives in this package; {@code DataManager} only calls the static entry points below.
 */
public final class DatabaseMigrator {
    private static final Set<String> JOURNAL_MODES = Set.of("DELETE", "TRUNCATE", "PERSIST", "MEMORY", "WAL", "OFF");
    private static final Set<String> SYNCHRONOUS_MODES = Set.of("OFF", "0", "NORMAL", "1", "FULL", "2", "EXTRA", "3");

    private final Graves plugin;
    private final Logger logger;
    private final DataManager dataManager;
    private final Type targetType;
    private final ColumnTypeMapper typeMapper;
    private final String prefix;

    private DatabaseMigrator(Graves plugin, DataManager dataManager, Type targetType) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataManager = dataManager;
        this.targetType = targetType;
        this.typeMapper = new ColumnTypeMapper(targetType, logger, plugin);
        this.prefix = dataManager.getStoragePrefix();
    }

    /**
     * Migrates a legacy database into the target database, if there is one. Does nothing otherwise.
     *
     * @param plugin      the plugin instance
     * @param dataManager the data manager that owns the target connection pool
     * @param targetType  the type of the target database
     */
    public static void migrate(Graves plugin, DataManager dataManager, Type targetType) {
        LegacyDatabaseFiles.relocateRootFiles(plugin);
        new DatabaseMigrator(plugin, dataManager, targetType).run();
    }

    /**
     * Prepares the files for a SQLite to H2 migration: brings a legacy root-folder {@code data.db} into
     * {@code data/} and moves any existing H2 database aside so the migration starts from a clean one.
     *
     * @param plugin the plugin instance
     */
    public static void prepareSQLiteToH2(Graves plugin) {
        LegacyDatabaseFiles.relocateRootFiles(plugin);
        LegacyDatabaseFiles.moveAsideExistingH2(plugin);
    }

    /**
     * Brings legacy SQLite files from the plugin's root folder into {@code data/} without overwriting anything.
     *
     * @param plugin the plugin instance
     */
    public static void relocateRootFiles(Graves plugin) {
        LegacyDatabaseFiles.relocateRootFiles(plugin);
    }

    private record Source(Type type, Path path) {}

    private void run() {
        Source source = detectSource();
        if (source == null) return;

        switch (source.type()) {
            case SQLITE -> migrateFromSQLite(source.path());
            case H2 -> migrateFromH2(source.path());
            default -> logger.warning("Unsupported migration source type: " + source.type());
        }
    }

    /**
     * Finds the legacy database to migrate. A SQLite file takes priority over an H2 file.
     *
     * @return the source, or null if there is nothing to migrate or the direction is not supported
     */
    private Source detectSource() {
        if (targetType == Type.SQLITE) return null; // nothing is ever migrated into SQLite

        Path dataDir = LegacyDatabaseFiles.dataDir(plugin);

        Path sqliteFile = dataDir.resolve(LegacyDatabaseFiles.SQLITE_FILE);
        if (Files.isRegularFile(sqliteFile)) {
            return new Source(Type.SQLITE, sqliteFile);
        }

        Path h2Base = dataDir.resolve(LegacyDatabaseFiles.H2_BASE_NAME);
        if (Files.isRegularFile(LegacyDatabaseFiles.h2File(h2Base, ".mv.db"))) {
            return targetType == Type.H2 ? null : new Source(Type.H2, h2Base); // H2 is never migrated into H2
        }

        return null;
    }

    private void migrateFromSQLite(Path sqliteFile) {
        if (!Files.isRegularFile(sqliteFile)) {
            plugin.databaseDebug("Skipping SQLite migration because the source database file does not exist.", 2);
            logger.warning("Skipping SQLite migration because the source database file does not exist.");
            return;
        }

        boolean migrated = copyFrom(sqliteSourceConfig(sqliteFile), Type.SQLITE, "Failed to migrate legacy SQLite database.");

        // The pool and connections are closed by now, so the file can be moved on Windows.
        if (!migrated) {
            plugin.databaseDebug("Legacy SQLite migration did not complete; data.db was left untouched.", 2);
            logger.severe("Legacy SQLite migration did not complete; data.db was left untouched.");
            return;
        }

        try {
            LegacyDatabaseFiles.renameMigratedSQLite(sqliteFile);
        } catch (IOException exception) {
            plugin.databaseDebug("SQLite data was migrated, but data.db could not be renamed to data.migrated.db.", 2);
            logger.severe("SQLite data was migrated, but data.db could not be renamed to data.migrated.db.");
            plugin.logStackTrace(exception);
        }
    }

    private HikariConfig sqliteSourceConfig(Path sqliteFile) {
        String journalMode = plugin.getConfig().getString("settings.storage.sqlite.journal-mode", "WAL").toUpperCase(Locale.ROOT);
        String synchronous = plugin.getConfig().getString("settings.storage.sqlite.synchronous", "OFF").toUpperCase(Locale.ROOT);

        if (!JOURNAL_MODES.contains(journalMode)) journalMode = "WAL";
        if (!SYNCHRONOUS_MODES.contains(synchronous)) synchronous = "OFF";

        return getHikariConfig(sqliteFile, journalMode, synchronous);
    }

    private HikariConfig getHikariConfig(Path sqliteFile, String journalMode, String synchronous) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + sqliteFile.toAbsolutePath() + "?busy_timeout=30000");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setPoolName("GravesX SQLite Migration to " + dataManager.getType());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionInitSql("PRAGMA journal_mode=" + journalMode + "; PRAGMA synchronous=" + synchronous + ";");
        config.setConnectionTestQuery("SELECT 1");
        return config;
    }

    private void migrateFromH2(Path h2Base) {
        Path mvFile = LegacyDatabaseFiles.h2File(h2Base, ".mv.db");

        if (!Files.isRegularFile(mvFile)) {
            plugin.databaseDebug("Skipping H2 migration because the source database file does not exist: " + mvFile, 2);
            logger.warning("Skipping H2 migration because the source database file does not exist: " + mvFile);
            return;
        }

        if (!copyFrom(h2SourceConfig(h2Base), Type.H2, "H2 migration failed")) {
            plugin.databaseDebug("H2 migration did not complete; source files were left untouched.", 2);
            logger.severe("H2 migration did not complete; source files were left untouched.");
            return;
        }

        try {
            LegacyDatabaseFiles.renameMigratedH2(h2Base);
        } catch (IOException exception) {
            plugin.databaseDebug("H2 data was migrated, but the old H2 files could not be renamed.", 2);
            logger.severe("H2 data was migrated, but the old H2 files could not be renamed.");
            plugin.logStackTrace(exception);
        }
    }

    private HikariConfig h2SourceConfig(Path h2Base) {
        int maxConnections = plugin.getConfig().getInt("settings.storage.h2.maxConnections", 50);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:file:" + h2Base.toAbsolutePath() + ";AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1;");
        config.setDriverClassName("com.ranull.graves.libraries.h2.Driver");
        config.setUsername(plugin.getConfig().getString("settings.storage.h2.username", "sa"));
        config.setPassword(plugin.getConfig().getString("settings.storage.h2.password", ""));
        config.setPoolName("GravesX h2 Migration to " + dataManager.getType());
        config.setMaximumPoolSize(maxConnections);
        config.setMinimumIdle(Math.min(2, maxConnections));
        config.setConnectionTimeout(plugin.getConfig().getLong("settings.storage.h2.connectionTimeout", 30000));
        config.setIdleTimeout(600000);
        config.setMaxLifetime(plugin.getConfig().getLong("settings.storage.h2.maxLifetime", 1800000));
        config.setConnectionTestQuery("SELECT 1");

        try {
            config.setKeepaliveTime(300000);
            config.setValidationTimeout(5000);
        } catch (Throwable ignored) {
        }

        return config;
    }

    /**
     * Opens the source and target, copies everything and closes the source again.
     *
     * @return true only if every table was copied and committed
     */
    private boolean copyFrom(HikariConfig sourceConfig, Type sourceType, String failureMessage) {
        try (HikariDataSource sourceDataSource = new HikariDataSource(sourceConfig);
             Connection sourceConn = sourceDataSource.getConnection();
             Connection targetConn = dataManager.getConnection()) {

            boolean copied = copyAllTables(sourceConn, targetConn, sourceType);
            if (copied && sourceType == Type.H2) {
                releaseH2(sourceConn);
            }
            return copied;
        } catch (Exception exception) {
            plugin.databaseDebug(failureMessage, 2);
            logger.severe(failureMessage);
            plugin.logStackTrace(exception);
            return false;
        }
    }

    /**
     * The source H2 URL uses DB_CLOSE_DELAY=-1, which keeps the file open after the pool closes.
     * Shutting the database down releases it so it can be renamed.
     */
    private void releaseH2(Connection sourceConn) {
        try (Statement statement = sourceConn.createStatement()) {
            statement.execute("SHUTDOWN");
        } catch (SQLException ignored) {
            // Best effort; the rename reports a failure if the file is still locked.
        }
    }

    private boolean copyAllTables(Connection source, Connection target, Type sourceType) throws SQLException {
        if (target == null) {
            plugin.databaseDebug("Migration aborted: target database connection is null.", 2);
            logger.severe("Migration aborted: target database connection is null.");
            return false;
        }

        List<String> tables = listSourceTables(source);
        if (tables.isEmpty()) {
            plugin.databaseDebug("Migration aborted: no tables were found in the " + sourceType + " source database.", 2);
            logger.severe("Migration aborted: no tables were found in the " + sourceType + " source database.");
            return false;
        }

        // Phase 1: create / reconcile every target table (DDL) before any row is copied.
        Map<String, List<String>> plan = new LinkedHashMap<>();
        for (String table : tables) {
            List<String> columns = prepareTargetTable(source, target, sourceType, table);
            if (columns != null) {
                plan.put(table, columns);
            }
        }

        // Phase 2: copy all rows in ONE transaction so a failure never leaves a partial copy behind.
        copyInTransaction(source, target, sourceType, plan);
        return true;
    }

    private List<String> listSourceTables(Connection source) throws SQLException {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> tables = new ArrayList<>();

        try (ResultSet rs = source.getMetaData().getTables(null, null, "%", null)) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                String schema = rs.getString("TABLE_SCHEM");
                String tableType = rs.getString("TABLE_TYPE");

                if (name == null) continue;
                if (schema != null && schema.equalsIgnoreCase("INFORMATION_SCHEMA")) continue;
                if (tableType == null
                        || !(tableType.equalsIgnoreCase("TABLE") || tableType.equalsIgnoreCase("BASE TABLE"))) continue;

                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.startsWith("sqlite_")) continue;
                if (!lowerPrefix.isEmpty() && !lower.startsWith(lowerPrefix)) continue;

                tables.add(lower);
            }
        }

        // Copy the grave table first.
        String graveTable = lowerPrefix + "grave";
        tables.sort(Comparator.comparing((String name) -> !name.equals(graveTable)));
        return tables;
    }

    /**
     * Creates the target table from the source schema if it does not exist, then works out which source
     * columns can be copied into it.
     *
     * @return the columns to copy, or null if the table has nothing to copy
     */
    private List<String> prepareTargetTable(Connection source, Connection target, Type sourceType, String table)
            throws SQLException {
        Map<String, String> columnTypes = readSourceColumns(source, sourceType, table);
        if (columnTypes.isEmpty()) {
            plugin.databaseDebug("No columns mapped for " + table + "; skipping.", 2);
            logger.warning("No columns mapped for " + table + "; skipping.");
            return null;
        }

        boolean isGraveTable = table.equalsIgnoreCase(prefix + "grave");
        String uuidColumn = isGraveTable ? findColumn(columnTypes, "uuid") : null;
        if (uuidColumn != null) {
            columnTypes.put(uuidColumn, targetType == Type.MSSQL ? "NVARCHAR(255)" : "VARCHAR(255)");
        }

        boolean alreadyExisted = tableExists(target, table);

        String definitions = columnTypes.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
        execute(target, "CREATE TABLE IF NOT EXISTS " + table + " (" + definitions + ")");

        if (isGraveTable) {
            adjustGraveTable(target);

            if (!alreadyExisted && uuidColumn != null) {
                addUniqueConstraint(target, table, uuidColumn);
            }
        }

        return reconcileColumns(target, table, columnTypes);
    }

    private static String findColumn(Map<String, String> columns, String name) {
        for (String column : columns.keySet()) {
            if (column.equalsIgnoreCase(name)) return column;
        }
        return null;
    }

    private boolean tableExists(Connection connection, String table) {
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1 FROM " + table + " WHERE 1 = 0").close();
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    private void addUniqueConstraint(Connection target, String table, String column) {
        try {
            execute(target, "ALTER TABLE " + table + " ADD UNIQUE (" + column + ")");
        } catch (SQLException exception) {
            // Not worth blocking the data copy over; the schema upgrade keeps working without it.
            plugin.databaseDebug("Could not add the UNIQUE constraint on " + table + "." + column + ": " + exception.getMessage(), 2);
            logger.warning("Could not add the UNIQUE constraint on " + table + "." + column + ": " + exception.getMessage());
        }
    }

    /**
     * @return source column name to target column type, in source order
     */
    private Map<String, String> readSourceColumns(Connection source, Type sourceType, String table) throws SQLException {
        Map<String, String> columns = new LinkedHashMap<>();

        try (Statement statement = source.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table + " LIMIT 1")) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            StringBuilder described = new StringBuilder();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String column = metaData.getColumnName(i);
                described.append(described.length() > 0 ? ", " : "").append(column).append(' ').append(metaData.getColumnTypeName(i));

                // Legacy 'chunk' columns no longer exist in any table; the value is derived from 'location'.
                if (sourceType == Type.SQLITE && column.equalsIgnoreCase("chunk")) {
                    plugin.databaseDebug("Skipping obsolete legacy column '" + table + ".chunk' (derived from location).", 1);
                    logger.info("Skipping obsolete legacy column '" + table + ".chunk' (derived from location).");
                    continue;
                }

                String sourceColumnType = metaData.getColumnTypeName(i);
                String targetColumnType = typeMapper.map(sourceType, sourceColumnType, column);

                if (targetColumnType == null) {
                    if (sourceType == Type.SQLITE) {
                        // Skipping the column would silently drop its data.
                        throw new IllegalStateException("Cannot migrate column '" + column + "' (type '"
                                + sourceColumnType + "') of table '" + table + "': no type mapping. Aborting so no data is lost.");
                    }
                    continue;
                }

                columns.put(column, targetColumnType);
            }
            plugin.databaseDebug("Source table '" + table + "' columns: " + described, 1);
        }

        return columns;
    }

    /**
     * The target table may already exist with the current schema, so only copy columns it really has.
     * A column that exists only in the source is added so its data is kept. Target columns the source
     * lacks (for example hologram.backend) stay NULL and are defaulted by the normal loader.
     */
    private List<String> reconcileColumns(Connection target, String table, Map<String, String> columnTypes)
            throws SQLException {
        Set<String> targetColumns = readTargetColumns(target, table);
        List<String> copyColumns = new ArrayList<>();

        for (Map.Entry<String, String> entry : columnTypes.entrySet()) {
            String column = entry.getKey();

            if (!targetColumns.contains(column.toLowerCase(Locale.ROOT))) {
                String keyword = targetType == Type.MSSQL ? " ADD " : " ADD COLUMN ";
                execute(target, "ALTER TABLE " + table + keyword + column + " " + entry.getValue());
                plugin.databaseDebug("Added legacy column '" + column + "' to '" + table + "' to preserve its data.", 1);
                logger.info("Added legacy column '" + column + "' to '" + table + "' to preserve its data.");
            }

            copyColumns.add(column);
        }

        return copyColumns;
    }

    private Set<String> readTargetColumns(Connection target, String table) throws SQLException {
        Set<String> names = new HashSet<>();

        try (Statement statement = target.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table + " WHERE 1 = 0")) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                names.add(metaData.getColumnName(i).toLowerCase(Locale.ROOT));
            }
        }

        return names;
    }

    private void copyInTransaction(Connection source, Connection target, Type sourceType,
                                   Map<String, List<String>> plan) throws SQLException {
        boolean previousAutoCommit = target.getAutoCommit();
        target.setAutoCommit(false);

        try {
            for (Map.Entry<String, List<String>> entry : plan.entrySet()) {
                copyTable(source, target, sourceType, entry.getKey(), entry.getValue());
            }
            target.commit();
        } catch (SQLException | RuntimeException exception) {
            try {
                target.rollback();
            } catch (SQLException rollbackException) {
                plugin.databaseDebug("Failed to roll back partial migration: " + rollbackException.getMessage(), 2);
                logger.severe("Failed to roll back partial migration: " + rollbackException.getMessage());
            }
            throw exception;
        } finally {
            try {
                target.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    private void copyTable(Connection source, Connection target, Type sourceType, String table, List<String> columns)
            throws SQLException {
        long sourceCount = countRows(source, table);
        long existing = countRows(target, table);

        if (sourceCount == 0) {
            plugin.databaseDebug("Table '" + table + "' is empty in the " + sourceType + " source; nothing to copy.", 1);
            logger.info("Table '" + table + "' is empty in the " + sourceType + " source; nothing to copy.");
            return;
        }

        if (existing >= sourceCount) {
            // Left over from an earlier run whose final rename failed. Do not insert duplicates.
            plugin.databaseDebug("Table '" + table + "' already holds " + existing + " row(s) (source has " + sourceCount + "); skipping copy.", 1);
            logger.info("Table '" + table + "' already holds " + existing + " row(s) (source has " + sourceCount + "); skipping copy.");
            return;
        }

        if (existing > 0) {
            throw new IllegalStateException("Table '" + table + "' in the target already has " + existing
                    + " row(s) but the source has " + sourceCount + ". Refusing to merge automatically.");
        }

        int copied = copyRows(source, target, table, columns);
        long inTarget = countRows(target, table);

        if (copied != sourceCount || inTarget != sourceCount) {
            throw new IllegalStateException("Row count mismatch for table '" + table + "': source=" + sourceCount
                    + ", copied=" + copied + ", target=" + inTarget);
        }

        plugin.databaseDebug("Migrated " + copied + " row(s) from " + sourceType + " table '" + table + "'.", 1);
        logger.info("Migrated " + copied + " row(s) from " + sourceType + " table '" + table + "'.");
    }

    private int copyRows(Connection source, Connection target, String table, List<String> columns) throws SQLException {
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        String insertSql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
        int copied = 0;

        try (PreparedStatement insert = target.prepareStatement(insertSql);
             Statement read = source.createStatement();
             ResultSet rows = read.executeQuery("SELECT * FROM " + table)) {

            boolean logRows = plugin.getDebugManager().isDatabaseEnabled(1);
            while (rows.next()) {
                if (logRows) {
                    plugin.databaseDebug("Migrating row from '" + table + "': " + DataManager.describeRow(rows), 1);
                }
                for (int i = 0; i < columns.size(); i++) {
                    insert.setObject(i + 1, rows.getObject(columns.get(i)));
                }
                insert.executeUpdate();
                copied++;
            }
        }

        return copied;
    }

    private long countRows(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        }
    }

    /**
     * Widens the grave table's text and time columns where the target database needs it.
     */
    private void adjustGraveTable(Connection target) throws SQLException {
        String table = prefix + "grave";
        String[] textColumns = {"owner_texture", "owner_texture_signature", "inventory", "equipment"};
        String[] timeColumns = {"time_creation", "time_protection", "time_alive"};

        switch (targetType) {
            case MSSQL -> {
                plugin.databaseDebug("Altering table " + table + " to ensure column sizes are correct for MSSQL.", 1);
                logger.info("Altering table " + table + " to ensure column sizes are correct for MSSQL.");
                alterColumns(target, table, "ALTER COLUMN ", " ", textColumns, "NVARCHAR(MAX)");
                alterColumns(target, table, "ALTER COLUMN ", " ", timeColumns, "BIGINT");
            }
            case MYSQL, MARIADB -> {
                plugin.databaseDebug("Altering table " + table + " to ensure column sizes are correct for MySQL/MariaDB.", 1);
                logger.info("Altering table " + table + " to ensure column sizes are correct for MySQL/MariaDB.");
                alterColumns(target, table, "MODIFY COLUMN ", " ", textColumns, "LONGTEXT");
                alterColumns(target, table, "MODIFY COLUMN ", " ", timeColumns, "BIGINT");
            }
            case POSTGRESQL, H2 -> {
                plugin.databaseDebug("Altering table " + table + " to ensure column sizes are correct for PostgreSQL/H2.", 1);
                logger.info("Altering table " + table + " to ensure column sizes are correct for PostgreSQL/H2.");
                alterColumns(target, table, "ALTER COLUMN ", " TYPE ", textColumns, "TEXT");
                alterColumns(target, table, "ALTER COLUMN ", " TYPE ", timeColumns, "BIGINT");
            }
            default -> {
                // No adjustment needed for other types.
            }
        }
    }

    private void alterColumns(Connection target, String table, String verb, String typeKeyword,
                              String[] columns, String columnType) throws SQLException {
        for (String column : columns) {
            execute(target, "ALTER TABLE " + table + " " + verb + column + typeKeyword + columnType);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        plugin.databaseDebug("Migration executing: " + sql, 1);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}