package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.functionality.server.TPS;
import lombok.Getter;

public class CheckCancellation {

    @Getter
    private final String reason, pointer;
    private final long expiration;

    CheckCancellation(Compatibility.CompatibilityType compatibilityType) {
        this.reason = compatibilityType.toString();
        this.pointer = null;
        this.expiration = 0L;
    }

    CheckCancellation(String reason, String pointer, int ticks) {
        this.reason = reason;
        this.pointer = pointer;
        this.expiration = ticks == 0
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + (ticks * TPS.tickTime);
    }

    boolean hasExpired() {
        return expiration < System.currentTimeMillis();
    }

    boolean pointerMatches(String info) {
        return this.pointer == null || info.contains(this.pointer);
    }
}
