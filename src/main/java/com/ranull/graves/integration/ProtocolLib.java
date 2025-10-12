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
import java.util.Objects;

/**
 * Provides integration with ProtocolLib to manage block changes and updates.
 */
public class ProtocolLib {
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
     * @param block    The block to change.
     * @param material The material to set the block to.
     * @param player   The player to whom the update will be sent.
     */
    public void setBlock(Block block, Material material, Player player) throws InvocationTargetException {
        if (block == null || material == null || player == null) return;
        block.getWorld();
        if (!Objects.equals(block.getWorld(), player.getWorld())) return;

        WrappedBlockData wrapped = WrappedBlockData.createData(material);
        sendServerPacket(player, createBlockChangePacket(block, wrapped));
    }

    /**
     * Refreshes the block at a specific location to reflect its current state.
     *
     * @param block  The block to refresh.
     * @param player The player to whom the update will be sent.
     */
    public void refreshBlock(Block block, Player player) throws InvocationTargetException {
        if (block == null || player == null) return;
        block.getWorld();
        if (!Objects.equals(block.getWorld(), player.getWorld())) return;

        WrappedBlockData current = WrappedBlockData.createData(block.getBlockData());
        sendServerPacket(player, createBlockChangePacket(block, current));
    }

    /**
     * Creates a PacketContainer for a block change packet.
     */
    private PacketContainer createBlockChangePacket(Block block, WrappedBlockData wrappedBlockData) {
        Location loc = block.getLocation();
        BlockPosition pos = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0, pos);
        packet.getBlockData().write(0, wrappedBlockData);
        return packet;
    }

    /**
     * Sends a server packet to a specific player.
     */
    private void sendServerPacket(Player player, PacketContainer packetContainer) throws InvocationTargetException {
        try {
            protocolManager.sendServerPacket(player, packetContainer);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to send BLOCK_CHANGE packet: " + e.getMessage());
            }
        }
    }
}