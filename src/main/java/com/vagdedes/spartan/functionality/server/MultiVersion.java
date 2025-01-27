package com.vagdedes.spartan.functionality.server;

import com.comphenix.protocol.ProtocolLibrary;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import com.vagdedes.spartan.utils.minecraft.server.PluginUtils;
import com.viaversion.viaversion.api.Via;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MultiVersion {

    private static final boolean paperClass = ReflectionUtils.classExists(
            "com.destroystokyo.paper.network.NetworkClient"
    );
    public static final MCVersion serverVersion;
    public static final boolean folia;

    static {
        serverVersion = judge();
        folia = Bukkit.getVersion().toLowerCase().contains("folia");
    }

    public enum MCVersion {
        V1_7(5),
        V1_8(47),
        V1_9(110),
        V1_10(210),
        V1_11(316),
        V1_12(340),
        V1_13(404),
        V1_14(498),
        V1_15(578),
        V1_16(754),
        V1_17(756),
        V1_18(758),
        V1_19(762),
        V1_20(766),
        V1_21(Integer.MAX_VALUE),
        OTHER(-1); // Always last

        public final int maxProtocol;

        MCVersion(int maxProtocol) {
            this.maxProtocol = maxProtocol;
        }

        @Override
        public String toString() {
            return this.maxProtocol == -1
                    ? "Unknown"
                    : this.defaultString().substring(1).replace("_", ".");
        }

        private String defaultString() {
            return super.toString();
        }
    }

    public static boolean isOrGreater(MCVersion trialVersion) {
        return serverVersion.ordinal() >= trialVersion.ordinal();
    }

    public static MCVersion get(Player player) {
        int protocol;

        if (ProtocolLib.isTemporary(player)) {
            protocol = -1;
        } else if (PluginUtils.exists("viaversion")) {
            protocol = Via.getAPI().getPlayerVersion(player);
        } else if (paperClass) {
            protocol = player.getProtocolVersion();
        } else if (PluginBase.packetsEnabled()) {
            protocol = ProtocolLibrary.getProtocolManager().getProtocolVersion(player);
        } else {
            protocol = -1;
        }

        if (protocol >= 0) {
            for (MCVersion version : MCVersion.values()) {
                if (protocol <= version.maxProtocol) {
                    return version;
                }
            }
        }
        return MultiVersion.MCVersion.OTHER;
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
                            if (mcversion.defaultString().equalsIgnoreCase("V" + version.substring(0, version.length() - x).replace(".", "_"))) {
                                return mcversion;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return MCVersion.OTHER;
    }
}
