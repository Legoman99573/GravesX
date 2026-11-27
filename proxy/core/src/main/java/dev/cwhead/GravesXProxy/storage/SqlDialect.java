package dev.cwhead.GravesXProxy.storage;

import java.util.List;
import java.util.Locale;

/** The databases supported for the global network store. */
public enum SqlDialect {
    MYSQL("com.mysql.cj.jdbc.Driver", 3306),
    MARIADB("org.mariadb.jdbc.Driver", 3306),
    POSTGRESQL("org.postgresql.Driver", 5432),
    MSSQL("com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433);

    public final String driverClass;
    public final int defaultPort;

    SqlDialect(String driverClass, int defaultPort) {
        this.driverClass = driverClass;
        this.defaultPort = defaultPort;
    }

    public static SqlDialect parse(String type) {
        String t = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case "MYSQL" -> MYSQL;
            case "MARIADB" -> MARIADB;
            case "POSTGRESQL", "POSTGRES", "PGSQL" -> POSTGRESQL;
            case "MSSQL", "SQLSERVER", "SQL_SERVER" -> MSSQL;
            default -> null;
        };
    }

    String url(String host, int port, String database) {
        int p = port > 0 ? port : defaultPort;
        return switch (this) {
            case MYSQL -> "jdbc:mysql://" + host + ":" + p + "/" + database;
            case MARIADB -> "jdbc:mariadb://" + host + ":" + p + "/" + database;
            case POSTGRESQL -> "jdbc:postgresql://" + host + ":" + p + "/" + database;
            case MSSQL -> "jdbc:sqlserver://" + host + ":" + p + ";databaseName=" + database
                    + ";encrypt=true;trustServerCertificate=true";
        };
    }

    /** Statements that create the table and index if they do not exist yet. */
    List<String> schema(String table) {
        String idx = "idx_" + table + "_owner";
        return switch (this) {
            case MYSQL, MARIADB -> List.of(
                    "CREATE TABLE IF NOT EXISTS " + table + " ("
                            + "server_name VARCHAR(64) NOT NULL, grave_uuid CHAR(36) NOT NULL, owner_uuid CHAR(36) NOT NULL, "
                            + "world_name VARCHAR(255) NOT NULL, pos_x DOUBLE NOT NULL, pos_y DOUBLE NOT NULL, "
                            + "pos_z DOUBLE NOT NULL, created_at BIGINT NOT NULL, "
                            + "PRIMARY KEY (server_name, grave_uuid), INDEX " + idx + " (owner_uuid, server_name))");
            case POSTGRESQL -> List.of(
                    "CREATE TABLE IF NOT EXISTS " + table + " ("
                            + "server_name VARCHAR(64) NOT NULL, grave_uuid CHAR(36) NOT NULL, owner_uuid CHAR(36) NOT NULL, "
                            + "world_name VARCHAR(255) NOT NULL, pos_x DOUBLE PRECISION NOT NULL, pos_y DOUBLE PRECISION NOT NULL, "
                            + "pos_z DOUBLE PRECISION NOT NULL, created_at BIGINT NOT NULL, "
                            + "PRIMARY KEY (server_name, grave_uuid))",
                    "CREATE INDEX IF NOT EXISTS " + idx + " ON " + table + " (owner_uuid, server_name)");
            case MSSQL -> List.of(
                    "IF OBJECT_ID(N'" + table + "', N'U') IS NULL CREATE TABLE " + table + " ("
                            + "server_name VARCHAR(64) NOT NULL, grave_uuid CHAR(36) NOT NULL, owner_uuid CHAR(36) NOT NULL, "
                            + "world_name VARCHAR(255) NOT NULL, pos_x FLOAT NOT NULL, pos_y FLOAT NOT NULL, "
                            + "pos_z FLOAT NOT NULL, created_at BIGINT NOT NULL, "
                            + "CONSTRAINT pk_" + table + " PRIMARY KEY (server_name, grave_uuid))",
                    "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'" + idx + "' AND object_id = OBJECT_ID(N'" + table
                            + "')) CREATE INDEX " + idx + " ON " + table + " (owner_uuid, server_name)");
        };
    }

    /** SELECT of one owner's graves on one server, newest first, with the row limit already applied. */
    String selectGraves(String table, int limit) {
        int n = Math.max(1, limit);
        String cols = "grave_uuid, world_name, pos_x, pos_y, pos_z, created_at";
        String where = " FROM " + table + " WHERE owner_uuid = ? AND server_name = ? ORDER BY created_at DESC";
        return this == MSSQL
                ? "SELECT TOP (" + n + ") " + cols + where
                : "SELECT " + cols + where + " LIMIT " + n;
    }
}
