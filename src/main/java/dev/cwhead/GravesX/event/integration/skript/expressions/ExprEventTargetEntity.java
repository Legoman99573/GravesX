package dev.cwhead.GravesX.event.integration.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import dev.cwhead.GravesX.event.graveevent.GraveEntityEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprEventTargetEntity extends SimpleExpression<LivingEntity> {

    @Override
    protected @Nullable LivingEntity[] get(Event e) {
        if (e instanceof GraveEntityEvent) {
            return new LivingEntity[]{((GraveEntityEvent) e).getTargetEntity()};
        }
        return null;
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends LivingEntity> getReturnType() {
        return LivingEntity.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "event target entity";
    }
}