package dev.cwhead.GravesX.event;

import com.ranull.graves.type.Grave;
import com.ranull.graves.data.BlockData;
import dev.cwhead.GravesX.event.graveevent.GravePlayerEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an event that occurs when a grave is broken by a {@link Player}.
 * <p>
 * This event extends {@link GravePlayerEvent} and provides information about the grave
 * and the player involved when the grave is broken.
 * </p>
 */
public class GraveBreakEvent extends GravePlayerEvent {

    /**
     * A static final instance of {@link HandlerList} used to manage event handlers.
     * <p>
     * This {@link HandlerList} is used to register and manage the handlers for events of this type.
     * It provides the mechanism for adding, removing, and invoking event handlers.
     * </p>
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new GraveBreakEvent.
     *
     * @param block   The block being broken.
     * @param player  The player breaking the block.
     * @param grave   The grave associated with the block being broken.
     */
    public GraveBreakEvent(@NotNull Block block, @NotNull Player player, @NotNull Grave grave) {
        this(block, player, grave, null);
    }

    /**
     * Constructs a new GraveBreakEvent with an optional block type.
     *
     * @param block     The block being broken.
     * @param player    The player breaking the block.
     * @param grave     The grave associated with the block being broken.
     * @param blockType The block type if already known (nullable). If null, it will be resolved from the block.
     */
    public GraveBreakEvent(@NotNull Block block, @NotNull Player player, @NotNull Grave grave, @Nullable BlockData.BlockType blockType) {
        super(grave, player, safeLocation(block, grave), resolveBlockType(blockType, block), block, null, player, null);
    }

    private static @NotNull Location safeLocation(@NotNull Block block, @NotNull Grave grave) {
        return block.getLocation();
    }

    private static @Nullable BlockData.BlockType resolveBlockType(@Nullable BlockData.BlockType given, @NotNull Block block) {
        if (given != null) return given;

        final Material mat = block.getType();

        try {
            return BlockData.BlockType.valueOf(mat.name());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list for this event.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the static list of handlers for this event.
     *
     * @return The static handler list for this event.
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}