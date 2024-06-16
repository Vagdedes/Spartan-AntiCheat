package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.async.LagCompensation;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class Join extends PacketAdapter {

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
        SpartanBukkit.getProtocol(player).setLastTransaction();
        LagCompensation.newPacket(player.getEntityId());
        SpartanBukkit.transferTask(player, () -> {
            SpartanBukkit.createProtocol(player);
            Shared.join(new PlayerJoinEvent(player, (String) null));
        });
    }

}