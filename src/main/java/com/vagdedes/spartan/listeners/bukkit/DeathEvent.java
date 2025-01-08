package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerDeathEvent e) {
        event(e.getEntity(), false);
    }

    public static void event(Player player, boolean packets) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player, true);
        protocol.timerBalancer.addBalance(50);

        if (protocol.packetsEnabled() == packets) {
            // Detections
            protocol.profile().getRunner(Enums.HackType.AutoRespawn).handle(false, null);
            protocol.profile().getRunner(Enums.HackType.ImpossibleInventory).handle(false, null);

            // Objects
            protocol.spartan.resetCrucialData();
        }
    }

}
