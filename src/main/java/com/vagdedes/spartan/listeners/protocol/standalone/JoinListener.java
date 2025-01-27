package com.vagdedes.spartan.listeners.protocol.standalone;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
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
        PlayerProtocol protocol = PluginBase.getProtocol(player);

        if (protocol.bukkitExtra.isBedrockPlayer()) {
            return;
        }
        LegacyLagCompensationListener.newPacket(protocol.bukkitExtra.getEntityId());
        protocol.transactionBoot = true;
        Bukkit.getScheduler().runTaskLater(Register.plugin, () -> {
            if (protocol.bukkitExtra.getVehicle() != null) {
                protocol.entityHandle = true;
            }
        }, 1L);
    }

}