package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.vagdedes.spartan.Register;

public class Packet_BlockPlace extends PacketAdapter {

    public Packet_BlockPlace() {
        super(
                        Register.plugin,
                        ListenerPriority.HIGHEST,
                        PacketType.Play.Client.USE_ITEM
        );
        // Method: Event_BlockPlace.event()
    }

}