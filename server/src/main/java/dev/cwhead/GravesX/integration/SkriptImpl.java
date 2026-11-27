package dev.cwhead.GravesX.integration;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.api.grave.GraveCreationAPI;
import dev.cwhead.GravesX.api.grave.GraveManagementAPI;
import dev.cwhead.GravesX.api.util.UtilAPI;
import dev.cwhead.GravesX.api.world.LocationAPI;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveAbandoned;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveAutoLoot;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveBlockPlace;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveBreak;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveClose;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveCompassAdd;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveCompassUse;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveCreate;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveExplode;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveItemTake;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveLooted;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveObituaryAdd;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveOpen;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveParticle;
import dev.cwhead.GravesX.event.integration.skript.EvtGravePistonExtend;
import dev.cwhead.GravesX.event.integration.skript.EvtGravePlayerHeadDrop;
import dev.cwhead.GravesX.event.integration.skript.EvtGravePostCreate;
import dev.cwhead.GravesX.event.integration.skript.EvtGravePostTeleport;
import dev.cwhead.GravesX.event.integration.skript.EvtGravePreExplode;
import dev.cwhead.GravesX.event.integration.skript.EvtGravePreTeleport;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveProjectileHit;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveProtectionCreate;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveProtectionExpired;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveTeleport;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveTimeout;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveVirtualOpen;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveWalkOver;
import dev.cwhead.GravesX.event.integration.skript.EvtGraveZombieSpawn;
import dev.cwhead.GravesX.event.integration.skript.conditions.CondGraveLocked;
import dev.cwhead.GravesX.event.integration.skript.conditions.CondGraveHasAvailableSlot;
import dev.cwhead.GravesX.event.integration.skript.conditions.CondIsGrave;
import dev.cwhead.GravesX.event.integration.skript.conditions.CondIsNearGrave;
import dev.cwhead.GravesX.event.integration.skript.effects.EffAbandonGrave;
import dev.cwhead.GravesX.event.integration.skript.effects.EffAddItemToGrave;
import dev.cwhead.GravesX.event.integration.skript.effects.EffAutoLootGrave;
import dev.cwhead.GravesX.event.integration.skript.effects.EffBreakGrave;
import dev.cwhead.GravesX.event.integration.skript.effects.EffCreateGrave;
import dev.cwhead.GravesX.event.integration.skript.effects.EffDropGraveItems;
import dev.cwhead.GravesX.event.integration.skript.effects.EffRemoveGrave;
import dev.cwhead.GravesX.event.integration.skript.effects.EffRemoveOldestGrave;
import dev.cwhead.GravesX.event.integration.skript.effects.EffSetGraveProtection;
import dev.cwhead.GravesX.event.integration.skript.expressions.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.List;

public class SkriptImpl {
    private static SkriptAddon activeSkriptAddon;
    private static GraveCreationAPI activeGraveCreationAPI;
    private static GraveManagementAPI activeGraveManagementAPI;

    private final Graves plugin;
    private SkriptAddon skriptAddon;

    /**
     * Constructs a SkriptIntegration instance and registers it with Skript.
     *
     * @param plugin The Graves plugin instance.
     */
    public SkriptImpl(Graves plugin) {
        this.plugin = plugin;

        register();
    }

    /**
     * Registers GravesX Skript support using the Skript 2.15.3 API.
     */
    private void register() {
        try {
            skriptAddon = Skript.instance().registerAddon(Graves.class, plugin.getName());
            activeSkriptAddon = skriptAddon;

            LocationAPI locationAPI = new LocationAPI(plugin);
            UtilAPI utilAPI = new UtilAPI(plugin, locationAPI);
            activeGraveManagementAPI = new GraveManagementAPI(plugin);
            activeGraveCreationAPI = new GraveCreationAPI(plugin, locationAPI, utilAPI, activeGraveManagementAPI);

            SkriptTypes.register();
            registerStaticSyntaxClasses();
            registerExpressions();

            plugin.integrationMessage("Skript integration loaded successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load Skript implementation");
            plugin.logStackTrace(e);
        }
    }

