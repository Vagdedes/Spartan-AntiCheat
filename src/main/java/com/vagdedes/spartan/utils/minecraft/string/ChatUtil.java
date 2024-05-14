package com.vagdedes.spartan.utils.minecraft.string;

import org.bukkit.ChatColor;

public final class ChatUtil {

    private ChatUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String translate(final String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String format(final String... strings) {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < strings.length; i++) {
            builder.append(translate(strings[i]));

            if (i != (strings.length - 1)) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
