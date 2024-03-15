package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternCache;

import java.util.List;

public class PatternSpecificCache extends PatternCache {

    List<PatternValue> data;

    PatternSpecificCache(List<PatternValue> cached) {
        super();
        this.data = cached;
    }

    void update(List<PatternValue> cached) {
        this.data = cached;
        this.refresh();
    }
}
