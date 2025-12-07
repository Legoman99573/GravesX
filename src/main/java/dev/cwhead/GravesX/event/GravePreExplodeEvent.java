package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import dev.cwhead.GravesX.exception.GravesXEventNullPointerException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GravePreExplodeEvent extends GraveEvent {

    /**
     * The entity causing the explosion (e.g. TNT, Creeper), may be null.
     */
    private @Nullable Entity source;

    /**
     * The radius of the primed explosion.
     */
    private float radius;

    /**
     * Constructs a new GravePreExplosionEvent.
     *
     * @param grave             The grave that will be affected by the explosion.
     * @param explosionLocation The location where the explosion is primed to occur.
     * @param source            The entity causing the explosion, may be null.
     * @param radius            The radius of the primed explosion.
     */
    public GravePreExplodeEvent(@NotNull Grave grave,
                                  @NotNull Location explosionLocation,
                                  @Nullable Entity source,
                                  float radius) {
        super(grave, explosionLocation, null, null);
        this.source = source;
        this.radius = radius;
    }

    /**
     * The entity that is causing the explosion (TNT, Creeper, etc.), or null.
     */
    public @Nullable Entity getSource() {
        return source;
    }

    /**
     * Sets the entity that is causing the explosion (TNT, Creeper, etc.), or null.
     *
     * @param source The new source entity, or null.
     */
    public void setSource(@Nullable Entity source) {
        this.source = source;
    }

    /**
     * The type of the source entity causing the explosion, or null if there is no source.
     */
    public @Nullable EntityType getSourceType() {
        return source != null ? source.getType() : null;
    }

    /**
     * Convenience alias for readability – the explosion center.
     */
    public @NotNull Location getExplosionLocation() {
        return getLocation();
    }

    /**
     * Sets the explosion center location.
     *
     * @param location The new explosion location.
     */
    public void setExplosionLocation(@NotNull Location location) {
        setLocation(Objects.requireNonNull(location, "location"));
    }

    /**
     * The radius of the primed explosion.
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Sets the radius of the primed explosion.
     *
     * @param radius The new radius.
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Checks whether the explosion location has an associated world.
     *
     * @return true if the world is not null, false otherwise.
     */
    public boolean hasWorld() {
        return getExplosionLocation().getWorld() != null;
    }

    /**
     * Gets the world of the explosion location.
     *
     * @return The world of the explosion.
     * @throws GravesXEventNullPointerException if the world is null.
     */
    public @NotNull World getWorld() {
        World world = getExplosionLocation().getWorld();
        if (world != null) {
            return world;
        }
        throw new GravesXEventNullPointerException(this, "world");
    }

    /**
     * Gets the world of the explosion location, or null if not set.
     *
     * @return The world of the explosion, or null.
     */
    public @Nullable World getWorldNullable() {
        return getExplosionLocation().getWorld();
    }

    /**
     * Sets the world of the explosion location while keeping its coordinates.
     *
     * @param world The new world.
     */
    public void setWorld(@NotNull World world) {
        Location loc = getExplosionLocation();
        loc.setWorld(Objects.requireNonNull(world, "world"));
        setLocation(loc);
    }
}
