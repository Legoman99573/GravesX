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
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("GravesX Set Grave Protection")
@Description("Sets or clears protection on an existing grave through the GravesX grave management API.")
@Examples({
        "set grave protection of {_grave} for 300000 milliseconds",
        "protect grave {_grave} for 300000 milliseconds",
        "clear grave protection of {_grave}",
        "unprotect grave {_grave}"
})
public class EffSetGraveProtection extends Effect {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffSetGraveProtection.class)
                        .addPatterns(
                                "set [the] grave protection of %grave% for %number% milliseconds",
                                "protect [the] grave %grave% for %number% milliseconds",
                                "clear [the] grave protection of %grave%",
                                "remove [the] grave protection of %grave%",
                                "unprotect [the] grave %grave%"
                        )
                        .build()
        );
    }

    private Expression<Grave> grave;
    private Expression<Number> timeProtection;
    private boolean protection;
    private int matchedPattern;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.matchedPattern = matchedPattern;
        this.grave = (Expression<Grave>) expressions[0];
        this.protection = matchedPattern == 0 || matchedPattern == 1;

        if (protection) {
            this.timeProtection = (Expression<Number>) expressions[1];
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

        if (!protection) {
            api.clearGraveProtection(graveValue);
            return;
        }

        Number timeValue = timeProtection != null ? timeProtection.getSingle(event) : null;
        long protectionMillis = timeValue != null ? timeValue.longValue() : 0L;
        api.setGraveProtection(graveValue, true, protectionMillis);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return switch (matchedPattern) {
            case 0 -> "set grave protection of " + grave + " for " + timeProtection + " milliseconds";
            case 1 -> "protect grave " + grave + " for " + timeProtection + " milliseconds";
            case 2, 3 -> "clear grave protection of " + grave;
            case 4 -> "unprotect grave " + grave;
            default -> "set grave protection";
        };
    }
}
