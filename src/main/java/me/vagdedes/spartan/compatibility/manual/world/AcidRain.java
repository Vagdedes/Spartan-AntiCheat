package me.vagdedes.spartan.compatibility.manual.world;

import com.wasteofplastic.acidisland.events.AcidEvent;
import com.wasteofplastic.acidisland.events.AcidRainEvent;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
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
