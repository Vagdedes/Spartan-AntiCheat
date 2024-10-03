package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.listeners.protocol.*;
import com.vagdedes.spartan.listeners.protocol.standalone.Packet_BlockPlaceP;
import com.vagdedes.spartan.listeners.protocol.standalone.Packet_EntityAction;
import com.vagdedes.spartan.listeners.protocol.standalone.Packet_Join;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;

public class BackgroundProtocolLib {

    static void run() {
        ProtocolManager p = ProtocolLibrary.getProtocolManager();
        p.addPacketListener(new Packet_Join());
        p.addPacketListener(new Packet_EntityAction());
        p.addPacketListener(new Packet_Velocity());
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            p.addPacketListener(new Packet_Combat());
            p.addPacketListener(new Packet_Movement());
        } else {
            p.addPacketListener(new Packet_Combat_Legacy());
            p.addPacketListener(new Packet_Movement_Legacy());
        }
        p.addPacketListener(new Packet_Teleport());
        p.addPacketListener(new Packet_Vehicle());
        p.addPacketListener(new Packet_Death());
        p.addPacketListener(new Packet_BlockPlaceP());
        p.addPacketListener(new Packet_BlockPlace());

        if (PlayerUtils.trident) {
            p.addPacketListener(new Packet_Trident());
        }
        if (false) {
            p.addPacketListener(new Packet_Debug());
        }
    }

}
