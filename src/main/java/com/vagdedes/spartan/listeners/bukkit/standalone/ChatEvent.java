package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(AsyncPlayerChatEvent e) {
        SpartanProtocol p = SpartanBukkit.getProtocol(e.getPlayer());

        // Detections
        p.profile().getRunner(Enums.HackType.Exploits).handle(e.isCancelled(), e.getMessage());
    }

}
