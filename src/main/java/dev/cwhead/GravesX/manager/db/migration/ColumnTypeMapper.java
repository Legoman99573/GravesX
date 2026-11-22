package dev.cwhead.GravesX.manager.db.migration;

import com.ranull.graves.Graves;
import com.ranull.graves.manager.DataManager.Type;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Translates column types from a legacy SQLite or H2 database to the type syntax of the target database
 * (H2, MySQL/MariaDB, PostgreSQL or MSSQL) used when migrating tables.
 */
public final class ColumnTypeMapper {
    private static final Logger SILENT = createSilentLogger();

    private final Type type;
    private final Logger logger;
    private final Graves plugin;

    private static Logger createSilentLogger() {
        Logger silent = Logger.getAnonymousLogger();
        silent.setUseParentHandlers(false);
        silent.setLevel(java.util.logging.Level.OFF);
        return silent;
    }

    /**
     * @param targetType the database the migrated tables are created in
     * @param logger     logger for unmapped types
     */
    public ColumnTypeMapper(Type targetType, Logger logger) {
        this(targetType, logger, null);
    }

    /**
     * @param targetType the database the migrated tables are created in
     * @param logger     logger for unmapped types
     * @param plugin     plugin used for database debug output (may be null)
     */
    public ColumnTypeMapper(Type targetType, Logger logger, com.ranull.graves.Graves plugin) {
        this.type = targetType;
        this.logger = logger;
        this.plugin = plugin;
    }

    private void debug(String message, int level) {
        if (plugin != null) {
            plugin.databaseDebug(message, level);
        }
    }

    /**
     * Maps a column type from the source database to the equivalent type for the configured target database.
     *
     * @param sourceType       the kind of database the column comes from (SQLITE or H2)
     * @param sourceColumnType the declared column type in the source database
     * @param columnName       the column name, used for special-case mappings
     * @return the target type declaration, or null if no mapping is available
     */
    public String map(Type sourceType, String sourceColumnType, String columnName) {
        return switch (sourceType) {
            case SQLITE -> mapFromSQLite(sourceColumnType, columnName);
            case H2 -> mapH2TypeToTargetDB(sourceColumnType, columnName);
            default -> null;
        };
    }

    private String mapFromSQLite(String sqliteType, String columnName) {
        String mapped = probe().mapSQLiteTypeToTargetDB(sqliteType, columnName);

        if (mapped == null) {
            mapped = mapBySQLiteAffinity(sqliteType, columnName);
        }

        if (mapped == null) {
            debug("No safe mapping for SQLite type '" + sqliteType + "' (column " + columnName + ").", 2);
            logger.warning("No safe mapping for SQLite type '" + sqliteType + "' (column " + columnName + ").");
            return null;
        }

        // MySQL/MariaDB TEXT holds at most 64 KB; the plugin's own schema uses LONGTEXT for these columns.
        if ((type == Type.MYSQL || type == Type.MARIADB) && mapped.equals("TEXT")) {
            return "LONGTEXT";
        }

        return mapped;
    }

    /**
     * SQLite accepts any declared type name and decides how to store a column from its "affinity", which it
     * derives from the name (https://sqlite.org/datatype3.html). Types the exact mapper does not know are mapped
     * the same way, but only for the affinities whose values have a single predictable type.
     *
     * @return the target type, or null if there is no safe mapping
     */
    private String mapBySQLiteAffinity(String sqliteType, String columnName) {
        if (sqliteType == null) return null;

        String declared = sqliteType.toUpperCase(Locale.ROOT).trim();
        if (declared.isEmpty()) return null;

        ColumnTypeMapper exact = probe();

        if (declared.equals("BOOLEAN") || declared.equals("BOOL")) return exact.mapSQLiteTypeToTargetDB("BIGINT", columnName); // stored as 0/1
        if (declared.contains("INT")) return exact.mapSQLiteTypeToTargetDB("BIGINT", columnName);
        if (declared.contains("CLOB") || declared.contains("TEXT")) return exact.mapSQLiteTypeToTargetDB("TEXT", columnName);
        if (declared.contains("CHAR")) return exact.mapSQLiteTypeToTargetDB("VARCHAR(255)", columnName);
        if (declared.contains("BLOB")) return exact.mapSQLiteTypeToTargetDB("BLOB", columnName);
        if (declared.contains("REAL") || declared.contains("FLOA") || declared.contains("DOUB")) {
            return exact.mapSQLiteTypeToTargetDB("REAL", columnName);
        }

        return null;
    }

