package ai.idealistic.spartan.listeners.protocol;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.abstraction.data.EnvironmentData;
import ai.idealistic.spartan.abstraction.event.PlayerTickEvent;
import ai.idealistic.spartan.abstraction.event.SuperPositionPacketEvent;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.compatibility.necessary.protocollib.BackPlib;
import ai.idealistic.spartan.compatibility.necessary.protocollib.ProtocolLib;
import ai.idealistic.spartan.functionality.concurrent.CheckThread;
import ai.idealistic.spartan.functionality.moderation.AwarenessNotifications;
import ai.idealistic.spartan.functionality.server.Config;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.listeners.bukkit.MovementEvent;
import ai.idealistic.spartan.utils.minecraft.protocol.ProtocolTools;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Set;

public class MovementListener extends PacketAdapter {

    public MovementListener() {
        super(
                Register.plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Server.POSITION,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                        ? PacketType.Play.Client.GROUND
                        : PacketType.Play.Client.FLYING
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol p = PluginBase.getProtocol(player);
        Location tp = ProtocolTools.readLocation(event);

        if (tp == null) {
            return;
        }
        if (event.getPacket().getFloat().size() > 0) {
            tp.setYaw(BackPlib.getSafeFloat(event.getPacket(), 0));
            tp.setPitch(BackPlib.getSafeFloat(event.getPacket(), 1));
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
        p.setTeleportTick(true);
        p.setTeleport(result.clone());
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol p = PluginBase.getProtocol(player);
        PacketContainer packet = event.getPacket();
        // Always loaded if you use this listener

        if (!Config.settings.getBoolean("Important.bedrock_on_protocollib")
                && p.isBedrockPlayer()) {
            return;
        }
        CheckThread.run(p, () -> {
            Location l = p.getLocation();
            p.setFrom(l.clone());
            boolean onGround = ProtocolTools.onGroundPacketLevel(event);
            p.setOnGround(onGround);
            Location r = ProtocolTools.readLocation(event);

            if (r == null) {
                return;
            }
            Location c = ProtocolTools.readLocation(event);

            if (c == null) {
                return;
            }
            if (ProtocolTools.hasRotation(event.getPacket().getType())) {
                c.setYaw(BackPlib.getSafeFloat(event.getPacket(), 0));
                c.setPitch(BackPlib.getSafeFloat(event.getPacket(), 1));
            }
            double[] v = new double[]{c.getX(), c.getY(), c.getZ(), c.getYaw(), c.getPitch()};

            for (Double check : v) {
                if (check.isNaN()
                        || check.isInfinite()
                        || Math.abs(check) > 3E8) {
                    PluginBase.getProtocol(player).punishments.kick(Bukkit.getConsoleSender(), "Invalid packet");
                    return;
                }
            }
            boolean legacy = ProtocolTools.isFlying(event, l, r);

            if (c.toVector().distance(p.getFrom().toVector()) > 1e-5
                    && !p.isTeleportTick()) {
                PlayerTickEvent tickEvent = new PlayerTickEvent(p, legacy, onGround).build();
                MovementEvent.tick(tickEvent);

                if (tickEvent.getDelay() > 65) {
                    p.lagTick = tickEvent.getDelay();
                } else if (tickEvent.getDelay() > 10 && p.lagTick != 0)
                    p.lagTick = 0;

                if (p.isDesync()
                        && tickEvent.getDelay() > 40
                        && tickEvent.getDelay() < 60) {
                    p.transactionVl += (p.isBlatantDesync() ? 2 : 1);

                    if (p.transactionVl > 40
                            && !p.isAFK()) {
                        AwarenessNotifications.optionallySend(player.getName()
                                + " moves faster than the transaction response ("
                                + tickEvent.getDelay() + "ms > "
                                + (System.currentTimeMillis() - p.transactionTime) + "ms).");
                        //p.teleport(p.getLocation());
                        //event.setCancelled(true);
                        //return;
                    }
                } else if (p.transactionVl > 0) {
                    p.transactionVl -= 2;
                }
            }
            if (p.transactionBoot) {
                PacketLatencyHandler.startChecking(p);
            }
            if (!ProtocolLib.getWorld(player).getName().equals(p.fromWorld)) {
                p.fromWorld = ProtocolLib.getWorld(player).getName();
                p.setLocation(ProtocolTools.getLoadLocation(player));
            }
            if (!legacy) {
                boolean hasPosition = ProtocolTools.hasPosition(packet.getType());
                boolean hasRotation = ProtocolTools.hasRotation(packet.getType());

                if (hasPosition) {
                    p.addRawLocation(r);
                }
                if (ProtocolTools.isLoadLocation(p.getLocation())) {
                    p.setLocation(r);
                    p.setFrom(r);
                } else {
                    if (hasPosition) {
                        Location to = p.getTeleport();

                        if (to != null) { // From
                            // Let's check guys with bad internet
                            if (to.getX() == r.getX()
                                    && to.getY() == r.getY()
                                    && to.getZ() == r.getZ()) {
                                p.setLocation(to.clone());
                                p.setFrom(to.clone());
                                p.setTeleport(null);
                            } else {
                                if (false) return; // todo vehicle enter
                                for (Entity entity : p.getNearbyEntities(5)) {
                                    if (entity instanceof Boat) {
                                        return;
                                    }
                                }
                                // Force packet stop if your packet are shit
                                event.setCancelled(true);
                                Location teleport = p.getTeleport();

                                if (teleport != null) {
                                    Location build = teleport.clone();
                                    build.setYaw(p.getLocation().getYaw());
                                    build.setPitch(p.getLocation().getPitch());
                                    p.teleport(build);
                                }
                                return;
                            }
                        } else { // Build
                            l.setX(r.getX());
                            l.setY(r.getY());
                            l.setZ(r.getZ());
                        }
                    }
                    if (hasRotation) {
                        l.setYaw(BackPlib.getSafeFloat(packet, 0));
                        l.setPitch(BackPlib.getSafeFloat(packet, 1));
                    }
                }
                if (p.useItemPacket) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                        if (!p.getInventory().getItemInMainHand().getType().isEdible()
                                && !p.getInventory().getItemInOffHand().getType().isEdible()) {
                            p.useItemPacket = false;
                        }
                    } else {
                        if (!p.getInventory().getItemInHand().getType().isEdible()) {
                            p.useItemPacket = false;
                        }
                    }
                    if (player.getFoodLevel() == 20 || p.useItemPacketReset) {
                        p.useItemPacket = false;
                    }
                }
                PlayerMoveEvent moveEvent = new PlayerMoveEvent(
                        player,
                        p.getFromLocation(),
                        p.getLocation()
                );
                moveEvent.setCancelled(event.isCancelled());
                p.setEnvironment(new EnvironmentData(p));
                MovementEvent.event(moveEvent, true);

                if (p.flyingTicks > 0) {
                    p.flyingTicks--;
                }
                p.setTeleportTick(false);
            } else {
                p.executeRunners(
                        event.isCancelled(),
                        new SuperPositionPacketEvent(p, event)
                );
            }
        });
    }

    public enum tpFlags {
        X, Y, Z, Y_ROT, X_ROT
    }

}