    /**
     * Initializes syntax classes that intentionally register from static blocks.
     *
     * @throws ClassNotFoundException If a class cannot be initialized.
     */
    private void registerStaticSyntaxClasses() throws ClassNotFoundException {
        initializeClass(EvtGraveAbandoned.class);
        initializeClass(EvtGraveAutoLoot.class);
        initializeClass(EvtGraveBlockPlace.class);
        initializeClass(EvtGraveBreak.class);
        initializeClass(EvtGraveClose.class);
        initializeClass(EvtGraveCompassAdd.class);
        initializeClass(EvtGraveCompassUse.class);
        initializeClass(EvtGraveCreate.class);
        initializeClass(EvtGraveExplode.class);
        initializeClass(EvtGraveItemTake.class);
        initializeClass(EvtGraveLooted.class);
        initializeClass(EvtGraveObituaryAdd.class);
        initializeClass(EvtGraveOpen.class);
        initializeClass(EvtGraveParticle.class);
        initializeClass(EvtGravePistonExtend.class);
        initializeClass(EvtGravePlayerHeadDrop.class);
        initializeClass(EvtGravePostCreate.class);
        initializeClass(EvtGravePostTeleport.class);
        initializeClass(EvtGravePreExplode.class);
        initializeClass(EvtGravePreTeleport.class);
        initializeClass(EvtGraveProjectileHit.class);
        initializeClass(EvtGraveProtectionCreate.class);
        initializeClass(EvtGraveProtectionExpired.class);
        initializeClass(EvtGraveTeleport.class);
        initializeClass(EvtGraveTimeout.class);
        initializeClass(EvtGraveVirtualOpen.class);
        initializeClass(EvtGraveWalkOver.class);
        initializeClass(EvtGraveZombieSpawn.class);

        initializeClass(EffCreateGrave.class);
        initializeClass(EffRemoveGrave.class);
        initializeClass(EffBreakGrave.class);
        initializeClass(EffAutoLootGrave.class);
        initializeClass(EffAbandonGrave.class);
        initializeClass(EffDropGraveItems.class);
        initializeClass(EffRemoveOldestGrave.class);
        initializeClass(EffSetGraveProtection.class);
        initializeClass(EffAddItemToGrave.class);

        initializeClass(CondIsNearGrave.class);
        initializeClass(CondIsGrave.class);
        initializeClass(CondGraveLocked.class);
        initializeClass(CondGraveHasAvailableSlot.class);

        initializeClass(ExprGraveFromUUID.class);
        initializeClass(ExprGraveUUID.class);
        initializeClass(ExprGraveUUIDs.class);
        initializeClass(ExprGraves.class);
        initializeClass(ExprGraveAmount.class);
        initializeClass(ExprGraveViewerUUID.class);
        initializeClass(ExprNextAvailableGraveSlot.class);
    }

    /**
     * Forces a class to initialize so its static registration block runs.
     *
     * @param clazz The class to initialize.
     * @throws ClassNotFoundException If the class cannot be initialized.
     */
    private void initializeClass(Class<?> clazz) throws ClassNotFoundException {
        Class.forName(clazz.getName(), true, clazz.getClassLoader());
    }

    /**
     * Registers existing GravesX event expressions through SyntaxRegistry.
     */
    private void registerExpressions() {
        registerExpression(ExprEventGrave.class, Grave.class, "[the] event[-]grave");
        registerExpression(ExprEventEntity.class, Entity.class, "[the] event[-]entity");
        registerExpression(ExprEventTargetEntity.class, LivingEntity.class, "[the] event[-]target[-]entity");
        registerExpression(ExprEventEntityType.class, EntityType.class, "[the] event[-]entity[-]type");
        registerExpression(ExprEventLocation.class, Location.class, "[the] event[-]location");
        registerExpression(ExprEventInventoryView.class, InventoryView.class, "[the] event[-]inventory[-]view");
        registerExpression(ExprEventLivingEntity.class, LivingEntity.class, "[the] event[-]living[-]entity");
        registerExpression(ExprEventBlockType.class, BlockData.BlockType.class, "[the] event[-]block[-]type");
        registerExpression(ExprEventBlockExp.class, Integer.class, "[the] event[-]blockexp", "[the] event[-]block[-]exp", "[the] event[-]block[-]experience");
        registerExpression(ExprEventBlock.class, Block.class, "[the] event[-]block");
        registerExpression(ExprEventPistonBlock.class, Block.class, "[the] event[-]piston[-]block");
        registerExpression(ExprEventDirection.class, BlockFace.class, "[the] event[-]direction");
        registerExpression(ExprEventMovedBlocks.class, List.class, "[the] event[-]moved[-]blocks");
        registerExpression(ExprEventPlayer.class, Player.class, "[the] event[-]player");
        registerExpression(ExprEventItemStack.class, ItemStack.class, "[the] event[-]itemstack", "[the] event[-]item");
        registerExpression(ExprEventInventoryAction.class, InventoryAction.class, "[the] event[-]inventoryaction", "[the] event[-]inventory[-]action");
    }

    /**
     * Registers a Skript expression through the Skript 2.15.3 SyntaxRegistry API.
     *
     * @param expressionClass The expression class.
     * @param returnType      The return type.
     * @param patterns        The Skript patterns.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerExpression(Class<? extends Expression<?>> expressionClass, Class<?> returnType, String... patterns) {
        registerExpressionTyped((Class) expressionClass, (Class) returnType, patterns);
    }

    private <R, E extends Expression<R>> void registerExpressionTyped(
            Class<E> expressionClass,
            Class<R> returnType,
            String... patterns
    ) {
        DefaultSyntaxInfos.Expression<E, R> info = DefaultSyntaxInfos.Expression
                .builder(expressionClass, returnType)
                .addPatterns(patterns)
                .build();

        skriptAddon.syntaxRegistry().register(SyntaxRegistry.EXPRESSION, info);
    }

    /**
     * Gets the active SkriptAddon instance for static Skript registrations.
     *
     * @return The active SkriptAddon instance.
     */
    public static SkriptAddon getActiveSkriptAddon() {
        return activeSkriptAddon;
    }

    /**
     * Gets the active GravesX grave creation API.
     *
     * @return The active grave creation API.
     */
    public static GraveCreationAPI getGraveCreationAPI() {
        return activeGraveCreationAPI;
    }

    /**
     * Gets the active GravesX grave management API.
     *
     * @return The active grave management API.
     */
    public static GraveManagementAPI getGraveManagementAPI() {
        return activeGraveManagementAPI;
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
