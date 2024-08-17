package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.Shared;
import com.vagdedes.spartan.utils.java.OverflowMap;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Attack extends PacketAdapter {

    private final Map<UUID, Integer> pendingAttacks = new OverflowMap<>(new ConcurrentHashMap<>(), 1_024);

    public Attack() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Server.DAMAGE_EVENT
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ENTITY) {
            handleUseEntity(event);
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.DAMAGE_EVENT) {
            handleEntityDamage(event);
        }
    }

    private void handleUseEntity(PacketEvent event) {
        Player player = event.getPlayer();

        if (ProtocolLib.isTemporary(player)) {
            return;
        }
        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);

        if ((!packet.getEntityUseActions().getValues().isEmpty()) ?
                        packet.getEntityUseActions().read(0).equals(EnumWrappers.EntityUseAction.ATTACK)
                        : packet.getEnumEntityUseActions().read(0).getAction().equals(
                        EnumWrappers.EntityUseAction.ATTACK)) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol(entityId);

            if (protocol != null) {
                pendingAttacks.put(player.getUniqueId(), entityId);
                Shared.useEntity(
                        new PlayerAttackEvent(
                                player,
                                protocol.player,
                                false
                        )
                );
            }
        }
    }

    private void handleEntityDamage(PacketEvent event) {
        if (ProtocolLib.isTemporary(event.getPlayer())) {
            return;
        }
        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);

        pendingAttacks.entrySet().removeIf(entry -> {
            UUID playerUUID = entry.getKey();
            int pendingEntityId = entry.getValue();

            if (pendingEntityId == entityId) {
                Player attacker = plugin.getServer().getPlayer(playerUUID);

                if (attacker != null) {
                    SpartanProtocol protocol = SpartanBukkit.getProtocol(entityId);

                    if (protocol != null) {
                        Shared.attack(
                                new PlayerAttackEvent(
                                        attacker,
                                        protocol.player,
                                        false
                                )
                        );
                    }
                }
                return true;
            }
            return false;
        });
    }

}
