package dev.cwhead.GravesX.module.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable descriptor of a module parsed from {@code module.yml}.
 * Holds IDs, main class, version, and plugin/module dependency lists.
 */
public final class ModuleInfo {
    private final String name, mainClass, version;
    private final List<String> pluginDepends, pluginSoftDepends, pluginLoadBefore;
    private final List<String> moduleDepends, moduleSoftDepends, moduleLoadBefore;

    private ModuleInfo(
            String name, String mainClass, String version,
            List<String> pDep, List<String> pSoft, List<String> pBefore,
            List<String> mDep, List<String> mSoft, List<String> mBefore
    ) {
        this.name = name;
        this.mainClass = mainClass;
        this.version = (version == null || version.isBlank()) ? "0.0.0" : version;
        this.pluginDepends = List.copyOf(pDep);
        this.pluginSoftDepends = List.copyOf(pSoft);
        this.pluginLoadBefore = List.copyOf(pBefore);
        this.moduleDepends = List.copyOf(mDep);
        this.moduleSoftDepends = List.copyOf(mSoft);
        this.moduleLoadBefore = List.copyOf(mBefore);
    }

    /**
     * Gets the module name.
     *
     * @return Module name or {@code null} if not provided.
     */
    public String name() { return name; }

    /**
     * Gets the fully qualified main class.
     *
     * @return Main class name or {@code null} if not provided.
     */
    public String mainClass() { return mainClass; }

    /**
     * Gets the module version.
     *
     * @return Version string (defaults to {@code "0.0.0"} if missing).
     */
    public String version() { return version; }

    /**
     * Gets required Bukkit plugin dependencies.
     *
     * @return Unmodifiable list of plugin names.
     */
    public List<String> pluginDepends() { return pluginDepends; }

    /**
     * Gets optional Bukkit plugin dependencies.
     *
     * @return Unmodifiable list of plugin names.
     */
    public List<String> pluginSoftDepends() { return pluginSoftDepends; }

    /**
     * Gets plugins that should load after this module.
     *
     * @return Unmodifiable list of plugin names.
     */
    public List<String> pluginLoadBefore() { return pluginLoadBefore; }

    /**
     * Gets required module dependencies.
     *
     * @return Unmodifiable list of module names.
     */
    public List<String> moduleDepends() { return moduleDepends; }

    /**
     * Gets optional module dependencies.
     *
     * @return Unmodifiable list of module names.
     */
    public List<String> moduleSoftDepends() { return moduleSoftDepends; }

    /**
     * Gets modules that should load after this module.
     *
     * @return Unmodifiable list of module names.
     */
    public List<String> moduleLoadBefore() { return moduleLoadBefore; }

    /**
     * Parses a minimal YAML-like stream into a {@link ModuleInfo}.
     * Supports keys: {@code name}, {@code main}, {@code version},
     * {@code pluginDepends}, {@code pluginSoftDepends}, {@code pluginLoadBefore},
     * {@code moduleDepends}, {@code moduleSoftDepends}, {@code moduleLoadBefore}.
     * Lists may be comma-separated or {@code - item} lines; comments (#) and blanks are ignored.
     *
     * @param in Input stream of {@code module.yml}. Must not be {@code null}.
     * @return Parsed module info.
     * @throws Exception If reading or parsing fails.
     */
    public static ModuleInfo fromYaml(InputStream in) throws Exception {
        String text = new String(in.readAllBytes());
        Map<String, List<String>> lists = new HashMap<String, List<String>>();
        Map<String, String> scalars = new HashMap<String, String>();
        String currentList = null;
        Pattern keyLine = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*\\s*:");

        Set<String> listKeys = new HashSet<String>();
        listKeys.add("plugindepends");
        listKeys.add("pluginsoftdepends");
        listKeys.add("pluginloadbefore");
        listKeys.add("moduledepends");
        listKeys.add("modulesoftdepends");
        listKeys.add("moduleloadbefore");

        String[] lines = text.split("\\R");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (keyLine.matcher(line).find()) {
                int idx = line.indexOf(':');
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                String k = key.toLowerCase(Locale.ROOT);
                currentList = null;

                boolean isListKey = listKeys.contains(k);

                if (val.isEmpty()) {
                    if (isListKey) {
                        if (!lists.containsKey(k)) lists.put(k, new ArrayList<String>());
                        currentList = k;
                    } else {
                        scalars.put(k, "");
                    }
                } else {
                    if (isListKey) {
                        List<String> arr = new ArrayList<String>();
                        String[] parts = val.split(",");
                        for (String p : parts) {
                            String v = p.trim();
                            if (!v.isEmpty()) arr.add(v);
                        }
                        lists.put(k, arr);
                    } else {
                        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2)
                            val = val.substring(1, val.length() - 1);
                        scalars.put(k, val);
                    }
                }
                continue;
            }

            if (line.startsWith("-") && currentList != null) {
                String v = line.substring(1).trim();
                if (!v.isEmpty()) lists.get(currentList).add(v);
            }
        }

        String name  = nv(scalars.get("name"));
        String main  = nv(scalars.get("main"));
        String ver   = scalars.getOrDefault("version", "0.0.0");

        return new ModuleInfo(
                name, main, ver,
                lists.getOrDefault("plugindepends", List.of()),
                lists.getOrDefault("pluginsoftdepends", List.of()),
                lists.getOrDefault("pluginloadbefore", List.of()),
                lists.getOrDefault("moduledepends", List.of()),
                lists.getOrDefault("modulesoftdepends", List.of()),
                lists.getOrDefault("moduleloadbefore", List.of())
        );
    }

    private static String nv(String s) { return (s == null || s.isBlank()) ? null : s; }
}