package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Attack extends PacketAdapter {

    public Attack() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.USE_ENTITY);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();

        SpartanBukkit.transferTask(player, () -> {
            Entity entity = ProtocolLibrary.getProtocolManager().
                    getEntityFromID(player.getWorld(), event.getPacket().getIntegers().read(0));
            PacketContainer packet = event.getPacket();

            if (entity instanceof LivingEntity
                    && SpartanBukkit.packetsEnabled(player)) {
                Shared.attack(
                        new PlayerAttackEvent(
                                player,
                                (LivingEntity) entity,
                                false
                        )
                );
            }
        });
    }

}
