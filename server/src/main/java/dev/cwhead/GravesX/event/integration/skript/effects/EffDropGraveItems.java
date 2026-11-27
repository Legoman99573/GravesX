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

@Name("GravesX Drop Grave Items")
@Description("Drops the items stored in a grave through the GravesX grave management API.")
@Examples("drop grave items of event-grave at event-location")
public class EffDropGraveItems extends Effect {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffDropGraveItems.class)
                        .addPatterns("drop [the] grave items of %grave% at %location%", "drop items of [the] grave %grave% at %location%")
                        .build()
        );
    }

    private Expression<Grave> grave;
    private Expression<Location> location;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.grave = (Expression<Grave>) expressions[0];
        this.location = (Expression<Location>) expressions[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Grave graveValue = grave.getSingle(event);
        Location locationValue = location.getSingle(event);
        if (api != null && graveValue != null && locationValue != null) {
            api.dropGraveItems(locationValue, graveValue);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "drop grave items of " + grave + " at " + location;
    }
}
