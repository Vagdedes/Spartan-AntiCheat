package com.vagdedes.spartan.functionality.notifications.clickable;

import com.vagdedes.spartan.utils.java.ReflectionUtils;
import org.bukkit.command.CommandSender;

public class ClickableMessage {

    private static final boolean exists = ReflectionUtils.classExists(
            "net.md_5.bungee.api.chat.ClickEvent"
    );

    public static boolean sendDescription(CommandSender p, String message, String preview) {
        if (exists) {
            return BackgroundClickableMessage.send(p, message, preview, null, true);
        }
        return false;
    }

    public static boolean sendCommand(CommandSender p, String message, String preview, String command) {
        if (exists) {
            return BackgroundClickableMessage.send(p, message, preview, command, true);
        }
        return false;
    }

    public static boolean sendURL(CommandSender p, String message, String preview, String url) {
        if (exists) {
            return BackgroundClickableMessage.sendURL(p, message, preview, url, true);
        }
        return false;
    }
}
