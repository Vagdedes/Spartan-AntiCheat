package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.*;
import com.vagdedes.spartan.listeners.protocol.async.transactions.SendPingPong;
import com.vagdedes.spartan.listeners.protocol.async.transactions.SendTransaction;
import com.vagdedes.spartan.listeners.protocol.move.Move;
import com.vagdedes.spartan.listeners.protocol.move.Move_Deprecated;

public class BackgroundProtocolLib {

    static void run() {
        SpartanBukkit.packetsThread.execute(() -> {
            ProtocolManager p = ProtocolLibrary.getProtocolManager();
            p.addPacketListener(new Join());
            p.addPacketListener(new EntityAction());
            p.addPacketListener(new Velocity());
            p.addPacketListener(new Attack());

            if (SpartanBukkit.movementPacketsForcedState) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                    p.addPacketListener(new Move());
                    p.addPacketListener(new SendPingPong());
                } else {
                    p.addPacketListener(new Move_Deprecated());
                    p.addPacketListener(new SendTransaction());
                }
            }
            p.addPacketListener(new ServerPosition());
            //p.addPacketListener(new Debug());
        });
    }

}
