package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_Trident;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Packet_Trident extends PacketAdapter {

    public Packet_Trident() {
        super(
                Register.plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Client.USE_ITEM,
                PacketType.Play.Client.BLOCK_DIG
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (!event.isCancelled()) { // PlayerRiptideEvent does not implement cancellable
            Player player = event.getPlayer();
            SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
            if (protocol.spartan.isBedrockPlayer()) {
                return;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().equals(Material.TRIDENT)) {
                double r = Math.toRadians(protocol.getLocation().getYaw());
                Event_Trident.event(
                                new PlayerRiptideEvent(
                                                player,
                                                item,
                                                new Vector(-Math.sin(r), protocol.getLocation().getPitch() / 90, Math.cos(r))
                                ),
                                true
                );
            }
        }
    }

}