package com.vagdedes.spartan.compatibility.manual.world;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.wasteofplastic.acidisland.events.AcidEvent;
import com.wasteofplastic.acidisland.events.AcidRainEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AcidRain implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void AcidRainEvent(AcidRainEvent e) {
        if (Compatibility.CompatibilityType.AcidRain.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer().getUniqueId());
            p.getHandlers().add(Handlers.HandlerType.Floor, 10);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void AcidEvent(AcidEvent e) {
        if (Compatibility.CompatibilityType.AcidRain.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer().getUniqueId());
            p.getHandlers().add(Handlers.HandlerType.Floor, 10);
        }
    }
}
