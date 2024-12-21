package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Packet_Debug extends PacketAdapter {

    public Packet_Debug() {
        super(
                Register.plugin,
                ListenerPriority.MONITOR,
                PacketType.Play.Client.getInstance()
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();
        if (event.getPlayer().getName().equals("pawsashatoy")) display(player, "i: " + packet.getType().name() + " " + packet.getStructures().getValues());
    }

    private void display(Player player, String display) {
        player.sendMessage(display);
        Bukkit.getLogger().info(display);
    }

}