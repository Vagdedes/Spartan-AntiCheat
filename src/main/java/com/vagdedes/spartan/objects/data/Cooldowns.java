package com.vagdedes.spartan.objects.data;

import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Cooldowns {

    private final Map<String, Long> storage;
    private final SpartanPlayer player;

    public Cooldowns(SpartanPlayer player) {
        this.storage = Collections.synchronizedMap(new LinkedHashMap<>());
        this.player = player;
    }

    public void clear() {
        synchronized (storage) {
            storage.clear();
        }
    }

    public int get(String name) {
        Long object;

        synchronized (storage) {
            object = storage.get(name);
        }
        if (object == null) {
            return 0;
        } else {
            if (player == null) {
                object -= System.currentTimeMillis();
            } else {
                object -= TPS.getTick(player);
            }
            return object < 0L ? 0 : object.intValue();
        }
    }

    public boolean canDo(String name) {
        return get(name) == 0;
    }

    public void add(String name, int ticks) {
        if (player == null) {
            synchronized (storage) {
                storage.put(name, System.currentTimeMillis() + (ticks * TPS.tickTime));
            }
        } else {
            synchronized (storage) {
                storage.put(name, TPS.getTick(player) + ticks);
            }
        }
    }

    public void add(String[] names, int ticks) {
        if (player == null) {
            synchronized (storage) {
                for (String name : names) {
                    storage.put(name, System.currentTimeMillis() + (ticks * TPS.tickTime));
                }
            }
        } else {
            synchronized (storage) {
                for (String name : names) {
                    storage.put(name, TPS.getTick(player) + ticks);
                }
            }
        }
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
}
