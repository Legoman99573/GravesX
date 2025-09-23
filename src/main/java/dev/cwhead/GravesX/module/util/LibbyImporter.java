package dev.cwhead.GravesX.module.util;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.ModuleContext;
import dev.cwhead.GravesX.util.LibraryLoaderUtil;

import java.util.Locale;
import java.util.Objects;

/**
 * Implements a {@link LibraryImporter} that parses Maven-style coordinates and
 * loads libraries for modules using {@link LibraryLoaderUtil}.
 */
public final class LibbyImporter implements LibraryImporter {
    private final Graves plugin;
    private final LibraryLoaderUtil util;

    /**
     * Creates a new importer bound to the Graves plugin.
     *
     * @param plugin Owning Graves plugin.
     */
    public LibbyImporter(Graves plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.util = new LibraryLoaderUtil(plugin);
    }

    /**
     * Loads libraries described by coordinate strings.
     * Each coordinate is trimmed and ignored if null/empty.
     *
     * @param ctx Module context of the caller.
     * @param coordinates One or more coordinates in the form
     *                    {@code group:artifact:version[?key=value&...]}.
     */
    @Override
    public void importLibrary(ModuleContext ctx, String... coordinates) {
        if (coordinates == null) return;
        for (String spec : coordinates) {
            if (spec == null) continue;
            String s = spec.trim();
            if (s.isEmpty()) continue;
            loadOne(s);
        }
    }

    /**
     * Parses a single coordinate string and loads it.
     * Supports query keys: {@code isolated}, {@code transitive}, {@code repo},
     * {@code id}, {@code relocate} (format {@code from->to}), {@code relocateFrom}, {@code relocateto}.
     * On parse/load failure, logs the error and disables the plugin.
     *
     * @param spec Coordinate string to load.
     */
    private void loadOne(String spec) {
        try {
            String coords = spec;
            String query = null;
            int q = spec.indexOf('?');
            if (q >= 0) {
                coords = spec.substring(0, q);
                query = spec.substring(q + 1);
            }

            String[] parts = coords.split(":");
            if (parts.length < 3) throw new IllegalArgumentException("Invalid coordinates: " + spec);

            String group = parts[0];
            String artifact = parts[1];
            String version = parts[2];

            boolean isolated = false;
            boolean resolveTransitive = true;
            String relocateFrom = null;
            String relocateTo = null;
            String repo = null;
            String id = null;

            if (query != null) {
                String[] kvps = query.split("&");
                for (String kvp : kvps) {
                    int eq = kvp.indexOf('=');
                    String key = (eq >= 0 ? kvp.substring(0, eq) : kvp).trim().toLowerCase(Locale.ROOT);
                    String value = (eq >= 0 ? kvp.substring(eq + 1) : "").trim();
                    if ("isolated".equals(key)) isolated = parseBoolean(value);
                    else if ("transitive".equals(key)) resolveTransitive = parseBoolean(value);
                    else if ("repo".equals(key)) repo = value;
                    else if ("id".equals(key)) id = value;
                    else if ("relocate".equals(key)) {
                        int arrow = value.indexOf("->");
                        if (arrow > 0) {
                            relocateFrom = value.substring(0, arrow).trim();
                            relocateTo = value.substring(arrow + 2).trim();
                        }
                    } else if ("relocatefrom".equals(key)) relocateFrom = value;
                    else if ("relocateto".equals(key)) relocateTo = value;
                }
            }

            if (id != null || repo != null) {
                util.loadLibrary(group, artifact, version, id, relocateFrom, relocateTo, isolated, repo, resolveTransitive);
            } else if (relocateFrom != null || relocateTo != null) {
                util.loadLibrary(group, artifact, version, relocateFrom, relocateTo, isolated, resolveTransitive);
            } else if (isolated) {
                util.loadLibrary(group, artifact, version, true);
            } else {
                util.loadLibrary(group, artifact, version);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse or load library spec: " + spec + " -> " + e.getMessage());
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * Parses a human-friendly boolean string.
     * Truthy: {@code "1"}, {@code "true"}, {@code "yes"}, {@code "y"} (case-insensitive).
     *
     * @param s String to parse.
     * @return True if the value is truthy, otherwise false.
     */
    private static boolean parseBoolean(String s) {
        String v = s.toLowerCase(Locale.ROOT);
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "y".equals(v);
    }
}