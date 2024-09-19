package com.vagdedes.spartan.abstraction.player;

import com.vagdedes.spartan.functionality.server.TPS;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTrackers {

    public enum TrackerFamily {
        VELOCITY, MOTION
    }

    public enum TrackerType {
        ABSTRACT_VELOCITY(TrackerFamily.VELOCITY), BOUNCING_BLOCKS(TrackerFamily.VELOCITY),
        PISTON(TrackerFamily.VELOCITY), BUBBLE_WATER(TrackerFamily.VELOCITY),
        TRIDENT(TrackerFamily.VELOCITY), DAMAGE(TrackerFamily.VELOCITY),

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
                            TPS.tick() + ticks
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
                            TPS.tick() + ticks
                    )
            );
        }
    }

    public void disable(TrackerType trackerType, int ticks) {
        disable.put(trackerType,
                Math.max(
                        disable.getOrDefault(trackerType, 0L),
                        TPS.tick() + ticks
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

    private boolean has(long ticks) {
        return ticks == -1L || ticks > TPS.tick();
    }

    public boolean has() {
        if (!enable.isEmpty()) {
            for (long ticks : enable.values()) {
                if (has(ticks)) {
                    return true;
                }
            }
        }
        if (!child.isEmpty()) {
            for (Map<String, Long> sub : child.values()) {
                for (long ticks : sub.values()) {
                    if (has(ticks)) {
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
        Long ticks = enable.get(trackerType);
        if (ticks != null && has(ticks)) {
            return true;
        } else {
            Map<String, Long> map = child.get(trackerType);
            return map != null && map.values().stream().anyMatch(this::has);
        }
    }

    public boolean has(TrackerType trackerType, String key) {
        Map<String, Long> map = child.get(trackerType);

        if (map != null) {
            Long ticks = map.get(key);
            return ticks != null && has(ticks);
        } else {
            return false;
        }
    }

    public boolean isDisabled(TrackerType trackerType) {
        Long ticks = disable.get(trackerType);
        return ticks != null && ticks >= TPS.tick();
    }

    public int getRemainingTicks(TrackerType trackerType, String key) {
        Map<String, Long> map = child.get(trackerType);
        return map == null ? 0 : getRemainingTicks(map.get(key));
    }

    public int getRemainingTicks(TrackerType trackerType) {
        return getRemainingTicks(enable.get(trackerType));
    }

    private int getRemainingTicks(Long ticks) {
        if (ticks == null) {
            return 0;
        } else {
            ticks -= TPS.tick();
            return ticks < 0 ? 0 : ticks.intValue();
        }
    }

}
