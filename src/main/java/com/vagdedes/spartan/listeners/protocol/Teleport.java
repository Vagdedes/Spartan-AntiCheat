package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class Teleport extends PacketAdapter {

    public Teleport() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.POSITION);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanLocation currentLocation = ProtocolStorage.positionManager.get(player.getUniqueId()),
                newLocation = Move.readMovePacket(event);

        // Check if the worlds are the same before adding the locations
        if (currentLocation != null
                && currentLocation.world.equals(newLocation.world)) {
            Shared.teleport(new PlayerTeleportEvent(
                    player,
                    currentLocation.getBukkitLocation(),
                    newLocation.clone().add(currentLocation).getBukkitLocation()
            ));
        }
    }

}