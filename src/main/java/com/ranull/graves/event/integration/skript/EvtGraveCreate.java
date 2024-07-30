package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveCreateEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Create Event")
@Description("Triggered when a grave is created. Provides access to the entity and grave.")
@Examples({
        "on grave create:",
        "\tbroadcast \"Grave %event-grave% created for entity %event-entity%\""
})
public class EvtGraveCreate extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Create", EvtGraveCreate.class, GraveCreateEvent.class, "[grave] creat(e|ing)");

        // Registering event values
        EventValues.registerEventValue(GraveCreateEvent.class, Entity.class, new Getter<Entity, GraveCreateEvent>() {
            @Override
            public Entity get(GraveCreateEvent e) {
                return e.getEntity();
            }
        }, 0);
        EventValues.registerEventValue(GraveCreateEvent.class, Grave.class, new Getter<Grave, GraveCreateEvent>() {
            @Override
            public Grave get(GraveCreateEvent e) {
                return e.getGrave();
            }
        }, 0);
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
            if (entity != null && !entity.check(event, new Checker<Entity>() {
                @Override
                public boolean check(Entity ent) {
                    return ent.equals(event.getEntity());
                }
            })) {
                return false;
            }
            if (grave != null && !grave.check(event, new Checker<Grave>() {
                @Override
                public boolean check(Grave g) {
                    return g.equals(event.getGrave());
                }
            })) {
                return false;
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