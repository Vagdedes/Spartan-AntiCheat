package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Join extends PacketAdapter implements Listener {

    public Join() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.LOGIN);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
        protocol.spawnStatus = true;
        protocol.position = new Location(event.getPlayer().getWorld(), 0, 0, 0);

        if (SpartanBukkit.packetsEnabled(protocol)) {
            Shared.join(new PlayerJoinEvent(
                    player,
                    (String) null
            ));
        }
    }


}
