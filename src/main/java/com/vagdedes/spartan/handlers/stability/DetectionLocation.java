package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DetectionLocation {

    private static final Map<UUID, SpartanLocation>
            loc = Collections.synchronizedMap(new LinkedHashMap<>(Config.getMaxPlayers()));

    public static void run(SpartanPlayer player) {
        synchronized (loc) {
            SpartanLocation old = loc.get(player.uuid);

            if (old != null) {
                SpartanLocation location = player.getLocation();
                double distance = location.distance(old);

                if (distance > (MoveUtils.chunk * 2)
                        && !player.getProfile().isHacker()) {
                    loc.put(player.uuid, location);
                } else if (distance >= 1.0 && !player.isDetected(true)) {
                    loc.put(player.uuid, location);
                }
            } else {
                loc.put(player.uuid, player.getLocation());
            }
        }
    }

    public static void remove(SpartanPlayer player) {
        synchronized (loc) {
            loc.remove(player.uuid);
        }
    }

    public static SpartanLocation get(SpartanPlayer player, boolean replaceNull) {
        synchronized (loc) {
            SpartanLocation location = loc.get(player.uuid);
            return location != null
                    ? location
                    : (replaceNull ? player.getEventFromLocation() : null);
        }
    }

    public static void update(SpartanPlayer player, SpartanLocation location, boolean force) {
        if (force || !player.isDetected(true)) {
            int minHeight = BlockUtils.getMinHeight(player.getWorld());

            if (location.getBlockY() < minHeight) {
                location.setY(minHeight);
            }
            synchronized (loc) {
                loc.put(player.uuid, location);
            }
        }
    }
}
