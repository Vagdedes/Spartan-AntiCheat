package com.vagdedes.spartan.listeners.protocol.modules;

import org.bukkit.Location;

public class TeleportData {

    private final Location location;
    private final boolean silent;

    public TeleportData(Location location, boolean silent) {
        this.location = location;
        this.silent = silent;
    }
    public Location getLocation() { return location; }
    public boolean isSilent() { return silent; }
}
