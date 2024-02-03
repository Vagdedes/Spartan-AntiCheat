package com.vagdedes.spartan.functionality.protections;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;

import java.util.UUID;

public class Teleport {

    public static final int ticks = 3;
    public static final String reason = "Teleport-Protection";

    public static void run(SpartanPlayer p) {
        if (!p.getHandlers().isDisabled(Handlers.HandlerType.Teleport)) {
            UUID uuid = p.getUniqueId();

            if (Config.settings.getBoolean("Protections.use_teleport_protection")) {
                for (Enums.HackType hackType : Enums.HackType.values()) {
                    hackType.getCheck().addSilentUser(uuid, reason, ticks);
                }
            } else {
                for (Enums.HackType hackType : Enums.HackType.values()) {
                    Check check = hackType.getCheck();

                    if (!check.hasMaximumDefaultCancelViolation()) {
                        hackType.getCheck().addSilentUser(uuid, reason, ticks);
                    }
                }
            }
        }
    }
}
