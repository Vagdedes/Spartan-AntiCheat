package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_Teleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Packet_Teleport extends PacketAdapter {

    public static class TeleportData {

        private final Location location;
        private final boolean silent;

        public TeleportData(Location location, boolean silent) {
            this.location = location;
            this.silent = silent;
        }

        public Location getLocation() {
            return location;
        }

        public boolean isSilent() {
            return silent;
        }
    }

    public Packet_Teleport() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.POSITION,
                PacketType.Play.Server.RESPAWN
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

        if (protocol.spartanPlayer.bedrockPlayer
                || protocol.isLoading()) {
            return;
        }
        PacketType packetType = event.getPacket().getType();

        if (packetType.equals(PacketType.Play.Server.POSITION)) {
            protocol.teleported = true;
            Event_Teleport.teleport(player, true);
            /*
            Location teleportLocation = add(
                    protocol.getLocation().clone(),
                    ProtocolTools.readLocation(event)
            );
            protocol.setLocation(teleportLocation);

            if (protocol.mutateTeleport) {
                protocol.teleportEngine.add(new TeleportData(teleportLocation, true));
                protocol.mutateTeleport = false;
            } else {
                protocol.teleportEngine.add(new TeleportData(teleportLocation, false));
            }
             */

        } else if (packetType.equals(PacketType.Play.Server.RESPAWN)
                || packetType.equals(PacketType.Play.Server.MOUNT)) {
            /*
            protocol.setLocation(new Location(protocol.getLocation().getWorld(), 0, 0, 0));
            protocol.mutateTeleport = true;
             */
            protocol.teleported = true;

            if (packetType.equals(PacketType.Play.Server.RESPAWN)) {
                Event_Teleport.respawn(player, true);
            }
        }
    }

    public static Location add(Location f, Location t) {
        return new Location(
                t.getWorld(),
                f.getX() + t.getX(),
                f.getY() + t.getY(),
                f.getZ() + t.getZ()
        );
    }

}
