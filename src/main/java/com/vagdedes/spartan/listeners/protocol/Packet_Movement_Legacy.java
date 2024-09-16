package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;

public class Packet_Movement_Legacy extends PacketAdapter {

    public Packet_Movement_Legacy() {
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
    public void onPacketReceiving (PacketEvent event){
        Packet_Movement.receivePacket(event);
    }

}