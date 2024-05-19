package com.vagdedes.spartan.functionality.chat;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.command.CommandSender;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ChatProtection {

    private static final Map<UUID, String> previous = new LinkedHashMap<>();

    public static void clear() {
        previous.clear();
    }

    public static void remove(SpartanPlayer p) {
        previous.remove(p.uuid);
    }

    private static boolean canBlock(String string, String content, boolean blockedCommands) {
        return blockedCommands ? string.equals(content) || string.startsWith(content + " ") :
                string.contains(content + " ") || string.contains(" " + content) || string.equals(content);
    }

    private static boolean isBlocked(String string, String type) {
        String keysString = Config.settings.getString("Chat." + type);

        if (keysString != null && keysString.contains(", ")) {
            string = string.toLowerCase();
            boolean blockedCommands = type.equals("blocked_commands");
            String pluginName = blockedCommands ? Register.plugin.getName().toLowerCase() : null;

            for (String key : keysString.split(", ")) {
                if (key != null) {
                    key = key.toLowerCase();

                    if ((!blockedCommands || !key.startsWith(pluginName)) && canBlock(string, key, blockedCommands)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean runChat(SpartanPlayer p, String msg) {
        if (Permissions.has(p, Enums.Permission.CHAT_PROTECTION)) {
            return false;
        }
        UUID uuid = p.uuid;
        StringBuilder doubleLessMsg = new StringBuilder();

        for (String s : msg.split(" ")) {
            if (s.startsWith("-")) {
                s = s.substring(1);
            }
            if (!AlgebraUtils.validDecimal(s)) {
                doubleLessMsg.append(s);
            }
        }

        if (!p.cooldowns.canDo("chat=cooldown=delay")) {
            double seconds = (double) p.cooldowns.get("chat=cooldown=delay") / 20.0;
            String message = Config.messages.getColorfulString("chat_cooldown_message").replace("{time}", String.valueOf(seconds));
            p.sendMessage(ConfigUtils.replaceWithSyntax(p, message, null));
            return true;
        }
        if (isBlocked(msg, "blocked_words")) {
            String message = Config.messages.getColorfulString("blocked_word_message");
            p.sendMessage(ConfigUtils.replaceWithSyntax(p, message, null));
            return true;
        }
        if (Config.settings.getBoolean("Chat.prevent_same_message")) {
            String previousMessage = previous.get(uuid);

            if (previousMessage != null
                    && doubleLessMsg.length() > 0
                    && previousMessage.equalsIgnoreCase(doubleLessMsg.toString())) {
                String message = Config.messages.getColorfulString("same_message_warning");
                p.sendMessage(ConfigUtils.replaceWithSyntax(p, message, null));
                return true;
            }
        }
        int seconds = Config.settings.getInteger("Chat.message_cooldown");

        if (seconds > 0) {
            p.cooldowns.add("chat=cooldown=delay", Math.min(60, seconds) * 20);
        }
        previous.put(uuid, doubleLessMsg.toString());
        return false;
    }

    public static boolean runCommand(SpartanPlayer p, String msg, boolean tab) {
        if (Permissions.has(p, Enums.Permission.CHAT_PROTECTION)) {
            return false;
        }
        if (!p.cooldowns.canDo("command=cooldown=delay") && !tab) {
            double seconds = (double) p.cooldowns.get("command=cooldown=delay") / 20.0;
            String message = Config.messages.getColorfulString("command_cooldown_message").replace("{time}", String.valueOf(seconds));
            p.sendMessage(ConfigUtils.replaceWithSyntax(p, message, null));
            return true;
        }
        if (!tab) {
            int seconds = Config.settings.getInteger("Chat.command_cooldown");

            if (seconds > 0) {
                p.cooldowns.add("command=cooldown=delay", Math.min(seconds, 60) * 20);
            }
        }
        if (isBlocked(StringUtils.substring(msg, 1, msg.length()), "blocked_commands")) {
            if (!tab) {
                p.sendMessage(ConfigUtils.replaceWithSyntax(p, Config.messages.getColorfulString("blocked_command_message"), null));
            }
            return true;
        }
        return false;
    }

    public static boolean runConsoleCommand(CommandSender s, String msg) {
        if (isBlocked(StringUtils.substring(msg, 1, msg.length()), "blocked_commands")) {
            s.sendMessage(Config.messages.getColorfulString("blocked_command_message"));
            return true;
        }
        return false;
    }
}
