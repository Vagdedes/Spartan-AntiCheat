package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.event.PlayerStayEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.modules.DeprecateTypes;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Move {

    public static void registerPacketListeners(ProtocolManager protocolManager) {
        PacketListener serverSideListener = new PacketAdapter(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.POSITION) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

                if (protocol.hasDataFor(protocol.position) && !protocol.spawnStatus
                        && protocol.position == protocol.verifiedPosition) {
                    protocol.position = protocol.position.clone().add(
                            readMovePacket(event, protocol)
                    );

                    if (SpartanBukkit.packetsEnabled(protocol)) {
                        Shared.teleport(new PlayerTeleportEvent(
                                player,
                                protocol.lastTeleport,
                                protocol.position)
                        );
                    }
                }
            }
        };
        protocolManager.addPacketListener(serverSideListener);

        Set<PacketType> packetTypes = new HashSet<>();
        packetTypes.add(PacketType.Play.Client.POSITION);
        packetTypes.add(PacketType.Play.Client.POSITION_LOOK);
        packetTypes.add(PacketType.Play.Client.LOOK);

        for (DeprecateTypes type : DeprecateTypes.values()) {
            for (PacketType packetType : PacketType.values()) {
                if (packetType.name().equals(type.name())) {
                    packetTypes.add(packetType);
                    break;
                }
            }
        }

        for (PacketType packetType : packetTypes) {
            PacketListener clientSideListener = new PacketAdapter(Register.plugin, ListenerPriority.LOWEST, packetType) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    moving(event);
                }
            };
            protocolManager.addPacketListener(clientSideListener);
        }
    }

    private static void moving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();

        if (packet.getType().equals(PacketType.Play.Client.POSITION)
                || packet.getType().equals(PacketType.Play.Client.POSITION_LOOK)
                || packet.getType().equals(PacketType.Play.Client.LOOK)) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
            Location to = readMovePacket(event, protocol);
            protocol.verifiedPosition = to;
            double xz = to.getX() + to.getZ();

            if (xz == 17) {
                if (protocol.trueOrFalse(protocol.spawnStatus)) {
                    return;
                }
            } else if (xz == 0) {
                if (protocol.trueOrFalse(protocol.spawnStatus)) {
                    return;
                }
            } else if (protocol.trueOrFalse(protocol.spawnStatus)) {
                protocol.spawnStatus = false;
                protocol.position = to;
                return;
            }
            // Register.plugin.getLogger().info(to.getX() + " " + to.getY() + " " + to.getZ());
            importRotation(protocol, to);
            Location from = protocol.position == null ? to : protocol.position;
            importRotation(protocol, from);
            boolean ground = packet.getBooleans().read(0);
            protocol.onGround = ground;
            protocol.position = to;

            if (SpartanBukkit.packetsEnabled(protocol)) {
                Shared.move(new PlayerMoveEvent(
                        player, from, to
                ));
            }
        } else if (Objects.equals(packet.getType().name(), DeprecateTypes.GROUND.name())
                || Objects.equals(packet.getType().name(), DeprecateTypes.FLYING.name())) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
            boolean ground = packet.getBooleans().read(0);
            protocol.onGround = ground;

            if (SpartanBukkit.packetsEnabled(protocol)) {
                Shared.stay(new PlayerStayEvent(ground, player));
            }
        }
    }

    private static Location readMovePacket(PacketEvent event, SpartanProtocol protocol) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();

        if (event.getPacket().getType().equals(PacketType.Play.Client.LOOK)
                || event.getPacket().getType().equals(PacketType.Play.Client.POSITION_LOOK)) {
            protocol.lastRotation.setYaw(packet.getFloat().read(0));
            protocol.lastRotation.setPitch(packet.getFloat().read(1));
        }
        if (event.getPacket().getType().equals(PacketType.Play.Client.LOOK)) {
            return protocol.position;
        } else {
            return new Location(
                    player.getWorld(),
                    packet.getDoubles().read(0),
                    packet.getDoubles().read(1),
                    packet.getDoubles().read(2)
            );
        }
    }

    private static void importRotation(SpartanProtocol protocol, Location l) {
        l.setYaw(protocol.lastRotation.getYaw());
        l.setPitch(protocol.lastRotation.getPitch());
    }

}