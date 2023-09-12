package me.vagdedes.spartan.features.chat;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.ConfigUtils;
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
        previous.remove(p.getUniqueId());
    }

    private static boolean canBlock(String string, String content, boolean blockedCommands) {
        return blockedCommands ? string.equals(content) || string.startsWith(content + " ") :
                string.contains(content + " ") || string.contains(" " + content) || string.equals(content);
    }

    private static boolean isBlocked(String string, String type) {
        String keysString = Settings.getString("Chat." + type);

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
        if (Permissions.has(p.getPlayer(), Enums.Permission.CHAT_PROTECTION)) {
            return false;
        }
        UUID uuid = p.getUniqueId();
        StringBuilder doubleLessMsg = new StringBuilder();

        for (String s : msg.split(" ")) {
            if (s.startsWith("-")) {
                s = s.substring(1);
            }
            if (!AlgebraUtils.validDecimal(s)) {
                doubleLessMsg.append(s);
            }
        }

        if (!p.getCooldowns().canDo("chat=cooldown=delay")) {
            double seconds = (double) p.getCooldowns().get("chat=cooldown=delay") / 20.0;
            String message = Messages.get("chat_cooldown_message").replace("{time}", String.valueOf(seconds));
            p.sendMessage(ConfigUtils.replaceWithSyntax(p, message, null));
            return true;
        }
        if (isBlocked(msg, "blocked_words")) {
            String message = Messages.get("blocked_word_message");
            p.sendMessage(ConfigUtils.replaceWithSyntax(p, message, null));
            return true;
        }
        if (Settings.getBoolean("Chat.prevent_same_message")) {
            String previousMessage = previous.get(uuid);

            if (previousMessage != null
                    && doubleLessMsg.length() > 0
                    && previousMessage.equalsIgnoreCase(doubleLessMsg.toString())) {
                String message = Messages.get("same_message_warning");
                p.sendMessage(ConfigUtils.replaceWithSyntax(p, message, null));
                return true;
            }
        }
        int seconds = Settings.getInteger("Chat.message_cooldown");

        if (seconds > 0) {
            p.getCooldowns().add("chat=cooldown=delay", Math.min(60, seconds) * 20);
        }
        previous.put(uuid, doubleLessMsg.toString());
        return false;
    }

    public static boolean runCommand(SpartanPlayer p, String msg, boolean tab) {
        if (Permissions.has(p, Enums.Permission.CHAT_PROTECTION)) {
            return false;
        }
        if (!p.getCooldowns().canDo("command=cooldown=delay") && !tab) {
            double seconds = (double) p.getCooldowns().get("command=cooldown=delay") / 20.0;
            String message = Messages.get("command_cooldown_message").replace("{time}", String.valueOf(seconds));
            p.sendMessage(ConfigUtils.replaceWithSyntax(p, message, null));
            return true;
        }
        if (!tab) {
            int seconds = Settings.getInteger("Chat.command_cooldown");

            if (seconds > 0) {
                p.getCooldowns().add("command=cooldown=delay", Math.min(seconds, 60) * 20);
            }
        }
        if (isBlocked(me.vagdedes.spartan.utils.java.StringUtils.substring(msg, 1, msg.length()), "blocked_commands")) {
            if (!tab) {
                p.sendMessage(ConfigUtils.replaceWithSyntax(p, Messages.get("blocked_command_message"), null));
            }
            return true;
        }
        return false;
    }

    public static boolean runConsoleCommand(CommandSender s, String msg) {
        if (isBlocked(me.vagdedes.spartan.utils.java.StringUtils.substring(msg, 1, msg.length()), "blocked_commands")) {
            s.sendMessage(Messages.get("blocked_command_message"));
            return true;
        }
        return false;
    }
}
