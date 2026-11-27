package com.ranull.graves.util;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling YAML file operations.
 */
public class YAMLUtil {

    private YAMLUtil() {}

    /**
     * Checks if a given file is a valid YAML file.
     *
     * @param file The file to check.
     * @return True if the file is a valid YAML file (i.e., does not start with a dot and ends with ".yml"), otherwise false.
     */
    public static boolean isValidYAML(File file) {
        return !file.getName().startsWith(".") && file.getName().endsWith(".yml");
    }


    /**
     * Represents a YAML parsing/validation failure.
     */
    public static class YamlParseError {

        /**
         * The 1-based line number where the parsing error occurred, or {@code -1} if unknown.
         */
        public final int line;

        /**
         * The 1-based column number where the parsing error occurred, or {@code -1} if unknown.
         */
        public final int column;

        /**
         * The raw error message reported by the underlying YAML loader.
         */
        public final @NotNull String message;

        /**
         * Best-effort key name inferred near the reported error location, or {@code null} if none can be determined.
         */
        public final @Nullable String nearKey;

        /**
         * A formatted snippet from the disk file around the reported error location, or {@code null} if unavailable.
         */
        public final @Nullable String diskSnippet;

        /**
         * A formatted snippet from the bundled resource used as the expected reference, or {@code null} if unavailable.
         */
        public final @Nullable String expectedJarSnippet;

        /**
         * Creates a new YAML parse error container.
         *
         * @param line              The 1-based line number, or {@code -1} if unknown.
         * @param column            The 1-based column number, or {@code -1} if unknown.
         * @param message           The raw error message from the YAML loader.
         * @param nearKey           Best-effort key name near the error location, or {@code null}.
         * @param diskSnippet       Formatted disk snippet around the error location, or {@code null}.
         * @param expectedJarSnippet Formatted snippet from the bundled resource for comparison, or {@code null}.
         */
        public YamlParseError(int line,
                              int column,
                              @NotNull String message,
                              @Nullable String nearKey,
                              @Nullable String diskSnippet,
                              @Nullable String expectedJarSnippet) {
            this.line = line;
            this.column = column;
            this.message = message;
            this.nearKey = nearKey;
            this.diskSnippet = diskSnippet;
            this.expectedJarSnippet = expectedJarSnippet;
        }
    }

    /**
     * Validates a YAML file by attempting to load it through Bukkit's {@link YamlConfiguration}.
     *
     * @param plugin      The plugin instance used to read bundled resources.
     * @param diskFile    The YAML file on disk to validate.
     * @param jarResource The resource path inside the JAR for the corresponding config file (maybe null).
     * @return Null if the YAML loads successfully; otherwise a populated {@link YamlParseError}.
     */
    @Nullable
    public static YamlParseError validateWithBukkit(@NotNull Plugin plugin,
                                                    @NotNull File diskFile,
                                                    @Nullable String jarResource) {
        List<String> diskLinesRaw = readDiskLines(diskFile);

        try {
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.load(diskFile);
            return null;
        } catch (InvalidConfigurationException e) {
            String msg = (e.getMessage() != null) ? e.getMessage() : e.toString();

            LineCol lc = extractLineCol(msg);
            int line = lc.line1;
            int col = lc.col1;

            List<String> diskLines = expandTabs(diskLinesRaw);

            String diskSnippet = (line > 0)
                    ? buildSnippet(diskLines, line - 1, (col > 0 ? col - 1 : -1), 2, 2)
                    : null;

            String nearKey = (line > 0)
                    ? guessNearestKey(diskLinesRaw, line - 1)
                    : null;

            String expected = null;
            if (jarResource != null) {
                // Prefer key-based context in the JAR (more robust than matching line numbers).
                if (nearKey != null) {
                    expected = buildJarContextAroundKey(plugin, jarResource, nearKey, 5, 5);
                }
            }

            return new YamlParseError(line, col, msg, nearKey, diskSnippet, expected);
        } catch (Exception e) {
            String msg = (e.getMessage() != null) ? e.getMessage() : e.toString();
            return new YamlParseError(-1, -1, msg, null, null, null);
        }
    }

    /**
     * Logs a formatted YAML parse error to the console.
     *
     * @param plugin   The plugin instance used for logging.
     * @param fileName The file name being logged (used only for display).
     * @param err      The parse error to log.
     */
    public static void logParseError(@NotNull Plugin plugin,
                                     @NotNull String fileName,
                                     @NotNull YamlParseError err) {
        String where = (err.nearKey != null ? " (near/at key '" + err.nearKey + "')" : "");
        String at = (err.line > 0 ? " at line " + err.line + ", column " + err.column : "");

        plugin.getLogger().severe("Cannot load " + fileName + where + at + ".");

        if (err.diskSnippet != null) {
            plugin.getLogger().severe("--- Disk (" + fileName + ") ---\n" + err.diskSnippet);
        }

        if (err.expectedJarSnippet != null) {
            plugin.getLogger().severe("--- Expected ---\n" + err.expectedJarSnippet);
        } else if (err.nearKey != null) {
            plugin.getLogger().severe("--- Expected ---\n" +
                    "(Could not locate key '" + err.nearKey + "' in bundled resource)");
        }
    }

