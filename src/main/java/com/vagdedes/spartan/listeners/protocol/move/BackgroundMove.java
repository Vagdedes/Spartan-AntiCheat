package com.vagdedes.spartan.listeners.protocol.move;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.async.LagCompensation;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BackgroundMove {

    static void run(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
        movePacket(event, protocol);

        if (!invalidTeleport(protocol.getLocation())
                && protocol.isOnLoadStatus()) {
            protocol.setOnLoadStatus(false);
        } else if (!invalidTeleport(protocol.getLocation())
                || (invalidTeleport(protocol.getLocation()) && !protocol.isOnLoadStatus())) {
            LagCompensation.add(player.getEntityId(), protocol.getLocation());
        }
    }

    private static void movePacket(PacketEvent event, SpartanProtocol protocol) {
        PacketContainer packet = event.getPacket();
        PacketType type = event.getPacket().getType();

        if (type.equals(PacketType.Play.Client.LOOK)) {
            protocol.getLocation().setYaw(packet.getFloat().read(0));
            protocol.getLocation().setPitch(packet.getFloat().read(1));
            protocol.spartanPlayer.movement.refreshLocation(protocol.getLocation());
        } else if (type.equals(PacketType.Play.Client.POSITION_LOOK)) {
            Location location = readLocation(event);
            protocol.getLocation().setX(location.getX());
            protocol.getLocation().setY(location.getY());
            protocol.getLocation().setZ(location.getZ());
            protocol.getLocation().setYaw(packet.getFloat().read(0));
            protocol.getLocation().setPitch(packet.getFloat().read(1));
            protocol.spartanPlayer.movement.refreshLocation(protocol.getLocation());
        } else if (type.equals(PacketType.Play.Client.POSITION)) {
            Location location = readLocation(event);
            protocol.getLocation().setX(location.getX());
            protocol.getLocation().setY(location.getY());
            protocol.getLocation().setZ(location.getZ());
            protocol.spartanPlayer.movement.refreshLocation(protocol.getLocation());
        }
        protocol.setOnGround(onGroundPacketLevel(event));
    }

    private static Location readLocation(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        return new Location(
                event.getPlayer().getWorld(),
                packet.getDoubles().read(0),
                packet.getDoubles().read(1),
                packet.getDoubles().read(2)
        );
    }

    private static boolean onGroundPacketLevel(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        return packet.getBooleans().read(0);
    }

    private static boolean invalidTeleport(Location location) {
        return location.getX() + location.getZ() == 17;
    }
}
