package com.vagdedes.spartan.utils.minecraft.protocol;

import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;

public class SuperPositionPacket {

    private final SpartanProtocol protocol;
    private final PacketEvent packetEvent;

    public SuperPositionPacket(SpartanProtocol protocol, PacketEvent packetEvent) {
        this.protocol = protocol;
        this.packetEvent = packetEvent;
    }

    public SpartanProtocol getProtocol() {
        return this.protocol;
    }

    public PacketEvent getEvent() {
        return this.packetEvent;
    }
}
