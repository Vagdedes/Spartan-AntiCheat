package com.vagdedes.spartan.abstraction.event;

import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;

public class SuperPositionPacketEvent {

    public final PlayerProtocol protocol;
    public final PacketEvent packetEvent;

    public SuperPositionPacketEvent(PlayerProtocol protocol, PacketEvent packetEvent) {
        this.protocol = protocol;
        this.packetEvent = packetEvent;
    }

}
