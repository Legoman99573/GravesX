package dev.cwhead.GravesXProxy.storage;

import dev.cwhead.GravesXProxy.protocol.Protocol.GraveSummary;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Network-wide record of graves, keyed by server + grave (worlds are only a property of a grave, so the same
 * world name may exist on several servers). Implementations may block; callers use a background thread.
 * Server names are compared case-insensitively.
 */
public interface GraveStore {
    /** Replaces everything known about one owner's graves on one server with this snapshot. */
    void replaceOwner(String server, UUID owner, List<GraveSummary> graves) throws Exception;

    /** Grave count per server (lower-case server name -> count) for an owner. */
    Map<String, Integer> counts(UUID owner) throws Exception;

    /** Newest-first graves of an owner on one server. */
    List<GraveSummary> graves(UUID owner, String server, int limit) throws Exception;

    /** Removes all graves recorded for a server. */
    void forgetServer(String server) throws Exception;

    /** Persists to disk where the store is file based. */
    default void save() {}

    default void close() {}

    /** Short human readable description for /gravesproxy status. */
    String describe();
}
