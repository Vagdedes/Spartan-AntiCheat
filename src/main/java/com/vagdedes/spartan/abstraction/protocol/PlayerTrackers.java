package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTrackers {

    public enum TrackerFamily {
        VELOCITY, MOTION
    }

    public enum TrackerType {
        BOUNCING_BLOCKS(TrackerFamily.VELOCITY), PISTON(TrackerFamily.VELOCITY),
        BUBBLE_WATER(TrackerFamily.VELOCITY), TRIDENT(TrackerFamily.VELOCITY),
        DAMAGE(TrackerFamily.VELOCITY), TELEPORT(TrackerFamily.VELOCITY),

        VEHICLE(TrackerFamily.MOTION);

        public final TrackerFamily family;

        TrackerType(TrackerFamily family) {
            this.family = family;
        }
    }

    private final Map<TrackerType, Long> enable, disable;
    private final Map<TrackerType, Map<String, Long>> child;

    public PlayerTrackers() {
        int length = TrackerType.values().length;
        this.enable = new ConcurrentHashMap<>(length);
        this.disable = new ConcurrentHashMap<>(length);
        this.child = new ConcurrentHashMap<>(length);
    }

    public void add(TrackerType trackerType) {
        if (!isDisabled(trackerType)) {
            enable.put(trackerType, -1L);
        }
    }

    public void add(TrackerType trackerType, int ticks) {
        if (!isDisabled(trackerType)) {
            enable.put(
                    trackerType,
                    Math.max(
                            enable.getOrDefault(trackerType, 0L),
                            System.currentTimeMillis() + (ticks * TPS.tickTime)
                    )
            );
        }
    }

    public void add(TrackerType trackerType, String key) {
        if (!isDisabled(trackerType)) {
            child.computeIfAbsent(trackerType, k -> new LinkedHashMap<>()).put(key, -1L);
        }
    }

    public void add(TrackerType trackerType, String key, int ticks) {
        if (!isDisabled(trackerType)) {
            Map<String, Long> map = child.computeIfAbsent(trackerType, k -> new LinkedHashMap<>());
            map.put(
                    key,
                    Math.max(
                            map.getOrDefault(key, 0L),
                            System.currentTimeMillis() + (ticks * TPS.tickTime)
                    )
            );
        }
    }

    public void disable(TrackerType trackerType, int ticks) {
        disable.put(trackerType,
                Math.max(
                        disable.getOrDefault(trackerType, 0L),
                        System.currentTimeMillis() + (ticks * TPS.tickTime)
                )
        );
    }

    public void removeMany(TrackerFamily trackerFamily) {
        for (TrackerType trackerType : TrackerType.values()) {
            if (trackerType.family == trackerFamily) {
                remove(trackerType);
            }
        }
    }

    public void remove(TrackerType trackerType) {
        enable.remove(trackerType);
        child.remove(trackerType);
    }

    public void remove(TrackerType trackerType, String key) {
        Map<String, Long> map = child.get(trackerType);

        if (map != null
                && map.remove(key) != null
                && map.isEmpty()) {
            child.remove(trackerType);
        }
    }

    private boolean has(long time) {
        return time == -1L || time > System.currentTimeMillis();
    }

    public boolean has() {
        if (!enable.isEmpty()) {
            for (long time : enable.values()) {
                if (has(time)) {
                    return true;
                }
            }
        }
        if (!child.isEmpty()) {
            for (Map<String, Long> sub : child.values()) {
                for (long time : sub.values()) {
                    if (has(time)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean has(TrackerFamily trackerFamily) {
        for (TrackerType trackerType : TrackerType.values()) {
            if (trackerType.family == trackerFamily && this.has(trackerType)) {
                return true;
            }
        }
        return false;
    }

    public boolean has(TrackerType trackerType) {
        Long time = enable.get(trackerType);

        if (time != null && has(time)) {
            return true;
        } else {
            Map<String, Long> map = child.get(trackerType);
            return map != null && map.values().stream().anyMatch(this::has);
        }
    }

    public boolean has(TrackerType trackerType, String key) {
        Map<String, Long> map = child.get(trackerType);

        if (map != null) {
            Long time = map.get(key);

            return time != null && has(time);
        } else {
            return false;
        }
    }

    public boolean isDisabled(TrackerType trackerType) {
        Long time = disable.get(trackerType);
        return time != null && time >= System.currentTimeMillis();
    }

    public int getRemainingTicks(TrackerType trackerType, String key) {
        Map<String, Long> map = child.get(trackerType);
        return map == null ? 0 : getRemainingTicks(map.get(key));
    }

    public int getRemainingTicks(TrackerType trackerType) {
        return getRemainingTicks(enable.get(trackerType));
    }

    private int getRemainingTicks(Long time) {
        if (time == null) {
            return 0;
        } else {
            time -= System.currentTimeMillis();
            return time < 0L ? 0 : AlgebraUtils.integerCeil(time / (double) TPS.tickTime);
        }
    }

}
