package dev.cwhead.GravesX.event.integration.skript.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.api.grave.GraveManagementAPI;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("GravesX Break Grave")
@Description("Breaks a grave through the GravesX grave management API.")
@Examples({
        "break grave event-grave",
        "break grave event-grave at event-location"
})
public class EffBreakGrave extends Effect {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffBreakGrave.class)
                        .addPatterns("break [the] grave %grave%", "break [the] grave %grave% at %location%")
                        .build()
        );
    }

    private Expression<Grave> grave;
    private Expression<Location> location;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.grave = (Expression<Grave>) expressions[0];
        if (matchedPattern == 1) {
            this.location = (Expression<Location>) expressions[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Grave graveValue = grave.getSingle(event);
        if (api == null || graveValue == null) {
            return;
        }

        Location locationValue = location != null ? location.getSingle(event) : null;
        if (locationValue != null) {
            api.breakGrave(locationValue, graveValue);
        } else {
            api.breakGrave(graveValue);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "break grave " + grave + (location != null ? " at " + location : "");
    }
}
