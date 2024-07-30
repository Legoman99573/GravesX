package com.ranull.graves.event.integration.skript.expressions;

import org.bukkit.event.Event;

import com.ranull.graves.event.GraveEvent;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Nullable;

public class ExprEventInventoryView extends SimpleExpression<InventoryView> {

    @Override
    protected @Nullable InventoryView[] get(Event e) {
        if (e instanceof GraveEvent) {
            return new InventoryView[]{((GraveEvent) e).getInventoryView()};
        }
        return null;
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends InventoryView> getReturnType() {
        return InventoryView.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "event inventory view";
    }
}