package me.vagdedes.spartan.api;

import com.vagdedes.spartan.Register;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class SpartanReloadEvent extends Event implements Cancellable {

    private boolean cancelled;

    public SpartanReloadEvent() {
        cancelled = false;
    }

    public Plugin getPlugin() {
        return Register.plugin;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
