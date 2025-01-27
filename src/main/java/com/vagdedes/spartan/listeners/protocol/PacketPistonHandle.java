package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.Location;

public class PacketPistonHandle extends PacketAdapter {

    public PacketPistonHandle() {
        super(Register.plugin, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_ACTION);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        if (packet.getStructures().getValues().toString().contains("piston")) {
            PlayerProtocol protocol = PluginBase.getProtocol(event.getPlayer());
            protocol.getComponentY().pistonHandle = true;
            Location blockLocation = packet.getBlockPositionModifier()
                    .read(0)
                    .toLocation(protocol.getWorld());

            if (isPlayerInBox(protocol.getLocation(), blockLocation, 3)) {
                protocol.getComponentY().pistonTick = true;
                protocol.getComponentXZ().pistonTick = true;
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