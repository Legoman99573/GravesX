package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveVirtualOpenEvent;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.addon.SkriptAddon;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Virtual Open Event")
@Description("Triggered when a virtual grave open is about to be processed. Provides access to the entity, player (if applicable), grave, location, and distance.")
@Examples({
        "on grave virtual open:",
        "\tbroadcast \"%event-player% virtually opened grave %event-grave% from %event-number% blocks away at %event-location%\"",
})
public class EvtGraveVirtualOpen extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveVirtualOpen.class, "Grave Virtual Open")
                        .addEvent(GraveVirtualOpenEvent.class)
                        .addPatterns("[grave] virtua(l|ly) ope(n|ning|ned)")
                        .addDescription("Triggered when a virtual grave open is about to be processed. Provides access to the entity, player (if applicable), grave, location, and distance.")
                        .addExamples(
                                "on grave virtual open:",
                                "\tbroadcast \"%event-player% virtually opened grave %event-grave% from %event-number% blocks away at %event-location%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveVirtualOpenEvent.class, Player.class)
                .getter(event -> event.hasPlayer() ? event.getPlayer() : null)
                .patterns("player")
                .build());

        registry.register(EventValue.builder(GraveVirtualOpenEvent.class, Entity.class)
                .getter(GraveVirtualOpenEvent::getEntity)
                .patterns("entity")
                .build());

        registry.register(EventValue.builder(GraveVirtualOpenEvent.class, Grave.class)
                .getter(GraveVirtualOpenEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveVirtualOpenEvent.class, Location.class)
                .getter(GraveVirtualOpenEvent::getLocation)
                .patterns("location")
                .build());

        registry.register(EventValue.builder(GraveVirtualOpenEvent.class, Number.class)
                .getter(GraveVirtualOpenEvent::getDistance)
                .patterns("distance")
                .build());
    }

    private Literal<Player> player;
    private Literal<Grave> grave;
    private Literal<Entity> entity;
    private Literal<Location> location;
    private Literal<Number> distance;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //player = (Literal<Player>) args[0];
        //entity = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        //distance = (Literal<Number>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveVirtualOpenEvent) {
            GraveVirtualOpenEvent event = (GraveVirtualOpenEvent) e;

            if (player != null) {
                player.check(event, new Predicate<Player>() {
                    @Override
                    public boolean test(Player p) {
                        Entity ent = event.getEntity();
                        return ent instanceof Player && ent.equals(p);
                    }
                });
            }

            if (entity != null) {
                entity.check(event, new Predicate<Entity>() {
                    @Override
                    public boolean test(Entity ent) {
                        return ent.equals(event.getEntity());
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

            if (location != null) {
                location.check(event, new Predicate<Location>() {
                    @Override
                    public boolean test(Location l) {
                        return l.equals(event.getLocation());
                    }
                });
            }

            if (distance != null) {
                distance.check(event, new Predicate<Number>() {
                    @Override
                    public boolean test(Number n) {
                        return n.doubleValue() == event.getDistance();
                    }
                });
            }

            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave virtual open event " +
                (player != null ? " with player " + player.toString(e, debug) : "") +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " with location " + location.toString(e, debug) : "") +
                (distance != null ? " with distance " + distance.toString(e, debug) : "");
    }
}
