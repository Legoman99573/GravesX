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
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("GravesX Auto Loot Grave")
@Description("Auto-loots a grave through the GravesX grave management API.")
@Examples("auto loot grave event-grave at event-location by player")
public class EffAutoLootGrave extends Effect {
    static {
        SkriptAddon addon = SkriptImpl.getActiveSkriptAddon();

        addon.syntaxRegistry().register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffAutoLootGrave.class)
                        .addPatterns("auto loot [the] grave %grave% at %location% by %entity%")
                        .build()
        );
    }

    private Expression<Grave> grave;
    private Expression<Location> location;
    private Expression<Entity> entity;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.grave = (Expression<Grave>) expressions[0];
        this.location = (Expression<Location>) expressions[1];
        this.entity = (Expression<Entity>) expressions[2];
        return true;
    }

    @Override
    protected void execute(Event event) {
        GraveManagementAPI api = SkriptImpl.getGraveManagementAPI();
        Grave graveValue = grave.getSingle(event);
        Location locationValue = location.getSingle(event);
        Entity entityValue = entity.getSingle(event);

        if (api != null && graveValue != null && locationValue != null && entityValue != null) {
            api.autoLootGrave(entityValue, locationValue, graveValue);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "auto loot grave " + grave + " at " + location + " by " + entity;
    }
}
