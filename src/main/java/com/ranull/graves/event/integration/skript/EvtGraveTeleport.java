package com.ranull.graves.event.integration.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import ch.njol.util.Checker;
import com.ranull.graves.event.GraveTeleportEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name("Grave Teleport Event")
@Description("Triggered when an entity teleports to a grave. Provides access to the grave, entity and location.")
@Examples({
        "on grave teleport:",
        "\tbroadcast \"%event-entity% teleported to grave %event-grave% at location %event-location%\""
})
public class EvtGraveTeleport extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Teleport", EvtGraveTeleport.class, GraveTeleportEvent.class, "[grave] teleport(ing|ed)");

        // Registering event values
        EventValues.registerEventValue(GraveTeleportEvent.class, Entity.class, new Getter<Entity, GraveTeleportEvent>() {
            @Override
            public Entity get(GraveTeleportEvent e) {
                return e.getEntity();
            }
        }, 0);
        EventValues.registerEventValue(GraveTeleportEvent.class, Grave.class, new Getter<Grave, GraveTeleportEvent>() {
            @Override
            public Grave get(GraveTeleportEvent e) {
                return e.getGrave();
            }
        }, 0);
        EventValues.registerEventValue(GraveTeleportEvent.class, Location.class, new Getter<Location, GraveTeleportEvent>() {
            @Override
            public Location get(GraveTeleportEvent e) {
                return e.getGrave().getLocationDeath();
            }
        }, 0);
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
        if (e instanceof GraveTeleportEvent) {
            GraveTeleportEvent event = (GraveTeleportEvent) e;
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
            if (location != null && !location.check(event, new Checker<Location>() {
                @Override
                public boolean check(Location loc) {
                    return loc.equals(event.getGrave().getLocation());
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
        return "Grave teleport event " +
                (entity != null ? " with entity " + entity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}
