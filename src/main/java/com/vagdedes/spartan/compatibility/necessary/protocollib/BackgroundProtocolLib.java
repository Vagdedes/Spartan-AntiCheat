package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.listeners.protocol.*;
import com.vagdedes.spartan.listeners.protocol.combat.CombatListener;
import com.vagdedes.spartan.listeners.protocol.combat.LegacyCombatListener;
import com.vagdedes.spartan.listeners.protocol.standalone.BlockPlaceBalancerListener;
import com.vagdedes.spartan.listeners.protocol.standalone.EntityActionListener;
import com.vagdedes.spartan.listeners.protocol.standalone.JoinListener;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;

public class BackgroundProtocolLib {

    static void run() {
        ProtocolManager p = ProtocolLibrary.getProtocolManager();
        p.addPacketListener(new JoinListener());
        p.addPacketListener(new EntityActionListener());
        p.addPacketListener(new VelocityListener());

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            p.addPacketListener(new CombatListener());
        } else {
            p.addPacketListener(new LegacyCombatListener());
        }
        p.addPacketListener(new MovementListener());
        p.addPacketListener(new TeleportListener());
        p.addPacketListener(new VehicleHandle());
        p.addPacketListener(new DeathListener());
        p.addPacketListener(new BlockPlaceBalancerListener());
        p.addPacketListener(new BlockPlaceListener());
        p.addPacketListener(new ClicksListener());
        p.addPacketListener(new PacketPistonHandle());
        p.addPacketListener(new ExplosionListener());
        p.addPacketListener(new PacketServerBlockHandle());
        p.addPacketListener(new PacketLatencyHandler());
        p.addPacketListener(new AbilitiesListener());
        p.addPacketListener(new UseItemStatusHandle());
        p.addPacketListener(new UseEntityListener());

        if (PlayerUtils.trident) {
            p.addPacketListener(new TridentListener());
        }
        if (false) {
            p.addPacketListener(new PacketDebug());
        }
    }

}
