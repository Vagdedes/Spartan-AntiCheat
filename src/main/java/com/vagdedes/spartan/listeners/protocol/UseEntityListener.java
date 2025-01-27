package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.functionality.concurrent.CheckThread;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class UseEntityListener extends PacketAdapter {

    public UseEntityListener() {
        super(Register.plugin, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(player);
        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);

        CheckThread.run(() -> {
            if ((!packet.getEntityUseActions().getValues().isEmpty())
                    ? !packet.getEntityUseActions().read(0).equals(EnumWrappers.EntityUseAction.ATTACK)
                    : !packet.getEnumEntityUseActions().read(0).getAction().equals(
                    EnumWrappers.EntityUseAction.ATTACK)) {
                Entity t = null;
                for (Entity entity : protocol.bukkitExtra.getNearbyEntities(5)) {
                    if (entity.getEntityId() == entityId) {
                        t = entity;
                        break;
                    }
                }
                if (t instanceof Vehicle) {
                    protocol.bukkitExtra.trackers.add(PlayerTrackers.TrackerType.VEHICLE, "exit_tp", 1);
                    protocol.bukkitExtra.trackers.add(PlayerTrackers.TrackerType.VEHICLE, "vh_tp", 1);
                    protocol.profile().executeRunners(
                            false,
                            new VehicleEnterEvent(null, player)
                    );
                }
            }
        });
    }

}
