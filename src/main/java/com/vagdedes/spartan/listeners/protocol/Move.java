package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.modules.DeprecateTypes;
import com.vagdedes.spartan.listeners.protocol.modules.RotationData;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;


public class Move extends PacketAdapter {

    public Move() {
        super(Register.plugin, ListenerPriority.LOWEST, PacketType.Play.Client.getInstance().values());
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        if (packet.getType() == PacketType.Play.Client.POSITION
                || packet.getType() == PacketType.Play.Client.POSITION_LOOK
                || packet.getType() == PacketType.Play.Client.LOOK) {
            Player real = event.getPlayer();
            SpartanPlayer player = SpartanBukkit.getPlayer(real);

            if (player == null) {
                return;
            }
            // CPlayerPacket
            SpartanLocation to = readMovePacket(event);
            double xz = to.getX() + to.getZ();

            if (xz == 17
                    || xz == 0 && ProtocolStorage.getSpawnStatus(player.uuid)) {
                return;
            } else {
                ProtocolStorage.spawnStatus.put(player.uuid, false);
            }
            if (ProtocolStorage.lastTeleport.remove(player.uuid) != null) {
                return;
            }
            importRotation(player, to);
            SpartanLocation from = ProtocolStorage.getLocation(player.uuid, to);
            importRotation(player, from);
            boolean ground = packet.getBooleans().read(0);
            ProtocolStorage.groundManager.put(player.uuid, ground);

            if (ProtocolStorage.canCheck(player.uuid)
                    && !ProtocolStorage.getSpawnStatus(player.uuid)) {
                Shared.move(new PlayerMoveEvent(
                        real, from.getBukkitLocation(), to.getBukkitLocation()
                ));
            }
            ProtocolStorage.positionManager.put(player.uuid, to);
            // Action
        } else if (Objects.equals(packet.getType().name(), DeprecateTypes.GROUND.name())
                || Objects.equals(packet.getType().name(), DeprecateTypes.FLYING.name())) {
            boolean ground = packet.getBooleans().read(0);
        }

    }

    public static SpartanLocation readMovePacket(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        if (event.getPacket().getType() == PacketType.Play.Client.LOOK
                || event.getPacket().getType() == PacketType.Play.Client.POSITION_LOOK) {
            SpartanPlayer player = SpartanBukkit.getPlayer(event.getPlayer());

            if (player == null) {
                return new SpartanLocation();
            } else {
                ProtocolStorage.lastRotation.put(
                        player.uuid,
                        new RotationData(
                                packet.getFloat().read(0),
                                packet.getFloat().read(1)
                        )
                );
            }
        }
        if (event.getPacket().getType() == PacketType.Play.Client.LOOK) {
            SpartanPlayer player = SpartanBukkit.getPlayer(event.getPlayer());

            if (player == null) {
                return new SpartanLocation();
            } else {
                return ProtocolStorage.getLocation(player.uuid);
            }
        } else {
            SpartanPlayer player = SpartanBukkit.getPlayer(event.getPlayer());

            if (player == null) {
                return new SpartanLocation();
            } else {
                return new SpartanLocation(
                        player.getWorld(),
                        null,
                        packet.getDoubles().read(0),
                        packet.getDoubles().read(1),
                        packet.getDoubles().read(2),
                        0.0f,
                        0.0f
                );
            }
        }
    }

    public static void importRotation(SpartanPlayer player, SpartanLocation location) {
        RotationData rotation = ProtocolStorage.getRotation(player.uuid);
        location.setYaw(rotation.yaw());
        location.setPitch(rotation.pitch());
    }

    public static boolean onGroundPacketLevel(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        return packet.getBooleans().read(0);
    }

    private static boolean rumiaShield(Player player, Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double y = RayUtils.scaleVal(to.getY() - from.getY(), 2);
        double speed = RayUtils.scaleVal(Math.sqrt(dx * dx + dz * dz), 2);
        return y == -0.09;
    }

}