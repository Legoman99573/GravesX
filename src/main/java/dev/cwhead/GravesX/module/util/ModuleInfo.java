package dev.cwhead.GravesX.module.util;

import dev.cwhead.GravesX.module.GravesXModuleController;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable descriptor of a module parsed from {@code module.yml}.
 *
 * <p>Holds name, description, main class, version, authors, website,
 * plugin/module dependency lists, Folia support flag, declared load phase,
 * libraries, and simple permission/command metadata.</p>
 */
public final class ModuleInfo {

    /**
     * Simple immutable description of a permission from module.yml:
     *
     * <pre>
     * permissions:
     *   graves.example.permission:
     *     description: "Allows use of example action"
     *     default: true
     * </pre>
     */
    public static final class PermissionDef {
        private final String node;
        private final String description;
        private final String defaultValue;

        public PermissionDef(String node, String description, String defaultValue) {
            this.node = node;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        /** Permission node string, e.g. {@code graves.example.permission}. */
        public String node() {
            return node;
        }

        /** Human-friendly description, or {@code null} if not provided. */
        public String description() {
            return description;
        }

        /**
         * Default value as string (e.g. {@code "true"}, {@code "false"},
         * {@code "op"}, {@code "not op"}), or {@code null} if absent.
         */
        public String defaultValue() {
            return defaultValue;
        }
    }

    /**
     * Simple immutable description of a command from module.yml:
     *
     * <pre>
     * commands:
     *   example:
     *     description: "Runs example command"
     *     usage: "/example [arg]"
     *     permission: "graves.example.command"
     *     aliases: ["ex"]
     *     executor: dev.cwhead.GravesX.modules.example.command.GxCommand
     *     tab-completer: dev.cwhead.GravesX.modules.example.command.GxTab
     * </pre>
     */
    public static final class CommandDef {
        private final String name;
        private final String description;
        private final String usage;
        private final String permission;
        private final List<String> aliases;
        private final String executor;
        private final String tabCompleter;

        public CommandDef(
                String name,
                String description,
                String usage,
                String permission,
                List<String> aliases,
                String executor,
                String tabCompleter
        ) {
            this.name = name;
            this.description = description;
            this.usage = usage;
            this.permission = permission;
            this.aliases = List.copyOf(aliases == null ? List.of() : aliases);
            this.executor = executor;
            this.tabCompleter = tabCompleter;
        }

        /** Command name (as defined under {@code commands:}). */
        public String name() {
            return name;
        }

        /** Command description, or {@code null} if not provided. */
        public String description() {
            return description;
        }

        /** Usage string, e.g. {@code "/example [arg]"} or {@code null}. */
        public String usage() {
            return usage;
        }

        /** Required permission node, or {@code null} if not specified. */
        public String permission() {
            return permission;
        }

        /** Aliases for this command (may be empty). */
        public List<String> aliases() {
            return aliases;
        }

        /** Executor class name (if provided). */
        public String executor() {
            return executor;
        }

        /** Tab completer class name (if provided). */
        public String tabCompleter() {
            return tabCompleter;
        }
    }

    /**
     * Immutable descriptor for a library entry under {@code libraries:} in module.yml.
     *
     * <pre>
     * libraries:
     *   - com.squareup.okhttp3:okhttp:4.9.0
     *   - old.loc.library:library:1.0.0
     *     relocatefrom: "old.loc.library"
     *     relocateTo: "new.loc.library"
     *     isIsolated: true
     *     useTransitive: false
     *     repo: "repo.url"
     *     id: "id"
     * </pre>
     */
    public static final class LibraryDef {
        private final String coordinates;
        private final String relocateFrom;
        private final String relocateTo;
        private final Boolean isIsolated;
        private final Boolean useTransitive;
        private final String repo;
        private final String id;

        public LibraryDef(
                String coordinates,
                String relocateFrom,
                String relocateTo,
                Boolean isIsolated,
                Boolean useTransitive,
                String repo,
                String id
        ) {
            this.coordinates = coordinates;
            this.relocateFrom = relocateFrom;
            this.relocateTo = relocateTo;
            this.isIsolated = isIsolated;
            this.useTransitive = useTransitive;
            this.repo = repo;
            this.id = id;
        }

