package com.vagdedes.spartan.listeners.protocol.standalone;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class JoinListener extends PacketAdapter {

    public JoinListener() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.LOGIN
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

        if (protocol.spartan.isBedrockPlayer()) {
            return;
        }
        LegacyLagCompensationListener.newPacket(protocol.spartan.getEntityId());
        protocol.transactionBoot = true;
        Bukkit.getScheduler().runTaskLater(Register.plugin, () -> {
            if (protocol.spartan.getVehicle() != null) {
                protocol.entityHandle = true;
            }
        }, 1L);
    }

}