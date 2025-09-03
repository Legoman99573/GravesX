package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.compatibility.CompatibilityParticleEnum;
import dev.cwhead.GravesX.compatibility.CompatibilitySoundEnum;
import dev.cwhead.GravesX.event.GraveProjectileHitEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Objects;

public class ProjectileHitListener implements Listener {
    private final Graves plugin;

    /**
     * Constructs a new ProjectileHitListener with the specified Graves plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public ProjectileHitListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHitGrave(ProjectileHitEvent event) {
        Block block = event.getHitBlock();
        if (block == null) return;

        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);
        if (grave == null) return;
        if (!plugin.getConfig("drop.projectile.enabled", grave).getBoolean("drop.projectile.enabled"))
            return;

        Location location = block.getLocation();
        ProjectileSource shooter = event.getEntity().getShooter();

        ShooterKind kind = classifyShooter(shooter);
        switch (kind) {
            case PLAYER:
                if (!plugin.getConfig("drop.projectile.player", grave).getBoolean("drop.projectile.player"))
                    return;
                handleProjectileHitFromPlayer((Player) shooter, event, block, grave, location);
                break;

            case LIVING:
                if (!plugin.getConfig("drop.projectile.living-entity", grave).getBoolean("drop.projectile.living-entity"))
                    return;
                handleProjectileHitFromLiving((LivingEntity) shooter, event, block, grave, location);
                break;

            case OTHER:
                if (!plugin.getConfig("drop.projectile.other", grave).getBoolean("drop.projectile.other"))
                    return;
                handleProjectileHitFromOther(event, block, grave, location);
                break;
        }
    }

    private ShooterKind classifyShooter(ProjectileSource shooter) {
        if (shooter instanceof Player)
            return ShooterKind.PLAYER;
        if (shooter instanceof LivingEntity)
            return ShooterKind.LIVING;
        return ShooterKind.OTHER;
    }

    private void handleProjectileHitFromPlayer(Player player, ProjectileHitEvent event, Block block, Grave grave, Location location) {
        GraveProjectileHitEvent modern =
                new GraveProjectileHitEvent(location, player, grave, event.getEntity(), block);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveProjectileHitEvent legacy =
                new com.ranull.graves.event.GraveProjectileHitEvent(location, player, grave, event.getEntity(), block);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon()) return;

        Location effectiveLoc = modern.hasLocation() ? modern.getLocation()
                : (legacy.hasLocation() ? legacy.getLocation() : location);

        playLootedExplosionEffectIfEnabled(grave);
        breakCloseAndEffect(effectiveLoc, grave);

        if (plugin.getIntegrationManager().hasNoteBlockAPI()) {
            if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForPlayer(player)) {
                plugin.getIntegrationManager().getNoteBlockAPI().stopSongForPlayer(player);
            }
            if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForAllPlayers()) {
                plugin.getIntegrationManager().getNoteBlockAPI().stopSongForAllPlayers();
            }
        }

        String playerName = player.getDisplayName();
        plugin.debugMessage("Grave destroyed by projectile "
                + event.getEntity().getName().toLowerCase() + " caused by player " + playerName + ".", 1);
    }

    private void handleProjectileHitFromLiving(LivingEntity livingShooter, ProjectileHitEvent event, Block block, Grave grave, Location location) {
        GraveProjectileHitEvent modern = new GraveProjectileHitEvent(location, livingShooter, grave, event.getEntity(), block);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveProjectileHitEvent legacy =
                new com.ranull.graves.event.GraveProjectileHitEvent(location, livingShooter, grave, event.getEntity(), block);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon()) return;

        Location effectiveLoc = modern.hasLocation() ? modern.getLocation()
                : (legacy.hasLocation() ? legacy.getLocation() : location);

        breakCloseAndEffect(effectiveLoc, grave);

        if (plugin.getIntegrationManager().hasNoteBlockAPI()) {
            if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForPlayer(grave.getOwnerUUID())) {
                plugin.getIntegrationManager().getNoteBlockAPI().stopSongForPlayer(grave.getOwnerUUID());
            }
            if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForAllPlayers()) {
                plugin.getIntegrationManager().getNoteBlockAPI().stopSongForAllPlayers();
            }
        }

        plugin.debugMessage("Grave destroyed by projectile "
                + event.getEntity().getName().toLowerCase()
                + "  caused by living entity " + livingShooter.getType().name().toLowerCase() + ".", 1);
    }

    private void handleProjectileHitFromOther(ProjectileHitEvent event,
                                              Block block,
                                              Grave grave,
                                              Location location) {

        GraveProjectileHitEvent modern =
                new GraveProjectileHitEvent(location, grave, event.getEntity(), block);
        plugin.getServer().getPluginManager().callEvent(modern);

        com.ranull.graves.event.GraveProjectileHitEvent legacy =
                new com.ranull.graves.event.GraveProjectileHitEvent(location, grave, event.getEntity(), block);
        plugin.getServer().getPluginManager().callEvent(legacy);

        if (modern.isCancelled() || modern.isAddon() || legacy.isCancelled() || legacy.isAddon()) return;

        Location effectiveLoc = modern.hasLocation() ? modern.getLocation()
                : (legacy.hasLocation() ? legacy.getLocation() : location);

        breakCloseAndEffect(effectiveLoc, grave);

        if (plugin.getIntegrationManager().hasNoteBlockAPI()) {
            if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForPlayer(grave.getOwnerUUID())) {
                plugin.getIntegrationManager().getNoteBlockAPI().stopSongForPlayer(grave.getOwnerUUID());
            }
            if (plugin.getIntegrationManager().getNoteBlockAPI().isSongPlayingForAllPlayers()) {
                plugin.getIntegrationManager().getNoteBlockAPI().stopSongForAllPlayers();
            }
        }

        plugin.debugMessage("Grave destroyed by projectile "
                + event.getEntity().getName().toLowerCase()
                + "  caused by unknown player or living entity.", 1);
    }

    private void playLootedExplosionEffectIfEnabled(Grave grave) {
        if (!plugin.getConfig("drop.looted-explosion-effect", grave).getBoolean("drop.looted-explosion-effect", false)) return;

        try {
            Location loc = grave.getLocationDeath();
            Objects.requireNonNull(loc.getWorld()).spawnParticle(CompatibilityParticleEnum.valueOf("EXPLOSION"), loc, 1);
            try {
                loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("ENTITY_GENERIC_EXPLODE")), 1.0f, 1.0f);
            } catch (Exception e) {
                loc.getWorld().playSound(loc, Objects.requireNonNull(CompatibilitySoundEnum.valueOf("EXPLODE")), 1.0f, 1.0f); // pre 1.9
            }
        } catch (Exception ignored) { /* ignored */ }
    }

    private void breakCloseAndEffect(Location loc, Grave grave) {
        plugin.getGraveManager().breakGrave(loc, grave);
        plugin.getGraveManager().closeGrave(grave);
        plugin.getGraveManager().playEffect("effect.loot", loc, grave);
    }

    private enum ShooterKind {
        PLAYER,
        LIVING,
        OTHER
    }
}