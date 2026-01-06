package dev.cwhead.GravesX.manager;

import com.ranull.graves.Graves;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Chunk manager that provides cross-platform chunk helpers for Spigot, Paper, and Folia.
 */
public final class ChunkManager {

    /**
     * Represents the chunk/runtime type detected for the current server.
     */
    public enum ChunkType {
        /**
         * Chunk Type is handled as Spigot.
         */
        SPIGOT,
        /**
         * Chunk Type is handled as Paper.
         */
        PAPER,
        /**
         * Chunk Type is handled as Folia.
         */
        FOLIA,
        /**
         * Unknown runtime; treat as SPIGOT for safety.
         */
        UNKNOWN;

        /**
         * Returns the effective type, mapping UNKNOWN to SPIGOT.
         *
         * @return effective type
         */
        public ChunkType effective() {
            return this == UNKNOWN ? SPIGOT : this;
        }
    }

    /**
     * Allows external APIs (your own integrations/modules) to provide the runtime chunk type.
     *
     * <p>Return null if you don't know. The first non-null result wins.</p>
     */
    @FunctionalInterface
    public interface ChunkTypeResolver {
        ChunkType resolve(Graves plugin);
    }

    private final Graves plugin;
    private final List<ChunkTypeResolver> resolvers = new CopyOnWriteArrayList<>();
    private volatile ChunkType overrideType;
    private volatile ChunkType cachedDetectedType;

