package dev.cwhead.GravesX.manager;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orders hologram creation against hologram removal for the same grave.
 */
public final class HologramEpoch {
    private static final int TRIM_THRESHOLD = 20_000;
    private static final Map<UUID, AtomicLong> EPOCHS = new ConcurrentHashMap<>();

    private HologramEpoch() {}

    /**
     * Starts a new hologram generation for a grave.
     *
     * @param graveUUID the grave
     * @return the epoch the queued spawn must still be current for
     */
    public static long begin(UUID graveUUID) {
        return counter(graveUUID).incrementAndGet();
    }

    /**
     * Cancels every spawn queued for the grave that has not run yet.
     *
     * @param graveUUID the grave
     */
    public static void invalidate(UUID graveUUID) {
        counter(graveUUID).incrementAndGet();
    }

    /**
     * Whether a queued spawn is still wanted.
     *
     * @param graveUUID the grave
     * @param epoch     the epoch returned by {@link #begin(UUID)}
     * @return true if no removal or newer creation has happened since
     */
    public static boolean isCurrent(UUID graveUUID, long epoch) {
        AtomicLong counter = EPOCHS.get(graveUUID);

        return counter == null || counter.get() == epoch;
    }

    private static AtomicLong counter(UUID graveUUID) {
        if (EPOCHS.size() > TRIM_THRESHOLD) {
            Iterator<UUID> iterator = EPOCHS.keySet().iterator();
            int toRemove = TRIM_THRESHOLD / 2;

            while (toRemove-- > 0 && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }

        return EPOCHS.computeIfAbsent(graveUUID, id -> new AtomicLong());
    }
}
