package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.modules.TeleportData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ServerPosition extends PacketAdapter {

    public ServerPosition() {
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

        if (ProtocolLib.isTemporary(player)) {
            return;
        }
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

        if (protocol.isLoading()) {
            return;
        }
        PacketContainer packetContainer = event.getPacket();

        if (packetContainer.getType().equals(PacketType.Play.Server.POSITION)) {
            Location teleportLocation = add(protocol.getLocation().clone(), Move.readLocation(event));
            protocol.setLocation(teleportLocation);
            if (protocol.isMutateTeleport()) {
                protocol.teleportEngine.add(new TeleportData(teleportLocation, true));
                protocol.setMutateTeleport(false);
            } else {
                protocol.teleportEngine.add(new TeleportData(teleportLocation, false));
            }

        }
        if (packetContainer.getType().equals(PacketType.Play.Server.RESPAWN)
                || packetContainer.getType().equals(PacketType.Play.Server.MOUNT)) {
            protocol.setLocation(new Location(protocol.getLocation().getWorld(), 0, 0, 0));
            protocol.setMutateTeleport(true);
        }

    }
    public static Location add(Location f, Location t) {
        return new Location(t.getWorld(), f.getX() + t.getX(),
                        f.getY() + t.getY(), f.getZ() + t.getZ());
    }

}
