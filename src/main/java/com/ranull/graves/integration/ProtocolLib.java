package com.ranull.graves.integration;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.ranull.graves.Graves;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

/**
 * Provides integration with ProtocolLib to manage block changes and updates.
 */
public final class ProtocolLib {
    private final Graves plugin;
    private final ProtocolManager protocolManager;

    /**
     * Constructs a new ProtocolLib instance with the specified Graves plugin.
     *
     * @param plugin The main Graves plugin instance.
     */
    public ProtocolLib(Graves plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * Sets the block at a specific location to a new material and updates the client.
     *
     * @param block   The block to change.
     * @param material The material to set the block to.
     * @param player  The player to whom the update will be sent.
     */
    public void setBlock(Block block, Material material, Player player) {
        WrappedBlockData wrappedBlockData = WrappedBlockData.createData(material);
        sendServerPacket(player, createBlockChangePacket(block, wrappedBlockData));
    }

    /**
     * Refreshes the block at a specific location to reflect its current state.
     *
     * @param block   The block to refresh.
     * @param player  The player to whom the update will be sent.
     */
    public void refreshBlock(Block block, Player player) {
        sendServerPacket(player, createBlockChangePacket(block, WrappedBlockData.createData(block.getBlockData())));
    }

    /**
     * Creates a PacketContainer for a block change packet.
     *
     * @param block              The block to change.
     * @param wrappedBlockData   The block data to set.
     * @return The PacketContainer for the block change packet.
     */
    private PacketContainer createBlockChangePacket(Block block, WrappedBlockData wrappedBlockData) {
        Location location = block.getLocation();
        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        PacketContainer packetContainer = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);

        packetContainer.getBlockPositionModifier().write(0, blockPosition);
        packetContainer.getBlockData().write(0, wrappedBlockData);

        return packetContainer;
    }

    /**
     * Sends a server packet to a specific player.
     *
     * @param player          The player to send the packet to.
     * @param packetContainer The packet to send.
     */
    private void sendServerPacket(Player player, PacketContainer packetContainer) {
        try {
            protocolManager.sendServerPacket(player, packetContainer);
        } catch (InvocationTargetException ignored) {
        }
    }
}