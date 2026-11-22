package dev.cwhead.GravesX.manager.db.migration;

import com.ranull.graves.Graves;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * File-level handling of legacy database files: finding them, relocating them, backing them up and
 * renaming them once their data has been migrated.
 */
public final class LegacyDatabaseFiles {
    static final String SQLITE_FILE = "data.db";
    static final String H2_BASE_NAME = "graves.data";

    private LegacyDatabaseFiles() {
    }

    static Path dataDir(Graves plugin) {
        return plugin.getDataFolder().toPath().resolve("data");
    }

    /**
     * @param h2Base the H2 database path without extension
     * @param extension for example {@code ".mv.db"}
     */
    static Path h2File(Path h2Base, String extension) {
        return h2Base.resolveSibling(h2Base.getFileName() + extension);
    }

    /**
     * Moves SQLite files ({@code data.db}, {@code data.db-wal}, {@code data.db-shm}) found in the plugin's root
     * folder into {@code data/}, where the rest of the plugin expects them.
     * <p>
     * A file is never overwritten: if the same name already exists in {@code data/}, the root file is left
     * where it is and a warning is logged.
     * </p>
     */
    static void relocateRootFiles(Graves plugin) {
        Path root = plugin.getDataFolder().toPath();
        Path dataDir = dataDir(plugin);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, SQLITE_FILE + "*")) {
            for (Path source : stream) {
                if (!Files.isRegularFile(source)) continue;

                Files.createDirectories(dataDir);
                Path destination = dataDir.resolve(source.getFileName());

                if (Files.exists(destination)) {
                    plugin.databaseDebug("Not moving " + source.getFileName() + " into /data because "
                            + destination.getFileName() + " already exists there. Remove or rename one of them.", 2);
                    plugin.getLogger().warning("Not moving " + source.getFileName() + " into /data because "
                            + destination.getFileName() + " already exists there. Remove or rename one of them.");
                    continue;
                }

                move(source, destination);
                plugin.databaseDebug("Moved legacy file " + source.getFileName() + " into /data.", 1);
                plugin.getLogger().info("Moved legacy file " + source.getFileName() + " into /data.");
            }
        } catch (IOException exception) {
            plugin.databaseDebug("Failed to move legacy SQLite files into /data: " + exception.getMessage(), 2);
            plugin.getLogger().warning("Failed to move legacy SQLite files into /data: " + exception.getMessage());
            plugin.logStackTrace(exception);
        }
    }

    /**
     * If a legacy SQLite file is about to be migrated, moves any existing H2 database out of the way so the
     * migration starts from a clean database and the normal schema upgrade runs on top of the copied data.
     * <p>
     * The old file becomes {@code graves.data.old.mv.db}, or {@code graves.data.old-yyyyMMdd-HHmmss.mv.db}
     * if that name is taken. If the move fails the migration carries on into the existing database.
     * </p>
     */
    static void moveAsideExistingH2(Graves plugin) {
        Path dataDir = dataDir(plugin);
        if (!Files.isRegularFile(dataDir.resolve(SQLITE_FILE))) return;

        Path h2Base = dataDir.resolve(H2_BASE_NAME);
        Path mvFile = h2File(h2Base, ".mv.db");
        if (!Files.isRegularFile(mvFile)) return;

        String suffix = ".old";
        if (Files.exists(dataDir.resolve(H2_BASE_NAME + ".old.mv.db"))) {
            suffix = ".old-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        }

        try {
            Path moved = dataDir.resolve(H2_BASE_NAME + suffix + ".mv.db");
            Files.move(mvFile, moved);

            Path traceFile = h2File(h2Base, ".trace.db");
            if (Files.isRegularFile(traceFile)) {
                Files.move(traceFile, dataDir.resolve(H2_BASE_NAME + suffix + ".trace.db"));
            }

            plugin.databaseDebug("Existing H2 database moved to " + moved.getFileName()
                    + " so the SQLite migration starts from a clean database.", 2);
            plugin.getLogger().warning("Existing H2 database moved to " + moved.getFileName()
                    + " so the SQLite migration starts from a clean database.");
        } catch (IOException exception) {
            plugin.databaseDebug("Could not move the existing H2 database aside; migrating into it instead.", 2);
            plugin.getLogger().warning("Could not move the existing H2 database aside; migrating into it instead.");
            plugin.logStackTrace(exception);
        }
    }

    /**
     * Renames {@code data.db} to {@code data.migrated.db}, together with any WAL sidecar files.
     */
    static void renameMigratedSQLite(Path sqliteFile) throws IOException {
        Path target = sqliteFile.resolveSibling("data.migrated.db");
        Files.move(sqliteFile, target, StandardCopyOption.REPLACE_EXISTING);

        for (String suffix : new String[]{"-wal", "-shm"}) {
            Path sidecar = sqliteFile.resolveSibling(sqliteFile.getFileName() + suffix);
            if (Files.exists(sidecar)) {
                Files.move(sidecar, target.resolveSibling(target.getFileName() + suffix), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Renames the H2 files ({@code .mv.db} and, if present, {@code .trace.db}) by appending {@code .migrated.db}.
     */
    static void renameMigratedH2(Path h2Base) throws IOException {
        renameWithSuffix(h2File(h2Base, ".mv.db"));
        renameWithSuffix(h2File(h2Base, ".trace.db"));
    }

    private static void renameWithSuffix(Path source) throws IOException {
        if (!Files.exists(source)) return;
        move(source, source.resolveSibling(source.getFileName() + ".migrated.db"), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void move(Path source, Path destination, StandardCopyOption... options) throws IOException {
        try {
            Files.move(source, destination, options.length == 0
                    ? new StandardCopyOption[]{StandardCopyOption.ATOMIC_MOVE}
                    : new StandardCopyOption[]{StandardCopyOption.ATOMIC_MOVE, options[0]});
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, options);
        }
    }
}