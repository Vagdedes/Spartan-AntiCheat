package com.vagdedes.spartan.abstraction.event;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;

public class PlayerTickEvent {

    private final SpartanProtocol protocol;
    private final long time;
    private final boolean legacy;
    private long delay;

    public PlayerTickEvent(SpartanProtocol protocol, boolean legacy) {
        this.time = System.currentTimeMillis();
        this.protocol = protocol;
        this.delay = -1;
        this.legacy = legacy;
    }

    public PlayerTickEvent build() {
        this.delay = this.time - this.protocol.tickTime;

        if (this.legacy) {
            // Via Version :cry:
            if (this.delay > 1020 && this.delay < 1060) {
                this.delay -= 1000;
            } else if (this.delay > 950) {
                this.delay -= 950;
            }
        }
        this.protocol.tickTime = this.time;
        return this;
    }

    public SpartanProtocol getProtocol() {
        return protocol;
    }

    public long getTime() {
        return this.time;
    }

    public long getDelay() {
        return this.delay;
    }
}
