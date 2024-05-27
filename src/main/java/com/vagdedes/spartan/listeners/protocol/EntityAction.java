package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesContainer;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesEnum;
import org.bukkit.Bukkit;

public class EntityAction extends PacketAdapter {

    public EntityAction() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ENTITY_ACTION);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        SpartanPlayer player = SpartanBukkit.getPlayer(event.getPlayer());

        if (player == null) {
            return;
        }
        AbilitiesEnum type = AbilitiesEnum.valueOf(event.getPacket().getModifier().getValues().get(1).toString());

        if (type == AbilitiesEnum.START_SPRINTING) {
            AbilitiesContainer container = ProtocolStorage.getAbilities(player.uuid);

            if (container.sprint()) {
                AntiCheatLogs.logInfo(
                        player,
                        player.name
                                + " use impossible EntityAction! (START_SPRINTING while already sprinting)",
                        true
                );
                player.punishments.kick(
                        Bukkit.getConsoleSender(),
                        "Invalid packet received (1)"
                );
            } else {
                container.setSprint(true);
            }
        }
        if (type == AbilitiesEnum.STOP_SPRINTING) {
            AbilitiesContainer container = ProtocolStorage.getAbilities(player.uuid);

            if (!container.sprint()) {
                AntiCheatLogs.logInfo(
                        player,
                        player.name
                                + " use impossible EntityAction! (STOP_SPRINTING while already non-sprinting)",
                        true
                );
                player.punishments.kick(
                        Bukkit.getConsoleSender(),
                        "Invalid packet received (2)"
                );
            } else {
                container.setSprint(false);
            }
        }
        if (type == AbilitiesEnum.PRESS_SHIFT_KEY) {
            AbilitiesContainer container = ProtocolStorage.getAbilities(player.uuid);

            if (container.sneak()) {
                AntiCheatLogs.logInfo(
                        player,
                        player.name
                                + " use impossible EntityAction! (PRESS_SHIFT_KEY while already sneaking)",
                        true
                );
                player.punishments.kick(
                        Bukkit.getConsoleSender(),
                        "Invalid packet received (3)"
                );
            } else {
                container.setSneak(true);
            }
        }
        if (type == AbilitiesEnum.RELEASE_SHIFT_KEY) {
            AbilitiesContainer container = ProtocolStorage.getAbilities(player.uuid);

            if (!container.sneak()) {
                AntiCheatLogs.logInfo(
                        player,
                        player.name
                                + " use impossible EntityAction! (RELEASE_SHIFT_KEY while already non-sneaking)",
                        true
                );
                player.punishments.kick(
                        Bukkit.getConsoleSender(),
                        "Invalid packet received (4)"
                );
            } else {
                container.setSneak(false);
            }
        }
    }

}
