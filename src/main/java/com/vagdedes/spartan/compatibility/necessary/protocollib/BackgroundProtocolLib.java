package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.vagdedes.spartan.listeners.protocol.*;

public class BackgroundProtocolLib {

    static void run() {
        ProtocolManager p = ProtocolLibrary.getProtocolManager();
        p.addPacketListener(new Join());
        p.addPacketListener(new EntityAction());
        p.addPacketListener(new Velocity());
        p.addPacketListener(new Attack());
        Move.registerPacketListeners(p);
    }
}
