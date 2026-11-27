package dev.cwhead.GravesXCommon.libby;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;

import java.util.Objects;

/**
 * The one place GravesX loads runtime libraries with Libby, for the server and for every proxy. It is
 * platform independent: the caller supplies the {@link LibraryManager} for its platform
 * (BukkitLibraryManager, VelocityLibraryManager, StandaloneLibraryManager) and a {@link LibraryLog}.
 */
public final class LibraryLoader {
    private final LibraryManager manager;
    private final LibraryLog log;
    private boolean mavenCentralAdded;

    public LibraryLoader(LibraryManager manager, LibraryLog log) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.log = Objects.requireNonNull(log, "log");
    }

    /** Registers Maven Central once. Enough for everything the proxies load. */
    public LibraryLoader useMavenCentral() {
        if (!mavenCentralAdded) {
            addSafely("addMavenCentral", manager::addMavenCentral);
            mavenCentralAdded = true;
        }
        return this;
    }

    /**
     * Registers an optional custom repository followed by Maven Central, Sonatype, JCenter and JitPack.
     * A repository that fails to register is skipped with a warning.
     */
    public LibraryLoader useDefaultRepositories(String customUrl) {
        if (customUrl != null && !customUrl.isBlank()) {
            addSafely("custom repository '" + customUrl + "'", () -> manager.addRepository(customUrl));
        }
        useMavenCentral();
        addSafely("addSonatype", manager::addSonatype);
        addSafely("addJCenter", manager::addJCenter);
        addSafely("addJitPack", manager::addJitPack);
        return this;
    }

    /** Loads the library. Never throws; see {@link LoadResult}. */
    public LoadResult load(LibraryRequest request) {
        final String label = request.label();
        final boolean relocate = request.relocateFrom != null && !request.relocateFrom.isBlank();
        final boolean hasId = request.loaderId != null && !request.loaderId.isBlank();

        try {
            Library.Builder builder = Library.builder()
                    .groupId(request.groupId)
                    .artifactId(request.artifactId)
                    .version(request.version)
                    .resolveTransitiveDependencies(request.transitive);

            if (hasId) {
                builder.loaderId(request.loaderId);
            }
            if (relocate) {
                builder.relocate(request.relocateFrom, request.relocateTo);
            }
            if (request.isolated) {
                if (!hasId) {
                    builder.loaderId(request.artifactId + "-isolated");
                }
                builder.isolatedLoad(true);
            }

            final Library library = builder.build();
            if (library == null) {
                log.warn("Builder returned null Library for " + label + ", skipping.");
                return LoadResult.FAILED;
            }

            log.debug("Loading library " + label
                    + (request.isolated ? " [isolated]" : "")
                    + (relocate ? " [relocated]" : "")
                    + ((hasId || request.isolated) ? " (loaderId=" + library.getLoaderId() + ")" : ""));

            try {
                manager.loadLibrary(library);
            } catch (NullPointerException npe) {
                log.error("NullPointerException while loading library " + label
                        + ". Likely cause: missing repository, malformed library descriptor, or resolver returned null objects.", npe);
                return LoadResult.FAILED;
            } catch (Exception ex) {
                log.error("Exception while loading library " + label + ": " + ex.getClass().getSimpleName(), ex);
                return LoadResult.FAILED;
            }

            if (request.isolated && relocate) {
                return verifyRelocated(label, request.relocateTo);
            }

            log.debug("Loaded library " + label + ".");
            return LoadResult.LOADED;
        } catch (IllegalArgumentException iae) {
            log.error("Invalid arguments while preparing to load " + label + ": " + iae.getMessage(), iae);
            return LoadResult.FAILED;
        } catch (Exception e) {
            log.error("Failed to load " + label + ": " + e.getClass().getSimpleName(), e);
            return LoadResult.FAILED;
        }
    }

    /** Post-load check for isolated + relocated libraries (behaviour carried over from the server's loader). */
    private LoadResult verifyRelocated(String label, String relocateTo) {
        try {
            final String probe = relocateTo.replace("{}", ".");
            if (probe.indexOf('.') < 0) {
                log.warn("Relocation verification probe '" + probe + "' looks invalid for " + label + ", skipping verification.");
                return LoadResult.LOADED;
            }

            try {
                Class.forName(probe, false, Thread.currentThread().getContextClassLoader());
                log.debug("Verified shaded library " + label + ".");
                return LoadResult.LOADED;
            } catch (ClassNotFoundException cnfe) {
                log.error("Shaded verification failed for " + label + ": ClassNotFoundException", cnfe);
            } catch (Throwable th) {
                log.error("Shaded verification failed for " + label + ": " + th.getClass().getSimpleName(), th);
            }
        } catch (Throwable t) {
            log.error("Unexpected error during shaded verification for " + label + ": " + t.getClass().getSimpleName(), t);
        }
        return LoadResult.VERIFICATION_FAILED;
    }

    /** True if the class can already be loaded by the given class loader (so there is nothing to download). */
    public static boolean isPresent(String className, ClassLoader loader) {
        try {
            Class.forName(className, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private void addSafely(String what, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            log.warn(what + " failed: " + t.getClass().getSimpleName());
        }
    }
}