        /** Maven coordinates, e.g. {@code group:artifact:version}. */
        public String coordinates() {
            return coordinates;
        }

        /** Relocation source package, or {@code null}. */
        public String relocateFrom() {
            return relocateFrom;
        }

        /** Relocation target package, or {@code null}. */
        public String relocateTo() {
            return relocateTo;
        }

        /** Whether this library should be isolated, or {@code null} if unspecified. */
        public Boolean isIsolated() {
            return isIsolated;
        }

        /** Whether transitive dependencies should be resolved, or {@code null} if unspecified. */
        public Boolean useTransitive() {
            return useTransitive;
        }

        /** Optional repository URL, or {@code null}. */
        public String repo() {
            return repo;
        }

        /** Optional repository id/name, or {@code null}. */
        public String id() {
            return id;
        }
    }

    private final String name, description, mainClass, version, website;
    private final List<String> authors;
    private final List<String> pluginDepends, pluginSoftDepends, pluginLoadBefore;
    private final List<String> moduleDepends, moduleSoftDepends, moduleLoadBefore;
    private final boolean supportsFolia;
    private final GravesXModuleController.LoadPhase loadPhase;
    private final Map<String, PermissionDef> permissions;
    private final Map<String, CommandDef> commands;
    private final List<LibraryDef> libraries;

    private ModuleInfo(
            String name,
            String description,
            String mainClass,
            String version,
            String website,
            List<String> authors,
            List<String> pDep,
            List<String> pSoft,
            List<String> pBefore,
            List<String> mDep,
            List<String> mSoft,
            List<String> mBefore,
            boolean supportsFolia,
            GravesXModuleController.LoadPhase loadPhase,
            Map<String, PermissionDef> permissions,
            Map<String, CommandDef> commands,
            List<LibraryDef> libraries
    ) {
        this.name = name;
        this.description = nv(description);
        this.mainClass = mainClass;
        this.version = (version == null || version.isBlank()) ? "0.0.0" : version;
        this.website = nv(website);
        this.authors = List.copyOf(authors == null ? List.of() : authors);
        this.pluginDepends = List.copyOf(pDep);
        this.pluginSoftDepends = List.copyOf(pSoft);
        this.pluginLoadBefore = List.copyOf(pBefore);
        this.moduleDepends = List.copyOf(mDep);
        this.moduleSoftDepends = List.copyOf(mSoft);
        this.moduleLoadBefore = List.copyOf(mBefore);
        this.supportsFolia = supportsFolia;
        this.loadPhase = (loadPhase != null) ? loadPhase : GravesXModuleController.LoadPhase.COMPLETED;
        this.permissions = Map.copyOf(permissions == null ? Map.of() : permissions);
        this.commands = Map.copyOf(commands == null ? Map.of() : commands);
        this.libraries = List.copyOf(libraries == null ? List.of() : libraries);
    }

    /** Gets the module name, or {@code null} if not provided. */
    public String name() {
        return name;
    }

    /** Gets the module description (from {@code description} in module.yml). */
    public String description() {
        return description;
    }

    /** Gets the fully qualified main class, or {@code null} if not provided. */
    public String mainClass() {
        return mainClass;
    }

    /** Gets the module version (defaults to {@code "0.0.0"} if missing). */
    public String version() {
        return version;
    }

    /** Gets the website URL for this module, or {@code null} if not provided. */
    public String website() {
        return website;
    }

    /** Gets authors of this module (may be empty, never {@code null}). */
    public List<String> authors() {
        return authors;
    }

    /** Required Bukkit plugin dependencies. */
    public List<String> pluginDepends() {
        return pluginDepends;
    }

    /** Optional Bukkit plugin dependencies. */
    public List<String> pluginSoftDepends() {
        return pluginSoftDepends;
    }

    /** Plugins that should load after this module. */
    public List<String> pluginLoadBefore() {
        return pluginLoadBefore;
    }

    /** Required module dependencies. */
    public List<String> moduleDepends() {
        return moduleDepends;
    }

    /** Optional module dependencies. */
    public List<String> moduleSoftDepends() {
        return moduleSoftDepends;
    }

    /** Modules that should load after this module. */
    public List<String> moduleLoadBefore() {
        return moduleLoadBefore;
    }

