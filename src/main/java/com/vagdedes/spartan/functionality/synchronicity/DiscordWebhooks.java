package com.vagdedes.spartan.functionality.synchronicity;

import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudConnections;

import java.util.UUID;

public class DiscordWebhooks {

    public static final String configurationSection = "Discord";

    static void send(UUID uuid, String name,
                     int x, int y, int z,
                     String type, String string) {
        String webhook;

        switch (type) {
            case "Kick":
            case "Warning":
            case "Punishment":
                webhook = "punishments";
                break;
            case "Staff Chat":
                webhook = "communication";
                break;
            default:
                return;
        }
        CloudConnections.executeDiscordWebhook(
                webhook,
                uuid, (name == null ? "-" : name),
                x, y, z,
                type, string
        );
    }
}
