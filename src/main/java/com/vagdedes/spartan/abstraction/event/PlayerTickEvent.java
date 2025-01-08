package com.vagdedes.spartan.abstraction.event;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import lombok.Getter;

public class PlayerTickEvent {

    public final SpartanProtocol protocol;
    public final long time;
    public final boolean legacy;
    public final boolean onGround;
    @Getter
    private long delay;

    public PlayerTickEvent(SpartanProtocol protocol, boolean legacy, boolean onGround) {
        this.time = System.currentTimeMillis();
        this.protocol = protocol;
        this.delay = -1;
        this.onGround = onGround;
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
}
