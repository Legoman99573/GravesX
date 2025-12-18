package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilityParticleEnum;
import dev.cwhead.GravesX.compatibility.CompatibilitySoundEnum;
import dev.cwhead.GravesX.event.GraveSpearAttackEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Listens for EntityDamageByEntityEvent to manage damage to specific entities.
 */
public class EntityDamageByEntityListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new EntityDamageByEntityListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public EntityDamageByEntityListener(Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles EntityDamageByEntityEvent to determine if damage should be cancelled
     * based on the entity type and associated data.
     *
     * @param event The EntityDamageByEntityEvent to handle.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        final Entity entity = event.getEntity();

        if (shouldCancelDamage(entity)) {

            event.setCancelled(true);
            if (entity instanceof ArmorStand stand) {
                if (plugin.getVersionManager().hasSpears()) {
                    Grave grave = plugin.getEntityDataManager().getGrave(stand);

                    if (grave != null && plugin.getConfig("drop.spear-attack", grave).getBoolean("drop.spear-attack", false)) {
                        Entity damager = event.getDamager();

                        if (damager instanceof Player player) {
                            if (isSpear(player.getInventory().getItemInMainHand())) {
                                onSpearAttackGraveHologram(player, grave, stand, event);
                            }
                        } else if (damager instanceof LivingEntity living) {
                            ItemStack inHand = living.getEquipment() != null ? living.getEquipment().getItemInMainHand() : null;
                            if (isSpear(inHand)) {
                                onSpearAttackGraveHologram(living, grave, stand, event);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Determines if damage to the entity should be cancelled.
     *
     * @param entity The entity being damaged.
     * @return True if the damage should be cancelled, false otherwise.
     */
    private boolean shouldCancelDamage(Entity entity) {
        return (entity instanceof ItemFrame
                || (isVersion1_7OrAbove() && entity instanceof ArmorStand))
                && isAssociatedWithGrave(entity);
    }

    /**
     * Checks if the server version is 1.7 or above.
     *
     * @return True if the server version is 1.7 or above, false otherwise.
     */
    private boolean isVersion1_7OrAbove() {
        return plugin.getVersionManager().is_v1_7();
    }

    /**
     * Checks if the entity is associated with a grave.
     *
     * @param entity The entity to check.
     * @return True if the entity is associated with a grave, otherwise false.
     */
    private boolean isAssociatedWithGrave(Entity entity) {
        return plugin.getEntityDataManager().getGrave(entity) != null;
    }

    /**
     * Called when a spear attack hits a grave
     *
     * @param attacker The living entity attacking with a spear.
     * @param grave    The grave associated with the hologram.
     * @param stand    The hologram armor stand that was hit.
     * @param event    The original damage event.
     */
    private void onSpearAttackGraveHologram(LivingEntity attacker, Grave grave, ArmorStand stand, EntityDamageByEntityEvent event) {
        if (!plugin.getConfig("drop.spear-attack", grave).getBoolean("drop.spear-attack", false)) {
            return;
        }

        Location hitLoc = stand.getLocation();

        GraveSpearAttackEvent spearEvent = new GraveSpearAttackEvent(grave, attacker, hitLoc, attacker);
        plugin.getServer().getPluginManager().callEvent(spearEvent);

        if (spearEvent.isCancelled() || spearEvent.isAddon()) {
            return;
        }

        try {
            Location loc = grave.getLocationDeath();
            Objects.requireNonNull(loc.getWorld()).spawnParticle(CompatibilityParticleEnum.valueOf("EXPLOSION"), loc, 1);
            try {
                loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("ENTITY_GENERIC_EXPLODE")), 1.0f, 1.0f);
            } catch (Exception e) {
                loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("EXPLODE")), 1.0f, 1.0f);
            }
        } catch (Exception ignored) {
            // ignored
        }

        plugin.getGraveManager().breakGrave(hitLoc, grave);

        if (grave.getExperience() > 0) {
            plugin.getGraveManager().dropGraveExperience(hitLoc, grave);
        }

        if (attacker instanceof Player player && plugin.getIntegrationManager().hasNoteBlockAPI()) {
            if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForPlayer(player)) {
                plugin.getIntegrationManager().getNoteBlockAPI().stopSongForPlayer(player);
            }
            if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForAllPlayers()) {
                plugin.getIntegrationManager().getNoteBlockAPI().stopSongForAllPlayers();
            }
        }

        if (spearEvent.getEntity() instanceof Player player) {
            finalizeGraveBreak(player, hitLoc.getBlock(), grave);
        } else {
            finalizeGraveBreak(spearEvent.getEntity(), hitLoc.getBlock(), grave);
        }
    }

    /**
     * Checks whether the given item stack is a spear.
     *
     * @param item The item stack to check.
     * @return True if the item is a spear, otherwise false.
     */
    private boolean isSpear(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type.name().endsWith("_SPEAR");
    }

    /**
     * Finalizes the process of breaking a grave by closing the grave, playing effects, and running commands.
     *
     * @param player The player breaking the block.
     * @param block  The block being broken.
     * @param grave  The grave associated with the block.
     */
    private void finalizeGraveBreak(Player player, Block block, Grave grave) {
        plugin.getGraveManager().closeGrave(grave);
        plugin.getGraveManager().playEffect("effect.loot", block.getLocation(), grave);
        plugin.getEntityManager().spawnZombie(block.getLocation(), player, player, grave);
        plugin.getEntityManager().runCommands("event.command.break", player, block.getLocation(), grave);
    }

    /**
     * Finalizes the process of breaking a grave by closing the grave, playing effects, and running commands.
     *
     * @param entity The entity breaking the block.
     * @param block  The block being broken.
     * @param grave  The grave associated with the block.
     */
    private void finalizeGraveBreak(Entity entity, Block block, Grave grave) {
        plugin.getGraveManager().closeGrave(grave);
        plugin.getGraveManager().playEffect("effect.loot", block.getLocation(), grave);
        plugin.getEntityManager().spawnZombie(block.getLocation(), entity, (LivingEntity) entity, grave);
        plugin.getEntityManager().runCommands("event.command.break", entity, block.getLocation(), grave);
    }
}