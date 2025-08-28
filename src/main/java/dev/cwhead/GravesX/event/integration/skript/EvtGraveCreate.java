package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import com.ranull.graves.event.GraveCreateEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Create Event")
@Description("Triggered when a grave is created. Provides access to the entity and grave.")
@Examples({
        "on grave create:",
        "\tbroadcast \"Grave %event-grave% created for entity %event-entity%\""
})
public class EvtGraveCreate extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Create", EvtGraveCreate.class, GraveCreateEvent.class, "[grave] creat(e|ing|ed)");

        EventValues.registerEventValue(GraveCreateEvent.class, Entity.class, GraveCreateEvent::getEntity, 0);

        EventValues.registerEventValue(GraveCreateEvent.class, Grave.class, GraveCreateEvent::getGrave, 0);
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
        if (e instanceof GraveCreateEvent) {
            GraveCreateEvent event = (GraveCreateEvent) e;
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
        return "Grave create event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}