package com.ranull.graves.manager;

import dev.cwhead.GravesX.compatibility.CompatibilityParticleEnum;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

/**
 * Manages version-specific functionality for the Graves plugin.
 */
public class VersionManager {
    /**
     * The version of the server or plugin.
     * <p>
     * This {@link String} holds the version information, which is used to check compatibility and feature availability.
     * </p>
     */
    private final String version;

    /**
     * Indicates whether the configuration contains specific settings or features.
     * <p>
     * This {@code boolean} flag shows if certain configuration settings or features are present in the plugin's configuration.
     * </p>
     */
    private final boolean hasConfigContains;

    /**
     * Indicates whether the API version is present or supported.
     * <p>
     * This {@code boolean} flag indicates if the API version information is available and supported by the server or plugin.
     * </p>
     */
    private final boolean hasAPIVersion;

    /**
     * Indicates whether block data is supported.
     * <p>
     * This {@code boolean} flag indicates if the server or plugin supports block data manipulation or retrieval.
     * </p>
     */
    private final boolean hasBlockData;

    /**
     * Indicates whether persistent data is supported.
     * <p>
     * This {@code boolean} flag shows if the server or plugin supports the use of persistent data containers.
     * </p>
     */
    private final boolean hasPersistentData;

    /**
     * Indicates whether scoreboard tags are supported.
     * <p>
     * This {@code boolean} flag indicates if the server or plugin supports scoreboard tags for entities.
     * </p>
     */
    private final boolean hasScoreboardTags;

    /**
     * Indicates whether hex color codes are supported.
     * <p>
     * This {@code boolean} flag shows if the server or plugin supports hexadecimal color codes for text or other elements.
     * </p>
     */
    private final boolean hasHexColors;

    /**
     * Indicates whether compass meta data is supported.
     * <p>
     * This {@code boolean} flag indicates if the server or plugin supports compass meta data functionality.
     * </p>
     */
    private final boolean hasCompassMeta;

    /**
     * Indicates whether hand swing actions are supported.
     * <p>
     * This {@code boolean} flag shows if the server or plugin supports actions related to hand swings.
     * </p>
     */
    private final boolean hasSwingHand;

    /**
     * Indicates whether world height data is supported.
     * <p>
     * This {@code boolean} flag indicates if the server or plugin supports retrieving or managing world height information.
     * </p>
     */
    private final boolean hasWorldHeight;

    /**
     * Indicates whether a second hand item is supported.
     * <p>
     * This {@code boolean} flag shows if the server or plugin supports having items in a second hand slot.
     * </p>
     */
    private final boolean hasSecondHand;

    /**
     * Indicates whether curse enchantments are supported.
     * <p>
     * This {@code boolean} flag indicates if the server or plugin supports curse enchantments on items.
     * </p>
     */
    private final boolean hasEnchantmentCurse;

    /**
     * Indicates whether particle effects are supported.
     * <p>
     * This {@code boolean} flag shows if the server or plugin supports particle effects for visual effects or gameplay.
     * </p>
     */
    private final boolean hasParticle;

    /**
     * Indicates whether the server or plugin is based on the Bukkit API.
     * <p>
     * This {@code boolean} flag shows if the server or plugin is using the Bukkit API.
     * </p>
     */
    private boolean isBukkit;

    /**
     * Indicates whether the server or plugin is based on Mohist.
     * <p>
     * This {@code boolean} flag shows if the server or plugin is using Mohist, a server software that combines Bukkit and Forge.
     * </p>
     */
    private boolean isMohist;

    /**
     * Indicates whether the server or plugin is based on Paper.
     * <p>
     * This {@code boolean} flag shows if the server or plugin is using Paper.
     * </p>
     */
    private boolean isPaper;

    /**
     * Indicates whether the server or plugin is based on Folia.
     * <p>
     * This {@code boolean} flag shows if the server or plugin is using Folia.
     * </p>
     */
    private boolean isFolia;

