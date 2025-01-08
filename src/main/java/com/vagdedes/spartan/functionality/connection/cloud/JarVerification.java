package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class JarVerification {

    static final String
            website = "aHR0cHM6Ly93d3cudmFnZGVkZXMuY29tL21pbmVjcmFmdC9jbG91ZC8=",
            accountWebsite = "aHR0cHM6Ly93d3cuaWRlYWxpc3RpYy5haS9hcGkvdjEvcHJvZHVjdC92ZXJpZnlEb3dubG9hZC8=";
    private static boolean valid = true;

    public static void run(Plugin plugin) {
        if (!IDs.enabled) {
            SpartanBukkit.connectionThread.execute(() -> {
                int userID = CloudConnections.getUserIdentification();

                if (userID <= 0) {
                    valid = false;
                }
            });
        }

        if (IDs.enabled || IDs.hasToken()) {
            if (isValid(plugin)) {
                SpartanEdition.refresh();
            } else {
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        } else {
            SpartanEdition.refresh();

            if (!isValid(plugin)) {
                String message = "This version of " + plugin.getName() + " does not have a license."
                        + " If this download is pirated, please consider purchasing the plugin"
                        + " when your server starts making enough money. We also sell on BuiltByBit"
                        + " which supports many payment methods for all countries including yours.";
                List<SpartanProtocol> staff = Permissions.getStaff();

                if (!staff.isEmpty()) {
                    for (SpartanProtocol protocol : staff) {
                        protocol.spartan.sendImportantMessage(AwarenessNotifications.getNotification(message));
                    }
                }
                AwarenessNotifications.forcefullySend(message);
            }
        }
    }

    private static boolean isValid(Plugin plugin) {
        boolean b = valid
                && plugin.getDescription().getAuthors().toString().startsWith("[Evangelos Dedes @Vagdedes");

        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(website)
                    + "?" + CloudBase.identification() + "&action=add&data=userVerification");

            if (results.length > 0) {
                String line = results[0];

                if (line.equalsIgnoreCase(String.valueOf(false))) {
                    valid = false;
                    return false;
                }
                if (IDs.hasToken()) {
                    IDs.setPlatform(Integer.parseInt(line));
                }
            }
        } catch (Exception e) {
            if (IDs.canAdvertise()) {
                e.printStackTrace();
            }
        }
        return b;
    }

}