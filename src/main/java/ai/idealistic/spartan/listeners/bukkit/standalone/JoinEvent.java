package ai.idealistic.spartan.listeners.bukkit.standalone;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.functionality.moderation.AwarenessNotifications;
import ai.idealistic.spartan.functionality.server.Config;
import ai.idealistic.spartan.functionality.server.Permissions;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.functionality.tracking.LimitedEdition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerJoinEvent e) {
        Player n = e.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(n);

        if ((Register.isFreeEdition()
                || Config.settings.getBoolean("Important.enable_watermark"))
                && !Permissions.isStaff(n)) {
            n.sendMessage("");
            AwarenessNotifications.forcefullySend(
                    protocol,
                    "\nThis server is protected by the " + Register.pluginName + " AntiCheat",
                    false
            );
            n.sendMessage("");
        }

        PluginBase.runDelayedTask(protocol, () -> {
            Config.settings.runOnLogin(protocol);
            LimitedEdition.notify(protocol);
        }, 10L);
    }

}
