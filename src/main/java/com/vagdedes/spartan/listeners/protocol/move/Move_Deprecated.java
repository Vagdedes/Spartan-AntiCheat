package com.vagdedes.spartan.listeners.protocol.move;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;

public class Move_Deprecated extends PacketAdapter {

    public Move_Deprecated() {
        super(
                        Register.plugin,
                        ListenerPriority.LOWEST,
                        PacketType.Play.Client.POSITION,
                        PacketType.Play.Client.POSITION_LOOK,
                        PacketType.Play.Client.LOOK,
                        PacketType.Play.Client.FLYING
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        BackgroundMove.run(event);
    }
}