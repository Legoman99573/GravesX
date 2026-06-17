package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveProjectileHitEvent;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.addon.SkriptAddon;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Projectile Hit Event")
@Description("Triggered when a grave block is broken. Provides access to the grave, player, block, and block type.")
@Examples({
        "on grave projectile hit:",
        "\tbroadcast \"%event-grave% \"",
})
public class EvtGraveProjectileHit extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveProjectileHit.class, "Grave Projectile Hit")
                        .addEvent(GraveProjectileHitEvent.class)
                        .addPatterns("[grave] projectil(e|es) hi(t|tting)")
                        .addDescription("Triggered when a grave block is broken. Provides access to the grave, player, block, and block type.")
                        .addExamples(
                                "on grave projectile hit:",
                                "\tbroadcast \"%event-grave% \""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveProjectileHitEvent.class, Player.class)
                .getter(GraveProjectileHitEvent::getPlayer)
                .patterns("player")
                .build());

        registry.register(EventValue.builder(GraveProjectileHitEvent.class, Grave.class)
                .getter(GraveProjectileHitEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveProjectileHitEvent.class, Block.class)
                .getter(GraveProjectileHitEvent::getBlock)
                .patterns("block")
                .build());

        registry.register(EventValue.builder(GraveProjectileHitEvent.class, Entity.class)
                .getter(GraveProjectileHitEvent::getEntity)
                .patterns("entity")
                .build());

        registry.register(EventValue.builder(GraveProjectileHitEvent.class, LivingEntity.class)
                .getter(GraveProjectileHitEvent::getLivingEntity)
                .patterns("living[-]entity")
                .build());
    }

    private Literal<Player> player;
    private Literal<Grave> grave;
    private Literal<Block> block;
    private Literal<Entity> entity;
    private Literal<LivingEntity> livingEntity;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //player = (Literal<Player>) args[0];
        //grave = (Literal<Grave>) args[0];
        //block = (Literal<Block>) args[0];
        //entity = (Literal<Entity>) args[0];
        //livingEntity = (Literal<LivingEntity>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveProjectileHitEvent) {
            GraveProjectileHitEvent event = (GraveProjectileHitEvent) e;
            if (player != null) {
                player.check(event, new Predicate<Player>() {
                    @Override
                    public boolean test(Player p) {
                        return p.equals(event.getPlayer());
                    }
                });
            }
            if (grave != null) {
                grave.check(event, new Predicate<Grave>() {
                    @Override
                    public boolean test(Grave g) {
                        return g.equals(event.getGrave());
                    }
                });
            }
            if (block != null) {
                block.check(event, new Predicate<Block>() {
                    @Override
                    public boolean test(Block b) {
                        return b.equals(event.getBlock());
                    }
                });
            }
            if (entity != null) {
                entity.check(event, new Predicate<Entity>() {
                    @Override
                    public boolean test(Entity e) {
                        return e.equals(event.getEntity());
                    }
                });
            }
            if (livingEntity != null) {
                livingEntity.check(event, new Predicate<LivingEntity>() {
                    @Override
                    public boolean test(LivingEntity le) {
                        return le.equals(event.getLivingEntity());
                    }
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave break event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (block != null ? " with block " + block.toString(e, debug) : "") +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (livingEntity != null ? " with living entity " + livingEntity.toString(e, debug) : "");
    }
}