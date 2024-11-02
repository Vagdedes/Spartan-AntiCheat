package com.vagdedes.spartan.utils.minecraft.protocol;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Location;

public class ProtocolTools {

    public static Location readLocation(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        if (packet.getDoubles().size() >= 3) {
            return new Location(
                    event.getPlayer().getWorld(),
                    packet.getDoubles().read(0),
                    packet.getDoubles().read(1),
                    packet.getDoubles().read(2)
            );
        } else {
            return null;
        }
    }

    public static boolean onGroundPacketLevel(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        return packet.getBooleans().read(0);
    }

    public static boolean invalidTeleport(Location location) {
        return location == null
                || location.getX() == 8.5
                || location.getZ() == 8.5;
    }
}
