package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveProtectionCreateEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Protection Create Event")
@Description("Triggered when a grave is protected. Provides access to the entity and grave.")
@Examples({
        "on grave protection create:",
        "\tbroadcast \"Grave %event-grave% protection created for entity %event-entity%\""
})
public class EvtGraveProtectionCreate extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Protection Create", EvtGraveProtectionCreate.class, GraveProtectionCreateEvent.class, "[grave] protec(t|tion|ted|ting) creat(e|ing|ed)");

        // Registering event values
        EventValues.registerEventValue(GraveProtectionCreateEvent.class, Entity.class, new Getter<Entity, GraveProtectionCreateEvent>() {
            @Override
            public Entity get(GraveProtectionCreateEvent e) {
                return e.getEntity();
            }
        }, 0);
        EventValues.registerEventValue(GraveProtectionCreateEvent.class, Grave.class, new Getter<Grave, GraveProtectionCreateEvent>() {
            @Override
            public Grave get(GraveProtectionCreateEvent e) {
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
        if (e instanceof GraveProtectionCreateEvent) {
            GraveProtectionCreateEvent event = (GraveProtectionCreateEvent) e;
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
        return "Grave protection create event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}