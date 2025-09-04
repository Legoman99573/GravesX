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
public final class VersionManager {
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

    private boolean isPaper;

    private boolean isFolia;

    private final boolean isPost1_20_5;

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

        try {
            Class.forName("org.spigotmc.SpigotConfig", false, getClass().getClassLoader());
            this.isBukkit = false;
        } catch (ClassNotFoundException ignored) {
            this.isBukkit = true;
        }

        try {
            Class.forName("ca.spottedleaf.moonrise.common.util.TickThread", false, getClass().getClassLoader());
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
        return version.matches("(?i)v1_21_R1");
    }

    public boolean isPost1_20_5() {
        return isPost1_20_5;
    }

    /**
     * Retrieves the appropriate particle type for the given version.
     *
     * @param particle The particle name.
     * @return The Particle enum corresponding to the given particle name.
     */
    public Particle getParticleForVersion(String particle) {
        Particle toReturn;
        switch (particle) {
            case "REDSTONE":
            case "REDDUST":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("REDSTONE");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("DUST");
                }
                break;
            case "DUST":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("DUST");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("REDSTONE");
                }
                break;
            case "ENCHANTMENT_TABLE":
            case "ENCHANTMENTTABLE":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("ENCHANTMENT_TABLE");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("ENCHANT");
                }
                break;
            case "ENCHANT":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("ENCHANT");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("ENCHANTMENT_TABLE");
                }
                break;
            case "EXPLOSION_NORMAL":
            case "EXPLODE":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("EXPLOSION_NORMAL");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("POOF");
                }
                break;
            case "POOF":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("POOF");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("EXPLOSION_NORMAL");
                }
                break;
            case "EXPLOSION_LARGE":
            case "LARGE_EXPLOSION":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("EXPLOSION_LARGE");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("EXPLOSION");
                }
                break;
            case "EXPLOSION":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("EXPLOSION");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("EXPLOSION_LARGE");
                }
                break;
            case "EXPLOSION_HUGE":
            case "HUGE_EXPLOSION":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("EXPLOSION_HUGE");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("EXPLOSION_EMITTER");
                }
                break;
            case "EXPLOSION_EMITTER":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("EXPLOSION_EMITTER");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("EXPLOSION_HUGE");
                }
                break;
            case "SMOKE_NORMAL":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SMOKE_NORMAL");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SMOKE");
                }
                break;
            case "SMOKE":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SMOKE");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SMOKE_NORMAL");
                }
                break;
            case "SMOKE_LARGE":
            case "LARGESMOKE":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SMOKE_LARGE");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("LARGE_SMOKE");
                }
                break;
            case "LARGE_SMOKE":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("LARGE_SMOKE");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SMOKE_LARGE");
                }
                break;
            case "WATER_BUBBLE":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("WATER_BUBBLE");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("BUBBLE");
                }
                break;
            case "BUBBLE":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("BUBBLE");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("WATER_BUBBLE");
                }
                break;
            case "WATER_SPLASH":
            case "SPLASH":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("WATER_SPLASH");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SPLASH");
                }
                break;
            case "WATER_WAKE":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("WATER_WAKE");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("FISHING");
                }
                break;
            case "FISHING":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("FISHING");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("WATER_WAKE");
                }
                break;
            case "WATER_DROP":
            case "DROPLET":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("WATER_DROP");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("RAIN");
                }
                break;
            case "RAIN":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("RAIN");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("WATER_DROP");
                }
                break;
            case "DRIP_WATER":
            case "DRIPWATER":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("DRIP_WATER");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("DRIPPING_WATER");
                }
                break;
            case "DRIPPING_WATER":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("DRIPPING_WATER");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("DRIP_WATER");
                }
                break;
            case "DRIP_LAVA":
            case "DRIPLAVA":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("DRIP_LAVA");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("DRIPPING_LAVA");
                }
                break;
            case "DRIPPING_LAVA":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("DRIPPING_LAVA");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("DRIP_LAVA");
                }
                break;
            case "SUSPENDED":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SUSPENDED");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("UNDERWATER");
                }
                break;
            case "UNDERWATER":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("UNDERWATER");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SUSPENDED");
                }
                break;
            case "SUSPENDED_DEPTH":
            case "DEPTHSUSPEND":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SUSPENDED_DEPTH");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("UNDERWATER");
                }
                break;
            case "TOWN_AURA":
            case "TOWNAURA":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("TOWN_AURA");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("MYCELIUM");
                }
                break;
            case "MYCELIUM":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("MYCELIUM");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("TOWN_AURA");
                }
                break;
            case "VILLAGER_ANGRY":
            case "ANGRYVILLAGER":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("VILLAGER_ANGRY");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("ANGRY_VILLAGER");
                }
                break;
            case "ANGRY_VILLAGER":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("ANGRY_VILLAGER");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("VILLAGER_ANGRY");
                }
                break;
            case "VILLAGER_HAPPY":
            case "HAPPYVILLAGER":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("VILLAGER_HAPPY");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("HAPPY_VILLAGER");
                }
                break;
            case "HAPPY_VILLAGER":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("HAPPY_VILLAGER");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("VILLAGER_HAPPY");
                }
                break;
            case "SPELL":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SPELL");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("EFFECT");
                }
                break;
            case "EFFECT":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("EFFECT");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SPELL");
                }
                break;
            case "SPELL_INSTANT":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SPELL_INSTANT");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("INSTANT_EFFECT");
                }
                break;
            case "INSTANT_EFFECT":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("INSTANT_EFFECT");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SPELL_INSTANT");
                }
                break;
            case "SPELL_MOB":
            case "SPELL_MOB_AMBIENT":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SPELL_MOB");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("ENTITY_EFFECT");
                }
                break;
            case "ENTITY_EFFECT":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("ENTITY_EFFECT");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SPELL_MOB");
                }
                break;
            case "SPELL_WITCH":
            case "WITCHMAGIC":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SPELL_WITCH");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("WITCH");
                }
                break;
            case "WITCH":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("WITCH");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SPELL_WITCH");
                }
                break;
            case "CRIT_MAGIC":
            case "MAGICCRIT":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("CRIT_MAGIC");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("ENCHANTED_HIT");
                }
                break;
            case "ENCHANTED_HIT":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("ENCHANTED_HIT");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("CRIT_MAGIC");
                }
                break;
            case "ICONCRACK":
            case "ITEM_CRACK":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("ITEM_CRACK");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("ITEM");
                }
                break;
            case "ITEM":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("ITEM");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("ITEM_CRACK");
                }
                break;
            case "BLOCK_CRACK":
            case "BLOCK_DUST":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("BLOCK_CRACK");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("BLOCK");
                }
                break;
            case "BLOCK":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("BLOCK");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("BLOCK_CRACK");
                }
                break;
            case "SNOWBALL":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SNOWBALL");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("ITEM_SNOWBALL");
                }
                break;
            case "ITEM_SNOWBALL":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("ITEM_SNOWBALL");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SNOWBALL");
                }
                break;
            case "SLIME":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("SLIME");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("ITEM_SLIME");
                }
                break;
            case "ITEM_SLIME":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("ITEM_SLIME");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("SLIME");
                }
                break;
            case "FIREWORKS_SPARK":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("FIREWORKS_SPARK");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("FIREWORK");
                }
                break;
            case "FIREWORK":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("FIREWORK");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("FIREWORKS_SPARK");
                }
                break;
            case "PORTAL":
            case "NOTE":
            case "FLAME":
            case "HEART":
            case "DRAGON_BREATH":
            case "CLOUD":
            case "CRIT":
            case "LAVA":
            case "ASH":
            case "ELECTRIC_SPARK":
                toReturn = CompatibilityParticleEnum.valueOf(particle);
                break;
            case "BARRIER":
            case "LIGHT":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf(particle);
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("BLOCK_MARKER");
                }
                break;
            case "BLOCK_MARKER":
                try {
                    toReturn = CompatibilityParticleEnum.valueOf("BLOCK_MARKER");
                } catch (Exception e) {
                    toReturn = CompatibilityParticleEnum.valueOf("BARRIER");
                }
                break;
            default:
                toReturn = CompatibilityParticleEnum.valueOf(particle);
                break;
        }
        return toReturn;
    }

    /**
     * Retrieves the appropriate enchantment type for the given version.
     *
     * @param enchantment The enchantment name.
     * @return The Enchantment enum corresponding to the given enchantment name.
     */
    public Enchantment getEnchantmentForVersion(String enchantment) {
        Enchantment toReturn = null;
        switch (enchantment) {
            case "DURABILITY":
                try {
                    toReturn = Enchantment.getByName("DURABILITY");
                    if (toReturn == null) {
                        toReturn = Enchantment.getByName("UNBREAKING"); // Assume server is running on 1.20.5 or newer
                    }
                } catch (NullPointerException | IllegalArgumentException e) {
                    toReturn = Enchantment.getByName("UNBREAKING"); // Assume server is running on 1.20.5 or newer
                }
                break;
            // Add other cases for different enchantments here
        }

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
        Material toReturn = null;
        switch (material) {
            case "RECOVERY_COMPASS":
                try {
                    toReturn = Material.valueOf("RECOVERY_COMPASS"); // Server is running on 1.19 or newer
                } catch (NullPointerException | IllegalArgumentException e) {
                    toReturn = Material.valueOf("COMPASS"); // Server is older than 1.19
                }
                break;
                // Add other cases for different materials here
        }
        if (toReturn == null) {
            throw new IllegalArgumentException("Material can't be null. This is a bug.");
        }

        return toReturn;
    }

    public PotionEffectType getPotionEffectTypeFromVersion(String potionEffect) {
        PotionEffectType toReturn = null;
        switch (potionEffect) {
            case "RESISTANCE":
                try {
                    toReturn = PotionEffectType.getByName("RESISTANCE");
                } catch (NullPointerException | IllegalArgumentException e) {
                    toReturn = PotionEffectType.getByName("DAMAGE_RESISTANCE");
                }
                break;
            case "FIRE_RESISTANCE":
                toReturn = PotionEffectType.getByName("FIRE_RESISTANCE");
            // Add other cases for different sounds here
        }
        if (toReturn == null) {
            throw new IllegalArgumentException("Potion Effect Type can't be null. This is a bug.");
        }

        return toReturn;
    }
}