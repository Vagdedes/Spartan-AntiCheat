package me.vagdedes.spartan.compatibility.necessary;

import me.vagdedes.spartan.utils.server.PluginUtils;

public class AntiAltAccount {

    public static final String name = "AntiAltAccount";

    public static boolean isEnabled() {
        return PluginUtils.exists(name.toLowerCase());
    }

}
