package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesEnum;

public class EntityAction extends PacketAdapter {

    public EntityAction() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ENTITY_ACTION);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(event.getPlayer());

        if (SpartanBukkit.packetsEnabled(protocol)) {
            String typeString = event.getPacket().getModifier().getValues().get(1).toString();
            AbilitiesEnum type = getEnum(typeString);

            if (type == null) {
                if (AwarenessNotifications.canSend(
                        AwarenessNotifications.uuid,
                        "unknown-entity-action",
                        60)) {
                    AwarenessNotifications.forcefullySend("Unknown Entity Action: " + typeString);
                }
                return;
            }
            if (type == AbilitiesEnum.START_SPRINTING) protocol.abilities.setSprinting(true);
            if (type == AbilitiesEnum.STOP_SPRINTING) protocol.abilities.setSprinting(false);
            if (type == AbilitiesEnum.PRESS_SHIFT_KEY) protocol.abilities.setSneaking(true);
            if (type == AbilitiesEnum.RELEASE_SHIFT_KEY) protocol.abilities.setSneaking(false);
            if (type == AbilitiesEnum.START_SNEAKING) protocol.abilities.setSneaking(true);
            if (type == AbilitiesEnum.STOP_SNEAKING) protocol.abilities.setSneaking(false);
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
