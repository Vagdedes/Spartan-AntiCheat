package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.functionality.server.TPS;

public class CancelCause {

    private String reason, pointer;
    private long expiration;

    CancelCause(Compatibility.CompatibilityType compatibilityType) {
        this.reason = compatibilityType.toString();
        this.pointer = " ";
        this.expiration = 0L;
    }

    CancelCause(String reason, String pointer, int ticks) {
        this.reason = reason;
        this.pointer = pointer;
        this.expiration = ticks == 0
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + (ticks * TPS.tickTime);
    }

    void merge(CancelCause other) {
        this.reason = other.reason;
        this.pointer = other.pointer;
        this.expiration = other.expiration;
    }

    boolean hasExpired() {
        return expiration < System.currentTimeMillis();
    }

    public String getReason() {
        return reason;
    }

    boolean pointerMatches(String info) {
        return this.pointer == null || info.contains(this.pointer);
    }
}
