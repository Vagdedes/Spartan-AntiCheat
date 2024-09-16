package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion.MotionVector;
import com.vagdedes.spartan.abstraction.event.CPlayerVelocityEvent;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.MCClient;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_Movement;
import com.vagdedes.spartan.listeners.bukkit.Event_Velocity;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.protocol.ProtocolTools;
import com.vagdedes.spartan.utils.minecraft.protocol.SuperPositionPacket;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class Packet_Movement extends PacketAdapter {

    public Packet_Movement() {
        super(
                Register.plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.GROUND
        );
    }

    public static void receivePacket(PacketEvent event) {
        long time = System.currentTimeMillis();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(event.getPlayer());

        if (protocol.spartanPlayer.bedrockPlayer) {
            return;
        }
        PacketContainer p = event.getPacket();
        PacketType t = p.getType();

        boolean legacy = t.equals(PacketType.Play.Client.FLYING)
                || (t.equals(PacketType.Play.Client.POSITION)
                && protocol.isSameWithHash(ProtocolTools.readLocation(event)));
        Event_Movement.tick(new PlayerTickEvent(protocol, legacy).build());

        if (t.equals(PacketType.Play.Client.POSITION)
                || t.equals(PacketType.Play.Client.POSITION_LOOK)) {
            Location l = ProtocolTools.readLocation(event);
            protocol.pushHashPosition(l);
            protocol.addRawLocation(l);
        }

        /*
        protocol.player.sendMessage(t.toString() + " " + ProtocolTools.readLocation(event).getX() + " " + ProtocolTools.readLocation(event).getZ());
        Register.plugin.getLogger().info(t.toString() + " " + ProtocolTools.readLocation(event).getX() + " " + ProtocolTools.readLocation(event).getZ());
        */

        if (ProtocolTools.invalidTeleport(ProtocolTools.readLocation(event))) {
            return;
        }
        if (t.equals(PacketType.Play.Client.POSITION)
                || t.equals(PacketType.Play.Client.POSITION_LOOK)) {
            if (protocol.claimedVelocity != null && RayUtils.scaleVal(
                    ProtocolTools.readLocation(event).getY() - protocol.getLocation().getY(), 3)
                    == RayUtils.scaleVal(protocol.claimedVelocity.getVelocity().getY(), 3)) {
                Event_Velocity.claim(new CPlayerVelocityEvent(protocol, protocol.claimedVelocity));
                protocol.claimedVelocity = null;
            }
            if (protocol.teleported) {
                protocol.setLocation(ProtocolTools.readLocation(event));
                protocol.teleported = false;
                return;
            }
        }
        if (System.currentTimeMillis() - protocol.mcClient.lastPacketTime > 4) {
            protocol.mcClient.verifiedPacket++;
        }
        protocol.mcClient.lastPacketTime = time;
        movePacket(event, protocol);

        if (protocol.simulationFlag) {
            Location to = protocol.getLocation();
            Location from = protocol.spartanPlayer.movement.getEventFromLocation().getBukkitLocation();
            double dx = RayUtils.scaleVal(to.getX() - from.getX(), 6),
                    dy = RayUtils.scaleVal(to.getY() - from.getY(), 6),
                    dz = RayUtils.scaleVal(to.getZ() - from.getZ(), 6);
            MCClient c = protocol.mcClient;
            MotionVector v = c.getNext(dx, dy, dz);
            Location finalLocation = protocol.simulationStartPoint.clone().add(v.x, v.y, v.z);

            if (RayUtils.isSaveToTeleport(protocol.player, finalLocation)
                    && RayUtils.isSaveToTeleport(protocol.player, finalLocation.clone().add(0, 1, 0))) {
                if (protocol.simulationDelayPerTP > 0) {
                    protocol.simulationDelayPerTP--;
                } else {
                    protocol.spartanPlayer.teleport(new SpartanLocation(finalLocation));
                    protocol.simulationStartPoint = finalLocation.clone();
                    protocol.simulationDelayPerTP = 2;
                }
            } else {
                protocol.endSimulationFlag();
            }
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        receivePacket(event);
    }

    private static void movePacket(PacketEvent event, SpartanProtocol protocol) {
        PacketContainer packet = event.getPacket();
        PacketType type = event.getPacket().getType();
        protocol.setOnGround(ProtocolTools.onGroundPacketLevel(event));

        /*if (!protocol.teleportEngine.isEmpty()) {
            for (TeleportData teleportData : protocol.teleportEngine) {
                Location teleportLocation = protocol.getLocation().clone().add(teleportData.getLocation());
                Location teleportSilentLocation = teleportData.getLocation();
                int hl = protocol.getLocation().toVector().hashCode();
                int ht = teleportLocation.toVector().hashCode();
                int hSt = teleportSilentLocation.toVector().hashCode();
                if (hl == ht) {
                    protocol.setLocation(teleportLocation);
                    protocol.teleportEngine.remove(teleportData);
                } else if (hl == hSt) {
                    protocol.setLocation(teleportSilentLocation);
                    protocol.teleportEngine.remove(teleportData);
                }
            }
        }*/

        if (type.equals(PacketType.Play.Client.LOOK)) {
            Player player = event.getPlayer();
            Location from = protocol.getLocation().clone();
            protocol.getLocation().setYaw(packet.getFloat().read(0));
            protocol.getLocation().setPitch(packet.getFloat().read(1));
            protocol.getLocation().setWorld(player.getWorld());

            if (!from.getWorld().equals(protocol.getLocation().getWorld())
                    || ProtocolTools.invalidTeleport(protocol.getLocation())) {
                protocol.spartanPlayer.resetData(true);
            } else if (!protocol.loaded) {
                protocol.loaded = true;
            } else {
                PlayerMoveEvent bukkitEvent = new PlayerMoveEvent(
                        player,
                        from,
                        protocol.getLocation()
                );
                bukkitEvent.setCancelled(event.isCancelled());
                Event_Movement.event(bukkitEvent, true);
            }
        } else if (type.equals(PacketType.Play.Client.POSITION)
                || type.equals(PacketType.Play.Client.POSITION_LOOK)) {
            Player player = event.getPlayer();
            Location from = protocol.getLocation().clone(),
                    location = ProtocolTools.readLocation(event);
            protocol.getLocation().setX(location.getX());
            protocol.getLocation().setY(location.getY());
            protocol.getLocation().setZ(location.getZ());
            protocol.getLocation().setWorld(player.getWorld());

            if (type.equals(PacketType.Play.Client.POSITION_LOOK)) {
                protocol.getLocation().setYaw(packet.getFloat().read(0));
                protocol.getLocation().setPitch(packet.getFloat().read(1));
            }
            if (!from.getWorld().equals(location.getWorld())
                    || ProtocolTools.invalidTeleport(location)) {
                protocol.spartanPlayer.resetData(true);
            } else if (!protocol.loaded) {
                protocol.loaded = true;
            } else {
                PlayerMoveEvent bukkitEvent = new PlayerMoveEvent(
                        player,
                        from,
                        protocol.getLocation()
                );
                bukkitEvent.setCancelled(event.isCancelled());
                Event_Movement.event(bukkitEvent, true);
            }
        } else {
            superPosition(new SuperPositionPacket(protocol, event));
        }
    }

    private static void superPosition(SuperPositionPacket packet) {
        SpartanPlayer p = SpartanBukkit.getProtocol(packet.getProtocol().player).spartanPlayer;
        boolean cancelled = packet.getEvent().isCancelled();
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, packet);
    }

}