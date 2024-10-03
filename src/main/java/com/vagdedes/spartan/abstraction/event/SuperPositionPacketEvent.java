package com.vagdedes.spartan.abstraction.event;

import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;

public class SuperPositionPacketEvent {

    public final SpartanProtocol protocol;
    public final PacketEvent packetEvent;

    public SuperPositionPacketEvent(SpartanProtocol protocol, PacketEvent packetEvent) {
        this.protocol = protocol;
        this.packetEvent = packetEvent;
    }

}
