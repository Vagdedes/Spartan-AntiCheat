package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Packet_PistonHandle extends PacketAdapter {

    public Packet_PistonHandle() {
        super(Register.plugin, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_ACTION);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
        PacketContainer packet = event.getPacket();
        if (packet.getStructures().getValues().toString().contains("piston")) {
            protocol.getComponentY().pistonHandle = true;
            Location blockLocation = packet.getBlockPositionModifier()
                            .read(0)
                            .toLocation(player.getWorld());
            if (isPlayerInBox(player.getLocation(), blockLocation, 3)) {
                protocol.getComponentY().pistonTick = true;
                protocol.pistonTick = true;
            }
        }
    }
    private boolean isPlayerInBox(Location playerLocation, Location centerLocation, int boxSize) {
        return Math.abs(playerLocation.getX() - centerLocation.getX()) <= boxSize
                        && Math.abs(playerLocation.getY() - centerLocation.getY()) <= boxSize
                        && Math.abs(playerLocation.getZ() - centerLocation.getZ()) <= boxSize;
    }
}