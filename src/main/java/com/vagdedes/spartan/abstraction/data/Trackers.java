package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Trackers {

    public enum TrackerFamily {
        VELOCITY, MOTION
    }

    public enum TrackerType {
        ABSTRACT_VELOCITY(TrackerFamily.VELOCITY), BOUNCING_BLOCKS(TrackerFamily.VELOCITY),
        PISTON(TrackerFamily.VELOCITY), BUBBLE_WATER(TrackerFamily.VELOCITY),
        EXTREME_COLLISION(TrackerFamily.VELOCITY), TRIDENT(TrackerFamily.VELOCITY),
        DAMAGE(TrackerFamily.VELOCITY),

        GAME_MODE(TrackerFamily.MOTION), ELYTRA_USE(TrackerFamily.MOTION),
        FLIGHT(TrackerFamily.MOTION);

        public final TrackerFamily family;

        TrackerType(TrackerFamily family) {
            this.family = family;
        }
    }

    private final Map<TrackerType, Long> enable, disable;
    private final Map<TrackerType, Map<String, Long>> child;
    private final SpartanPlayer player;

    public Trackers(SpartanPlayer player) {
        int length = TrackerType.values().length;
        this.player = player;
        this.enable = Collections.synchronizedMap(new LinkedHashMap<>(length));
        this.disable = Collections.synchronizedMap(new LinkedHashMap<>(length));
        this.child = Collections.synchronizedMap(new LinkedHashMap<>(length));
    }

    public void add(TrackerType trackerType) {
        if (!isDisabled(trackerType)) {
            synchronized (enable) {
                enable.put(trackerType, -1L);
            }
        }
    }

    public void add(TrackerType trackerType, int ticks) {
        if (!isDisabled(trackerType)) {
            synchronized (enable) {
                enable.put(
                        trackerType,
                        Math.max(
                                enable.getOrDefault(trackerType, 0L),
                                TPS.getTick(player) + ticks
                        )
                );
            }
        }
    }

    public void add(TrackerType trackerType, String key) {
        if (!isDisabled(trackerType)) {
            synchronized (child) {
                child.computeIfAbsent(trackerType, k -> new LinkedHashMap<>()).put(key, -1L);
            }
        }
    }

    public void add(TrackerType trackerType, String key, int ticks) {
        if (!isDisabled(trackerType)) {
            synchronized (child) {
                Map<String, Long> map = child.computeIfAbsent(trackerType, k -> new LinkedHashMap<>());
                map.put(
                        key,
                        Math.max(
                                map.getOrDefault(key, 0L),
                                TPS.getTick(player) + ticks
                        )
                );
            }
        }
    }

    public void disable(TrackerType trackerType, int ticks) {
        synchronized (disable) {
            disable.put(trackerType,
                    Math.max(
                            disable.getOrDefault(trackerType, 0L),
                            TPS.getTick(player) + ticks
                    )
            );
        }
    }

    public void removeMany(TrackerFamily trackerFamily) {
        for (TrackerType trackerType : TrackerType.values()) {
            if (trackerType.family == trackerFamily) {
                remove(trackerType);
            }
        }
    }

    public void remove(TrackerType trackerType) {
        synchronized (enable) {
            synchronized (child) {
                enable.remove(trackerType);
                child.remove(trackerType);
            }
        }
    }

    public void remove(TrackerType trackerType, String key) {
        synchronized (child) {
            Map<String, Long> map = child.get(trackerType);

            if (map != null
                    && map.remove(key) != null
                    && map.isEmpty()) {
                child.remove(trackerType);
            }
        }
    }

    private boolean has(long ticks) {
        return ticks == -1L || ticks > TPS.getTick(player);
    }

    public boolean has() {
        if (!enable.isEmpty()) {
            synchronized (enable) {
                for (long ticks : enable.values()) {
                    if (has(ticks)) {
                        return true;
                    }
                }
            }
        }
        if (!child.isEmpty()) {
            synchronized (child) {
                for (Map<String, Long> sub : child.values()) {
                    for (long ticks : sub.values()) {
                        if (has(ticks)) {
                            return true;
                        }
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
        Long ticks;

        synchronized (enable) {
            ticks = enable.get(trackerType);
        }
        return ticks != null && has(ticks);
    }

    public boolean has(TrackerType trackerType, String key) {
        synchronized (child) {
            Map<String, Long> map = child.get(trackerType);

            if (map != null) {
                Long ticks = map.get(key);
                return ticks != null && has(ticks);
            } else {
                return false;
            }
        }
    }

    public boolean isDisabled(TrackerType trackerType) {
        Long ticks;

        synchronized (disable) {
            ticks = disable.get(trackerType);
        }
        return ticks != null && ticks >= TPS.getTick(player);
    }

    public int getRemainingTicks(TrackerType trackerType, String key) {
        synchronized (child) {
            Map<String, Long> map = child.get(trackerType);
            return map == null ? 0 : getRemainingTicks(map.get(key));
        }
    }

    public int getRemainingTicks(TrackerType trackerType) {
        Long ticks;

        synchronized (enable) {
            ticks = enable.get(trackerType);
        }
        return getRemainingTicks(ticks);
    }

    private int getRemainingTicks(Long ticks) {
        if (ticks == null) {
            return 0;
        } else {
            ticks -= TPS.getTick(player);
            return ticks < 0 ? 0 : ticks.intValue();
        }
    }

}
