package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveCreateEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Create Event")
@Description("Triggered when a grave is created for an entity. Provides access to the entity, grave, and related details.")
@Examples({
        "on grave create:",
        "\tbroadcast \"%entity% created a grave at location %event-location%\"",
        "\tbroadcast \"Grave owner: %event-grave's owner displayname% (UUID: %event-grave's owner uuid%)\"",
        "\tbroadcast \"Experience in grave: %event-grave's experience%\""
})
public class EvtGraveCreate extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Create", EvtGraveCreate.class, GraveCreateEvent.class, "[grave] create[ing]");

        // Registering entity values
        EventValues.registerEventValue(GraveCreateEvent.class, Entity.class, new Getter<Entity, GraveCreateEvent>() {
            @Override
            @Nullable
            public Entity get(GraveCreateEvent e) {
                return e.getEntity();
            }
        }, 0);
        EventValues.registerEventValue(GraveCreateEvent.class, EntityType.class, new Getter<EntityType, GraveCreateEvent>() {
            @Override
            @Nullable
            public EntityType get(GraveCreateEvent e) {
                return e.getEntity() != null ? e.getEntity().getType() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveCreateEvent.class, String.class, new Getter<String, GraveCreateEvent>() {
            @Override
            @Nullable
            public String get(GraveCreateEvent e) {
                return e.getEntity() != null ? e.getEntity().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveCreateEvent.class, String.class, new Getter<String, GraveCreateEvent>() {
            @Override
            @Nullable
            public String get(GraveCreateEvent e) {
                return e.getEntity() != null ? e.getEntity().getUniqueId().toString() : null;
            }
        }, 0);

        // Registering grave values
        EventValues.registerEventValue(GraveCreateEvent.class, Grave.class, new Getter<Grave, GraveCreateEvent>() {
            @Override
            @Nullable
            public Grave get(GraveCreateEvent e) {
                return e.getGrave();
            }
        }, 0);

        // Registering additional grave values
        EventValues.registerEventValue(GraveCreateEvent.class, String.class, new Getter<String, GraveCreateEvent>() {
            @Override
            @Nullable
            public String get(GraveCreateEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerUUID().toString() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveCreateEvent.class, String.class, new Getter<String, GraveCreateEvent>() {
            @Override
            @Nullable
            public String get(GraveCreateEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveCreateEvent.class, Number.class, new Getter<Number, GraveCreateEvent>() {
            @Override
            @Nullable
            public Number get(GraveCreateEvent e) {
                return e.getGrave() != null ? e.getGrave().getExperience() : null;
            }
        }, 0);
    }

    private Literal<EntityData<?>> entities;
    private Literal<Grave> grave;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
        entities = (Literal<EntityData<?>>) args[0];
        grave = (Literal<Grave>) args[1];
        return true;
    }

    @Override
    public boolean check(@NotNull Event e) {
        if (e instanceof GraveCreateEvent) {
            GraveCreateEvent event = (GraveCreateEvent) e;
            if (entities != null && !entities.check(event, new Checker<EntityData<?>>() {
                @Override
                public boolean check(EntityData<?> data) {
                    return data.isInstance(event.getEntity());
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
    public @NotNull String toString(@Nullable Event e, boolean debug) {
        return "Grave create event " + (entities != null ? entities.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}