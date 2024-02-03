package com.vagdedes.spartan.compatibility.semi;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.utils.server.PluginUtils;

public class Authentication {

    public static boolean isEnabled() {
        return Compatibility.CompatibilityType.Authentication.isFunctional()
                && PluginUtils.contains("auth");
    }
}
