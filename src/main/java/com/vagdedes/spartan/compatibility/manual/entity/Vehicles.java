package com.vagdedes.spartan.compatibility.manual.entity;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.OverflowMap;
import es.pollitoyeye.vehicles.enums.VehicleType;
import es.pollitoyeye.vehicles.events.VehicleEnterEvent;
import es.pollitoyeye.vehicles.events.VehicleExitEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.concurrent.ConcurrentHashMap;

public class Vehicles implements Listener {

    private static final Cooldowns cooldowns = new Cooldowns(
            new OverflowMap<>(new ConcurrentHashMap<>(), 512)
    );
    private static final String key = Compatibility.CompatibilityType.VEHICLES + "=compatibility=";
    public static final String
            DRILL = "drill",
            TRACTOR = "tractor";

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Enter(VehicleEnterEvent e) {
        if (Compatibility.CompatibilityType.VEHICLES.isFunctional()) {
            Player n = e.getPlayer();

            if (ProtocolLib.isTemporary(n)) {
                return;
            }
            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

            if (p != null) {
                VehicleType vehicleType = e.getVehicleType();

                if (vehicleType == VehicleType.DRILL) {
                    add(p, Vehicles.DRILL);
                } else if (vehicleType == VehicleType.TRACTOR) {
                    add(p, Vehicles.TRACTOR);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Exit(VehicleExitEvent e) {
        if (Compatibility.CompatibilityType.VEHICLES.isFunctional()) {
            Player n = e.getPlayer();

            if (ProtocolLib.isTemporary(n)) {
                return;
            }
            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

            if (p != null) {
                p.buffer.clear(key);
            }
        }
    }

    private static void add(SpartanPlayer p, String type) {
        cooldowns.add(p.uuid + "=" + key + type, 20);
    }

    public static boolean has(SpartanPlayer p, String type) {
        return !cooldowns.canDo(p.uuid + "=" + key + type);
    }

    public static boolean has(SpartanPlayer p, String[] types) {
        for (String type : types) {
            if (has(p, type)) {
                return true;
            }
        }
        return false;
    }
}
