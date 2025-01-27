package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.listeners.bukkit.TeleportEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportListener extends PacketAdapter {

    public static final PacketType[] packetTypes = new PacketType[]{
            PacketType.Play.Server.POSITION,
            PacketType.Play.Server.RESPAWN
    };

    public TeleportListener() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                packetTypes
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(player);

        if (protocol.bukkitExtra.isBedrockPlayer()) {
            return;
        }
        PacketType packetType = event.getPacket().getType();

        if (packetType.equals(PacketType.Play.Server.POSITION)) {
            protocol.teleported = true;
            TeleportEvent.teleport(player, true, event);
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

        } else if (packetType.equals(PacketType.Play.Server.RESPAWN)) {
            /*
            protocol.setLocation(new Location(protocol.getLocation().getWorld(), 0, 0, 0));
            protocol.mutateTeleport = true;
             */
            protocol.teleported = true;
            TeleportEvent.respawn(player, true, event);
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
