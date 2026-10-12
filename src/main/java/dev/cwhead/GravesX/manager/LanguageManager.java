package dev.cwhead.GravesX.manager;

import com.ranull.graves.Graves;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Manages language files, locale selection, and translation fallback.
 */
public final class LanguageManager {

    private static final String ENGLISH = "en-US";
    private static final String RESOURCE_PATH = "config/language/";
    private static final Pattern CODE = Pattern.compile("[a-zA-Z]{2,8}[-_][a-zA-Z0-9]{2,8}");

    private final Graves plugin;

    private final Map<String, YamlConfiguration> translations = new HashMap<>();

    private final Map<String, YamlConfiguration> addonTranslations = new HashMap<>();

    private final Map<UUID, String> lastNotifiedLocale = new HashMap<>();

    private YamlConfiguration english = new YamlConfiguration();
    private YamlConfiguration bundledEnglish = new YamlConfiguration();

    /**
     * Initializes and loads available languages.
     *
     * @param plugin GravesX plugin instance
     */
    public LanguageManager(Graves plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Reloads language files while preserving add-on registrations.
     */
    public void reload() {
        File folder = new File(plugin.getDataFolder(), "config/language");

        if (!folder.isDirectory() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create language folder: " + folder);
        }

        for (String locale : bundledLocales(plugin)) {
            File file = new File(folder, locale + ".yml");

            if (file.isFile())
                continue;

            try (InputStream resource = plugin.getResource(RESOURCE_PATH + locale + ".yml")) {
                if (resource != null) {
                    Files.copy(resource, file.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not extract language " + locale + ": " + e.getMessage());
            }
        }

        translations.clear();

        File[] files = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".yml"));

        if (files != null) {
            for (File file : files) {
                String code = file.getName().substring(0, file.getName().length() - 4);

                if (!CODE.matcher(code).matches())
                    continue;

                translations.put(normalize(code), YamlConfiguration.loadConfiguration(file));
            }
        }

        translations.putAll(addonTranslations);

        bundledEnglish = new YamlConfiguration();

        try (InputStream resource = plugin.getResource(RESOURCE_PATH + ENGLISH + ".yml")) {
            if (resource != null) {
                bundledEnglish = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load bundled English: " + e.getMessage());
        }

        english = translations.getOrDefault(normalize(ENGLISH), bundledEnglish);

        lastNotifiedLocale.clear();

        plugin.getLogger().info("Loaded " + translations.size() + " language file(s)" + (translations.isEmpty() ? "." : ": " + String.join(", ", translations.keySet().stream().sorted().toList())));
    }

    /**
     * Discovers language files bundled with GravesX.
     *
     * @param plugin GravesX plugin instance
     * @return bundled locale codes
     */
    public static Set<String> bundledLocales(Graves plugin) {
        Set<String> locales = new LinkedHashSet<>();

        try {
            File location = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

            if (location.isFile()) {
                try (JarFile jar = new JarFile(location)) {
                    var entries = jar.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();

                        if (entry.isDirectory() || !name.startsWith(RESOURCE_PATH) || !name.endsWith(".yml"))
                            continue;

                        String code = name.substring(RESOURCE_PATH.length(), name.length() - 4);

                        if (CODE.matcher(code).matches())
                            locales.add(code);
                    }
                }
            } else if (location.isDirectory()) {
                File folder = new File(location, RESOURCE_PATH);

                File[] files = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".yml"));

                if (files != null) {
                    for (File file : files) {
                        String code = file.getName().substring(0, file.getName().length() - 4);

                        if (CODE.matcher(code).matches())
                            locales.add(code);
                    }
                }
            }
        } catch (IOException | URISyntaxException | SecurityException e) {
            plugin.getLogger().warning("Could not enumerate bundled languages: " + e.getMessage());
        }

        if (locales.isEmpty() && plugin.getResource(RESOURCE_PATH + ENGLISH + ".yml") != null) {
            locales.add(ENGLISH);
        }

