package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Player;

public class Attack extends PacketAdapter {

    public Attack() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.USE_ENTITY
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();
        int id = packet.getIntegers().read(0);

        if (packet.getEnumEntityUseActions().read(0).getAction().equals(
                EnumWrappers.EntityUseAction.ATTACK
        )) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol(id);

            if (protocol != null) {
                Shared.attack(
                        new PlayerAttackEvent(
                                player,
                                protocol.player,
                                false
                        )
                );
            }
        }
    }
}
