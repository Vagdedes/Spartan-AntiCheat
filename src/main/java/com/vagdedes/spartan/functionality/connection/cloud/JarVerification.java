package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.List;

public class JarVerification {

    private static boolean valid = true;

    public static void run() {
        if (!IDs.enabled) {
            SpartanBukkit.connectionThread.execute(() -> {
                int userID = CloudConnections.getUserIdentification();

                if (userID <= 0) {
                    valid = false;
                }
            });
        }

        if (IDs.enabled || CloudBase.hasToken()) {
            if (isValid(true)) {
                SpartanEdition.refresh();
            } else {
                Register.disablePlugin();
            }
        } else {
            SpartanEdition.refresh();

            if (!isValid(true)) {
                String message = "This version of Spartan does not have a license."
                        + " If this download is pirated, please consider purchasing the plugin"
                        + " when your server starts making enough money. We also sell on BuiltByBit"
                        + " which supports many payment methods for all countries including yours.";
                List<SpartanPlayer> staff = Permissions.getStaff();

                if (!staff.isEmpty()) {
                    for (SpartanPlayer sp : staff) {
                        sp.sendImportantMessage(AwarenessNotifications.getNotification(message));
                    }
                }
                AwarenessNotifications.forcefullySend(message);
            }
        }
    }

    static boolean isValid(boolean first) {
        boolean b = valid
                && Register.plugin.getName().equalsIgnoreCase("Spartan")
                && Register.plugin.getDescription().getAuthors().toString().startsWith("[Evangelos Dedes @Vagdedes");

        if (first) {
            try {
                String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website)
                        + "?" + CloudBase.identification + "&action=add&data=userVerification");

                if (results.length > 0) {
                    String line = results[0];

                    if (line.equalsIgnoreCase(String.valueOf(false))) {
                        valid = false;
                        return false;
                    }
                    if (CloudBase.hasToken() && AlgebraUtils.validInteger(line)) {
                        IDs.setPlatform(Integer.parseInt(line));
                    }
                }
            } catch (Exception e) {
                if (IDs.canAdvertise()) {
                    e.printStackTrace();
                }
            }
        }
        return b;
    }

}