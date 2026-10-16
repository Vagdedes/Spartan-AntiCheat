package ai.idealistic.spartan.listeners.protocol;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.abstraction.event.ServerBlockChange;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.compatibility.necessary.protocollib.*;
import ai.idealistic.spartan.functionality.concurrent.CheckThread;
import ai.idealistic.spartan.functionality.server.Config;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.listeners.bukkit.PlaceEvent;
import ai.idealistic.spartan.listeners.bukkit.standalone.ChunksEvent;
import ai.idealistic.spartan.utils.minecraft.world.BlockUtils;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BlockPlaceListener extends PacketAdapter {

    public BlockPlaceListener() {
        super(Register.plugin, ListenerPriority.HIGHEST, init());
        // Method: Event_BlockPlace.event()
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(player);

        if (!Config.settings.getBoolean("Important.bedrock_on_protocollib")
                && protocol.isBedrockPlayer()) {
            return;
        }
        PacketContainer packet = event.getPacket();
        World world = player.getWorld();

        CheckThread.run(protocol, () -> {
            if (isPlacingBlock(packet)) {
                if (packet.getBlockPositionModifier().size() == 0
                        && packet.getMovingBlockPositions().size() == 0) {
                    // stub for debug
                } else {
                    BlockPosition blockPosition = new BlockPosition(0, 0, 0);
                    EnumWrappers.Direction direction = null;
                    if (packet.getMovingBlockPositions().size() > 0) {
                        blockPosition = MovingBlockPositionsPlib.getSafeMovingBlockPositions(packet, 0).getBlockPosition();
                        direction = MovingBlockPositionsPlib.getSafeMovingBlockPositions(packet, 0).getDirection();
                    }
                    if (packet.getBlockPositionModifier().size() > 0) {
                        blockPosition = BlockPositionPlib.getSafeBlockPosition(packet, 0);
                        if (packet.getDirections().size() == 0) {
                            int directionInt = BackPlib.getSafeInt(packet, 0);
                            EnumWrappers.Direction[] directions = EnumWrappers.Direction.values();

                            if (directionInt < 0 || directionInt >= directions.length) {
                                return; // Legacy packets use 255 when no face was clicked
                            }
                            direction = directions[directionInt];
                        } else {
                            direction = DirectionPlib.getSafeDirection(packet, 0);
                        }
                    }

                    Location l = new Location(
                            world,
                            blockPosition.toVector().getBlockX(),
                            blockPosition.toVector().getBlockY(),
                            blockPosition.toVector().getBlockZ()
                    );
                    if (direction == null) return;
                    l.add(getDirection(BlockFace.valueOf(direction.name())));

                    Block block = ChunksEvent.getBlockAsync(new Location(world, (int) l.getX(), (int) l.getY(), (int) l.getZ()));

                    if (block == null) return;
                    boolean isMainHand = !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                            || HandsPlib.getSafeHand(event.getPacket(), 0) == EnumWrappers.Hand.MAIN_HAND;

                    ItemStack itemInHand = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                            ? (isMainHand ? protocol.getInventory().getItemInMainHand()
                               : protocol.getInventory().getItemInOffHand()) : player.getItemInHand();
                    if (itemInHand.getType().isBlock()) {
                        if (!isInPlayer(protocol.getLocation(), block.getLocation())) {
                            Block blockAgainst = ChunksEvent.getBlockAsync(player.getLocation());
                            Location blockLoc = new Location(world, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
                            if (!BlockUtils.isSurroundedByAir(protocol, blockLoc)
                                    || !BlockUtils.isSurroundedByAir(protocol, blockLoc.clone().add(0, 1, 0))) {
                                Material material = itemInHand.getType();
                                protocol.packetWorld.worldChange(new ServerBlockChange(blockPosition, material, world));
                                protocol.packetWorld.worldChange(new ServerBlockChange(
                                        new BlockPosition(blockPosition.getX(), blockPosition.getY() + 1, blockPosition.getZ()),
                                        material, world
                                ));
                            }
                            PlaceEvent.event(protocol, block, blockAgainst, event, true);
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

    private boolean isPlacingBlock(PacketContainer packet) {
        BlockPosition blockPosition = new BlockPosition(0, 0, 0);
        if (packet.getHands().size() == 0) {
            if (packet.getMovingBlockPositions().size() > 0) {
                blockPosition = MovingBlockPositionsPlib.getSafeMovingBlockPositions(packet, 0).getBlockPosition();
            }
            if (packet.getBlockPositionModifier().size() > 0) {
                blockPosition = BlockPositionPlib.getSafeBlockPosition(packet, 0);
            }

            return (blockPosition.getY() != -1);
        } else {
            return ((packet.getType().equals(PacketType.Play.Client.USE_ITEM)
                    || packet.getType().toString().contains("USE_ITEM_ON"))
                    && HandsPlib.getSafeHand(packet, 0).equals(EnumWrappers.Hand.MAIN_HAND));
        }
    }

    private boolean isInPlayer(Location player, Location block) {
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

    private Vector getDirection(BlockFace face) {
        Vector direction = new Vector(face.getModX(), face.getModY(), face.getModZ());
        if (face.getModX() != 0 || face.getModY() != 0 || face.getModZ() != 0) {
            direction.normalize();
        }

        return direction;
    }

    private static List<PacketType> init() {
        List<PacketType> listeners = new ArrayList<>();
        listeners.add(PacketType.Play.Client.BLOCK_PLACE);
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9))
            listeners.add(PacketType.Play.Client.USE_ITEM);
        if (ProtocolLib.isPacketSupported("USE_ITEM_ON"))
            listeners.add(PacketType.Play.Client.USE_ITEM_ON);
        return listeners;
    }
}