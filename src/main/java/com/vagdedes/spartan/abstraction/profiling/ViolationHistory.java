package com.vagdedes.spartan.abstraction.profiling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ViolationHistory {

    private final Collection<PlayerViolation> memory;
    private final boolean sync;

    public ViolationHistory(Collection<PlayerViolation> list, boolean sync) { // Constructor
        this.memory = sync ? Collections.synchronizedCollection(list) : list;
        this.sync = sync;
    }

    ViolationHistory() { // Local
        this(new ArrayList<>(), true);
    }

    public void clear() {
        if (sync) {
            synchronized (memory) {
                memory.clear();
            }
        } else {
            memory.clear();
        }
    }

    public void store(PlayerViolation playerViolation) {
        if (sync) {
            synchronized (memory) {
                memory.add(playerViolation);
            }
        } else {
            memory.add(playerViolation);
        }
    }

    public int getCount() {
        return memory.size();
    }

    public Collection<PlayerViolation> getCollection() {
        if (sync) {
            synchronized (memory) {
                if (!memory.isEmpty()) {
                    Collection<PlayerViolation> collection = new ArrayList<>(memory.size());

                    for (PlayerViolation playerViolation : memory) {
                        if (playerViolation.isDetectionEnabled()) {
                            collection.add(playerViolation);
                        }
                    }
                    return collection;
                } else {
                    return new ArrayList<>(0);
                }
            }
        } else {
            if (!memory.isEmpty()) {
                Collection<PlayerViolation> collection = new ArrayList<>(memory.size());

                for (PlayerViolation playerViolation : memory) {
                    if (playerViolation.isDetectionEnabled()) {
                        collection.add(playerViolation);
                    }
                }
                return collection;
            } else {
                return new ArrayList<>(0);
            }
        }
    }

    public Collection<PlayerViolation> getRawCollection() {
        if (sync) {
            synchronized (memory) {
                return new ArrayList<>(memory);
            }
        } else {
            return memory;
        }
    }

}
