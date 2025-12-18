package dev.cwhead.GravesX.event;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Called when a living entity attacks a grave hologram with a spear.
 * <p>
 * This event is intended to be fired from {@code EntityDamageByEntityListener} when a grave hologram
 * {@link org.bukkit.entity.ArmorStand} is hit and {@code drop.spear-attack} is enabled for the grave.
 * </p>
 */
public class GraveSpearAttackEvent extends GraveEntityEvent {

    /**
     * The location where the spear attack hit (typically the hologram armor stand location).
     */
    private final @NotNull Location hitLocation;

    /**
     * Constructs a new {@code GraveSpearAttackEvent}.
     *
     * @param grave        The grave associated with the hologram.
     * @param entity       The entity involved in the event (the damager, non-null).
     * @param hitLocation  The location where the spear attack hit.
     * @param livingEntity The living entity performing the spear attack.
     * @throws dev.cwhead.GravesX.exception.GravesXNullPointerException if any required parameter is null.
     */
    public GraveSpearAttackEvent(
            @NotNull Grave grave,
            @NotNull Entity entity,
            @NotNull Location hitLocation,
            @NotNull LivingEntity livingEntity
    ) {
        super(
                Objects.requireNonNull(grave, "grave"),
                Objects.requireNonNull(entity, "entity"),
                Objects.requireNonNull(hitLocation, "hitLocation"),
                null,
                null,
                Objects.requireNonNull(livingEntity, "livingEntity"),
                null
        );

        this.hitLocation = hitLocation;
    }

    /**
     * Gets the location where the spear attack hit.
     *
     * @return The hit location.
     */
    public @NotNull Location getHitLocation() {
        return hitLocation;
    }

    /**
     * Gets the spear attacker.
     *
     * @return The attacking living entity.
     */
    public @NotNull LivingEntity getAttacker() {
        return getLivingEntity();
    }
}