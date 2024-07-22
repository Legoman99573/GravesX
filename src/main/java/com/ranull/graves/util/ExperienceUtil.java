package com.ranull.graves.util;

import org.bukkit.entity.Player;

/**
 * Utility class for handling experience-related operations for players.
 */
public final class ExperienceUtil {

    /**
     * Gets the total experience of a player.
     *
     * @param player The player to get the experience from.
     * @return The total experience of the player.
     */
    public static int getPlayerExperience(Player player) {
        int experience = Math.round(getExperienceAtLevel(player.getLevel()) * player.getExp());
        int level = player.getLevel();

        while (level > 0) {
            level--;
            experience += getExperienceAtLevel(level);
        }

        if (experience < 0) {
            return -1;
        }

        return experience;
    }

    /**
     * Gets the experience required to reach a specific level.
     *
     * @param level The level to get the experience for.
     * @return The experience required to reach the specified level.
     */
    public static int getExperienceAtLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        }

        return 9 * level - 158;
    }

    /**
     * Calculates the level from a given amount of experience.
     *
     * @param experience The experience to calculate the level from.
     * @return The level corresponding to the given experience.
     */
    public static long getLevelFromExperience(long experience) {
        double result = 0;

        if (experience > 1395) {
            result = (Math.sqrt(72 * experience - 54215) + 325) / 18;
        } else if (experience > 315) {
            result = Math.sqrt(40 * experience - 7839) / 10 + 8.1;
        } else if (experience > 0) {
            result = Math.sqrt(experience + 9) - 3;
        }

        return (long) (Math.round(result * 100.0) / 100.0);
    }

    /**
     * Calculates the drop percentage of experience.
     *
     * @param experience The total experience.
     * @param percent    The percentage to drop.
     * @return The experience drop amount.
     */
    public static int getDropPercent(int experience, float percent) {
        return experience > 0 ? (int) (experience * percent) : 0;
    }

    /**
     * Gets the amount of experience a player will drop upon death based on a percentage.
     *
     * @param player          The player to get the drop experience from.
     * @param expStorePercent The percentage of experience to drop.
     * @return The amount of experience to drop.
     */
    public static int getPlayerDropExperience(Player player, float expStorePercent) {
        int experience = getPlayerExperience(player);

        return experience > 0 ? (int) (experience * expStorePercent) : 0;
    }
}