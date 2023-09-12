package me.vagdedes.spartan.objects.system.hackPrevention;

import me.vagdedes.spartan.checks.movement.speed.Speed;
import me.vagdedes.spartan.handlers.stability.DetectionLocation;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.interfaces.listeners.EventsHandler7;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;

public class HackPreventionUtils {

    private static final String str = "hack-prevention=";

    public static void run(SpartanPlayer p) {
        boolean teleportCooldown = Moderation.runTeleportCooldown(p); // Always call regardless as it executes algorithms if true

        if ((handleOrganizedPrevention(p) || teleportCooldown) && p.canDo(false)) {
            if (teleportCooldown) {
                p.safeTeleport(DetectionLocation.get(p, true));
            }
        }
    }

    public static void preventVelocity(SpartanPlayer p, int amount) {
        p.getCooldowns().add(str + "velocity", amount);
    }

    public static void preventVelocity(SpartanPlayer p) {
        preventVelocity(p, 2);
    }

    public static void grantLocationAccess(SpartanPlayer p) {
        p.getCooldowns().add(str + "location-access", 2);
    }

    public static boolean handleOrganizedPrevention(SpartanPlayer p) {
        // Attention Velocity Check
        HackPrevention hackPrevention = HackPrevention.specifyCancel(p, EventsHandler7.handledChecks);

        if (hackPrevention != null) {
            SpartanLocation loc = p.getLocation();
            Enums.HackType hackType = hackPrevention.getHackType();
            int teleportCooldown = hackPrevention.getTeleportCooldown();
            SpartanLocation preventionFrom = hackPrevention.getLocation();
            double damage = hackPrevention.getDamage();
            boolean teleported = false;

            if (preventionFrom != null) {
                if (p.getCooldowns().canDo(str + "location-access")) {
                    SpartanLocation fromLocation = DetectionLocation.get(p, false);

                    if (fromLocation != null) {
                        preventionFrom = fromLocation;
                    }
                }
                if (p.safeTeleport(preventionFrom)) { // Attention First
                    teleported = true;

                    if (preventionFrom.distance(loc) >= MoveUtils.chunk || p.getProfile().isSuspectedOrHacker()) {
                        teleportCooldown = Math.max(teleportCooldown, 2);
                    } else {
                        teleportCooldown = Math.max(teleportCooldown, 1);
                    }
                }
            }
            if (hackPrevention.hasGroundTeleport() && p.groundTeleport(true)) {
                teleported = true;
            }
            if (teleported && p.getCooldowns().canDo(str + "velocity")) {
                String verbose = hackPrevention.getInformation();

                if (hackType == Enums.HackType.IrregularMovements
                        || hackType == Enums.HackType.Speed && verbose.contains(Speed.strongPrevention)) {
                    PlayerMoveEvent event = EventsHandler7.getMovementEvent(p);

                    if (event != null) {
                        Location from = event.getFrom();

                        if (AlgebraUtils.getHorizontalDistance(loc, from) <= 0.75) {
                            double vertical = Math.abs(loc.getY() - event.getFrom().getY());

                            if (vertical == 0.0 || vertical >= MoveUtils.nearMaxFallingMotion) {
                                preventVelocity(p, 5);
                                p.getHandlers().disable(Handlers.HandlerType.Velocity, 2);
                                p.setVelocity(loc.getDirection().setY(MoveUtils.gravityAcceleration));
                            }
                        }
                    }
                }
            }

            if (teleportCooldown > 0) {
                HackPrevention.cancel(p, hackType, teleportCooldown);
            }
            if (damage > 0.0) {
                p.applyFallDamage(damage);
            }
            return true;
        }
        return false;
    }
}
