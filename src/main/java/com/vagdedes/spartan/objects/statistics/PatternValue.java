package com.vagdedes.spartan.objects.statistics;

public class PatternValue {

    public final Number number;
    public final long time;

     PatternValue(Number number) {
        this(number, System.currentTimeMillis());
    }

     PatternValue(Number number, long time) {
        this.number = number;
        this.time = time;
    }
}
