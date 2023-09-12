package me.vagdedes.spartan.api;

import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CheckToggleEvent extends Event implements Cancellable {

    private final Enums.HackType ht;
    private final Enums.ToggleAction ta;
    private boolean cancelled;

    public CheckToggleEvent(Enums.HackType hackType, Enums.ToggleAction toggleAction) {
        ht = hackType;
        ta = toggleAction;
        cancelled = false;
    }

    public Enums.HackType getHackType() {
        return ht;
    }

    public Enums.ToggleAction getToggleAction() {
        return ta;
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
