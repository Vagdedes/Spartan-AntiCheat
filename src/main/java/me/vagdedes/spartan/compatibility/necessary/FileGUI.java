package me.vagdedes.spartan.compatibility.necessary;

import me.vagdedes.spartan.utils.server.PluginUtils;

public class FileGUI {

    public static final String name = "FileGUI", permission = "filegui.modify";

    public static boolean isEnabled() {
        return PluginUtils.exists(name.toLowerCase());
    }
}
