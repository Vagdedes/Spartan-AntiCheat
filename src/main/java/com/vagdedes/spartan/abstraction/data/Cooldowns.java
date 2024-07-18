package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.functionality.server.TPS;

import java.util.Map;

public class Cooldowns {

    private final Map<String, Long> storage;

    public Cooldowns(Map<String, Long> map) {
        this.storage = map;
    }

    public int get(String name) {
        Long object = storage.get(name);

        if (object == null) {
            return 0;
        } else {
            object -= TPS.tick();
            return object < 0L ? 0 : object.intValue();
        }
    }

    public boolean canDo(String name) {
        return get(name) == 0;
    }

    public void add(String name, int ticks) {
        storage.put(name, TPS.tick() + ticks);
    }

    public void add(String[] names, int ticks) {
        for (String name : names) {
            storage.put(name, TPS.tick() + ticks);
        }
    }

    public void remove(String name) {
        storage.remove(name);
    }

    public void remove(String[] names) {
        for (String name : names) {
            storage.remove(name);
        }
    }

}
