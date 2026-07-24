package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveAutoLootEvent;
import dev.cwhead.GravesX.event.graveevent.GraveEvent;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

import java.util.UUID;

@Name("Grave Auto Loot Event")
@Description("Triggered when an entity auto loots a grave. Provides access to the entity, grave, and location.")
@Examples({
        "on grave auto loot:",
        "\tbroadcast \"Entity %event-entity% auto looted grave %event-grave% at location %event-location%\"",
})
public class EvtGraveAutoLoot extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveAutoLoot.class, "Grave Auto Loot")
                        .addEvent(GraveAutoLootEvent.class)
                        .addPatterns("grav(e|es) auto loo(t|ting|ted)")
                        .addDescription("Triggered when an entity auto loots a grave. Provides access to the entity, grave, and location.")
                        .addExamples(
                                "on grave auto loot:",
                                "\tbroadcast \"Entity %event-entity% auto looted grave %event-grave% at location %event-location%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveAutoLootEvent.class, Grave.class)
                .getter(GraveAutoLootEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveAutoLootEvent.class, Entity.class)
                .getter(GraveAutoLootEvent::getEntity)
                .patterns("entity")
                .build());

        registry.register(EventValue.builder(GraveAutoLootEvent.class, Location.class)
                .getter(GraveAutoLootEvent::getLocation)
                .patterns("location")
                .build());

        registry.register(EventValue.builder(GraveAutoLootEvent.class, String.class)
                .getter(GraveAutoLootEvent::getEntityName)
                .patterns("entity[-]name")
                .build());

        registry.register(EventValue.builder(GraveAutoLootEvent.class, UUID.class)
                .getter(GraveAutoLootEvent::getEntityUniqueId)
                .patterns("entity[-]uuid", "entity[-]unique[-]id")
                .build());

        registry.register(EventValue.builder(GraveAutoLootEvent.class, UUID.class)
                .getter(GraveAutoLootEvent::getGraveOwnerUniqueId)
                .patterns("grave[-]owner[-]uuid", "grave[-]owner[-]unique[-]id")
                .build());

        registry.register(EventValue.builder(GraveAutoLootEvent.class, String.class)
                .getter(GraveAutoLootEvent::getGraveOwnerDisplayName)
                .patterns("grave[-]owner[-]display[-]name", "grave[-]owner[-]name")
                .build());

        registry.register(EventValue.builder(GraveAutoLootEvent.class, Number.class)
                .getter(event -> event.getGraveExperience())
                .patterns("grave[-]experience", "grave[-]exp")
                .build());
    }

    private Literal<Entity> entity;
    private Literal<Grave> grave;
    private Literal<Location> location;

    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (!(e instanceof GraveAutoLootEvent event)) {
            return false;
        }

        if (entity != null && !entity.check(event, entityValue -> entityValue.equals(event.getEntity()))) {
            return false;
        }

        if (grave != null && !grave.check(event, graveValue -> graveValue.equals(event.getGrave()))) {
            return false;
        }

        if (location != null && !location.check(event, locationValue -> locationValue.equals(event.getLocation()))) {
            return false;
        }

        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave auto loot event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}