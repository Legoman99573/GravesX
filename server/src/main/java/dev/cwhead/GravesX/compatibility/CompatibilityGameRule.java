package dev.cwhead.GravesX.compatibility;

import dev.cwhead.GravesX.exception.GravesXIllegalArgumentException;
import org.bukkit.GameRule;
import org.bukkit.World;

import java.lang.reflect.Method;

/**
 * Handles compatibility for GameRules to prevent runtime errors across versions.
 */
public final class CompatibilityGameRule {

    /**
     * Retrieves the {@link GameRule} value associated with the given gamerule name.
     *
     * <p>This calls {@code GameRule.getByName(String)} via reflection to avoid bytecode linkage issues
     * (e.g., {@link IncompatibleClassChangeError}) on mismatched compile/runtime environments.</p>
     *
     * <p>Uses the raw {@link GameRule} type so it works on older APIs where {@code GameRule} was not generic.</p>
     *
     * @param ruleName The gamerule name (e.g., "keepInventory", "randomTickSpeed").
     * @return The corresponding {@link GameRule}, or {@code null} if not found.
     */
    @SuppressWarnings({"rawtypes"})
    public static GameRule getByName(String ruleName) {
        if (ruleName == null || ruleName.isBlank()) return null;

        try {
            Method method = GameRule.class.getMethod("getByName", String.class);
            Object value = method.invoke(null, ruleName);
            return (value instanceof GameRule) ? (GameRule) value : null;
        } catch (NoSuchMethodException e) {
            throw new GravesXIllegalArgumentException("GameRule lookup is not supported on this server version.");
        } catch (Exception e) {
            throw new GravesXIllegalArgumentException("An issue occurred while retrieving gamerule " + ruleName);
        }
    }

    /**
     * Retrieves a gamerule value from a world using the best available API.
     *
     * <p>Prefers {@code World.getGameRuleValue(GameRule)} when available, otherwise falls back to
     * {@code World.getGameRuleValue(String)}.</p>
     *
     * @param world    The world to read the gamerule from.
     * @param ruleName The gamerule name (e.g., "keepInventory").
     * @return The gamerule value (Boolean/Integer/etc.), or {@code null} if unavailable.
     */
    @SuppressWarnings({"rawtypes"})
    public static Object getValue(World world, String ruleName) {
        if (world == null || ruleName == null || ruleName.isBlank()) return null;

        GameRule rule = getByName(ruleName);

        if (rule != null) {
            try {
                Method byRule = World.class.getMethod("getGameRuleValue", GameRule.class);
                return byRule.invoke(world, rule);
            } catch (NoSuchMethodException ignored) {
                // fall back
            } catch (Exception e) {
                throw new GravesXIllegalArgumentException("An issue occurred while reading gamerule " + ruleName + " from world " + world.getName());
            }
        }

        try {
            Method byString = World.class.getMethod("getGameRuleValue", String.class);
            return byString.invoke(world, ruleName);
        } catch (NoSuchMethodException e) {
            throw new GravesXIllegalArgumentException("GameRule values are not supported on this server version.");
        } catch (Exception e) {
            throw new GravesXIllegalArgumentException("An issue occurred while reading gamerule " + ruleName + " from world " + world.getName());
        }
    }

    /**
     * Retrieves a boolean gamerule value from a world.
     *
     * @param world    The world to read the gamerule from.
     * @param ruleName The gamerule name.
     * @return The boolean gamerule value.
     */
    public static boolean getBoolean(World world, String ruleName) {
        Object value = getValue(world, ruleName);

        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s);

        throw new GravesXIllegalArgumentException("GameRule " + ruleName + " is not a boolean gamerule on world " + (world != null ? world.getName() : "unknown"));
    }

    /**
     * Retrieves an integer gamerule value from a world.
     *
     * @param world    The world to read the gamerule from.
     * @param ruleName The gamerule name.
     * @return The integer gamerule value.
     */
    public static int getInt(World world, String ruleName) {
        Object value = getValue(world, ruleName);

        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }

        throw new GravesXIllegalArgumentException("GameRule " + ruleName + " is not an integer gamerule on world " + (world != null ? world.getName() : "unknown"));
    }

    /**
     * Sets a gamerule value on a world using the best available API.
     *
     * <p>Prefers {@code World.setGameRule(GameRule, T)} when available, otherwise falls back to
     * {@code World.setGameRuleValue(String, String)}.</p>
     *
     * @param world    The world to set the gamerule on.
     * @param ruleName The gamerule name.
     * @param value    The value to set (Boolean/Integer/String/etc.).
     */
    @SuppressWarnings({"rawtypes"})
    public static void setValue(World world, String ruleName, Object value) {
        if (world == null || ruleName == null || ruleName.isBlank()) return;

        GameRule rule = getByName(ruleName);

        if (rule != null) {
            try {
                for (Method m : World.class.getMethods()) {
                    if (!m.getName().equals("setGameRule")) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 2 && p[0] == GameRule.class) {
                        m.invoke(world, rule, value);
                        return;
                    }
                }
            } catch (Exception e) {
                throw new GravesXIllegalArgumentException("An issue occurred while setting gamerule " + ruleName + " on world " + world.getName());
            }
        }

        try {
            Method setString = World.class.getMethod("setGameRuleValue", String.class, String.class);
            setString.invoke(world, ruleName, String.valueOf(value));
        } catch (NoSuchMethodException e) {
            throw new GravesXIllegalArgumentException("Setting GameRules is not supported on this server version.");
        } catch (Exception e) {
            throw new GravesXIllegalArgumentException("An issue occurred while setting gamerule " + ruleName + " on world " + world.getName());
        }
    }
}
