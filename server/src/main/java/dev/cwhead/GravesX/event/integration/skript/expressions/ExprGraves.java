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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.List;

@Name("GravesX Graves")
@Description("Gets loaded graves, optionally filtered by player, through the GravesX grave management API.")
@Examples({
        "loop loaded graves of player:",
        "\tbroadcast \"Grave: %loop-value%, UUID: %grave uuid of loop-value%\""
})
public class ExprGraves extends SimpleExpression<Grave> {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprGraves.class, Grave.class)
                        .addPatterns(
                                "[the] loaded graves",
                                "[the] loaded graves of %player%",
                                "[the] graves of %player%"
                        )
                        .build()
        );
    }

    private Expression<Player> player;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        if (matchedPattern == 1 || matchedPattern == 2) {
            this.player = (Expression<Player>) expressions[0];
        }
        return true;
    }

    @Override
    protected @Nullable Grave[] get(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Player playerValue = player != null ? player.getSingle(event) : null;

        List<Grave> graves = api.getGraves(playerValue);
        return graves.toArray(new Grave[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Grave> getReturnType() {
        return Grave.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return player != null ? "loaded graves of " + player : "loaded graves";
    }
}
