package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Packet_Clicks extends PacketAdapter {

    private final Set<UUID> diggingPlayers = new HashSet<>();

    public Packet_Clicks() {
        super(Register.plugin, ListenerPriority.NORMAL, PacketType.Play.Client.ARM_ANIMATION, PacketType.Play.Client.BLOCK_DIG);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();

        if (event.getPacketType() == PacketType.Play.Client.ARM_ANIMATION) {

            if (!diggingPlayers.contains(player.getUniqueId())) {
                handleLeftClick(player);
            }
        } else if (event.getPacketType() == PacketType.Play.Client.BLOCK_DIG) {
            int digType = event.getPacket().getPlayerDigTypes().readSafely(0).ordinal();
            if (digType == 0) {
                diggingPlayers.add(player.getUniqueId());
            } else if (digType == 2) {
                diggingPlayers.remove(player.getUniqueId());
            }
        }
    }

    private void handleLeftClick(Player player) {
        //player.sendMessage("Left-click detected without digging.");
    }
}
