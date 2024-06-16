package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
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
        PacketContainer packetContainer = event.getPacket();
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

        /*
        if (packetContainer.getType() == PacketType.Play.Server.POSITION) {
            Location teleportLocation = BackgroundMove.readLocation(event);
            protocol.getLocation().add(teleportLocation);
            Shared.teleport(new PlayerTeleportEvent(player, protocol.getFrom(), protocol.getLocation()));
        }
        if (packetContainer.getType() == PacketType.Play.Server.RESPAWN) {
            protocol.setLocation(new Location(player.getWorld(), 0, 0, 0));
        }

         */
    }

}
