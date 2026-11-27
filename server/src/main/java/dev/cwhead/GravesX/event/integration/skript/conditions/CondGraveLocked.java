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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("GravesX Grave Locked")
@Description("Checks whether a grave is locked through the GravesX grave management API.")
@Examples({
        "if event-grave is locked:",
        "if event-grave is locked for player:"
})
public class CondGraveLocked extends Condition {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.CONDITION,
                SyntaxInfo.builder(CondGraveLocked.class)
                        .addPatterns(
                                "%grave% is locked",
                                "%grave% is not locked",
                                "%grave% is locked for %player%",
                                "%grave% is not locked for %player%"
                        )
                        .build()
        );
    }

    private Expression<Grave> grave;
    private Expression<Player> player;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.grave = (Expression<Grave>) expressions[0];
        setNegated(matchedPattern == 1 || matchedPattern == 3);

        if (matchedPattern == 2 || matchedPattern == 3) {
            this.player = (Expression<Player>) expressions[1];
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

        boolean result;
        if (player != null) {
            Player playerValue = player.getSingle(event);
            result = playerValue != null && api.isGraveLocked(graveValue, playerValue);
        } else {
            result = api.isGraveLocked(graveValue);
        }

        return isNegated() != result;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return grave + (isNegated() ? " is not locked" : " is locked") +
                (player != null ? " for " + player : "");
    }
}
