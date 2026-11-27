package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveObituaryAddEvent;
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

@Name("Grave Obituary Add Event")
@Description("Triggered when an obituary is to be added to a grave. Provides access to the entity, grave, and location.")
@Examples({
        "on grave obituary add:",
        "\tbroadcast \"Obituary added to %event-player%'s grave %event-grave% at location %event-location%\"",
})
public class EvtGraveObituaryAdd extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveObituaryAdd.class, "Grave Obituary Add")
                        .addEvent(GraveObituaryAddEvent.class)
                        .addPatterns("grav(e|es) obituar(y|ies) ad(d|ded)")
                        .addDescription("Triggered when an obituary is to be added to a grave. Provides access to the entity, grave, and location.")
                        .addExamples(
                                "on grave obituary add:",
                                "\tbroadcast \"Obituary added to %event-player%'s grave %event-grave% at location %event-location%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveObituaryAddEvent.class, Entity.class)
                .getter(GraveObituaryAddEvent::getEntity)
                .patterns("entity")
                .build());

        registry.register(EventValue.builder(GraveObituaryAddEvent.class, Grave.class)
                .getter(GraveObituaryAddEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveObituaryAddEvent.class, Location.class)
                .getter(GraveObituaryAddEvent::getLocation)
                .patterns("location")
                .build());
    }

    private Literal<Entity> entity;
    private Literal<Grave> grave;
    private Literal<Location> location;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull SkriptParser.ParseResult parseResult) {
        //entity = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveObituaryAddEvent) {
            GraveObituaryAddEvent event = (GraveObituaryAddEvent) e;
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
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave obituary add event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}
