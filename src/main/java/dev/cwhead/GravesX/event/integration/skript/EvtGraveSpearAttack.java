package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveSpearAttackEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Spear Attack Event")
@Description("Triggered when an entity spear-attacks a grave hologram. Provides access to the grave, entity and hit location.")
@Examples({
        "on grave spear attack:",
        "\tbroadcast \"%event-entity% spear-attacked grave %event-grave% at location %event-location%\""
})
public class EvtGraveSpearAttack extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Spear Attack", EvtGraveSpearAttack.class, GraveSpearAttackEvent.class,
                "[grave] spear attack[ing|ed]");

        EventValues.registerEventValue(GraveSpearAttackEvent.class, Entity.class, GraveSpearAttackEvent::getEntity, 0);

        EventValues.registerEventValue(GraveSpearAttackEvent.class, Grave.class, GraveSpearAttackEvent::getGrave, 0);

        // We use hit location (armor stand location) as the location value for Skript.
        EventValues.registerEventValue(GraveSpearAttackEvent.class, Location.class, GraveSpearAttackEvent::getHitLocation, 0);
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
        if (e instanceof GraveSpearAttackEvent) {
            GraveSpearAttackEvent event = (GraveSpearAttackEvent) e;

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
                        return l.equals(event.getHitLocation());
                    }
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave spear attack event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}
