package dev.cwhead.GravesX.compatibility;

import dev.cwhead.GravesX.exception.GravesXEventIllegalArgumentException;
import org.bukkit.Particle;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Handles compatibility for Particle enums to prevent runtime errors across versions.
 */
public final class CompatibilityParticleEnum {

    /**
     * Retrieves the {@link Particle} value associated with the given particle name.
     *
     * <p>For modern versions, this calls {@code Particle.valueOf(String)} via reflection.
     * If the particle is not present on the running server, the method logs the error and returns {@code null}.</p>
     *
     * @param particleName The particle name (e.g., "FLAME", "angry_villager").
     * @return The corresponding {@link Particle} value, or {@code null} if not found or an error occurs.
     */
    public static Particle valueOf(String particleName) {
        if (particleName == null || particleName.isBlank()) return null;

        String normalized = particleName.toUpperCase(Locale.ROOT);

        try {
            Method method = Particle.class.getMethod("valueOf", String.class);
            return (Particle) method.invoke(null, normalized);
        } catch (IllegalArgumentException | NoSuchMethodException e) {
            throw new GravesXEventIllegalArgumentException("Particle " + normalized + " does not exist on this server version.");
        } catch (Exception e) {
            throw new GravesXEventIllegalArgumentException("An issue occurred while retrieving particle enum " + normalized);
        }
    }
}