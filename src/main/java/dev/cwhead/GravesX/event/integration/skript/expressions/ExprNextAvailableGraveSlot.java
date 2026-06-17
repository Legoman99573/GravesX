package dev.cwhead.GravesX.event.integration.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.api.grave.GraveManagementAPI;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("GravesX Next Available Grave Slot")
@Description("Gets the next empty slot in a grave inventory through the GravesX grave management API. Returns -1 when no slot is available.")
@Examples({
        "set {_slot} to next available grave slot of {_grave}",
        "if {_slot} >= 0:",
        "\tadd player's tool to slot {_slot} of grave {_grave}"
})
public class ExprNextAvailableGraveSlot extends SimpleExpression<Number> {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprNextAvailableGraveSlot.class, Number.class)
                        .addPatterns(
                                "[the] next available grave slot of %grave%",
                                "[the] next available slot of [the] grave %grave%"
                        )
                        .build()
        );
    }

    private Expression<Grave> grave;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.grave = (Expression<Grave>) expressions[0];
        return true;
    }

    @Override
    protected @Nullable Number[] get(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Grave graveValue = grave.getSingle(event);
        if (api == null || graveValue == null) {
            return null;
        }

        return new Number[]{api.getNextAvailableGraveSlot(graveValue)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "next available grave slot of " + grave;
    }
}
