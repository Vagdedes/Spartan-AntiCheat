package com.vagdedes.spartan.abstraction.event;

import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;

public class PlayerTransactionEvent {

    public final PlayerProtocol protocol;
    public final long time, delay;

    public PlayerTransactionEvent(PlayerProtocol protocol) {
        this.time = System.currentTimeMillis();
        this.protocol = protocol;
        this.delay = protocol.transactionPing;
    }
}
