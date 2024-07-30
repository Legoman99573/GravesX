package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveZombieSpawnEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Checker;
import ch.njol.skript.util.Getter;

@Name("Grave Zombie Spawn Event")
@Description("Triggered when a zombie spawns targeting an entity. Provides access to the grave, target entity, and location.")
@Examples({
        "on grave zombie spawn:",
        "\tbroadcast \"A zombie targeting %event-target-entity% spawned at location %event-location% from grave %event-grave%\""
})
public class EvtGraveZombieSpawn extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Zombie Spawn", EvtGraveZombieSpawn.class, GraveZombieSpawnEvent.class, "[grave] zombie spawn[ing]");

        // Registering event values
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, LivingEntity.class, new Getter<LivingEntity, GraveZombieSpawnEvent>() {
            @Override
            public LivingEntity get(GraveZombieSpawnEvent e) {
                return e.getTargetEntity();
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, Grave.class, new Getter<Grave, GraveZombieSpawnEvent>() {
            @Override
            public Grave get(GraveZombieSpawnEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GraveZombieSpawnEvent.class, Location.class, new Getter<Location, GraveZombieSpawnEvent>() {
            @Override
            public Location get(GraveZombieSpawnEvent e) {
                return e.getLocation();
            }
        }, 0);
    }

    private Literal<LivingEntity> targetEntity;
    private Literal<Grave> grave;
    private Literal<Location> location;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        //targetEntity = (Literal<LivingEntity>) args[0];
        //grave = (Literal<Grave>) args[0];
        //location = (Literal<Location>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveZombieSpawnEvent) {
            GraveZombieSpawnEvent event = (GraveZombieSpawnEvent) e;
            if (targetEntity != null && !targetEntity.check(event, new Checker<LivingEntity>() {
                @Override
                public boolean check(LivingEntity ent) {
                    return ent.equals(event.getTargetEntity());
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
            if (location != null && !location.check(event, new Checker<Location>() {
                @Override
                public boolean check(Location loc) {
                    return loc.equals(event.getLocation());
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
        return "Grave zombie spawn event " +
                (targetEntity != null ? " targeting entity " + targetEntity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}
