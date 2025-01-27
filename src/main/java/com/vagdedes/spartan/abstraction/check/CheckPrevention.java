package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.Location;

public class CheckPrevention {

    boolean canPrevent;
    private final Location location;
    private final boolean groundTeleport;
    private final double damage;
    private final long expiration;

    CheckPrevention() {
        this.canPrevent = false;
        this.location = null;
        this.groundTeleport = false;
        this.damage = 0.0;
        this.expiration = 0L;
    }

    CheckPrevention(Location location, int cancelTicks, boolean groundTeleport, double damage) {
        this.canPrevent = false;
        this.location = location;
        this.groundTeleport = groundTeleport
                && Config.settings.getBoolean("Detections.ground_teleport_on_detection");
        this.damage = Config.settings.getBoolean("Detections.damage_on_detection")
                ? damage
                : 0.0;
        this.expiration = cancelTicks <= 1
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + (cancelTicks * TPS.tickTime);
    }

    boolean complete() {
        return this.canPrevent && System.currentTimeMillis() <= this.expiration;
    }

    void handle(CheckDetection detection) {
        if (PluginBase.isSynchronised()) {
            detection.prevention(
                    this.location,
                    this.groundTeleport,
                    this.damage
            ).run();
        } else {
            PluginBase.transferTask(
                    detection.protocol,
                    detection.prevention(
                            this.location,
                            this.groundTeleport,
                            this.damage
                    )
            );
        }
    }

}
