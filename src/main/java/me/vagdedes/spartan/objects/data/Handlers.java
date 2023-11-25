package me.vagdedes.spartan.objects.data;

import me.vagdedes.spartan.functionality.important.MultiVersion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Handlers {

    public enum HandlerFamily {
        Velocity, Constant, Environment, Motion, Part
    }

    public enum HandlerType {
        SensitiveBlockBreak(HandlerFamily.Environment), TowerBuilding(HandlerFamily.Environment),

        Velocity(HandlerFamily.Velocity), BouncingBlocks(HandlerFamily.Velocity), Floor(HandlerFamily.Velocity),
        Piston(HandlerFamily.Velocity), WaterElevator(HandlerFamily.Velocity), Damage(HandlerFamily.Velocity),
        Explosion(HandlerFamily.Velocity), ExtremeCollision(HandlerFamily.Velocity),

        ElytraWear(HandlerFamily.Part), Vehicle(HandlerFamily.Part), GameMode(HandlerFamily.Part), Teleport(HandlerFamily.Part),

        ElytraUse(HandlerFamily.Motion), Trident(HandlerFamily.Motion);

        // Separator

        public final HandlerFamily family;

        HandlerType(HandlerFamily family) {
            this.family = family;
        }
    }

    private final Map<HandlerType, Long> enable, disable;
    private final Map<HandlerType, Map<String, Long>> child;

    public Handlers() {
        int length = HandlerType.values().length;
        this.enable = (MultiVersion.folia ? new LinkedHashMap<>(length) : new ConcurrentHashMap<>(length));
        this.disable = (MultiVersion.folia ? new LinkedHashMap<>(length) : new ConcurrentHashMap<>(length));
        this.child = (MultiVersion.folia ? new LinkedHashMap<>(length) : new ConcurrentHashMap<>(length));
    }

    public void add(HandlerType handlerType) {
        if (!isDisabled(handlerType)) {
            enable.put(handlerType, -1L);
        }
    }

    public void add(HandlerType handlerType, int ticks) {
        if (!isDisabled(handlerType)) {
            enable.put(handlerType, Math.max(enable.getOrDefault(handlerType, 0L), System.currentTimeMillis() + (ticks * 50L)));
        }
    }

    public void add(HandlerType handlerType, String key) {
        if (!isDisabled(handlerType)) {
            Map<String, Long> map = child.get(handlerType);

            if (map == null) {
                map = new ConcurrentHashMap<>();
                child.put(handlerType, map);
            }
            map.put(key, -1L);
        }
    }

    public void add(HandlerType handlerType, String key, int ticks) {
        if (!isDisabled(handlerType)) {
            Map<String, Long> map = child.get(handlerType);

            if (map == null) {
                map = new ConcurrentHashMap<>();
                child.put(handlerType, map);
            }
            map.put(key, Math.max(map.getOrDefault(key, 0L), System.currentTimeMillis() + (ticks * 50L)));
        }
    }

    public void disable(HandlerType handlerType, int ticks) {
        disable.put(handlerType, Math.max(disable.getOrDefault(handlerType, 0L), System.currentTimeMillis() + (ticks * 50L)));
    }

    public void removeMany(HandlerFamily handlerFamily) {
        for (HandlerType handlerType : HandlerType.values()) {
            if (handlerType.family == handlerFamily) {
                remove(handlerType);
            }
        }
    }

    public void remove(HandlerType handlerType) {
        enable.remove(handlerType);
        child.remove(handlerType);
    }

    public void remove(HandlerType handlerType, String key) {
        Map<String, Long> map = child.get(handlerType);

        if (map != null
                && map.remove(key) != null
                && map.isEmpty()) {
            child.remove(handlerType);
        }
    }

    private boolean has(long time) {
        return time == -1L || time >= System.currentTimeMillis();
    }

    public boolean has() {
        if (!enable.isEmpty()) {
            for (Long time : enable.values()) {
                if (has(time)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean has(HandlerType handlerType) {
        Long time = enable.get(handlerType);
        return time != null && has(time);
    }

    public boolean has(HandlerType handlerType, String key) {
        Map<String, Long> map = child.get(handlerType);

        if (map != null) {
            Long time = map.get(key);
            return time != null && (time == -1L || time >= System.currentTimeMillis());
        }
        return false;
    }

    public boolean isDisabled(HandlerType handlerType) {
        Long time = disable.get(handlerType);
        return time != null && time >= System.currentTimeMillis();
    }

    public int getRemainingTicks(HandlerType handlerType) {
        Long time = enable.get(handlerType);
        return time == null ? 0 : (int) Math.max(0, (time - System.currentTimeMillis()) / 50L);
    }

    public int getRemainingTicks(HandlerType handlerType, String key) {
        Map<String, Long> map = child.get(handlerType);

        if (map != null) {
            Long time = map.get(key);
            return time == null ? 0 : (int) Math.max(0, (time - System.currentTimeMillis()) / 50L);
        }
        return 0;
    }

    public long getRemainingTime(HandlerType handlerType) {
        Long time = enable.get(handlerType);
        return time == null ? 0L : Math.max(0L, time - System.currentTimeMillis());
    }

    public long getRemainingTime(HandlerType handlerType, String key) {
        Map<String, Long> map = child.get(handlerType);

        if (map != null) {
            Long time = map.get(key);
            return time == null ? 0L : Math.max(0, time - System.currentTimeMillis());
        }
        return 0;
    }
}
