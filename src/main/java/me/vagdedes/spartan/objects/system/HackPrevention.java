package me.vagdedes.spartan.objects.system;

import me.vagdedes.spartan.checks.movement.speed.Speed;
import me.vagdedes.spartan.functionality.protections.Teleport;
import me.vagdedes.spartan.handlers.identifiers.simple.VehicleAccess;
import me.vagdedes.spartan.handlers.stability.DetectionLocation;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class HackPrevention {

    final String information;
    final SpartanLocation location;
    final int violation;
    final boolean groundTeleport;
    boolean processed;
    final double damage;
    final long time, expiration;

    private static final Cooldowns cooldowns = new Cooldowns(false);

    // Separator

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose,
                          SpartanLocation location, int cancelTicks, boolean groundTeleport,
                          double damage, int violation) {
        // Object Data
        this.time = System.currentTimeMillis();
        this.expiration = cancelTicks == 0 ? 0L : this.time + (cancelTicks * 50L);
        this.information = verbose;
        this.location = location;
        this.groundTeleport = groundTeleport;
        this.damage = damage;
        this.processed = false;

        // Redundancy
        Check check = hackType.getCheck();

        if (violation == 1 && check.getCheckType() == Enums.CheckType.MOVEMENT) {
            if (VehicleAccess.hasExitCooldown(player, hackType)) {
                this.violation = 0;
            } else {
                CancelCause silentCause = hackType.getCheck().getSilentCause(player.getUniqueId());

                if (silentCause != null && silentCause.getReason().equals(Teleport.reason)) {
                    this.violation = 0;
                } else {
                    this.violation = violation;
                }
            }
        } else {
            this.violation = violation;
        }
        player.getViolations(hackType).queue(this, check);
    }

    void handle(SpartanPlayer player, Enums.HackType hackType) {
        SpartanLocation preventionFrom = this.location;
        boolean teleported = false;

        if (preventionFrom != null) {
            SpartanLocation fromLocation = DetectionLocation.get(player, false);

            if (fromLocation != null) {
                preventionFrom = fromLocation;
            }
            if (player.safeTeleport(preventionFrom)) { // Attention First
                teleported = true;
            }
        }
        if (this.groundTeleport
                && player.groundTeleport(true)) {
            teleported = true;
        }
        if (teleported) {
            String key = player.getUniqueId() + "velocity";

            if (cooldowns.canDo(key)
                    && (hackType == Enums.HackType.IrregularMovements
                    || hackType == Enums.HackType.Speed && this.information.contains(Speed.strongPrevention))) {
                SpartanLocation to = player.getLocation(),
                        from = player.getEventFromLocation();

                if (AlgebraUtils.getHorizontalDistance(to, from) <= 0.75) {
                    double vertical = Math.abs(to.getY() - from.getY());

                    if (vertical == 0.0 || vertical >= MoveUtils.nearMaxFallingMotion) {
                        cooldowns.add(key, 5);
                        player.getHandlers().disable(Handlers.HandlerType.Velocity, 2);
                        player.setVelocity(to.getDirection().setY(MoveUtils.gravityAcceleration));
                    }
                }
            }
        }

        if (damage > 0.0) {
            player.applyFallDamage(damage);
        }
    }
}
