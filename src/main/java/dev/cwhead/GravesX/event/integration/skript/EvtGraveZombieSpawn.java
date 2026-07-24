package dev.cwhead.GravesX.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.GraveZombieSpawnEvent;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.addon.SkriptAddon;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Name("Grave Zombie Spawn Event")
@Description("Triggered when a zombie spawns targeting an entity. Provides access to the grave, target entity, and location.")
@Examples({
        "on grave zombie spawn:",
        "\tbroadcast \"A zombie targeting %event-target-entity% spawned at location %event-location% from grave %event-grave%\""
})
public class EvtGraveZombieSpawn extends SkriptEvent {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(EvtGraveZombieSpawn.class, "Grave Zombie Spawn")
                        .addEvent(GraveZombieSpawnEvent.class)
                        .addPatterns("grav(e|es) zombi(e|es) spaw(n|ning|ned)")
                        .addDescription("Triggered when a zombie spawns targeting an entity. Provides access to the grave, target entity, and location.")
                        .addExamples(
                                "on grave zombie spawn:",
                                "\tbroadcast \"A zombie targeting %event-target-entity% spawned at location %event-location% from grave %event-grave%\""
                        )
                        .build()
        );

        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.builder(GraveZombieSpawnEvent.class, LivingEntity.class)
                .getter(GraveZombieSpawnEvent::getTargetEntity)
                .patterns("target[-]entity")
                .build());

        registry.register(EventValue.builder(GraveZombieSpawnEvent.class, Grave.class)
                .getter(GraveZombieSpawnEvent::getGrave)
                .patterns("grave")
                .build());

        registry.register(EventValue.builder(GraveZombieSpawnEvent.class, Location.class)
                .getter(GraveZombieSpawnEvent::getLocation)
                .patterns("location")
                .build());
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
            if (targetEntity != null) {
                targetEntity.check(event, new Predicate<LivingEntity>() {
                    @Override
                    public boolean test(LivingEntity le) {
                        return le.equals(event.getTargetEntity());
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
        return "Grave zombie spawn event " +
                (targetEntity != null ? " targeting entity " + targetEntity.toString(e, debug) : "") +
                (grave != null ? " with grave " + grave.toString(e, debug) : "") +
                (location != null ? " at location " + location.toString(e, debug) : "");
    }
}