    /**
     * Whether this module declares Folia support via {@code supportsFolia: true}.
     *
     * @return {@code true} if {@code supportsFolia} is explicitly true; otherwise {@code false}.
     */
    public boolean supportsFolia() {
        return supportsFolia;
    }

    /**
     * Declared enable phase for this module via {@code load:}.
     *
     * <p>Valid values are {@code STARTUP}, {@code POSTWORLD}, {@code COMPLETED}.
     * If missing or invalid, defaults to {@link GravesXModuleController.LoadPhase#COMPLETED}.</p>
     *
     * @return declared load phase (never {@code null})
     */
    public GravesXModuleController.LoadPhase loadPhase() {
        return loadPhase;
    }

    /**
     * Libraries declared under {@code libraries:} in module.yml.
     *
     * @return immutable list of library definitions (may be empty, never {@code null})
     */
    public List<LibraryDef> libraries() {
        return libraries;
    }

    /**
     * Returns permissions defined in {@code permissions:} in module.yml,
     * keyed by permission node.
     */
    public Map<String, PermissionDef> permissions() {
        return permissions;
    }

    /**
     * Returns commands defined in {@code commands:} in module.yml,
     * keyed by command name.
     */
    public Map<String, CommandDef> commands() {
        return commands;
    }

    /**
     * Parses a minimal YAML-like stream into a {@link ModuleInfo}.
     *
     * <p>Supports top-level keys:
     * {@code name}, {@code description}, {@code main}, {@code version}, {@code website},
     * {@code author} (single) or {@code authors} (list),
     * {@code pluginDepends}, {@code pluginSoftDepends}, {@code pluginLoadBefore},
     * {@code moduleDepends}, {@code moduleSoftDepends} / {@code moduleSoftDepend},
     * {@code moduleLoadBefore}, {@code supportsFolia}, {@code load},
     * {@code libraries},
     * {@code permissions}, {@code commands}.</p>
     *
     * <p>Libraries support a simple list with optional nested flags:</p>
     *
     * <pre>
     * libraries:
     *   - group:artifact:version
     *   - group:artifact:version
     *     relocatefrom: old.pkg
     *     relocateto: new.pkg
     *     isIsolated: true
     *     useTransitive: false
     *     repo: https://repo.example/
     *     id: myRepo
     * </pre>
     *
     * <p>List values may be comma-separated on the same line or via {@code - item} lines.
     * Comments (#) and blank lines are ignored.</p>
     *
     * @param in Input stream of {@code module.yml}. Must not be {@code null}.
     * @return Parsed module info.
     * @throws Exception If reading or parsing fails.
     */
    public static ModuleInfo fromYaml(InputStream in) throws Exception {
        String text = new String(in.readAllBytes());
        Map<String, List<String>> lists = new HashMap<>();
        Map<String, String> scalars = new HashMap<>();
        Map<String, PermissionDef> permissions = new LinkedHashMap<>();
        Map<String, CommandDef> commands = new LinkedHashMap<>();
        List<LibraryDef> libraries = new ArrayList<>();

        String currentList = null;
        String currentSection = null;

        class PermBuilder {
            String node;
            String description;
            String defaultValue;
        }
        class CmdBuilder {
            String name;
            String description;
            String usage;
            String permission;
            String executor;
            String tabCompleter;
            List<String> aliases = new ArrayList<>();
        }
        class LibBuilder {
            String coordinates;
            String relocateFrom;
            String relocateTo;
            Boolean isIsolated;
            Boolean useTransitive;
            String repo;
            String id;
        }

        PermBuilder currentPerm = null;
        CmdBuilder currentCmd = null;
        LibBuilder currentLib = null;

        Pattern keyLine = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*\\s*:");

        Set<String> listKeys = new HashSet<>(Set.of(
                "authors",
                "plugindepends",
                "pluginsoftdepends",
                "pluginloadbefore",
                "moduledepends",
                "modulesoftdepends",
                "modulesoftdepend",
                "moduleloadbefore"
        ));

        String[] lines = text.split("\\R");
        for (String raw : lines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            int indent = leadingSpaces(raw);

            if (indent == 0 && keyLine.matcher(trimmed).find()) {
                String topKeyName = trimmed.split(":")[0].trim();

                // flush section builders when leaving their section
                if (!"permissions".equalsIgnoreCase(topKeyName)
                        && currentPerm != null && currentPerm.node != null) {
                    permissions.put(currentPerm.node,
                            new PermissionDef(currentPerm.node,
                                    nv(currentPerm.description),
                                    nv(currentPerm.defaultValue)));
                    currentPerm = null;
                }
                if (!"commands".equalsIgnoreCase(topKeyName)
                        && currentCmd != null && currentCmd.name != null) {
                    commands.put(currentCmd.name,
                            new CommandDef(currentCmd.name,
                                    nv(currentCmd.description),
                                    nv(currentCmd.usage),
                                    nv(currentCmd.permission),
                                    currentCmd.aliases,
                                    nv(currentCmd.executor),
                                    nv(currentCmd.tabCompleter)));
                    currentCmd = null;
                }
                if (!"libraries".equalsIgnoreCase(topKeyName)
                        && currentLib != null && currentLib.coordinates != null) {
                    libraries.add(new LibraryDef(
                            nv(currentLib.coordinates),
                            nv(currentLib.relocateFrom),
                            nv(currentLib.relocateTo),
                            currentLib.isIsolated,
                            currentLib.useTransitive,
                            nv(currentLib.repo),
                            nv(currentLib.id)
                    ));
                    currentLib = null;
                }

                int idx = trimmed.indexOf(':');
                String key = trimmed.substring(0, idx).trim();
                String val = trimmed.substring(idx + 1).trim();
                String k = key.toLowerCase(Locale.ROOT);
                currentList = null;
                currentSection = null;

                if ("permissions".equals(k)) {
                    currentSection = "permissions";
                    continue;
                } else if ("commands".equals(k)) {
                    currentSection = "commands";
                    continue;
                } else if ("libraries".equals(k)) {
                    currentSection = "libraries";
                    continue;
                }

                boolean isListKey = listKeys.contains(k);

                if (val.isEmpty()) {
                    if (isListKey) {
                        lists.computeIfAbsent(k, __ -> new ArrayList<>());
                        currentList = k;
                    } else {
                        scalars.put(k, "");
                    }
                } else {
                    if (isListKey) {
                        List<String> arr = new ArrayList<>();
                        String[] parts = val.split(",");
                        for (String p : parts) {
                            String v = p.trim();
                            if (!v.isEmpty()) arr.add(v);
                        }
                        lists.put(k, arr);
                    } else {
                        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        }
                        scalars.put(k, val);
                    }
                }
                continue;
            }

            if (trimmed.startsWith("-") && currentList != null && currentSection == null) {
                String v = trimmed.substring(1).trim();
                if (!v.isEmpty()) lists.get(currentList).add(v);
                continue;
            }

            if ("libraries".equals(currentSection)) {
                // New library entry "- group:artifact:version"
                if (indent == 2 && trimmed.startsWith("-")) {
                    if (currentLib != null && currentLib.coordinates != null) {
                        libraries.add(new LibraryDef(
                                nv(currentLib.coordinates),
                                nv(currentLib.relocateFrom),
                                nv(currentLib.relocateTo),
                                currentLib.isIsolated,
                                currentLib.useTransitive,
                                nv(currentLib.repo),
                                nv(currentLib.id)
                        ));
                    }
                    currentLib = new LibBuilder();
                    currentLib.coordinates = stripSimpleQuotes(trimmed.substring(1).trim());
                    continue;
                }

                int idx = trimmed.indexOf(':');
                if (idx <= 0 || currentLib == null) continue;

                String key = trimmed.substring(0, idx).trim();
                String val = stripSimpleQuotes(trimmed.substring(idx + 1).trim());
                String lk = key.toLowerCase(Locale.ROOT);

                if ("relocatefrom".equals(lk)) {
                    currentLib.relocateFrom = val;
                } else if ("relocateto".equals(lk) || "relocateto".equals(lk)) {
                    currentLib.relocateTo = val;
                } else if ("isisolated".equals(lk)) {
                    currentLib.isIsolated = parseBoolNullable(val);
                } else if ("usetransitive".equals(lk)) {
                    currentLib.useTransitive = parseBoolNullable(val);
                } else if ("repo".equals(lk)) {
                    currentLib.repo = val;
                } else if ("id".equals(lk)) {
                    currentLib.id = val;
                }
                continue;
            }

            if ("permissions".equals(currentSection)) {
                int idx = trimmed.indexOf(':');
                if (idx <= 0) continue;
                String key = trimmed.substring(0, idx).trim();
                String val = trimmed.substring(idx + 1).trim();

                if (indent == 2) {
                    if (currentPerm != null && currentPerm.node != null) {
                        permissions.put(currentPerm.node,
                                new PermissionDef(currentPerm.node,
                                        nv(currentPerm.description),
                                        nv(currentPerm.defaultValue)));
                    }
                    currentPerm = new PermBuilder();
                    currentPerm.node = key;
                    continue;
                }

                if (indent >= 4 && currentPerm != null) {
                    if ("description".equalsIgnoreCase(key)) {
                        currentPerm.description = stripSimpleQuotes(val);
                    } else if ("default".equalsIgnoreCase(key)) {
                        currentPerm.defaultValue = stripSimpleQuotes(val);
                    }
                }
                continue;
            }

            if ("commands".equals(currentSection)) {
                int idx = trimmed.indexOf(':');
                if (idx <= 0) continue;
                String key = trimmed.substring(0, idx).trim();
                String val = trimmed.substring(idx + 1).trim();

                if (indent == 2) {
                    if (currentCmd != null && currentCmd.name != null) {
                        commands.put(currentCmd.name,
                                new CommandDef(currentCmd.name,
                                        nv(currentCmd.description),
                                        nv(currentCmd.usage),
                                        nv(currentCmd.permission),
                                        currentCmd.aliases,
                                        nv(currentCmd.executor),
                                        nv(currentCmd.tabCompleter)));
                    }
                    currentCmd = new CmdBuilder();
                    currentCmd.name = key; // e.g. "example"
                    continue;
                }

                if (indent >= 4 && currentCmd != null) {
                    String valStripped = stripSimpleQuotes(val);
                    if ("description".equalsIgnoreCase(key)) {
                        currentCmd.description = valStripped;
                    } else if ("usage".equalsIgnoreCase(key)) {
                        currentCmd.usage = valStripped;
                    } else if ("permission".equalsIgnoreCase(key)) {
                        currentCmd.permission = valStripped;
                    } else if ("aliases".equalsIgnoreCase(key)) {
                        currentCmd.aliases = parseInlineList(valStripped);
                    } else if ("executor".equalsIgnoreCase(key)) {
                        currentCmd.executor = valStripped;
                    } else if ("tab-completer".equalsIgnoreCase(key) || "tabcompleter".equalsIgnoreCase(key)) {
                        currentCmd.tabCompleter = valStripped;
                    }
                }
            }
        }

