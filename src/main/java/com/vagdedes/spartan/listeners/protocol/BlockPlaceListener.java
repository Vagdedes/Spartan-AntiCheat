package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.event.ServerBlockChange;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.concurrent.SpartanScheduler;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.BlockPlaceEvent;
import com.vagdedes.spartan.listeners.bukkit.standalone.ChunksEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BlockPlaceListener extends PacketAdapter {

    public BlockPlaceListener() {
        super(Register.plugin, ListenerPriority.HIGHEST,
                via8blockPlace(), PacketType.Play.Client.BLOCK_PLACE,
                via21blockPlace());
        // Method: Event_BlockPlace.event()
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
        PacketContainer packet = event.getPacket();
        SpartanScheduler.run(() -> {
            if (event.getPacket().getType().equals(PacketType.Play.Client.BLOCK_DIG))
                return;
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

                    Location l = new Location(
                                    protocol.getWorld(),
                                    blockPosition.toVector().getBlockX(),
                                    blockPosition.toVector().getBlockY(),
                                    blockPosition.toVector().getBlockZ()
                    );
                    if (direction == null) return;
                    l.add(getDirection(BlockFace.valueOf(direction.name())));

                    World world = player.getWorld();
                    Block block = ChunksEvent.getBlockAsync(new Location(world, (int) l.getX(), (int) l.getY(), (int) l.getZ()));

                    if (block == null) return;
                    boolean isMainHand = !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                                    || event.getPacket().getHands().read(0) == EnumWrappers.Hand.MAIN_HAND;

                    ItemStack itemInHand = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                                    ? (isMainHand ? player.getInventory().getItemInMainHand()
                                    : player.getInventory().getItemInOffHand()) : player.getItemInHand();
                    if (itemInHand.getType().isBlock()) {
                        if (!isInPlayer(protocol.getLocation(), block.getLocation())) {
                            Block blockAgainst = player.getLocation().getBlock();
                            Material material = itemInHand.getType();
                            protocol.packetWorld.worldChange(new ServerBlockChange(blockPosition, material));
                            protocol.packetWorld.worldChange(new ServerBlockChange(
                                            new BlockPosition(blockPosition.getX(), blockPosition.getY() + 1, blockPosition.getZ()),
                                            material
                            ));

                            BlockPlaceEvent.event(protocol, block, blockAgainst, event.isCancelled());
                            protocol.rightClickCounter = 0;
                        } else {
                            protocol.rightClickCounter++;
                        }
                    } else {
                        protocol.rightClickCounter++;
                    }

                }
            } else {
                protocol.rightClickCounter++;
            }
        });
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
            return ((packet.getType().equals(PacketType.Play.Client.USE_ITEM)
                    ||packet.getType().equals(PacketType.Play.Client.USE_ITEM_ON)))
                    && packet.getHands().read(0).equals(EnumWrappers.Hand.MAIN_HAND);
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

    private static PacketType via21blockPlace() {
        return PacketType.Play.Client.USE_ITEM_ON;
        /*
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_21)
                ? PacketType.Play.Client.USE_ITEM_ON
                : PacketType.Play.Client.BLOCK_PLACE;
         */
    }

    private static PacketType via8blockPlace() {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                ? PacketType.Play.Client.USE_ITEM
                : PacketType.Play.Client.BLOCK_DIG;
    }


}