package me.vagdedes.spartan.objects.data;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Buffer {

    // Orientation

    private final Map<String, BufferChild> hm;

    public enum BufferType {
        Default, Combat
    }

    private static class BufferChild {

        private int count;
        private long ticks;
        private final BufferType type;

        private BufferChild(int count, BufferType type) {
            this.count = count;
            this.ticks = 0L;
            this.type = type;
        }
    }

    public Buffer(boolean async) {
        this.hm = (async && !MultiVersion.folia ? new ConcurrentHashMap<>() : new LinkedHashMap<>());
    }

    // Runnable

    public void run(SpartanPlayer p) {
        if (!hm.isEmpty()) {
            boolean staticTruth = TestServer.isIdentified() || Config.settings.getBoolean("Detections.allow_cancelled_hit_checking");

            for (BufferChild bufferChild : hm.values()) {
                if (bufferChild.type == BufferType.Combat && bufferChild.ticks > 0L) {
                    if (staticTruth
                            || Damage.getLastDealt(p) <= 55L
                            || PlayerData.isInActiveEntityCombat(p)
                            || CombatUtils.getEnemiesNumber(p, CombatUtils.maxHitDistance, true, 1) > 0) {
                        bufferChild.ticks -= 1L;
                    } else {
                        for (Enums.HackType hackType : Enums.HackType.values()) {
                            if (hackType.getCheck().getCheckType() == Enums.CheckType.COMBAT
                                    && p.getViolations(hackType).getLastViolationTime(true) <= 55L) {
                                bufferChild.ticks -= 1L;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    // Implementation

    public void clear() {
        hm.clear();
    }

    public boolean hasTimer(String name) {
        return hm.get(name) != null;
    }

    public int get(String name, int def) {
        BufferChild object = hm.get(name);
        return object != null ? object.count : def;
    }

    public int get(String name) {
        return get(name, 0);
    }

    public void set(BufferType type, String name, int amount) {
        BufferChild obj = hm.get(name);

        if (obj != null) {
            obj.count = amount;
        } else {
            hm.put(name, new BufferChild(amount, type));
        }
    }

    public void set(String name, int amount) {
        set(BufferType.Default, name, amount);
    }

    public int increase(BufferType type, String name, int amount) {
        BufferChild obj = hm.get(name);

        if (obj != null) {
            return obj.count += amount;
        }
        hm.put(name, new BufferChild(amount, type));
        return amount;
    }

    public int increase(String name, int amount) {
        return increase(BufferType.Default, name, amount);
    }

    public int decrease(BufferType type, String name, int amount) {
        BufferChild obj = hm.get(name);

        if (obj != null) {
            return obj.count = Math.max(obj.count - amount, 0);
        }
        hm.put(name, new BufferChild(amount, type));
        return amount;
    }

    public int decrease(String name, int amount) {
        return decrease(BufferType.Default, name, amount);
    }

    public int start(BufferType type, String name, int time) {
        if (time <= 0) {
            return 0;
        }
        BufferChild obj = hm.get(name);

        if (obj == null) {
            obj = new BufferChild(0, type);
            hm.put(name, obj);
        }
        long ms = 0L; // Remove 5 milliseconds for inaccuracy
        boolean combat = type == BufferType.Combat;

        if (combat ? (obj.ticks == ms) : (obj.ticks - 5L) <= (ms = System.currentTimeMillis())) {
            obj.ticks = combat ? (CombatUtils.newPvPMechanicsEnabled() ? time * 2L : time) : (ms + (time * 50L));

            if (obj.count >= 0) {
                return obj.count = 1;
            }
        }
        obj.count += 1;
        return obj.count;
    }

    public int start(String name, int time) {
        return start(BufferType.Default, name, time);
    }

    public int getRemainingTicks(String name) {
        BufferChild object = hm.get(name);
        return object == null ? 0 : object.type == BufferType.Combat ? (int) object.ticks :
                (int) Math.max((object.ticks - System.currentTimeMillis()) / 50L, 0);
    }

    public void setRemainingTicks(String name, int ticks) {
        BufferChild object = hm.get(name);

        if (object != null) {
            object.ticks = object.type == BufferType.Combat ? ticks : System.currentTimeMillis() + (50L * ticks);
        }
    }

    public boolean canDo(String name) {
        return get(name) >= 0;
    }

    public void remove(String name) {
        hm.remove(name);
    }

    public void remove(String[] names) {
        for (String name : names) {
            remove(name);
        }
    }

    public void clear(String[] ignore) {
        if (hm.size() > 0) {
            List<String> internal = new ArrayList<>();

            for (String name : hm.keySet()) {
                if (!StringUtils.stringContainsPartOfArray(ignore, name)) {
                    internal.add(name);
                }
            }
            remove(internal.toArray(new String[0]));
        }
    }

    public void clear(String s) {
        if (hm.size() > 0) {
            List<String> internal = new ArrayList<>();

            for (String name : hm.keySet()) {
                if (name.contains(s)) {
                    internal.add(name);
                }
            }
            remove(internal.toArray(new String[0]));
        }
    }
}
