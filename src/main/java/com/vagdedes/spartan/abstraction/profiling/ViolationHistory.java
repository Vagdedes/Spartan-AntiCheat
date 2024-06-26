package com.vagdedes.spartan.abstraction.profiling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class ViolationHistory {

    private final Collection<PlayerViolation> memory;

    ViolationHistory() {
        this.memory = new CopyOnWriteArrayList<>();
    }

    public void clear() {
        memory.clear();
    }

    public void store(PlayerViolation playerViolation) {
        memory.add(playerViolation);
    }

    public int getCount() {
        return memory.size();
    }

    public Collection<PlayerViolation> getCollection() {
        return new ArrayList<>(memory);
    }

}
