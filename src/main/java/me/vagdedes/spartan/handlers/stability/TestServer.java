package me.vagdedes.spartan.handlers.stability;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.notifications.DetectionNotifications;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class TestServer {

    private static final String[] anticheats = new String[]{
            "AntiCheat", // General Identification
            "AAC", "AdvancedAntiCheat", "Gurei", "Matrix", "NoCheatPlus", "Hawk", "BetterAntiCheat", "Reflex", "Grim",
            "TakaAntiCheat", "TakaAC", "AntiAura", "Kauri", "KAC", "NekoAC", "Horizon", "Intave", "GodsEye", "Frequency",
            "DakataAntiCheat", "DakataAC", "Wraith", "Iris", "Fiona", "Vulcan", "Karhu", "Watchdog", "Crimson", "Storm",
            "Negativity", "Soaroma", "Warden", "Wither", "ThotPatrol", "NESS", "AntiHaxerman", "Hades", "Medusa", "Phoenix",
            "Karma", "OpenEye", "UltraAC", "AngelX", "Cardinal", "Kokumin", "JI", "zHack", "Meow", "Sparky", "Artemis"
    };
    private static int[] identification = new int[]{};

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (Settings.getBoolean("Performance.disable_test_server_detection") || isIdentified()) {
                    return;
                }
                int identify = identify(Bukkit.getMotd(), false);

                if (identify != 0) {
                    identification = new int[]{1, identify};
                    return;
                }
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (players.size() > 0) {
                    for (SpartanPlayer p : players) {
                        SpartanBukkit.runTask(p, () -> {
                            if (p != null) {
                                Player n = p.getPlayer();

                                if (n != null && n.isOnline()) {
                                    int counter = 0;

                                    // Scoreboard
                                    for (String entry : n.getScoreboard().getEntries()) {
                                        int result = identify(entry, true);

                                        if (result != 0) {
                                            if (counter == 0) {
                                                counter++;
                                            } else {
                                                identification = new int[]{2, result};
                                                return;
                                            }
                                        }
                                    }

                                    // Tablist
                                    counter = 0;

                                    for (String anticheat : anticheats) {
                                        anticheat = anticheat.toLowerCase();
                                        int scenario;

                                        if (!n.getName().toLowerCase().contains(anticheat)) {
                                            if (n.getDisplayName().toLowerCase().contains(anticheat)) {
                                                scenario = 1;
                                            } else if (n.getPlayerListName().toLowerCase().contains(anticheat)) {
                                                scenario = 2;
                                            } else {
                                                scenario = 0;
                                            }
                                        } else {
                                            scenario = 0;
                                        }

                                        if (scenario != 0) {
                                            identification = new int[]{3, counter, scenario};
                                            return;
                                        }
                                        counter++;
                                    }
                                }
                            }
                        });
                    }
                }

                // Anticheats
                int count = 0;
                boolean skipFirst = true;

                for (String anticheat : anticheats) {
                    if (skipFirst) {
                        skipFirst = false;
                    } else if (PluginUtils.contains(anticheat)) {
                        count++;
                    }
                }

                if (count >= 4) {
                    identification = new int[]{4, count};
                }
            }, 1L, 15 * 20L);
        }
    }

    public static void refresh() {
        if (isIdentified() && Settings.getBoolean("Performance.disable_test_server_detection")) {
            identification = new int[]{};
        }
    }

    private static int identify(String s, boolean scoreboard) {
        s = StringUtils.getClearColorString(s.toLowerCase());
        boolean anti_cheat = s.contains("anti") && s.contains("cheat"),
                server = s.contains("serve");

        if (anti_cheat && server) {
            return 1;
        }
        if (s.contains("test") && (anti_cheat || server || s.contains("hack"))) {
            return 2;
        }
        if (scoreboard) {
            for (String anticheat : anticheats) {
                if (s.contains(anticheat.toLowerCase())) {
                    return 3;
                }
            }
        }
        return 0;
    }

    public static boolean isIdentified() {
        return identification.length != 0;
    }

    public static String getIdentification() {
        int iMax = identification.length - 1;

        if (iMax == -1) {
            return "";
        }
        StringBuilder b = new StringBuilder();

        for (int i = 0; ; i++) {
            b.append(identification[i]);

            if (i == iMax) {
                return b.toString();
            }
            b.append("-");
        }
    }

    public static boolean isTester(SpartanPlayer p) {
        if (isIdentified()
                || CloudFeature.hasException()
                || p.isOp()
                || Permissions.isStaff(p)
                || DetectionNotifications.hasPermission(p)) {
            p.getProfile().setTester(true);
            return true;
        } else {
            PlayerProfile profile = p.getProfile();

            if (profile.wasStaff()) {
                profile.setTester(true);
                return true;
            }
        }
        return false;
    }
}
