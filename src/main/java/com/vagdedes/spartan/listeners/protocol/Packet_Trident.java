package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
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
                PacketType.Play.Client.USE_ITEM
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (!event.isCancelled()) { // PlayerRiptideEvent does not implement cancellable
            Player player = event.getPlayer();
            SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

            if (protocol.spartanPlayer.bedrockPlayer) {
                return;
            }
            EnumWrappers.Hand hand = event.getPacket().getHands().read(0);
            ItemStack item = player.getInventory().getItem(hand.ordinal());

            if (item != null && item.getType().equals(Material.TRIDENT)) {
                boolean inWater = player.isInWater(),
                        inRain = protocol.spartanPlayer.getWorld().hasStorm()
                                && protocol.spartanPlayer.movement.getLocation().getBlockY()
                                >= protocol.spartanPlayer.getWorld().getHighestBlockYAt(ProtocolLib.getLocation(player));

                if (inWater || inRain) {
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

}