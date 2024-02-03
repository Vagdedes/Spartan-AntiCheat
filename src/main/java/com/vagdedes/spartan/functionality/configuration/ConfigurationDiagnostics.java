package com.vagdedes.spartan.functionality.configuration;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.gui.helpers.PlayerStateLists;
import com.vagdedes.spartan.handlers.stability.CancelViolation;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.ChatColor;

public class ConfigurationDiagnostics {

    private static long cooldown = 0;
    private static final String s = "[Configuration Diagnostics] ";

    public static void execute(SpartanPlayer p) {
        if (Config.hasCancelAfterViolationOption()) {
            boolean send = p != null;
            long milliseconds = System.currentTimeMillis();

            if (cooldown < milliseconds) {
                cooldown = milliseconds + 60_000L;

                if (!ResearchEngine.enoughData()) {
                    if (send) {
                        String message = ConfigUtils.replaceWithSyntax(p,
                                Config.messages.getColorfulString("not_enough_saved_logs").replace("{amount}", String.valueOf(ResearchEngine.logRequirement)),
                                null);
                        p.sendMessage(message);
                    }
                } else {
                    if (!send) {
                        AwarenessNotifications.forcefullySend("Running automated Configuration Diagnostics.");
                    } else {
                        AwarenessNotifications.forcefullySend(p, "Running manual Configuration Diagnostics.");
                    }
                    if (!ResearchEngine.isCaching()) {
                        boolean access = false;

                        if (send) {
                            p.sendMessage(ChatColor.DARK_GREEN + s.substring(0, s.length() - 1) + ":");
                        }

                        for (Enums.HackType hackType : Enums.HackType.values()) {
                            Check check = hackType.getCheck();

                            if (check.hasCancelViolation()) {
                                int preferred = CancelViolation.get(hackType, ResearchEngine.DataType.Universal);

                                if (preferred > check.getCancelViolation() && check.setCancelViolation(preferred)) {
                                    access = true;

                                    if (send) {
                                        p.sendMessage(ChatColor.GREEN + "Changed " + hackType + "'s cancel-after-violation to " + preferred + ".");
                                    }
                                }
                            }
                        }
                        if (send) {
                            if (!access) {
                                p.sendMessage(ChatColor.GREEN + "No changes needed.");
                            }
                            p.sendMessage("");
                        }
                    } else if (send) {
                        p.sendMessage(ChatColor.RED + PlayerStateLists.calculatingData);
                    }
                }
            } else if (send) {
                AwarenessNotifications.forcefullySend(p, ChatColor.RED + s + "Under cooldown. Please wait " + ((cooldown - milliseconds) / 1000L) + " second(s).");
            }
        }
    }
}
