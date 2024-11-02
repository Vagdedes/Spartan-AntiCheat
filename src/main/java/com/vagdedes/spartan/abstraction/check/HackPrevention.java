package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;

public class HackPrevention {

    boolean canPrevent;
    private final SpartanLocation location;
    private final boolean groundTeleport;
    private final double damage;
    private final long expiration;

    HackPrevention() {
        this.canPrevent = false;
        this.location = null;
        this.groundTeleport = false;
        this.damage = 0.0;
        this.expiration = 0L;
    }

    HackPrevention(SpartanLocation location, int cancelTicks, boolean groundTeleport, double damage) {
        this.canPrevent = false;
        this.location = location;
        this.groundTeleport = groundTeleport;
        this.damage = damage;
        this.expiration = cancelTicks <= 1
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + (cancelTicks * TPS.tickTime);
    }

    boolean complete() {
        return this.canPrevent && System.currentTimeMillis() <= this.expiration;
    }

    void handle(SpartanPlayer player) {
        Runnable runnable = () -> {
            if (this.location != null
                    && player.protocol.packetsEnabled()) {
                player.teleport(this.location);
            }
            if (this.groundTeleport) {
                player.groundTeleport();
            }
            if (this.damage > 0.0
                    && (this.location == null && !this.groundTeleport
                    || Config.settings.getBoolean("Detections.fall_damage_on_teleport"))) {
                player.damage(this.damage);
            }
        };

        if (SpartanBukkit.isSynchronised()) {
            runnable.run();
        } else {
            SpartanBukkit.transferTask(player, runnable);
        }
    }

}
