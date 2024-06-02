package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.functionality.server.TPS;

public class CancelCause {

    private String reason, pointer;
    private long expiration;

    CancelCause(String reason, String pointer, int ticks) {
        this.reason = reason;
        this.pointer = pointer;
        this.expiration = ticks == 0 ? 0L : System.currentTimeMillis() + (ticks * TPS.tickTime);
    }

    void merge(CancelCause other) {
        this.reason = other.reason;
        this.pointer = other.pointer;
        this.expiration = other.expiration;
    }

    private boolean hasExpiration() {
        return expiration != 0L;
    }

    boolean hasExpired() {
        return hasExpiration() && expiration < System.currentTimeMillis();
    }

    public String getReason() {
        return reason;
    }

    boolean pointerMatches(String info) {
        return this.pointer == null || info.contains(this.pointer);
    }
}
