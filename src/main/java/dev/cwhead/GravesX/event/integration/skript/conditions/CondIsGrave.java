package dev.cwhead.GravesX.event.integration.skript.conditions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Condition;
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

@Name("GravesX Grave Location")
@Description("Checks grave location state through the GravesX grave management API.")
@Examples({
        "if event-location is a grave location of event-grave:",
        "if event-grave is a grave:"
})
public class CondIsGrave extends Condition {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.CONDITION,
                SyntaxInfo.builder(CondIsGrave.class)
                        .addPatterns(
                                "%grave% is [a] grave",
                                "%grave% is not [a] grave",
                                "%location% is [a] grave location of %grave%",
                                "%location% is not [a] grave location of %grave%"
                        )
                        .build()
        );
    }

    private Expression<Grave> grave;
    private Expression<Location> location;
    private boolean locationPattern;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        setNegated(matchedPattern == 1 || matchedPattern == 3);
        locationPattern = matchedPattern == 2 || matchedPattern == 3;

        if (locationPattern) {
            this.location = (Expression<Location>) expressions[0];
            this.grave = (Expression<Grave>) expressions[1];
        } else {
            this.grave = (Expression<Grave>) expressions[0];
        }

        return true;
    }

    @Override
    public boolean check(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Grave graveValue = grave.getSingle(event);
        if (api == null || graveValue == null) {
            return isNegated();
        }

        Location locationValue = locationPattern ? location.getSingle(event) : graveValue.getLocationDeath();
        boolean result = locationValue != null && api.isGrave(graveValue, locationValue);
        return isNegated() != result;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (locationPattern) {
            return location + (isNegated() ? " is not a grave location of " : " is a grave location of ") + grave;
        }
        return grave + (isNegated() ? " is not a grave" : " is a grave");
    }
}
