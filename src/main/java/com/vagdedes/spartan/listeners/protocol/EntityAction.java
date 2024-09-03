package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesEnum;
import org.bukkit.entity.Player;

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
        Player player = event.getPlayer();

        if (ProtocolLib.isTemporary(player)) {
            return;
        }
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
        String typeString = event.getPacket().getModifier().getValues().get(1).toString();
        AbilitiesEnum type = getEnum(typeString);

        if (typeString != null) {
            if (type == AbilitiesEnum.PRESS_SHIFT_KEY) {
                protocol.setSneaking(true);
            } else if (type == AbilitiesEnum.RELEASE_SHIFT_KEY) {
                protocol.setSneaking(false);
            } else if (type == AbilitiesEnum.START_SPRINTING) {
                protocol.setSprinting(true);
            } else if (type == AbilitiesEnum.STOP_SPRINTING) {
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