        if (currentPerm != null && currentPerm.node != null) {
            permissions.put(currentPerm.node,
                    new PermissionDef(currentPerm.node,
                            nv(currentPerm.description),
                            nv(currentPerm.defaultValue)));
        }
        if (currentCmd != null && currentCmd.name != null) {
            commands.put(currentCmd.name,
                    new CommandDef(currentCmd.name,
                            nv(currentCmd.description),
                            nv(currentCmd.usage),
                            nv(currentCmd.permission),
                            currentCmd.aliases,
                            nv(currentCmd.executor),
                            nv(currentCmd.tabCompleter)));
        }
        if (currentLib != null && currentLib.coordinates != null) {
            libraries.add(new LibraryDef(
                    nv(currentLib.coordinates),
                    nv(currentLib.relocateFrom),
                    nv(currentLib.relocateTo),
                    currentLib.isIsolated,
                    currentLib.useTransitive,
                    nv(currentLib.repo),
                    nv(currentLib.id)
            ));
        }

        // Scalars
        String name = nv(scalars.get("name"));
        String desc = nv(scalars.get("description"));
        String main = nv(scalars.get("main"));
        String ver = scalars.getOrDefault("version", "0.0.0");
        String site = nv(scalars.get("website"));

        List<String> authors = new ArrayList<>(lists.getOrDefault("authors", List.of()));
        String singleAuthor = nv(scalars.get("author"));
        if (singleAuthor != null && !singleAuthor.isEmpty()) {
            authors.add(singleAuthor);
        }
        List<String> cleanAuthors = new ArrayList<>();
        for (String a : authors) {
            String t = (a == null) ? null : a.trim();
            if (t != null && !t.isEmpty()) cleanAuthors.add(t);
        }