    /**
     * A mapper for the same target that does not log, used to ask "is there an exact mapping?" without
     * producing a warning for every type that the affinity rules then handle.
     */
    private ColumnTypeMapper probe() {
        return new ColumnTypeMapper(type, SILENT);
    }

    private String mapH2TypeToTargetDB(String h2Type, String columnName) {
        return switch (type) {
            case MYSQL, MARIADB -> mapH2TypeToMySQL(h2Type, columnName);
            case POSTGRESQL -> mapH2TypeToPostgreSQL(h2Type, columnName);
            case MSSQL -> mapH2TypeToMSSQL(h2Type, columnName);
            default -> {
                debug("Unhandled target database type: " + type, 2);
                logger.warning("Unhandled target database type: " + type);
                yield null;
            }
        };
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
                debug("Unhandled database type: " + type, 2);
                logger.warning("Unhandled database type: " + type);
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

        debug("Unhandled SQLite type: " + sqliteType + " for column: " + columnName, 2);
        logger.warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
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

        debug("Unhandled SQLite type: " + sqliteType + " for column: " + columnName, 2);
        logger.warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
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

        debug("Unhandled SQLite type: " + sqliteType + " for column: " + columnName, 2);
        logger.warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
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

        debug("Unhandled SQLite type: " + sqliteType + " for column: " + columnName, 2);
        logger.warning("Unhandled SQLite type: " + sqliteType + " for column: " + columnName);
        return null;
    }

