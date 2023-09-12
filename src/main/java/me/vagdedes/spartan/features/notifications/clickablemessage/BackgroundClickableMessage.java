package me.vagdedes.spartan.features.notifications.clickablemessage;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackgroundClickableMessage {

    static boolean send(CommandSender p, String message, String preview, String command, boolean handle) {
        try {
            if (p instanceof Player) {
                TextComponent clickable = new TextComponent();
                clickable.setText(message);
                clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(preview).create()));

                if (command != null) {
                    clickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
                }
                ((Player) p).spigot().sendMessage(clickable);
            } else {
                p.sendMessage(message);
            }
            return true;
        } catch (Exception ex) {
            if (handle) {
                p.sendMessage(message);
            }
        }
        return false;
    }

    static boolean sendURL(CommandSender p, String message, String preview, String url, boolean handle) {
        try {
            if (p instanceof Player) {
                TextComponent clickable = new TextComponent();
                clickable.setText(message);
                clickable.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(preview).create()));
                ((Player) p).spigot().sendMessage(clickable);
            } else {
                p.sendMessage(message);
            }
            return true;
        } catch (Exception ex) {
            if (handle) {
                p.sendMessage(message);
            }
        }
        return false;
    }
}
