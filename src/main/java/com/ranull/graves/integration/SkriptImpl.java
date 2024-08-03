package com.ranull.graves.integration;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.lang.ExpressionType;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.event.integration.skript.expressions.*;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

public class SkriptImpl {
    private final Graves plugin;
    private SkriptAddon skriptAddon;
    private Skript skript;

    /**
     * Constructs a SkriptIntegration instance and registers it with the Skript plugin.
     *
     * @param plugin The Graves plugin instance.
     */
    public SkriptImpl(Graves plugin) {
        this.plugin = plugin;

        unregister();
        register();
    }

    /**
     * Unregisters the current SkriptAddon listener if it exists.
     */
    private void unregister() {
        if (skriptAddon != null) {
            // Skript doesn't provide a direct way to unregister addons
            // Generally, this involves cleaning up any event registrations manually
            // For simplicity, we are not handling unregistering here
            skriptAddon = null;
        }
    }

    /**
     * Registers the SkriptAddon listener with the Skript plugin.
     */
    private void register() {
        SkriptAddon addon = Skript.registerAddon(plugin);
        try {
            addon.loadClasses("com.ranull.graves.event.integration.skript");
            Skript.registerExpression(ExprEventGrave.class, Grave.class, ExpressionType.SIMPLE, "[the] event[-]grave");
            Skript.registerExpression(ExprEventEntity.class, Entity.class, ExpressionType.SIMPLE, "[the] event[-]entity");
            Skript.registerExpression(ExprEventTargetEntity.class, LivingEntity.class, ExpressionType.SIMPLE, "[the] event[-]target[-]entity");
            Skript.registerExpression(ExprEventEntityType.class, EntityType.class, ExpressionType.SIMPLE, "[the] event[-]entity[-]type");
            Skript.registerExpression(ExprEventLocation.class, Location.class, ExpressionType.SIMPLE, "[the] event[-]location");
            Skript.registerExpression(ExprEventInventoryView.class, InventoryView.class, ExpressionType.SIMPLE, "[the] event[-]inventory[-]view");
            Skript.registerExpression(ExprEventLivingEntity.class, LivingEntity.class, ExpressionType.SIMPLE, "[the] event[-]living[-]entity");
            Skript.registerExpression(ExprEventBlockType.class, BlockData.BlockType.class, ExpressionType.SIMPLE, "[the] event[-]block[-]type");
            Skript.registerExpression(ExprEventBlock.class, Block.class, ExpressionType.SIMPLE, "[the] event[-]block");
            Skript.registerExpression(ExprEventPlayer.class, Player.class, ExpressionType.SIMPLE, "[the] event[-]player");
            plugin.integrationMessage("Skript integration loaded successfully.");
        } catch (Exception e) {
            plugin.logStackTrace(e);
        }
    }

    /**
     * Gets the SkriptAddon instance.
     *
     * @return The SkriptAddon instance.
     */
    public SkriptAddon getSkriptAddon() {
        return skriptAddon;
    }
}
