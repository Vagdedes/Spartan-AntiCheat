package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DetectionLocation {

    private static final Map<UUID, SpartanLocation>
            loc = new ConcurrentHashMap<>(Config.getMaxPlayers()),
            set = new ConcurrentHashMap<>(Config.getMaxPlayers());

    public static void run(SpartanPlayer p) {
        UUID uuid = p.getUniqueId();
        SpartanLocation old = loc.get(uuid);

        if (old != null) {
            SpartanLocation location = p.getLocation();
            double distance = location.distance(old);

            if (distance > (MoveUtils.chunk * 2)
                    && !p.getProfile().isHacker()) {
                update(p, location, true);
            } else if (distance >= 1.0
                    && (p.getTimer().get("from-location=protection") > 10_000L
                    || p.getLastViolation().getLastViolationTime(false) > 1_000L
                    || !p.hasViolations())) {
                update(p, location, false);
            }
        } else {
            update(p, p.getLocation(), true);
        }
    }

    public static void remove(SpartanPlayer p) {
        UUID uuid = p.getUniqueId();
        loc.remove(uuid);
        set.remove(uuid);
    }

    public static SpartanLocation get(SpartanPlayer p, boolean replaceNull) {
        UUID uuid = p.getUniqueId();
        SpartanLocation location = set.remove(uuid);

        if (location == null) {
            location = loc.get(uuid);
        }
        return location != null ? location : (replaceNull ? p.getEventFromLocation() : null);
    }

    public static void update(SpartanPlayer p, SpartanLocation location, boolean force) {
        if (force || !Moderation.wasDetected(p)) {
            int minHeight = BlockUtils.getMinHeight(p.getWorld());

            if (location.getBlockY() < minHeight) {
                location.setY(minHeight);
            }
            loc.put(p.getUniqueId(), location);
            p.getTimer().set("from-location=protection");
        }
    }

    public static void set(SpartanPlayer p, SpartanLocation location) {
        set.put(p.getUniqueId(), location);
    }
}