    /**
     * Maps an H2 source column type to MySQL/MariaDB.
     *
     * @param h2Type     the H2 source column type
     * @param columnName the column name
     * @return the mapped MySQL/MariaDB type
     */
    private String mapH2TypeToMySQL(String h2Type, String columnName) {
        String normalized = h2Type.toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "CHARACTER VARYING", "VARCHAR", "VARCHAR_IGNORECASE", "TEXT", "CHARACTER LARGE OBJECT", "CLOB" -> {
                if ("uuid".equalsIgnoreCase(columnName)
                        || "owner_uuid".equalsIgnoreCase(columnName)
                        || "killer_uuid".equalsIgnoreCase(columnName)
                        || "entity_uuid".equalsIgnoreCase(columnName)
                        || "uuid_grave".equalsIgnoreCase(columnName)
                        || "uuid_entity".equalsIgnoreCase(columnName)) {
                    yield "VARCHAR(36)";
                }
                yield "LONGTEXT";
            }
            case "CHARACTER", "CHAR", "NCHAR" -> "CHAR(255)";
            case "BOOLEAN", "BIT" -> "BOOLEAN";
            case "TINYINT" -> "TINYINT";
            case "SMALLINT" -> "SMALLINT";
            case "INTEGER", "INT", "MEDIUMINT" -> "INT";
            case "BIGINT" -> "BIGINT";
            case "REAL" -> "FLOAT";
            case "DOUBLE", "DOUBLE PRECISION" -> "DOUBLE";
            case "NUMERIC", "DECIMAL", "DECFLOAT" -> "DECIMAL(65,30)";
            case "DATE" -> "DATE";
            case "TIME", "TIME WITHOUT TIME ZONE" -> "TIME";
            case "TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE", "TIMESTAMP WITH TIME ZONE" -> "DATETIME";
            case "BINARY", "VARBINARY", "BINARY VARYING", "BLOB", "BYTEA", "BINARY LARGE OBJECT" -> "LONGBLOB";
            case "JAVA_OBJECT", "JSON", "UUID", "ENUM", "GEOMETRY", "OTHER" -> "LONGTEXT";
            default -> {
                debug("Unknown H2 type for MySQL/MariaDB migration: " + h2Type
                        + " (column: " + columnName + ")", 2);
                logger.warning("Unknown H2 type for MySQL/MariaDB migration: " + h2Type
                        + " (column: " + columnName + ")");
                yield "LONGTEXT";
            }
        };
    }

    /**
     * Maps an H2 source column type to PostgreSQL.
     *
     * @param h2Type     the H2 source column type
     * @param columnName the column name
     * @return the mapped PostgreSQL type
     */
    private String mapH2TypeToPostgreSQL(String h2Type, String columnName) {
        String normalized = h2Type.toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "CHARACTER VARYING", "VARCHAR", "VARCHAR_IGNORECASE", "TEXT", "CHARACTER LARGE OBJECT", "CLOB" -> {
                if ("uuid".equalsIgnoreCase(columnName)
                        || "owner_uuid".equalsIgnoreCase(columnName)
                        || "killer_uuid".equalsIgnoreCase(columnName)
                        || "entity_uuid".equalsIgnoreCase(columnName)
                        || "uuid_grave".equalsIgnoreCase(columnName)
                        || "uuid_entity".equalsIgnoreCase(columnName)) {
                    yield "VARCHAR(36)";
                }
                yield "TEXT";
            }
            case "CHARACTER", "CHAR", "NCHAR" -> "CHAR(255)";
            case "BOOLEAN", "BIT" -> "BOOLEAN";
            case "TINYINT", "SMALLINT" -> "SMALLINT";
            case "INTEGER", "INT", "MEDIUMINT" -> "INTEGER";
            case "BIGINT" -> "BIGINT";
            case "REAL" -> "REAL";
            case "DOUBLE", "DOUBLE PRECISION" -> "DOUBLE PRECISION";
            case "NUMERIC", "DECIMAL", "DECFLOAT" -> "NUMERIC";
            case "DATE" -> "DATE";
            case "TIME", "TIME WITHOUT TIME ZONE" -> "TIME";
            case "TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE" -> "TIMESTAMP";
            case "TIMESTAMP WITH TIME ZONE" -> "TIMESTAMPTZ";
            case "BINARY", "VARBINARY", "BINARY VARYING", "BLOB", "BINARY LARGE OBJECT", "BYTEA" -> "BYTEA";
            case "UUID" -> "UUID";
            case "JSON" -> "JSONB";
            case "JAVA_OBJECT", "ENUM", "GEOMETRY", "OTHER" -> "TEXT";
            default -> {
                debug("Unknown H2 type for PostgreSQL migration: " + h2Type
                        + " (column: " + columnName + ")", 2);
                logger.warning("Unknown H2 type for PostgreSQL migration: " + h2Type
                        + " (column: " + columnName + ")");
                yield "TEXT";
            }
        };
    }

    /**
     * Maps an H2 source column type to Microsoft SQL Server.
     *
     * @param h2Type     the H2 source column type
     * @param columnName the column name
     * @return the mapped MSSQL type
     */
    private String mapH2TypeToMSSQL(String h2Type, String columnName) {
        String normalized = h2Type.toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "CHARACTER VARYING", "VARCHAR", "VARCHAR_IGNORECASE", "TEXT", "CHARACTER LARGE OBJECT", "CLOB" -> {
                if ("uuid".equalsIgnoreCase(columnName)
                        || "owner_uuid".equalsIgnoreCase(columnName)
                        || "killer_uuid".equalsIgnoreCase(columnName)
                        || "entity_uuid".equalsIgnoreCase(columnName)
                        || "uuid_grave".equalsIgnoreCase(columnName)
                        || "uuid_entity".equalsIgnoreCase(columnName)) {
                    yield "VARCHAR(36)";
                }
                yield "NVARCHAR(MAX)";
            }
            case "CHARACTER", "CHAR", "NCHAR" -> "NCHAR(255)";
            case "BOOLEAN", "BIT" -> "BIT";
            case "TINYINT" -> "TINYINT";
            case "SMALLINT" -> "SMALLINT";
            case "INTEGER", "INT", "MEDIUMINT" -> "INT";
            case "BIGINT" -> "BIGINT";
            case "REAL" -> "REAL";
            case "DOUBLE", "DOUBLE PRECISION" -> "FLOAT";
            case "NUMERIC", "DECIMAL", "DECFLOAT" -> "DECIMAL(38,18)";
            case "DATE" -> "DATE";
            case "TIME", "TIME WITHOUT TIME ZONE" -> "TIME";
            case "TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE", "TIMESTAMP WITH TIME ZONE" -> "DATETIME2";
            case "BINARY", "VARBINARY", "BINARY VARYING", "BLOB", "BINARY LARGE OBJECT", "BYTEA" -> "VARBINARY(MAX)";
            case "UUID" -> "UNIQUEIDENTIFIER";
            case "JSON", "JAVA_OBJECT", "ENUM", "GEOMETRY", "OTHER" -> "NVARCHAR(MAX)";
            default -> {
                debug("Unknown H2 type for MSSQL migration: " + h2Type
                        + " (column: " + columnName + ")", 2);
                logger.warning("Unknown H2 type for MSSQL migration: " + h2Type
                        + " (column: " + columnName + ")");
                yield "NVARCHAR(MAX)";
            }
        };
    }
}