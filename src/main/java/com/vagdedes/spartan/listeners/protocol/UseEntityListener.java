package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.concurrent.SpartanScheduler;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.util.UUID;

public class UseEntityListener extends PacketAdapter {

    public UseEntityListener() {
        super(Register.plugin, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        UUID uuid = ProtocolLib.getUUID(player);
        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);
        ProtocolManager m = ProtocolLibrary.getProtocolManager();
        SpartanPlayer spartanPlayer = SpartanBukkit.getProtocol(uuid).spartan;
        SpartanScheduler.run(() -> {
            if ((!packet.getEntityUseActions().getValues().isEmpty()) ?
                            !packet.getEntityUseActions().read(0).equals(EnumWrappers.EntityUseAction.ATTACK)
                            : !packet.getEnumEntityUseActions().read(0).getAction().equals(
                            EnumWrappers.EntityUseAction.ATTACK)) {
                Entity t = null;
                for (Entity entity : spartanPlayer.getNearbyEntities(5)) {
                    if (entity.getEntityId() == entityId) {
                        t = entity;
                        break;
                    }
                }
                if (t instanceof Vehicle) {
                    spartanPlayer.trackers.add(PlayerTrackers.TrackerType.VEHICLE, "exit_tp", 1);
                    spartanPlayer.trackers.add(PlayerTrackers.TrackerType.VEHICLE, "vh_tp", 1);
                    SpartanBukkit.getProtocol(uuid).profile().getRunner(Enums.HackType.IrregularMovements)
                                    .handle(false,
                                                    new VehicleEnterEvent(null, player));
                }
            }
        });
    }

}
