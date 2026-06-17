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
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("GravesX Grave Has Available Slot")
@Description("Checks whether a grave has an empty inventory slot through the GravesX grave management API.")
@Examples({
        "if grave {_grave} has an available slot:",
        "\tadd player's tool to next available slot of grave {_grave}"
})
public class CondGraveHasAvailableSlot extends Condition {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.CONDITION,
                SyntaxInfo.builder(CondGraveHasAvailableSlot.class)
                        .addPatterns(
                                "[the] grave %grave% has [an] available slot",
                                "[the] grave %grave% does not have [an] available slot",
                                "[the] grave %grave% has [an] empty slot",
                                "[the] grave %grave% does not have [an] empty slot"
                        )
                        .build()
        );
    }

    private Expression<Grave> grave;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.grave = (Expression<Grave>) expressions[0];
        setNegated(matchedPattern == 1 || matchedPattern == 3);
        return true;
    }

    @Override
    public boolean check(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Grave graveValue = grave.getSingle(event);
        if (api == null || graveValue == null) {
            return isNegated();
        }

        boolean result = api.hasAvailableGraveSlot(graveValue);
        return isNegated() != result;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "grave " + grave + (isNegated() ? " does not have an available slot" : " has an available slot");
    }
}
