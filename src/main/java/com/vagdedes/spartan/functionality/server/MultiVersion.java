package com.vagdedes.spartan.functionality.server;

import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.Bukkit;

public class MultiVersion {

    private static MCVersion detectedVersion;
    private static final String fork;
    public static final boolean other, unknownFork, folia;

    static {
        detectedVersion = judge();
        other = detectedVersion == MCVersion.OTHER;

        // Separator
        String serverType = Bukkit.getVersion().toLowerCase(),
                unknownKeyword = "Unknown";

        if (serverType.contains("folia")) {
            fork = "Folia";
            folia = true;
            unknownFork = false;
        } else {
            folia = false;

            if (serverType.contains("taco")) {
                fork = "TacoSpigot";
            } else if (serverType.contains("paper")) {
                fork = "Paper";
            } else if (serverType.contains("spigot")) {
                fork = "Spigot";
            } else if (serverType.contains("craftbukkit")) {
                fork = "CraftBukkit";
            } else if (serverType.contains("tuinity")) {
                fork = "Tuinity";
            } else if (serverType.contains("yatopia")) {
                fork = "Yatopia";
            } else if (serverType.contains("purpur")) {
                fork = "Purpur";
            } else if (serverType.contains("airplane")) {
                fork = "Airplane";
            } else {
                fork = unknownKeyword;
            }
            unknownFork = fork.equals(unknownKeyword);
        }
    }

    public enum MCVersion {
        V1_7, V1_8, V1_9, V1_10, V1_11, V1_12, V1_13, V1_14, V1_15, V1_16,
        V1_17, V1_18, V1_19, V1_20,
        OTHER // Always last
    }

    public static boolean isOrGreater(MCVersion trialVersion) {
        return detectedVersion.ordinal() >= trialVersion.ordinal();
    }

    public static String fork() {
        return fork;
    }

    public static String versionString() {
        return other ? "Unknown" : detectedVersion.toString().substring(1).replace("_", ".");
    }

    public static MCVersion version() {
        return detectedVersion;
    }

    private static MCVersion judge() {
        String version = Bukkit.getVersion();
        version = version.substring(0, version.length() - 1);

        try {
            for (int i = 0; i <= version.length(); i++) {
                if (version.substring(i).startsWith("(MC: ")) {
                    version = version.substring(i + 5);

                    for (int x = 0; x <= version.length(); x++) {
                        for (MCVersion mcversion : MCVersion.values()) {
                            if (mcversion.toString().equalsIgnoreCase("V" + version.substring(0, version.length() - x).replace(".", "_"))) {
                                return MultiVersion.detectedVersion = mcversion;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ConfigUtils.add(Config.settings.getFile(), "Important.server_version", "Configure this if the plugin is unable to detect your server's version.");
        String option = "Important.server_version";
        String custom = Config.settings.getString(option);

        if (custom != null) {
            String[] split = custom.split("\\.");

            if (split.length >= 2) {
                try {
                    return MultiVersion.detectedVersion = MCVersion.valueOf("V" + split[0] + "_" + split[1]);
                } catch (Exception ex) {
                    AwarenessNotifications.forcefullySend("Invalid Config.settings.yml " + option + " configured value.");
                }
            }
        }
        return MultiVersion.detectedVersion = MCVersion.OTHER;
    }
}
