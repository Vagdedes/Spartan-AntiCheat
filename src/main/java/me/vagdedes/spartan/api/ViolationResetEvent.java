package me.vagdedes.spartan.api;

import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ViolationResetEvent extends Event implements Cancellable {

    private final Player player;
    private final Enums.HackType hackType;
    private boolean cancelled;

    public ViolationResetEvent(Player player, Enums.HackType hackType) {
        this.player = player;
        this.hackType = hackType;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public Enums.HackType getHackType() {
        return hackType;
    }

    @Deprecated
    public int getTime() {
        return BackgroundAPI.getViolationResetTime();
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
