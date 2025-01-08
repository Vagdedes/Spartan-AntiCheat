package com.vagdedes.spartan.listeners.protocol.standalone;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.concurrent.SpartanScheduler;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;

public class EntityActionListener extends PacketAdapter {

    private enum AbilitiesEnum {
        START_SPRINTING,
        STOP_SPRINTING,
        PRESS_SHIFT_KEY,
        RELEASE_SHIFT_KEY
    }

    public EntityActionListener() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.ENTITY_ACTION
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(event.getPlayer());

        if (protocol.spartan.isBedrockPlayer()) {
            return;
        }
        SpartanScheduler.run(() -> {
            if (event.getPacket().getModifier().getValues().size() > 1) {
                String typeString = event.getPacket().getModifier().getValues().get(1).toString();
                AbilitiesEnum type = getEnum(typeString);

                if (typeString != null) {
                    if (type == AbilitiesEnum.PRESS_SHIFT_KEY) {
                        protocol.sneaking = true;
                    } else if (type == AbilitiesEnum.RELEASE_SHIFT_KEY) {
                        protocol.sneaking = false;
                    } else if (type == AbilitiesEnum.START_SPRINTING) {
                        protocol.sprinting = true;
                    } else if (type == AbilitiesEnum.STOP_SPRINTING) {
                        protocol.sprinting = false;
                    }
                }
            }
        });
    }

    private AbilitiesEnum getEnum(String s) {
        for (AbilitiesEnum type : AbilitiesEnum.values()) {
            if (type.toString().equals(s)) {
                return type;
            }
        }
        return null;
    }

}
