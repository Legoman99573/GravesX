package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveZombieSpawnEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Zombie Spawn Event")
@Description("Triggered when a zombie spawns from a grave. Provides access to the target entity, grave, and location.")
@Examples({
        "on grave zombie spawn:",
        "\tbroadcast \"A zombie targeting %entity% spawned at location %event-location% from grave %event-grave%\"",
        "\tbroadcast \"Grave owner: %event-grave's owner displayname% (UUID: %event-grave's owner uuid%)\""
})
public class EvtGraveZombieSpawn extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Zombie Spawn", EvtGraveZombieSpawn.class, GraveZombieSpawnEvent.class, "[grave] zombie spawn[ing]");

        // Registering entity values
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, Entity.class, new Getter<Entity, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public Entity get(GraveZombieSpawnEvent e) {
                return e.getTargetEntity();
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, EntityType.class, new Getter<EntityType, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public EntityType get(GraveZombieSpawnEvent e) {
                return e.getTargetEntity() != null ? e.getTargetEntity().getType() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, String.class, new Getter<String, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public String get(GraveZombieSpawnEvent e) {
                return e.getTargetEntity() != null ? e.getTargetEntity().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, String.class, new Getter<String, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public String get(GraveZombieSpawnEvent e) {
                return e.getTargetEntity() != null ? e.getTargetEntity().getUniqueId().toString() : null;
            }
        }, 0);

        // Registering grave values
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, Grave.class, new Getter<Grave, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public Grave get(GraveZombieSpawnEvent e) {
                return e.getGrave();
            }
        }, 0);

        // Registering location values
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, Location.class, new Getter<Location, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public Location get(GraveZombieSpawnEvent e) {
                return e.getLocation();
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, String.class, new Getter<String, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public String get(GraveZombieSpawnEvent e) {
                return e.getLocation() != null ? e.getLocation().getWorld().getName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, Number.class, new Getter<Number, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public Number get(GraveZombieSpawnEvent e) {
                return e.getLocation() != null ? e.getLocation().getX() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, Number.class, new Getter<Number, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public Number get(GraveZombieSpawnEvent e) {
                return e.getLocation() != null ? e.getLocation().getY() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, Number.class, new Getter<Number, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public Number get(GraveZombieSpawnEvent e) {
                return e.getLocation() != null ? e.getLocation().getZ() : null;
            }
        }, 0);

        // Registering additional grave values
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, String.class, new Getter<String, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public String get(GraveZombieSpawnEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerUUID().toString() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, String.class, new Getter<String, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public String get(GraveZombieSpawnEvent e) {
                return e.getGrave() != null ? e.getGrave().getOwnerName() : null;
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, Number.class, new Getter<Number, GraveZombieSpawnEvent>() {
            @Override
            @Nullable
            public Number get(GraveZombieSpawnEvent e) {
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
        if (e instanceof GraveZombieSpawnEvent) {
            GraveZombieSpawnEvent event = (GraveZombieSpawnEvent) e;
            if (entities != null && !entities.check(event, new Checker<EntityData<?>>() {
                @Override
                public boolean check(EntityData<?> data) {
                    return data.isInstance(event.getTargetEntity());
                }
            })) {
                return false;
            }
            if (location != null && !location.check(event, new Checker<Location>() {
                @Override
                public boolean check(Location loc) {
                    return loc.equals(event.getLocation());
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
        return "Grave zombie spawn event " + (entities != null ? entities.toString(e, debug) : "") +
                (location != null ? " at " + location.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}