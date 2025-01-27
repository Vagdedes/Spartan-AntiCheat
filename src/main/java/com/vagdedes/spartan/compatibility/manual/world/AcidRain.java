package com.vagdedes.spartan.compatibility.manual.world;

import com.vagdedes.spartan.abstraction.protocol.PlayerBukkit;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.wasteofplastic.acidisland.events.AcidEvent;
import com.wasteofplastic.acidisland.events.AcidRainEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AcidRain implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void AcidRainEvent(AcidRainEvent e) {
        if (Compatibility.CompatibilityType.ACID_RAIN.isFunctional()) {
            PlayerBukkit p = PluginBase.getProtocol(e.getPlayer()).bukkitExtra;

            p.handleReceivedDamage();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void AcidEvent(AcidEvent e) {
        if (Compatibility.CompatibilityType.ACID_RAIN.isFunctional()) {
            PlayerBukkit p = PluginBase.getProtocol(e.getPlayer()).bukkitExtra;

            p.handleReceivedDamage();
        }
    }
}
