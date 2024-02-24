package com.vagdedes.spartan.objects.data;

import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Buffer {

    private final Map<String, BufferChild> storage;
    private final Enums.HackType hackType;
    private final SpartanPlayer player;
    private final boolean combat;

    private static class BufferChild {

        private int count;
        private long ticks;

        private BufferChild(int count) {
            this.count = count;
            this.ticks = 0L;
        }
    }

    public Buffer(SpartanPlayer player, Enums.HackType hackType) {
        this.player = player;
        this.storage = Collections.synchronizedMap(new LinkedHashMap<>());
        this.hackType = hackType;
        this.combat = hackType != null
                && hackType.getCheck().getCheckType() == Enums.CheckType.COMBAT;
    }

    // Runnable

    public void run(SpartanPlayer player) {
        if (combat && !storage.isEmpty()) {
            Iterator<BufferChild> iterator;

            synchronized (storage) {
                iterator = storage.values().iterator();
            }
            while (true) {
                BufferChild bufferChild;

                synchronized (storage) {
                    if (!iterator.hasNext()) {
                        break;
                    }
                    bufferChild = iterator.next();

                    if (bufferChild.ticks == 0L) {
                        iterator.remove();
                        continue;
                    }
                }

                if (player.getEnemiesNumber(CombatUtils.maxHitDistance, true) > 0) {
                    bufferChild.ticks -= 1L;
                } else {
                    for (Enums.HackType hackType : Enums.HackType.values()) {
                        if (hackType.getCheck().getCheckType() == Enums.CheckType.COMBAT
                                && player.getViolations(hackType).isDetected(false)) {
                            bufferChild.ticks -= 1L;
                            break;
                        }
                    }
                }
            }
        }
    }

    // Implementation

    public void clear() {
        synchronized (storage) {
            storage.clear();
        }
    }

    public int get(String name, int def) {
        BufferChild object;

        synchronized (storage) {
            object = storage.get(name);
        }
        return object != null ? object.count : def;
    }

    public int get(String name) {
        return get(name, 0);
    }

    public void set(String name, int amount) {
        BufferChild obj;

        synchronized (storage) {
            obj = storage.get(name);
        }
        if (obj != null) {
            obj.count = amount;
        } else {
            storage.put(name, new BufferChild(amount));
        }
    }

    public int increase(String name, int amount) {
        synchronized (storage) {
            BufferChild obj = storage.get(name);

            if (obj != null) {
                return obj.count += amount;
            } else {
                storage.put(name, new BufferChild(amount));
                return amount;
            }
        }
    }

    public int decrease(String name, int amount) {
        synchronized (storage) {
            BufferChild obj = storage.get(name);

            if (obj != null) {
                return obj.count = Math.max(obj.count - amount, 0);
            } else {
                storage.put(name, new BufferChild(amount));
                return amount;
            }
        }
    }

    public int start(String name, int ticks) {
        BufferChild obj;

        synchronized (storage) {
            obj = storage.computeIfAbsent(name, k -> new BufferChild(0));
        }

        if (this.getRemainingTicks(obj) == 0) {
            obj.ticks = combat
                    ? (CombatUtils.newPvPMechanicsEnabled() ? ticks * 2L : ticks)
                    : TPS.getTick(player) + ticks;

            if (obj.count >= 0) {
                return obj.count = 1;
            }
        }
        obj.count += 1;
        return obj.count;
    }

    private int getRemainingTicks(BufferChild object) {
        if (combat) {
            return (int) object.ticks;
        } else {
            long ticks = object.ticks - TPS.getTick(player);
            return ticks < 0L ? 0 : (int) ticks;
        }
    }

    public int getRemainingTicks(String name) {
        BufferChild object;

        synchronized (storage) {
            object = storage.get(name);
        }
        return object == null ? 0 : this.getRemainingTicks(object);
    }

    public void setRemainingTicks(String name, int ticks) {
        BufferChild object;

        synchronized (storage) {
            object = storage.get(name);
        }
        if (object != null) {
            object.ticks = combat ? ticks : TPS.getTick(player) + ticks;
        }
    }

    public boolean canDo(String name) {
        return get(name) >= 0;
    }

    public void remove(String name) {
        synchronized (storage) {
            storage.remove(name);
        }
    }

    public void remove(String[] names) {
        synchronized (storage) {
            for (String name : names) {
                storage.remove(name);
            }
        }
    }

    public void clear(String s) {
        if (!storage.isEmpty()) {
            synchronized (storage) {
                Iterator<String> iterator = storage.keySet().iterator();

                while (iterator.hasNext()) {
                    if (iterator.next().contains(s)) {
                        iterator.remove();
                    }
                }
            }
        }
    }
}
