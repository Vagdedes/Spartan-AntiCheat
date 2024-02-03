package com.vagdedes.spartan.functionality.commands;

import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

import java.util.List;

public class UnbanCommand {

    public static void run(SpartanPlayer p, String m) {
        if (Permissions.isStaff(p)
                && (executed(m, "unban")
                || executed(m, "pardon"))) {
            List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

            if (!playerProfiles.isEmpty()) {
                for (String arg : m.split(" ")) {
                    for (PlayerProfile playerProfile : playerProfiles) {
                        String name = playerProfile.getName();

                        if (name.equalsIgnoreCase(arg)) {
                            ResearchEngine.resetData(name);
                            return;
                        }
                    }
                }
            }
        }
    }

    private static boolean executed(String m, String c) {
        return m.toLowerCase().startsWith("/" + c + " ")
                || m.equalsIgnoreCase("/" + c);
    }
}