    /**
     * Creates a new chunk manager.
     *
     * @param plugin the GravesX plugin instance
     */
    public ChunkManager(Graves plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Registers a resolver that can determine the ChunkType using optional APIs.
     *
     * @param resolver resolver
     */
    public void registerChunkTypeResolver(ChunkTypeResolver resolver) {
        if (resolver == null) return;
        resolvers.add(resolver);
        cachedDetectedType = null;
    }

    /**
     * Sets a hard override type.
     *
     * @param type override
     */
    public void setChunkTypeOverride(ChunkType type) {
        this.overrideType = type;
        cachedDetectedType = null;
    }

    /**
     * Gets the detected chunk/runtime type.
     *
     * @return chunk type
     */
    public ChunkType getChunkType() {
        ChunkType override = this.overrideType;
        if (override != null) {
            return override;
        }

        ChunkType cached = this.cachedDetectedType;
        if (cached != null) {
            return cached;
        }

        ChunkType detected = detectChunkType();
        this.cachedDetectedType = detected;
        return detected;
    }

    /**
     * Ensures the chunk containing {@code location} is loaded, then executes {@code task}
     * on the correct scheduler for the runtime
     *
     * @param anchor         location inside the target chunk (used for Folia region execution). If null, {@code location} is used.
     * @param location       target location
     * @param generate       whether to generate the chunk if needed
     * @param allowForceLoad only applies on non-Folia (safe there)
     * @param task           task to run after the chunk is loaded
     * @return true if work was scheduled or completed, false if refused (Folia w/o async API or invalid location)
     */
    public boolean ensureLoadedAndExecute(Location anchor,
                                          Location location,
                                          boolean generate,
                                          boolean allowForceLoad,
                                          Runnable task) {

        if (location == null) return false;

        World world = location.getWorld();
        if (world == null) return false;

        if (anchor == null) {
            anchor = location;
        }

        final Location useAnchor = anchor;
        final int chunkX = location.getBlockX() >> 4;
        final int chunkZ = location.getBlockZ() >> 4;

        if (world.isChunkLoaded(chunkX, chunkZ)) {
            execute(useAnchor, task);
            return true;
        }

        ChunkType type = getChunkType().effective();
        boolean folia = type == ChunkType.FOLIA;

        CompletableFuture<?> async = tryGetChunkAtAsync(world, chunkX, chunkZ, generate);
        if (async != null) {
            async.whenComplete((ok, ex) -> {
                if (ex != null) {
                    if (!folia) {
                        runMainThreadLoad(world, chunkX, chunkZ, generate, allowForceLoad, () -> execute(useAnchor, task));
                    } else {
                        plugin.getLogger().severe(ex.getMessage());
                        plugin.logStackTrace(ex);
                    }
                    return;
                }
                execute(useAnchor, task);
            });
            return true;
        }

        if (folia) {
            return false;
        }

        runMainThreadLoad(world, chunkX, chunkZ, generate, allowForceLoad, () -> execute(useAnchor, task));
        return true;
    }

    /**
     * Executes a task on the correct scheduler for this runtime.
     *
     * @param anchor anchor location for Folia region execution
     * @param task task to run
     */
    private void execute(Location anchor, Runnable task) {
        if (getChunkType().effective() == ChunkType.FOLIA) {
            plugin.getGravesXScheduler().execute(anchor, task);
        } else {
            plugin.getGravesXScheduler().runTask(task);
        }
    }

    /**
     * Detects the current server runtime: FOLIA, PAPER, SPIGOT, or UNKNOWN.
     */
    private ChunkType detectChunkType() {
        for (ChunkTypeResolver resolver : resolvers) {
            try {
                ChunkType resolved = resolver.resolve(plugin);
                if (resolved != null) {
                    return resolved;
                }
            } catch (Throwable t) {
                plugin.getLogger().severe(t.getMessage());
                plugin.logStackTrace(t);
            }
        }

        try {
            if (plugin.getVersionManager().isFolia()) {
                return ChunkType.FOLIA;
            }
            if (plugin.getVersionManager().isPaper()) {
                return ChunkType.PAPER;
            }
            return ChunkType.SPIGOT;
        } catch (Throwable t) {
            plugin.getLogger().severe(t.getMessage());
            plugin.logStackTrace(t);
        }

        try {
            Method m = World.class.getMethod("getChunkAtAsync", int.class, int.class, boolean.class, boolean.class);
            if (m != null) {
                return ChunkType.PAPER;
            }
        } catch (Throwable ignored) {
        }

        return ChunkType.UNKNOWN;
    }

    private CompletableFuture<?> tryGetChunkAtAsync(World world, int chunkX, int chunkZ, boolean generate) {
        try {
            Method m = world.getClass().getMethod(
                    "getChunkAtAsync",
                    int.class, int.class, boolean.class, boolean.class
            );

            Object result = m.invoke(world, chunkX, chunkZ, generate, true);
            if (result instanceof CompletableFuture<?> cf) {
                return cf;
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            plugin.getLogger().severe(t.getMessage());
            plugin.logStackTrace(t);
        }
        return null;
    }

    private void runMainThreadLoad(World world,
                                   int chunkX,
                                   int chunkZ,
                                   boolean generate,
                                   boolean allowForceLoad,
                                   Runnable onLoaded) {

        plugin.getGravesXScheduler().runTask(() -> {
            Chunk chunk = null;
            boolean didForceLoad = false;

            try {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    world.loadChunk(chunkX, chunkZ, false);
                }
                if (!world.isChunkLoaded(chunkX, chunkZ) && generate) {
                    world.loadChunk(chunkX, chunkZ, true);
                }

                if (allowForceLoad) {
                    try {
                        chunk = world.getChunkAt(chunkX, chunkZ);
                        if (!chunk.isForceLoaded()) {
                            chunk.setForceLoaded(true);
                            didForceLoad = true;
                        }
                    } catch (Throwable t) {
                        plugin.getLogger().severe(t.getMessage());
                        plugin.logStackTrace(t);
                    }
                }

                onLoaded.run();

            } catch (Throwable t) {
                plugin.getLogger().severe(t.getMessage());
                plugin.logStackTrace(t);
                onLoaded.run();
            } finally {
                if (didForceLoad) {
                    try {
                        chunk.setForceLoaded(false);
                    } catch (Throwable t) {
                        plugin.getLogger().severe(t.getMessage());
                        plugin.logStackTrace(t);
                    }
                }
            }
        });
    }
}
