package com.vagdedes.spartan.objects.system;

import com.vagdedes.spartan.handlers.stability.TPS;

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

    private boolean hasExpired() {
        return hasExpiration() && expiration < System.currentTimeMillis();
    }

    // Separator

    public String getReason() {
        return reason;
    }

    public boolean pointerMatches(String info) {
        return !hasExpired() && (this.pointer == null || info.contains(this.pointer));
    }
}