        boolean supportsFolia = Boolean.parseBoolean(
                scalars.getOrDefault("supportsfolia", "false")
        );

        GravesXModuleController.LoadPhase loadPhase = parseLoadPhase(scalars.get("load"));

        List<String> moduleSoftDeps = new ArrayList<>(lists.getOrDefault("modulesoftdepends", List.of()));
        moduleSoftDeps.addAll(lists.getOrDefault("modulesoftdepend", List.of()));

        return new ModuleInfo(
                name, desc, main, ver, site, cleanAuthors,
                lists.getOrDefault("plugindepends", List.of()),
                lists.getOrDefault("pluginsoftdepends", List.of()),
                lists.getOrDefault("pluginloadbefore", List.of()),
                lists.getOrDefault("moduledepends", List.of()),
                moduleSoftDeps,
                lists.getOrDefault("moduleloadbefore", List.of()),
                supportsFolia,
                loadPhase,
                permissions,
                commands,
                libraries
        );
    }

    /**
     * Parses the {@code load:} value from module.yml into a {@link GravesXModuleController.LoadPhase}.
     *
     * <p>Accepts case-insensitive phase names. If the value is missing/blank or invalid,
     * this returns {@link GravesXModuleController.LoadPhase#COMPLETED}.</p>
     *
     * @param raw raw string value from {@code load:} (maybe {@code null})
     * @return resolved load phase (never {@code null})
     */
    private static GravesXModuleController.LoadPhase parseLoadPhase(String raw) {
        String v = nv(stripSimpleQuotes(raw));
        if (v == null) return GravesXModuleController.LoadPhase.COMPLETED;

        String u = v.trim().toUpperCase(Locale.ROOT);
        try {
            return GravesXModuleController.LoadPhase.valueOf(u);
        } catch (IllegalArgumentException ignored) {
            return GravesXModuleController.LoadPhase.COMPLETED;
        }
    }

