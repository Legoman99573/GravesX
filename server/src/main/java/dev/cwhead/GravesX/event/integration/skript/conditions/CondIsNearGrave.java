package dev.cwhead.GravesX.event.integration.skript.conditions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import dev.cwhead.GravesX.api.grave.GraveManagementAPI;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("GravesX Near Grave")
@Description("Checks whether a location is near a grave through the GravesX grave management API.")
@Examples({
        "if player's location is near a grave:",
        "if player's location is near a grave for player:",
        "if player's location is not near a grave:"
})
public class CondIsNearGrave extends Condition {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.CONDITION,
                SyntaxInfo.builder(CondIsNearGrave.class)
                        .addPatterns(
                                "%location% is near [a] grave",
                                "%location% is not near [a] grave",
                                "%location% is near [a] grave for %player%",
                                "%location% is not near [a] grave for %player%",
                                "%location% is near [a] grave at %block%",
                                "%location% is not near [a] grave at %block%"
                        )
                        .build()
        );
    }

    private Expression<Location> location;
    private Expression<Player> player;
    private Expression<Block> block;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.location = (Expression<Location>) expressions[0];
        setNegated(matchedPattern == 1 || matchedPattern == 3 || matchedPattern == 5);

        if (matchedPattern == 2 || matchedPattern == 3) {
            this.player = (Expression<Player>) expressions[1];
        } else if (matchedPattern == 4 || matchedPattern == 5) {
            this.block = (Expression<Block>) expressions[1];
        }

        return true;
    }

    @Override
    public boolean check(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Location locationValue = location.getSingle(event);
        if (api == null || locationValue == null) {
            return isNegated();
        }

        boolean result;
        if (player != null) {
            Player playerValue = player.getSingle(event);
            result = playerValue != null && api.isNearGrave(locationValue, playerValue);
        } else if (block != null) {
            Block blockValue = block.getSingle(event);
            result = blockValue != null && api.isNearGrave(locationValue, blockValue);
        } else {
            result = api.isNearGrave(locationValue);
        }

        return isNegated() != result;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return location + (isNegated() ? " is not near a grave" : " is near a grave") +
                (player != null ? " for " + player : "") +
                (block != null ? " at " + block : "");
    }
}
