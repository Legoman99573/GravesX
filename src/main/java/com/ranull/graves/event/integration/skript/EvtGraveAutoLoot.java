package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveAutoLootEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Auto Loot Event")
@Description("Triggered when a grave is auto looted. Provides access to the entity, location, and grave involved in the event.")
@Examples({
        "on grave auto loot:",
        "\tbroadcast \"%event-entity% auto-looted a grave at location %event-location%\"",
        "\tbroadcast \"Grave owner: %event-grave's owner displayname% (UUID: %event-grave's owner uuid%)\"",
        "\tbroadcast \"Experience in grave: %event-grave's experience%\""
})
public class EvtGraveAutoLoot extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Auto Loot", EvtGraveAutoLoot.class, GraveAutoLootEvent.class, "[grave] auto loot[ing] [(of|for) %-entitydatas%]");

        // Registering entity values
        EventValues.registerEventValue(GraveAutoLootEvent.class, Entity.class, new Getter<Entity, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public Entity get(GraveAutoLootEvent e) {
                return e.getEntity();
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, String.class, new Getter<String, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public String get(GraveAutoLootEvent e) {
                return e.getEntity() != null ? e.getEntity().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, String.class, new Getter<String, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public String get(GraveAutoLootEvent e) {
                return e.getEntity() != null ? e.getEntity().getUniqueId().toString() : null;
            }
        }, 0);

        // Registering location values
        EventValues.registerEventValue(GraveAutoLootEvent.class, Location.class, new Getter<Location, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public Location get(GraveAutoLootEvent e) {
                return e.getLocation();
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, Number.class, new Getter<Number, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public Number get(GraveAutoLootEvent e) {
                return e.getLocation() != null ? e.getLocation().getX() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, Number.class, new Getter<Number, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public Number get(GraveAutoLootEvent e) {
                return e.getLocation() != null ? e.getLocation().getY() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, Number.class, new Getter<Number, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public Number get(GraveAutoLootEvent e) {
                return e.getLocation() != null ? e.getLocation().getZ() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, Number.class, new Getter<Number, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public Number get(GraveAutoLootEvent e) {
                return e.getLocation() != null ? e.getLocation().getYaw() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, Number.class, new Getter<Number, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public Number get(GraveAutoLootEvent e) {
                return e.getLocation() != null ? e.getLocation().getPitch() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, String.class, new Getter<String, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public String get(GraveAutoLootEvent e) {
                return e.getLocation() != null ? e.getLocation().getWorld().getName() : null;
            }
        }, 0);

        // Registering grave values
        EventValues.registerEventValue(GraveAutoLootEvent.class, Grave.class, new Getter<Grave, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public Grave get(GraveAutoLootEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, String.class, new Getter<String, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public String get(GraveAutoLootEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerUUID().toString() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, String.class, new Getter<String, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public String get(GraveAutoLootEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveAutoLootEvent.class, Number.class, new Getter<Number, GraveAutoLootEvent>() {
            @Override
            @Nullable
            public Number get(GraveAutoLootEvent e) {
                return e.getGrave() != null ? e.getGrave().getExperience() : null;
            }
        }, 0);
    }

    private Literal<EntityData<?>> entities;
    private Literal<Location> location;
    private Literal<Grave> grave;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
        entities = (Literal<EntityData<?>>) args[0];
        location = (Literal<Location>) args[1];
        grave = (Literal<Grave>) args[2];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (entities != null) {
            Entity entity = ((GraveAutoLootEvent) e).getEntity();
            if (!entities.check(e, new Checker<EntityData<?>>() {
                @Override
                public boolean check(EntityData<?> data) {
                    return data.isInstance(entity);
                }
            })) {
                return false;
            }
        }
        if (location != null) {
            Location eventLocation = ((GraveAutoLootEvent) e).getLocation();
            if (!location.check(e, new Checker<Location>() {
                @Override
                public boolean check(Location loc) {
                    return loc.equals(eventLocation);
                }
            })) {
                return false;
            }
        }
        if (grave != null) {
            Grave eventGrave = ((GraveAutoLootEvent) e).getGrave();
            if (!grave.check(e, new Checker<Grave>() {
                @Override
                public boolean check(Grave g) {
                    return g.equals(eventGrave);
                }
            })) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Grave auto loot event " + (entities != null ? entities.toString(e, debug) : "") +
                (location != null ? " at " + location.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}