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
import org.joml.Vector2f;
import org.joml.Vector3d;

import static java.lang.Math.PI;

public class Attack extends PacketAdapter {

    public Attack() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.USE_ENTITY);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();

        SpartanBukkit.runTask(player, () -> {
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

    public Vector2f calculate(final Vector3d from, final Vector3d to) {
        final Vector3d diff = to.sub(from);
        final double distance = Math.hypot(diff.x(), diff.z());
        final float yaw = (float) (Math.atan2(diff.z(), diff.x()) * 180.0F / PI) - 90.0F;
        final float pitch = (float) (-(Math.atan2(diff.y(), distance) * 180.0F / PI));
        return new Vector2f(yaw, pitch);
    }

}
