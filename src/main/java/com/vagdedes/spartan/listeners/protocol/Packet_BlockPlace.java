package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_BlockPlace;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

public class Packet_BlockPlace extends PacketAdapter {

    public Packet_BlockPlace() {
        super(Register.plugin, ListenerPriority.HIGHEST,
                        PacketType.Play.Client.USE_ITEM,
                        PacketType.Play.Client.BLOCK_PLACE
        );
        // Method: Event_BlockPlace.event()
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
        PacketContainer packet = event.getPacket();
        if (isPlacingBlock(packet)) {
            if (packet.getBlockPositionModifier().getValues().isEmpty() && packet.getMovingBlockPositions().getValues().isEmpty()) {
                // stub for debug
            } else {

                BlockPosition blockPosition = new BlockPosition(0, 0, 0);
                EnumWrappers.Direction direction = null;
                if (!packet.getMovingBlockPositions().getValues().isEmpty()) {
                    blockPosition = packet.getMovingBlockPositions().read(0).getBlockPosition();
                    direction = packet.getMovingBlockPositions().read(0).getDirection();
                }
                if (!packet.getBlockPositionModifier().getValues().isEmpty()) {
                    blockPosition = packet.getBlockPositionModifier().read(0);
                    if (packet.getDirections().getValues().isEmpty()) {
                        int directionInt = packet.getIntegers().read(0);
                        direction = EnumWrappers.Direction.values()[directionInt];
                    } else {
                        direction = packet.getDirections().read(0);
                    }
                }

                Location l = new Location(player.getWorld(), blockPosition.toVector().getBlockX(), blockPosition.toVector().getBlockY(),  blockPosition.toVector().getBlockZ());
                l.add(getDirection(BlockFace.valueOf(direction.name())));
                World world = player.getWorld();
                Block block = world.getBlockAt((int) l.getX(), (int) l.getY(), (int) l.getZ());
                if (player.getInventory().getItemInHand() instanceof Block
                                && !isInPlayer(protocol.getLocation(), block.getLocation())) {
                    BlockPlaceEvent blockPlaceEvent =
                                    new BlockPlaceEvent(
                                                    block,
                                                    block.getState(), player.getLocation().getBlock(),
                                                    player.getInventory().getItemInMainHand(), player, true);
                    Event_BlockPlace.event(blockPlaceEvent);
                    protocol.rightClickCounter = 0;
                } else {
                    protocol.rightClickCounter++;
                }
            }
        } else {
            protocol.rightClickCounter++;
        }
    }

    public boolean isPlacingBlock(PacketContainer packet) {
        BlockPosition blockPosition = new BlockPosition(0, 0, 0);
        if (packet.getHands().getValues().isEmpty()) {
            if (!packet.getMovingBlockPositions().getValues().isEmpty()) {
                blockPosition = packet.getMovingBlockPositions().read(0).getBlockPosition();
            }
            if (!packet.getBlockPositionModifier().getValues().isEmpty()) {
                blockPosition = packet.getBlockPositionModifier().read(0);
            }
            return (blockPosition.getY() != -1);
        } else {
            return (packet.getType().equals(PacketType.Play.Client.USE_ITEM)
                            && packet.getHands().read(0).equals(EnumWrappers.Hand.MAIN_HAND));
        }
    }

    public boolean isInPlayer(Location player, Location block) {
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();

        double playerWidth = 1.0;
        double playerHeight = 2.0;

        double minX = playerX - (playerWidth / 2);
        double maxX = playerX + (playerWidth / 2);
        double minY = playerY;
        double maxY = playerY + playerHeight;
        double minZ = playerZ - (playerWidth / 2);
        double maxZ = playerZ + (playerWidth / 2);

        double blockX = block.getX();
        double blockY = block.getY();
        double blockZ = block.getZ();

        return (blockX >= minX && blockX <= maxX) &&
                        (blockY >= minY && blockY <= maxY) &&
                        (blockZ >= minZ && blockZ <= maxZ);
    }

    public Vector getDirection(BlockFace face) {
        Vector direction = new Vector(face.getModX(), face.getModY(), face.getModZ());
        if (face.getModX() != 0 || face.getModY() != 0 || face.getModZ() != 0) {
            direction.normalize();
        }

        return direction;
    }


}