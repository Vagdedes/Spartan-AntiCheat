package com.vagdedes.spartan.compatibility.manual.entity;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import es.pollitoyeye.vehicles.enums.VehicleType;
import es.pollitoyeye.vehicles.events.VehicleEnterEvent;
import es.pollitoyeye.vehicles.events.VehicleExitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class Vehicles implements Listener {

    private static final String key = Compatibility.CompatibilityType.Vehicles + "=compatibility=";

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Enter(VehicleEnterEvent e) {
        if (Compatibility.CompatibilityType.Vehicles.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer().getUniqueId());
            VehicleType vehicleType = e.getVehicleType();

            if (vehicleType == VehicleType.DRILL) {
                add(p, "drill");
            } else if (vehicleType == VehicleType.TRACTOR) {
                add(p, "tractor");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Exit(VehicleExitEvent e) {
        if (Compatibility.CompatibilityType.Vehicles.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer().getUniqueId());
            p.getBuffer().clear(key);
        }
    }

    private static void add(SpartanPlayer p, String type) {
        Cooldowns cooldowns = p.getCooldowns();
        cooldowns.add(key + type, 20);
    }

    private static boolean has(Cooldowns cooldowns, String type) {
        return !cooldowns.canDo(key + type);
    }

    public static boolean has(SpartanPlayer p, String type) {
        return has(p.getCooldowns(), type);
    }

    public static boolean has(SpartanPlayer p, String[] types) {
        Cooldowns cooldowns = p.getCooldowns();

        for (String type : types) {
            if (has(cooldowns, type)) {
                return true;
            }
        }
        return false;
    }
}