    /**
     * Indicates whether the plugin is running post 1.20.5.
     * <p>
     * This {@code boolean} flag shows if the plugin is running on post 1.20.5.
     * </p>
     */
    private final boolean isPost1_20_5;

    /**
     * Indicates whether the plugin is running post 1.20.9.
     * <p>
     * This {@code boolean} flag shows if the plugin is running on post 1.20.9.
     * </p>
     */
    private final boolean isPost1_21_9;

    /**
     * Indicates whether the server supports Spears in 1.21.11
     * <p>
     * This {@code boolean} flag shows if the server supports Spears.
     * </p>
     */
    private final boolean hasSpears;

    /**
     * Indicates whether the server is older than 1.20.5/1.20.6.
     * <p>
     * This {@code boolean} flag shows if the server or plugin is older than 1.20.5/1.20.6.
     * </p>
     */
    private boolean isLegacyVersion;

    /**
     * Initializes a new instance of the VersionManager class.
     */
    public VersionManager() {
        this.version = getVersion();
        this.hasConfigContains = !is_v1_7() && !is_v1_8() && !is_v1_9();
        this.hasAPIVersion = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12();
        this.hasBlockData = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12();
        this.hasPersistentData = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13();
        this.hasScoreboardTags = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10();
        this.hasHexColors = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15() && !isBukkit();
        this.hasCompassMeta = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11()
                && !is_v1_12() && !is_v1_13() && !is_v1_14() && !is_v1_15()
                && !version.matches("(?i)v1_16_R1|");
        this.hasSwingHand = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15();
        this.hasWorldHeight = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15() && !is_v1_16();
        this.hasSecondHand = !is_v1_7() && !is_v1_8();
        this.hasEnchantmentCurse = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10();
        this.hasParticle = !is_v1_7() && !is_v1_8();
        this.isPost1_20_5 = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15() && !is_v1_16() && !is_v1_17() && !is_v1_18() && !is_v1_19() && !is_v1_20();
        this.isPost1_21_9 = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15() && !is_v1_16() && !is_v1_17() && !is_v1_18() && !is_v1_19() && !is_v1_20() && !is_v1_21();
        this.hasSpears = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15() && !is_v1_16() && !is_v1_17() && !is_v1_18() && !is_v1_19() && !is_v1_20() && !is_v1_21() && !is_v1_20_5() && !isPost1_21_9();

        try {
            Class.forName("org.spigotmc.SpigotConfig", false, getClass().getClassLoader());
            this.isBukkit = false;
        } catch (ClassNotFoundException ignored) {
            this.isBukkit = true;
        }

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer", false, getClass().getClassLoader());
            this.isFolia = true;
        } catch (ClassNotFoundException ignored) {
            this.isFolia = false;
        }

        try {
            Class.forName("com.mohistmc.config.MohistConfigUtil", false, getClass().getClassLoader());
            this.isMohist = true;
        } catch (ClassNotFoundException ignored) {
            this.isMohist = false;
        }

