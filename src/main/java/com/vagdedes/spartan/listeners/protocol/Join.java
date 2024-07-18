package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.MCClient;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.Shared;
import com.vagdedes.spartan.listeners.protocol.async.LagCompensation;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Join extends PacketAdapter {

    private static Map<UUID, MCClient> clientContainer = new ConcurrentHashMap<>();
    public static Map<UUID, MCClient> getContainer() {
        return clientContainer;
    }

    public Join() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.LOGIN
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();

        if (ProtocolLib.isTemporary(player)) {
            return;
        }
        SpartanBukkit.getProtocol(player).setLastTransaction();
        LagCompensation.newPacket(player.getEntityId());
        clientContainer.put(player.getUniqueId(), new MCClient(SpartanBukkit.getProtocol(player)));
        SpartanBukkit.transferTask(player, () -> {
            SpartanBukkit.createProtocol(player);
            Shared.join(new PlayerJoinEvent(player, (String) null));
        });
    }

}