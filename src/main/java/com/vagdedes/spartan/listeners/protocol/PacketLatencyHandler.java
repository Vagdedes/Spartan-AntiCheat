package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.event.PlayerTransactionEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.listeners.bukkit.MovementEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PacketLatencyHandler extends PacketAdapter {

    public PacketLatencyHandler() {
        super(
                Register.plugin,
                ListenerPriority.MONITOR,
                        (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17))
                                        ? PacketType.Play.Client.PONG :
                                        (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12))
                                        ? PacketType.Play.Client.TRANSACTION
                                        : PacketType.Play.Client.KEEP_ALIVE,
                        (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17))
                                        ? PacketType.Play.Server.PING :
                                        (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12))
                                        ? PacketType.Play.Server.TRANSACTION
                                        : PacketType.Play.Server.KEEP_ALIVE
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(player);
        PacketContainer packet = event.getPacket();
        int id;
        if (!packet.getShorts().getFields().isEmpty()) {
            id = packet.getShorts().read(0);
        } else if (!packet.getIntegers().getFields().isEmpty()) {
            id = packet.getIntegers().read(0);
        } else if (!packet.getLongs().getFields().isEmpty()) {
            id = Math.toIntExact(packet.getLongs().read(0));
        } else return;
        if (id <= -1939 && id >= -1945) {
            protocol.transactionPing = System.currentTimeMillis() - protocol.transactionTime;
            protocol.transactionLastTime = System.currentTimeMillis();
            protocol.transactionSentKeep = false;
            PlayerTransactionEvent transactionEvent = new PlayerTransactionEvent(protocol);
            MovementEvent.transaction(transactionEvent);
            protocol.packetWorld.transactionLock = false;
            Bukkit.getScheduler().runTaskLaterAsynchronously(Register.plugin,
                            () -> sendTransaction(protocol, protocol.transactionId), 10L);
        }
    }
    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(player);
        protocol.transactionSentKeep = true;
        protocol.transactionTime = System.currentTimeMillis();
    }

    public static void startChecking(PlayerProtocol protocol) {
        protocol.transactionId = -1939;
        protocol.transactionBoot = false;
        sendTransaction(protocol, protocol.transactionId);
    }
    public static void sendTransaction(PlayerProtocol protocol, short id) {

        PacketContainer packet = new PacketContainer(
                        MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                        ? PacketType.Play.Server.PING :
                        (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12))
                                        ? PacketType.Play.Server.TRANSACTION
                                        : PacketType.Play.Server.KEEP_ALIVE);

        if (!packet.getShorts().getFields().isEmpty()) {
            packet.getShorts().write(0, id);
            if (packet.getType().equals(PacketType.Play.Server.TRANSACTION)) {
                packet.getIntegers().write(0, 0);
                packet.getBooleans().write(0, false);
            }
        } else if (!packet.getIntegers().getFields().isEmpty()) {
            packet.getIntegers().write(0, (int) id);
        } else if (!packet.getLongs().getFields().isEmpty()) {
            packet.getLongs().write(0, (long) id);
        } else return;

        ProtocolLibrary.getProtocolManager().sendServerPacket(protocol.bukkit(), packet);
        protocol.transactionId--;
        if (protocol.transactionId < -1945)
            protocol.transactionId = -1939;
    }
}