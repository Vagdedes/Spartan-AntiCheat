package com.vagdedes.spartan.compatibility.necessary;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.utils.server.PluginUtils;

public class Authentication {

    public static boolean isEnabled() {
        return Compatibility.CompatibilityType.Authentication.isFunctional()
                && PluginUtils.contains("auth");
    }
}
