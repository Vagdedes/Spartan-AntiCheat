package com.vagdedes.spartan.objects.system;

public class CancelCause {

    private String reason, pointer;
    private long expiration;

    CancelCause(String reason, String pointer, int ticks) {
        this.reason = reason;
        this.pointer = pointer;
        this.expiration = ticks == 0 ? 0L : System.currentTimeMillis() + (ticks * 50L);
    }

    void merge(CancelCause other) {
        if (!this.hasExpiration()) {
            if (!other.hasExpiration()) {
                this.reason = other.reason;
                this.pointer = other.pointer;
            }
        } else if (!other.hasExpiration() || other.expiration > this.expiration) {
            this.reason = other.reason;
            this.pointer = other.pointer;
            this.expiration = other.expiration;
        }
    }

    boolean hasExpiration() {
        return expiration != 0L;
    }

    boolean hasExpired() {
        return hasExpiration() && expiration < System.currentTimeMillis();
    }

    // Separator

    public String getReason() {
        return reason;
    }

    public boolean pointerMatches(String info) {
        return this.pointer != null && info.contains(this.pointer);
    }
}
