package dev.cwhead.GravesX.manager;

import com.ranull.graves.Graves;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Manages registration of bStats metrics for GravesX.
 */
public class MetricsManager {

    private final Graves plugin;

    /**
     * Creates a new metrics manager.
     *
     * @param plugin the GravesX plugin instance
     */
    public MetricsManager(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers bStats metrics under the GravesX bStats plugin ID.
     *
     * <p>This method creates a new {@link Metrics} instance and adds the plugin's custom charts.</p>
     */
    public void registerMetrics() {
        final int id = getMetricsID();
        final String url = getMetricsUrl(id);

        try {
            Metrics metrics = new Metrics(plugin, id);

            metrics.addCustomChart(new SingleLineChart("graves",
                    () -> plugin.getCacheManager().getGraveMap().size()));

            metrics.addCustomChart(new SimplePie("permission_handler", () -> {
                if (plugin.getIntegrationManager().hasLuckPermsHandler()) {
                    return "LuckPerms";
                } else if (plugin.getIntegrationManager().hasVaultPermProvider()) {
                    return "Vault";
                } else {
                    return "Bukkit";
                }
            }));

            metrics.addCustomChart(new SimplePie("database",
                    () -> plugin.getDataManager().getType()));

            metrics.addCustomChart(new SimplePie("plugin_release", () -> {
                if (plugin.isPluginDevelopmentBuild()) {
                    return "Development Build";
                } else if (plugin.isPluginOutdatedBuild()) {
                    return "Outdated Build";
                } else if (plugin.isPluginUnknownBuild()) {
                    return "Unknown Build";
                } else {
                    return "Production Build";
                }
            }));

            metrics.addCustomChart(new DrilldownPie("database_versions",
                    (Callable<Map<String, Map<String, Integer>>>) () -> plugin.getDataManager().getDatabaseVersions()));

            plugin.getLogger().info("bStats metrics loaded: " + url);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to load bStats metrics. Stats won't be sent to: " + url);
        }
    }

    /**
     * Registers bStats metrics under the legacy Graves bStats plugin ID.
     *
     * <p>This is intended to preserve continuity with older plugin versions that reported
     * to the legacy bStats listing, while still emitting the same set of charts.</p>
     */
    public void registerMetricsLegacy() {
        final int id = getMetricsIDLegacy();
        final String url = getMetricsUrl(id);

        try {
            Metrics metricsLegacy = new Metrics(plugin, id);

            metricsLegacy.addCustomChart(new SingleLineChart("graves",
                    () -> plugin.getCacheManager().getGraveMap().size()));

            metricsLegacy.addCustomChart(new SimplePie("permission_handler", () -> {
                if (plugin.getIntegrationManager().hasLuckPermsHandler()) {
                    return "LuckPerms";
                } else if (plugin.getIntegrationManager().hasVaultPermProvider()) {
                    return "Vault";
                } else {
                    return "Bukkit";
                }
            }));

            metricsLegacy.addCustomChart(new SimplePie("database",
                    () -> plugin.getDataManager().getType()));

            metricsLegacy.addCustomChart(new SimplePie("plugin_release", () -> {
                if (plugin.isPluginDevelopmentBuild()) {
                    return "Development Build";
                } else if (plugin.isPluginOutdatedBuild()) {
                    return "Outdated Build";
                } else if (plugin.isPluginOutdatedBuild()) {
                    return "Outdated Build";
                } else if (plugin.isPluginUnknownBuild()) {
                    return "Unknown Build";
                } else {
                    return "Production Build";
                }
            }));

            metricsLegacy.addCustomChart(new DrilldownPie("database_versions",
                    (Callable<Map<String, Map<String, Integer>>>) () -> plugin.getDataManager().getDatabaseVersions()));

            plugin.getLogger().info("bStats legacy metrics loaded: " + url);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to load bStats legacy metrics. Stats won't be sent to: " + url);
        }
    }

    /**
     * @return the bStats plugin ID used for usage metrics.
     */
    public final int getMetricsID() {
        return 23069; // https://bstats.org/plugin/bukkit/GravesX/23069
    }

    /**
     * @return the legacy bStats plugin ID (for previous plugin versions).
     */
    public final int getMetricsIDLegacy() {
        return 12849; // https://bstats.org/plugin/bukkit/Graves/12849
    }

    private String getMetricsUrl(int id) {
        return "https://bstats.org/plugin/bukkit/" + (id == getMetricsID() ? "GravesX" : "Graves") + "/" + id;
    }
}