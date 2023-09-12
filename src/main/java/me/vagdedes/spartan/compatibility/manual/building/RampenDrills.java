package me.vagdedes.spartan.compatibility.manual.building;

import me.rampen88.drills.events.DrillBreakEvent;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RampenDrills implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(DrillBreakEvent e) {
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.RampenDrills;

        if (compatibilityType.isFunctional()) {
            CheckProtection.evadeCommonFalsePositives(e.getPlayer(), compatibilityType,
                    new Enums.HackType[]{
                            Enums.HackType.FastBreak,
                            Enums.HackType.NoSwing,
                            Enums.HackType.GhostHand,
                    }, 5);
        }
    }
}
