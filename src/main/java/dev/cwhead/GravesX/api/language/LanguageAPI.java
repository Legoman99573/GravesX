package dev.cwhead.GravesX.api.language;

import com.ranull.graves.Graves;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *  Provides localized messages and locale registration for modules and plugins.
 */
public final class LanguageAPI {
    private final Graves plugin;

    /**
     * Creates the language API.
     *
     * @param plugin GravesX instance
     */
    public LanguageAPI(@NotNull Graves plugin) { this.plugin = Objects.requireNonNull(plugin); }

    /**
     * Gets the currently loaded locale codes.
     *
     * @return unmodifiable set of normalized locale codes
     */
    public Set<String> getAvailableLocales() {
        return plugin.getLanguageManager().getAvailableLocales();
    }

    /**
     * Resolves the player's client locale to an available language.
     *
     * @param player player, or {@code null} for the server default
     * @return resolved locale code
     */
    public String getResolvedLocale(@Nullable Player player) {
        return plugin.getLanguageManager().getResolvedLocale(player);
    }

    /**
     * Gets an untranslated-format message using the player's locale.
     * Missing keys fall back to the configured default, then English.
     *
     * @param player player, or {@code null} for the server default
     * @param key message path, such as {@code message.empty}
     * @return raw message, or an empty string if unavailable
     */
    public String get(@Nullable Player player, String key) {
        return plugin.getLanguageManager().get(player, key);
    }

    /**
     * Gets a raw message for a locale using the standard fallback chain.
     *
     * @param locale requested locale code
     * @param key message path
     * @return raw message, or an empty string if unavailable
     */
    public String get(String locale, String key) {
        return plugin.getLanguageManager().get(locale, key);
    }

    /**
     * Gets localized list entries using the player's locale.
     * Missing keys fall back to the default language, then English.
     *
     * @param player player, or {@code null} for the server default
     * @param key list path
     * @return entries, or an empty list if unavailable
     */
    public List<String> getList(@Nullable Player player, String key) {
        return plugin.getLanguageManager().getList(player, key);
    }

    /**
     * Gets localized list entries for the requested locale.
     *
     * @param locale requested locale code
     * @param key list path
     * @return entries with standard key fallback
     */
    public List<String> getList(String locale, String key) {
        return plugin.getLanguageManager().getList(locale, key);
    }

    /**
     * Registers an in-memory locale, replacing any loaded entry for its code.
     * Registrations persist across language reloads.
     *
     * @param locale locale code, such as {@code fr-FR}
     * @param translations YAML translation values
     * @throws IllegalArgumentException if the code or translations are invalid
     */
    public void registerLocale(String locale, YamlConfiguration translations) {
        plugin.getLanguageManager().registerLocale(locale, translations);
    }

    /**
     * Unregisters an add-on locale and restores any disk-backed translation.
     *
     * @param locale locale code to unregister
     */
    public void unregisterLocale(String locale) {
        plugin.getLanguageManager().unregisterLocale(locale);
    }

    /** Reloads disk translations while retaining add-on registrations. */
    public void reload() {
        plugin.getLanguageManager().reload();
    }

    /**
     * Notifies a player when their client locale is unavailable.
     * Repeated notices for the same locale are suppressed.
     *
     * @param player player to notify
     */
    public void notifyFallback(Player player) {
        plugin.getLanguageManager().notifyFallback(player);
    }
}
