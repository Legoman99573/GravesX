package dev.cwhead.GravesXCommon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared MiniMessage translator for the GravesX server and every proxy platform.
 *
 * <h2>{@link #toMiniMessage(String)}: legacy to MiniMessage</h2>
 * <p>Used by the server and by platforms with native MiniMessage (Velocity). Converts legacy formatting codes
 * into MiniMessage.</p>
 *
 * <h2>What is converted</h2>
 * <ul>
 *   <li>Colors: {@code &0}-{@code &9}, {@code &a}-{@code &f} become named tags such as {@code <gold>}.</li>
 *   <li>Hex colors: {@code &#RRGGBB}, {@code &x&R&R&G&G&B&B} and, optionally, a bare {@code #RRGGBB}
 *       become {@code <#rrggbb>}.</li>
 *   <li>Decorations: {@code &k}, {@code &l}, {@code &m}, {@code &n}, {@code &o} become
 *       {@code <obfuscated>}, {@code <bold>}, {@code <strikethrough>}, {@code <underlined>},
 *       {@code <italic>}.</li>
 *   <li>{@code &r} becomes {@code <reset>}.</li>
 *   <li>{@code &&} becomes a literal {@code &}.</li>
 *   <li>The section sign ({@code §}) is treated exactly like {@code &}.</li>
 * </ul>
 *
 * <p>Legacy colors reset decorations, MiniMessage colors do not. To keep the legacy behavior, a color
 * code closes the decorations that the legacy codes opened ({@code &l&aText} gives bold off, then
 * green). Only decorations opened by legacy codes are closed, so events such as {@code <click>} and
 * {@code <hover>} that wrap legacy text are left alone.</p>
 *
 * <p>One caveat: {@code &&} produces a literal {@code &}, which a second pass would read as the start of a
 * code ({@code &&a} becomes {@code &a}, then green). Everything else is idempotent. Convert once, which is
 * what {@code StringUtil.parseString} does.</p>
 *
 * <p>This class is pure string processing with no server or Adventure dependency, so it can be
 * unit tested on its own.</p>
 *
 * <h2>{@link #toLegacy(String)}: MiniMessage to legacy</h2>
 * <p>Used by platforms without native MiniMessage (BungeeCord). A minimal subset: colours (named and
 * #rrggbb), bold/italic/underlined/strikethrough/obfuscated, reset and closing tags. Other tags (click, hover,
 * gradient, ...) are dropped. Output uses section-sign codes.</p>
 */
public final class MiniMessageTranslator {
    private static final char SECTION = '\u00A7';

    private MiniMessageTranslator() {}

    /**
     *  Tag names for the legacy color codes, indexed by their hex digit value (0-f).
     */
    private static final String[] MINI_COLOR_NAMES = {
            "black", "dark_blue", "dark_green", "dark_aqua",
            "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua",
            "red", "light_purple", "yellow", "white"
    };

        /**
     * Converts legacy codes to MiniMessage, including bare {@code #RRGGBB} hex colors.
     *
     * @param text the text to convert, may already contain MiniMessage tags
     * @return the MiniMessage text, or the input if it is null or empty
     */
    public static String toMiniMessage(String text) {
        return toMiniMessage(text, true);
    }

    /**
     * Converts every string in a list.
     *
     * @param lines the lines to convert
     * @return a new list with each line converted
     */
    public static List<String> toMiniMessageAll(List<String> lines) {
        List<String> converted = new ArrayList<>(lines.size());

        for (String line : lines) {
            converted.add(toMiniMessage(line));
        }

        return converted;
    }

    /**
     * Converts legacy codes to MiniMessage.
     *
     * @param text          the text to convert, may already contain MiniMessage tags
     * @param standaloneHex whether a bare {@code #RRGGBB} (not inside a tag and not directly after a letter
     *                      or digit) should be treated as a hex color
     * @return the MiniMessage text, or the input if it is null or empty
     */
    public static String toMiniMessage(String text, boolean standaloneHex) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        if (!mightNeedConversion(text, standaloneHex)) {
            return text;
        }

        final int length = text.length();
        final StringBuilder out = new StringBuilder(length + 16);
        // Decoration tags opened by legacy codes, in the order they were opened.
        final List<String> openDecorations = new ArrayList<>(5);

        for (int i = 0; i < length; ) {
            final char c = text.charAt(i);

            // MiniMessage escape, e.g. \<red>. Keep the backslash and the escaped character as written.
            if (c == '\\' && i + 1 < length) {
                out.append(c).append(text.charAt(i + 1));
                i += 2;
                continue;
            }

            // MiniMessage tag: copy it through untouched.
            if (c == '<') {
                int end = tagEnd(text, i);

                if (end > 0) {
                    out.append(text, i, end);
                    i = end;
                } else {
                    out.append(c);
                    i++;
                }

                continue;
            }

            if (c == '&' || c == SECTION) {
                // && is an escaped ampersand.
                if (c == '&' && i + 1 < length && text.charAt(i + 1) == '&') {
                    out.append('&');
                    i += 2;
                    continue;
                }

                // &#RRGGBB
                if (i + 8 <= length && text.charAt(i + 1) == '#' && isHex(text, i + 2, 6)) {
                    appendColor(out, openDecorations, "#" + text.substring(i + 2, i + 8).toLowerCase());
                    i += 8;
                    continue;
                }

                // &x&R&R&G&G&B&B
                if (i + 14 <= length && isLower(text.charAt(i + 1))) {
                    String hex = readXHex(text, i + 2);

                    if (hex != null) {
                        appendColor(out, openDecorations, "#" + hex);
                        i += 14;
                        continue;
                    }
                }

                if (i + 1 < length) {
                    char code = Character.toLowerCase(text.charAt(i + 1));
                    int color = Character.digit(code, 16);

                    if (color >= 0) {
                        appendColor(out, openDecorations, MINI_COLOR_NAMES[color]);
                        i += 2;
                        continue;
                    }

                    String decoration = decorationFor(code);

                    if (decoration != null) {
                        if (!openDecorations.contains(decoration)) {
                            out.append('<').append(decoration).append('>');
                            openDecorations.add(decoration);
                        }

                        i += 2;
                        continue;
                    }

                    if (code == 'r') {
                        out.append("<reset>");
                        openDecorations.clear();
                        i += 2;
                        continue;
                    }
                }

                // Not a legacy code (for example "&g" or a trailing "&"): leave it alone.
                out.append(c);
                i++;
                continue;
            }

            // Bare #RRGGBB
            if (standaloneHex && c == '#' && i + 7 <= length && isHex(text, i + 1, 6)
                    && (i == 0 || !Character.isLetterOrDigit(text.charAt(i - 1)))) {
                appendColor(out, openDecorations, "#" + text.substring(i + 1, i + 7).toLowerCase());
                i += 7;
                continue;
            }

            out.append(c);
            i++;
        }

        return out.toString();
    }

    /**
     * Quick check so plain text is returned without allocating.
     */
    private static boolean mightNeedConversion(String text, boolean standaloneHex) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '&' || c == SECTION || (standaloneHex && c == '#')) {
                return true;
            }
        }

        return false;
    }

    /**
     * Appends a color tag. Legacy colors reset decorations, so decorations opened by legacy codes are
     * closed first.
     */
    private static void appendColor(StringBuilder out, List<String> openDecorations, String color) {
        closeDecorations(out, openDecorations);
        out.append('<').append(color).append('>');
    }

    private static void closeDecorations(StringBuilder out, List<String> openDecorations) {
        for (int i = openDecorations.size() - 1; i >= 0; i--) {
            String name = openDecorations.get(i);
            String open = "<" + name + ">";
            int start = out.length() - open.length();

            // Opened and immediately closed (for example "&l&a"): drop the empty pair.
            if (start >= 0 && out.indexOf(open, start) == start) {
                out.setLength(start);
            } else {
                out.append("</").append(name).append('>');
            }
        }

        openDecorations.clear();
    }

    private static String decorationFor(char code) {
        return switch (code) {
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            default -> null;
        };
    }

    /**
     * Finds the end of a MiniMessage tag that starts at {@code start}.
     *
     * <p>A tag is {@code <}, an optional {@code /} or {@code !}, a name starting with a letter or
     * {@code #}, arguments that may be quoted with {@code '} or {@code "} (quoted arguments may contain
     * {@code >}), and a closing {@code >}.</p>
     *
     * @return the index just past the closing {@code >}, or -1 if this is not a tag
     */
    private static int tagEnd(String text, int start) {
        final int length = text.length();
        int i = start + 1;

        if (i >= length) {
            return -1;
        }

        char first = text.charAt(i);

        if (first == '/' || first == '!') {
            i++;

            if (i >= length) {
                return -1;
            }

            first = text.charAt(i);
        }

        if (!Character.isLetter(first) && first != '#') {
            return -1;
        }

        char quote = 0;

        for (; i < length; i++) {
            char c = text.charAt(i);

            if (c == '\\' && i + 1 < length) {
                i++;
                continue;
            }

            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }

                continue;
            }

            if (c == '\'' || c == '"') {
                quote = c;
                continue;
            }

            if (c == '<') {
                return -1;
            }

            if (c == '>') {
                return i + 1;
            }
        }

        return -1;
    }

    /**
     * Reads the six {@code &R&R&G&G&B&B} pairs of the {@code &x} hex format.
     *
     * @return the six hex digits in lower case, or null if the text is not in that format
     */
    private static String readXHex(String text, int from) {
        StringBuilder hex = new StringBuilder(6);

        for (int n = 0; n < 6; n++) {
            int at = from + n * 2;
            char marker = text.charAt(at);

            if (marker != '&' && marker != SECTION) {
                return null;
            }

            char digit = text.charAt(at + 1);

            if (Character.digit(digit, 16) < 0) {
                return null;
            }

            hex.append(Character.toLowerCase(digit));
        }

        return hex.toString();
    }

    private static boolean isHex(String text, int from, int count) {
        if (from + count > text.length()) {
            return false;
        }

        for (int i = from; i < from + count; i++) {
            if (Character.digit(text.charAt(i), 16) < 0) {
                return false;
            }
        }

        return true;
    }

    private static boolean isLower(char c) {
        return Character.toLowerCase(c) == 'x';
    }

    // ------------------------------------------------------------------ MiniMessage -> legacy

    private static final Map<String, Character> LEGACY_COLOR_CODES = Map.ofEntries(
            Map.entry("black", '0'), Map.entry("dark_blue", '1'), Map.entry("dark_green", '2'),
            Map.entry("dark_aqua", '3'), Map.entry("dark_red", '4'), Map.entry("dark_purple", '5'),
            Map.entry("gold", '6'), Map.entry("gray", '7'), Map.entry("dark_gray", '8'),
            Map.entry("blue", '9'), Map.entry("green", 'a'), Map.entry("aqua", 'b'),
            Map.entry("red", 'c'), Map.entry("light_purple", 'd'), Map.entry("yellow", 'e'),
            Map.entry("white", 'f'));

    private static final Map<String, Character> FORMATS = Map.ofEntries(
            Map.entry("obfuscated", 'k'), Map.entry("obf", 'k'),
            Map.entry("bold", 'l'), Map.entry("b", 'l'),
            Map.entry("strikethrough", 'm'), Map.entry("st", 'm'),
            Map.entry("underlined", 'n'), Map.entry("u", 'n'),
            Map.entry("italic", 'o'), Map.entry("em", 'o'), Map.entry("i", 'o'));

    private record Style(String tag, String code, boolean color) {}

        public static String toLegacy(String text) {
        StringBuilder out = new StringBuilder(text.length());
        List<Style> stack = new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && text.charAt(i + 1) == '<') {
                out.append('<');
                i++;
                continue;
            }
            if (c != '<') {
                out.append(c);
                continue;
            }

            int end = text.indexOf('>', i);
            if (end < 0) {
                out.append(c);
                continue;
            }

            String tag = text.substring(i + 1, end).toLowerCase(Locale.ROOT).trim();
            i = end;

            if (tag.equals("reset")) {
                stack.clear();
                out.append("§r");
            } else if (tag.startsWith("/")) {
                String name = tag.substring(1);
                for (int s = stack.size() - 1; s >= 0; s--) {
                    if (stack.get(s).tag().equals(name)) {
                        stack.remove(s);
                        out.append("§r");
                        for (Style style : stack) out.append(style.code());
                        break;
                    }
                }
            } else {
                Style style = parseOpen(tag);
                if (style != null) {
                    stack.add(style);
                    out.append(style.code());
                    if (style.color()) {
                        // legacy colour codes clear formatting, so re-apply active formats
                        for (Style s : stack) {
                            if (!s.color()) out.append(s.code());
                        }
                    }
                }
            }
        }
        return out.toString();
    }

    private static Style parseOpen(String tag) {
        String name = tag.startsWith("color:") ? tag.substring(6) : tag;
        if (LEGACY_COLOR_CODES.containsKey(name)) {
            return new Style(tag, "§" + LEGACY_COLOR_CODES.get(name), true);
        }
        if (name.matches("#[0-9a-f]{6}")) {
            StringBuilder hex = new StringBuilder("§x");
            for (char ch : name.substring(1).toCharArray()) hex.append('§').append(ch);
            return new Style(tag, hex.toString(), true);
        }
        if (FORMATS.containsKey(tag)) {
            return new Style(tag, "§" + FORMATS.get(tag), false);
        }
        return null;
    }
}
