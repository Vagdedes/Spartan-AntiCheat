package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;

public class ProtocolLib {

    public static void run() {
        if (SpartanBukkit.packetsForcedState) {
            BackgroundProtocolLib.run();
        }
    }
}
