package com.vagdedes.spartan.objects.data;

import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Handlers {

    public enum HandlerFamily {
        Velocity, Constant, Environment, Motion, Part
    }

    public enum HandlerType {
        SensitiveBlockBreak(HandlerFamily.Environment), TowerBuilding(HandlerFamily.Environment),
        BridgeBuilding(HandlerFamily.Environment),

        Velocity(HandlerFamily.Velocity), BouncingBlocks(HandlerFamily.Velocity), Floor(HandlerFamily.Velocity),
        Piston(HandlerFamily.Velocity), WaterElevator(HandlerFamily.Velocity), Damage(HandlerFamily.Velocity),
        Explosion(HandlerFamily.Velocity), ExtremeCollision(HandlerFamily.Velocity),

        ElytraWear(HandlerFamily.Part), Vehicle(HandlerFamily.Part), GameMode(HandlerFamily.Part),

        ElytraUse(HandlerFamily.Motion), Trident(HandlerFamily.Motion);

        // Separator

        public final HandlerFamily family;

        HandlerType(HandlerFamily family) {
            this.family = family;
        }
    }

    private final Map<HandlerType, Long> enable, disable;
    private final Map<HandlerType, Map<String, Long>> child;
    private final SpartanPlayer player;

    public Handlers(SpartanPlayer player) {
        int length = HandlerType.values().length;
        this.player = player;
        this.enable = Collections.synchronizedMap(new LinkedHashMap<>(length));
        this.disable = Collections.synchronizedMap(new LinkedHashMap<>(length));
        this.child = Collections.synchronizedMap(new LinkedHashMap<>(length));
    }

    public void add(HandlerType handlerType) {
        if (!isDisabled(handlerType)) {
            synchronized (enable) {
                enable.put(handlerType, -1L);
            }
        }
    }

    public void add(HandlerType handlerType, int ticks) {
        if (!isDisabled(handlerType)) {
            synchronized (enable) {
                enable.put(
                        handlerType,
                        Math.max(
                                enable.getOrDefault(handlerType, 0L),
                                TPS.getTick(player) + ticks
                        )
                );
            }
        }
    }

    public void add(HandlerType handlerType, String key) {
        if (!isDisabled(handlerType)) {
            synchronized (child) {
                child.computeIfAbsent(handlerType, k -> new LinkedHashMap<>()).put(key, -1L);
            }
        }
    }

    public void add(HandlerType handlerType, String key, int ticks) {
        if (!isDisabled(handlerType)) {
            synchronized (child) {
                Map<String, Long> map = child.computeIfAbsent(handlerType, k -> new LinkedHashMap<>());
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

    public void disable(HandlerType handlerType, int ticks) {
        synchronized (disable) {
            disable.put(handlerType,
                    Math.max(
                            disable.getOrDefault(handlerType, 0L),
                            TPS.getTick(player) + ticks
                    )
            );
        }
    }

    public void removeMany(HandlerFamily handlerFamily) {
        for (HandlerType handlerType : HandlerType.values()) {
            if (handlerType.family == handlerFamily) {
                remove(handlerType);
            }
        }
    }

    public void remove(HandlerType handlerType) {
        synchronized (enable) {
            synchronized (child) {
                enable.remove(handlerType);
                child.remove(handlerType);
            }
        }
    }

    public void remove(HandlerType handlerType, String key) {
        synchronized (child) {
            Map<String, Long> map = child.get(handlerType);

            if (map != null
                    && map.remove(key) != null
                    && map.isEmpty()) {
                child.remove(handlerType);
            }
        }
    }

    private boolean has(long ticks) {
        return ticks == -1L || ticks > TPS.getTick(player);
    }

    public boolean has() {
        if (!enable.isEmpty()) {
            synchronized (enable) {
                for (Long ticks : enable.values()) {
                    if (has(ticks)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean has(HandlerType handlerType) {
        Long ticks;

        synchronized (enable) {
            ticks = enable.get(handlerType);
        }
        return ticks != null && has(ticks);
    }

    public boolean has(HandlerType handlerType, String key) {
        synchronized (child) {
            Map<String, Long> map = child.get(handlerType);

            if (map != null) {
                Long ticks = map.get(key);
                return ticks != null && has(ticks);
            } else {
                return false;
            }
        }
    }

    public boolean isDisabled(HandlerType handlerType) {
        Long ticks;

        synchronized (disable) {
            ticks = disable.get(handlerType);
        }
        return ticks != null && ticks >= TPS.getTick(player);
    }

    public int getRemainingTicks(HandlerType handlerType, String key) {
        synchronized (child) {
            Map<String, Long> map = child.get(handlerType);
            return map == null ? 0 : getRemainingTicks(map.get(key));
        }
    }

    public int getRemainingTicks(HandlerType handlerType) {
        Long ticks;

        synchronized (enable) {
            ticks = enable.get(handlerType);
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
