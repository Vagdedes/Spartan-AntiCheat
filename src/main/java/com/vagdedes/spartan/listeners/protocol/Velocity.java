package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.listeners.Shared;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

public class Velocity extends PacketAdapter {
    

    public Velocity() {
        super(
                Register.plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.ENTITY_VELOCITY
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();

        if (ProtocolLib.isTemporary(player)) {
            return;
        }
        PacketContainer packet = event.getPacket();
        int id = packet.getIntegers().getValues().get(0);

        if (player.getEntityId() == id) {
            double x = packet.getIntegers().read(1).doubleValue() / 8000.0D,
                    y = packet.getIntegers().read(2).doubleValue() / 8000.0D,
                    z = packet.getIntegers().read(3).doubleValue() / 8000.0D;
            Shared.velocity(new PlayerVelocityEvent(player, new Vector(x, y, z)));
        }
    }

}
