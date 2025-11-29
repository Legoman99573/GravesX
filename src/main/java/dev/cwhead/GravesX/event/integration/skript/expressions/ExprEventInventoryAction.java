package dev.cwhead.GravesX.event.integration.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import dev.cwhead.GravesX.event.GraveItemTakeEvent;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryAction;
import org.jetbrains.annotations.Nullable;

public class ExprEventInventoryAction extends SimpleExpression<InventoryAction> {

    @Override
    protected @Nullable InventoryAction[] get(Event e) {
        if (e instanceof GraveItemTakeEvent event) {
            return new InventoryAction[]{event.getAction()};
        }
        return null;
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends InventoryAction> getReturnType() {
        return InventoryAction.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "event inventory action";
    }
}
