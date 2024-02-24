package com.vagdedes.spartan.objects.system;

import com.vagdedes.spartan.checks.movement.speed.Speed;
import com.vagdedes.spartan.handlers.identifiers.simple.VehicleAccess;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.data.Cooldowns;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

public class HackPrevention {

    final String information;
    final SpartanLocation location;
    final int violation;
    final boolean groundTeleport;
    boolean processed;
    final double damage;
    final long time, tick, expiration;

    private static final Cooldowns cooldowns = new Cooldowns(null);

    // Separator

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose,
                          SpartanLocation location, int cancelTicks, boolean groundTeleport,
                          double damage, int violation) {
        // Object Data
        this.time = System.currentTimeMillis();
        this.tick = TPS.getTick(player);
        this.expiration = cancelTicks == 0 ? 0L : this.tick + cancelTicks;
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
                this.violation = violation;
            }
        } else {
            this.violation = violation;
        }
        player.getViolations(hackType).queue(this, check);
    }

    void handle(SpartanPlayer player, Enums.HackType hackType) {
        SpartanLocation preventionFrom = this.location;
        boolean teleported = preventionFrom != null
                && player.safeTeleport(preventionFrom);

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
