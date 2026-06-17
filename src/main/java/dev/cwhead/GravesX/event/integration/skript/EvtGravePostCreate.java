package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GravePostCreateEvent;
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

@Name("Grave Post Create Event")
@Description("Triggered after a grave create attempt completes (after placement). Provides access to the entity, grave, and placed location (nullable).")
@Examples({
        "on grave post create:",
        "\tbroadcast \"Grave %event-grave% post-created for entity %event-entity%\"",
        "\tif event-placed location is set:",
        "\t\tbroadcast \"Placed at %event-placed location%\"",
        "\telse:",
        "\t\tbroadcast \"Grave was not placed\""
})
public class EvtGravePostCreate extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGravePostCreate.class, "Grave Post Create")
                        .addEvent(GravePostCreateEvent.class)
                        .addPatterns("[grave] post creat(e|ing|ed)")
                        .addDescription("Triggered after a grave create attempt completes (after placement). Provides access to the entity, grave, and placed location (nullable).")
                        .addExamples(
                                "on grave post create:",
                                "\tbroadcast \"Grave %event-grave% post-created for entity %event-entity%\"",
                                "\tif event-placed location is set:",
                                "\t\tbroadcast \"Placed at %event-placed location%\"",
                                "\telse:",
                                "\t\tbroadcast \"Grave was not placed\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GravePostCreateEvent.class, Entity.class)
                .getter(GravePostCreateEvent::getEntity)
                .patterns("entity")
                .build());

        registry.register(EventValue.builder(GravePostCreateEvent.class, Grave.class)
                .getter(GravePostCreateEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GravePostCreateEvent.class, Location.class)
                .getter(GravePostCreateEvent::getPlacedLocation)
                .patterns("placed[-]location")
                .build());
    }

    private Literal<Entity> entity;
    private Literal<Grave> grave;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //entity = (Literal<Entity>) args[0];
        //grave = (Literal<Grave>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GravePostCreateEvent) {
            GravePostCreateEvent event = (GravePostCreateEvent) e;

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

            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave post create event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}
