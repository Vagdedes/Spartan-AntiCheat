package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.data.EncirclementData;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.event.SuperPositionPacketEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.concurrent.SpartanScheduler;
import com.vagdedes.spartan.functionality.concurrent.Threads;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.MovementEvent;
import com.vagdedes.spartan.utils.minecraft.protocol.ProtocolTools;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Set;

public class MovementListener extends PacketAdapter {

    private static final Threads.ThreadPool movementThread = new Threads.ThreadPool(1L);

    public MovementListener() {
        super(
                Register.plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Server.POSITION,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17))
                        ? PacketType.Play.Client.GROUND
                        : PacketType.Play.Client.FLYING
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol p = SpartanBukkit.getProtocol(player);
        Location tp = ProtocolTools.readLocation(event);
        if (!event.getPacket().getFloat().getValues().isEmpty()) {
            tp.setYaw(event.getPacket().getFloat().read(0));
            tp.setPitch(event.getPacket().getFloat().read(1));
        }

        if (tp == null) {
            return;
        }
        Location loc = p.getLocation().clone();
        Location result = tp.clone();

        Set<tpFlags> flags = ProtocolTools.getTeleportFlags(event);
        for (tpFlags flag : flags) {
            if (flag.equals(tpFlags.X))
                result.setX(loc.getX() + tp.getX());
            if (flag.equals(tpFlags.Y))
                result.setY(loc.getY() + tp.getY());
            if (flag.equals(tpFlags.Z))
                result.setZ(loc.getZ() + tp.getZ());
        }
        p.setTeleport(result.clone());
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol p = SpartanBukkit.getProtocol(player);
        PacketContainer packet = event.getPacket();
        // Always loaded if you use this listener
        if (p.spartan.isBedrockPlayer()) {
            return;
        }
        Location l = p.getLocation();
        p.setFromLocation(l.clone());
        boolean onGround = ProtocolTools.onGroundPacketLevel(event);
        p.setOnGround(onGround);
        Location r = ProtocolTools.readLocation(event);

        if (r == null) {
            return;
        }

        Location c = ProtocolTools.readLocation(event);
        if (ProtocolTools.hasRotation(event.getPacket().getType())) {
            c.setYaw(event.getPacket().getFloat().read(0));
            c.setPitch(event.getPacket().getFloat().read(1));
        }
        double[] v = new double[]{c.getX(), c.getY(), c.getZ(), c.getYaw(), c.getPitch()};
        for (Double check : v) {
            if (check.isNaN() || check.isInfinite() || Math.abs(check) > 3E8) {
                SpartanBukkit.getProtocol(player)
                        .spartan.punishments.kick(Bukkit.getConsoleSender(), "Invalid packet");
                return;
            }
        }
        boolean legacy = ProtocolTools.isFlying(event, l, r);
        PlayerTickEvent tickEvent = new PlayerTickEvent(p, legacy, onGround).build();
        MovementEvent.tick(tickEvent);
        if (tickEvent.getDelay() > 65) {
            p.lagTick = tickEvent.getDelay();
        } else if (tickEvent.getDelay() > 10 && p.lagTick != 0)
            p.lagTick = 0;

        if (p.isDesync() && tickEvent.getDelay() > 40 && tickEvent.getDelay() < 60) {
            p.transactionVl += (p.isBlatantDesync() ? 2 : 1);
            if (p.transactionVl > 40) {
                Bukkit.getScheduler().runTask(Register.plugin,
                        () -> player.teleport(p.getLocation()));
                AwarenessNotifications.optionallySend(player.getName()
                        + " moves faster than the transaction response ("
                        + tickEvent.getDelay() + "ms > "
                        + (System.currentTimeMillis() - p.transactionTime) + "ms).");
                event.setCancelled(true);
                return;
            }
        } else if (p.transactionVl > 0)
            p.transactionVl -= 2;

        if (p.transactionBoot)
            PacketLatencyHandler.startChecking(p);

        if (!ProtocolLib.getWorld(player).getName().equals(p.fromWorld)) {
            p.fromWorld = ProtocolLib.getWorld(player).getName();
            p.setLocation(ProtocolTools.getLoadLocation(player));
        }
        if (!legacy) {
            boolean hasPosition = ProtocolTools.hasPosition(packet.getType());
            boolean hasRotation = ProtocolTools.hasRotation(packet.getType());
            if (hasPosition) {
                p.addRawLocation(r);
                p.pushHashPosition(r);
            }
            if (ProtocolTools.isLoadLocation(p.getLocation())) {
                p.setLocation(r);
                p.setFromLocation(r);
            } else {
                if (hasPosition) {
                    if (p.getTeleport() != null) { // From
                        Location to = p.getTeleport();

                        // Let's check guys with bad internet
                        if (to.getX() == r.getX() && to.getY() == r.getY() && to.getZ() == r.getZ()) {
                            p.setLocation(p.getTeleport().clone());
                            p.setFromLocation(p.getTeleport().clone());
                            p.setTeleport(null);
                        } else {
                            if (p.spartan.trackers.has(PlayerTrackers.TrackerType.VEHICLE, "enter")) return;
                            for (Entity entity : p.spartan.getNearbyEntities(5))
                                if (entity instanceof Boat) return;

                            // Force packet stop if your packet are shit
                            event.setCancelled(true);
                            Bukkit.getScheduler().runTask(Register.plugin, () -> {
                                if (p.getTeleport() != null) {
                                    Location build = p.getTeleport().clone();
                                    build.setYaw(p.getLocation().getYaw());
                                    build.setPitch(p.getLocation().getPitch());
                                    p.bukkit().teleport(build);
                                }
                            });
                            return;
                        }
                    } else { // Build
                        l.setX(r.getX());
                        l.setY(r.getY());
                        l.setZ(r.getZ());
                    }
                }
                if (hasRotation) {
                    l.setYaw(packet.getFloat().read(0));
                    l.setPitch(packet.getFloat().read(1));
                }
            }
            if (p.useItemPacket) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                    if (!player.getInventory().getItemInMainHand().getType().isEdible()
                            && !player.getInventory().getItemInOffHand().getType().isEdible())
                        p.useItemPacket = false;
                } else {
                    if (!player.getInventory().getItemInHand().getType().isEdible()) p.useItemPacket = false;
                }
            }
            SpartanScheduler.run(() -> {
                PlayerMoveEvent moveEvent = new PlayerMoveEvent(
                                player,
                                p.getFromLocation(),
                                p.getLocation()
                );
                moveEvent.setCancelled(event.isCancelled());
                p.setEncirclementData(new EncirclementData(p));
                MovementEvent.event(moveEvent, true);
            });
            if (p.flyingTicks > 0) p.flyingTicks--;

            //player.sendMessage("event: " + moveEvent.getTo().toVector());
        } else {
            superPosition(new SuperPositionPacketEvent(p, event));
        }
    }

    private static void superPosition(SuperPositionPacketEvent packet) {
        boolean cancelled = packet.packetEvent.isCancelled();
        packet.protocol.profile().getRunner(Enums.HackType.Exploits).handle(cancelled, packet);
        packet.protocol.profile().getRunner(Enums.HackType.IrregularMovements).handle(cancelled, packet);
    }

    public enum tpFlags {
        X, Y, Z, Y_ROT, X_ROT
    }
}