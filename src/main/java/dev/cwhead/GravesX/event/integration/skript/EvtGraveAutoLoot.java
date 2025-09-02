package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveAutoLootEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

@Name("Grave Auto Loot Event")
@Description("Triggered when an entity auto loots a grave. Provides access to the entity, grave, and location.")
@Examples({
        "on grave auto loot:",
        "\tbroadcast \"Entity %event-entity% auto looted grave %event-grave% at location %event-location%\"",
})
public class EvtGraveAutoLoot extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Auto Loot", EvtGraveAutoLoot.class, GraveAutoLootEvent.class, "[grave] auto loo(t|ting|ted)");

        EventValues.registerEventValue(GraveAutoLootEvent.class, Grave.class, GraveAutoLootEvent::getGrave, 0);

        EventValues.registerEventValue(GraveAutoLootEvent.class, Entity.class, GraveAutoLootEvent::getEntity, 0);

        EventValues.registerEventValue(GraveAutoLootEvent.class, String.class, GraveAutoLootEvent::getEntityName, 0);

        EventValues.registerEventValue(GraveAutoLootEvent.class, UUID.class, GraveAutoLootEvent::getEntityUniqueId, 0);

        EventValues.registerEventValue(GraveAutoLootEvent.class, Location.class, GraveAutoLootEvent::getLocation, 0);

        EventValues.registerEventValue(GraveAutoLootEvent.class, UUID.class, GraveAutoLootEvent::getGraveOwnerUniqueId, 0);

        EventValues.registerEventValue(GraveAutoLootEvent.class, String.class, GraveAutoLootEvent::getGraveOwnerDisplayName, 0);

        EventValues.registerEventValue(GraveAutoLootEvent.class, Number.class, GraveAutoLootEvent::getGraveExperience, 0);
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
        if (e instanceof GraveAutoLootEvent) {
            GraveAutoLootEvent event = (GraveAutoLootEvent) e;
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
        return "Grave auto loot event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}
