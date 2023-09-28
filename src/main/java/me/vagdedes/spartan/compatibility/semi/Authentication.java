package me.vagdedes.spartan.compatibility.semi;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.utils.server.PluginUtils;

public class Authentication {

    public static boolean isEnabled() {
        return Compatibility.CompatibilityType.Authentication.isFunctional()
                && PluginUtils.contains("auth");
    }
}