    /**
     * Normalizes a string value by converting blank inputs to {@code null}.
     *
     * @param s input string (maybe {@code null})
     * @return trimmed input, or {@code null} if {@code s} is null/blank
     */
    private static String nv(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /**
     * Counts leading indentation characters.
     *
     * <p>Spaces count as 1. Tabs are treated as 4 spaces.</p>
     *
     * @param s input line (must not be {@code null})
     * @return number of leading spaces (with tabs expanded)
     */
    private static int leadingSpaces(String s) {
        int count = 0;
        while (count < s.length()) {
            char c = s.charAt(count);
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 4;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Strips a single pair of surrounding quotes from a value.
     *
     * <p>Supports both double quotes ({@code "}) and single quotes ({@code '}).</p>
     *
     * @param s input string (may be {@code null})
     * @return unquoted, trimmed value or {@code null} if input is {@code null}
     */
    private static String stripSimpleQuotes(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            t = t.substring(1, t.length() - 1);
        }
        if (t.startsWith("'") && t.endsWith("'") && t.length() >= 2) {
            t = t.substring(1, t.length() - 1);
        }
        return t;
    }

    /**
     * Parses a simple inline YAML-style list like {@code ["a", "b", c]} into
     * {@code List.of("a", "b", "c")}. This is intentionally minimal.
     */
    private static List<String> parseInlineList(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        String t = s.trim();
        if (!t.startsWith("[") || !t.endsWith("]")) {
            if (!t.isEmpty()) out.add(stripSimpleQuotes(t));
            return out;
        }
        String inner = t.substring(1, t.length() - 1).trim();
        if (inner.isEmpty()) return out;

        String[] parts = inner.split(",");
        for (String p : parts) {
            String v = stripSimpleQuotes(p.trim());
            if (v != null && !v.isEmpty()) out.add(v);
        }
        return out;
    }

    private static Boolean parseBoolNullable(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (t.equalsIgnoreCase("false")) return Boolean.FALSE;
        return null;
    }
}
