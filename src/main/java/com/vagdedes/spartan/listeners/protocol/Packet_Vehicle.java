package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_Vehicle;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
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
                        PacketType.Play.Client.LOOK,
                        PacketType.Play.Server.MOUNT
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            boolean dismount = event.getPacket().getBooleans().read(1);

            if (protocol.spartanPlayer.isBedrockPlayer()) {
                return;
            }

            protocol.keepEntity = 0;
            protocol.vehicleStatus = dismount;
        } else {
            if (protocol.keepEntity < 10) protocol.keepEntity++;
        }
    }
    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Server.MOUNT)) {
            Player player = event.getPlayer();
            SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
            protocol.timerBalancer.addBalance(50);
            if (protocol.vehicleStatus && protocol.keepEntity != 0) {
                VehicleEnterEvent bukkitEvent = new VehicleEnterEvent((Vehicle) player.getVehicle(), player);
                bukkitEvent.setCancelled(event.isCancelled());
                Event_Vehicle.enter(bukkitEvent);
            } else {
                VehicleExitEvent bukkitEvent = new VehicleExitEvent((Vehicle) player.getVehicle(), player);
                bukkitEvent.setCancelled(event.isCancelled());
                Event_Vehicle.exit(bukkitEvent);
            }
        }
    }

}