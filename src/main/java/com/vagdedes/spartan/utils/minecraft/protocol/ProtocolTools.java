package com.vagdedes.spartan.utils.minecraft.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.listeners.protocol.MovementListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class ProtocolTools {

    public static Location readLocation(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        if (packet.getDoubles().size() >= 3) {
            return new Location(
                    ProtocolLib.getWorld(event.getPlayer()),
                    packet.getDoubles().read(0),
                    packet.getDoubles().read(1),
                    packet.getDoubles().read(2)
            );
        } else {
            return null;
        }
    }
    public static boolean isFlying(PacketEvent event, Location to, Location from) {
        PacketType p = event.getPacket().getType();
        PlayerProtocol protocol = PluginBase.getProtocol(event.getPlayer());
        if (p.equals(PacketType.Play.Client.POSITION) && to.toVector().equals(from.toVector()))
            return true;
        else return
        (
        p.equals(
        PacketType.Play.Client.FLYING)
        ||
        p.equals(
        PacketType.Play.Client.GROUND)
        );
    }

    public static boolean onGroundPacketLevel(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        return packet.getBooleans().size() > 0
                && packet.getBooleans().read(0);
    }

    public static Set<MovementListener.tpFlags> getTeleportFlags(PacketEvent event) {
        String s = event.getPacket().getStructures().getValues().get(0).toString();
        Set<MovementListener.tpFlags> flags = new HashSet<>(3);
        s = s.replace("X_ROT", "").replace("Y_ROT", "");
        if (s.contains("X")) flags.add(MovementListener.tpFlags.X);
        if (s.contains("Y")) flags.add(MovementListener.tpFlags.Y);
        if (s.contains("Z")) flags.add(MovementListener.tpFlags.Z);
        return flags;
    }

    public static boolean invalidTeleport(Location location) {
        return location == null
                || location.getX() == 8.5D
                || location.getZ() == 8.5D;
    }
    public static boolean isLoadLocation(Location location) {
        return (location.getX() == 1 && location.getY() == 1 && location.getZ() == 1);
    }

    public static Location getLoadLocation(Player player) {
        return new Location(ProtocolLib.getWorld(player), 1, 1, 1);
    }

    public static boolean hasPosition(PacketType type) {
        return (type.equals(PacketType.Play.Client.POSITION)
                        || type.equals(PacketType.Play.Client.POSITION_LOOK));
    }
    public static boolean hasRotation(PacketType type) {
        return (type.equals(PacketType.Play.Client.LOOK)
                        || type.equals(PacketType.Play.Client.POSITION_LOOK));
    }
}
