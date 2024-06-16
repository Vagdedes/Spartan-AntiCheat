package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesEnum;

public class EntityAction extends PacketAdapter {

    public EntityAction() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.ENTITY_ACTION
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(event.getPlayer());
        String typeString = event.getPacket().getModifier().getValues().get(1).toString();
        AbilitiesEnum type = getEnum(typeString);

        if (typeString != null) {
            if (type == AbilitiesEnum.PRESS_SHIFT_KEY) {
                protocol.setSneaking(true);
            }
            if (type == AbilitiesEnum.RELEASE_SHIFT_KEY) {
                protocol.setSneaking(false);
            }
            if (type == AbilitiesEnum.START_SPRINTING) {
                protocol.setSprinting(true);
            }
            if (type == AbilitiesEnum.STOP_SNEAKING) {
                protocol.setSprinting(false);
            }
        }

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
