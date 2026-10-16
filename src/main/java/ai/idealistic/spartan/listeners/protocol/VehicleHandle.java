package ai.idealistic.spartan.listeners.protocol;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.functionality.concurrent.CheckThread;
import ai.idealistic.spartan.functionality.server.Config;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.listeners.bukkit.MovementEvent;
import ai.idealistic.spartan.listeners.bukkit.VehicleEvent;
import ai.idealistic.spartan.utils.minecraft.protocol.ProtocolTools;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class VehicleHandle extends PacketAdapter {

    public VehicleHandle() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.STEER_VEHICLE,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(player);

        if (!Config.settings.getBoolean("Important.bedrock_on_protocollib")
                && protocol.isBedrockPlayer()) {
            return;
        }
        CheckThread.run(protocol, () -> {
            if (ProtocolTools.hasPosition(event.getPacket().getType()) && protocol.entityHandle) {
                protocol.entityHandle = false;
            }
            for (Entity entity : protocol.getNearbyEntities(4.5)) {
                if (!entity.getUniqueId().equals(protocol.getUUID())) {
                    if (ProtocolTools.hasPosition(event.getPacket().getType()) && protocol.entityHandle) {
                        protocol.timerBalancer.addBalance(50);
                        VehicleExitEvent bukkitEvent = new VehicleExitEvent(null, protocol.bukkit());
                        bukkitEvent.setCancelled(event.isCancelled());
                        VehicleEvent.exit(bukkitEvent);
                    } else if (event.getPacket().getType().equals(PacketType.Play.Client.STEER_VEHICLE) && !protocol.entityHandle) {
                        protocol.timerBalancer.addBalance(50);
                        VehicleEnterEvent bukkitEvent = new VehicleEnterEvent(null, protocol.bukkit());
                        bukkitEvent.setCancelled(event.isCancelled());
                        VehicleEvent.enter(bukkitEvent);
                        protocol.entityHandle = true;
                    }
                    break;
                }
            }
            if (event.getPacket().getType().equals(PacketType.Play.Client.STEER_VEHICLE)
                    && protocol.getVehicle() != null) {
                Entity vehicle = protocol.getVehicle();
                Location location = vehicle.getLocation();
                protocol.setLocation(location);
                MovementEvent.event(new PlayerMoveEvent(player, protocol.getFromLocation(), protocol.getLocation()), true);
            }
        });
    }
}