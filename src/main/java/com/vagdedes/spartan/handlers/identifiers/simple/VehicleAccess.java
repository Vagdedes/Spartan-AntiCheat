package com.vagdedes.spartan.handlers.identifiers.simple;

import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class VehicleAccess {

    private static final int ticks = 7;

    public static void run() {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer p : players) {
                Entity vehicle = p.getVehicle();

                if (vehicle != null && !(vehicle instanceof LeashHitch)) {
                    runEnter(p, vehicle, true);
                }
            }
        }
    }

    public static void runExit(SpartanPlayer p) {
        Entity vehicle = p.getVehicle();

        if (vehicle == null || vehicle instanceof LivingEntity) {
            Handlers handlers = p.getHandlers();
            handlers.add(Handlers.HandlerType.Vehicle, ticks);
            handlers.add(Handlers.HandlerType.Vehicle, "exit", ticks);

            if (handlers.has(Handlers.HandlerType.Vehicle, "non-boat")) {
                handlers.add(Handlers.HandlerType.Vehicle, "non-boat", ticks);
            }
        }
    }

    public static void runEnter(SpartanPlayer p, Entity entity, boolean enter) {
        // Damage, BouncingBlocks, WaterElevator cover vehicles too
        // Piston cover players with distance but not via object
        // Velocity is far too important to be manipulated
        // Simple ones do not need to be manipulated here
        // Liquid is based on the past, so it's not counted here
        Handlers handlers = p.getHandlers();
        handlers.remove(Handlers.HandlerType.ElytraUse);
        handlers.remove(Handlers.HandlerType.Trident);
        handlers.remove(Handlers.HandlerType.ExtremeCollision);
        handlers.remove(Handlers.HandlerType.Floor);

        // Separator

        handlers.add(Handlers.HandlerType.Vehicle);

        if (enter) {
            handlers.add(Handlers.HandlerType.Vehicle, "enter", ticks);
        }
        if (!(entity instanceof Boat)) {
            handlers.add(Handlers.HandlerType.Vehicle, "non-boat");
        }
    }

    public static boolean hasExitCooldown(SpartanPlayer p, Enums.HackType hackType) {
        return p.getHandlers().has(Handlers.HandlerType.Vehicle, "exit")
                && !p.getProfile().isSuspectedOrHacker(hackType);
    }

    public static boolean hasEnterCooldown(SpartanPlayer p) {
        return p.getHandlers().has(Handlers.HandlerType.Vehicle, "enter");
    }
}