        try {
            Class.forName("io.papermc.paper.configuration.ServerConfiguration", false, getClass().getClassLoader());
            this.isPaper = true;
        } catch (ClassNotFoundException ignored) {
            this.isPaper = false;
        }
    }

    /**
     * Retrieves the server version.
     *
     * @return The server version string.
     */
    public String getVersion() {
        try {
            this.isLegacyVersion = true;
            return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (Exception e) {
            this.isLegacyVersion = false;
            return Bukkit.getServer().getVersion();
        }
    }

    /**
     * Checks if the server is running on Bukkit versions older than 1.20.5/1.20.6.
     *
     * @return True if the server is running on versions of Bukkit if older than 1.20.5/1.20.6.
     */
    public boolean isLegacyVersion() {
        try {
            return isLegacyVersion;
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Checks if the server is running on Bukkit.
     *
     * @return True if the server is running on Bukkit, otherwise false.
     */
    public boolean isBukkit() {
        return isBukkit;
    }

    public boolean isPaper() {
        return isPaper;
    }

    /**
     * Checks if the server is running on Mohist.
     *
     * @return True if the server is running on Mohist, otherwise false.
     */
    public boolean isMohist() {
        return isMohist;
    }

    /**
     * Checks if the server is running on Folia.
     *
     * @return True if the server is running on Folia, otherwise false.
     */
    public boolean isFolia() {
        return isFolia;
    }

    /**
     * Checks if the server version has the config contains method.
     *
     * @return True if the server version has the config contains method, otherwise false.
     */
    public boolean hasConfigContains() {
        return hasConfigContains;
    }

    /**
     * Checks if the server version has the API version.
     *
     * @return True if the server version has the API version, otherwise false.
     */
    public boolean hasAPIVersion() {
        return hasAPIVersion;
    }

    /**
     * Checks if the server version has block data support.
     *
     * @return True if the server version has block data support, otherwise false.
     */
    public boolean hasBlockData() {
        return hasBlockData;
    }

    /**
     * Checks if the server version has persistent data support.
     *
     * @return True if the server version has persistent data support, otherwise false.
     */
    public boolean hasPersistentData() {
        return hasPersistentData;
    }

    /**
     * Checks if the server version has scoreboard tags support.
     *
     * @return True if the server version has scoreboard tags support, otherwise false.
     */
    public boolean hasScoreboardTags() {
        return hasScoreboardTags;
    }

    /**
     * Checks if the server version has hex color support.
     *
     * @return True if the server version has hex color support, otherwise false.
     */
    public boolean hasHexColors() {
        return hasHexColors;
    }

    /**
     * Checks if the server version has compass meta support.
     *
     * @return True if the server version has compass meta support, otherwise false.
     */
    public boolean hasCompassMeta() {
        return hasCompassMeta;
    }

    /**
     * Checks if the server version has swing hand support.
     *
     * @return True if the server version has swing hand support, otherwise false.
     */
    public boolean hasSwingHand() {
        return hasSwingHand;
    }

    /**
     * Checks if the server version has min height support.
     *
     * @return True if the server version has min height support, otherwise false.
     */
    public boolean hasMinHeight() {
        return hasWorldHeight;
    }

    /**
     * Checks if the server version has second hand support.
     *
     * @return True if the server version has second hand support, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasSecondHand() {
        return hasSecondHand;
    }

    /**
     * Checks if the server version has enchantment curse support.
     *
     * @return True if the server version has enchantment curse support, otherwise false.
     */
    public boolean hasEnchantmentCurse() {
        return hasEnchantmentCurse;
    }

    /**
     * Checks if the server version has particle support.
     *
     * @return True if the server version has particle support, otherwise false.
     */
    public boolean hasParticle() {
        return hasParticle;
    }

    /**
     * Checks if the server version is 1.7.
     *
     * @return True if the server version is 1.7, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_7() {
        return version.matches("(?i)v1_7_R1|v1_7_R2|v1_7_R3|v1_7_R4");
    }

    /**
     * Checks if the server version is 1.8.
     *
     * @return True if the server version is 1.8, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_8() {
        return version.matches("(?i)v1_8_R1|v1_8_R2|v1_8_R3");
    }

    /**
     * Checks if the server version is 1.9.
     *
     * @return True if the server version is 1.9, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_9() {
        return version.matches("(?i)v1_9_R1|v1_9_R2");
    }

    /**
     * Checks if the server version is 1.10.
     *
     * @return True if the server version is 1.10, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_10() {
        return version.matches("(?i)v1_10_R1");
    }

    /**
     * Checks if the server version is 1.11.
     *
     * @return True if the server version is 1.11, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_11() {
        return version.matches("(?i)v1_11_R1");
    }

    /**
     * Checks if the server version is 1.12.
     *
     * @return True if the server version is 1.12, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_12() {
        return version.matches("(?i)v1_12_R1");
    }

    /**
     * Checks if the server version is 1.13.
     *
     * @return True if the server version is 1.13, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_13() {
        return version.matches("(?i)v1_13_R1|v1_13_R2");
    }

    /**
     * Checks if the server version is 1.14.
     *
     * @return True if the server version is 1.14, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_14() {
        return version.matches("(?i)v1_14_R1");
    }

    /**
     * Checks if the server version is 1.15.
     *
     * @return True if the server version is 1.15, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_15() {
        return version.matches("(?i)v1_15_R1");
    }

    /**
     * Checks if the server version is 1.16.
     *
     * @return True if the server version is 1.16, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_16() {
        return version.matches("(?i)v1_16_R1|v1_16_R2|v1_16_R3");
    }

    /**
     * Checks if the server version is 1.17.
     *
     * @return True if the server version is 1.17, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_17() {
        return version.matches("(?i)v1_17_R1");
    }

    /**
     * Checks if the server version is 1.18.
     *
     * @return True if the server version is 1.18, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_18() {
        return version.matches("(?i)v1_18_R1|v1_18_R2");
    }

    /**
     * Checks if the server version is 1.19.
     *
     * @return True if the server version is 1.19, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_19() {
        return version.matches("(?i)v1_19_R1|v1_19_R2|v1_19_R3");
    }

    /**
     * Checks if the server version is 1.20.
     *
     * @return True if the server version is 1.20, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_20() {
        return version.matches("(?i)v1_20_R1|v1_20_R2|v1_20_R3");
    }

    /**
     * Checks if the server version is 1.20.5/6.
     *
     * @return True if the server version is 1.20.5/6, otherwise false.
     */
    public boolean is_v1_20_5() {
        return version.matches("(?i)v1_20_R4");
    }

    /**
     * Checks if the server version is 1.21/1.21.1.
     *
     * @return True if the server version is 1.21/1.21.1, otherwise false.
     */
    public boolean is_v1_21() {
        return version.matches("(?i)v1_21_R1|(?i)v1_21_R2|(?i)v1_21_R3|(?i)v1_21_R4");
    }

    public boolean is_v1_21_5() {
        return version.matches("(?i)v1_21_R5");
    }

    public boolean is_v1_21_8() {
        return version.matches("(?i)v1_21_R6");
    }

    public boolean isPost1_21_9() {
        return isPost1_21_9;
    }

    public boolean hasSpears() {
        return hasSpears;
    }

    public boolean is_v1_21_11() {
        return version.matches("(?i)v1_21_R7");
    }

    /**
     * Retrieves the appropriate particle type for the given version.
     *
     * @param particle The particle name.
     * @return The Particle enum corresponding to the given particle name.
     */
    public Particle getParticleForVersion(String particle) {
        return switch (particle) {
            case "REDSTONE", "REDDUST" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("REDSTONE");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("DUST");
                }
            }
            case "DUST" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("DUST");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("REDSTONE");
                }
            }
            case "ENCHANTMENT_TABLE", "ENCHANTMENTTABLE" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("ENCHANTMENT_TABLE");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("ENCHANT");
                }
            }
            case "ENCHANT" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("ENCHANT");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("ENCHANTMENT_TABLE");
                }
            }
            case "EXPLOSION_NORMAL", "EXPLODE" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("EXPLOSION_NORMAL");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("POOF");
                }
            }
            case "POOF" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("POOF");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("EXPLOSION_NORMAL");
                }
            }
            case "EXPLOSION_LARGE", "LARGE_EXPLOSION" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("EXPLOSION_LARGE");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("EXPLOSION");
                }
            }
            case "EXPLOSION" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("EXPLOSION");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("EXPLOSION_LARGE");
                }
            }
            case "EXPLOSION_HUGE", "HUGE_EXPLOSION" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("EXPLOSION_HUGE");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("EXPLOSION_EMITTER");
                }
            }
            case "EXPLOSION_EMITTER" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("EXPLOSION_EMITTER");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("EXPLOSION_HUGE");
                }
            }
            case "SMOKE_NORMAL" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SMOKE_NORMAL");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SMOKE");
                }
            }
            case "SMOKE" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SMOKE");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SMOKE_NORMAL");
                }
            }
            case "SMOKE_LARGE", "LARGESMOKE" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SMOKE_LARGE");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("LARGE_SMOKE");
                }
            }
            case "LARGE_SMOKE" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("LARGE_SMOKE");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SMOKE_LARGE");
                }
            }
            case "WATER_BUBBLE" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("WATER_BUBBLE");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("BUBBLE");
                }
            }
            case "BUBBLE" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("BUBBLE");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("WATER_BUBBLE");
                }
            }
            case "WATER_SPLASH", "SPLASH" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("WATER_SPLASH");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SPLASH");
                }
            }
            case "WATER_WAKE" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("WATER_WAKE");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("FISHING");
                }
            }
            case "FISHING" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("FISHING");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("WATER_WAKE");
                }
            }
            case "WATER_DROP", "DROPLET" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("WATER_DROP");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("RAIN");
                }
            }
            case "RAIN" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("RAIN");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("WATER_DROP");
                }
            }
            case "DRIP_WATER", "DRIPWATER" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("DRIP_WATER");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("DRIPPING_WATER");
                }
            }
            case "DRIPPING_WATER" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("DRIPPING_WATER");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("DRIP_WATER");
                }
            }
            case "DRIP_LAVA", "DRIPLAVA" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("DRIP_LAVA");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("DRIPPING_LAVA");
                }
            }
            case "DRIPPING_LAVA" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("DRIPPING_LAVA");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("DRIP_LAVA");
                }
            }
            case "SUSPENDED" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SUSPENDED");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("UNDERWATER");
                }
            }
            case "UNDERWATER" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("UNDERWATER");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SUSPENDED");
                }
            }
            case "SUSPENDED_DEPTH", "DEPTHSUSPEND" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SUSPENDED_DEPTH");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("UNDERWATER");
                }
            }
            case "TOWN_AURA", "TOWNAURA" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("TOWN_AURA");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("MYCELIUM");
                }
            }
            case "MYCELIUM" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("MYCELIUM");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("TOWN_AURA");
                }
            }
            case "VILLAGER_ANGRY", "ANGRYVILLAGER" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("VILLAGER_ANGRY");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("ANGRY_VILLAGER");
                }
            }
            case "ANGRY_VILLAGER" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("ANGRY_VILLAGER");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("VILLAGER_ANGRY");
                }
            }
            case "VILLAGER_HAPPY", "HAPPYVILLAGER" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("VILLAGER_HAPPY");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("HAPPY_VILLAGER");
                }
            }
            case "HAPPY_VILLAGER" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("HAPPY_VILLAGER");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("VILLAGER_HAPPY");
                }
            }
            case "SPELL" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SPELL");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("EFFECT");
                }
            }
            case "EFFECT" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("EFFECT");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SPELL");
                }
            }
            case "SPELL_INSTANT" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SPELL_INSTANT");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("INSTANT_EFFECT");
                }
            }
            case "INSTANT_EFFECT" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("INSTANT_EFFECT");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SPELL_INSTANT");
                }
            }
            case "SPELL_MOB", "SPELL_MOB_AMBIENT" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SPELL_MOB");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("ENTITY_EFFECT");
                }
            }
            case "ENTITY_EFFECT" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("ENTITY_EFFECT");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SPELL_MOB");
                }
            }
            case "SPELL_WITCH", "WITCHMAGIC" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SPELL_WITCH");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("WITCH");
                }
            }
            case "WITCH" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("WITCH");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SPELL_WITCH");
                }
            }
            case "CRIT_MAGIC", "MAGICCRIT" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("CRIT_MAGIC");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("ENCHANTED_HIT");
                }
            }
            case "ENCHANTED_HIT" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("ENCHANTED_HIT");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("CRIT_MAGIC");
                }
            }
            case "ICONCRACK", "ITEM_CRACK" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("ITEM_CRACK");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("ITEM");
                }
            }
            case "ITEM" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("ITEM");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("ITEM_CRACK");
                }
            }
            case "BLOCK_CRACK", "BLOCK_DUST" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("BLOCK_CRACK");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("BLOCK");
                }
            }
            case "BLOCK" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("BLOCK");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("BLOCK_CRACK");
                }
            }
            case "SNOWBALL" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SNOWBALL");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("ITEM_SNOWBALL");
                }
            }
            case "ITEM_SNOWBALL" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("ITEM_SNOWBALL");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SNOWBALL");
                }
            }
            case "SLIME" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("SLIME");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("ITEM_SLIME");
                }
            }
            case "ITEM_SLIME" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("ITEM_SLIME");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("SLIME");
                }
            }
            case "FIREWORKS_SPARK" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("FIREWORKS_SPARK");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("FIREWORK");
                }
            }
            case "FIREWORK" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("FIREWORK");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("FIREWORKS_SPARK");
                }
            }
            case "PORTAL", "NOTE", "FLAME", "HEART", "DRAGON_BREATH", "CLOUD", "CRIT", "LAVA",
                 "ASH", "ELECTRIC_SPARK" -> CompatibilityParticleEnum.valueOf(particle);
            case "BARRIER", "LIGHT" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf(particle);
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("BLOCK_MARKER");
                }
            }
            case "BLOCK_MARKER" -> {
                try {
                    yield CompatibilityParticleEnum.valueOf("BLOCK_MARKER");
                } catch (Exception e) {
                    yield CompatibilityParticleEnum.valueOf("BARRIER");
                }
            }
            default -> CompatibilityParticleEnum.valueOf(particle);
        };
    }

    /**
     * Retrieves the appropriate enchantment type for the given version.
     *
     * @param enchantment The enchantment name.
     * @return The Enchantment enum corresponding to the given enchantment name.
     */
    public Enchantment getEnchantmentForVersion(String enchantment) {
        Enchantment toReturn = switch (enchantment) {
            case "DURABILITY" -> {
                try {
                    Enchantment e = Enchantment.getByName("DURABILITY");
                    if (e == null) e = Enchantment.getByName("UNBREAKING"); // 1.20.5+
                    yield e;
                } catch (NullPointerException | IllegalArgumentException ex) {
                    yield Enchantment.getByName("UNBREAKING"); // 1.20.5+
                }
            }
            // Add other cases for different enchantments here
            default -> null;
        };

        if (toReturn == null) {
            throw new IllegalArgumentException("Enchantment can't be null. This is a bug.");
        }

        return toReturn;
    }

    /**
     * Retrieves the appropriate enchantment type for the given version.
     *
     * @param material The enchantment name.
     * @return The Material enum corresponding to the given material name.
     */
    public Material getMaterialForVersion(String material) {
        Material toReturn = switch (material) {
            case "RECOVERY_COMPASS" -> {
                try {
                    yield Material.valueOf("RECOVERY_COMPASS"); // 1.19+
                } catch (NullPointerException | IllegalArgumentException e) {
                    yield Material.valueOf("COMPASS"); // older than 1.19
                }
            }
            // Add other cases for different materials here
            default -> null;
        };

        if (toReturn == null) {
            throw new IllegalArgumentException("Material can't be null. This is a bug.");
        }

        return toReturn;
    }

    public PotionEffectType getPotionEffectTypeFromVersion(String potionEffect) {
        PotionEffectType toReturn = switch (potionEffect) {
            case "RESISTANCE" -> {
                try {
                    yield PotionEffectType.getByName("RESISTANCE");
                } catch (NullPointerException | IllegalArgumentException e) {
                    yield PotionEffectType.getByName("DAMAGE_RESISTANCE");
                }
            }
            case "FIRE_RESISTANCE" -> PotionEffectType.getByName("FIRE_RESISTANCE");
            // Add other cases for different sounds here
            default -> null;
        };

        if (toReturn == null) {
            throw new IllegalArgumentException("Potion Effect Type can't be null. This is a bug.");
        }

        return toReturn;
    }
}