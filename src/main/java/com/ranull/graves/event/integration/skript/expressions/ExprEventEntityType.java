package com.ranull.graves.event.integration.skript.expressions;

import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import com.ranull.graves.event.GraveEvent;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.jetbrains.annotations.Nullable;

public class ExprEventEntityType extends SimpleExpression<EntityType> {

    @Override
    protected @Nullable EntityType[] get(Event e) {
        if (e instanceof GraveEvent) {
            return new EntityType[]{((GraveEvent) e).getEntityType()};
        }
        return null;
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends EntityType> getReturnType() {
        return EntityType.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "event entity type";
    }
}