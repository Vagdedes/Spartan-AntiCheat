package com.vagdedes.spartan.abstraction.event;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;

public class PlayerTransactionEvent {

    public final SpartanProtocol protocol;
    public final long time, delay;
    public PlayerTransactionEvent(SpartanProtocol protocol) {
        this.time = System.currentTimeMillis();
        this.protocol = protocol;
        this.delay = protocol.transactionPing;
    }
}
