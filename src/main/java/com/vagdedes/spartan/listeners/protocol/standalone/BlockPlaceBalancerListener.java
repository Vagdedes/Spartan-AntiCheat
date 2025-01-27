package com.vagdedes.spartan.listeners.protocol.standalone;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.entity.Player;

/*
That's not mean event after block place...
That's mean just Block place packet.
Need to fix timer falsest.
Minecraft's developers mixed up the names of packets,
and BLOCK_PLACE = USE_ITEM
*/
public class BlockPlaceBalancerListener extends PacketAdapter {

    public BlockPlaceBalancerListener() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                resolvePacketTypes()
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(player);
        protocol.timerBalancer.addBalance(50);
    }

    private static PacketType[] resolvePacketTypes() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            return new PacketType[]{PacketType.Play.Client.USE_ITEM,
                    ProtocolLib.isPacketSupported("USE_ITEM_ON")
                            ? PacketType.Play.Client.USE_ITEM_ON
                            : PacketType.Play.Client.BLOCK_PLACE};
        } else {
            return new PacketType[]{
                    ProtocolLib.isPacketSupported("USE_ITEM_ON")
                            ? PacketType.Play.Client.USE_ITEM_ON
                            : PacketType.Play.Client.BLOCK_PLACE};
        }
    }
}