package dev.cwhead.GravesXProxy.library;

import com.alessiodp.libby.LibraryManager;
import dev.cwhead.GravesXCommon.libby.LibraryLoader;
import dev.cwhead.GravesXCommon.libby.LibraryLog;
import dev.cwhead.GravesXCommon.libby.LibraryRequest;
import dev.cwhead.GravesXCommon.libby.LoadResult;
import dev.cwhead.GravesXProxy.platform.ProxyPlatform;
import dev.cwhead.GravesXProxy.storage.SqlDialect;

/**
 * Downloads the runtime libraries the proxy needs (snakeyaml and the JDBC driver of the configured
 * database) through the shared Libby loader in {@code gravesx-common}, instead of bundling them in the jar.
 * Only Libby itself is shaded.
 *
 * <p>Coordinates use {@code {}} for dots so the shade plugin never rewrites them as class names.</p>
 */
public final class ProxyLibraries {
    private static final String SNAKEYAML_VERSION = "2.2";

    private final LibraryLoader loader;
    private final ProxyPlatform platform;

    public ProxyLibraries(LibraryManager manager, ProxyPlatform platform) {
        this.platform = platform;
        this.loader = new LibraryLoader(manager, new LibraryLog() {
            @Override
            public void debug(String message) {
                // quiet on proxies; the "Downloading library" line below is enough
            }

            @Override
            public void warn(String message) {
                platform.warn(message);
            }

            @Override
            public void error(String message, Throwable error) {
                platform.warn(message + " (" + error + ")");
            }
        });
    }

    /** snakeyaml reads config.yml, so it has to be on the class path before the config is read. */
    public void loadYaml() {
        if (LibraryLoader.isPresent("org.yaml.snakeyaml.Yaml", ProxyLibraries.class.getClassLoader())) {
            return; // BungeeCord already ships it
        }
        load("org{}yaml", "snakeyaml", SNAKEYAML_VERSION);
    }

    /** Loads the JDBC driver for the given database type. Throws if it cannot be downloaded. */
    public void loadDriver(SqlDialect dialect) {
        if (LibraryLoader.isPresent(dialect.driverClass, ProxyLibraries.class.getClassLoader())) {
            return;
        }
        switch (dialect) {
            case MYSQL -> load("com{}mysql", "mysql-connector-j", "8.4.0");
            case MARIADB -> load("org{}mariadb{}jdbc", "mariadb-java-client", "3.5.1");
            case POSTGRESQL -> load("org{}postgresql", "postgresql", "42.7.4");
            case MSSQL -> load("com{}microsoft{}sqlserver", "mssql-jdbc", "12.8.1.jre11");
        }
    }

    private void load(String group, String artifact, String version) {
        // Not relocated and not transitive: these jars are self-contained for what we use them for,
        // and skipping relocation means no extra relocator download on the proxy.
        LibraryRequest request = LibraryRequest.builder()
                .coordinates(group, artifact, version)
                .transitive(false)
                .build();

        platform.info("Downloading library " + request.label());
        if (loader.useMavenCentral().load(request) != LoadResult.LOADED) {
            throw new IllegalStateException("Could not load library " + request.label());
        }
    }
}
