package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JarVerification {

    private static final Set<String> administrators = new HashSet<>(20);

    private static boolean valid = true;
    private static final String name = Register.plugin.getName();
    public static final boolean enabled = AlgebraUtils.validInteger("%%__RESOURCE__%%");

    static {
        if (!enabled) {
            SpartanBukkit.connectionThread.execute(() -> {
                int userID = CloudConnections.getUserIdentification();

                if (userID <= 0) {
                    valid = false;
                }
            });
        }

        SpartanEdition.refresh();

        // Separator
        long delay = 1200L;
        Object scheduledTask = SpartanBukkit.runRepeatingTask(JarVerification::collectAdministrators, 1L, 1L);

        SpartanBukkit.runDelayedTask(() -> SpartanBukkit.connectionThread.execute(() ->
                valid = isValid(
                        "https://www.vagdedes.com/minecraft/cloud/verification/?id=&nonce=",
                        IDs.user(),
                        IDs.nonce()
                )
        ), delay * 2L);

        // Separator

        SpartanBukkit.runDelayedTask(() -> {
            SpartanBukkit.cancelTask(scheduledTask);

            if (!valid && (enabled || CloudBase.hasToken())) {
                Register.disablePlugin();
            }
        }, delay * 3L);
    }

    private static boolean isValid(String site, String spigot, String nonce) {
        boolean b = valid
                && name.equalsIgnoreCase("Spartan")
                && Register.plugin.getDescription().getAuthors().toString().startsWith("[Evangelos Dedes @Vagdedes");

        try {
            int number = site.length() - 7;
            String platformName = IDs.platform(),
                    platform = platformName != null ? ("&platform=" + platformName) : "",
                    port = "&port=" + Bukkit.getPort(),
                    website = site.substring(0, number) + spigot + site.substring(number) + nonce + platform + port;

            // Separator
            String additional;

            if (!administrators.isEmpty()) {
                additional = StringUtils.toString(administrators.toArray(new String[0]), ",");
                administrators.clear();
            } else {
                additional = null;
            }

            // Separator
            String[] reply = RequestUtils.get(website, "GET", additional, RequestUtils.defaultTimeOut);

            if (reply.length > 0) {
                String line = reply[0];

                if (line.equalsIgnoreCase(String.valueOf(false))) {
                    return false;
                }
                if (CloudBase.hasToken() && AlgebraUtils.validInteger(line)) {
                    IDs.setPlatform(Integer.parseInt(line));
                }
            }
        } catch (Exception e) {
            if (SpartanBukkit.canAdvertise) {
                e.printStackTrace();
            }
        }
        return b;
    }

    private static void collectAdministrators() {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer p : players) {
                if (p != null) {
                    if (Permissions.isStaff(p)) {
                        String ip = p.getIpAddress();

                        if (ip != null) {
                            String dot = ".";
                            String[] split = ip.split("\\.");

                            if (split.length == 4) {
                                ip = split[0] + dot + split[1] + dot + split[2] + dot + "XXX";
                            } else {
                                split = ip.split(":");

                                if (split.length == 8) {
                                    String hidden = "XXXX";
                                    ip = split[0] + dot + split[1] + dot + split[2] + dot + split[3] + dot + split[4] + dot + split[5] + dot + hidden + dot + hidden;
                                } else {
                                    ip = "Unknown";
                                }
                            }
                            administrators.add(p.name + "|" + p.uuid + "|" + ip);
                        }
                    }
                }
            }
        }
    }

}