    /**
     * Builds a context snippet around a line with optional caret.
     *
     * @param lines      Tab-expanded lines for accurate display.
     * @param line0      0-based line index.
     * @param col0       0-based column index, -1 for none.
     * @param above      how many lines above to include
     * @param below      how many lines below to include
     */
    @Nullable
    public static String buildSnippet(@NotNull List<String> lines,
                                      int line0,
                                      int col0,
                                      int above,
                                      int below) {
        if (lines.isEmpty() || line0 < 0) return null;

        int start = Math.max(0, line0 - Math.max(0, above));
        int end = Math.min(lines.size() - 1, line0 + Math.max(0, below));

        final String prefix = "     |";

        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            int lineNo = i + 1;

            sb.append(String.format("%4d |%s%n", lineNo, lines.get(i)));

            if (i == line0 && col0 >= 0) {
                sb.append(prefix);
                sb.append(" ".repeat(col0));
                sb.append("^\n\n");
            }
        }
        return sb.toString();
    }


    /**
     * Attempts to find the nearest YAML key above the specified line index.
     *
     * @param rawLines     The raw file lines (unmodified).
     * @param errorLine0   The 0-based line index where the error occurred.
     * @return The nearest key name, or null if none can be determined.
     */
    @Nullable
    public static String guessNearestKey(@NotNull List<String> rawLines, int errorLine0) {
        if (rawLines.isEmpty() || errorLine0 < 0) return null;

        Pattern keyPattern = Pattern.compile("^\\s*([A-Za-z0-9_.-]+)\\s*:(?:\\s*.*)?$");

        for (int i = Math.min(errorLine0, rawLines.size() - 1); i >= 0; i--) {
            String raw = rawLines.get(i);
            String t = raw.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;

            Matcher m = keyPattern.matcher(raw);
            if (m.find()) return m.group(1);
        }

        return null;
    }

    /**
     * Builds a formatted snippet of a bundled JAR resource around a matching key line.
     *
     * @param plugin       The plugin instance used to read bundled resources.
     * @param resourcePath The resource path inside the JAR.
     * @param key          The key name to locate.
     * @param above        Number of lines above the key line to include.
     * @param below        Number of lines below the key line to include.
     * @return A formatted snippet, or null if the resource is missing or the key cannot be found.
     */
    @Nullable
    public static String buildJarContextAroundKey(@NotNull Plugin plugin,
                                                  @NotNull String resourcePath,
                                                  @NotNull String key,
                                                  int above,
                                                  int below) {
        String text = readResourceText(plugin, resourcePath);
        if (text == null) return null;

        List<String> rawLines = Arrays.asList(text.split("\\r?\\n", -1));
        List<String> lines = expandTabs(rawLines);

        int keyLine0 = findFirstKeyLine(rawLines, key);
        if (keyLine0 < 0) return null;

        return buildSnippet(lines, keyLine0, -1, above, below);
    }

    private static int findFirstKeyLine(@NotNull List<String> rawLines, @NotNull String key) {
        for (int i = 0; i < rawLines.size(); i++) {
            String raw = rawLines.get(i);
            String t = raw.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;

            if (t.startsWith(key + ":")) return i;
        }
        return -1;
    }

    private static final class LineCol {
        final int line1;
        final int col1;

        LineCol(int line1, int col1) {
            this.line1 = line1;
            this.col1 = col1;
        }
    }

    private static @NotNull LineCol extractLineCol(@NotNull String msg) {
        Pattern p = Pattern.compile("line\\s+(\\d+)\\s*,\\s*column\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(msg);
        if (m.find()) {
            int line = safeParseInt(m.group(1));
            int col = safeParseInt(m.group(2));
            return new LineCol(line, col);
        }
        return new LineCol(-1, -1);
    }

    private static int safeParseInt(@Nullable String s) {
        if (s == null) return -1;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static @NotNull List<String> readDiskLines(@NotNull File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Nullable
    private static String readResourceText(@NotNull Plugin plugin, @NotNull String resourcePath) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Expands tabs to spaces for consistent display and indentation math.
     * Uses 4 spaces per tab.
     */
    private static @NotNull List<String> expandTabs(@NotNull List<String> rawLines) {
        return rawLines.stream()
                .map(s -> s.replace("\t", "    "))
                .toList();
    }
}