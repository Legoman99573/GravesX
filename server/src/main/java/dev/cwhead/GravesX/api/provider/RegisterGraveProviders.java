package dev.cwhead.GravesX.api.provider;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility for discovering {@link GraveProvider} implementations that have been registered
 * with the Bukkit Services API. Providers are returned in ascending {@linkplain GraveProvider#order() order}
 * so lower numbers (higher priority) come first.
 *
 * <p>This method takes a snapshot of the currently-registered providers each time it is called.
 * It does not track future registrations/unregistrations.</p>
 *
 * @since 4.9.9.1
 */
public class RegisterGraveProviders {
    /**
     * Static-only utility; prevent instantiation.
     */
    private RegisterGraveProviders() {}

    private static final Comparator<GraveProvider> ORDERING = Comparator.comparingInt(GraveProvider::order).thenComparing(GraveProvider::id, String.CASE_INSENSITIVE_ORDER);

    /**
     * Normalized providerId -> provider (O(1) lookup for removal path).
     */
    private static final ConcurrentMap<String, GraveProvider> BY_ID = new ConcurrentHashMap<>();

    private static String norm(String id) {
        if (id == null) return null;
        String s = id.trim();
        if (s.isEmpty()) return null;
        return s.toLowerCase(Locale.ROOT);
    }

    /**
     * One-time (or occasional) rebuild from Bukkit Services.
     * Call after modules/providers have registered their services.
     *
     * This performs the loop ONCE here, so removeGrave() never has to.
     */
    public static void bootstrapFromServices() {
        rebuildIndexFromServices();
    }

    /**
     * If you control registration code, call this right after you register the provider.
     * (Avoids needing to call bootstrapFromServices again.)
     */
    public static void index(GraveProvider provider) {
        if (provider == null) return;
        String id = norm(provider.id());
        if (id == null) return;
        BY_ID.put(id, provider);
    }

    /**
     * If you control unregistration/disable, call this to keep the index clean.
     */
    public static void unindex(GraveProvider provider) {
        if (provider == null) return;
        String id = norm(provider.id());
        if (id == null) return;
        BY_ID.remove(id, provider);
    }

    /**
     * Clear all cached/indexed providers (call on plugin disable, or before a rebuild if you want).
     */
    public static void clear() {
        BY_ID.clear();
    }

    /**
     * O(1) lookup by provider id.
     * Intended for hot paths (like removeGrave).
     */
    public static GraveProvider getById(String providerId) {
        String id = norm(providerId);
        if (id == null) return null;
        return BY_ID.get(id);
    }

    /**
     * Returns every provider at the highest priority tier (the minimal order value).
     * If none are registered, returns an empty list.
     */
    public static List<GraveProvider> getHighestPriorityAll() {
        List<GraveProvider> all = getAll();
        if (all.isEmpty()) return List.of();
        final int best = all.get(0).order();

        List<GraveProvider> out = new ArrayList<>();
        for (GraveProvider p : all) {
            if (p.order() != best) break;
            out.add(p);
        }
        return List.copyOf(out);
    }

    /**
     * Retrieves all registered {@link GraveProvider} instances from the Bukkit Services registry,
     * sorted by ascending {@linkplain GraveProvider#order() order}. The returned list is immutable.
     *
     * NOTE: This remains a snapshot (loops). That's fine outside hot removal paths.
     * As a bonus, this also refreshes the id index.
     */
    public static List<GraveProvider> getAll() {
        Collection<RegisteredServiceProvider<GraveProvider>> regs =
                Bukkit.getServicesManager().getRegistrations(GraveProvider.class);

        List<GraveProvider> out = new ArrayList<>(regs.size());
        for (RegisteredServiceProvider<GraveProvider> rsp : regs) {
            GraveProvider p = rsp.getProvider();
            out.add(p);
        }
        out.sort(ORDERING);
        rebuildIndex(out);

        return List.copyOf(out);
    }

    /**
     * Returns a single highest-priority provider (arbitrary among ties).
     */
    public static Optional<GraveProvider> getHighestPriorityOne() {
        List<GraveProvider> all = getAll();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    private static void rebuildIndexFromServices() {
        Collection<RegisteredServiceProvider<GraveProvider>> regs = Bukkit.getServicesManager().getRegistrations(GraveProvider.class);

        List<GraveProvider> providers = new ArrayList<>(regs.size());
        for (RegisteredServiceProvider<GraveProvider> rsp : regs) {
            GraveProvider p = rsp.getProvider();
            providers.add(p);
        }
        rebuildIndex(providers);
    }

    private static void rebuildIndex(List<GraveProvider> providers) {
        BY_ID.clear();

        for (GraveProvider p : providers) {
            String id = norm(p.id());
            if (id == null) continue;
            BY_ID.put(id, p);
        }
    }
}