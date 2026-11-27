package dev.cwhead.GravesXProxy.storage;

import dev.cwhead.GravesXProxy.protocol.Protocol.GraveSummary;
import dev.cwhead.GravesXProxy.config.DatabaseConfig;
import dev.cwhead.GravesXProxy.library.ProxyLibraries;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Global network store in MySQL, MariaDB, PostgreSQL or MSSQL. One row per grave, keyed by
 * (server_name, grave_uuid), so identical world names on different servers never clash.
 * All access happens on the proxy's single background store thread.
 */
public final class SqlGraveStore implements GraveStore {
    private final SqlDialect dialect;
    private final DatabaseConfig config;
    private final String table;
    private Connection connection;

    public SqlGraveStore(DatabaseConfig config) throws SQLException {
        this.config = config;
        this.dialect = SqlDialect.parse(config.type());
        if (dialect == null) {
            throw new SQLException("Unsupported database type '" + config.type()
                    + "'. Use MYSQL, MARIADB, POSTGRESQL or MSSQL.");
        }
        this.table = config.tablePrefix().replaceAll("[^A-Za-z0-9_]", "") + "network_graves";

        try (Statement st = conn().createStatement()) {
            for (String sql : dialect.schema(table)) {
                st.execute(sql);
            }
        }
    }

    private synchronized Connection conn() throws SQLException {
        if (connection != null) {
            try {
                if (!connection.isClosed() && connection.isValid(2)) {
                    return connection;
                }
            } catch (SQLException ignored) {
                // fall through and reconnect
            }
            closeQuietly();
        }

        try {
            // The driver is downloaded by Libby into this plugin's own class loader (see ProxyLibraries).
            Class.forName(dialect.driverClass, true, SqlGraveStore.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver " + dialect.driverClass + " could not be loaded", e);
        }

        Properties props = new Properties();
        props.putAll(config.options());
        props.setProperty("user", config.username());
        props.setProperty("password", config.password());

        connection = DriverManager.getConnection(dialect.url(config.host(), config.port(), config.name()), props);
        return connection;
    }

    @Override
    public synchronized void replaceOwner(String server, UUID owner, List<GraveSummary> graves) throws SQLException {
        Connection c = conn();
        boolean auto = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM " + table + " WHERE server_name = ? AND owner_uuid = ?")) {
                del.setString(1, server);
                del.setString(2, owner.toString());
                del.executeUpdate();
            }
            if (!graves.isEmpty()) {
                try (PreparedStatement ins = c.prepareStatement("INSERT INTO " + table
                        + " (server_name, grave_uuid, owner_uuid, world_name, pos_x, pos_y, pos_z, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    for (GraveSummary g : graves) {
                        ins.setString(1, server);
                        ins.setString(2, g.id().toString());
                        ins.setString(3, owner.toString());
                        ins.setString(4, g.world());
                        ins.setDouble(5, g.x());
                        ins.setDouble(6, g.y());
                        ins.setDouble(7, g.z());
                        ins.setLong(8, g.created());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException ignored) {
                // connection will be re-validated on the next call
            }
            throw e;
        } finally {
            try {
                c.setAutoCommit(auto);
            } catch (SQLException ignored) {
                // connection will be re-validated on the next call
            }
        }
    }

    @Override
    public synchronized Map<String, Integer> counts(UUID owner) throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT server_name, COUNT(*) FROM " + table + " WHERE owner_uuid = ? GROUP BY server_name")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1).toLowerCase(Locale.ROOT), rs.getInt(2));
                }
            }
        }
        return result;
    }

    @Override
    public synchronized List<GraveSummary> graves(UUID owner, String server, int limit) throws SQLException {
        List<GraveSummary> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(dialect.selectGraves(table, limit))) {
            ps.setString(1, owner.toString());
            ps.setString(2, server);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new GraveSummary(UUID.fromString(rs.getString(1).trim()), rs.getString(2),
                            rs.getDouble(3), rs.getDouble(4), rs.getDouble(5), rs.getLong(6)));
                }
            }
        }
        return result;
    }

    @Override
    public synchronized void forgetServer(String server) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM " + table + " WHERE server_name = ?")) {
            ps.setString(1, server);
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // nothing to do
            }
            connection = null;
        }
    }

    @Override
    public String describe() {
        return dialect.name().toLowerCase(Locale.ROOT) + " (" + config.host() + "/" + config.name() + ", table " + table + ")";
    }
}
