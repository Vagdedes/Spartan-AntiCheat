package com.vagdedes.spartan.functionality.server;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.configuration.ConfigurationBuilder;
import com.vagdedes.spartan.abstraction.configuration.implementation.*;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.functionality.moderation.Wave;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import me.vagdedes.spartan.api.SpartanReloadEvent;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.command.CommandSender;

public class Config {

    public static final Advanced advanced = new Advanced();
    public static final Settings settings = new Settings();
    public static final SQLFeature sql = new SQLFeature();
    public static final Messages messages = new Messages();
    public static final Compatibility compatibility = new Compatibility();

    public static final ConfigurationBuilder[] configurations = {
            advanced,
            settings,
            sql,
            messages
    };

    // Separator

    public static Check getCheckByName(String s) {
        for (HackType hackType : Enums.HackType.values()) {
            Check check = hackType.getCheck();
            String checkName = check.getName();

            if (checkName != null && checkName.equals(s)) {
                return check;
            }
        }
        return null;
    }

    // Separator

    public static void create() {
        boolean enabledPlugin = Register.isPluginEnabled();

        for (HackType hackType : Enums.HackType.values()) {
            hackType.resetCheck();
        }
        if (enabledPlugin) {
            for (ConfigurationBuilder configuration : configurations) {
                configuration.create();
            }
            Compatibility.create();
            Wave.create();
            AwarenessNotifications.refresh();
        } else {
            for (ConfigurationBuilder configuration : configurations) {
                configuration.clear();
            }
            compatibility.clearCache();
            Wave.clearCache();
            AwarenessNotifications.clear();
        }

        // System
        ResearchEngine.refresh(enabledPlugin);
    }

    public static void reload(CommandSender sender) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            SpartanReloadEvent event = new SpartanReloadEvent();
            Register.manager.callEvent(event);

            if (event.isCancelled()) {
                return;
            }
        }
        if (sender != null) {
            sender.sendMessage(
                    Config.messages.getColorfulString("config_reload")
            );
        }
        create();
    }

    // Separator

    public static void enableChecks() {
        for (HackType hackType : Enums.HackType.values()) {
            hackType.getCheck().setEnabled(null, true);
        }
    }

    public static void disableChecks() {
        for (HackType hackType : Enums.HackType.values()) {
            hackType.getCheck().setEnabled(null, false);
        }
    }

    // Separator

    public static void enableSilentChecking() {
        for (HackType hackType : Enums.HackType.values()) {
            hackType.getCheck().setSilent(null, true);
        }
    }

    public static void disableSilentChecking() {
        for (HackType hackType : Enums.HackType.values()) {
            hackType.getCheck().setSilent(null, false);
        }
    }

    // Separator

    public static boolean isEnabled(Check.DataType dataType) {
        for (HackType hackType : Enums.HackType.values()) {
            if (hackType.getCheck().isEnabled(dataType, null)) {
                return true;
            }
        }
        return false;
    }
}
