package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveWalkOverEvent;
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

import java.util.UUID;
import java.util.function.Predicate;

@Name("Grave Walk Over Event")
@Description("Triggered when an entity walks over a grave. Provides access to the entity, grave, and location.")
@Examples({
        "on grave walk over:",
        "\tbroadcast \"Entity %event-entity% walked over and looted grave %event-grave% at location %event-location%\"",
})
public class EvtGraveWalkOver extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveWalkOver.class, "Grave Walk Over")
                        .addEvent(GraveWalkOverEvent.class)
                        .addPatterns("[grave] wal(k|ked|king) over")
                        .addDescription("Triggered when an entity walks over a grave. Provides access to the entity, grave, and location.")
                        .addExamples(
                                "on grave walk over:",
                                "\tbroadcast \"Entity %event-entity% walked over and looted grave %event-grave% at location %event-location%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveWalkOverEvent.class, Entity.class)
                .getter(GraveWalkOverEvent::getEntity)
                .patterns("entity")
                .build());

        registry.register(EventValue.builder(GraveWalkOverEvent.class, Grave.class)
                .getter(GraveWalkOverEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveWalkOverEvent.class, String.class)
                .getter(GraveWalkOverEvent::getEntityName)
                .patterns("entity[-]name")
                .build());

        registry.register(EventValue.builder(GraveWalkOverEvent.class, UUID.class)
                .getter(GraveWalkOverEvent::getEntityUniqueId)
                .patterns("entity[-]uuid", "entity[-]unique[-]id")
                .build());

        registry.register(EventValue.builder(GraveWalkOverEvent.class, Location.class)
                .getter(GraveWalkOverEvent::getLocation)
                .patterns("location")
                .build());

        registry.register(EventValue.builder(GraveWalkOverEvent.class, UUID.class)
                .getter(GraveWalkOverEvent::getGraveOwnerUniqueId)
                .patterns("grave[-]owner[-]uuid", "grave[-]owner[-]unique[-]id")
                .build());

        registry.register(EventValue.builder(GraveWalkOverEvent.class, String.class)
                .getter(GraveWalkOverEvent::getGraveOwnerDisplayName)
                .patterns("grave[-]owner[-]display[-]name", "grave[-]owner[-]name")
                .build());

        registry.register(EventValue.builder(GraveWalkOverEvent.class, Number.class)
                .getter(event -> event.getGraveExperience())
                .patterns("grave[-]experience", "grave[-]exp")
                .build());
    }

    private Literal<Entity> entity;
    private Literal<Grave> grave;
    private Literal<Location> location;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //entity = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveWalkOverEvent) {
            GraveWalkOverEvent event = (GraveWalkOverEvent) e;
            if (entity != null) {
                entity.check(event, new Predicate<Entity>() {
                    @Override
                    public boolean test(Entity e) {
                        return e.equals(event.getEntity());
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
        return "Grave walk over event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}