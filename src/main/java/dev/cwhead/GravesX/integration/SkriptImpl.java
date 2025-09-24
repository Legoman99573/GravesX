package dev.cwhead.GravesX.integration;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.lang.ExpressionType;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.integration.skript.expressions.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

import java.util.List;

public class SkriptImpl {
    private final Graves plugin;
    private SkriptAddon skriptAddon;

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
            skriptAddon = null;
        }
    }

    /**
     * Registers the SkriptAddon listener with the Skript plugin.
     */
    private void register() {
        skriptAddon = Skript.registerAddon(plugin);
        try {
            skriptAddon.loadClasses("dev.cwhead.GravesX.event.integration.skript");
            Skript.registerExpression(ExprEventGrave.class, Grave.class, ExpressionType.SIMPLE, "[the] event[-]grave");
            Skript.registerExpression(ExprEventEntity.class, Entity.class, ExpressionType.SIMPLE, "[the] event[-]entity");
            Skript.registerExpression(ExprEventTargetEntity.class, LivingEntity.class, ExpressionType.SIMPLE, "[the] event[-]target[-]entity");
            Skript.registerExpression(ExprEventEntityType.class, EntityType.class, ExpressionType.SIMPLE, "[the] event[-]entity[-]type");
            Skript.registerExpression(ExprEventLocation.class, Location.class, ExpressionType.SIMPLE, "[the] event[-]location");
            Skript.registerExpression(ExprEventInventoryView.class, InventoryView.class, ExpressionType.SIMPLE, "[the] event[-]inventory[-]view");
            Skript.registerExpression(ExprEventLivingEntity.class, LivingEntity.class, ExpressionType.SIMPLE, "[the] event[-]living[-]entity");
            Skript.registerExpression(ExprEventBlockType.class, BlockData.BlockType.class, ExpressionType.SIMPLE, "[the] event[-]block[-]type");
            Skript.registerExpression(ExprEventBlockExp.class, Integer.class, ExpressionType.SIMPLE, "[the] event[-]blockexp");
            Skript.registerExpression(ExprEventBlock.class, Block.class, ExpressionType.SIMPLE, "[the] event[-]block");
            Skript.registerExpression(ExprEventPistonBlock.class, Block.class, ExpressionType.SIMPLE, "[the] event[-]piston[-]block");
            Skript.registerExpression(ExprEventDirection.class, BlockFace.class, ExpressionType.SIMPLE, "[the] event[-]direction");
            Skript.registerExpression(ExprEventMovedBlocks.class, List.class, ExpressionType.SIMPLE, "[the] event[-]moved[-]blocks");
            Skript.registerExpression(ExprEventPlayer.class, Player.class, ExpressionType.SIMPLE, "[the] event[-]player");
            plugin.integrationMessage("Skript integration loaded successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load Skript implementation");
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