package com.vagdedes.spartan.abstraction.player;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.event.player.PlayerVelocityEvent;

public class SpartanPlayerVelocity {

    private final SpartanPlayer parent;
    public final long tick;
    public final PlayerVelocityEvent event;
    public final SpartanLocation location;

    SpartanPlayerVelocity(SpartanPlayer player,
                          PlayerVelocityEvent event,
                          SpartanLocation location) {
        this.parent = player;
        this.tick = TPS.getTick(player);
        this.event = event;
        this.location = location.clone();

        if (!event.isCancelled()) {
            player.trackers.add(
                    Trackers.TrackerType.ABSTRACT_VELOCITY,
                    (int) (Math.ceil(event.getVelocity().length()) * TPS.maximum)
            );
        }
    }

    public long ticksPassed() {
        return TPS.getTick(this.parent) - tick;
    }

}
