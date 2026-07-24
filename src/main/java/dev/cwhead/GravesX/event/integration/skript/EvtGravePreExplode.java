package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GravePreExplodeEvent;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.addon.SkriptAddon;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Pre Explode Event")
@Description("Triggered before an explosion affecting a grave is processed. " +
        "Provides access to the grave, explosion location, source entity (if any), and radius.")
@Examples({
        "on grave pre explode:",
        "\tbroadcast \"Grave %event-grave% is about to be exploded at %event-location% with radius %event-number% by %event-entity%\"",
})
public class EvtGravePreExplode extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGravePreExplode.class, "Grave Pre Explode")
                        .addEvent(GravePreExplodeEvent.class)
                        .addPatterns("grav(e|es) pre[-]explod(e|ing)")
                        .addDescription("")
                        .addExamples(
                                "on grave pre explode:",
                                "\tbroadcast \"Grave %event-grave% is about to be exploded at %event-location% with radius %event-number% by %event-entity%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GravePreExplodeEvent.class, Grave.class)
                .getter(GravePreExplodeEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GravePreExplodeEvent.class, Location.class)
                .getter(GravePreExplodeEvent::getExplosionLocation)
                .patterns("explosion[-]location", "location")
                .build());

        registry.register(EventValue.builder(GravePreExplodeEvent.class, Entity.class)
                .getter(GravePreExplodeEvent::getSource)
                .patterns("source", "explosion[-]source")
                .build());

        registry.register(EventValue.builder(GravePreExplodeEvent.class, Number.class)
                .getter(GravePreExplodeEvent::getRadius)
                .patterns("radius")
                .build());
    }

    private Literal<Grave> grave;
    private Literal<Location> location;
    private Literal<Entity> source;
    private Literal<Number> radius;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        //source = (Literal<Entity>) args[0];
        //radius = (Literal<Number>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GravePreExplodeEvent) {
            GravePreExplodeEvent event = (GravePreExplodeEvent) e;

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
                        return l.equals(event.getExplosionLocation());
                    }
                });
            }

            if (source != null) {
                source.check(event, new Predicate<Entity>() {
                    @Override
                    public boolean test(Entity ent) {
                        return ent != null && ent.equals(event.getSource());
                    }
                });
            }

            if (radius != null) {
                radius.check(event, new Predicate<Number>() {
                    @Override
                    public boolean test(Number r) {
                        return r.floatValue() == event.getRadius();
                    }
                });
            }

            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave pre explode event" +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " with location " + location.toString(e, debug) : "") +
                (source != null ? " with source " + source.toString(e, debug) : "") +
                (radius != null ? " with radius " + radius.toString(e, debug) : "");
    }
}
