package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.event.ServerBlockChange;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.entity.Player;

public class PacketServerBlockHandle extends PacketAdapter {

    public PacketServerBlockHandle() {
        super(Register.plugin, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(player);
        PacketContainer packet = event.getPacket();
        BlockPosition blockPosition = packet.getBlockPositionModifier().read(0);
        WrappedBlockData blockData = packet.getBlockData().read(0);
        ServerBlockChange serverBlockChange = new ServerBlockChange(blockPosition, blockData.getType());
        protocol.packetWorld.worldChange(serverBlockChange);
    }

}