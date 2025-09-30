package dev.cwhead.GravesX.api.provider;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

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
public final class RegisterGraveProviders {
    /**
     * Static-only utility; prevent instantiation.
     */
    private RegisterGraveProviders() {}

    /**
     * Returns every provider at the highest priority tier (the minimal order value).
     * If none are registered, returns an empty list.
     */
    public static List<GraveProvider> getHighestPriorityAll() {
        List<GraveProvider> all = getAll();
        if (all.isEmpty()) return List.of();
        final int best = all.get(0).order();
        return all.stream()
                .takeWhile(p -> p.order() == best)
                .toList();
    }

    /**
     * Retrieves all registered {@link GraveProvider} instances from the Bukkit Services registry,
     * sorted by ascending {@linkplain GraveProvider#order() order}. The returned list is immutable.
     *
     * @return an unmodifiable, sorted list of discovered providers (possibly empty, never {@code null})
     */
    public static List<GraveProvider> getAll() {
        Collection<RegisteredServiceProvider<GraveProvider>> regs =
                Bukkit.getServicesManager().getRegistrations(GraveProvider.class);

        List<GraveProvider> out = new ArrayList<>(regs.size());
        for (RegisteredServiceProvider<GraveProvider> rsp : regs) {
            out.add(rsp.getProvider());
        }
        out.sort(Comparator.comparingInt(GraveProvider::order));
        return List.copyOf(out);
    }

    /**
     * Returns a single highest-priority provider (arbitrary among ties).
     */
    public static Optional<GraveProvider> getHighestPriorityOne() {
        List<GraveProvider> all = getAll();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }
}