package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_Vehicle;
import com.vagdedes.spartan.utils.minecraft.protocol.ProtocolTools;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class Packet_Vehicle extends PacketAdapter {

    public Packet_Vehicle() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.STEER_VEHICLE,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(event.getPlayer());
        if (protocol.spartan.isBedrockPlayer()) {
            return;
        }
        if (protocol.spartan.getNearbyEntities(4).size() < 2) return;
        if (ProtocolTools.hasPosition(event.getPacket().getType()) && protocol.entityHandle) {
            protocol.timerBalancer.addBalance(50);
            VehicleExitEvent bukkitEvent = new VehicleExitEvent(null, protocol.bukkit);
            bukkitEvent.setCancelled(event.isCancelled());
            Event_Vehicle.exit(bukkitEvent);
            protocol.entityHandle = false;
        } else if (event.getPacket().getType().equals(PacketType.Play.Client.STEER_VEHICLE) && !protocol.entityHandle) {
            protocol.timerBalancer.addBalance(50);
            VehicleEnterEvent bukkitEvent = new VehicleEnterEvent(null, protocol.bukkit);
            bukkitEvent.setCancelled(event.isCancelled());
            Event_Vehicle.enter(bukkitEvent);
            protocol.entityHandle = true;
        }
    }

}