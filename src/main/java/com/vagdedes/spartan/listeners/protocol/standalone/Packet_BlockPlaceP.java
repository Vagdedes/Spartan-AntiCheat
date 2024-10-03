package com.vagdedes.spartan.listeners.protocol.standalone;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Player;

/*
That's not mean event after block place...
That's mean just Block place packet.
Need to fix timer falsest.
Minecraft's developers mixed up the names of packets,
and BLOCK_PLACE = USE_ITEM
*/
public class Packet_BlockPlaceP extends PacketAdapter {

    public Packet_BlockPlaceP() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.BLOCK_PLACE
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
        protocol.timerBalancer.addBalance(50);
    }
}