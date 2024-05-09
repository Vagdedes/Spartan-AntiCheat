package com.vagdedes.spartan.functionality.notifications.clickable;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClickableMessage {

    private static final boolean craftBukkit = MultiVersion.fork().equals("CraftBukkit");

    public static boolean sendDescription(SpartanPlayer p, String message, String preview) {
        Player n = p.getPlayer();

        if (n != null) {
            return sendDescription(n, message, preview);
        }
        return false;
    }

    public static boolean sendCommand(SpartanPlayer p, String message, String preview, String command) {
        Player n = p.getPlayer();

        if (n != null) {
            return sendCommand(n, message, preview, command);
        }
        return false;
    }

    public static boolean sendURL(SpartanPlayer p, String message, String preview, String url) {
        Player n = p.getPlayer();

        if (n != null) {
            return sendURL(n, message, preview, url);
        }
        return false;
    }

    // Separator

    public static boolean sendDescription(CommandSender p, String message, String preview) {
        if (!craftBukkit) {
            return BackgroundClickableMessage.send(p, message, preview, null, true);
        }
        return false;
    }

    public static boolean sendCommand(CommandSender p, String message, String preview, String command) {
        if (!craftBukkit) {
            return BackgroundClickableMessage.send(p, message, preview, command, true);
        }
        return false;
    }

    public static boolean sendURL(CommandSender p, String message, String preview, String url) {
        if (!craftBukkit) {
            return BackgroundClickableMessage.sendURL(p, message, preview, url, true);
        }
        return false;
    }
}
