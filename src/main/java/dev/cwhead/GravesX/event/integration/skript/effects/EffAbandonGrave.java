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

@Name("GravesX Abandon Grave")
@Description("Marks a grave as abandoned through the GravesX grave management API.")
@Examples("abandon grave event-grave")
public class EffAbandonGrave extends Effect {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffAbandonGrave.class)
                        .addPatterns("abandon [the] grave %grave%", "mark [the] grave %grave% as abandoned")
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
    protected void execute(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Grave graveValue = grave.getSingle(event);
        if (api != null && graveValue != null) {
            api.abandonGrave(graveValue);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "abandon grave " + grave;
    }
}
