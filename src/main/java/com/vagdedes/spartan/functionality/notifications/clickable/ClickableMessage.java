package com.vagdedes.spartan.functionality.notifications.clickable;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import org.bukkit.command.CommandSender;

public class ClickableMessage {

    private static final boolean craftBukkit = MultiVersion.fork().equals("CraftBukkit");

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