        return Collections.unmodifiableSet(locales);
    }

    /**
     * Gets all currently loaded locale codes.
     *
     * @return available locale codes
     */
    public Set<String> getAvailableLocales() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(translations.keySet()));
    }

    /**
     * Registers or replaces an add-on language.
     * Registrations persist through reloads.
     *
     * @param locale locale code
     * @param contents language configuration
     * @throws IllegalArgumentException if arguments are invalid
     */
    public void registerLocale(String locale, YamlConfiguration contents) {
        if (locale == null || !CODE.matcher(locale).matches()) {
            throw new IllegalArgumentException("Invalid locale code");
        }

        if (contents == null) {
            throw new IllegalArgumentException("Language contents are required");
        }

        String normalized = normalize(locale);

        addonTranslations.put(normalized, contents);
        translations.put(normalized, contents);

        if (normalized.equals(normalize(ENGLISH))) {
            english = contents;
        }
    }

    /**
     * Unregisters an add-on language and restores any file-backed version.
     *
     * @param locale locale code
     */
    public void unregisterLocale(String locale) {
        if (locale == null)
            throw new IllegalArgumentException("Locale cannot be null");

        addonTranslations.remove(normalize(locale));
        reload();
    }

    /**
     * Resolves a player's client locale to an available language.
     *
     * @param player player, or null to use the server default
     * @return resolved locale code
     */
    public String getResolvedLocale(@Nullable Player player) {
        String requested = player == null ? configuredLocale() : player.getLocale();

        return resolveLocale(requested);
    }

    /**
     * Gets a raw message for a locale using language fallbacks.
     *
     * @param locale requested locale code
     * @param path message key
     * @return raw message, or an empty string if unavailable
     */
    public String get(String locale, String path) {
        return lookup(translations.getOrDefault(resolveLocale(locale), english), path);
    }

    /**
     * Gets a raw message using the player's locale and fallbacks.
     *
     * @param player player, or null to use the server default
     * @param path message key
     * @return raw message, or an empty string if unavailable
     */
    public String get(@Nullable Player player, String path) {
        return lookup(select(player), path);
    }

    /**
     * Resolves a message from the selected, default, or English language.
     *
     * @param selected selected language configuration
     * @param path message key
     * @return raw message, or an empty string if unavailable
     */
    private String lookup(YamlConfiguration selected, String path) {
        if (selected.contains(path, false))
            return selected.getString(path, "");

        YamlConfiguration fallback = defaultLanguage();

        if (fallback != selected && fallback.contains(path, false))
            return fallback.getString(path, "");

        if (english.contains(path, false))
            return english.getString(path, "");

        return bundledEnglish.getString(path, "");
    }

    /**
     * Gets a raw message list for a locale using language fallbacks.
     *
     * @param locale requested locale code
     * @param path message key
     * @return raw messages, or an empty list if unavailable
     */
    public List<String> getList(String locale, String path) {
        return lookupList(translations.getOrDefault(resolveLocale(locale), english), path);
    }

    /**
     * Gets a raw message list using the player's locale and fallbacks.
     *
     * @param player player, or null to use the server default
     * @param path message key
     * @return raw messages, or an empty list if unavailable
     */
    public List<String> getList(@Nullable Player player, String path) {
        return lookupList(select(player), path);
    }

    /**
     * Resolves a message list using the standard fallback order.
     *
     * @param selected selected language configuration
     * @param path message key
     * @return raw messages, or an empty list if unavailable
     */
    private List<String> lookupList(YamlConfiguration selected, String path) {
        if (selected.contains(path, false))
            return selected.getStringList(path);

        YamlConfiguration fallback = defaultLanguage();

        if (fallback != selected && fallback.contains(path, false))
            return fallback.getStringList(path);

        if (english.contains(path, false))
            return english.getStringList(path);

        return bundledEnglish.getStringList(path);
    }

    /**
     * Normalizes a locale code for case-insensitive lookup.
     *
     * @param locale locale code
     * @return normalized locale code
     */
    private static String normalize(String locale) {
        return locale.replace('_', '-').toLowerCase(Locale.ROOT);
    }

    /**
     * Gets the configured default locale code.
     *
     * @return configured locale, or English if unspecified
     */
    private String configuredLocale() {
        String configured = plugin.getConfig().getString("settings.language.default", ENGLISH);

        return configured == null || configured.isBlank() ? normalize(ENGLISH) : normalize(configured);
    }

    /**
     * Resolves a locale to an available regional variant or fallback.
     *
     * @param requested requested locale code
     * @return resolved locale code
     */
    private String resolveLocale(String requested) {
        String locale = requested == null || requested.isBlank() ? configuredLocale() : normalize(requested);

        if (translations.containsKey(locale))
            return locale;

        String language = locale.split("-", 2)[0];
        String configured = configuredLocale();

        if (configured.startsWith(language + "-") && translations.containsKey(configured))
            return configured;

        return translations.keySet().stream().filter(code -> code.startsWith(language + "-")).sorted().findFirst().orElseGet(() -> translations.containsKey(configured) ? configured : normalize(ENGLISH));
    }

    /**
     * Selects the language configuration for a player.
     *
     * @param player player, or null to use the server default
     * @return selected language configuration
     */
    private YamlConfiguration select(@Nullable Player player) {
        String requested = player == null ? configuredLocale() : player.getLocale();

        return translations.getOrDefault(resolveLocale(requested), english);
    }

    /**
     * Gets the configured fallback language, or English if unavailable.
     *
     * @return fallback language configuration
     */
    private YamlConfiguration defaultLanguage() {
        return translations.getOrDefault(configuredLocale(), english);
    }

    /**
     * Notifies a player when their exact client locale is unavailable.
     * Repeated notifications for the same locale are suppressed.
     *
     * @param player player to notify
     */
    public void notifyFallback(Player player) {
        String clientLocale = normalize(player.getLocale());

        if (translations.containsKey(clientLocale)) {
            lastNotifiedLocale.remove(player.getUniqueId());
            return;
        }

        if (clientLocale.equals(lastNotifiedLocale.get(player.getUniqueId())))
            return;

        String message = get(player, "message.language-fallback").replace("%client_locale%", player.getLocale()).replace("%fallback_locale%", resolveLocale(clientLocale));

        if (message.isEmpty())
            return;

        lastNotifiedLocale.put(player.getUniqueId(), clientLocale);

        String prefix = get(player, "message.prefix");

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    /**
     * Clears cached language notification state for a player.
     *
     * @param player disconnecting player
     */
    public void clearPlayer(Player player) {
        lastNotifiedLocale.remove(player.getUniqueId());
    }
}