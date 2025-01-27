package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.concurrent.CheckThread;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.listeners.bukkit.MovementEvent;
import com.vagdedes.spartan.listeners.bukkit.VehicleEvent;
import com.vagdedes.spartan.utils.minecraft.protocol.ProtocolTools;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class VehicleHandle extends PacketAdapter {

    public VehicleHandle() {
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
        Player player = event.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(player);

        if (protocol.bukkitExtra.isBedrockPlayer()) {
            return;
        }
        if (ProtocolTools.hasPosition(event.getPacket().getType()) && protocol.entityHandle) {
            protocol.entityHandle = false;
        }
        final boolean[] nearbyEntities = {false};
        CheckThread.run(() -> {
            for (Entity entity : protocol.bukkitExtra.getNearbyEntities(4.5)) {
                if (entity.getUniqueId() != protocol.getUUID()) {
                    nearbyEntities[0] = true;
                    break;
                }
            }
            if (nearbyEntities[0]) {
                if (ProtocolTools.hasPosition(event.getPacket().getType()) && protocol.entityHandle) {
                    protocol.timerBalancer.addBalance(50);
                    VehicleExitEvent bukkitEvent = new VehicleExitEvent(null, protocol.bukkit());
                    bukkitEvent.setCancelled(event.isCancelled());
                    VehicleEvent.exit(bukkitEvent);
                } else if (event.getPacket().getType().equals(PacketType.Play.Client.STEER_VEHICLE) && !protocol.entityHandle) {
                    protocol.timerBalancer.addBalance(50);
                    VehicleEnterEvent bukkitEvent = new VehicleEnterEvent(null, protocol.bukkit());
                    bukkitEvent.setCancelled(event.isCancelled());
                    VehicleEvent.enter(bukkitEvent);
                    protocol.entityHandle = true;
                }
            }
            if (event.getPacket().getType().equals(PacketType.Play.Client.STEER_VEHICLE)) {
                if (protocol.bukkitExtra.getVehicle() != null) {
                    Entity vehicle = protocol.bukkitExtra.getVehicle();
                    Location location = vehicle.getLocation();
                    protocol.setLocation(location);
                    MovementEvent.event(new PlayerMoveEvent(player, protocol.getFromLocation(), protocol.getLocation()), true);
                }
            }
        });
    }
}