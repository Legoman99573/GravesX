package com.ranull.graves.event.integration.skript;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.util.Checker;
import com.ranull.graves.util.UUIDUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ranull.graves.event.GraveProtectionExpiredEvent;
import com.ranull.graves.type.Grave;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;

import java.util.UUID;

@Name("Grave Protection Expired Event")
@Description("Triggered when a grave's protection expires. Provides access to the grave and the UUID of the owner.")
@Examples({
        "on grave protection expired:",
        "\tbroadcast \"Grave %event-grave% protection expired!\"",
        "\tif event-uuid is set:",
        "\t\tsend \"Your grave's protection has expired!\" to uuid of event-uuid"
})
public class EvtGraveProtectionExpired extends SkriptEvent {

    static {
        Skript.registerEvent("Grave Protection Expired", EvtGraveProtectionExpired.class, GraveProtectionExpiredEvent.class, "[grave] protection expir(ing|ed)");

        // Registering event values
        EventValues.registerEventValue(GraveProtectionExpiredEvent.class, Grave.class, new Getter<Grave, GraveProtectionExpiredEvent>() {
            @Override
            public Grave get(GraveProtectionExpiredEvent e) {
                return e.getGrave();
            }
        }, 0);

        EventValues.registerEventValue(GraveProtectionExpiredEvent.class, UUID.class, new Getter<UUID, GraveProtectionExpiredEvent>() {
            @Override
            public UUID get(GraveProtectionExpiredEvent e) {
                return e.getGrave().getOwnerUUID();
            }
        }, 0);
    }

    private Literal<Grave> grave;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        grave = (Literal<Grave>) args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (e instanceof GraveProtectionExpiredEvent) {
            GraveProtectionExpiredEvent event = (GraveProtectionExpiredEvent) e;
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
        return "Grave protection expired event " +
                (grave != null ? " with grave " + grave.toString(e, debug) : "");
    }
}