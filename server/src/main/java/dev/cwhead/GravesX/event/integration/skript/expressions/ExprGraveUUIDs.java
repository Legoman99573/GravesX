package dev.cwhead.GravesX.event.integration.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import dev.cwhead.GravesX.api.grave.GraveManagementAPI;
import dev.cwhead.GravesX.integration.SkriptImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.List;
import java.util.UUID;

@Name("GravesX Grave UUIDs")
@Description("Gets loaded grave UUIDs, optionally filtered by player, through the GravesX grave management API.")
@Examples({
        "loop grave uuids of player:",
        "\tset {_grave} to grave from uuid loop-value",
        "\tif {_grave} is set:",
        "\t\tbroadcast \"%player% has grave UUID %loop-value% at %event-location%\"",
        "set {_graveUUIDs::*} to grave uuids of player",
        "if size of {_graveUUIDs::*} > 1:",
        "\tset {_secondGrave} to grave from uuid {_graveUUIDs::2}",
        "\tif {_secondGrave} is set:",
        "\t\tremove grave {_secondGrave}"
})
public class ExprGraveUUIDs extends SimpleExpression<String> {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprGraveUUIDs.class, String.class)
                        .addPatterns(
                                "[the] grave uuids",
                                "[the] grave uuids of %player%",
                                "[the] uuid[s] of graves",
                                "[the] uuid[s] of graves of %player%"
                        )
                        .build()
        );
    }

    private Expression<Player> player;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        if (matchedPattern == 1 || matchedPattern == 3) {
            this.player = (Expression<Player>) expressions[0];
        }
        return true;
    }

    @Override
    protected @Nullable String[] get(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Player playerValue = player != null ? player.getSingle(event) : null;

        List<UUID> graveUUIDs = playerValue != null ? api.getGraveUUIDs(playerValue) : api.getGraveUUIDs();
        return graveUUIDs.stream()
                .map(UUID::toString)
                .toArray(String[]::new);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return player != null ? "grave uuids of " + player : "grave uuids";
    }
}
