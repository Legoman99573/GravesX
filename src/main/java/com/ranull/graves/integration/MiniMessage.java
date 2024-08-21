package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for parsing MiniMessage formatted strings into legacy text format.
 */
public final class MiniMessage {
    public static net.kyori.adventure.text.minimessage.MiniMessage miniMessage;
    private static BukkitAudiences audiences;
    private static LegacyComponentSerializer legacyComponentSerializer;



    /**
     * Initializes a new MiniMessage instance. Attempts to instantiate the MiniMessage parser.
     * If the MiniMessage class is not found, the instance will be null.
     */
    public MiniMessage() {
        audiences = BukkitAudiences.create(JavaPlugin.getPlugin(Graves.class));
        legacyComponentSerializer = LegacyComponentSerializer.legacySection();
        miniMessage = net.kyori.adventure.text.minimessage.MiniMessage.builder()
                .tags(
                        TagResolver.builder()
                                .resolver(StandardTags.defaults())
                                .resolver(StandardTags.hoverEvent())
                                .resolver(StandardTags.clickEvent())
                                .resolver(StandardTags.insertion())
                                .resolver(StandardTags.rainbow())
                                .resolver(StandardTags.gradient())
                                .resolver(StandardTags.newline())
                                .resolver(StandardTags.score())
                                .resolver(StandardTags.nbt())
                                .resolver(StandardTags.font())
                                .resolver(StandardTags.decorations())
                                .resolver(StandardTags.keybind())
                                .resolver(StandardTags.selector())
                                .resolver(StandardTags.transition())
                                .resolver(StandardTags.translatable())
                                .resolver(StandardTags.translatableFallback())
                                .resolver(StandardTags.reset())
                                .build()
                ).build();
    }

    /**
     * Parses a MiniMessage formatted string into a legacy text format.
     *
     * @param string The MiniMessage formatted string to parse.
     * @return The legacy text representation of the MiniMessage formatted string.
     *         If MiniMessage is not initialized, returns the original string.
     */
    public String parseString(String string) {
        return (miniMessage != null) ?
                legacyComponentSerializer.serialize(miniMessage.deserialize(string)) :
                string;
    }

    /**
     * Attempts to convert legacy color codes to Kyori's Adventure Text MiniMessage format.
     * @param legacyText    The Legacy Text to be converted.
     * @return              All text to be converted to StringBuilder, that is required by net.kyori.adventure.text.minimessage
     */
    public static String convertLegacyToMiniMessage(String legacyText) {
        StringBuilder miniMessageText = new StringBuilder();
        int length = legacyText.length();

        // Regex pattern to match hex color codes
        Pattern hexColorPattern = Pattern.compile("&#([0-9a-fA-F]{6})");

        // Pattern matching hex colors
        Matcher matcher = hexColorPattern.matcher(legacyText);

        int lastEnd = 0;
        while (matcher.find()) {
            miniMessageText.append(legacyText, lastEnd, matcher.start());
            String hexColor = matcher.group(1);
            miniMessageText.append("<color:#").append(hexColor).append(">");
            lastEnd = matcher.end();
        }

        miniMessageText.append(legacyText.substring(lastEnd)); // Append remaining text

        // Convert to MiniMessage format
        StringBuilder finalText = new StringBuilder();
        length = miniMessageText.length();
        for (int i = 0; i < length; i++) {
            char c = miniMessageText.charAt(i);

            if (c == '§' && i + 1 < length) {
                char code = miniMessageText.charAt(i + 1);
                String tag = getMiniMessageTag(code);

                if (!tag.equals("§" + code)) {
                    finalText.append(tag);
                    i++; // Skip the next character as it has been processed
                }
            } else if (c == '&' && i + 1 < length && miniMessageText.charAt(i + 1) == 'r') {
                // Handle reset tag for &r
                finalText.append("<reset>");
                i++; // Skip the 'r' character
            } else {
                finalText.append(c);
            }
        }

        return finalText.toString();
    }

    /**
     * Gets code character to convert to MiniMessage format
     * @param code  Gets the legacy color code tag
     * @return      Correct MiniMessage Format
     */
    private static String getMiniMessageTag(char code) {
        switch (code) {
            case '0': return "<black>";
            case '1': return "<dark_blue>";
            case '2': return "<dark_green>";
            case '3': return "<dark_aqua>";
            case '4': return "<dark_red>";
            case '5': return "<dark_purple>";
            case '6': return "<gold>";
            case '7': return "<gray>";
            case '8': return "<dark_gray>";
            case '9': return "<blue>";
            case 'a': return "<green>";
            case 'b': return "<aqua>";
            case 'c': return "<red>";
            case 'd': return "<light_purple>";
            case 'e': return "<yellow>";
            case 'f': return "<white>";
            case 'l': return "<bold>";
            case 'o': return "<italic>";
            case 'm': return "<strikethrough>";
            case 'n': return "<underline>";
            case 'r': return "<reset>";
            default: return "§" + code; // Return the original if the code is not found
        }
    }

    /**
     * Formats a book's metadata using MiniMessage formatting.
     *
     * @param itemStack  The ItemStack of the book.
     * @param title      The title of the book.
     * @param author     The author of the book.
     * @param pages      The pages of the book, each page represented by a MiniMessage formatted string.
     * @return           The updated ItemStack with formatted BookMeta.
     */
    public static ItemStack formatBookMeta(ItemStack itemStack, Component title, Component author, List<Component> pages, List<Component> lore) {
        if (miniMessage != null && legacyComponentSerializer != null) {
            if (itemStack == null || !(itemStack.getItemMeta() instanceof BookMeta)) {
                return itemStack;
            }

            BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();

            // Convert title and author components to legacy format
            String titleString = legacyComponentSerializer.serialize(title);
            String authorString = legacyComponentSerializer.serialize(author);

            bookMeta.setTitle(titleString);
            bookMeta.setAuthor(authorString);

            // Convert pages components to legacy format
            List<String> serializedPages = pages.stream()
                    .map(page -> legacyComponentSerializer.serialize(page))
                    .collect(Collectors.toList());
            bookMeta.setPages(serializedPages);

            // Convert lore components to legacy format
            List<String> serializedLore = lore.stream()
                    .map(l -> legacyComponentSerializer.serialize(l))
                    .collect(Collectors.toList());
            bookMeta.setLore(serializedLore);

            itemStack.setItemMeta(bookMeta);
        }

        return itemStack;
    }

    public static void sendMessage(final Player player, final String message) {
        if (miniMessage != null && audiences != null) {
            String output = convertLegacyToMiniMessage(message);
            audiences.sender(player).sendMessage(miniMessage.deserialize(output));
            return;
        }
        player.sendMessage(message);
    }

    public static net.kyori.adventure.text.minimessage.MiniMessage miniMessage() {
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
    }

    /**
     * Sets the name of an Armor Stand using MiniMessage formatting and returns the formatted name.
     *
     * @param name       The name to apply, formatted using MiniMessage.
     * @return           The formatted name as a string.
     */
    public static String setArmorStandName(@NotNull String name) {
        String formattedName = name;
        if (miniMessage != null) {
            // Convert the name from MiniMessage format to a Component
            String output = convertLegacyToMiniMessage(name);
            formattedName = Component.text().content(output).build().toString();

        }
        return formattedName;
    }
}