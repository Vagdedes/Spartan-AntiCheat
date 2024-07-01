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
import com.vagdedes.spartan.listeners.protocol.move.BackgroundMove;
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
        PacketContainer packetContainer = event.getPacket();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

        if (protocol.isOnLoadStatus()) return;

        if (packetContainer.getType().equals(PacketType.Play.Server.POSITION)) {
            Location teleportLocation = BackgroundMove.readLocation(event);
            if (protocol.isMutateTeleport()) {
                protocol.teleportEngine.add(new TeleportData(teleportLocation, true));
                protocol.setMutateTeleport(false);
            } else {
                protocol.teleportEngine.add(new TeleportData(teleportLocation, false));
            }

        }
        if (packetContainer.getType().equals(PacketType.Play.Server.RESPAWN)
                        || packetContainer.getType().equals(PacketType.Play.Server.MOUNT)) {
            protocol.setMutateTeleport(true);
        }
    }

}
