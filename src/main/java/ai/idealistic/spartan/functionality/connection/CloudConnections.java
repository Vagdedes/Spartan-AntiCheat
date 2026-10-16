package ai.idealistic.spartan.functionality.connection;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.functionality.moderation.AwarenessNotifications;
import ai.idealistic.spartan.functionality.moderation.CrossServerNotifications;
import ai.idealistic.spartan.functionality.server.Config;
import ai.idealistic.spartan.functionality.server.PluginBase;
import org.bukkit.ChatColor;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class CloudConnections {

    public static void executeDiscordWebhook(String webhook, UUID uuid, String name, int ping, int x, int y, int z, String type, String information) {
        String urlString = Config.settings.getString("Discord." + webhook + "_webhook_url");

        if (urlString == null
                || (!urlString.startsWith("https://") && !urlString.startsWith("http://"))) {
            return;
        }
        PluginBase.connectionThread.executeIfUnknownThreadElseHere(() -> {
            try {
                String serverName = CrossServerNotifications.getServerName();
                String titleSuffix = (serverName != null && !serverName.isEmpty() && !serverName.equals("NULL"))
                        ? " (" + serverName + ")"
                        : "";
                String colorHex = Config.settings.getString("Discord.webhook_hex_color");
                int colorDecimal = 0;

                try {
                    if (colorHex != null
                            && !colorHex.isEmpty()) {
                        colorDecimal = Integer.parseInt(colorHex.replace("#", ""), 16);
                    }
                } catch (NumberFormatException ignored) {
                }
                String avatarUrl = "https://mc-heads.net/avatar/" + uuid.toString() + "/64";
                String safeName = escapeJson(truncate(name, 32));
                String safeUuid = escapeJson(truncate(uuid.toString(), 1024));
                String safeType = escapeJson(truncate(type, 256));
                String safeInfo = escapeJson(truncate(ChatColor.stripColor(information).replace("\n", " | "), 1024));
                String safeTitle = escapeJson(truncate(Register.plugin + " AntiCheat" + titleSuffix, 256));
                String jsonPayload = "{"
                        + "\"username\": \"Spartan AntiCheat\","
                        + "\"avatar_url\": \"https://vagdedes.com/.images/spartan/logo.png\","
                        + "\"embeds\": [{"
                        + "\"color\": " + colorDecimal + ","
                        + "\"author\": {"
                        + "\"name\": \"" + safeTitle + "\","
                        + "\"icon_url\": \"" + avatarUrl + "\""
                        + "},"
                        + "\"fields\": ["
                        + "{\"name\": \"" + safeName + "\", \"value\": \"``" + safeUuid + "``\", \"inline\": true},"
                        + "{\"name\": \"X, Y, Z, Ping\", \"value\": \"``" + x + "``**,** ``" + y + "``**,** ``" + z + "``**,** ``" + ping + "``\", \"inline\": true},"
                        + "{\"name\": \"" + safeType + "\", \"value\": \"``" + safeInfo + "``\", \"inline\": false}"
                        + "]"
                        + "}]"
                        + "}";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Java-Discord-Webhook");

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                int responseCode = connection.getResponseCode();

                if (responseCode < 200
                        || responseCode > 299) {
                    AwarenessNotifications.optionallySend("Webhook failed: " + responseCode);
                }

            } catch (Exception e) {
                AwarenessNotifications.optionallySend("Webhook exception: " + e.getMessage());
            }
        });
    }

    private static String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        for (char c : raw.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        continue;
                    }
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

}
